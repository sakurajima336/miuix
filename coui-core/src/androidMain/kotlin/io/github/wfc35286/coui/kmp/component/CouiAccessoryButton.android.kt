// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import android.graphics.BlendMode
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

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

/**
 * `TYPE_FRAMEWORK_BOTTOM_BAR` on a circle - see the expect for the evidence trail.
 *
 * The pipeline is the pill's own ([panelBlurEffect]) with the same spec, so the two surfaces can
 * never drift apart. What is added here is the **backdrop capture** the official gets for free
 * from `OplusViewBackgroundRenderEffect`: the effect is attached to a layer that first paints the
 * captured page, so the blur reads the page and not the button.
 */
@Composable
internal actual fun Modifier.couiAccessoryMaterial(
    dark: Boolean,
    backdrop: CouiBackdrop?,
    backgroundColor: Color,
    scaleX: () -> Float,
    scaleY: () -> Float,
): Modifier {
    val spec = if (dark) CouiFloatingMaterialDark else CouiFloatingMaterialLight
    val density: androidx.compose.ui.unit.Density = LocalDensity.current
    val marginPx = remember(spec) {
        kotlin.math.ceil(spec.blurRadiusPx * 1.4143f).toInt() + 1
    }
    var backdropOrigin by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    // A circle is just a capsule whose corners meet, so the pill's shape maths applies unchanged.
    val circle = remember { androidx.compose.foundation.shape.RoundedCornerShape(percent = 50) }
    if (backdrop == null || Build.VERSION.SDK_INT < 33) {
        // The degradation the official *should* have: a flat panel instead of nothing at all.
        return this.background(color = backgroundColor, shape = circle)
    }
    // The circle is a **fixed** 56dp square (`onMeasure` ignores the incoming spec), so its size is
    // known without measuring. Reading it back from `onSizeChanged` instead fed the expanded size
    // into the expansion, which grew the layer a little every frame until it covered the screen -
    // and it recomposed the whole modifier chain on every measure pass, which is the stutter.
    val surfaceSize = remember(density) {
        val side = with(density) { CouiAccessoryButtonDefaults.Size.roundToPx() }
        IntSize(side, side)
    }
    val effect = remember(spec, surfaceSize, marginPx) { panelBlurEffect(spec, surfaceSize, marginPx) }
    val expandedWidth = surfaceSize.width + 2 * marginPx
    val expandedHeight = surfaceSize.height + 2 * marginPx
    return this
        // The material's minimum fill plus the final silhouette. Both sit **outside** the expanded
        // sampling layer, so they are measured at the button's own 56dp and the layer's overspill is
        // clipped away instead of resizing the circle.
        //
        // The order used to be the reverse - the expanded layout modifier outermost and the
        // clip/background inside it - which meant every node outside the expansion measured the
        // *expanded* box: a percent-50 rounded corner shape then drew a circle of
        // 56dp + 2 * margin (81dp at this density) instead of 56dp. The accessory rendered as a
        // soft blob nearly 1.5x the capsule's height, which is exactly the reported
        // button-to-pill ratio defect.
        .clip(circle)
        // The **base** the blur is composited over - the official's `applyFallbackBackgroundColor()`
        // (`COUIBottomFloatingAccessoryView.smali:294-322`), which only paints while
        // `isMaterialQualityEnable()` is false. On a material device the caller's colour is
        // therefore *under* the blurred surface, never on top of it; painting it above the
        // material flattened the circle to a solid disc.
        .background(backgroundColor)
        // The material's own minimum fill, directly under the blur: the capture can be
        // translucent and both shaders pass the sample's alpha straight through, so without it the
        // circle would be as see-through as the page. Same layer the pill paints.
        .background(Color(spec.layer2Color))
        .then(
            if (effect == null) {
                Modifier
            } else {
                // The layer that carries the render effect is expanded by the kernel's reach so the
                // blur has real pixels to average *outside* the silhouette - but it must still
                // **report** the button's own size, so the enclosing clip can cut the overspill
                // back to the circle. Reporting the expanded size is what let the circle grow.
                Modifier
                    .layout { measurable, _ ->
                        val w = expandedWidth.coerceAtLeast(1)
                        val h = expandedHeight.coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(w, h))
                        val side = surfaceSize.width.coerceAtLeast(1)
                        layout(side, side) { placeable.place(-marginPx, -marginPx) }
                    }
                    .graphicsLayer { renderEffect = effect }
                    .onGloballyPositioned { backdropOrigin = it }
                    .drawBehind {
                        drawCouiBackdropLayer(
                            backdrop = backdrop,
                            origin = backdropOrigin,
                            // The button's own scale layer sits between the page and this draw,
                            // so the sample must be taken with the inverse - the pill passes the
                            // same pair for the same reason (`CouiFloatingMaterial.android.kt:335`).
                            scaleX = scaleX(),
                            scaleY = scaleY(),
                        )
                    }
            },
        )
}

