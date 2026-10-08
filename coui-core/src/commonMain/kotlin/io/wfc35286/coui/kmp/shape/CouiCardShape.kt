// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * COUICardListSelectedItemLayout on C17: XL normal 16_1 radius, OplusPathAdapter style 1,
 * explicit weight 2.5. HWUI uses the Lp rounded rectangle, not COUIShapePath's cubic table.
 * The radius is rounded to integer pixels, matching Resources.getDimensionPixelSize.
 */
@Immutable
class CouiCardShape(val cornerRadius: Dp = 24.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Generic(couiCardPath(size, with(density) { cornerRadius.roundToPx().toFloat() }))
}

internal const val COUI_CARD_EXPONENT = 2.5f

internal fun couiCardPath(size: Size, radius: Float): Path {
    val path = Path()
    if (size.width <= 0f || size.height <= 0f) return path
    val r = radius.coerceIn(0f, min(size.width, size.height) / 2f)
    val count = ceil(r * PI / 0.5).toInt().coerceAtLeast(16)
    path.moveTo(size.width - r, 0f)
    for (corner in 0..3) {
        val cx = if (corner < 2) size.width - r else r
        val cy = if (corner == 0 || corner == 3) r else size.height - r
        for (i in 0..count) {
            val angle = -PI / 2 + (corner + i.toDouble() / count) * PI / 2
            val c = cos(angle)
            val s = sin(angle)
            val x = kotlin.math.abs(c).pow(2.0 / COUI_CARD_EXPONENT) * if (c < 0) -1 else 1
            val y = kotlin.math.abs(s).pow(2.0 / COUI_CARD_EXPONENT) * if (s < 0) -1 else 1
            path.lineTo(cx + r * x.toFloat(), cy + r * y.toFloat())
        }
    }
    path.close()
    return path
}

// sdRoundRect / lpNorm and g2RoundRectMask from the device's libhwui.so. Finite differences
// replace fwidth, which RuntimeShader cannot expose. GPU oracle: <= 2/255 alpha at full corners.
internal const val COUI_CARD_SHADER_SOURCE = """
    uniform float2 halfSize;
    uniform float radius;
    layout(color) uniform half4 fillColor;
    float lpNorm(float2 v) {
        v = abs(v);
        float m = max(v.x, v.y);
        if (m < 1e-8) return 0.0;
        float2 u = v / m;
        float s = pow(u.x, 2.5) + pow(u.y, 2.5);
        return m * exp2(log2(max(s, 1e-6)) / 2.5);
    }
    float2 cardDistance(float2 p) {
        float r = clamp(radius, 0.0, min(halfSize.x, halfSize.y));
        float2 q = abs(p) - halfSize + r;
        float d = lpNorm(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        return float2(d, min(q.x, q.y));
    }
    half4 main(float2 coord) {
        float2 p = coord - halfSize;
        float2 d = cardDistance(p);
        float aa = abs(cardDistance(p + float2(0.5, 0.0)).x - cardDistance(p - float2(0.5, 0.0)).x)
                 + abs(cardDistance(p + float2(0.0, 0.5)).x - cardDistance(p - float2(0.0, 0.5)).x);
        float factor = smoothstep(-6.0, -0.8, d.y);
        float coverageWidth = mix(1e-4, aa, factor);
        float coverage = 1.0 - smoothstep(-coverageWidth, coverageWidth, d.x);
        return half4(fillColor.rgb * fillColor.a, fillColor.a) * half(coverage);
    }
"""

internal interface CouiCardShader {
    fun brush(size: Size, radius: Float, color: Color): ShaderBrush
}

internal expect fun createCouiCardShader(): CouiCardShader?
