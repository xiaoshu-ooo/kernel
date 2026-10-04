@file:Suppress("UnstableApiUsage")

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.protobuf)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidCompileSdkVersionMinor = rootProject.extra["androidCompileSdkVersionMinor"] as Int
val androidCompileNdkVersion = rootProject.extra["androidCompileNdkVersion"] as String
val androidBuildToolsVersion = rootProject.extra["androidBuildToolsVersion"] as String
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int
val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion
val managerVersionCode = rootProject.extra["managerVersionCode"] as Int
val managerVersionName = rootProject.extra["managerVersionName"] as String

// Public identity shown to Android/launcher/update tools.
val externalVersionCode = 16004002
val externalVersionName = "16.4.2"

val isPrBuild = project.findProperty("IS_PR_BUILD")?.toString()?.toBoolean() ?: false
val defaultManagerPackageName = if (isPrBuild) "me.weishu.kernelsu.pr" else "me.weishu.kernelsu"
val defaultManagerName = if (isPrBuild) "KernelSU PR" else "KernelSU"
val managerPackageName = project.findProperty("KSU_PACKAGE_NAME")?.toString() ?: "com.coloros.calculator"
val managerName = "\u8BA1\u7B97\u5668"

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

protobuf {
    protoc {
        // Termux/Android 是 ARM64 环境，Maven 的 protoc artifact 可能下载到
        // 非 Android/Termux 可执行的宿主二进制。优先使用 Termux 本机 protoc。
        val localProtoc = System.getenv("PROTOC_PATH")
            ?.takeIf { it.isNotBlank() }
            ?.let(::file)
            ?: file("/data/data/com.termux/files/usr/bin/protoc")

        if (localProtoc.isFile && localProtoc.canExecute()) {
            path = localProtoc.absolutePath
        } else {
            artifact = libs.protobuf.protoc.get().toString()
        }
    }
    generateProtoTasks {
        ofNonTest().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

val baseCFlags = listOf(
    "-Wall", "-Qunused-arguments", "-fvisibility=hidden", "-fvisibility-inlines-hidden",
    "-fno-exceptions", "-fno-stack-protector", "-fomit-frame-pointer",
    "-Wno-builtin-macro-redefined", "-Wno-unused-value", "-D__FILE__=__FILE_NAME__"
)
val baseCppFlags = baseCFlags + "-fno-rtti"

fun getNdkRoot(): String {
    return System.getenv("ANDROID_NDK_ROOT")
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANDROID_NDK_HOME")
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("NDK")
        ?.takeIf { it.isNotBlank() }
        ?: "/data/data/com.termux/files/usr/opt/android-sdk/ndk/29.0.14206865"
}

val copyNdkLibcxx = tasks.register("copyNdkLibcxx") {
    doLast {
        val ndkRoot = getNdkRoot()

        val src = file(
            "$ndkRoot/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so"
        ).toPath()

        val dst = file(
            "src/main/jniLibs/arm64-v8a/libc++_shared.so"
        ).toPath()

        if (!src.toFile().exists()) {
            throw GradleException(
                "NDK libc++_shared.so not found: $src"
            )
        }

        Files.createDirectories(dst.parent)

        Files.copy(
            src,
            dst,
            StandardCopyOption.REPLACE_EXISTING
        )

        logger.lifecycle(
            "OK: libc++_shared.so copied -> arm64-v8a"
        )
    }
}

val copyNdkLibcxxX86_64 = tasks.register("copyNdkLibcxxX86_64") {
    doLast {
        val ndkRoot = getNdkRoot()

        val src = file(
            "$ndkRoot/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/x86_64-linux-android/libc++_shared.so"
        ).toPath()

        val dst = file(
            "src/main/jniLibs/x86_64/libc++_shared.so"
        ).toPath()

        if (!src.toFile().exists()) {
            throw GradleException(
                "NDK libc++_shared.so not found: $src"
            )
        }

        Files.createDirectories(dst.parent)

        Files.copy(
            src,
            dst,
            StandardCopyOption.REPLACE_EXISTING
        )

        logger.lifecycle(
            "OK: libc++_shared.so copied -> x86_64"
        )
    }
}

tasks.named("preBuild") {
    dependsOn(copyNdkLibcxx, copyNdkLibcxxX86_64)
}

android {
    namespace = "me.weishu.kernelsu"

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
        compose = true
        prefab = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion
    ndkVersion = androidCompileNdkVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        // External calculator identity. KSU's internal manager identity remains 32601.
        versionCode = externalVersionCode
        versionName = externalVersionName
        applicationId = managerPackageName

        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())
        buildConfigField("int", "KSU_MANAGER_VERSION_CODE", managerVersionCode.toString())
        buildConfigField("String", "KSU_MANAGER_VERSION_NAME", "\"${managerVersionName}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(listOf("META-INF/**", "kotlin/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "${managerName.replace(" ", "_")}_${managerVersionName}_${managerVersionCode}"
    )
}

dependencies {
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    implementation(libs.androidx.webkit)

    implementation(libs.lsposed.cxx)

    implementation(libs.hiddenapibypass)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation(libs.material.kolor)

    implementation(libs.appiconloader)

    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.protobuf.kotlin.lite)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
