plugins {
    // Lets Gradle auto-provision a JDK 17 toolchain (host JDK can be newer).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lombok-to-builder-plugin"
