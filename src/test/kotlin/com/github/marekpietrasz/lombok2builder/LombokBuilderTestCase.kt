package com.github.marekpietrasz.lombok2builder

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Base for tests: registers a minimal `lombok.Builder` stub so `@Builder` resolves. */
abstract class LombokBuilderTestCase : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package lombok;\npublic @interface Builder {}")
    }
}
