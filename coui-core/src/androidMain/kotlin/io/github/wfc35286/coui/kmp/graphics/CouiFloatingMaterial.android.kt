// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.wfc35286.coui.kmp.motion.couiComposeScale
import kotlin.math.ceil
import kotlin.math.roundToInt


/**
 * The ColorOS material, driven through the same framework entry points the official widget uses.
 *
 * Everything the official `BaseBottomFloatingMenuView` does to its own `View` is reproduced here,
 * call for call:
 *
 * 1. `COUIMaterialBlurEffect$Companion.setViewBlurEffect` builds
 *    `RenderEffect.createBlurEffect(r, r, MIRROR)` and stacks two
 *    `createColorFilterEffect(BlendModeColorFilter(color, blendMode))` on top, then hands the chain
 *    to `com.oplus.view.OplusViewBackgroundRenderEffect.setBackgroundRenderEffect(effect, view)`.
 *    Oplus' HWUI turns that into a **backdrop** blur - it samples what is painted behind the view
 *    (`OplusUniBackdropFilterDrawable` in libhwui), which is why the pill shows the page through a
 *    soft wash instead of a flat fill.
 * 2. `COUIMaterialStrokeEffect` maps `StrokeEffectParams` onto
 *    `OplusMaterialUtil.setEdgeParams(view, OplusMaterialEdgeParams(type, widthPx, alpha, angle))`
 *    (the inner hairline) and
 *    `OplusMaterialUtil.setShadowParams(view, OplusMaterialShadowParams(type, fadeScale, fadeIn,
 *    fadeOut))` (the inner shadow gradient).
 * 3. `COUIBottomFloatingNavigationBarMenuView.updateEdgeBounds()` feeds the silhouette to
 *    `OplusMaterialUtil.setBaseParams(view, OplusMaterialBaseParams(0, innerBounds))`.
 * 4. `BaseBottomFloatingMenuView.setCausticShadowEffect()` adds the outer shadow:
 *    `ShadowUtils.setElevationToView(view, 7)` (the `caustic_down` preset: elevation 24dp, light
 *    `y = 16776896`, `z = 0`, `r = 1600`, blur 0) plus
 *    `OplusMaterialUtil.setOutlineCausticShadow{Color,Layout,Params}` with `#42ffffff`,
 *    `l/t/r = 0`, `b = 4.9dp`, `sdf = 10.4dp`.
 * 5. `applySmoothCornerOutlineState()` sets `OplusMaterialCornerParams(mRadius, 1.0f)` and leaves
 *    `clipToOutline = false`: the material owns the silhouette, not the View.
 *
 * Every call is reflective on purpose: `com.oplus.view.*` only exists on ColorOS, and the library
 * must still build (and degrade) everywhere else. All the entry points are marked `whitelist` in
 * the Oplus framework - ordinary public API that simply is not part of the Android SDK.
 */
/**
 * The panel's backdrop blur, as the platform's own **two-shader AGSL chain**.
 *
 * Verbatim transcription of what `libhwui.so`'s `OplusUniBackdropFilterDrawable` runs:
 * `animated gaussian blur` -> `luminosity + overlay/colorDodge material mix`. The old
 * `createBlurEffect + createColorFilterEffect` chain could never reproduce the fog - its filters
 * are spatially uniform, while the official mix carries a `ratio²` vertical falloff and a
 * runtime radius. Both shaders live in [CouiPlatformBlurShaders] with their extraction
 * provenance; `null` below API 33 (AGSL) or when the official would clear the effect.
 */
internal fun panelBlurEffect(
    spec: CouiFloatingMaterialSpec,
    surfaceSize: IntSize = IntSize.Zero,
    marginPx: Int = 0,
): androidx.compose.ui.graphics.RenderEffect? =
    CouiPlatformBlurPipeline.materialEffect(
        blurRadiusPx = spec.blurRadiusPx,
        layer1 = spec.layer1Color.toMaterialColor(),
        layer2 = spec.layer2Color.toMaterialColor(),
        layer1IsDodge = spec.layer1Blend == CouiBlendMode.ColorDodge,
        surfaceWidthPx = surfaceSize.width.toFloat(),
        surfaceHeightPx = surfaceSize.height.toFloat(),
        coordOffsetPx = marginPx.toFloat(),
    )?.asComposeRenderEffect()

