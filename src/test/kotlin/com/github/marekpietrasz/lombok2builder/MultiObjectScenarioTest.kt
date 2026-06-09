package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiJavaFile

/**
 * Multi-object scenarios: three `@Builder` classes where B and C hold a reference to A. A cross-
 * reference to a variable that is already built folds into the builder; only a reference to the
 * variable being built (or a non-contiguous back-reference) stays as a setter.
 */
class MultiObjectScenarioTest : LombokBuilderTestCase() {

    private val classes =
        """
        import lombok.Builder;

        @Builder
        class A {
            int a1; int a2; int a3; int a4; int a5; B b; C c;
            void setA1(int v) {} void setA2(int v) {} void setA3(int v) {}
            void setA4(int v) {} void setA5(int v) {} void setB(B v) {} void setC(C v) {}
        }

        @Builder
        class B {
            int b1; int b2; int b3; int b4; A a;
            void setB1(int v) {} void setB2(int v) {} void setB3(int v) {}
            void setB4(int v) {} void setA(A v) {}
        }

        @Builder
        class C {
            int c1; int c2; int c3; int c4; A a;
            void setC1(int v) {} void setC2(int v) {} void setC3(int v) {}
            void setC4(int v) {} void setA(A v) {}
        }
        """.trimIndent()

    private fun convertWholeFile() {
        val file = myFixture.file as PsiJavaFile
        WriteCommandAction.runWriteCommandAction(project) {
            BuilderConversionEngine.convertFile(file, null)
        }
    }

    fun testCrossReferenceToAlreadyBuiltVariableFolds() {
        setMultiline(false)
        // A is built first; B and C each set A. Because `a` exists by the time B and C are built,
        // setA(a) folds into their builders — no trailing setters are needed.
        myFixture.configureByText(
            "Use.java",
            """
            $classes

            class Use {
                void make() {
                    A a = new A();
                    a.setA1(1); a.setA2(2); a.setA3(3); a.setA4(4); a.setA5(5);
                    B b = new B();
                    b.setB1(1); b.setB2(2); b.setB3(3); b.setB4(4); b.setA(a);
                    C c = new C();
                    c.setC1(1); c.setC2(2); c.setC3(3); c.setC4(4); c.setA(a);
                }
            }
            """.trimIndent(),
        )

        convertWholeFile()

        myFixture.checkResult(
            """
            $classes

            class Use {
                void make() {
                    A a = A.builder().a1(1).a2(2).a3(3).a4(4).a5(5).build();
                    B b = B.builder().b1(1).b2(2).b3(3).b4(4).a(a).build();
                    C c = C.builder().c1(1).c2(2).c3(3).c4(4).a(a).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testParentChildCycleKeepsBackReferencesAsSetters() {
        setMultiline(false)
        // A bidirectional cycle: A points back at B and C, while B and C reference A. All three
        // become builders; A's back-references (after B and C are declared) stay as trailing setters,
        // which is the only form that compiles.
        myFixture.configureByText(
            "Use.java",
            """
            $classes

            class Use {
                void make() {
                    A a = new A();
                    a.setA1(1); a.setA2(2); a.setA3(3);
                    B b = new B();
                    b.setB1(1); b.setB2(2); b.setB3(3); b.setB4(4); b.setA(a);
                    C c = new C();
                    c.setC1(1); c.setC2(2); c.setC3(3); c.setC4(4); c.setA(a);
                    a.setB(b);
                    a.setC(c);
                }
            }
            """.trimIndent(),
        )

        convertWholeFile()

        myFixture.checkResult(
            """
            $classes

            class Use {
                void make() {
                    A a = A.builder().a1(1).a2(2).a3(3).build();
                    B b = B.builder().b1(1).b2(2).b3(3).b4(4).a(a).build();
                    C c = C.builder().c1(1).c2(2).c3(3).c4(4).a(a).build();
                    a.setB(b);
                    a.setC(c);
                }
            }
            """.trimIndent(),
        )
    }
}
