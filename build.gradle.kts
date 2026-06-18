plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.3.20"
    `maven-publish`
}

group = "io.github.master_bw3"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

    }
    js(IR) {
        browser()
        nodejs()

        binaries.library()
        useEsModules()
        generateTypeScriptDefinitions()

        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }

        compilations["main"].packageJson {
            customField("types", "kotlin/KEndec.d.ts")
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.10.2")
                implementation("com.benasher44:uuid:0.8.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("io.github.gciatto:kt-math:0.10.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.google.code.gson:gson:2.10.1")
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "transFishMaven"
            url = uri("https://maven.trans.fish/releases")

            credentials(PasswordCredentials::class)

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
