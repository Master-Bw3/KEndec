plugins {
    kotlin("multiplatform") version "2.1.0"
}

group = "tree.maple"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
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

        dependencies {
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
        val jvmMain by getting {
            dependencies {
                implementation("com.google.code.gson:gson:2.10.1")
            }
        }
    }
}
