import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "eu.tilo"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("tilo.perf.items", System.getProperty("tilo.perf.items") ?: "100000")
    systemProperty("tilo.perf.queries", System.getProperty("tilo.perf.queries") ?: "2000")
}
