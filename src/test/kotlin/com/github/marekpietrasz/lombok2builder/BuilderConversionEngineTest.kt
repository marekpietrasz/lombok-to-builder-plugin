package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiJavaFile

class BuilderConversionEngineTest : LombokBuilderTestCase() {

    fun testConvertsWholeFileAndLeavesNonBuilderUsages() {
        setMultiline(false)
        myFixture.configureByText(
            "Use.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo() {}
                Demo(int a, String b) {}
                void setA(int a) {}
                void setB(String b) {}
            }

            class Plain {
                Plain(int a) {}
            }

            class Use {
                void chain() {
                    Demo d = new Demo();
                    d.setA(1);
                    d.setB("x");
                }

                Demo ctor() {
                    return new Demo(2, "y");
                }

                Plain keep() {
                    return new Plain(3);
                }
            }
            """.trimIndent(),
        )

        val file = myFixture.file as PsiJavaFile
        val converted = WriteCommandAction.runWriteCommandAction<Int>(project) {
            BuilderConversionEngine.convertFile(file, null)
        }

        assertEquals(2, converted)
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo() {}
                Demo(int a, String b) {}
                void setA(int a) {}
                void setB(String b) {}
            }

            class Plain {
                Plain(int a) {}
            }

            class Use {
                void chain() {
                    Demo d = Demo.builder().a(1).b("x").build();
                }

                Demo ctor() {
                    return Demo.builder().a(2).b("y").build();
                }

                Plain keep() {
                    return new Plain(3);
                }
            }
            """.trimIndent(),
        )
    }

    fun testConvertsNestedConstructorsInnermostFirst() {
        setMultiline(false)
        myFixture.configureByText(
            "Use.java",
            """
            import lombok.Builder;

            @Builder
            class Inner {
                int x;
                int y;

                Inner(int x, int y) {}
            }

            @Builder
            class Outer {
                Inner inner;

                Outer(Inner inner) {}
            }

            class Use {
                Outer make() {
                    return new Outer(new Inner(1, 2));
                }
            }
            """.trimIndent(),
        )

        val file = myFixture.file as PsiJavaFile
        val converted = WriteCommandAction.runWriteCommandAction<Int>(project) {
            BuilderConversionEngine.convertFile(file, null)
        }

        assertEquals(2, converted)
        assertTrue(
            file.text.contains("Outer.builder().inner(Inner.builder().x(1).y(2).build()).build()"),
        )
    }

    fun testMultilineConversionInBatch() {
        // multiline is the default; verify the engine path also produces a chopped chain.
        setMultiline(true)
        myFixture.configureByText(
            "Use.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}
            }

            class Use {
                Demo make() {
                    return new Demo(1, "x");
                }
            }
            """.trimIndent(),
        )

        val file = myFixture.file as PsiJavaFile
        WriteCommandAction.runWriteCommandAction(project) {
            BuilderConversionEngine.convertFile(file, null)
        }

        // Indentation-agnostic: each call sits on its own line.
        val withoutSpaces = file.text.replace(" ", "")
        assertTrue(withoutSpaces.contains("Demo.builder()\n.a(1)\n.b(\"x\")\n.build()"))
    }

    fun testSelfReferencingSetterDeferredInBatch() {
        setMultiline(false)
        myFixture.configureByText(
            "Use.java",
            """
            import lombok.Builder;

            @Builder
            class Node {
                String name;
                Node self;

                void setName(String name) {}
                void setSelf(Node self) {}
            }

            class Use {
                void build() {
                    Node n = new Node();
                    n.setName("x");
                    n.setSelf(n);
                }
            }
            """.trimIndent(),
        )

        val file = myFixture.file as PsiJavaFile
        val converted = WriteCommandAction.runWriteCommandAction<Int>(project) {
            BuilderConversionEngine.convertFile(file, null)
        }

        assertEquals(1, converted)
        assertTrue("folded setters in builder", file.text.contains("Node n = Node.builder().name(\"x\").build();"))
        assertTrue("self-reference kept as trailing setter", file.text.contains("n.setSelf(n);"))
    }

    fun testMinValuesGatesConstructorsButNotSettersInBatch() {
        setMultiline(false)
        setMinValues(3)
        myFixture.configureByText(
            "Use.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo() {}
                Demo(int a, String b) {}
                void setA(int a) {}
            }

            class Use {
                Demo ctorBelowThreshold() {
                    return new Demo(1, "x");
                }

                void setterChain() {
                    Demo d = new Demo();
                    d.setA(2);
                }
            }
            """.trimIndent(),
        )

        val file = myFixture.file as PsiJavaFile
        val converted = WriteCommandAction.runWriteCommandAction<Int>(project) {
            BuilderConversionEngine.convertFile(file, null)
        }

        // The 2-arg constructor is below the threshold (3) and is skipped; the setter chain converts.
        assertEquals(1, converted)
        assertTrue("constructor left alone", file.text.contains("return new Demo(1, \"x\");"))
        assertTrue("setter chain converted", file.text.contains("Demo d = Demo.builder().a(2).build();"))
    }
}
