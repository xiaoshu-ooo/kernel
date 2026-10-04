package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.BuildConfig

@Immutable
data class ManagerVersion(
    val versionName: String,
    val versionCode: Long
)

@Immutable
data class SystemInfo(
    val kernelVersion: String,
    val managerVersion: String,
    val deviceModel: String,
    val fingerprint: String,
    val selinuxStatus: String,
    val seccompStatus: Int
)

fun getManagerVersion(context: Context): ManagerVersion {
    // KSU internal identity is intentionally independent from the public calculator version.
    return ManagerVersion(
        versionName = BuildConfig.KSU_MANAGER_VERSION_NAME,
        versionCode = BuildConfig.KSU_MANAGER_VERSION_CODE.toLong(),
    )
}
