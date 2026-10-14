// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.ceil
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor

/** Native Canvas operations in the same order as Settings 17's renderer smali. */
internal actual class CouiThumbRenderer actual constructor() {
    private val textureRect = RectF()
    private val layerAPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val layerAMatrix = Matrix()
    private var layerAShader: LinearGradient? = null
    private var gradientHeight = 0f
    private val layerBFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x77FFFFFF
    }
    private val layerBShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = AndroidColor.TRANSPARENT
        setShadowLayer(5f, 0f, 0.4f, AndroidColor.WHITE)
    }
    private val layerBClipPath = Path()
    private val layerBBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val layerBDstRect = RectF()
    private var layerBBitmap: Bitmap? = null
    private val layerCPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    private val standardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
        drawNative(
            canvas.nativeCanvas,
            centreX,
            centreY,
            radius,
            textureAlpha,
            layerCAlpha,
            thumbColor.toArgb(),
            shadowRadius,
            shadowOffsetY,
            shadowColor.toArgb(),
            enabled,
        )
    }

    // Kept as a separate operation so the offline Android probe can compare this production code
    // directly against the APK renderer without involving gestures or screenshot compression.
    fun drawNative(
        canvas: AndroidCanvas,
        centreX: Float,
        centreY: Float,
        radius: Float,
        textureAlpha: Float,
        layerCAlpha: Float,
        thumbColor: Int,
        shadowRadius: Float,
        shadowOffsetY: Float,
        shadowColor: Int,
        enabled: Boolean,
    ) {
        textureRect.set(centreX - radius, centreY - radius, centreX + radius, centreY + radius)
        // The branch tests the float before truncating the group's alpha to an integer.
        if (!enabled || textureAlpha <= 0f) {
            standardPaint.color = thumbColor
            if (enabled && shadowRadius > 0f) {
                standardPaint.setShadowLayer(shadowRadius, 0f, shadowOffsetY, shadowColor)
            }
            canvas.drawRoundRect(textureRect, radius, radius, standardPaint)
            standardPaint.clearShadowLayer()
            return
        }
        val groupAlpha = (textureAlpha * 255f).toInt().coerceIn(0, 255)
        if (groupAlpha > 0) {
            canvas.saveLayerAlpha(textureRect, groupAlpha)
            drawLayerA(canvas, radius)
            drawLayerB(canvas, radius)
            canvas.restore()
        }
        val discAlpha = (layerCAlpha * 255f).toInt().coerceIn(0, 255)
        if (discAlpha > 0) {
            layerCPaint.alpha = discAlpha
            canvas.drawRoundRect(textureRect, radius, radius, layerCPaint)
        }
    }

    private fun drawLayerA(canvas: AndroidCanvas, radius: Float) {
        val height = textureRect.height()
        if (height <= 0f) return
        if (height != gradientHeight || layerAShader == null) {
            gradientHeight = height
            // Smali constructor registers: (x0=0, y0=height, x1=0, y1=0).
            // The TOP/BOTTOM constant names do not describe their final screen positions.
            layerAShader = LinearGradient(
                0f,
                height,
                0f,
                0f,
                intArrayOf(0xE6FFFFFF.toInt(), 0xE6FFFFFF.toInt(), 0x8CFFFFFF.toInt()),
                floatArrayOf(0f, 0.2f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val shader = layerAShader ?: return
        layerAMatrix.setTranslate(textureRect.left, textureRect.top)
        shader.setLocalMatrix(layerAMatrix)
        layerAPaint.shader = shader
        canvas.drawRoundRect(textureRect, radius, radius, layerAPaint)
    }

    private fun drawLayerB(canvas: AndroidCanvas, radius: Float) {
        val bitmap = getOrCreateLayerBBitmap(textureRect.width(), radius) ?: return
        layerBDstRect.set(
            textureRect.left - 12f,
            textureRect.top - 12f,
            textureRect.right + 12f,
            textureRect.bottom + 12f,
        )
        canvas.drawBitmap(bitmap, null, layerBDstRect, layerBBitmapPaint)
    }

    private fun getOrCreateLayerBBitmap(diameter: Float, radius: Float): Bitmap? {
        if (diameter <= 0f) return null
        layerBBitmap?.let { return it }
        val size = ceil((diameter + 24f).toDouble()).toInt()
        if (size <= 0) return null
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val disc = RectF(12f, 12f, diameter + 12f, diameter + 12f)
        canvas.drawRoundRect(disc, radius, radius, layerBFillPaint)
        layerBClipPath.reset()
        layerBClipPath.addOval(disc, Path.Direction.CCW)
        canvas.save()
        canvas.clipPath(layerBClipPath)
        canvas.drawOval(disc, layerBShadowPaint)
        canvas.restore()
        layerBBitmap = bitmap
        return bitmap
    }
}
