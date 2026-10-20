// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush

/**
 * The ColorOS "material" of a floating surface, transcribed from
 * `com.coui.appcompat.COUIMaterialBlurEffect` / `COUIMaterialStrokeEffect`.
 *
 * On ColorOS the whole thing is handed to the framework:
 *
 * 1. `RenderEffect.createBlurEffect(r, r, MIRROR)`, then two
 *    `RenderEffect.createColorFilterEffect(BlendModeColorFilter(color, blend))` layers, applied with
 *    `com.oplus.view.OplusViewBackgroundRenderEffect.setBackgroundRenderEffect(effect, view)` -
 *    i.e. the **RenderNode** blurs whatever is painted *behind* the view (a true backdrop blur).
 * 2. `com.oplus.view.material.OplusMaterialUtil.setCornerParams` rounds that background.
 * 3. `...setEdgeParams` + `...setShadowParams` draw the inner stroke and the drop shadow.
 *
 * None of that is reachable from Compose, so the surface is hosted by a plain Android `View`
 * (see [CouiFloatingMaterialSurface]) and the framework does the painting. When the framework is
 * missing the same silhouette is approximated in Compose - that is a **fallback**, not the design.
 */
internal data class CouiFloatingMaterialSpec(
    /** `mRadius` from `updateSmoothRoundPath()`: always `(height - paddingTop - paddingBottom) / 2`. */
    val cornerRadiusPx: Float,
    /** `BlurEffectParams.blurRadius` - raw px, no dp conversion in the official. */
    val blurRadiusPx: Float,
    val layer1Color: Int,
    val layer1Blend: CouiBlendMode,
    val layer2Color: Int,
    val layer2Blend: CouiBlendMode,
    val strokeType: Int,
    val edgeWidthPx: Float,
    val edgeAlpha: Float,
    val edgeAngle: Float,
    val shadowFadeScale: Float,
    val shadowFadeIn: Float,
    val shadowFadeOut: Float,
    /**
     * Whether `BaseBottomFloatingMenuView.setCausticShadowEffect()` runs - the outer shadow
     * (`OplusMaterialUtil.setOutlineCausticShadow*` + a 24dp elevation with the `caustic_down`
     * light geometry). It is the only part of the material that paints **outside** the pill, so it
     * is kept switchable for bisection.
     */
    val applyCausticShadow: Boolean = true,
    /**
     * Whether `OplusMaterialUtil.setShadowParams` is called. Kept switchable so the drop shadow can
     * be bisected away: it is the only part of the material that paints **outside** the pill, so it is
     * the prime suspect whenever a block larger than the pill shows up.
     */
    val applyShadow: Boolean = true,
)

/**
 * The `BlendMode`s the official material uses. Only these four appear in `COUIMaterialBlurEffect`,
 * so the list is deliberately closed (the obfuscated `a.a.a.{k,l,m,n}92` helpers were resolved to
 * exactly these).
 */
internal enum class CouiBlendMode {
    /** `k92` */
    SrcOver,

    /** `l92` */
    ColorDodge,

    /** `m92` */
    Luminosity,

    /** `n92` */
    Overlay,
}

/** `COUIMaterialStrokeEffect.FRAMEWORK_CAPSULE_PARAMS_12_LIGHT` - runtime dump of `设置_17.0.0`. */
internal val CouiFloatingMaterialLight =
    CouiFloatingMaterialSpec(
        cornerRadiusPx = 0f,
        blurRadiusPx = 25f,
        layer1Color = 0xB3666666.toInt(),
        layer1Blend = CouiBlendMode.ColorDodge,
        layer2Color = 0x99F5F5F5.toInt(),
        layer2Blend = CouiBlendMode.Luminosity,
        strokeType = 1,
        // StrokeEffectParams(strokeType=1, edgeAlpha=0.8, edgeWidthDp=2.0, edgeAngle=0.0,
        //                    shadowFadeIn=0.1, shadowFadeOut=1.0, shadowFadeScale=1.0)
        edgeWidthPx = 0f,
        edgeAlpha = 0.8f,
        edgeAngle = 0f,
        shadowFadeScale = 1f,
        shadowFadeIn = 0.1f,
        shadowFadeOut = 1f,
    )

/** `COUIMaterialStrokeEffect.FRAMEWORK_CAPSULE_PARAMS_12_DARK` - same source. */
internal val CouiFloatingMaterialDark =
    CouiFloatingMaterialSpec(
        cornerRadiusPx = 0f, // filled in from the measured height
        blurRadiusPx = 25f,
        layer1Color = 0x80666666.toInt(),
        // `FRAMEWORK_BOTTOM_BAR_PARAMS_DARK` uses OVERLAY here, not the light theme's COLOR_DODGE:
        // dodge against a dark backdrop saturates to a flat light grey and eats the blur.
        layer1Blend = CouiBlendMode.Overlay,
        layer2Color = 0xB3262626.toInt(),
        layer2Blend = CouiBlendMode.Luminosity,
        strokeType = 1,
        // StrokeEffectParams(strokeType=1, edgeAlpha=0.15, edgeWidthDp=2.0, edgeAngle=0.0,
        //                    shadowFadeIn=0.01, shadowFadeOut=0.0, shadowFadeScale=1.0)
        edgeWidthPx = 0f,
        edgeAlpha = 0.15f,
        edgeAngle = 0f,
        shadowFadeScale = 1f,
        shadowFadeIn = 0.01f,
        shadowFadeOut = 0f,
    )