private fun Int.toMaterialColor(): Color = Color(this)

/**
 * The two-shader pipeline, adapted from [CouiPlatformBlurShaders]
 * but living here so the navigation bar can drive the uniforms directly (the fog's direction and
 * extent belong to the pill, not to a generic blur surface).
 */
internal object CouiPlatformBlurPipeline {
    fun materialEffect(
        blurRadiusPx: Float,
        layer1: Color,
        layer2: Color,
        layer1IsDodge: Boolean,
        surfaceWidthPx: Float,
        surfaceHeightPx: Float,
        coordOffsetPx: Float = 0f,
    ): android.graphics.RenderEffect? {
        if (Build.VERSION.SDK_INT < 33) return null
        if (blurRadiusPx <= 0f && layer1.alpha == 0f && layer2.alpha == 0f) return null
        return runCatching {
            val mix = android.graphics.RuntimeShader(CouiPlatformBlurShaders.MIX)
            mix.setFloatUniform("uBlendColor", layer1.red, layer1.green, layer1.blue, layer1.alpha)
            mix.setFloatUniform("uMixColor", layer2.red, layer2.green, layer2.blue, layer2.alpha)
            // The fog runs over the pill's height, bottom -> top: `uDirection == 1` with
            // `uRatios = (0, 1)` gives ratio = coord.y/uSize, i.e. strongest at the BOTTOM edge
            // (the official bar's fog hugs the bottom and fades upward). An earlier (1, 0) put
            // the bright wash at the top - visibly inverted against the official.
            mix.setFloatUniform("uRatios", 0f, 1f)
            mix.setFloatUniform("uSize", surfaceHeightPx.coerceAtLeast(1f))
            mix.setIntUniform("uDirection", 1)
            mix.setIntUniform("uBlendIsDodge", if (layer1IsDodge) 1 else 0)
            // The sampled layer is expanded by the kernel's reach around the pill (see
            // `CouiFloatingMaterialSurface`); the gradient's origin has to move back by the same
            // amount so the fog still measures from the pill's own top edge.
            mix.setFloatUniform("uCoordOffset", coordOffsetPx, coordOffsetPx)
            val mixEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(mix, "child")

            // The platform's blur pass is a **1-D** gaussian: it walks `in_blurOffset * i` in
            // both directions, so `in_blurOffset` is the kernel's axis, not a diagonal step.
            // Running it once with (1, 1) - the previous revision - samples a single 45-degree
            // line through the pixel and smears everything along that line: that is the
            // "astigmatism / brushed streaks" look, not a blur. libhwui applies the pass once
            // per axis; a separable pair is also exactly equal to a 2-D gaussian, so the result
            // is isotropic.
            var blurred: android.graphics.RenderEffect? = null
            if (blurRadiusPx > 1f) {
                val horizontal = blurPass(blurRadiusPx, 1f, 0f)
                val vertical = blurPass(blurRadiusPx, 0f, 1f)
                blurred = android.graphics.RenderEffect.createChainEffect(vertical, horizontal)
            }
            if (blurred == null) {
                mixEffect
            } else {
                // outer(child = inner): mix over the two blur passes.
                android.graphics.RenderEffect.createChainEffect(mixEffect, blurred)
            }
        }.onFailure {
            android.util.Log.w("CouiPanel", "platform blur pipeline failed: ${it.message}")
        }.getOrNull()
    }

    /**
     * One axis of the separable gaussian: the platform's animated blur shader with
     * `in_blurOffset` pinned to the axis.
     */
    private fun blurPass(
        blurRadiusPx: Float,
        offsetX: Float,
        offsetY: Float,
    ): android.graphics.RenderEffect {
        val blur = android.graphics.RuntimeShader(CouiPlatformBlurShaders.BLUR)
        blur.setFloatUniform("in_blurRadius", blurRadiusPx)
        blur.setFloatUniform("in_speed", 64f)
        blur.setFloatUniform("in_blurOffset", offsetX, offsetY)
        // The shader adds `noise / in_ditherValue` with noise in [-0.5, 0.5], so ditherValue
        // is the **divisor**: 128 puts the noise at +-0.5/128 = ~1/255 - one LSB, exactly the
        // band-breaking amplitude the official dither pass runs at. (2.0 - an early guess -
        // was +-25% luminance, which read as a crawling checkerboard the moment the
        // backdrop moved.)
        blur.setFloatUniform("in_ditherValue", 128f)
        // A *fixed* per-pass random offset: the dither pattern must not crawl with the frame
        // counter, otherwise the blur visibly boils while the backdrop moves. The two axes get
        // different, irrational offsets so their noise patterns do not line up.
        blur.setFloatUniform("in_random", blurRadiusPx * (0.6180339887f + offsetX * 0.37f))
        return android.graphics.RenderEffect.createRuntimeShaderEffect(blur, "child")
    }
}

