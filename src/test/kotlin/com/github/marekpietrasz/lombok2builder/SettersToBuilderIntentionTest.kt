package com.github.marekpietrasz.lombok2builder

class SettersToBuilderIntentionTest : LombokBuilderTestCase() {

    private val intentionName = "Convert setters to builder"

    fun testConvertsSetterChainSingleLine() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static void use() {
                    Demo d = Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testConvertsSetterChainMultilineByDefault() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static void use() {
                    Demo d = Demo.builder()
                            .a(1)
                            .b("x")
                            .build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testAvailableFromSetterCall() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}

                static void use() {
                    Demo d = new Demo();
                    d.set<caret>A(1);
                }
            }
            """.trimIndent(),
        )

        assertNotEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    fun testNotAvailableWithoutBuilder() {
        myFixture.configureByText(
            "Plain.java",
            """
            class Plain {
                void setA(int a) {}

                static void use() {
                    Plain p = new Pl<caret>ain();
                    p.setA(1);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }
}
