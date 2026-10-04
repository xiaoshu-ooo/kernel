package me.weishu.kernelsu.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

object ModuleBackgroundStore {
    private const val PREFS = "module_backgrounds"
    private const val KEY_PREFIX = "bg_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context, moduleId: String): String =
        prefs(context).getString(KEY_PREFIX + moduleId, "") ?: ""

    fun set(context: Context, moduleId: String, uri: String) {
        prefs(context).edit { putString(KEY_PREFIX + moduleId, uri) }
    }

    fun clear(context: Context, moduleId: String) {
        prefs(context).edit { remove(KEY_PREFIX + moduleId) }
    }
}

@Composable
fun ModuleBackgroundLayer(
    moduleId: String,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.30f,
) {
    val context = LocalContext.current
    val uriString = remember(moduleId) { ModuleBackgroundStore.get(context, moduleId) }
    var bitmap by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = if (uriString.isBlank()) null else runCatching {
            decodeModuleBitmap(context, Uri.parse(uriString))
        }.getOrNull()
    }
    if (bitmap != null) {
        Box(modifier = modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlayAlpha))
            )
        }
    }
}

private fun decodeModuleBitmap(context: Context, uri: Uri): android.graphics.Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { input ->
        if (input == null) return null
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val dm = context.resources.displayMetrics
    val targetW = (dm.widthPixels * 1.5f).toInt().coerceAtLeast(720)
    val targetH = (dm.heightPixels * 0.55f).toInt().coerceAtLeast(480)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri).use { input ->
        if (input == null) null else BitmapFactory.decodeStream(input, null, opts)
    }
}
