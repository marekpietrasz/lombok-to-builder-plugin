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

    companion object {
        fun getInstance(): Lombok2BuilderSettings =
            ApplicationManager.getApplication().getService(Lombok2BuilderSettings::class.java)
    }
}
