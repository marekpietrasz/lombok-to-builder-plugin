import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.marekpietrasz"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // The IDE we build & test against. Bundled "java" plugin gives us Java PSI.
        intellijIdeaCommunity("2024.2.5")
        bundledPlugin("com.intellij.java")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "252.*"
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
    // The 2024.2 platform runs on JBR 21 and expects plugins compiled to 21.
    // Auto-provisioned via foojay if absent.
    jvmToolchain(21)
}

tasks {
    test {
        useJUnit()
    }
}
