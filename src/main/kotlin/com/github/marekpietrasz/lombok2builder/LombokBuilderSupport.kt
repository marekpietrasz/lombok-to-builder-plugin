package com.github.marekpietrasz.lombok2builder

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
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
import com.intellij.psi.PsiReturnStatement
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

    /** The Java setter prefix (`setFoo`). Its length is where the property name starts. */
    private const val SETTER_PREFIX = "set"

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

    /**
     * A planned setter-chain conversion: the builder initializer text plus, for the chain's trailing
     * setter statements, which to delete (folded into the builder) and which to keep in place.
     *
     * A setter whose argument references the variable being built (e.g. a child pointing back at its
     * parent) can't be folded into the initializer — that would read the variable before it is
     * assigned. Those are [deferredStatements]: they stay put and run after the builder assigns the
     * variable. See [ConversionOptions.deferSelfReferencingSetters].
     */
    data class ChainPlan(
        val builderText: String,
        val foldedStatements: List<PsiExpressionStatement>,
        val deferredStatements: List<PsiExpressionStatement>,
    )

    /** Plans how to fold [chain] into a builder, or null when it isn't convertible. */
    fun planChain(chain: SetterChain, options: ConversionOptions): ChainPlan? {
        val psiClass = resolveClass(chain.newExpression) ?: return null
        val receiver = builderReceiver(chain.newExpression) ?: return null
        val arguments = chain.newExpression.argumentList?.expressions ?: emptyArray()
        val constructorCalls = constructorValueCalls(psiClass, chain.newExpression, arguments, options) ?: return null
        val partition = partitionSetters(chain, options) ?: return null
        val setterCalls = setterValueCalls(psiClass, partition.foldedCalls, options) ?: return null

        val valueCalls = constructorCalls + setterCalls
        // Setter blocks ignore the minimum-values threshold (that gates constructor calls only); only
        // bail when nothing can be folded into the builder (every value was null and/or deferred).
        if (valueCalls.isEmpty()) return null
        val builderText = assemble("$receiver.builder()", valueCalls + ".build()", options.multiline)
        return ChainPlan(builderText, partition.foldedStatements, partition.deferredStatements)
    }

    /** Builder initializer text for [chain], or null when not convertible (deferred setters aside). */
    fun chainToBuilderText(chain: SetterChain, options: ConversionOptions): String? =
        planChain(chain, options)?.builderText

    private data class SetterPartition(
        val foldedCalls: List<PsiMethodCallExpression>,
        val foldedStatements: List<PsiExpressionStatement>,
        val deferredStatements: List<PsiExpressionStatement>,
    )

    /**
     * Splits a chain's setter calls into those foldable into the builder and those that must stay as
     * trailing setters because their argument references the variable being built. Returns null when
     * a setter is malformed, or — when [ConversionOptions.deferSelfReferencingSetters] is false — when
     * any setter self-references (the whole chain is then left unconverted).
     */
    private fun partitionSetters(chain: SetterChain, options: ConversionOptions): SetterPartition? {
        val foldedCalls = mutableListOf<PsiMethodCallExpression>()
        val foldedStatements = mutableListOf<PsiExpressionStatement>()
        val deferredStatements = mutableListOf<PsiExpressionStatement>()
        for (i in chain.setterCalls.indices) {
            val call = chain.setterCalls[i]
            val value = call.argumentList.expressions.firstOrNull() ?: return null
            if (referencesVariable(value, chain.variable)) {
                if (!options.deferSelfReferencingSetters) return null
                deferredStatements += chain.setterStatements[i]
            } else {
                foldedCalls += call
                foldedStatements += chain.setterStatements[i]
            }
        }
        return SetterPartition(foldedCalls, foldedStatements, deferredStatements)
    }

    /** True when [expression] reads [variable] anywhere within it. */
    private fun referencesVariable(expression: PsiExpression, variable: PsiLocalVariable): Boolean {
        var found = false
        expression.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceExpression(reference: PsiReferenceExpression) {
                if (found) return
                super.visitReferenceExpression(reference)
                if (reference.resolve() == variable) found = true
            }
        })
        return found
    }

    /** `.method(arg)` call strings for a chain's `setX(...)` calls (null values dropped per options),
     *  or null when a setter call is malformed. */
    private fun setterValueCalls(
        psiClass: PsiClass,
        setterCalls: List<PsiMethodCallExpression>,
        options: ConversionOptions,
    ): List<String>? {
        val calls = mutableListOf<String>()
        for (call in setterCalls) {
            val value = call.argumentList.expressions.firstOrNull() ?: return null
            if (options.skipNullValues && isNullLiteral(value)) continue
            val methodName = builderMethodName(psiClass, call.methodExpression.referenceName ?: return null)
            calls += ".$methodName(${value.text})"
        }
        return calls
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
        setterName.removePrefix(SETTER_PREFIX).replaceFirstChar { it.lowercaseChar() }

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
        val isField = "is" + setterName.removePrefix(SETTER_PREFIX) // setSomething -> isSomething
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

        val setters = collectFollowingSetterCalls(declaration, variable)
        if (setters.isEmpty()) return null
        val (setterStatements, setterCalls) = setters.unzip()
        return SetterChain(variable, declaration, newExpression, setterStatements, setterCalls)
    }

    /** The contiguous `variable.setX(...)` statements immediately following [declaration]. */
    private fun collectFollowingSetterCalls(
        declaration: PsiDeclarationStatement,
        variable: PsiLocalVariable,
    ): List<Pair<PsiExpressionStatement, PsiMethodCallExpression>> {
        val setters = mutableListOf<Pair<PsiExpressionStatement, PsiMethodCallExpression>>()
        var sibling = declaration.nextSibling
        while (sibling != null) {
            if (sibling is PsiWhiteSpace || sibling is PsiComment) {
                sibling = sibling.nextSibling
                continue
            }
            val setter = setterStatementOf(sibling, variable) ?: break
            setters += setter
            sibling = sibling.nextSibling
        }
        return setters
    }

    /** [element] as a `variable.setX(arg)` statement paired with its call, or null when it isn't one. */
    private fun setterStatementOf(
        element: PsiElement,
        variable: PsiLocalVariable,
    ): Pair<PsiExpressionStatement, PsiMethodCallExpression>? {
        val statement = element as? PsiExpressionStatement ?: return null
        val call = statement.expression as? PsiMethodCallExpression ?: return null
        if (!isSetterCallOn(call, variable)) return null
        return statement to call
    }

    private fun isSetterCallOn(call: PsiMethodCallExpression, variable: PsiLocalVariable): Boolean {
        val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return false
        if (qualifier.resolve() != variable) return false
        val name = call.methodExpression.referenceName ?: return false
        if (!name.startsWith(SETTER_PREFIX) || name.length <= SETTER_PREFIX.length) return false
        return call.argumentList.expressions.size == 1
    }

    /** Replaces a constructor call with its builder chain. Returns false if not convertible. */
    fun applyConstructor(newExpression: PsiNewExpression, factory: PsiElementFactory, options: ConversionOptions): Boolean {
        val text = constructorToBuilderText(newExpression, options) ?: return false
        // The local being initialised, if any — captured before replace so it survives the swap and
        // can be inlined into a following `return`.
        val variable = newExpression.parent as? PsiLocalVariable
        val replaced = newExpression.replace(factory.createExpressionFromText(text, newExpression))
        if (options.multiline) reformat(replaced)
        if (variable != null) inlineReturnedVariable(variable, factory, options)
        return true
    }

    /** Folds a setter chain into a single builder declaration, leaving any self-referencing setters
     *  in place as trailing statements. Returns false if not convertible. */
    fun applyChain(chain: SetterChain, factory: PsiElementFactory, options: ConversionOptions): Boolean {
        val plan = planChain(chain, options) ?: return false
        val declaration = chain.declarationStatement
        val newInitializer = factory.createExpressionFromText(plan.builderText, chain.variable)
        chain.variable.initializer?.replace(newInitializer) ?: return false
        for (statement in plan.foldedStatements) statement.delete()
        // Deferred (self-referencing) setters stay put so they run after the variable is assigned.
        if (options.multiline) reformat(declaration)
        // A deferred setter would sit between the declaration and any `return`, so this only fires
        // when the builder is the variable's last word before it's returned.
        inlineReturnedVariable(chain.variable, factory, options)
        return true
    }

    /**
     * When [variable]'s single-variable declaration is immediately followed by `return <variable>;`,
     * folds the initializer into the return and drops the now-pointless local:
     * `Foo f = Foo.builder()...build(); return f;` -> `return Foo.builder()...build();`.
     *
     * No-op (returns false) unless the very next statement returns exactly this variable; a comment
     * between the two suppresses it, to avoid orphaning the comment. Gated by
     * [ConversionOptions.inlineReturnedVariable].
     */
    fun inlineReturnedVariable(
        variable: PsiLocalVariable,
        factory: PsiElementFactory,
        options: ConversionOptions,
    ): Boolean {
        if (!options.inlineReturnedVariable) return false
        val declaration = variable.parent as? PsiDeclarationStatement ?: return false
        if (declaration.declaredElements.size != 1) return false
        val initializer = variable.initializer ?: return false
        // The next statement, skipping only whitespace — an intervening comment suppresses the inline.
        var sibling = declaration.nextSibling
        while (sibling is PsiWhiteSpace) sibling = sibling.nextSibling
        val returnStatement = sibling as? PsiReturnStatement ?: return false
        val returned = PsiUtil.skipParenthesizedExprDown(returnStatement.returnValue) as? PsiReferenceExpression ?: return false
        if (returned.resolve() != variable) return false

        val newReturn = returnStatement.replace(
            factory.createStatementFromText("return ${initializer.text};", returnStatement),
        )
        declaration.delete()
        if (options.multiline) reformat(newReturn)
        return true
    }

    private fun reformat(element: PsiElement) {
        if (!element.isValid) return
        CodeStyleManager.getInstance(element.project).reformat(element)
    }
}
