package com.github.marekpietrasz.lombok2builder

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiWhiteSpace

/**
 * Pure-ish helpers around detecting Lombok `@Builder` classes and turning constructor / setter
 * usages into the equivalent builder chain. Functions that read PSI must run under a read action;
 * the `apply*` functions mutate PSI and must run under a write action.
 */
object LombokBuilderSupport {

    private val BUILDER_ANNOTATIONS = setOf("lombok.Builder", "lombok.experimental.SuperBuilder")

    /** A `Foo v = new Foo(...);` declaration immediately followed by `v.setX(...)` statements. */
    data class SetterChain(
        val variable: PsiLocalVariable,
        val declarationStatement: PsiDeclarationStatement,
        val newExpression: PsiNewExpression,
        val setterStatements: List<PsiExpressionStatement>,
        val setterCalls: List<PsiMethodCallExpression>,
    )

    fun resolveClass(newExpression: PsiNewExpression): PsiClass? =
        newExpression.classReference?.resolve() as? PsiClass

    /** True when the class, or one of its constructors/methods, carries `@Builder`/`@SuperBuilder`. */
    fun hasBuilder(psiClass: PsiClass?): Boolean {
        if (psiClass == null) return false
        if (psiClass.isBuilderAnnotated()) return true
        if (psiClass.constructors.any { it.isBuilderAnnotated() }) return true
        if (psiClass.methods.any { it.isBuilderAnnotated() }) return true
        return false
    }

    private fun PsiModifierListOwner.isBuilderAnnotated(): Boolean =
        annotations.any { it.qualifiedName in BUILDER_ANNOTATIONS }

    /**
     * Text for converting a constructor call to a builder chain, or null when not safely convertible
     * (no `@Builder`, anonymous/array creation, or arguments that can't be mapped to parameter names).
     */
    fun constructorToBuilderText(newExpression: PsiNewExpression): String? {
        if (newExpression.anonymousClass != null) return null
        val psiClass = resolveClass(newExpression) ?: return null
        if (!hasBuilder(psiClass)) return null
        val className = psiClass.name ?: return null
        val arguments = newExpression.argumentList?.expressions ?: return null

        val builder = StringBuilder(className).append(".builder()")
        if (!appendArgumentCalls(builder, newExpression, arguments)) return null
        return builder.append(".build()").toString()
    }

    /** Text for folding a setter chain into a builder chain, or null when not convertible. */
    fun chainToBuilderText(chain: SetterChain): String? {
        val className = resolveClass(chain.newExpression)?.name ?: return null
        val arguments = chain.newExpression.argumentList?.expressions ?: emptyArray()

        val builder = StringBuilder(className).append(".builder()")
        if (!appendArgumentCalls(builder, chain.newExpression, arguments)) return null
        for (call in chain.setterCalls) {
            val fieldName = builderFieldName(call.methodExpression.referenceName ?: return null)
            val value = call.argumentList.expressions.firstOrNull() ?: return null
            builder.append(".").append(fieldName).append("(").append(value.text).append(")")
        }
        return builder.append(".build()").toString()
    }

    /** Appends `.param(arg)` calls for constructor arguments; returns false if they can't be mapped. */
    private fun appendArgumentCalls(
        builder: StringBuilder,
        newExpression: PsiNewExpression,
        arguments: Array<out com.intellij.psi.PsiExpression>,
    ): Boolean {
        if (arguments.isEmpty()) return true
        val parameters = newExpression.resolveConstructor()?.parameterList?.parameters ?: return false
        // Bail on varargs / mismatches where positional->name mapping is ambiguous.
        if (parameters.size != arguments.size) return false
        for (i in arguments.indices) {
            val name = parameters[i].name
            builder.append(".").append(name).append("(").append(arguments[i].text).append(")")
        }
        return true
    }

    /** Lombok builder method name for a setter: `setFooBar` -> `fooBar`. */
    fun builderFieldName(setterName: String): String =
        setterName.removePrefix("set").replaceFirstChar { it.lowercaseChar() }

    /**
     * Collects the contiguous setter chain rooted at [variable], or null when the variable isn't a
     * `@Builder` type initialised with `new ...()`, or has no following setter calls.
     */
    fun collectChain(variable: PsiLocalVariable): SetterChain? {
        val newExpression = variable.initializer as? PsiNewExpression ?: return null
        if (!hasBuilder(resolveClass(newExpression))) return null
        val declaration = variable.parent as? PsiDeclarationStatement ?: return null
        // Replacing a multi-variable declaration ("Foo a, b;") would be unsafe.
        if (declaration.declaredElements.size != 1) return null

        val setterStatements = mutableListOf<PsiExpressionStatement>()
        val setterCalls = mutableListOf<PsiMethodCallExpression>()
        var sibling = declaration.nextSibling
        while (sibling != null) {
            if (sibling is PsiWhiteSpace || sibling is PsiComment) {
                sibling = sibling.nextSibling
                continue
            }
            val statement = sibling as? PsiExpressionStatement ?: break
            val call = statement.expression as? PsiMethodCallExpression ?: break
            if (!isSetterCallOn(call, variable)) break
            setterStatements += statement
            setterCalls += call
            sibling = sibling.nextSibling
        }
        if (setterCalls.isEmpty()) return null
        return SetterChain(variable, declaration, newExpression, setterStatements, setterCalls)
    }

    private fun isSetterCallOn(call: PsiMethodCallExpression, variable: PsiLocalVariable): Boolean {
        val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return false
        if (qualifier.resolve() != variable) return false
        val name = call.methodExpression.referenceName ?: return false
        if (!name.startsWith("set") || name.length <= 3) return false
        return call.argumentList.expressions.size == 1
    }

    /** Replaces a constructor call with its builder chain. Returns false if not convertible. */
    fun applyConstructor(newExpression: PsiNewExpression, factory: PsiElementFactory): Boolean {
        val text = constructorToBuilderText(newExpression) ?: return false
        newExpression.replace(factory.createExpressionFromText(text, newExpression))
        return true
    }

    /** Folds a setter chain into a single builder declaration. Returns false if not convertible. */
    fun applyChain(chain: SetterChain, factory: PsiElementFactory): Boolean {
        val text = chainToBuilderText(chain) ?: return false
        val newInitializer = factory.createExpressionFromText(text, chain.variable)
        chain.variable.initializer?.replace(newInitializer) ?: return false
        for (statement in chain.setterStatements) statement.delete()
        return true
    }
}
