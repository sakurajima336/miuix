// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

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
 */
@TargetApi(33)
internal class AndroidCouiSpotLightShader : CouiSpotLightShader {
    val runtimeShader = RuntimeShader(COUI_SPOT_LIGHT_SHADER_SOURCE)
    val brush = ShaderBrush(runtimeShader)

    private val shader = runtimeShader

    private var x = 0f
    private var y = 0f
    private var radius = 0f
    private var intensity = 0f
    private var ambient = 0f
    private var diffuse = 0f
    private var lightHeight = 0f
    private var noise = 0f
    private var ditherN = 0f

    private fun pushLightPos() = shader.setFloatUniform("u_lightPosAndHeight", x, y, radius, intensity)

    private fun pushLighting() = shader.setFloatUniform("u_lightingParams", diffuse, lightHeight, noise, ditherN)

    override fun setLightPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
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

    override fun setLightColor(r: Float, g: Float, b: Float) = shader.setFloatUniform("u_lightColorAndAmbient", r, g, b, ambient)

    override fun setAmbientStrength(strength: Float) {
        ambient = strength
        shader.setFloatUniform("u_lightColorAndAmbient", 1f, 1f, 1f, ambient)
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

    override fun setResolution(width: Float, height: Float) = shader.setFloatUniform("resolution", width, height)
}
