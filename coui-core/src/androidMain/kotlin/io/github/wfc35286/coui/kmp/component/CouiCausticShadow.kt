// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.component

/**
 * The COUI "caustic shadow" (`setOutlineCausticShadowEffect`), as an AGSL shader.
 *
 * On ColorOS this is a native outline effect (`OplusMaterialUtil.setOutlineCausticShadow*`),
 * parameterised by:
 *
 * - `coui_caustic_color_2` = `#42FFFFFF` - the wash colour, white at 25% alpha;
 * - `coui_caustic_layout_b_level4` = `4.9dp` - the lit outline sits **below** the view edge
 *   (l/t/r are 0), so the glow is strongest under the bottom edge;
 * - `coui_caustic_layout_sdf_level4` = `10.4dp` - how far the glow spills from that outline.
 *
 * The glow lives **outside** the pill: on a dark page the official bar shows a *faint* bloom
 * spilling out from under the bottom edge, and essentially nothing along the top/sides (the
 * offsets there are 0 and the light comes from below). `COUICausticShadowEffect` constants
 * (from its `<clinit>`): DEFAULT_SPEED 18, DEFAULT_FACTOR_0/1 0.6, DEFAULT_LUMINANCE_THRESHOLD
 * 0.45, DEFAULT_LUMINANCE_INTENSITY 0.6; `convertCausticShadowColor` tints the colour toward a
 * target luma of 80 (0.299/0.587/0.114) - on the dark material the practical result is the raw
 * `#42FFFFFF`.
 */
internal object CouiCausticShadow {

    /** `coui_caustic_color_2` - the raw resource alpha. */
    const val COLOR_ALPHA: Float = 0x42 / 255f

    /**
     * The alpha actually fed to the shader. `#42FFFFFF` is the *resource* value, but the native
     * outline effect composites it through several attenuating stages (outline SDF mask, the
     * light's solid-angle falloff, the luma tint) before anything reaches the screen; feeding the
     * raw value straight into a single-pass shader produced a donut. Measured against the
     * official bar on a dark page: the spill peak sits at roughly a third of the resource alpha.
     */
    const val SHADER_ALPHA: Float = COLOR_ALPHA / 3f

    /** `coui_caustic_layout_b_level4` - how far the lit outline sits below the bottom edge. */
    const val BOTTOM_OFFSET_DP: Float = 4.9f

    /** `coui_caustic_layout_sdf_level4` - how far the glow spills from the lit outline. */
    const val SDF_WIDTH_DP: Float = 10.4f

    /**
     * The caustic outline glow.
     *
     * All geometry is expressed in the **drawn rect's own** coordinate space, and the pill's
     * centre is passed in explicitly (`uPillCenter`) instead of being inferred: the shader's
     * `fragCoord` origin is the *node's* top-left, while the drawn rect starts at
     * `(-spill, -spill)`, so "the rect's centre" and "the pill's centre" differ by exactly one
     * spill. Assuming they coincided shifted the glow right and down by `spill` - the
     * misalignment the earlier revision showed (glow running off the right end while stopping
     * short on the left).
     *
     * Uniforms (px): `uPillCenter` = the pill's centre in `fragCoord` space; `uPillSize` = the
     * pill (w, h); `uBottomOffsetPx`; `uSdfPx`; `uAlpha`.
     */
    const val SHADER = """
        uniform float2 uPillCenter;
        uniform float2 uPillSize;
        uniform float uSdfPx;
        uniform float uAlpha;
        half4 main(float2 fragCoord) {
            float r = uPillSize.y * 0.5;
            // Capsule SDF of the **pill's own** outline. `coui_caustic_layout_b_level4` (4.9dp)
            // must *not* be used as a translation of that outline: doing so moves the brightest
            // band 5dp below the bottom edge, i.e. the glow visibly detaches from the pill
            // ("偏移"). The official's glow starts at the edge and fades outwards; the `b` value
            // only describes how far the effect's bounds reach below the view.
            float2 p = fragCoord - uPillCenter;
            float2 q = abs(p) - float2(uPillSize.x * 0.5 - r, 0.0);
            float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
            float glow = exp(-max(dist, 0.0) / uSdfPx);
            // bottomness: 0 at the top edge, 1 at the bottom edge. Raised to the 4th power: at
            // mid-height this is 0.0625 instead of 0.25, which is what keeps a halo off the two
            // ends - the official shows essentially nothing along the sides.
            float bottomness = clamp((fragCoord.y - (uPillCenter.y - r)) / uPillSize.y, 0.0, 1.0);
            float a = uAlpha * pow(bottomness, 4.0) * glow;
            // AGSL shader output is premultiplied: rgb must never exceed a, or Skia clamps the
            // fragment to full white.
            return half4(a, a, a, a);
        }
    """
}