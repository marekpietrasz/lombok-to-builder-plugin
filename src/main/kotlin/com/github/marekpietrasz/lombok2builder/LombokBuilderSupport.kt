package com.github.marekpietrasz.lombok2builder

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtil

/**
 * Pure-ish helpers around detecting Lombok `@Builder` classes and turning constructor / setter
 * usages into the equivalent builder chain. Functions that read PSI must run under a read action;
 * the `apply*` functions mutate PSI and must run under a write action.
 *
 * [ConversionOptions] controls the output: line layout, whether `.x(null)` calls are dropped, and
 * the minimum number of (non-null) values required before a usage is converted at all.
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
     * (no `@Builder`, anonymous/array creation, arguments that can't be mapped to parameter names,
     * or fewer than [ConversionOptions.minValues] values once nulls are dropped).
     */
    fun constructorToBuilderText(newExpression: PsiNewExpression, options: ConversionOptions): String? {
        if (newExpression.anonymousClass != null) return null
        val psiClass = resolveClass(newExpression) ?: return null
        if (!hasBuilder(psiClass)) return null
        val receiver = builderReceiver(newExpression) ?: return null
        val arguments = newExpression.argumentList?.expressions ?: return null

        val valueCalls = constructorValueCalls(psiClass, newExpression, arguments, options) ?: return null
        // The minimum-values threshold gates constructor conversions only.
        if (valueCalls.size < options.minValues) return null
        return assemble("$receiver.builder()", valueCalls + ".build()", options.multiline)
    }

    /** Text for folding a setter chain into a builder chain, or null when not convertible. */
    fun chainToBuilderText(chain: SetterChain, options: ConversionOptions): String? {
        val psiClass = resolveClass(chain.newExpression) ?: return null
        val receiver = builderReceiver(chain.newExpression) ?: return null
        val arguments = chain.newExpression.argumentList?.expressions ?: emptyArray()

        val valueCalls = constructorValueCalls(psiClass, chain.newExpression, arguments, options)?.toMutableList()
            ?: return null
        for (call in chain.setterCalls) {
            val value = call.argumentList.expressions.firstOrNull() ?: return null
            if (options.skipNullValues && isNullLiteral(value)) continue
            val methodName = builderMethodName(psiClass, call.methodExpression.referenceName ?: return null)
            valueCalls += ".$methodName(${value.text})"
        }
        // Setter blocks are always converted regardless of the minimum-values threshold (that gates
        // constructor calls only); only bail if nothing would be set (e.g. the lone setter was null).
        if (valueCalls.isEmpty()) return null
        return assemble("$receiver.builder()", valueCalls + ".build()", options.multiline)
    }

    /**
     * The receiver text for the generated `.builder()` call, preserving any outer-class or package
     * qualifier exactly as written at the `new` site (e.g. `Outer.Inner` for `new Outer.Inner(...)`).
     *
     * Using the class's simple name alone would drop the `Outer.` qualifier and emit
     * `Inner.builder()`, which doesn't compile when `Inner` isn't accessible by its simple name. The
     * reference name carries no type arguments, so `new Foo<String>(...)` still yields `Foo.builder()`.
     */
    private fun builderReceiver(newExpression: PsiNewExpression): String? {
        val reference = newExpression.classReference ?: return null
        val name = reference.referenceName ?: return null
        val qualifier = reference.qualifier?.text
        return if (qualifier != null) "$qualifier.$name" else name
    }

    /** `.param(arg)` call strings for constructor arguments (null args dropped per options), or null
     *  when arguments can't be mapped to parameter names. */
    private fun constructorValueCalls(
        psiClass: PsiClass,
        newExpression: PsiNewExpression,
        arguments: Array<out PsiExpression>,
        options: ConversionOptions,
    ): List<String>? {
        if (arguments.isEmpty()) return emptyList()
        val parameters = newExpression.resolveConstructor()?.parameterList?.parameters ?: return null
        // Bail on varargs / mismatches where positional->name mapping is ambiguous.
        if (parameters.size != arguments.size) return null
        // The builder method for a constructor argument is the field named after the parameter.
        // A hand-written constructor may use parameter names that don't match the fields (e.g.
        // parameter `category` backing field `feeCategory`, often assigning via a setter); we can't
        // map those safely, so refuse to convert rather than emit an invalid `.category(...)` call.
        if (parameters.any { psiClass.findFieldByName(it.name, true) == null }) return null
        val calls = mutableListOf<String>()
        for (i in arguments.indices) {
            if (options.skipNullValues && isNullLiteral(arguments[i])) continue
            calls += ".${parameters[i].name}(${arguments[i].text})"
        }
        return calls
    }

    /** Joins the builder calls onto one line, or one call per line when [multiline]. */
    private fun assemble(prefix: String, calls: List<String>, multiline: Boolean): String =
        if (multiline) (listOf(prefix) + calls).joinToString("\n") else prefix + calls.joinToString("")

    private fun isNullLiteral(expression: PsiExpression): Boolean {
        val unwrapped = PsiUtil.skipParenthesizedExprDown(expression) ?: return false
        return unwrapped is PsiLiteralExpression && unwrapped.text == "null"
    }

    /** Naive Lombok builder/field name for a setter: `setFooBar` -> `fooBar`. */
    fun builderFieldName(setterName: String): String =
        setterName.removePrefix("set").replaceFirstChar { it.lowercaseChar() }

    /**
     * The Lombok builder method name for [setterName], which equals the backing field name.
     *
     * Resolves against the class's actual fields so the boolean `is`-prefix quirk is handled: a
     * primitive `boolean isFoo` field has setter `setFoo`, but its field — and therefore its builder
     * method — is `isFoo`, not `foo`. Falls back to the naive name when no matching field is found.
     */
    private fun builderMethodName(psiClass: PsiClass, setterName: String): String {
        val candidate = builderFieldName(setterName)               // setSomething -> something
        if (psiClass.findFieldByName(candidate, true) != null) return candidate
        val isField = "is" + setterName.removePrefix("set")        // setSomething -> isSomething
        val field = psiClass.findFieldByName(isField, true)
        if (field != null && field.type.equalsToText("boolean")) return isField
        return candidate
    }

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
    fun applyConstructor(newExpression: PsiNewExpression, factory: PsiElementFactory, options: ConversionOptions): Boolean {
        val text = constructorToBuilderText(newExpression, options) ?: return false
        val replaced = newExpression.replace(factory.createExpressionFromText(text, newExpression))
        if (options.multiline) reformat(replaced)
        return true
    }

    /** Folds a setter chain into a single builder declaration. Returns false if not convertible. */
    fun applyChain(chain: SetterChain, factory: PsiElementFactory, options: ConversionOptions): Boolean {
        val text = chainToBuilderText(chain, options) ?: return false
        val declaration = chain.declarationStatement
        val newInitializer = factory.createExpressionFromText(text, chain.variable)
        chain.variable.initializer?.replace(newInitializer) ?: return false
        for (statement in chain.setterStatements) statement.delete()
        if (options.multiline) reformat(declaration)
        return true
    }

    private fun reformat(element: PsiElement) {
        if (!element.isValid) return
        CodeStyleManager.getInstance(element.project).reformat(element)
    }
}
