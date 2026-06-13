package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Application-level settings for the conversions. */
@Service(Service.Level.APP)
@State(name = "Lombok2BuilderSettings", storages = [Storage("lombok2builder.xml")])
class Lombok2BuilderSettings : PersistentStateComponent<Lombok2BuilderSettings.State> {

    data class State(
        /** When true, each builder call is generated on its own line. Defaults to true. */
        var multiline: Boolean = true,
        /** When true, `.x(null)` calls are dropped from the generated chain. Defaults to true. */
        var skipNullValues: Boolean = true,
        /** Don't convert when fewer than this many (non-null) values would be set. Defaults to 3. */
        var minValues: Int = 3,
        /**
         * When a setter references the object being built, keep it as a trailing setter after the
         * builder (true) instead of skipping the whole block (false). Defaults to true.
         */
        var deferSelfReferencingSetters: Boolean = true,
        /**
         * When a converted local is immediately returned (`Foo f = Foo.builder()...build(); return f;`),
         * drop the local and inline the builder into the return. Defaults to true.
         */
        var inlineReturnedVariable: Boolean = true,
        /**
         * When a `new Foo(...)` resolves to a hand-written constructor (real source, not itself
         * `@Builder`-annotated), convert it anyway (true) or leave it alone (false). A hand-written
         * constructor may run logic a builder would bypass, so this defaults to false.
         */
        var convertHandWrittenConstructors: Boolean = false,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var multiline: Boolean
        get() = state.multiline
        set(value) {
            state.multiline = value
        }

    var skipNullValues: Boolean
        get() = state.skipNullValues
        set(value) {
            state.skipNullValues = value
        }

    var minValues: Int
        get() = state.minValues
        set(value) {
            state.minValues = value
        }

    var deferSelfReferencingSetters: Boolean
        get() = state.deferSelfReferencingSetters
        set(value) {
            state.deferSelfReferencingSetters = value
        }

    var inlineReturnedVariable: Boolean
        get() = state.inlineReturnedVariable
        set(value) {
            state.inlineReturnedVariable = value
        }

    var convertHandWrittenConstructors: Boolean
        get() = state.convertHandWrittenConstructors
        set(value) {
            state.convertHandWrittenConstructors = value
        }

    companion object {
        fun getInstance(): Lombok2BuilderSettings =
            ApplicationManager.getApplication().getService(Lombok2BuilderSettings::class.java)
    }
}
