// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import kotlin.math.max


/**
 * Pure-function packers for the shadow-edge / gradient-stroke shader uniforms.
 *
 * They live in `commonMain` on purpose: the packing is plain float arithmetic, so keeping it
 * platform-free makes it unit-testable and reusable by the skiko fallback. Only the AGSL source
 * and the `RuntimeShader` wrapper are Android-specific.
 */
internal object CouiShadowEdgePacking {
    /** Returns `array[index]`, or `default` when the index is out of bounds. */
    fun at(
        array: FloatArray,
        index: Int,
        default: Float,
    ): Float = if (index >= 0 && index < array.size) array[index] else default

    /** Packs the common 12-float uniform array: resolution, position, size, corner radius, weight and anti-aliasing. */
    fun packCommon(
        width: Float,
        height: Float,
    ): FloatArray = floatArrayOf(
        width, height, // resolution
        0f, 0f, // position (unused)
        width, height, // size
        height / 2f, height / 2f, height / 2f, height / 2f, // setCornerRadius(h / 2)
        1f, // setWeight(1.0)
        1f, // anti-aliasing default
    )

    /**
     * Packs the 16-float inner-shadow uniform array.
     *
     * The fade alphas are folded straight into the preset's own `fadeAlpha = [1, 0, 0]`:
     *
     * ```
     * out[3] = fadeAlpha[0] * fadeInAlpha
     * out[4] = fadeAlpha[1] * fadeInAlpha    // = 0, always
     * out[5] = fadeAlpha[2] * fadeOutAlpha   // = 0, always
     * ```
     *
     * So the shader's fade-alpha vector is `(fadeInAlpha, 0, 0)`: the fade-in block runs with a
     * peak alpha of `fadeInAlpha` (0.1 light / 0.01 dark by default) and the fade-out block is
     * skipped outright, because its component never clears the effect threshold. The result is
     * a faint inward wash at the bottom edge, not a second rim.
     *
     * @param fadeInAlpha Peak alpha of the fade-in wash.
     * @param fadeOutAlpha Peak alpha of the fade-out wash (unused by the default preset).
     */
    fun packShadow(
        width: Float,
        height: Float,
        density: Float,
        fadeWidthScale: Float = 1f,
        fadeInAlpha: Float = 0.1f,
        fadeOutAlpha: Float = 0f,
    ): FloatArray {
        // Inner-shadow preset constants (light and dark presets are identical).
        val fadeSize = floatArrayOf(400f, 100f)
        val fadeAlpha = floatArrayOf(1f, 0f, 0f)
        val fadeOffset = floatArrayOf(0f, 20f)
        val fadeScale = floatArrayOf(1.3f, 1.15f)
        val fadeIn = 120f
        val fadeInPara1 = 4f
        val fadeInPara2 = 0.9f
        val fadeOut = 20f
        val fadeOutPara1 = 4f
        val fadeOutPara2 = 0.7f

        val scale = max(1f, density) * 0.34f
        val wN = max(1f, width) / scale
        val hN = max(1f, height) / scale

        return floatArrayOf(
            1f, 1f, 1f, // fadeColor
            at(fadeAlpha, 0, 0f) * fadeInAlpha,
            at(fadeAlpha, 1, 0f) * fadeInAlpha,
            at(fadeAlpha, 2, 0f) * fadeOutAlpha,
            fadeIn * fadeWidthScale * scale,
            at(fadeOffset, 0, 0f) * scale,
            at(fadeOffset, 1, 0f) * scale,
            1f + at(fadeSize, 0, 1f) * (at(fadeScale, 0, 1f) - 1f) / wN,
            1f + at(fadeSize, 1, 1f) * (at(fadeScale, 1, 1f) - 1f) / hN,
            fadeInPara1,
            fadeInPara2,
            fadeOut * fadeWidthScale * scale,
            fadeOutPara1,
            fadeOutPara2,
        )
    }

