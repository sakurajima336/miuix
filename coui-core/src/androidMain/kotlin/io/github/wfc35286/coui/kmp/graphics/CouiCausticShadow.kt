// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

/**
 * The COUI "caustic shadow" (`setOutlineCausticShadow*`), recovered from the platform.
 *
 * ## Where the pixels actually come from
 *
 * No APK draws this. The chain is
 * `COUIMaterialEffect.setOutlineCausticShadow*` -> `COUICausticShadowEffect` ->
 * `com.oplus.view.material.OplusMaterialUtil` -> `RenderNode.setOutlineCausticShadow{Color,Layout,Params}`
 * -> JNI `nSetCausticShadow*` -> `android::uirenderer::RenderProperties` -> `SkCanvas::onDrawCausticShadow`
 * -> `skgpu::ganesh::Device::drawCausticShadow` -> `OplusCaustionShadowEffect::Make` (a
 * `SkRuntimeEffect` built from an embedded SkSL block, `libhwui.so` vaddr `0xdeab9`).
 *
 * [SHADER] is a transcription of that SkSL block with the placeholders resolved. Everything below
 * that is marked "official" was read out of the binary, not guessed; see
 * `official-data/CouiBottomFloatingNavigationBar/38-CAUSTIC-SHADOW-NATIVE.md`.
 *
 * ## What the effect looks like, and why
 *
 * 1. **Shape** - a rounded-rect / oval SDF, softened by a Hermite `smoothstep` band of width
 *    `sdfWidth` (no gaussian, no blur filter anywhere in the path).
 * 2. **Direction** - the SDF term is multiplied by a *vertical* `smoothstep` raised to `speed`, so
 *    the band is bright at the bottom edge and dies before it reaches the top. That is the whole
 *    "light bends around the bottom edge" look.
 * 3. **Contrast** - `mix(value, pow(value, factor0), factor1)`.
 * 4. **Background dependence** - `alpha *= (1 - gray)^2` where
 *    `gray = clamp((luma(backdrop) - 0.45) * 0.6, 0, 1)`. On a black page `gray = 0` and the glow
 *    runs at full strength; the brighter the page behind it, the more it disappears. This is the
 *    "black vs. content below looks completely different" behaviour, and it is **squared**, not
 *    linear.
 * 5. **Compositing** - `rgb = bg + color*alpha` (pure additive, in sRGB), so it reads as light
 *    being added, not as a dark drop shadow.
 */
internal object CouiCausticShadow {

    /**
     * One official level: `coui_caustic_layout_{l,t,r,b}_levelN` + `coui_caustic_layout_sdf_levelN`.
     *
     * The four offsets are *relative to the view's edges* in dp (the framework multiplies by the
     * density and hands px to the render node). `bottom` is what makes the glow spill below the
     * pill; `left`/`right` are only non-zero on level 6 (the FAB), where they shrink the optics
     * rect by 8dp horizontally.
     *
     * `level3` and `level4` carry identical numbers in both APKs (the gallery and the store) -
     * that redundancy is in the official resources, not a transcription slip.
     */
    internal data class Level(
        val leftDp: Float,
        val topDp: Float,
        val rightDp: Float,
        val bottomDp: Float,
        val sdfDp: Float,
    ) {
        internal companion object {
            /** `COUIChipTab` */
            val Level1 = Level(0f, 0f, 0f, 4.299988f, 9.22998f)

            /** `COUISearchBar`, `ToolbarMaterialEffectDelegate`, the gallery's own `m1q` */
            val Level2 = Level(0f, 0f, 0f, 4.899994f, 9.22998f)

            /** `COUIBottomFloatingToolbarMenuView`, `COUIButton`, `COUISegmentButtonLayout` */
            val Level3 = Level(0f, 0f, 0f, 4.899994f, 10.399994f)

            /** `BaseBottomFloatingMenuView` - the floating navigation bar's own pill. */
            val Level4 = Level(0f, 0f, 0f, 4.899994f, 10.399994f)

            /** `COUIBottomFloatingAccessoryView` - the accessory strip at both ends. */
            val Level5 = Level(0f, 0f, 0f, 11.199982f, 20f)

            /** `COUIFABShadowUtils` - the round floating button. */
            val Level6 = Level(4f, 0f, -4f, 11.199982f, 14f)
        }
    }

