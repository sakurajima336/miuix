// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder


/** Skiko actual: the same AGSL source compiled through Skia's `RuntimeEffect`. */
internal actual fun spotLightBrush(shader: CouiSpotLightShader): ShaderBrush = ShaderBrush((shader as SkikoCouiSpotLightShader).makeShader().asComposeShader())

internal actual fun createCouiSpotLightShader(): CouiSpotLightShader? = runCatching { SkikoCouiSpotLightShader() }.getOrNull()

internal class SkikoCouiSpotLightShader : CouiSpotLightShader {
    private val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(COUI_SPOT_LIGHT_SHADER_SOURCE))

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

    /** `flushIfDirty`'s `u_lightPosAndHeight` block: normalized position times the resolution. */
    private fun pushLightPos() = builder.uniform("u_lightPosAndHeight", normX * resolutionW, normY * resolutionH, radius, intensity)

    private fun pushLighting() = builder.uniform("u_lightingParams", diffuse, lightHeight, noise, ditherN)

    private fun pushLightColor() = builder.uniform("u_lightColorAndAmbient", colorR, colorG, colorB, ambient)

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
        builder.uniform("resolution", width, height)
        // The packed vec4 embeds the product, so the position has to be re-pushed.
        pushLightPos()
    }

    fun makeShader() = builder.makeShader()
}
