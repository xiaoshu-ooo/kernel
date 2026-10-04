package me.weishu.kernelsu.ui.component.material

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.ModuleBackgroundLayer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    backgroundModuleId: String? = null,
    content: @Composable () -> Unit,
) {
    val glassColor = containerColor.copy(alpha = (containerColor.alpha * 0.72f).coerceIn(0.35f, 0.82f))
    val colors = CardDefaults.cardColors(
        containerColor = if (backgroundModuleId != null) Color.Transparent else glassColor,
        contentColor = contentColor,
    )
    val glassModifier = modifier
        .shadow(10.dp, shape, clip = false)
        .border(1.dp, Color.White.copy(alpha = 0.62f), shape)
    when {
        onLongClick != null -> Card(
            modifier = glassModifier
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                ),
            colors = colors,
            shape = shape,
        ) {
                if (backgroundModuleId != null) {
                    Box(Modifier.fillMaxWidth()) {
                        ModuleBackgroundLayer(backgroundModuleId, Modifier.fillMaxSize(), 0.32f)
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.20f)))
                        content()
                    }
                } else content()
            }

        onClick != null -> Card(
            onClick = onClick,
            modifier = glassModifier,
            enabled = enabled,
            colors = colors,
            shape = shape,
        ) {
                if (backgroundModuleId != null) {
                    Box(Modifier.fillMaxWidth()) {
                        ModuleBackgroundLayer(backgroundModuleId, Modifier.fillMaxSize(), 0.32f)
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.20f)))
                        content()
                    }
                } else content()
            }

        else -> Card(
            modifier = glassModifier,
            colors = colors,
            shape = shape,
        ) {
                if (backgroundModuleId != null) {
                    Box(Modifier.fillMaxWidth()) {
                        ModuleBackgroundLayer(backgroundModuleId, Modifier.fillMaxSize(), 0.32f)
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.20f)))
                        content()
                    }
                } else content()
            }
    }
}