/** `coui_shadow_elevation_caustic_down`. */
private const val COUI_CAUSTIC_ELEVATION_DP = 24f
/** `coui_shadow_light_{y,z,r}_caustic_down` + `coui_shadow_blur_r_caustic_down`. */
private const val COUI_CAUSTIC_LIGHT_Y = 16776896f
private const val COUI_CAUSTIC_LIGHT_Z = 0f
private const val COUI_CAUSTIC_LIGHT_R = 1600f
private const val COUI_CAUSTIC_LIGHT_BLUR = 0f

private val CouiCapsuleShape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)

internal actual fun isCouiFloatingMaterialAvailable(): Boolean = true

@Composable
internal actual fun CouiFloatingMaterialSurface(
    spec: CouiFloatingMaterialSpec,
    enabled: Boolean,
    backdrop: CouiBackdrop?,
    translation: () -> Offset,
    pressScale: () -> Float,
    scaleX: () -> Float,
    scaleY: () -> Float,
    transformOrigin: () -> Offset,
    shadowEnabled: Boolean,
    dark: Boolean,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val scaleXProvider = scaleX
    val scaleYProvider = scaleY
    val pressScaleProvider = pressScale
    val density = LocalDensity.current
    val densityScale = density.density

    // ---- captured backdrop: Compose-side blur ------------------------------------------------
    if (enabled && backdrop != null) {
        var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
        // **The layer the blur runs on has to be larger than the pill.** `RenderEffect` only sees
        // the pixels inside the layer it is attached to, so a pill-sized layer leaves the kernel
        // with nothing outside the silhouette: the outermost ~3 sigma never get blurred, and the
        // rim ends up looking sharp/"eaten". Expanding by the kernel's reach (3 sigma of the
        // separable pair = 3 * sqrt(2) * R / 3 = sqrt(2) * R) gives the effect real pixels to
        // average; the result is then clipped back to the capsule by the enclosing node.
        val marginPx = remember(spec) { ceil(spec.blurRadiusPx * 1.4143f).toInt() + 1 }
        val effect = remember(spec, surfaceSize, marginPx) { panelBlurEffect(spec, surfaceSize, marginPx) }
        // The caustic shader gets its own instance; its uniforms are per-surface constants.
        val caustic = remember {
            if (Build.VERSION.SDK_INT >= 33) {
                runCatching { android.graphics.RuntimeShader(CouiCausticShadow.SHADER) }.getOrNull()
            } else {
                null
            }
        }
        // The official shader reads the page under each fragment to decide how much of the glow
        // survives (alpha *= (1 - gray)^2). Sampling a live backdrop costs a GPU readback per
        // frame, so the fallback binds a 1x1 black shader: that is exactly the official's
        // "dark page" case (gray = 0, full strength), which is the appearance the component is
        // designed around. Passing a real backdrop shader is a drop-in upgrade.
        val causticBackdrop = remember { blackCausticBackdrop() }
        if (effect != null) {
            // Where the sampling layer's drawing origin actually sits (measured, not derived); see
            // `drawCouiBackdropLayer`.
            var backdropOrigin by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(modifier = modifier.onSizeChanged { surfaceSize = it }) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .offset {
                                val t = translation()
                                IntOffset(t.x.roundToInt(), t.y.roundToInt())
                            }
                            .graphicsLayer {
                                // The official does **not** multiply the two channels:
                                // `TouchMotionCollector.put` accumulates `mScaleX += value - 1`,
                                // so press and deform are summed about 1.0. Multiplying makes a
                                // shrinking press fight a stretching deform - a drag barely
                                // stretches because the pill is shrunk at the same time.
                                this.scaleX = couiComposeScale(pressScaleProvider(), scaleXProvider())
                                this.scaleY = couiComposeScale(pressScaleProvider(), scaleYProvider())
                                this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                            },
                ) {
                    // `setCausticShadowEffect()`: the outline glow lives OUTSIDE the pill - it spills
                    // from under the bottom edge onto the page. Drawn behind everything in this Box
                    // via drawBehind; Compose nodes don't clip out-of-bounds drawing unless a clip
                    // modifier is applied, and none is on this Box, so the spill stays visible.
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .drawBehind {
                                    caustic?.let {
                                        drawCouiCausticShadow(it, causticBackdrop, densityScale, surfaceSize, dark)
                                    }
                                },
                    )
                    // The clip lives on **this** node and the effect on the child, in that order:
                    // a single merged layer would clip the content *before* blurring it, which
                    // both darkens the edge and starves the kernel of the pixels just outside the
                    // silhouette.
                    Box(modifier = Modifier.matchParentSize().clip(CouiCapsuleShape)) {
                        // The official's backdrop filter always samples an **opaque** window (the
                        // page is painted underneath), so its mix output is opaque too. Our capture
                        // can be transparent - e.g. when the caller's `Modifier.couiBackdrop` sits
                        // *inside* the page background, or when the page has holes - and both
                        // shaders pass the sample's alpha straight through to the result. That made
                        // the capsule exactly as transparent as the capture: a glass window instead
                        // of a panel. Painting the material's own second tint layer first gives the
                        // panel the minimum fill the official always has; under an opaque capture it
                        // is completely covered and therefore invisible, so it only shows up as a
                        // safety net. Clipped to the capsule, so it can never square off a corner.
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .background(Color(spec.layer2Color)),
                        )
                        // Expansion done with an explicit `layout`: `offset { } + requiredSize`
                        // reported a placement that did not match the child's actual origin (the
                        // sampling ended up one margin off), and `size()` is coerced back to the
                        // pill. Placing the child ourselves makes the origin unambiguous.
                        val expandedWidth = surfaceSize.width + 2 * marginPx
                        val expandedHeight = surfaceSize.height + 2 * marginPx
                        Box(
                            modifier =
                                Modifier
                                    .layout { measurable, _ ->
                                        val w = expandedWidth.coerceAtLeast(1)
                                        val h = expandedHeight.coerceAtLeast(1)
                                        val placeable = measurable.measure(Constraints.fixed(w, h))
                                        layout(w, h) { placeable.place(-marginPx, -marginPx) }
                                    }
                                    .graphicsLayer { renderEffect = effect }
                                    .onGloballyPositioned { backdropOrigin = it }
                                    .drawBehind {
                                        drawCouiBackdropLayer(
                                            backdrop = backdrop,
                                            origin = backdropOrigin,
                                            scaleX = couiComposeScale(pressScaleProvider(), scaleXProvider()),
                                            scaleY = couiComposeScale(pressScaleProvider(), scaleYProvider()),
                                        )
                                    },
                        )
                    }
                    CouiPanelEdgeOverlay(
                        spec = spec,
                        shadowEnabled = shadowEnabled,
                        density = densityScale,
                    ) {
                        content()
                    }
                }
            }
            return
        }
    }

    // ---- flat fallback -----------------------------------------------------------------------
    if (!enabled || backdrop == null) {
        android.util.Log.w("CouiPanel", "flat fallback: enabled=$enabled backdrop=${backdrop != null}")
        Box(
            modifier =
                modifier
                    .offset {
                        val t = translation()
                        IntOffset(t.x.roundToInt(), t.y.roundToInt())
                    }
                    .graphicsLayer {
                        this.scaleX = couiComposeScale(pressScaleProvider(), scaleXProvider())
                        this.scaleY = couiComposeScale(pressScaleProvider(), scaleYProvider())
                        this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
        ) {
            fallback()
            CouiPanelEdgeOverlay(
                spec = spec,
                shadowEnabled = shadowEnabled,
                density = densityScale,
            ) {
                content()
            }
        }
        return
    }


}

