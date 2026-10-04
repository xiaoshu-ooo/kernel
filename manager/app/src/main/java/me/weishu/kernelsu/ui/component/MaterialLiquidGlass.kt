package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import me.weishu.kernelsu.ui.component.liquid.vibrancy

fun Modifier.liquidGlass(
    backdrop: top.yukonga.miuix.kmp.blur.Backdrop,
    shape: Shape = RoundedCornerShape(28.dp),
    fillAlpha: Float = 0.28f,
): Modifier = this
    .clip(shape)
    .drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(7.dp.toPx(), 7.dp.toPx())
        },
        onDrawSurface = {
            drawRect(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = fillAlpha + 0.10f),
                        Color.White.copy(alpha = fillAlpha),
                        Color.White.copy(alpha = fillAlpha - 0.06f),
                    )
                )
            )
        },
    )
    .border(1.dp, Brush.linearGradient(listOf(
        Color.White.copy(alpha = 0.92f),
        Color.White.copy(alpha = 0.42f),
        Color.White.copy(alpha = 0.78f)
    )), shape)
