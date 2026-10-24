// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity

/**
 * The caustic glow for the flat fallback panel.
 *
 * The real thing is a Skia runtime effect inside `libhwui.so` (see [CouiCausticShadow] for the
 * recovered source); skiko has no AGSL, so this reproduces its *shape*: a band that is brightest
 * at the pill's bottom edge and fades out over `sdf` below it, with nothing above or beside the
 * pill (level 4's layout is `(0, 0, 0, 4.9dp)`, so the optics rect is the pill grown downwards
 * only). The official's peak intensity at the bottom edge measures ~0.30 of the raw colour alpha
 * on a black page, which is where [PEAK] comes from.
 */
private fun DrawScope.drawCouiCausticGlow(density: Float) {
    val bottom = COUI_CAUSTIC_BOTTOM_DP * density
    val sdf = COUI_CAUSTIC_SDF_DP * density
    val w = size.width
    val h = size.height
    val radius = h / 2f
    val base = Color(COUI_CAUSTIC_COLOR)
    val peak = base.copy(alpha = base.alpha * PEAK)
    val top = (h - sdf).coerceAtLeast(0f)
    val end = h + bottom
    if (end <= top || w <= 0f) return
    // Stop positions are fractions of the drawn band; `edge` is where the pill's bottom sits.
    val edge = ((h - top) / (end - top)).coerceIn(0.01f, 0.99f)
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                0f to Color.Transparent,
                edge * 0.5f to peak.copy(alpha = peak.alpha * 0.35f),
                edge to peak,
                1f to Color.Transparent,
                startY = top,
                endY = end,
            ),
        topLeft = Offset(0f, top),
        size = Size(w, end - top),
        cornerRadius = CornerRadius(radius),
    )
}

/** The official's band value at the pill's bottom edge, measured against a real screenshot. */
private const val PEAK = 0.30f

/**
 * The ColorOS material is an Oplus framework feature (`com.oplus.view.material.*`), so every
 * non-Android target - and every Android build without that framework - draws the Compose
 * approximation instead. It is a fallback, never the reference.
 */
internal actual fun isCouiFloatingMaterialAvailable(): Boolean = false

@Composable
internal actual fun CouiFloatingMaterialSurface(
    spec: CouiFloatingMaterialSpec,
    enabled: Boolean,
    backdrop: CouiBackdrop?,
    translation: () -> Offset,
    pressScale: () -> Float,
    scaleX: () -> Float,
    scaleY: () -> Float,
    transformOrigin: () -> Offset,
    shadowEnabled: Boolean,
    dark: Boolean,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    // The parameter names shadow the `graphicsLayer` properties, so they are read through local
    // aliases: `scaleX = scaleX()` would assign to the parameter instead of the layer.
    val scaleXProvider = scaleX
    val scaleYProvider = scaleY
    val originProvider = transformOrigin
    val pressScaleProvider = pressScale
    val glowDensity = LocalDensity.current.density
    Box(
        modifier = modifier
            .graphicsLayer {
                val t = translation()
                translationX = t.x
                translationY = t.y
                // Press scale: centred, on its own layer.
                val press = pressScaleProvider()
                this.scaleX = press
                this.scaleY = press
            }
            .graphicsLayer {
                val originFraction = originProvider()
                this.scaleX = scaleXProvider()
                this.scaleY = scaleYProvider()
                this.transformOrigin = TransformOrigin(originFraction.x, originFraction.y)
            }
            .drawBehind {
                if (spec.applyCausticShadow) drawCouiCausticGlow(glowDensity)
            },
    ) {
        fallback()
        content()
    }
}