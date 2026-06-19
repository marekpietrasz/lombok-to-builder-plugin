package com.github.marekpietrasz.lombok2builder

class ConstructorToBuilderIntentionTest : LombokBuilderTestCase() {

    private val intentionName = "Convert constructor to builder"

    fun testConvertsConstructorSingleLine() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testConvertsConstructorMultilineByDefault() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

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

    fun testSkipsNullValuesByDefault() {
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;

                @Builder
                Demo(int a, String b, String c) {}

                static Demo make() {
                    return new De<caret>mo(1, null, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;

                @Builder
                Demo(int a, String b, String c) {}

                static Demo make() {
                    return Demo.builder().a(1).c("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testKeepsNullValuesWhenSkipDisabled() {
        setMultiline(false)
        setSkipNullValues(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;

                @Builder
                Demo(int a, String b, String c) {}

                static Demo make() {
                    return new De<caret>mo(1, null, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;

                @Builder
                Demo(int a, String b, String c) {}

                static Demo make() {
                    return Demo.builder().a(1).b(null).c("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableBelowMinValues() {
        setMinValues(3)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    /** The threshold counts constructor arguments, not the values that survive null-skipping, so a
     *  long, mostly-null call (the case a builder reads best for) still converts. */
    fun testConvertsMostlyNullConstructorAboveArgumentThreshold() {
        setMinValues(3)
        setMultiline(false)
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;
                String d;
                String e;

                @Builder
                Demo(int a, String b, String c, String d, String e) {}

                static Demo make() {
                    return new De<caret>mo(0, null, null, null, null);
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;
                String c;
                String d;
                String e;

                @Builder
                Demo(int a, String b, String c, String d, String e) {}

                static Demo make() {
                    return Demo.builder().a(0).build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableWhenParameterDoesNotMatchField() {
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            @Builder
            class Demo {
                String feeCategory;

                // Hand-written constructor: parameter `category` does not match field `feeCategory`.
                Demo(String category) {}

                static Demo make() {
                    return new De<caret>mo("x");
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    fun testNotAvailableForHandWrittenConstructorByDefault() {
        // @Builder on the class, but the call resolves to a hand-written constructor (params match
        // fields, so the only reason to skip is that it's hand-written and may carry logic).
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
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }

    fun testConvertsHandWrittenConstructorWhenEnabled() {
        setMultiline(false)
        setConvertHandWrittenConstructors(true)
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
                    return new De<caret>mo(1, "x");
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

    fun testConvertsBuilderAnnotatedConstructorByDefault() {
        setMultiline(false)
        // @Builder sits on the constructor itself, so it's the builder's own source: converting its
        // call is safe and stays available even with the hand-written default.
        myFixture.configureByText(
            "Demo.java",
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return new De<caret>mo(1, "x");
                }
            }
            """.trimIndent(),
        )

        myFixture.launchAction(myFixture.findSingleIntention(intentionName))

        myFixture.checkResult(
            """
            import lombok.Builder;

            class Demo {
                int a;
                String b;

                @Builder
                Demo(int a, String b) {}

                static Demo make() {
                    return Demo.builder().a(1).b("x").build();
                }
            }
            """.trimIndent(),
        )
    }

    fun testNotAvailableWithoutBuilder() {
        myFixture.configureByText(
            "Plain.java",
            """
            class Plain {
                int a;

                Plain(int a) {}

                static Plain make() {
                    return new Pla<caret>in(1);
                }
            }
            """.trimIndent(),
        )

        assertEmpty(myFixture.filterAvailableIntentions(intentionName))
    }
}
