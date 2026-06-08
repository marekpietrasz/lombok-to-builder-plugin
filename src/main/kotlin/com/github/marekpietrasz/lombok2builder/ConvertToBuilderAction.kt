package com.github.marekpietrasz.lombok2builder

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager

/**
 * Batch entry point. In the editor: no selection converts the whole file, a selection converts that
 * range. In the Project view: every Java file under the selected files/folders/modules is converted.
 */
class ConvertToBuilderAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && hasJavaTarget(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val targets = collectTargets(e, project)
        if (targets.isEmpty()) {
            notify(project, "No Java files found to convert.")
            return
        }

        var converted = 0
        WriteCommandAction.runWriteCommandAction(project, "Convert Lombok Usages to Builder", null, {
            for ((file, range) in targets) {
                converted += BuilderConversionEngine.convertFile(file, range)
            }
        })

        notify(project, "Converted $converted usage(s) to builder in ${targets.size} file(s).")
    }

    // --- target collection ----------------------------------------------------------------------

    /** A file to convert plus an optional sub-range (editor selection). */
    private data class Target(val file: PsiJavaFile, val range: TextRange?)

    private fun collectTargets(e: AnActionEvent, project: Project): List<Target> {
        // Prefer an active editor: whole file, or just the selection.
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (editor != null && psiFile is PsiJavaFile) {
            val selection = editor.selectionModel
            val range = if (selection.hasSelection()) {
                TextRange(selection.selectionStart, selection.selectionEnd)
            } else {
                null
            }
            return listOf(Target(psiFile, range))
        }

        // Otherwise treat the Project view selection as files/folders to recurse.
        val roots = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        val manager = PsiManager.getInstance(project)
        return collectJavaFiles(roots.asList())
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
            .map { Target(it, null) }
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
