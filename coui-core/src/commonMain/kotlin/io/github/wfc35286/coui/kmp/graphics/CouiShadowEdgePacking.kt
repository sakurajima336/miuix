// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.component

import kotlin.math.max

/**
 * The uniform packers of `com.coui.appcompat.shadowedge`, as **pure functions**.
 *
 * They live in `commonMain` on purpose: `GradientStrokeStyleMapper.toEdgeArray16`,
 * `InnerShadowStyleMapper.toShadowArray16` and `GradientStrokeStyleKt.rectLineFadeForSize` are
 * plain float arithmetic in the official, so keeping them platform-free makes them unit-testable
 * and reusable by the skiko fallback. Only the AGSL source and the `RuntimeShader` wrapper are
 * Android-specific.
 *
 * Every constant below is transcribed from `设置_17.0.0.apk`:
 * - the style presets from `GradientStrokeStylePresets.EDGE_STYLE_0` /
 *   `InnerShadowStylePresets.SHADOW_STYLE_{LIGHT,DARK}_0` (`<clinit>` payloads);
 * - the arithmetic from the two mappers and `GradientStrokeStyleKt`.
 */
internal object CouiShadowEdgePacking {
    /** `GradientStrokeStyleKt.at(array, index, default)`. */
    fun at(
        array: FloatArray,
        index: Int,
        default: Float,
    ): Float = if (index >= 0 && index < array.size) array[index] else default

    /**
     * `u_commonArray[12]` - `setResolution` + `setPosition` + `setSize` + `setCornerRadius` +
     * `setWeight` + `setAntiAliasing` on `ShadowEdgeShader`.
     */
    fun packCommon(
        width: Float,
        height: Float,
    ): FloatArray =
        floatArrayOf(
            width, height, // resolution
            0f, 0f, // position - the bar never calls setPosition
            width, height, // size
            height / 2f, height / 2f, height / 2f, height / 2f, // setCornerRadius(h / 2)
            1f, // setWeight(1.0)
            1f, // anti-aliasing default
        )

