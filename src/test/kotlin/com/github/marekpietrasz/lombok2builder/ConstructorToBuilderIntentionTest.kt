package com.github.marekpietrasz.lombok2builder

class ConstructorToBuilderIntentionTest : LombokBuilderTestCase() {

    private val intentionName = "Convert constructor to builder"

    fun testConvertsConstructorWithArguments() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableWithoutBuilder() {
        myFixture.configureByText(
            "Plain.java",
            """
            class Plain {
                Plain(int a) {}

                static Plain make() {
                    return new Pla<caret>in(1);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }
}