    /** `coui_caustic_color_1..4`. Only `_2` has no `-night` variant. */
    internal object Colors {
        /** `coui_caustic_color_1` */
        const val LIGHT_1: Int = 0x2EFFFFFF
        const val DARK_1: Int = 0x33FFFFFF

        /** `coui_caustic_color_2` - what the navigation bar uses. */
        const val LIGHT_2: Int = 0x42FFFFFF
        const val DARK_2: Int = 0x42FFFFFF

        /** `coui_caustic_color_3` */
        val LIGHT_3: Int = 0xCCFFFFFF.toInt()
        const val DARK_3: Int = 0x33FFFFFF

        /** `coui_caustic_color_4` - declared in the gallery's resources but referenced by no code. */
        val LIGHT_4: Int = 0x990066FF.toInt()
    }

    /**
     * `COUICausticShadowEffect`'s `DEFAULT_*` constants, i.e. what the single-float overload of
     * `setOutlineCausticShadowParams(View, F)` leaves in place for the other five.
     */
    const val DEFAULT_FACTOR_0: Float = 0.6f
    const val DEFAULT_FACTOR_1: Float = 0.6f
    const val DEFAULT_SPEED: Float = 18f
    const val DEFAULT_LUMINANCE_THRESHOLD: Float = 0.45f
    const val DEFAULT_LUMINANCE_INTENSITY: Float = 0.6f

    /**
     * `COUIFABShadowUtils` passes all six explicitly: 0.3 / 0.2 / 1.4 (sdf comes from level 6).
     * That is a much softer, wider band than the defaults - the FAB's glow is not a thin ring.
     */
    const val FAB_FACTOR_0: Float = 0.3f
    const val FAB_FACTOR_1: Float = 0.2f
    const val FAB_SPEED: Float = 1.4f

    /** `convertCausticShadowColor`'s target luminance. */
    const val LUMA_TARGET_BRIGHTNESS: Float = 80f

    /**
     * `COUICausticShadowEffect.convertCausticShadowColor` - normalises a colour to a fixed
     * luminance (80/255) while keeping its hue. Only the FAB path uses it; everything else feeds
     * the raw resource colour straight in.
     */
    internal fun convertCausticShadowColor(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        if (luma == 0f) return 0xFF828282.toInt()
        if (luma == 255f) return 0xFF0B0B0B.toInt()
        fun clamp(v: Float): Int = v.coerceIn(0f, 255f).toInt()
        return (0xFF shl 24) or (clamp(r * LUMA_TARGET_BRIGHTNESS / luma) shl 16) or
            (clamp(g * LUMA_TARGET_BRIGHTNESS / luma) shl 8) or
            clamp(b * LUMA_TARGET_BRIGHTNESS / luma)
    }

    /** A level resolved against the display density. All lengths are px. */
    internal data class Params(
        val color: Int,
        val leftPx: Float,
        val topPx: Float,
        val rightPx: Float,
        val bottomPx: Float,
        val sdfPx: Float,
        val factor0: Float = DEFAULT_FACTOR_0,
        val factor1: Float = DEFAULT_FACTOR_1,
        val speed: Float = DEFAULT_SPEED,
        val luminanceThreshold: Float = DEFAULT_LUMINANCE_THRESHOLD,
        val luminanceIntensity: Float = DEFAULT_LUMINANCE_INTENSITY,
    )

    internal fun level(
        level: Level,
        density: Float,
        color: Int,
    ): Params = Params(
        color = color,
        leftPx = level.leftDp * density,
        topPx = level.topDp * density,
        rightPx = level.rightDp * density,
        bottomPx = level.bottomDp * density,
        sdfPx = level.sdfDp * density,
    )

