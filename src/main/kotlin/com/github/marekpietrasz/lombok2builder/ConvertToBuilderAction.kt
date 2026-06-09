package com.github.marekpietrasz.lombok2builder

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager

/**
 * Batch entry point. In the editor: no selection converts the whole file, a selection converts that
 * range. In the Project view: every Java file under the selected files/folders/modules is converted.
 *
 * The editor path is a single, immediate write command (it touches one file). The Project-view path
 * can span thousands of files, so it runs in a cancellable background task that enumerates files off
 * the EDT and applies each file in its own short write command — the EDT is never held for the whole
 * batch, keeping the IDE responsive instead of freezing for the duration.
 */
class ConvertToBuilderAction : AnAction() {

    private val title = "Convert Lombok Usages to Builder"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && hasJavaTarget(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Editor path: one file (optionally a selection). Fast, immediate, single undo step.
        val editorTarget = editorTarget(e)
        if (editorTarget != null) {
            convertSingle(project, editorTarget)
            return
        }

        // Project-view path: the selected files/folders/modules. Enumeration and conversion happen in
        // the background; only the roots (event data) are read here on the EDT.
        val roots = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
        if (roots.isNullOrEmpty()) {
            notify(project, "No Java files found to convert.")
            return
        }
        convertBatchInBackground(project, roots)
    }

    private fun convertSingle(project: Project, target: Target) {
        var converted = 0
        WriteCommandAction.runWriteCommandAction(project, title, null, {
            converted += BuilderConversionEngine.convertFile(target.file, target.range)
        })
        notify(project, "Converted $converted usage(s) to builder in 1 file(s).")
    }

    private fun convertBatchInBackground(project: Project, roots: List<VirtualFile>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                // Walking the VFS tree is thread-safe and needs no read action; PSI is only touched
                // later, one file at a time, inside the per-file write command on the EDT.
                val files = collectJavaFiles(roots)
                if (files.isEmpty()) {
                    notify(project, "No Java files found to convert.")
                    return
                }

                val manager = PsiManager.getInstance(project)
                var converted = 0
                files.forEachIndexed { index, virtualFile ->
                    indicator.checkCanceled()
                    indicator.fraction = index.toDouble() / files.size
                    indicator.text2 = virtualFile.name
                    // One short write command per file: the EDT yields between files so the IDE stays
                    // responsive and the operation can be cancelled mid-run.
                    ApplicationManager.getApplication().invokeAndWait {
                        if (!virtualFile.isValid) return@invokeAndWait
                        val psiFile = manager.findFile(virtualFile) as? PsiJavaFile ?: return@invokeAndWait
                        WriteCommandAction.runWriteCommandAction(project, title, null, {
                            converted += BuilderConversionEngine.convertFile(psiFile, null)
                        })
                    }
                }

                notify(project, "Converted $converted usage(s) to builder in ${files.size} file(s).")
            }
        })
    }

    // --- target collection ----------------------------------------------------------------------

    /** A file to convert plus an optional sub-range (editor selection). */
    private data class Target(val file: PsiJavaFile, val range: TextRange?)

    /** The active-editor target (whole file, or just the selection), or null when not in an editor. */
    private fun editorTarget(e: AnActionEvent): Target? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return null
        val selection = editor.selectionModel
        val range = if (selection.hasSelection()) {
            TextRange(selection.selectionStart, selection.selectionEnd)
        } else {
            null
        }
        return Target(psiFile, range)
    }

    private fun collectJavaFiles(roots: List<VirtualFile>): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val stack = ArrayDeque(roots)
        while (stack.isNotEmpty()) {
            val file = stack.removeLast()
            when {
                file.isDirectory -> stack.addAll(file.children)
                file.extension == "java" -> result += file
            }
        }
        return result
    }

    private fun hasJavaTarget(e: AnActionEvent): Boolean {
        if (e.getData(CommonDataKeys.EDITOR) != null && e.getData(CommonDataKeys.PSI_FILE) is PsiJavaFile) {
            return true
        }
        val roots = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
        return roots.any { it.isDirectory || it.extension == "java" }
    }

    private fun notify(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Lombok To Builder")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }
}
