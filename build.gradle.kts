import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.1.0"
}

group = "tree.maple"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

    }
    js {
        browser()
        nodejs()

        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }

        dependencies {
            npm("prelude-ts", "1.0.6")
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.10.2")
                implementation(("com.benasher44:uuid:0.8.4"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}