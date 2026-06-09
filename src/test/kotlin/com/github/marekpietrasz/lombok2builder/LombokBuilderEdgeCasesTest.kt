package com.github.marekpietrasz.lombok2builder

class LombokBuilderEdgeCasesTest : LombokBuilderTestCase() {

    private val constructorIntention = "Convert constructor to builder"
    private val setterIntention = "Convert setters to builder"

    fun testVarargsConstructorNotConverted() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int[] xs;

                Demo(int... xs) {}

                static Demo make() {
                    return new De<caret>mo(1, 2, 3);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(constructorIntention))
    }

    fun testNonIsBooleanFieldUsesPlainName() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                boolean active;
                String name;

                void setActive(boolean active) {}
                void setName(String name) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setActive(true);
                    d.setName("x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                boolean active;
                String name;

                void setActive(boolean active) {}
                void setName(String name) {}

                static void use() {
                    Demo d = Demo.builder().active(true).name("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testSuperBuilderIsRecognized() {
        myFixture.addClass("package lombok.experimental;\npublic @interface SuperBuilder {}")
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.experimental.SuperBuilder;

            @SuperBuilder
            class Demo {
                int a;

                void setA(int a) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        myFixture.checkResult(
            """
            import lombok.experimental.SuperBuilder;

            @SuperBuilder
            class Demo {
                int a;

                void setA(int a) {}

                static void use() {
                    Demo d = Demo.builder().a(1).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testBuilderOnStaticMethodIsRecognized() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;

                @Builder
                static Demo create(int a) {
                    return null;
                }

                void setA(int a) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;

                @Builder
                static Demo create(int a) {
                    return null;
                }

                void setA(int a) {}

                static void use() {
                    Demo d = Demo.builder().a(1).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNestedClassConstructorKeepsOuterQualifier() {
        setMultiline(false)
        // The nested @Builder class lives in Outer; the usage is in a different class where `Inner`
        // is reachable only as `Outer.Inner`. The `Outer.` qualifier must survive the conversion —
        // emitting a bare `Inner.builder()` would not compile.
        myFixture.addClass(
            """
            package demo;

            import lombok.Builder;

            public class Outer {
                @Builder
                public static class Inner {
                    int a;
                    String b;

                    public Inner(int a, String b) {}
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Client.java",
            """
            package demo;

            class Client {
                static Outer.Inner make() {
                    return new Outer.In<caret>ner(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(constructorIntention))

        myFixture.checkResult(
            """
            package demo;

            class Client {
                static Outer.Inner make() {
                    return Outer.Inner.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNestedClassSetterKeepsOuterQualifier() {
        setMultiline(false)
        myFixture.addClass(
            """
            package demo;

            import lombok.Builder;

            public class Outer {
                @Builder
                public static class Inner {
                    int a;

                    public void setA(int a) {}
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Client.java",
            """
            package demo;

            class Client {
                static void use() {
                    Outer.Inner d = new Outer.In<caret>ner();
                    d.setA(1);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        myFixture.checkResult(
            """
            package demo;

            class Client {
                static void use() {
                    Outer.Inner d = Outer.Inner.builder().a(1).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testMultiVariableDeclarationNotConverted() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;

                void setA(int a) {}

                static void use() {
                    Demo x<caret> = new Demo(), y = new Demo();
                    x.setA(1);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(setterIntention))
    }

    fun testChainStopsAtInterveningStatement() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                int b;

                void setA(int a) {}
                void setB(int b) {}

                static void use() {
                    Demo d = new De<caret>mo();
                    d.setA(1);
                    System.out.println();
                    d.setB(2);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        // Only the contiguous setters before the println are folded; the rest is left as-is.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                int b;

                void setA(int a) {}
                void setB(int b) {}

                static void use() {
                    Demo d = Demo.builder().a(1).build();
                    System.out.println();
                    d.setB(2);
                }
            }
            """.trimIndent(),
        )
    }
}