/**
 * `BaseBottomFloatingMenuView.drawPanelStroke`, verbatim.
 *
 * The official sets the drawable's bounds to `mBackgroundBounds`, calls
 * `setCornerRadius(height / 2)` and then `draw()` - which is a single `canvas.drawPaint` with the
 * `ShadowEdge` shader. The shader's own `main()` masks to the capsule, so there is no clip to set
 * up: everything outside the silhouette returns `vec4(0.0)`.
 *
 * `initShadowEdgeDrawableIfNeeded()` configures the drawable once:
 * `setEdgeEnable(true)`, `setShadowEnable(true)`, `EDGE_STYLE_0`, `SHADOW_STYLE_0`,
 * `setLineAlpha(edgeAlpha)`, `setLineWidth(dp2px(edgeWidthDp))`, `setLineAngle(edgeAngle)`,
 * `setFadeAlpha(shadowFadeIn, shadowFadeOut)`, `setFadeWidthScale(shadowFadeScale)`,
 * `setIsDarkMode(dark)`, `setCornerType(2)` (= `NEW_G2`), `setWeight(1.0)`,
 * `setPaintBlendMode(SRC_OVER)`.
 */
internal fun DrawScope.drawCouiShadowEdge(
    shader: CouiShadowEdge.Shader,
    spec: CouiFloatingMaterialSpec,
    density: Float,
    edgeEnabled: Boolean = true,
    shadowEnabled: Boolean = true,
) {
    if (!CouiShadowEdge.isSupported()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    // `COUIShadowEdgeDrawable.onBoundsChange` → `setResolution(w, h)` + `setSize(w, h)`.
    shader.setResolution(w, h)
    shader.setSize(w, h)
    // `drawPanelStroke`: `setCornerRadius(mBackgroundBounds.height() / 2f)`.
    shader.setCornerRadius(h / 2f)
    // `initShadowEdgeDrawableIfNeeded`: `setWeight(1.0f)`, `setCornerType(2)` (NEW_G2 - the shader
    // source is already the NEW_G2 build).
    shader.setWeight(1f)
    shader.setShadowEnable(shadowEnabled)
    shader.setEdgeEnable(edgeEnabled)

    // `updateEdgeParams()` → `GradientStrokeStyleMapper.toEdgeArray16(...)`.
    shader.setEdgeUniform(
        CouiShadowEdgePacking.packEdge(
            width = w,
            height = h,
            lineWidthPx = spec.edgeWidthPx,
            lineAlpha = spec.edgeAlpha,
            lineAngle = spec.edgeAngle,
            density = density,
        ),
    )
    // `updateShadowParams()` → `InnerShadowStyleMapper.toShadowArray16(...)`.
    shader.setShadowUniform(
        CouiShadowEdgePacking.packShadow(
            width = w,
            height = h,
            density = density,
            fadeWidthScale = spec.shadowFadeScale,
            fadeInAlpha = spec.shadowFadeIn,
            fadeOutAlpha = spec.shadowFadeOut,
        ),
    )

    shader.paintBlendMode = android.graphics.BlendMode.SRC_OVER
    // `COUIShadowEdgeDrawable.draw` → `canvas.drawPaint(mPaint)`. The shader's own `main()` masks
    // to the capsule, so there is no clip to set up — **but** `drawPaint` fills whatever canvas it
    // is handed, and `drawContext.canvas.nativeCanvas` is the whole window. Saving a layer clipped
    // to this DrawScope's rect is what keeps the paint inside the pill: without it the rim's
    // anti-aliased fringe smeared a grey halo around the entire capsule.
    val canvas = drawContext.canvas.nativeCanvas
    val save = canvas.save()
    // The shader's own `main()` reads
    //     rectData.pos = vec2(fragCoord.x, u_commonArray[1] - fragCoord.y) - ...
    // i.e. it expects `fragCoord` with **y pointing down from the surface's top**, and turns it
    // into a y-up, centre-origin space itself. Both `View.onDraw` (the official) and Compose's
    // `nativeCanvas` hand `drawPaint` exactly that, so **no flip belongs here**.
    //
    // An earlier revision wrapped this in `translate(0, h) + scale(1, -1)`, on the theory that
    // Compose delivers a y-up canvas. Measured: that flip moves the bright arc from the top edge
    // to the bottom edge, and - because the shader picks `uRectLineAlpha.x` (0.7x) on one side and
    // `uRectLineAlpha.y` (1.0x) on the other - it also puts the **weaker** of the two rim lines on
    // the top edge. The official's own probe measures top 52 -> 83 (+31, the 1.0x line) and
    // bottom 53 -> 74 (+21, the 0.7x line); with the flip we got top +22 and bottom +0. The flip
    // was the reason the highlight "looked nothing like the official" while the uniforms were
    // already byte-identical.
    canvas.clipRect(0f, 0f, size.width, size.height)
    shader.draw(canvas)
    canvas.restoreToCount(save)
}

/**
 * The official panel **edge + inner shadow** overlay: the verbatim `ShadowEdge` AGSL shader,
 * painted as a full-rect draw under [content] - exactly how `BaseBottomFloatingMenuView` stacks
 * `COUIShadowEdgeDrawable` (`canvas.drawPaint`) beneath its children.
 *
 * On SDK <= 33 the official builds no shader at all (`COUIShadowEdgeDrawable` requires
 * `isRuntimeShaderVersion()`), so this draws nothing - which is the official behaviour.
 */
@Composable
private fun CouiPanelEdgeOverlay(
    spec: CouiFloatingMaterialSpec,
    shadowEnabled: Boolean,
    density: Float,
    content: @Composable () -> Unit,
) {
    // `initShadowEdgeDrawableIfNeeded()`: exactly one `COUIShadowEdgeDrawable` (and therefore one
    // `ShadowEdgeShader`) per view, created lazily and reused for every frame.
    val shader = rememberCouiShadowEdgeShader()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    // `BaseBottomFloatingMenuView.onDraw`: `super.onDraw()` runs
                    // `drawPanelStroke()` **first**, so the rim and the inner fog sit *under* the
                    // items. Drawing them after `drawContent()` washed the icons out.
                    if (shadowEnabled && shader != null) drawCouiShadowEdge(shader, spec, density)
                    this@drawWithContent.drawContent()
                },
) {
        content()
    }
}

