package com.github.marekpietrasz.lombok2builder

/** Snapshot of the settings that influence how a builder chain is generated. */
data class ConversionOptions(
    val multiline: Boolean,
    val skipNullValues: Boolean,
    val minValues: Int,
    /**
     * When a setter's argument references the variable being built (so it can't go in the builder),
     * `true` keeps it as a trailing setter after the builder; `false` leaves the whole chain
     * unconverted. Applies to the setters→builder path only.
     */
    val deferSelfReferencingSetters: Boolean,
    /**
     * When a converted local is immediately returned, `true` drops the local and inlines the builder
     * into the `return` statement (`Foo f = ...build(); return f;` -> `return ...build();`).
     */
    val inlineReturnedVariable: Boolean,
    /**
     * When a `new Foo(...)` resolves to a hand-written constructor (real source, not itself
     * `@Builder`-annotated), `false` leaves the call alone — a hand-written constructor may run logic
     * a builder would bypass. `true` converts it anyway. Applies to the constructor→builder path and
     * to the constructor part of a setter chain. Defaults to `false`.
     */
    val convertHandWrittenConstructors: Boolean,
) {
    companion object {
        fun fromSettings(): ConversionOptions {
            val settings = Lombok2BuilderSettings.getInstance()
            return ConversionOptions(
                multiline = settings.multiline,
                skipNullValues = settings.skipNullValues,
                minValues = settings.minValues,
                deferSelfReferencingSetters = settings.deferSelfReferencingSetters,
                inlineReturnedVariable = settings.inlineReturnedVariable,
                convertHandWrittenConstructors = settings.convertHandWrittenConstructors,
            )
        }
    }
}
