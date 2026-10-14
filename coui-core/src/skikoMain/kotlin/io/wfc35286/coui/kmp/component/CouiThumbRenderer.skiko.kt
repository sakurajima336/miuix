// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.RRect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import kotlin.math.ceil
import org.jetbrains.skia.Paint as SkiaPaint
import org.jetbrains.skia.Rect as SkiaRect

/** Skia equivalent of the Android bitmap, blurred stroke and isolated A+B group. */
internal actual class CouiThumbRenderer actual constructor() {
    private val layerAPaint = Paint()
    private val groupPaint = Paint()
    private val layerCPaint = Paint().apply { color = Color.White }
    private val standardPaint = Paint()
    private val layerBPaint = SkiaPaint().apply { isAntiAlias = true }
    private val shadowPaint = SkiaPaint().apply { isAntiAlias = true }
    private var layerBImage: Image? = null
    private var standardShadowRadius = -1f

    actual fun draw(
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
    ) {
        val left = centreX - radius
        val top = centreY - radius
        val right = centreX + radius
        val bottom = centreY + radius
        if (!enabled || textureAlpha <= 0f) {
            if (enabled && shadowRadius > 0f) {
                if (standardShadowRadius != shadowRadius) {
                    standardShadowRadius = shadowRadius
                    shadowPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma(shadowRadius))
                }
                shadowPaint.color = shadowColor.toArgb()
                canvas.skiaCanvas.drawCircle(centreX, centreY + shadowOffsetY, radius, shadowPaint)
            }
            standardPaint.color = thumbColor
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, standardPaint)
            return
        }
        val groupAlpha = (textureAlpha * 255f).toInt().coerceIn(0, 255)
        if (groupAlpha > 0) {
            groupPaint.alpha = groupAlpha / 255f
            canvas.saveLayer(Rect(left, top, right, bottom), groupPaint)
            layerAPaint.shader = LinearGradientShader(
                from = Offset(left, bottom),
                to = Offset(left, top),
                colors = listOf(Color(0xE6FFFFFF), Color(0xE6FFFFFF), Color(0x8CFFFFFF)),
                colorStops = listOf(0f, 0.2f, 1f),
            )
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, layerAPaint)
            val image = getOrCreateLayerBImage(radius * 2f, radius)
            canvas.skiaCanvas.drawImageRect(
                image,
                SkiaRect.makeWH(image.width.toFloat(), image.height.toFloat()),
                SkiaRect.makeLTRB(left - 12f, top - 12f, right + 12f, bottom + 12f),
                SamplingMode.LINEAR,
                layerBPaint,
                true,
            )
            canvas.restore()
        }
        val discAlpha = (layerCAlpha * 255f).toInt().coerceIn(0, 255)
        if (discAlpha > 0) {
            layerCPaint.alpha = discAlpha / 255f
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, layerCPaint)
        }
    }

    private fun getOrCreateLayerBImage(diameter: Float, radius: Float): Image {
        layerBImage?.let { return it }
        val size = ceil((diameter + 24f).toDouble()).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(size, size)
        val canvas = surface.canvas
        canvas.clear(0)
        val rect = SkiaRect.makeLTRB(12f, 12f, diameter + 12f, diameter + 12f)
        val paint = SkiaPaint().apply {
            isAntiAlias = true
            color = 0x77FFFFFF
        }
        canvas.drawRRect(RRect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom, radius), paint)
        canvas.save()
        canvas.clipRRect(RRect.makeOvalLTRB(rect.left, rect.top, rect.right, rect.bottom), false)
        paint.color = Color.White.toArgb()
        paint.mode = PaintMode.STROKE
        paint.strokeWidth = 10f
        val blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma(5f))
        paint.maskFilter = blur
        canvas.translate(0f, 0.4f)
        canvas.drawOval(rect, paint)
        canvas.restore()
        val image = surface.makeImageSnapshot()
        paint.close()
        blur.close()
        surface.close()
        layerBImage = image
        return image
    }
}

/** SkBlurMask's legacy radius-to-sigma conversion used by Android Paint shadows. */
private fun blurSigma(radius: Float): Float = radius * 0.57735f + 0.5f
