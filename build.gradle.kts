import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.marekpietrasz"
version = "0.2.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // The IDE we build & test against. Bundled "java" plugin gives us Java PSI.
        intellijIdeaCommunity("2025.1.7.1")
        bundledPlugin("com.intellij.java")
        // Lombok ships only with IDEA Ultimate, so pull it from the Marketplace. This makes it
        // available in the runIde sandbox (and tests) without adding a <depends> in plugin.xml,
        // so the published plugin does NOT require Lombok to be installed.
        // ("Lombook Plugin" is the plugin's real (historically misspelled) Marketplace id.)
        plugin("Lombook Plugin", "251.29188.36")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // Built against 2025.1, but the APIs used exist since 2024.2 and it was verified
            // running on 2024.2.5, so we keep the floor at 242. The plugin uses only long-stable
            // APIs, so leave the upper bound open to stay compatible with newer IDEs (261+).
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    // Marketplace requires signed plugins. Generate a key/cert once (see PUBLISHING.md) and supply
    // these via environment variables in CI; locally they are simply absent and signing is skipped.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        // Marketplace token from https://plugins.jetbrains.com/author/me/tokens
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    // The 2025.1 platform runs on JBR 21 and expects plugins compiled to 21.
    // Auto-provisioned via foojay if absent.
    jvmToolchain(21)
}

tasks {
    test {
        useJUnit()
    }
}