    /**
     * The recovered SkSL, transcribed to AGSL.
     *
     * Differences from the original block, all deliberate:
     * - the two `smoothstep`s with reversed edges (`smoothstep(sdf, 0, d)` and
     *   `smoothstep(hy+sdf, -hy-sdf, -y)`) are written out as `1 - hermite(d/sdf)` and
     *   `hermite(0.5 + 0.5*y/(hy+sdf))`. GLSL leaves reversed edges undefined, SkSL happens to
     *   clamp the usual way, and spelling it out keeps the two compilers agreeing.
     * - `u_pyrolysisArray` / `u_commonArray` are unpacked into named uniforms.
     * - the backdrop is read with `uBackdrop.eval(coord)` for the luminance term only; the
     *   original also does `base = blend(base, opticsColor)`, which the caller reproduces by
     *   drawing this output with [android.graphics.BlendMode.PLUS] (the official blend is
     *   `bg + color*alpha`, i.e. pure addition).
     */
    const val SHADER: String = """
        uniform shader uBackdrop;
        uniform float4 uColor;
        uniform float4 uLayout;
        uniform float4 uParams;
        uniform float2 uLum;
        uniform float2 uRectLT;
        uniform float2 uRectSize;
        uniform float uCorner;
        uniform int uOutlineType;

        float sdRBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
        }

        float sdOval(float2 p, float2 b) {
            float2 bb = max(b, float2(0.0001));
            float k0 = length(p / bb);
            float k1 = length(p / (bb * bb));
            return k1 > 0.000001 ? k0 * (k0 - 1.0) / k1 : 0.0;
        }

        float hermite(float t) {
            t = clamp(t, 0.0, 1.0);
            return t * t * (3.0 - 2.0 * t);
        }

        half4 main(float2 coord) {
            float sdfWidth = uParams.x;
            float factor0 = uParams.y;
            float factor1 = uParams.z;
            float speed = uParams.w;

            float2 halfRecSize = uRectSize * 0.5;
            float2 recCenter = uRectLT + halfRecSize;
            // The layout offsets are not an inset: they move the optics rect's centre by half the
            // sum and grow it by the difference. level4 (0,0,0,4.9dp) therefore extends the shape
            // downwards by 4.9dp and leaves the top edge where it was.
            float2 opticsOffset = float2(
                (uLayout.x + uLayout.z) * 0.5,
                (uLayout.y + uLayout.w) * 0.5
            );
            float2 halfSizeOptics = halfRecSize + float2(
                uLayout.z - uLayout.x,
                uLayout.w - uLayout.y
            ) * 0.5;
            float2 pos = coord - recCenter - opticsOffset;

            float sdfBand = min(sdfWidth, min(halfRecSize.x, halfRecSize.y));
            float alpha = 0.0;

            if (all(lessThanEqual(abs(pos), halfSizeOptics)) && sdfBand > 0.0) {
                float2 inner = max(halfSizeOptics - sdfBand, float2(0.0));
                float corner = min(uCorner, min(inner.x, inner.y));
                float d = (uOutlineType == 1) ? sdOval(pos, inner) : sdRBox(pos, inner, corner);

                float value = 1.0 - hermite(d / sdfBand);

                // Bottom-heavy: 0 at the optics rect's top edge, 1 at its bottom edge, then
                // raised to `speed` (18 for the defaults) which squeezes the band onto the
                // bottom edge.
                float span = inner.y + sdfBand;
                float tv = span > 0.0 ? clamp(0.5 + 0.5 * pos.y / span, 0.0, 1.0) : 1.0;
                value *= pow(hermite(tv), speed);

                if (factor1 > 0.001) {
                    value = pow(value, factor0) * factor1 + (1.0 - factor1) * value;
                }

                alpha = uColor.a * value;

                // Kill the glow on bright backdrops. Squared on purpose.
                half4 base = uBackdrop.eval(coord);
                float lum = dot(float3(0.2126, 0.7152, 0.0722), float3(base.rgb));
                float gray = clamp((lum - uLum.x) * uLum.y, 0.0, 1.0);
                alpha *= (1.0 - gray) * (1.0 - gray);
            }

            // Premultiplied: the caller composites with PLUS, which reproduces the official
            // `rgb = bg + color*alpha`.
            return half4(uColor.rgb * alpha, alpha);
        }
    """
}
