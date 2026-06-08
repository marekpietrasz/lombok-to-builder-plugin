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

    fun testMinValuesDoesNotBlockSetters() {
        // The minimum-values threshold gates constructor calls only; setter blocks always convert.
        setMultiline(false)
        setMinValues(5)
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

    fun testConvertsConstructorWithArgsPlusSetters() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int id;
                String name;

                Demo(int id) {}
                void setName(String name) {}

                static void use() {
                    Demo d = new De<caret>mo(5);
                    d.setName("x");
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
                int id;
                String name;

                Demo(int id) {}
                void setName(String name) {}

                static void use() {
                    Demo d = Demo.builder().id(5).name("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableWhenConstructorParamMismatchesField() {
        // Constructor param `category` doesn't match field `feeCategory`, so the whole block
        // (constructor + setters) is left unconverted rather than dropping the constructor's value.
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                String feeCategory;
                String name;

                Demo(String category) {}
                void setName(String name) {}

                static void use() {
                    Demo d = new De<caret>mo("cat");
                    d.setName("x");
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
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
