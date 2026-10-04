package me.weishu.kernelsu.ui.component

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R

@Composable
fun GlobalBackground(
    uriString: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = if (uriString.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching { decodeOptimizedBitmap(context, Uri.parse(uriString)) }.getOrNull()
        }
    }
    Box(modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.14f; scaleY = 1.14f }.alpha(1f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.custom_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.14f; scaleY = 1.14f }.alpha(1f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
            )
        }
        content()
    }
}

@Composable
fun StatusBackgroundImage(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = if (uriString.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching { decodeOptimizedBitmap(context, Uri.parse(uriString)) }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.alpha(1f),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.status_background),
            contentDescription = null,
            modifier = modifier.alpha(1f),
            contentScale = ContentScale.Crop,
        )
    }
}


private fun decodeOptimizedBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { input ->
        if (input == null) return null
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val dm = context.resources.displayMetrics
    val targetW = (dm.widthPixels * 1.5f).toInt().coerceAtLeast(720)
    val targetH = (dm.heightPixels * 1.5f).toInt().coerceAtLeast(1280)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri).use { input ->
        if (input == null) null else BitmapFactory.decodeStream(input, null, opts)
    }
}
