package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiJavaFile

/**
 * Nested-builder back-references: `A` holds `B`, `B` holds `C`, and both `B` and `C` point back at
 * `A`. The back-reference is set on `A` via `a.setB(new B(a, new C(a)))`, so its argument references
 * the variable being built. Folding that into A's builder would read `a` before it is assigned, so
 * the setter is deferred and kept after — the nested `B`/`C` builders then reference the assigned
 * `a` safely. This is the case that would not compile without self-reference handling.
 */
class NestedBuilderBackReferenceTest : LombokBuilderTestCase() {

    private val classes =
        """
        import lombok.Builder;

        @Builder
        class A {
            String name;
            B b;
            void setName(String v) {}
            void setB(B v) {}
        }

        class B {
            A a; C c;
            @Builder
            B(A a, C c) {}
        }

        class C {
            A a;
            @Builder
            C(A a) {}
        }
        """.trimIndent()

    private fun convertWholeFile() {
        val file = myFixture.file as PsiJavaFile
        WriteCommandAction.runWriteCommandAction(project) {
            BuilderConversionEngine.convertFile(file, null)
        }
    }

    fun testNestedBackReferenceKeptAsSetterWithFoldableField() {
        setMultiline(false)
        myFixture.configureByText(
            "Use.java",
            """
            $classes

            class Use {
                void make() {
                    A a = new A();
                    a.setName("root");
                    a.setB(new B(a, new C(a)));
                }
            }
            """.trimIndent(),
        )

        convertWholeFile()

        // A's own field folds into its builder; the back-reference setB is kept after, with the
        // nested B/C builders (which reference the now-assigned `a`) inside it.
        myFixture.checkResult(
            """
            $classes

            class Use {
                void make() {
                    A a = A.builder().name("root").build();
                    a.setB(B.builder().a(a).c(C.builder().a(a).build()).build());
                }
            }
            """.trimIndent(),
        )
    }

    fun testNestedBackReferenceLeavesOuterUnconvertedWhenNothingElseToFold() {
        setMultiline(false)
        // A has nothing foldable (its only setter self-references), so A stays `new A()`; the nested
        // constructors inside the deferred setter still convert to builders.
        myFixture.configureByText(
            "Use.java",
            """
            $classes

            class Use {
                void make() {
                    A a = new A();
                    a.setB(new B(a, new C(a)));
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
                    A a = new A();
                    a.setB(B.builder().a(a).c(C.builder().a(a).build()).build());
                }
            }
            """.trimIndent(),
        )
    }
}
