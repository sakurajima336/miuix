// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.blur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity

/**
 * Skiko fallback of the COUI material.
 *
 * `RenderEffect` is Android-only, so the same two-step maths is expressed with the primitives Skia
 * exposes through Compose: a Gaussian blur of the layer (`Modifier.blur`, sigma in dp) and the two
 * tint layers drawn with the matching [BlendMode]. Blend modes on a Compose canvas are applied
 * against what is already painted, which is exactly what `createColorFilterEffect` does.
 *
 * Note: `Modifier.blur` is a no-op on the web targets of Compose Multiplatform; there the material
 * degrades to the two tint layers only.
 */
@Composable
actual fun CouiBlurMaterial(
    params: CouiBlurParams,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val radiusDp = with(density) { params.blurRadiusPx.toDp() }
    Box(modifier) {
        Box(Modifier.matchParentSize().blur(radiusDp)) {
            content()
        }
        if (!params.isNoEffect) {
            Canvas(Modifier.matchParentSize()) {
                drawTint(params.colorLayer1, params.blendModeLayer1)
                drawTint(params.colorLayer2, params.blendModeLayer2)
            }
        }
    }
}

private fun DrawScope.drawTint(
    color: androidx.compose.ui.graphics.Color,
    blendMode: CouiBlendMode,
) {
    if (color.alpha == 0f) return
    drawRect(color = color, blendMode = blendMode.toComposeBlendMode())
}

internal fun CouiBlendMode.toComposeBlendMode(): BlendMode =
    when (this) {
        CouiBlendMode.SrcOver -> BlendMode.SrcOver
        CouiBlendMode.Overlay -> BlendMode.Overlay
        CouiBlendMode.ColorDodge -> BlendMode.ColorDodge
        CouiBlendMode.Luminosity -> BlendMode.Luminosity
    }