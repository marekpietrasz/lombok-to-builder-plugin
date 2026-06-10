package com.github.marekpietrasz.lombok2builder

/**
 * Exercises the post-conversion cleanup that inlines a freshly-built local into a following `return`,
 * across both the setter-chain and constructor entry points and via the whole-file action.
 */
class InlineReturnedVariableTest : LombokBuilderTestCase() {

    private val settersIntention = "Convert setters to builder"
    private val constructorIntention = "Convert constructor to builder"

    fun testInlinesSetterChainReturnedNext() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testInlinesConstructorReturnedNext() {
        setMultiline(false)
        setMinValues(2)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}

                static Demo make() {
                    Demo d = new De<caret>mo(1, "x");
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(constructorIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testInlinesMultilineBuilderIntoReturn() {
        // multiline is the default; the whole chain moves onto the return statement.
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    return Demo.builder()
                            .a(1)
                            .b("x")
                            .build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testDoesNotInlineWhenDisabled() {
        setMultiline(false)
        setInlineReturnedVariable(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        // The local and its return are left untouched; only the builder fold happened.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = Demo.builder().a(1).b("x").build();
                    return d;
                }
            }
            """.trimIndent(),
        )
    }

    fun testDoesNotInlineWhenDeferredSetterSitsBeforeReturn() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Node {
                String name;
                Node self;

                void setName(String name) {}
                void setSelf(Node self) {}

                static Node make() {
                    Node n = new No<caret>de();
                    n.setName("x");
                    n.setSelf(n);
                    return n;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        // The deferred self-setter sits between the declaration and the return, so the variable is
        // still used after the builder and must not be inlined.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Node {
                String name;
                Node self;

                void setName(String name) {}
                void setSelf(Node self) {}

                static Node make() {
                    Node n = Node.builder().name("x").build();
                    n.setSelf(n);
                    return n;
                }
            }
            """.trimIndent(),
        )
    }

    fun testDoesNotInlineWhenReturnUsesVariableInExpression() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                Demo copy() {
                    return null;
                }

                static Demo make() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                    return d.copy();
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        // `return d.copy();` is not a plain `return d;`, so the local stays.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                Demo copy() {
                    return null;
                }

                static Demo make() {
                    Demo d = Demo.builder().a(1).b("x").build();
                    return d.copy();
                }
            }
            """.trimIndent(),
        )
    }

    fun testDoesNotInlineWhenCommentSitsBeforeReturn() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    d.setB("x");
                    // keep this around
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(settersIntention))

        // A comment between the declaration and the return would be orphaned by the inline, so the
        // local is left in place.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                void setA(int a) {}
                void setB(String b) {}

                static Demo make() {
                    Demo d = Demo.builder().a(1).b("x").build();
                    // keep this around
                    return d;
                }
            }
            """.trimIndent(),
        )
    }

    fun testWholeFileActionInlinesReturnedConstructor() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}

                static Demo make() {
                    Demo d = new Demo(1, "x");
                    return d;
                }
            }
            """.trimIndent(),
        )

        myFixture.testAction(ConvertToBuilderAction())

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }
}
