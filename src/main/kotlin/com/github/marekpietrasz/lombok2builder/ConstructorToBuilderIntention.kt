package com.github.marekpietrasz.lombok2builder

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.util.PsiTreeUtil

/** Caret on a `new Foo(...)` call -> `Foo.builder()....build()`. */
class ConstructorToBuilderIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Convert constructor to builder"

    override fun getText(): String = familyName

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val newExpression = findNewExpression(element) ?: return false
        // Layout doesn't affect convertibility, so any value works here.
        return LombokBuilderSupport.constructorToBuilderText(newExpression, multiline = false) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val newExpression = findNewExpression(element) ?: return
        val multiline = Lombok2BuilderSettings.getInstance().multiline
        LombokBuilderSupport.applyConstructor(newExpression, JavaPsiFacade.getElementFactory(project), multiline)
    }

    private fun findNewExpression(element: PsiElement): PsiNewExpression? =
        PsiTreeUtil.getParentOfType(element, PsiNewExpression::class.java, false)
}
