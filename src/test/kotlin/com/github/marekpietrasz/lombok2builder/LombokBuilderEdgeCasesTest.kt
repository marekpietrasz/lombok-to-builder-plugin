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

    /** An all-null constructor would yield an empty `Foo.builder().build()`, which adds nothing over the
     *  `new Foo()` it came from, so the call is left unconverted (no intention offered). */
    fun testAllNullConstructorNotConverted() {
        setMinValues(3)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                String a;
                String b;
                String c;

                @Builder
                Demo(String a, String b, String c) {}

                static Demo make() {
                    return new De<caret>mo(null, null, null);
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
                public static class Inner {
                    int a;
                    String b;

                    @Builder
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

    fun testGenericClassConstructorDropsTypeArguments() {
        setMultiline(false)
        // The builder receiver must be the raw `Box`, not `Box<String>` — `Box<String>.builder()`
        // is not valid Java. (Lombok generates a static `builder()` on the raw type.)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Box<T> {
                T value;
                String label;

                @Builder
                Box(T value, String label) {}

                static Box<String> make() {
                    return new B<caret>ox<String>("v", "l");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(constructorIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Box<T> {
                T value;
                String label;

                @Builder
                Box(T value, String label) {}

                static Box<String> make() {
                    return Box.builder().value("v").label("l").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testGenericClassSetterChainDropsTypeArguments() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Box<T> {
                T value;

                void setValue(T value) {}

                static void use() {
                    Box<String> b = new B<caret>ox<String>();
                    b.setValue("v");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(setterIntention))

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Box<T> {
                T value;

                void setValue(T value) {}

                static void use() {
                    Box<String> b = Box.builder().value("v").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testFullyQualifiedConstructorKeepsQualifier() {
        setMultiline(false)
        // The @Builder class is referenced by its fully-qualified name at the `new` site; the
        // generated receiver must keep that qualifier rather than collapse to the simple name.
        myFixture.addClass(
            """
            package demo;

            import lombok.Builder;

            public class Widget {
                int a;
                String b;

                @Builder
                public Widget(int a, String b) {}
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Client.java",
            """
            class Client {
                static demo.Widget make() {
                    return new demo.Wid<caret>get(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(constructorIntention))

        myFixture.checkResult(
            """
            class Client {
                static demo.Widget make() {
                    return demo.Widget.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }
}
