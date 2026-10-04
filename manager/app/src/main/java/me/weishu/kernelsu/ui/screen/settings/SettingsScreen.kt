package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Composable
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.ui.component.ModuleBackgroundStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val viewModel = viewModel<SettingsViewModel>()
    val context = LocalContext.current
    var backgroundPickTarget by remember { mutableStateOf(0) }
    var moduleBackgroundTarget by remember { mutableStateOf<String?>(null) }
    var modules by remember { mutableStateOf<List<Module>>(emptyList()) }
    val moduleScanScope = rememberCoroutineScope()
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val moduleId = moduleBackgroundTarget
        if (moduleId != null) {
            ModuleBackgroundStore.set(context, moduleId, uri.toString())
            moduleBackgroundTarget = null
        } else if (backgroundPickTarget == 0) viewModel.setGlobalBackgroundUri(uri.toString())
        else viewModel.setStatusBackgroundUri(uri.toString())
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        modules = withContext(Dispatchers.IO) {
            ModuleRepositoryImpl().getModules().getOrDefault(emptyList())
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        // 每次回到设置页都重新扫描模块，保证安装/卸载模块后列表立即同步。
        moduleScanScope.launch {
            modules = withContext(Dispatchers.IO) {
                ModuleRepositoryImpl().getModules().getOrDefault(emptyList())
            }
        }
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = { index ->
            viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
        },
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onPickGlobalBackground = { backgroundPickTarget = 0; backgroundPicker.launch(arrayOf("image/*")) },
        onPickStatusBackground = { backgroundPickTarget = 1; backgroundPicker.launch(arrayOf("image/*")) },
        onSetModuleBackground = { moduleId -> moduleBackgroundTarget = moduleId; backgroundPicker.launch(arrayOf("image/*")) },
        onClearModuleBackground = { moduleId -> ModuleBackgroundStore.clear(context, moduleId) },
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetUseSoftReboot = viewModel::setUseSoftReboot,
        onOpenAbout = { navigator.push(Route.About) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions, bottomInnerPadding, modules)
        UiMode.Material -> SettingPagerMaterial(uiState, actions, bottomInnerPadding, modules)
    }
}