/**
 * A 1x1 opaque-black shader, used as the caustic's backdrop when no live capture is bound.
 *
 * The official effect multiplies its alpha by `(1 - gray)^2`, `gray = clamp((luma - 0.45) * 0.6,
 * 0, 1)`, so a black backdrop means `gray = 0` and full strength - the dark-page case the
 * component is designed for. Every APK that ships this effect draws it over a black or near-black
 * page (gallery video tab, photo detail, store home), so this is the common path, not a downgrade.
 */
private fun blackCausticBackdrop(): Shader {
    val pixel = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
    pixel.eraseColor(android.graphics.Color.BLACK)
    return android.graphics.BitmapShader(pixel, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
}

/**
 * `BaseBottomFloatingMenuView.setCausticShadowEffect()` - the pill's level-4 caustic.
 *
 * The effect is painted **behind** the pill, exactly like the official (the render node draws the
 * outline shadow under the view's own content). Only the part that spills below the pill's bottom
 * edge survives - level 4's layout is `(0, 0, 0, 4.9dp)`, so the optics rect is the pill grown
 * downwards by 4.9dp and nothing else, and the shader's bounding-box test drops everything outside
 * it. That is why the glow never appears above or beside the pill.
 *
 * Blending is `BlendMode.PLUS`: the official composite is `rgb = bg + color*alpha`, a pure
 * addition in sRGB, not a source-over.
 */
private fun DrawScope.drawCouiCausticShadow(
    shader: android.graphics.RuntimeShader,
    backdrop: Shader,
    density: Float,
    pillSize: IntSize,
    dark: Boolean,
) {
    val w = pillSize.width.toFloat()
    val h = pillSize.height.toFloat()
    if (w <= 0f || h <= 0f) return
    val params = CouiCausticShadow.level(
        level = CouiCausticShadow.Level.Level4,
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
    // The official passes the render node's outline corner radius; every COUI floating surface is
    // a capsule, so this is the pill's half height.
    shader.setFloatUniform("uCorner", h / 2f)
    shader.setIntUniform("uOutlineType", 0)
    // The shader's own bbox test clamps everything outside the optics rect to zero, so a hair of
    // slack around it is all the draw rect needs.
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
 * The single `ShadowEdgeShader` instance for this composition. The official also creates exactly
 * one `COUIShadowEdgeDrawable` per view and re-uploads the uniforms on every draw.
 */
@Composable
private fun rememberCouiShadowEdgeShader(): CouiShadowEdge.Shader? {
    if (!CouiShadowEdge.isSupported()) return null
    return remember { runCatching { CouiShadowEdge.Shader() }.getOrNull() }
}
