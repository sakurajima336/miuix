// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import android.annotation.TargetApi
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

internal actual fun createCouiCardShader(): CouiCardShader? = if (Build.VERSION.SDK_INT >= 33) AndroidCouiCardShader() else null

@TargetApi(33)
private class AndroidCouiCardShader : CouiCardShader {
    private val shader = RuntimeShader(COUI_CARD_SHADER_SOURCE)
    private val shaderBrush = ShaderBrush(shader)

    override fun brush(size: Size, radius: Float, color: Color): ShaderBrush {
        shader.setFloatUniform("halfSize", size.width / 2f, size.height / 2f)
        shader.setFloatUniform("radius", radius)
        shader.setColorUniform("fillColor", color.toArgb())
        return shaderBrush
    }
}
