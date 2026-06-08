package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.TestActionEvent

class ConvertToBuilderActionTest : LombokBuilderTestCase() {

    private val action = ConvertToBuilderAction()

    fun testConvertsWholeFileFromEditor() {
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
                void setA(int a) {}

                static void use() {
                    Demo first = new Demo(1, "x");
                    Demo second = new Demo();
                    second.setA(2);
                }
            }
            """.trimIndent(),
        )

        myFixture.testAction(action)

        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}
                void setA(int a) {}

                static void use() {
                    Demo first = Demo.builder().a(1).b("x").build();
                    Demo second = Demo.builder().a(2).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testConvertsOnlySelection() {
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

                static void use() {
                    <selection>Demo first = new Demo(1, "x");</selection>
                    Demo second = new Demo(3, "y");
                }
            }
            """.trimIndent(),
        )

        myFixture.testAction(action)

        // Only the selected usage is converted; the second one is left untouched.
        myFixture.checkResult(
            """
            import lombok.Builder;

            @Builder
            class Demo {
                int a;
                String b;

                Demo(int a, String b) {}

                static void use() {
                    Demo first = Demo.builder().a(1).b("x").build();
                    Demo second = new Demo(3, "y");
                }
            }
            """.trimIndent(),
        )
    }

    fun testConvertsAllJavaFilesUnderDirectory() {
        setMultiline(false)
        val classText = { name: String ->
            """
            import lombok.Builder;

            @Builder
            class $name {
                int a;
                String b;

                $name(int a, String b) {}

                static $name make() {
                    return new $name(1, "x");
                }
            }
            """.trimIndent()
        }
        val a = myFixture.addFileToProject("pkg/A.java", classText("A")) as PsiJavaFile
        val b = myFixture.addFileToProject("pkg/B.java", classText("B")) as PsiJavaFile
        val directory = a.virtualFile.parent

        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(directory))
            .build()
        val event = TestActionEvent.createTestEvent(action, context)

        action.update(event)
        assertTrue("Action should be enabled on a directory", event.presentation.isEnabledAndVisible)

        action.actionPerformed(event)

        assertTrue("A.java should be converted", a.text.contains("A.builder().a(1).b(\"x\").build()"))
        assertTrue("B.java should be converted", b.text.contains("B.builder().a(1).b(\"x\").build()"))
    }

    fun testDisabledWithoutJavaContext() {
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
        val event = TestActionEvent.createTestEvent(action, context)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }
}
