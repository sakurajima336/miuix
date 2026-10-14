// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun createCouiCapsuleShader(): CouiCapsuleShader? = SkikoCouiCapsuleShader()

private class SkikoCouiCapsuleShader : CouiCapsuleShader {
    private val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(COUI_CAPSULE_SHADER_SOURCE))

    override fun brush(size: Size, color: Color): ShaderBrush {
        builder.uniform("halfSize", size.width / 2f, size.height / 2f)
        val srgb = color.convert(ColorSpaces.Srgb)
        val alpha = srgb.alpha
        builder.uniform("fillColor", srgb.red, srgb.green, srgb.blue, alpha)
        return ShaderBrush(builder.makeShader().asComposeShader())
    }
}
