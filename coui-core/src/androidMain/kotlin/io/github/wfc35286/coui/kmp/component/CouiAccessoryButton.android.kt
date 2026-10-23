// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import android.graphics.BlendMode
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity

/**
 * `COUIBottomFloatingAccessoryView.setCausticShadowEffect()` - the round button's **level-5**
 * caustic (`COUIBottomFloatingAccessoryView.smali:1768-1843`).
 *
 * The wiring is identical to the pill's level-4 glow (see `drawCouiCausticShadow` in
 * `CouiFloatingMaterial.android.kt`); only the level and the silhouette differ:
 *
 * | | pill | accessory |
 * |---|---|---|
 * | level | 4 | **5** |
 * | `coui_caustic_layout_b` | 4.9dp | **11.199982dp** |
 * | `coui_caustic_layout_sdf` | 10.4dp | **20.0dp** |
 * | `coui_caustic_layout_l/t/r` | 0 / 0 / 0 | 0 / 0 / 0 |
 * | colour | `coui_caustic_color_2` | `coui_caustic_color_2` |
 * | old-shadow elevation | 7 | 7 (`:1773`) |
 *
 * The level comes from `CouiCausticShadow.Level.Level5` and the colour from
 * `CouiCausticShadow.Colors.*_2`; the shader algorithm itself is untouched.
 */
@Composable
internal actual fun Modifier.couiAccessoryCaustic(dark: Boolean): Modifier {
    // `SkCanvas::onDrawCausticShadow` is reached through a runtime shader, i.e. SDK 33 upwards.
    if (Build.VERSION.SDK_INT < 33) return this
    val shader = remember {
        runCatching { android.graphics.RuntimeShader(CouiCausticShadow.SHADER) }.getOrNull()
    } ?: return this
    val density = LocalDensity.current.density
    val backdrop = remember { couiAccessoryCausticBackdrop() }
    return this.drawBehind {
        drawCouiAccessoryCausticShadow(shader, backdrop, density, size.width, size.height, dark)
    }
}

/**
 * A 1x1 opaque-black backdrop, exactly what the pill's caustic uses when no live capture is bound.
 *
 * The official multiplies the glow by `(1 - gray)^2` with
 * `gray = clamp((luma - 0.45) * 0.6, 0, 1)`; a black sample means `gray = 0`, i.e. full strength -
 * the dark-page case every shipping caller draws this effect over.
 */
private fun couiAccessoryCausticBackdrop(): Shader {
    val pixel = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
    pixel.eraseColor(android.graphics.Color.BLACK)
    return android.graphics.BitmapShader(pixel, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
}

/** Paints [CouiCausticShadow.Level.Level5] behind the circle. */
private fun DrawScope.drawCouiAccessoryCausticShadow(
    shader: android.graphics.RuntimeShader,
    backdrop: Shader,
    density: Float,
    w: Float,
    h: Float,
    dark: Boolean,
) {
    if (w <= 0f || h <= 0f) return
    val params = CouiCausticShadow.level(
        level = CouiCausticShadow.Level.Level5,
        density = density,
        color = if (dark) CouiCausticShadow.Colors.DARK_2 else CouiCausticShadow.Colors.LIGHT_2,
    )
    // Straight (non-premultiplied) RGBA; the shader premultiplies its own output.
    val a = ((params.color ushr 24) and 0xFF) / 255f
    val r = ((params.color shr 16) and 0xFF) / 255f
    val g = ((params.color shr 8) and 0xFF) / 255f
    val b = (params.color and 0xFF) / 255f
    shader.setInputShader("uBackdrop", backdrop)
    shader.setFloatUniform("uColor", r, g, b, a)
    shader.setFloatUniform("uLayout", params.leftPx, params.topPx, params.rightPx, params.bottomPx)
    shader.setFloatUniform("uParams", params.sdfPx, params.factor0, params.factor1, params.speed)
    shader.setFloatUniform("uLum", params.luminanceThreshold, params.luminanceIntensity)
    shader.setFloatUniform("uRectLT", 0f, 0f)
    shader.setFloatUniform("uRectSize", w, h)
    // `onLayout` derives `mRadius = (measuredH - padT - padB) / 2` and hands it to
    // `OplusMaterialCornerParams(mRadius, 2.0f)` (`:2477-2509`): a full-radius rounded rect, i.e.
    // a circle. With `corner = h / 2` the box SDF is the circle SDF.
    shader.setFloatUniform("uCorner", h / 2f)
    shader.setIntUniform("uOutlineType", 0)
    // The shader's own bounding-box test clamps everything outside the optics rect to zero, so a
    // hair of slack around it is all the draw rect needs.
    val slack = 1f
    val paint = android.graphics.Paint().apply {
        this.shader = shader
        blendMode = BlendMode.PLUS
    }
    drawContext.canvas.nativeCanvas.drawRect(
        -slack,
        -slack,
        w + slack,
        h + params.bottomPx + params.sdfPx + slack,
        paint,
    )
}
