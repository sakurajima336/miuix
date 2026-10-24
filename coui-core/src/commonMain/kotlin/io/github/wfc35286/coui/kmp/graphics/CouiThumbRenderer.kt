// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope


/**
 * Stateful port of Settings 17's `TexturedThumbRenderer` and `StandardThumbRenderer`.
 *
 * Remember one renderer per seek bar: the original creates LayerB on the first textured draw and
 * scales that cached bitmap on subsequent draws, including while the thumb's radius is changing.
 * Its shadow radius (5), stroke width (10), vertical offset (0.4) and bitmap inset (12) are raw
 * pixels, not dp. A+B share one saveLayerAlpha; C is drawn after that group has been restored.
 */
internal expect class CouiThumbRenderer() {
    fun draw(
        canvas: Canvas,
        centreX: Float,
        centreY: Float,
        radius: Float,
        textureAlpha: Float,
        layerCAlpha: Float,
        thumbColor: Color,
        shadowRadius: Float,
        shadowOffsetY: Float,
        shadowColor: Color,
        enabled: Boolean,
    )
}

internal fun DrawScope.drawCouiTexturedThumb(
    renderer: CouiThumbRenderer,
    centreX: Float,
    centreY: Float,
    radius: Float,
    layerAAlpha: Float,
    layerCAlpha: Float,
    layerCColor: Color,
    shadowRadius: Float,
    shadowOffsetY: Float,
    shadowColor: Color,
    enabled: Boolean,
) {
    renderer.draw(
        canvas = drawContext.canvas,
        centreX = centreX,
        centreY = centreY,
        radius = radius,
        textureAlpha = layerAAlpha,
        layerCAlpha = layerCAlpha,
        thumbColor = layerCColor,
        shadowRadius = shadowRadius,
        shadowOffsetY = shadowOffsetY,
        shadowColor = shadowColor,
        enabled = enabled,
    )
}
