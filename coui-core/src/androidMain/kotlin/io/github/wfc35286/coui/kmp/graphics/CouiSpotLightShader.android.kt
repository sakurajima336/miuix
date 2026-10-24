// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import android.annotation.TargetApi
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.ShaderBrush

/** Android actual: `android.graphics.RuntimeShader`, available from API 33 (Android 13). */
internal actual fun createCouiSpotLightShader(): CouiSpotLightShader? = if (Build.VERSION.SDK_INT >= 33) AndroidCouiSpotLightShader() else null

internal actual fun spotLightBrush(shader: CouiSpotLightShader): ShaderBrush = (shader as AndroidCouiSpotLightShader).brush

/**
 * The shader keeps the packed vec4s in sync itself, because the AGSL source reads
 * `u_lightPosAndHeight.zw` for radius/intensity and `u_lightColorAndAmbient.w` for the ambient
 * term - exactly the packing `COUISpotLightEffect` uses. The original batches dirty setters
 * in `flushIfDirty`; this port keeps the same packed uniforms current as setters are called.
 *
 * `setLightPosition` takes **normalized** coordinates (0..1), matching
 * `SpotLightShader.setLightPosition`; the multiply by the resolution that `flushIfDirty` does
 * happens here, in [pushLightPos]. `mLevel` is a constant 1.0 in the official
 * (`SpotLightShader.<init>`), so radius and intensity are passed through unchanged.
 */
@TargetApi(33)
internal class AndroidCouiSpotLightShader : CouiSpotLightShader {
    val runtimeShader = RuntimeShader(COUI_SPOT_LIGHT_SHADER_SOURCE)
    val brush = ShaderBrush(runtimeShader)

    private val shader = runtimeShader

    private var normX = 0f
    private var normY = 0f
    private var radius = 0f
    private var intensity = 0f
    private var ambient = 0f
    private var diffuse = 0f
    private var lightHeight = 0f
    private var noise = 0f
    private var ditherN = 0f
    private var colorR = 1f
    private var colorG = 1f
    private var colorB = 1f
    private var resolutionW = 0f
    private var resolutionH = 0f

    /** `flushIfDirty`'s `u_lightPosAndHeight` block. */
    private fun pushLightPos() =
        shader.setFloatUniform(
            "u_lightPosAndHeight",
            normX * resolutionW,
            normY * resolutionH,
            radius,
            intensity,
        )

    private fun pushLighting() = shader.setFloatUniform("u_lightingParams", diffuse, lightHeight, noise, ditherN)

    private fun pushLightColor() =
        shader.setFloatUniform("u_lightColorAndAmbient", colorR, colorG, colorB, ambient)

    override fun setLightPosition(x: Float, y: Float) {
        this.normX = x
        this.normY = y
        pushLightPos()
    }

    override fun setLightRadius(radius: Float) {
        this.radius = radius
        pushLightPos()
    }

    override fun setIntensity(intensity: Float) {
        this.intensity = intensity
        pushLightPos()
    }

    override fun setLightColor(r: Float, g: Float, b: Float) {
        this.colorR = r
        this.colorG = g
        this.colorB = b
        pushLightColor()
    }

    override fun setAmbientStrength(strength: Float) {
        ambient = strength
        pushLightColor()
    }

    override fun setDiffuseStrength(strength: Float) {
        diffuse = strength
        pushLighting()
    }

    override fun setLightHeight(height: Float) {
        lightHeight = height
        pushLighting()
    }

    override fun setNoise(value: Float) {
        noise = value
        pushLighting()
    }

    override fun setDitherN(value: Float) {
        ditherN = value
        pushLighting()
    }

    override fun setResolution(width: Float, height: Float) {
        resolutionW = width
        resolutionH = height
        shader.setFloatUniform("resolution", width, height)
        // `flushIfDirty` marks the position dirty when the resolution changes, because the packed
        // vec4 embeds the product.
        pushLightPos()
    }
}
