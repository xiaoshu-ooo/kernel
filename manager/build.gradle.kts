plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
val envCompileSdk = (System.getenv("ANDROID_COMPILE_SDK_VERSION") ?: System.getenv("KSU_COMPILE_SDK"))?.toIntOrNull() ?: 37
val envTargetSdk = (System.getenv("ANDROID_TARGET_SDK_VERSION") ?: System.getenv("KSU_TARGET_SDK"))?.toIntOrNull() ?: envCompileSdk
extra["androidTargetSdkVersion"] = envTargetSdk
extra["androidCompileSdkVersion"] = envCompileSdk
extra["androidCompileSdkVersionMinor"] = 0
val envBuildTools = System.getenv("ANDROID_BUILD_TOOLS_VERSION") ?: System.getenv("KSU_BUILD_TOOLS") ?: "37.0.0"
extra["androidBuildToolsVersion"] = envBuildTools
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = 32601
extra["managerVersionName"] = "32601"

fun getGitCommitCount(): Int {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        process.waitFor()
        output.toIntOrNull() ?: 0
    } catch (_: Exception) {
        0
    }
}

fun getGitDescribe(): String {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        process.waitFor()
        output.ifBlank { "32601" }
    } catch (_: Exception) {
        "32601"
    }
}

fun getVersionCode(): Int = 32601

fun getVersionName(): String = "32601"
