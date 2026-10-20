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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity

/**
 * The caustic glow for the flat fallback panel.
 *
 * The official fallback (`applyPanelBackground()` + `updateOuterShadow()` without the material)
 * still carries the elevation caustic shadow, and that shadow is painted by the **native** HWUI
 * routine `SkCanvas::private_draw_oplus_caustic_shadow` - not by an AGSL string, so there is no
 * shader to port. A single soft capsule with the resource constants (`#42ffffff`, 4.9dp below,
 * 10.4dp softening) is the closest Compose equivalent.
 */
private fun DrawScope.drawCouiCausticGlow(density: Float) {
    val bottom = COUI_CAUSTIC_BOTTOM_DP * density
    val blur = COUI_CAUSTIC_SDF_DP * density
    val radius = size.height / 2f
    val base = Color(COUI_CAUSTIC_COLOR)
    drawRoundRect(
        color = base,
        topLeft = Offset(-blur * 0.5f, bottom - blur * 0.5f),
        size = Size(size.width + blur, size.height + blur),
        cornerRadius = CornerRadius(radius + blur * 0.5f),
    )
}

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