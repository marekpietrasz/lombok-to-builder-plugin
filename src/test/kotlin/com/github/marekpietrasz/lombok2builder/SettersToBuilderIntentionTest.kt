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

            class Demo {
                int id;
                String name;

                @Builder
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

            class Demo {
                int id;
                String name;

                @Builder
                Demo(int id) {}
                void setName(String name) {}

                static void use() {
                    Demo d = Demo.builder().id(5).name("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableForHandWrittenConstructorWithArgsPlusSetters() {
        // The chain starts with `new Demo(5)` calling a hand-written constructor (not @Builder-
        // annotated); folding it would drop whatever logic that constructor runs, so by default the
        // whole block is left as constructor + setters.
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

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
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

    fun testSelfReferencingSetterIsDeferred() {
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

                static void use() {
                    Node n = new No<caret>de();
                    n.setName("x");
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        // setSelf references the variable being built, so it can't go in the builder: the rest is
        // folded and setSelf is kept right after the builder, where `n` is already assigned.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Node {
                String name;
                Node self;

                void setName(String name) {}
                void setSelf(Node self) {}

                static void use() {
                    Node n = Node.builder().name("x").build();
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )
    }

    fun testSelfReferenceNestedInExpressionIsDeferred() {
        setMultiline(false)
        // The argument references the variable indirectly (inside a call), modelling a child that
        // points back at its parent. It must still be detected and deferred.
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Parent {
                String name;
                Object child;

                void setName(String name) {}
                void setChild(Object child) {}

                static Object wrap(Parent p) {
                    return null;
                }

                static void use() {
                    Parent p = new Par<caret>ent();
                    p.setName("p");
                    p.setChild(wrap(p));
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Parent {
                String name;
                Object child;

                void setName(String name) {}
                void setChild(Object child) {}

                static Object wrap(Parent p) {
                    return null;
                }

                static void use() {
                    Parent p = Parent.builder().name("p").build();
                    p.setChild(wrap(p));
                }
            }
            """.trimIndent(),
        )
    }

    fun testDeferredSetterKeepsRemainingSettersInBuilder() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Node {
                int a;
                int b;
                Node self;

                void setA(int a) {}
                void setB(int b) {}
                void setSelf(Node self) {}

                static void use() {
                    Node n = new No<caret>de();
                    n.setA(1);
                    n.setSelf(n);
                    n.setB(2);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        // a and b fold into the builder (even though b follows the self-reference); only setSelf stays.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Node {
                int a;
                int b;
                Node self;

                void setA(int a) {}
                void setB(int b) {}
                void setSelf(Node self) {}

                static void use() {
                    Node n = Node.builder().a(1).b(2).build();
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )
    }

    fun testSelfReferencingSetterDeferredMultiline() {
        // multiline is the default; the deferred setter sits on its own line after the builder.
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

                static void use() {
                    Node n = new No<caret>de();
                    n.setName("x");
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Node {
                String name;
                Node self;

                void setName(String name) {}
                void setSelf(Node self) {}

                static void use() {
                    Node n = Node.builder()
                            .name("x")
                            .build();
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )
    }

    fun testSelfReferencingChainSkippedWhenDeferDisabled() {
        setDeferSelfReferencingSetters(false)
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

                static void use() {
                    Node n = new No<caret>de();
                    n.setName("x");
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )

        // With deferral off, a chain containing a self-reference is left entirely as setters.
        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    fun testChainWithOnlySelfReferenceIsNotConverted() {
        // Nothing can be folded into the builder (the lone setter self-references), so there's no
        // point converting — the intention isn't offered even with deferral on (the default).
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Node {
                Node self;

                void setSelf(Node self) {}

                static void use() {
                    Node n = new No<caret>de();
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }
}
