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
}
