// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics

/**
 * The ColorOS platform backdrop blur, transcribed **verbatim** from the AGSL shader strings
 * embedded in `/system/lib64/libhwui.so` (ColorOS 17, Android 15/16 era).
 *
 * The strings were extracted with `/root/coui/hwuishader2.py` and are byte-identical to what the
 * platform runs inside `ur_altair.AltairKawaseBlurFilter` / `OplusUniBackdropFilterDrawable`.
 *
 * The pipeline is two RuntimeShaders:
 *
 *  1. **blur** — the animated one-pass gaussian:
 *     ```
 *     ratio  = 1 - pow(1 - ratio, in_speed)          // radius ramps in, speed-controlled
 *     radius = int(in_blurRadius * ratio)
 *     weight = gaussian(i, sigma=radius/3), normalised over the visible taps
 *     final  = c*(1-Σ2w) + Σ child.eval(coord±offset*i)*w   (+ IGN dither)
 *     ```
 *     `radius <= 1` short-circuits to a straight copy, which is what makes the official blur
 *     "degenerate" cleanly to nothing while animating in.
 *
 *  2. **mix** — the material wash:
 *     ```
 *     ratio = (uRatios.x + (uRatios.y-uRatios.x) * t/uSize)^2   // vertical gradient
 *     lum   = luminosity(uMixColor.rgb, c.rgb); c = mix(c, lum, uMixColor.a * ratio)
 *     blend = <overlay|colorDodge>(uBlendColor.rgb, c); c = mix(c, blend, uBlendColor.a * ratio)
 *     ```
 *     The `ratio²` falloff is what produces the official "fog is stronger at one edge" look —
 *     `RenderEffect.createColorFilterEffect` cannot express it (it is spatially uniform), which is
 *     exactly why the naive RenderEffect chain never looked right.
 *
 * Everything below is AGSL 13-compatible (no `const` arrays, no dynamic loops beyond the uniform
 * bound) and compiles on API 33+.
 */
internal object CouiPlatformBlurShaders {

    // ---- blend helpers, verbatim from libhwui.so --------------------------------------------
    //   vec3 luminosity(vec3 s, vec3 d)   @0x9e2c8
    //   half overlay(half s, half d)      @0xc404c
    //   half colorDodge(half s, half d)   @0xa1956
    private const val HELPERS = """
        vec3 luminosity(vec3 s, vec3 d) {
            float dLum = dot(d, vec3(0.3, 0.59, 0.11));
            float sLum = dot(s, vec3(0.3, 0.59, 0.11));
            float lum = sLum - dLum;
            vec3 c = d + lum;
            float minC = min(min(c.x, c.y), c.z);
            float maxC = max(max(c.x, c.y), c.z);
            if (minC < 0.0) {
                return sLum + ((c - sLum) * sLum) / (sLum - minC);
            } else if (maxC > 1.0) {
                return sLum + ((c - sLum) * (1.0 - sLum)) / (maxC - sLum);
            } else {
                return c;
            }
        }
        half overlayS(half s, half d) {
            return (d < 0.5) ? 2.0 * s * d : 1.0 - 2.0 * (1.0 - s) * (1.0 - d);
        }
        half3 overlay3(half3 s, half3 d) {
            half3 c;
            c.x = overlayS(s.x, d.x);
            c.y = overlayS(s.y, d.y);
            c.z = overlayS(s.z, d.z);
            return c;
        }
        half colorDodgeS(half s, half d) {
            return d / (1.0 - s);
        }
        half3 colorDodge3(half3 s, half3 d) {
            half3 c;
            c.x = colorDodgeS(s.x, d.x);
            c.y = colorDodgeS(s.y, d.y);
            c.z = colorDodgeS(s.z, d.z);
            return c;
        }
    """

