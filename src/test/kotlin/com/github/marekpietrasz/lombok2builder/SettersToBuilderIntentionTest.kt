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

    fun testSkipsNullSetterValuesByDefault() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}
                void setC(String c) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB(null);
                    d.setC("x");
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
                void setC(String c) {}

                static void use() {
                    Demo d = Demo.builder().a(1).c("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableBelowMinValues() {
        setMinValues(3)
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

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    fun testBooleanIsPrefixedFieldUsesFieldNameAsBuilderMethod() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                private boolean isSomething;
                private int value;

                void setSomething(boolean something) {}
                void setValue(int value) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setSomething(true);
                    d.setValue(5);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        // Field is `isSomething`, so the Lombok builder method is `isSomething(...)`, NOT `something(...)`.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                private boolean isSomething;
                private int value;

                void setSomething(boolean something) {}
                void setValue(int value) {}

                static void use() {
                    Demo d = Demo.builder().isSomething(true).value(5).build();
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