/**
 * `drawPanelStroke()` for the round button - the rim highlight and the inner glow.
 *
 * This is a **sibling** of the blur layer, never a modifier inside it. `couiAccessoryMaterial`
 * ends with a `graphicsLayer { renderEffect }`, and in a Compose modifier chain every node added
 * *after* it draws **inside** that layer - so a stroke hung there is blurred along with the
 * backdrop. That is exactly what made the first two attempts look almost blank: the rim was being
 * run through the same Gaussian kernel as the page behind the button.
 *
 * The official has the same separation: `COUIBottomFloatingAccessoryView.onDraw` only draws the
 * touch effect and the icon (`:2454-2468`), while the rim comes from the **view background**
 * (`updateStrokeEffect()` -> `COUIMaterialEffect.setViewStrokeEffect` ->
 * `OplusMaterialUtil.setEdgeParams` + `setShadowParams`), which the framework composites above the
 * blurred background and below the content.
 *
 * `drawCouiShadowEdge` feeds `shader.setSize(size)` from this DrawScope, so the node must report the
 * button's own 56dp - `COUIShadowEdgeDrawable.onBoundsChange` uses the view bounds.
 *
 * ## Which preset, and why it is `CAPSULE_12` and not the button's `CAPSULE_3`
 *
 * `COUIBottomFloatingAccessoryView.updateStrokeEffect()` asks the framework for
 * `TYPE_FRAMEWORK_CAPSULE_3` (`:1904-1928`), whose params are edge alpha **0.8 dark / 0.25 light**
 * at 2.5dp. Feeding those straight into this AGSL port renders *far* too bright: the framework's
 * own edge rasteriser is a different implementation, and its numbers are not transferable.
 *
 * The official shows the correct exchange rate itself. `BaseBottomFloatingMenuView`
 * `.updateStrokeEffect()` (`:1839-1879`) draws the same rim two ways - through
 * `COUIShadowEdgeDrawable` with `CAPSULE_PARAMS_12`, or, when that is unavailable, through the
 * framework with `CAPSULE_3`. It treats the two as visually equivalent, which means **`CAPSULE_12`
 * is the parameter set this shader expects**.
 *
 * So the button reuses [CouiFloatingMaterialLight] / [CouiFloatingMaterialDark] verbatim. What
 * still makes it look different from the pill is geometry, not alpha: on a 56dp square
 * `rectLineFadeForSize` normalises by `w + h` instead of a long capsule's width, which turns the
 * narrow top line into a broad arc covering roughly a third of the perimeter.
 */
@Composable
internal actual fun Modifier.couiAccessoryEdge(dark: Boolean): Modifier {
    if (!CouiShadowEdge.isSupported()) return this
    val density = LocalDensity.current
    // `StrokeEffectParams.getEdgeWidth(context)` is a plain `dp2px(edgeWidthDp)`; `CAPSULE_12` is
    // 2.0dp in both themes, the same value the pill passes.
    val strokeSpec =
        remember(dark, density) {
            val base = if (dark) CouiFloatingMaterialDark else CouiFloatingMaterialLight
            base.copy(edgeWidthPx = with(density) { COUI_PANEL_EDGE_WIDTH_DP.dp.toPx() })
        }
    val shader = rememberCouiShadowEdgeShaderForAccessory() ?: return this
    return this.drawWithContent {
        drawContent()
        drawCouiShadowEdge(
            shader = shader,
            spec = strokeSpec,
            density = density.density,
        )
    }
}

/**
 * One `CouiShadowEdge.Shader` for the accessory, created lazily and reused.
 *
 * `COUIBottomFloatingAccessoryView.initMaterialAppearance()` calls `updateStrokeEffect()`, and the
 * official keeps exactly one `COUIShadowEdgeDrawable` behind it (`initShadowEdgeDrawableIfNeeded()`,
 * `BaseBottomFloatingMenuView.smali`), so the shader is remembered rather than rebuilt per frame.
 * `null` when the platform cannot run AGSL (`CouiShadowEdge.isSupported()` gates on `SDK_INT > 33`,
 * the official's `COUIShadowEdgeDrawable.<init>` check) - in which case the official draws no rim
 * either.
 */
@Composable
private fun rememberCouiShadowEdgeShaderForAccessory(): CouiShadowEdge.Shader? {
    if (!CouiShadowEdge.isSupported()) return null
    return remember { runCatching { CouiShadowEdge.Shader() }.getOrNull() }
}