    /**
     * The animated gaussian blur. Verbatim from libhwui.so @0xf317d region
     * (`const float PI = 3.1415926;uniform shader child;uniform float in_size;...`), with the
     * `%d`-templated static variant rewritten as a uniform so one shader serves every radius.
     *
     * Uniforms:
     * - `in_blurRadius` — the official blur radius, raw pixels (`BlurEffectParams.blurRadius`).
     * - `in_speed` — the ramp exponent of `ratio = 1 - pow(1 - ratio, in_speed)`. The official
     *   drives this from the SurfaceFlinger animation; for a static surface `1.0` makes
     *   `ratio = blurRadius/in_blurRadius` exactly, i.e. the full radius.
     * - `in_blurOffset` — per-tap sample offset in **texels**, `1.0` in the official blur passes.
     * - `in_ditherValue` — IGN dither amplitude; the official passes the frame's dither config.
     * - `in_random` — per-frame random offset for the IGN function.
     */
    const val BLUR = """
        uniform shader child;
        uniform half2 in_blurOffset;
        uniform float in_blurRadius;
        uniform float in_speed;
        uniform float in_ditherValue;
        uniform float in_random;
        const float PI = 3.1415926;
        // AGSL requires a compile-time loop bound; 64 covers every radius the material uses
        // (the largest official preset is 40) and the body breaks out at the live radius.
        const int MAX_RADIUS = 64;
        float getWeight(int i, int radius) {
            float sigma = float(radius) / 3.0;
            float value = (1.0 / sqrt(2.0 * PI * sigma * sigma)) *
                exp(float(-i * i) / (2.0 * sigma * sigma + 0.00000001));
            return clamp(value, 0.0, 1.0);
        }
        float interleavedGradientNoise(float2 coord) {
            const float3 magicNumbers = float3(0.06711056, 0.00583715, 52.9829189);
            return fract(magicNumbers.z * fract(dot(coord, magicNumbers.xy)));
        }
        half4 addNoise(float2 coord, half4 color) {
            float noise = interleavedGradientNoise(coord + in_random) - 0.5;
            color.xyz += noise / in_ditherValue;
            return color;
        }
        half4 main(float2 coord) {
            half4 c = child.eval(coord);
            float ratio = 1.0;
            ratio = 1.0 - pow(1.0 - ratio, in_speed);
            int radius = int(float(in_blurRadius) * ratio);
            if (radius <= 1) return c;
            float2 offset = float2(0.0, 0.0);
            float sumWeight = 0.0;
            half4 finalColor = half4(0.0);
            for (int i = 1; i <= MAX_RADIUS; i++) {
                if (i > radius) break;
                float weight = getWeight(i, radius);
                offset.x = min(in_blurOffset.x, float(radius)) * float(i);
                offset.y = min(in_blurOffset.y, float(radius)) * float(i);
                finalColor += child.eval(coord - offset) * weight;
                finalColor += child.eval(coord + offset) * weight;
                sumWeight += 2.0 * weight;
            }
            finalColor += c * (1.0 - sumWeight);
            return addNoise(coord, finalColor);
        }
    """

    /**
     * The material wash. Verbatim uniforms from libhwui.so @0x9a49e
     * (`uniform shader child;uniform half4 uBlendColor;uniform half4 uMixColor;...`) and the
     * mix fragments @0xa1a12 (overlay) / @0xe9050 (colorDodge). The `overlay`/`colorDodge`
     * halves are renamed (`overlay3`/`colorDodge3`) because AGSL reserves the bare names for
     * the built-in blend enum; the maths is untouched.
     *
     * Uniforms:
     * - `uBlendColor` — layer 1: rgb = tint, a = mix weight (`colorLayer1.alpha`).
     * - `uMixColor` — layer 2: rgb = tint, a = mix weight (`colorLayer2.alpha`).
     * - `uRatios` — (start, end) of the vertical falloff, 0..1 across the surface.
     * - `uSize` — the extent the ratios run over: width when `uDirection == 0`, else height.
     * - `uDirection` — 0 = gradient along x, 1 = along y.
     * - `uBlendIsDodge` — 1 selects the light theme's `colorDodge`, 0 the dark theme's
     *   `overlay`. The official compiles two shaders; one uniform keeps both on the same object.
     */
    const val MIX = """
        uniform shader child;
        uniform half4 uBlendColor;
        uniform half4 uMixColor;
        uniform half2 uRatios;
        uniform float uSize;
        uniform int uDirection;
        uniform int uBlendIsDodge;
        // Composition-side addition (not in the libhwui source): the sampled layer is expanded by
        // the blur kernel's reach so the effect has real pixels outside the pill's silhouette. That
        // shifts the layer's coordinate origin outwards by the same amount, so the gradient's
        // origin has to be moved back - otherwise the fog's bottom-heavy falloff would start
        // (and end) some tens of pixels below where the pill actually begins.
        uniform float2 uCoordOffset;
        $HELPERS
        half4 main(float2 coord) {
            half4 inColor = child.eval(coord);
            float2 g = coord - uCoordOffset;
            float ratio = 1.0;
            if (uDirection == 0) {
                ratio = uRatios.x + (uRatios.y - uRatios.x) * (g.x / uSize);
            } else {
                ratio = uRatios.x + (uRatios.y - uRatios.x) * (g.y / uSize);
            }
            ratio = ratio * ratio;
            half3 luminosityCol = luminosity(uMixColor.rgb, inColor.rgb);
            inColor.rgb = mix(inColor.rgb, half3(luminosityCol), uMixColor.w * ratio);
            half3 blendColor = (uBlendIsDodge == 1)
                ? colorDodge3(uBlendColor.rgb, inColor.rgb)
                : overlay3(uBlendColor.rgb, inColor.rgb);
            inColor.rgb = mix(inColor.rgb, half3(blendColor), uBlendColor.w * ratio);
            return inColor;
        }
    """
}
