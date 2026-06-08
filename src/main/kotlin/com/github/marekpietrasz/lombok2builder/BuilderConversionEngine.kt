package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * Converts every applicable usage in a scope. Used by the right-click action for whole-file,
 * selection, and folder/project batch conversions. Must run inside a write action.
 */
object BuilderConversionEngine {

    /**
     * Converts setter chains and constructor calls (with arguments) of `@Builder` classes in [file].
     * When [range] is non-null only usages fully contained in it are converted.
     *
     * @return number of usages converted.
     */
    fun convertFile(file: PsiJavaFile, range: TextRange? = null): Int {
        val project = file.project
        val factory = JavaPsiFacade.getElementFactory(project)
        val pointerManager = SmartPointerManager.getInstance(project)
        val options = ConversionOptions.fromSettings()
        var converted = 0

        // Phase 1: setter chains (declaration + trailing setters). Resolve via smart pointers so
        // earlier edits don't invalidate later targets.
        val chainVariables = PsiTreeUtil.collectElementsOfType(file, PsiLocalVariable::class.java)
            .mapNotNull { LombokBuilderSupport.collectChain(it) }
            .filter { chain -> chain.isWithin(range) }
            .map { pointerManager.createSmartPsiElementPointer(it.variable) }

        for (pointer in chainVariables) {
            val variable = pointer.element ?: continue
            val chain = LombokBuilderSupport.collectChain(variable) ?: continue
            if (LombokBuilderSupport.applyChain(chain, factory, options)) converted++
        }

        // Phase 2: remaining standalone constructor calls (with arguments). Innermost-first so that
        // nested `new Outer(new Inner(...))` both convert.
        val newExpressions = PsiTreeUtil.collectElementsOfType(file, PsiNewExpression::class.java)
            .filter { it.argumentList?.expressions?.isNotEmpty() == true }
            .filter { range == null || range.contains(it.textRange) }
            .filter { LombokBuilderSupport.hasBuilder(LombokBuilderSupport.resolveClass(it)) }
            .sortedBy { it.textRange.length }
            .map { pointerManager.createSmartPsiElementPointer(it) }

        for (pointer in newExpressions) {
            val newExpression = pointer.element ?: continue
            if (!newExpression.isValid) continue
            if (LombokBuilderSupport.applyConstructor(newExpression, factory, options)) converted++
        }

        return converted
    }

    private fun LombokBuilderSupport.SetterChain.isWithin(range: TextRange?): Boolean {
        if (range == null) return true
        if (!range.contains(declarationStatement.textRange)) return false
        return setterStatements.all { range.contains(it.textRange) }
    }
}
