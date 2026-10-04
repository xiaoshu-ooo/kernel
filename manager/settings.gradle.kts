@file:Suppress("UnstableApiUsage")

import org.gradle.api.initialization.resolve.RepositoriesMode

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Termux may inject mirror repositories through ~/.gradle/init.gradle.
    // Resolve the Android Gradle Plugin directly to its real Google Maven
    // artifact instead of relying on the plugin marker artifact.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        // Prefer official endpoints first. If Termux TLS/network access fails,
        // Gradle can continue to the fallback mirrors below.
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    // Keep all dependency repositories here.  LibSU is JitPack-only; using
    // exclusiveContent prevents Gradle from resolving its metadata/artifacts
    // from Huawei/Aliyun mirrors.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            content {
                excludeGroupByRegex("com\\.github\\.topjohnwu\\.libsu(\\..*)?")
                excludeGroupByRegex("com\\.android.*")
                excludeGroupByRegex("androidx\\..*")
                excludeGroupByRegex("com\\.google\\.android.*")
            }
        }

        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.android.*")
                excludeGroupByRegex("com\\.github\\.topjohnwu\\.libsu(\\..*)?")
            }
        }

        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                excludeGroupByRegex("com\\.github\\.topjohnwu\\.libsu(\\..*)?")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "JitPackLibSU"
                    url = uri("https://jitpack.io")
                }
            }
            filter {
                includeGroupByRegex("com\\.github\\.topjohnwu\\.libsu(\\..*)?")
            }
        }
    }
}

rootProject.name = "FolKernelSU"
include(":app")
