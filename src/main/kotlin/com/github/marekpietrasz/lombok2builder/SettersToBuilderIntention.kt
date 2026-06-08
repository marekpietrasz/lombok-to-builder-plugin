package com.github.marekpietrasz.lombok2builder

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

/**
 * Caret on a `Foo f = new Foo();` declaration (or on one of its trailing `f.setX(...)` calls)
 * -> folds the whole chain into `Foo f = Foo.builder()....build();`.
 */
class SettersToBuilderIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Convert setters to builder"

    override fun getText(): String = familyName

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val chain = findChain(element) ?: return false
        // Respect settings (min-values / skip-nulls) so the intention is hidden when it wouldn't convert.
        return LombokBuilderSupport.chainToBuilderText(chain, ConversionOptions.fromSettings()) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val chain = findChain(element) ?: return
        LombokBuilderSupport.applyChain(chain, JavaPsiFacade.getElementFactory(project), ConversionOptions.fromSettings())
    }

    private fun findChain(element: PsiElement): LombokBuilderSupport.SetterChain? {
        val variable = findVariable(element) ?: return null
        return LombokBuilderSupport.collectChain(variable)
    }

    private fun findVariable(element: PsiElement): PsiLocalVariable? {
        PsiTreeUtil.getParentOfType(element, PsiLocalVariable::class.java, false)?.let { return it }
        // Caret sits on one of the `f.setX(...)` calls: resolve back to the variable.
        val call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java, false) ?: return null
        val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return null
        return qualifier.resolve() as? PsiLocalVariable
    }
}