    /**
     * Packs the 16-float edge (gradient stroke) uniform array.
     *
     * Callers pass the line width in pixels and the line alpha (0.8 light / 0.15 dark).
     */
    fun packEdge(
        width: Float,
        height: Float,
        lineWidthPx: Float,
        lineAlpha: Float,
        lineAngle: Float,
        density: Float,
    ): FloatArray {
        // Gradient-stroke preset constants (light and dark presets are identical).
        val rectLineSize = floatArrayOf(400f, 100f)
        val rectLineColor = floatArrayOf(1f, 1f, 1f)
        val rectLineNear = floatArrayOf(2f, 6f)
        val rectLineFar = floatArrayOf(2f, 3f)
        val rectLineAlpha = floatArrayOf(0.7f, 1f)
        val rectLineFade = floatArrayOf(0.2f, 0.4f, 0.15f, 0.35f)
        val para1 = 4f
        val para2 = 0.8f

        val scale = max(1f, density) * 0.34f
        val wN = max(1f, width) / scale
        val hN = max(1f, height) / scale
        val sizeW = max(1f, at(rectLineSize, 0, wN))
        val sizeH = max(1f, at(rectLineSize, 1, hN))

        val nearInner = max(1f, at(rectLineNear, 0, 1f))
        val nearOuter = max(1f, at(rectLineNear, 1, 6f))
        val farInner = max(1f, at(rectLineFar, 0, 1f))
        val farOuter = max(1f, at(rectLineFar, 1, 4f))
        val base = max(nearInner + nearOuter, farInner + farOuter).coerceAtLeast(1.0E-4f)
        // The requested line width is **divided** by the preset's base width before scaling the
        // four rim values.
        val lwScale = if (lineWidthPx.isNaN() || lineWidthPx <= 0f) 1f else lineWidthPx / base

        val out = FloatArray(16)
        out[0] = at(rectLineColor, 0, 1f)
        out[1] = at(rectLineColor, 1, 0f)
        out[2] = at(rectLineColor, 2, 0f)
        out[3] = max(2f, nearInner * lwScale)
        out[4] = max(2f, nearOuter * lwScale)
        out[5] = max(2f, farInner * lwScale)
        out[6] = max(2f, farOuter * lwScale)
        out[7] = at(rectLineAlpha, 0, 1f) * lineAlpha
        out[8] = at(rectLineAlpha, 1, 0.8f) * lineAlpha
        rectLineFadeForSize(
            at(rectLineFade, 0, 0f),
            at(rectLineFade, 1, 0f),
            sizeW, sizeH, wN, hN, out, 9, 10,
        )
        rectLineFadeForSize(
            at(rectLineFade, 2, 1f),
            at(rectLineFade, 3, 0f),
            sizeW, sizeH, wN, hN, out, 11, 12,
        )
        // The width pairs accumulate as **outer += inner**. Summing the wrong pairs piles the
        // widths onto the *inner* bounds instead: the rim's inner/outer edges swap, the drawn
        // line collapses to a ~1px sliver and its peak alpha drops accordingly.
        out[4] = out[4] + out[3]
        out[6] = out[6] + out[5]
        out[10] = out[10] + out[9]
        out[12] = out[12] + out[11]
        out[13] = lineAngle
        out[14] = para1
        out[15] = para2
        return out
    }

    /** Distributes the edge fade over the two rim spans for the current size. */
    fun rectLineFadeForSize(
        baseFadex: Float,
        baseFadey: Float,
        baseW: Float,
        baseH: Float,
        w: Float,
        h: Float,
        out: FloatArray,
        offset0: Int,
        offset1: Int,
    ) {
        // Both branches are driven by **w**, the normalised width - not `h`. Using `h` on a
        // wide pill (w >> h) turns the fade negative on both calls and the rim degenerates
        // into an even ring.
        val span0 = (baseW + baseH) * 2f
        val span1 = (h + w) * 2f
        val sum = baseFadex + baseFadey
        val scaled = sum * span0
        if (scaled > baseW) {
            val overflow = scaled - baseW
            val far = baseFadey * span0
            out[offset0] = (w + overflow - far) / span1
            out[offset1] = far / span1
            return
        }
        val rest = baseW - scaled
        if (sum <= 0f) {
            out[offset0] = baseFadex
            out[offset1] = baseFadey
            return
        }
        val t = (w - rest) / span1
        out[offset0] = baseFadex * t / sum
        out[offset1] = t * baseFadey / sum
    }
}
