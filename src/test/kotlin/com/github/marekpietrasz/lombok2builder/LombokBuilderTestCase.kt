package com.github.marekpietrasz.lombok2builder

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Base for tests: registers a minimal `lombok.Builder` stub so `@Builder` resolves. */
abstract class LombokBuilderTestCase : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package lombok;\npublic @interface Builder {}")
        // Most tests use 1-2 values; lower the threshold so the default (3) doesn't suppress them.
        // Threshold-specific tests set it explicitly.
        setMinValues(1)
    }

    override fun tearDown() {
        try {
            // Application-level settings persist across tests; restore the defaults.
            val settings = Lombok2BuilderSettings.getInstance()
            settings.multiline = true
            settings.skipNullValues = true
            settings.minValues = 3
            settings.deferSelfReferencingSetters = true
        } finally {
            super.tearDown()
        }
    }

    protected fun setMultiline(value: Boolean) {
        Lombok2BuilderSettings.getInstance().multiline = value
    }

    protected fun setSkipNullValues(value: Boolean) {
        Lombok2BuilderSettings.getInstance().skipNullValues = value
    }

    protected fun setMinValues(value: Int) {
        Lombok2BuilderSettings.getInstance().minValues = value
    }

    protected fun setDeferSelfReferencingSetters(value: Boolean) {
        Lombok2BuilderSettings.getInstance().deferSelfReferencingSetters = value
    }
}
