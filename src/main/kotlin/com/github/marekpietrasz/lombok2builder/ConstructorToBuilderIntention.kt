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
        return LombokBuilderSupport.constructorToBuilderText(newExpression) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val newExpression = findNewExpression(element) ?: return
        LombokBuilderSupport.applyConstructor(newExpression, JavaPsiFacade.getElementFactory(project))
    }

    private fun findNewExpression(element: PsiElement): PsiNewExpression? =
        PsiTreeUtil.getParentOfType(element, PsiNewExpression::class.java, false)
}