/**
 * `FRAMEWORK_CAPSULE_PARAMS_12.edgeWidthDp` - **2.0 in both themes**. The 0.8 / 0.15 pair that
 * used to live here was the *edge alpha* read out of the wrong field of `StrokeEffectParams`, and
 * it made the top rim roughly a third of its real width.
 */
internal const val COUI_PANEL_EDGE_WIDTH_DP = 2.0f

/** `coui_caustic_color_2` - the caustic drop shadow's colour (`#42ffffff`). */
internal const val COUI_CAUSTIC_COLOR: Int = 0x42FFFFFF

/** `coui_caustic_layout_b_level4` - how far the caustic shadow reaches below the pill. */
internal const val COUI_CAUSTIC_BOTTOM_DP = 4.9f

/** `coui_caustic_layout_sdf_level4` - the caustic shadow's SDF softening radius. */
internal const val COUI_CAUSTIC_SDF_DP = 10.4f

/**
 * The panel edge + inner shadow are the official AGSL shader, ported verbatim in
 * [CouiShadowEdge]: the exact `ShadowEdge` source plus the exact `GradientStrokeStyleMapper` /
 * `InnerShadowStyleMapper` uniform packing.
 *
 * On Android the shader runs (`COUIShadowEdgeDrawable` requires
 * `COUIEffectVersionUtil.isRuntimeShaderVersion()`, i.e. SDK > 33). On skiko it cannot, so
 * [CouiFloatingMaterial.skiko] falls back to the flat panel.
 */

/**
 * Hosts the pill's material.
 *
 * On Android the content is drawn over a framework-material `View`, so the official blur, stroke and
 * shadow come out exactly as in ColorOS. Everywhere else - and on Android builds without the Oplus
 * framework - the content is drawn over [fallback], which approximates the same silhouette in
 * Compose.
 */
@Composable
internal expect fun CouiFloatingMaterialSurface(
    spec: CouiFloatingMaterialSpec,
    /** Whether the framework material is requested; `false` paints [fallback] only. */
    enabled: Boolean,
    /**
     * Compose-side capture of the page behind the surface, if the host provides one. When present
     * the material is rendered from it inside Compose, which keeps the blur alive through every
     * scene transform (see [CouiBackdrop]); otherwise the Oplus window material is used.
     */
    backdrop: CouiBackdrop? = null,
    /** The press/rubber-band translation, read in the **layout** phase so interop views follow. */
    translation: () -> Offset,
    /** The uniform press scale, applied around the **centre** (`COUIPressAnimatorUnit`). */
    pressScale: () -> Float,
    /**
     * The horizontal deformation scale. On Android this is pushed onto the host View (a Compose
     * layer does not transform an interop view); everywhere else it is a Compose layer.
     */
    scaleX: () -> Float,
    /** The vertical deformation scale. See [scaleX]. */
    scaleY: () -> Float,
    /**
     * The deformation pivot, as a fraction of the surface: the edge the finger is pulling away from
     * stays put while the surface stretches.
     */
    transformOrigin: () -> Offset,
    /** Whether the panel edge / inner shadow (the `COUIShadowEdgeDrawable` pair) is requested. */
    shadowEnabled: Boolean = true,
    /** Whether the host is in dark theme (`isCOUIDarkThemeOrNightMode`). */
    dark: Boolean = false,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit,
)
/**
 * Whether the host is currently running a **scene transition** - the navigation scale/slide behind
 * predictive back, a push animation, and so on.
 *
 * The ColorOS material is painted by an interop `View` (a `RenderNode` background effect that
 * samples the backdrop). Interop views are positioned by layout only: they never follow a Compose
 * `graphicsLayer` transform. During a scene transition the pill therefore stays put while the page
 * (and with it the scene layer that the backdrop sampler reads from) shrinks away, and the part of
 * the pill that falls outside that layer samples nothing - which HWUI renders as a hard **black**
 * wedge (this is exactly the artifact seen on predictive back).
 *
 * A Compose layer transform is invisible to both `LayoutCoordinates.localToWindow` and
 * `View.getLocationInWindow`, so the component cannot detect it. Hosts that know - anything built
 * on `miuix-nav` can read `LocalNavTransitionScope.current` - provide this flag, and the bar falls
 * back to the flat panel (the official's own non-material appearance) for the duration.
 */
val LocalCouiSceneTransition: androidx.compose.runtime.ProvidableCompositionLocal<() -> Boolean> =
    androidx.compose.runtime.staticCompositionLocalOf { { false } }

/** True when the running device exposes the ColorOS material (`com.oplus.view.material.*`). */
internal expect fun isCouiFloatingMaterialAvailable(): Boolean
