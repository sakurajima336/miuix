// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ColorOS 17's full-round outline, distinct from COUIShapePath's cubic corner table.
 *
 * COUISwitch$1 installs OplusOutlineAdapter(outline, 1), radius = height / 2. The framework
 * selects FULL_RRECT_G2_WEIGHT = 1, so libhwui's g2RoundRectMask selects sdCapsule. Constants
 * and the distance function below come from that shader in /system/lib64/libhwui.so.
 *
 * The shader preserves the system's softened straight-edge/cap junction and its coverage.
 * Its finite differences replace HWUI's fwidth (unavailable in Android RuntimeShader).
 * HardwareRenderer comparisons at 132x72, 131x71 and 440x240 give max alpha error <= 2/255.
 */
internal const val COUI_CAPSULE_SHADER_SOURCE = """
    uniform float2 halfSize;
    layout(color) uniform half4 fillColor;

    float smoothAbs(float x) {
        return sqrt(x * x + 1.0);
    }

    float smoothMaxSqrt(float a, float k) {
        return 0.5 * (a + sqrt(a * a + k * k));
    }

    float capsuleDistance(float2 pLocal) {
        bool vertical = halfSize.x < halfSize.y;
        float2 hs = vertical ? halfSize.yx : halfSize;
        float2 p = vertical ? pLocal.yx : pLocal;
        float2 q = float2(smoothAbs(p.x), smoothAbs(p.y));
        float r = hs.y;
        float a = max(hs.x - r, 0.0);
        float scale = r * 0.0125;
        float k = 26.6 * scale;
        float xBand = 70.0 * scale;
        float yBand = 60.0 * scale;
        float s = q.x - a;
        float hardX = max(s, 0.0);
        float softX = k > 0.0 ? smoothMaxSqrt(s, k) : hardX;
        float wx = 1.0 - smoothstep(0.0, max(xBand, 1e-3), abs(s));
        float wy = smoothstep(r - max(yBand, 1e-3), r, q.y);
        float x = mix(hardX, softX, clamp(wx * wy, 0.0, 1.0));
        return length(float2(x, q.y)) - r;
    }

    half4 main(float2 coord) {
        float2 p = coord - halfSize;
        float d = capsuleDistance(p);
        float aa = abs(capsuleDistance(p + float2(0.5, 0.0)) - capsuleDistance(p - float2(0.5, 0.0)))
                 + abs(capsuleDistance(p + float2(0.0, 0.5)) - capsuleDistance(p - float2(0.0, 0.5)));
        float r = min(halfSize.x, halfSize.y);
        float axis = halfSize.x < halfSize.y ? p.y : p.x;
        float zone = abs(axis) - abs(halfSize.x - halfSize.y);
        float transition = max(4.0, 70.0 * r * 0.0125);
        float factor = smoothstep(-transition - 2.0, -transition * 0.2, zone);
        float coverageWidth = mix(1e-4, aa, factor);
        float coverage = 1.0 - smoothstep(-coverageWidth, coverageWidth, d);
        return half4(fillColor.rgb * fillColor.a, fillColor.a) * half(coverage);
    }
"""

/** Each Switch owns its uniforms; changing one must not recolor another. */
internal interface CouiCapsuleShader {
    fun brush(size: Size, color: Color): ShaderBrush
}

internal expect fun createCouiCapsuleShader(): CouiCapsuleShader?

/** The same analytic silhouette for thumb clipping and platforms without runtime shaders. */
internal fun couiCapsulePath(size: Size): Path {
    val path = Path()
    if (size.width <= 0f || size.height <= 0f) return path
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val vertical = halfWidth < halfHeight
    val longHalf = max(halfWidth, halfHeight)
    val radius = min(halfWidth, halfHeight)
    // Sample only one quadrant, then reflect it. At most 0.5px between samples before the
    // radial solve; 24 iterations locate the zero of the official distance function.
    val count = max(48, ceil(longHalf * PI / 0.5).toInt())
    val xs = FloatArray(count + 1)
    val ys = FloatArray(count + 1)
    for (i in 0..count) {
        val angle = PI / 2.0 * i / count
        val dx = longHalf * cos(angle).toFloat()
        val dy = radius * sin(angle).toFloat()
        var low = 0f
        var high = 1f
        repeat(24) {
            val t = (low + high) / 2f
            if (capsuleDistance(dx * t, dy * t, longHalf, radius) > 0f) high = t else low = t
        }
        val t = (low + high) / 2f
        xs[i] = if (vertical) dy * t else dx * t
        ys[i] = if (vertical) dx * t else dy * t
    }
    for (quadrant in 0..3) {
        for (j in 0..count) {
            val i = if ((quadrant % 2 == 0) != vertical) j else count - j
            val x = halfWidth + xs[i] * if (quadrant == 0 || quadrant == 3) 1f else -1f
            val y = halfHeight + ys[i] * if (quadrant < 2) -1f else 1f
            if (quadrant == 0 && j == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun capsuleDistance(x: Float, y: Float, longHalf: Float, radius: Float): Float {
    val qx = sqrt(x * x + 1f)
    val qy = sqrt(y * y + 1f)
    val scale = radius * 0.0125f
    val s = qx - max(longHalf - radius, 0f)
    val hardX = max(s, 0f)
    val k = 26.6f * scale
    val softX = if (k > 0f) 0.5f * (s + sqrt(s * s + k * k)) else hardX
    val wx = 1f - smoothStep(0f, max(70f * scale, 1e-3f), abs(s))
    val wy = smoothStep(radius - max(60f * scale, 1e-3f), radius, qy)
    val blend = (wx * wy).coerceIn(0f, 1f)
    val capX = hardX + (softX - hardX) * blend
    return sqrt(capX * capX + qy * qy) - radius
}

private fun smoothStep(start: Float, end: Float, value: Float): Float {
    val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
