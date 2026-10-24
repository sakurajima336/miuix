// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke


/**
 * The ColorOS "material" of a floating surface: a backdrop blur, two colour layers blended over
 * it, an inner edge stroke and a drop shadow, all rounded to the surface's capsule silhouette.
 *
 * On ColorOS the whole thing is handed to the framework, which blurs whatever is painted *behind*
 * the view (a true backdrop blur) via a **RenderNode** background effect. None of that is
 * reachable from Compose, so the surface is hosted by a plain Android `View` (see
 * [CouiFloatingMaterialSurface]) and the framework does the painting. When the framework is
 * missing, the same silhouette is approximated in Compose - that is a fallback, not the design.
 */
internal data class CouiFloatingMaterialSpec(
    /** The corner radius, in pixels: half the content height, giving a full capsule. */
    val cornerRadiusPx: Float,
    /** The backdrop blur radius, in raw pixels. */
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
     * Whether the outer caustic shadow is drawn. It is one of the only parts of the material that
     * paint **outside** the pill, so it is kept switchable to debug in isolation.
     */
    val applyCausticShadow: Boolean = true,
    /**
     * Whether the drop shadow is drawn. Kept switchable so it can be turned off in isolation: it
     * is the prime suspect whenever a block larger than the pill shows up.
     */
    val applyShadow: Boolean = true,
)

/**
 * The blend modes the material layers use. The list is deliberately closed: only these four
 * appear in the material.
 */
internal enum class CouiBlendMode {
    /** Source-over compositing. */
    SrcOver,

    /** Colour-dodge blending. */
    ColorDodge,

    /** Luminosity blending. */
    Luminosity,

    /** Overlay blending. */
    Overlay,
}

/** Light-theme material parameters for a capsule panel. */
internal val CouiFloatingMaterialLight =
    CouiFloatingMaterialSpec(
        cornerRadiusPx = 0f,
        blurRadiusPx = 25f,
        layer1Color = 0xB3666666.toInt(),
        layer1Blend = CouiBlendMode.ColorDodge,
        layer2Color = 0x99F5F5F5.toInt(),
        layer2Blend = CouiBlendMode.Luminosity,
        strokeType = 1,
        edgeWidthPx = 0f,
        edgeAlpha = 0.8f,
        edgeAngle = 0f,
        shadowFadeScale = 1f,
        shadowFadeIn = 0.1f,
        shadowFadeOut = 1f,
    )

/** Dark-theme material parameters for a capsule panel. */
internal val CouiFloatingMaterialDark =
    CouiFloatingMaterialSpec(
        cornerRadiusPx = 0f, // filled in from the measured height
        blurRadiusPx = 25f,
        layer1Color = 0x80666666.toInt(),
        // Overlay rather than the light theme's colour-dodge: dodge against a dark backdrop
        // saturates to a flat light grey and eats the blur.
        layer1Blend = CouiBlendMode.Overlay,
        layer2Color = 0xB3262626.toInt(),
        layer2Blend = CouiBlendMode.Luminosity,
        strokeType = 1,
        edgeWidthPx = 0f,
        edgeAlpha = 0.15f,
        edgeAngle = 0f,
        shadowFadeScale = 1f,
        shadowFadeIn = 0.01f,
        shadowFadeOut = 0f,
    )

/** The width of the panel's inner edge stroke, in dp - the same in both themes. */
internal const val COUI_PANEL_EDGE_WIDTH_DP = 2.0f

/** The colour of the caustic drop shadow (`#42ffffff`). */
internal const val COUI_CAUSTIC_COLOR: Int = 0x42FFFFFF

/** How far the caustic shadow reaches below the pill. */
internal const val COUI_CAUSTIC_BOTTOM_DP = 4.9f

/** The softening radius of the caustic shadow's signed-distance falloff. */
internal const val COUI_CAUSTIC_SDF_DP = 10.4f

/**
 * The panel edge and inner shadow are rendered by the AGSL shader in [CouiShadowEdge].
 *
 * On Android the shader runs on API levels above 33; on skiko it cannot, so the skiko
 * implementation of [CouiFloatingMaterialSurface] falls back to the flat panel.
 */

/**
 * Hosts the pill's material.
 *
 * On Android the content is drawn over a framework-material `View`, so the blur, stroke and shadow
 * are painted by the ColorOS material framework. Everywhere else - and on Android builds without
 * that framework - the content is drawn over [fallback], which approximates the same silhouette in
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
    /** The uniform press scale, applied around the **centre**. */
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
    /** Whether the panel edge / inner shadow is requested. */
    shadowEnabled: Boolean = true,
    /** Whether the host is in dark theme (including night mode). */
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
 * back to the flat panel for the duration.
 */
val LocalCouiSceneTransition: androidx.compose.runtime.ProvidableCompositionLocal<() -> Boolean> =
    androidx.compose.runtime.staticCompositionLocalOf { { false } }

/** True when the running device exposes the ColorOS material framework. */
internal expect fun isCouiFloatingMaterialAvailable(): Boolean