    /**
     * `u_shadowArray[16]` - `InnerShadowStyleMapper.toShadowArray16` with
     * `InnerShadowStylePresets.SHADOW_STYLE_0` (light == dark).
     *
     * The mapper's `fadeInAlpha` / `fadeOutAlpha` arguments are `mFadeAlpha[0]` / `mFadeAlpha[1]`,
     * i.e. the pair `ShadowEdgeShader.setFadeAlpha(fadeIn, fadeOut)` was called with - which for
     * the navigation bar is `StrokeEffectParams.shadowFadeIn` / `.shadowFadeOut`
     * (`FRAMEWORK_CAPSULE_PARAMS_12`). They are folded straight into the preset's own
     * `fadeAlpha = [1, 0, 0]`:
     *
     * ```
     * out[3] = fadeAlpha[0] * fadeInAlpha    // = shadowFadeIn
     * out[4] = fadeAlpha[1] * fadeInAlpha    // = 0, always
     * out[5] = fadeAlpha[2] * fadeOutAlpha   // = 0, always
     * ```
     *
     * So the shader's `uFadeAlpha` is `(shadowFadeIn, 0, 0)`: the `fadeIn` block runs with a
     * peak alpha of `shadowFadeIn` (0.1 light / 0.01 dark) and the `fadeOut` block is skipped
     * outright, because `uFadeAlpha.z` never clears `EFFECT_PARAM_THRESHOLD`. That is the
     * official bottom "雾光": a faint inward wash, not a second rim.
     *
     * @param fadeInAlpha `StrokeEffectParams.shadowFadeIn`.
     * @param fadeOutAlpha `StrokeEffectParams.shadowFadeOut`.
     */
    fun packShadow(
        width: Float,
        height: Float,
        density: Float,
        fadeWidthScale: Float = 1f,
        fadeInAlpha: Float = 0.1f,
        fadeOutAlpha: Float = 0f,
    ): FloatArray {
        // SHADOW_STYLE_0 (light and dark presets are byte-identical - verified against the
        // `InnerShadowStylePresets.<clinit>` payloads).
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
     * `u_edgeArray[16]` - `GradientStrokeStyleMapper.toEdgeArray16` with
     * `GradientStrokeStylePresets.EDGE_STYLE_0` (light == dark),
     * `lineWidth = dp2px(FRAMEWORK_CAPSULE_PARAMS_12.edgeWidthDp = 2.0)` and
     * `lineAlpha = 0.8` light / `0.15` dark.
     */
    fun packEdge(
        width: Float,
        height: Float,
        lineWidthPx: Float,
        lineAlpha: Float,
        lineAngle: Float,
        density: Float,
    ): FloatArray {
        // EDGE_STYLE_0 (light and dark presets are identical).
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
        // `GradientStrokeStyleMapper.toEdgeArray16` **divides** the requested line width by the
        // preset's base width before scaling the four rim values.
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
        // `toEdgeArray16` tail, instruction for instruction
        // (`GradientStrokeStyleMapper.smali`, the four `aput`s right after the two
        // `rectLineFadeForSize` calls):
        //
        //     const/4 v5, 0x4          ; v5 = 4
        //     aget v0, v16, v5         ; out[4]
        //     aget v2, v16, v4         ; out[3]   (v4 == 3)
        //     add-float/2addr v0, v2
        //     aput v0, v16, v5         ; out[4] += out[3]   <- nearOuter += nearInner
        //
        //     const/4 v3, 0x6          ; v3 = 6
        //     aget v0, v16, v3         ; out[6]
        //     aget v2, v16, v6         ; out[5]   (v6 == 5)
        //     add-float/2addr v0, v2
        //     aput v0, v16, v3         ; out[6] += out[5]   <- farOuter += farInner
        //
        // The pairs are **outer += inner**. An earlier port summed `out[5] += out[4]` and
        // `out[3] += out[6]` instead, which piles the widths onto the *inner* bounds: the
        // rim's inner/outer edges swapped, the drawn line collapsed to a ~1 px sliver, and
        // its peak alpha dropped to the 0.7x `uRectLineAlpha.x` on the top edge where the
        // official shows the full 1.0x `uRectLineAlpha.y`. That is why the highlight looked
        // nothing like the official even though "the numbers were aligned".
        out[4] = out[4] + out[3]
        out[6] = out[6] + out[5]
        out[10] = out[10] + out[9]
        out[12] = out[12] + out[11]
        out[13] = lineAngle
        out[14] = para1
        out[15] = para2
        return out
    }

    /** `GradientStrokeStyleKt.rectLineFadeForSize`, line for line. */
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
        // `GradientStrokeStyleKt.rectLineFadeForSize`, decoded from the smali instruction by
        // instruction. Parameter order is `(baseFadex, baseFadey, baseW, baseH, w, h, out, o0, o1)`,
        // so **p4 == w** and **p5 == h**:
        //
        //     add-float/2addr p3, p2     ; p3 = baseH + baseW
        //     mul-float/2addr p3, 2      ; span0 = (baseW + baseH) * 2
        //     add-float/2addr p5, p4     ; p5 = h + w
        //     mul-float/2addr p5, 2      ; span1 = (h + w) * 2
        //     add-float v0, p0, p1       ; sum    = baseFadex + baseFadey
        //     mul-float v1, v0, p3       ; scaled = sum * span0
        //     cmpl-float v2, v1, p2
        //     if-lez :cond_19            ; scaled <= baseW -> other branch
        //     sub-float/2addr v1, p2     ; overflow = scaled - baseW
        //     add-float/2addr p4, v1     ; w + overflow            <- p4, i.e. w
        //     mul-float/2addr p1, p3     ; baseFadey * span0
        //     sub-float/2addr p4, p1     ; w + overflow - baseFadey * span0
        //     div-float/2addr p4, p5     ; / span1
        //     aput p4, p6, p7            ; out[o0]
        //     div-float/2addr p1, p5
        //     aput p1, p6, p8            ; out[o1] = baseFadey * span0 / span1
        //   :cond_19
        //     sub-float/2addr p2, v1     ; rest = baseW - scaled
        //     cmpg-float p3, v0, 0
        //     if-gtz :cond_24            ; sum <= 0 -> copy the base fades
        //     aput p0 ... aput p1 ...
        //   :cond_24
        //     sub-float/2addr p4, p2     ; w - rest                 <- p4, i.e. w
        //     div-float/2addr p4, p5     ; t = (w - rest) / span1
        //     mul-float/2addr p0, p4
        //     div-float/2addr p0, v0     ; out[o0] = baseFadex * t / sum
        //     mul-float/2addr p4, p1
        //     div-float/2addr p4, v0     ; out[o1] = t * baseFadey / sum
        //
        // Both branches are driven by **w**, the normalised width - an earlier port used `h`
        // here, which on a wide pill (w >> h) turned the fade negative on both calls and the
        // rim degenerated into an even ring. That was the original "一圈围着" bug, and the
        // `h` variant is what kept the highlight direction subtle-but-wrong afterwards.
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