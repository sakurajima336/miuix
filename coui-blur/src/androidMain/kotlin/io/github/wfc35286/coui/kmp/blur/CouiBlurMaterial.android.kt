// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.blur

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb

/**
 * The COUI material as **two chained RuntimeShaders** — the same pipeline
 * `libhwui.so`'s `OplusUniBackdropFilterDrawable` runs:
 *
 * ```
 * backdrop ──> BLUR (animated gaussian, verbatim) ──> MIX (luminosity + overlay/dodge, verbatim)
 * ```
 *
 * The old `createBlurEffect + createColorFilterEffect` chain is gone for good: it is spatially
 * uniform, so it can never produce the official fog's `ratio²` vertical falloff, and its blur has
 * no `radius = f(amount)` animation hook. Both shaders below are byte-for-byte transcriptions of
 * the platform's own AGSL (see [CouiPlatformBlurShaders] for the extraction provenance).
 *
 * Falls back to the old RenderEffect chain below API 33 (RuntimeShader needs `AGSL`, which
 * shipped in Tiramisu) and to nothing below API 31.
 */
@Composable
actual fun CouiBlurMaterial(
    params: CouiBlurParams,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val effect = remember(params) { params.toRuntimeShaderEffectOrNull() }
    Box(modifier.graphicsLayer { renderEffect = effect?.asComposeRenderEffect() }) {
        content()
    }
}

/** True when the two-shader pipeline is available (`RuntimeShader` needs API 33). */
internal fun platformShaderBlurAvailable(): Boolean = Build.VERSION.SDK_INT >= 33

/**
 * Builds the blur→mix RuntimeShader chain for [params].
 *
 * `null` on platforms without AGSL, or when the official would clear the effect (`NO_EFFECT`).
 */
internal fun CouiBlurParams.toRuntimeShaderEffectOrNull(): android.graphics.RenderEffect? {
    if (!platformShaderBlurAvailable()) return toRenderEffectCompatOrNull()
    if (isNoEffect) return null
    return runCatching {
        val blur = RuntimeShader(CouiPlatformBlurShaders.BLUR)
        val mix = RuntimeShader(CouiPlatformBlurShaders.MIX)
        // `in_speed` controls how much of the radius is applied. The official animates it while
        // the surface moves; for a settled surface the full radius is wanted, and `ratio = 1`
        // (speed -> inf) is the only value that yields exactly that for every radius.
        blur.setFloatUniform("in_blurRadius", blurRadiusPx)
        blur.setFloatUniform("in_speed", 64f)
        blur.setFloatUniform("in_blurOffset", 1f, 1f)
        // The official feeds the frame's dither config; 2.0 puts the +-0.5/2 lsb-level noise at
        // the same amplitude the Kawase passes use, and the random seed is stable per params so
        // the grain does not crawl frame to frame.
        blur.setFloatUniform("in_ditherValue", 2f)
        blur.setFloatUniform("in_random", blurRadiusPx * 0.6180339887f)
        mix.setFloatUniform("uBlendColor", colorLayer1.toFloatArray())
        mix.setFloatUniform("uMixColor", colorLayer2.toFloatArray())
        mix.setFloatUniform("uRatios", 1f, 0f)
        mix.setIntUniform("uBlendIsDodge", if (blendModeLayer1 == CouiBlendMode.ColorDodge) 1 else 0)
        val blurEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(blur, "child")
        val mixEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(mix, "child")
        // Chain: input -> blur -> mix. `createRuntimeShaderEffect(shader, "child")` feeds the
        // previous effect into the shader's `child` input.
        android.graphics.RenderEffect.createChainEffect(mixEffect, blurEffect)
    }.onFailure {
        android.util.Log.w("CouiBlur", "runtime shader chain failed: ${it.message}")
    }.getOrNull()
}

/** `ViewBlurProxy.colorToFloatArray`: premultiplied RGBA scaled to 0..1, in that order. */
private fun Color.toFloatArray(): FloatArray = floatArrayOf(red, green, blue, alpha)

/**
 * The pre-AGSL fallback: the old uniform chain. Kept so the surface still reads as a material on
 * API 31/32 - just without the fog gradient and the animated radius.
 */
private fun CouiBlurParams.toRenderEffectCompatOrNull(): android.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    if (isNoEffect) return null
    var effect =
        android.graphics.RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, android.graphics.Shader.TileMode.MIRROR)
    effect = android.graphics.RenderEffect.createColorFilterEffect(
        android.graphics.BlendModeColorFilter(colorLayer1.toArgb(), blendModeLayer1.toAndroidBlendMode()),
        effect,
    )
    effect = android.graphics.RenderEffect.createColorFilterEffect(
        android.graphics.BlendModeColorFilter(colorLayer2.toArgb(), blendModeLayer2.toAndroidBlendMode()),
        effect,
    )
    return effect
}

internal fun CouiBlendMode.toAndroidBlendMode(): android.graphics.BlendMode =
    when (this) {
        CouiBlendMode.SrcOver -> android.graphics.BlendMode.SRC_OVER
        CouiBlendMode.Overlay -> android.graphics.BlendMode.OVERLAY
        CouiBlendMode.ColorDodge -> android.graphics.BlendMode.COLOR_DODGE
        CouiBlendMode.Luminosity -> android.graphics.BlendMode.LUMINOSITY
    }