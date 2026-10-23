// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity

/** `coui_caustic_layout_b_level5` (`0x7f070812`) - how far the glow reaches below the circle. */
private const val COUI_CAUSTIC_LEVEL5_BOTTOM_DP = 11.199982f

/** `coui_caustic_layout_sdf_level5` (`0x7f070824`) - the glow's band width. */
private const val COUI_CAUSTIC_LEVEL5_SDF_DP = 20f

/** `coui_caustic_color_2` = `#42ffffff` (`0x7f0607af`). */
private const val COUI_CAUSTIC_LEVEL5_COLOR: Int = 0x42FFFFFF

/**
 * `COUIBottomFloatingAccessoryView.setCausticShadowEffect()` - level 5.
 *
 * skiko has no AGSL, so this reproduces the *shape* of the recovered SkSL (see
 * [CouiCausticShadow]): a band that is brightest at the circle's bottom edge and fades out over
 * `sdf` below it, with nothing above or beside the circle - level 5's layout is
 * `(0, 0, 0, 11.199982dp)`, so the optics rect is the circle grown downwards only.
 */
@Composable
internal actual fun Modifier.couiAccessoryCaustic(dark: Boolean): Modifier {
    val density = LocalDensity.current.density
    return this.drawBehind {
        drawCouiAccessoryCausticGlow(density, dark)
    }
}

private fun DrawScope.drawCouiAccessoryCausticGlow(density: Float, dark: Boolean) {
    val bottom = COUI_CAUSTIC_LEVEL5_BOTTOM_DP * density
    val sdf = COUI_CAUSTIC_LEVEL5_SDF_DP * density
    val w = size.width
    val h = size.height
    val radius = h / 2f
    // `coui_caustic_color_2` has no `-night` variant: both themes use `#42ffffff`.
    val base = Color(COUI_CAUSTIC_LEVEL5_COLOR)
    val peak = base.copy(alpha = base.alpha * PEAK)
    val top = (h - sdf).coerceAtLeast(0f)
    val end = h + bottom
    if (end <= top || w <= 0f) return
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

/**
 * The official's band value at the circle's bottom edge. Same measured constant the pill's
 * fallback glow uses; `dark` does not change it because the caustic colour has no night variant.
 */
private const val PEAK = 0.30f
