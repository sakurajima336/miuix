// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import io.wfc35286.coui.kmp.shape.CouiContinuity
import io.wfc35286.coui.kmp.shape.CouiCornerRadius
import io.wfc35286.coui.kmp.shape.CouiCornerWeights
import io.wfc35286.coui.kmp.token.CouiColorTokens
import io.wfc35286.coui.kmp.token.CouiComponentColors

/**
 * Corner radii used by COUI components.
 *
 * These are the real `coui_round_corner_*_radius_normal_16_1` values, which are the ones this
 * device uses (`getOSVersionCode()` is past the 16.1 threshold). Components read them from here
 * rather than writing literals, so a single swap to [CouiCornerRadius.Large] reskins the whole
 * library to the "large" corner variant.
 */
@Immutable
data class CouiShapes(
    val extraSmall: Dp = CouiCornerRadius.Normal.Xs,
    val small: Dp = CouiCornerRadius.Normal.S,
    val medium: Dp = CouiCornerRadius.Normal.M,
    val large: Dp = CouiCornerRadius.Normal.L,
    val extraLarge: Dp = CouiCornerRadius.Normal.Xl,
    val extraExtraLarge: Dp = CouiCornerRadius.Normal.Xxl,
    val extraExtraExtraLarge: Dp = CouiCornerRadius.Normal.Xxxl,
    val extraExtraExtraExtraLarge: Dp = CouiCornerRadius.Normal.Xxxxl,
    /** The curvature family every corner is drawn with. G2 on ColorOS 16+. */
    val continuity: CouiContinuity = CouiContinuity.G2,
)

/**
 * Corner weights for COUI components, mirroring the `_weight` entries of `coui_round_corner_*`.
 *
 * Kept next to [CouiShapes] because the two are only meaningful together: ColorOS decides both
 * how big a corner is and how smooth it is drawn from the same corner-scale entry.
 */
@Immutable
data class CouiCornerWeightsSet(
    val extraSmall: Float = 1.5f,
    val small: Float = 1.5f,
    val medium: Float = 0.99f,
    val large: Float = 0.99f,
    val extraLarge: Float = 1.1f,
    val extraExtraLarge: Float = 1.1f,
    val extraExtraExtraLarge: Float = 1.1f,
    val extraExtraExtraExtraLarge: Float = 1.1f,
    /** `OS_16_1_FULL_ROUND_SMOOTH_WEIGHT` - used for pills. */
    val full: Float = CouiCornerWeights.FullRound,
)

// Fallbacks used when no `CouiTheme {}` is above a COUI component - for example a CouiSwitch
// dropped into a foreign host like the example app's Miuix Card. Following the Material 3
// precedent they resolve to the light palette; wrap content in `CouiTheme {}` for dark mode.
private val LocalCouiColors = staticCompositionLocalOf { CouiColorTokens.Light }

private val LocalCouiShapes = staticCompositionLocalOf { CouiShapes() }

private val LocalCouiCornerWeights = staticCompositionLocalOf { CouiCornerWeightsSet() }

private val LocalCouiComponentColors = staticCompositionLocalOf { CouiComponentColors.Light }

/**
 * The COUI design system.
 *
 * Wrap your UI in [CouiTheme] and read tokens through [CouiTheme.colors] / [CouiTheme.shapes].
 *
 * The default colour scheme follows the system dark-mode setting and is the real ColorOS
 * palette - both variants were extracted from the device's own `resources.arsc`, so there is
 * nothing to eyeball or maintain by hand.
 */
object CouiTheme {
    /** The active colour tokens. */
    val colors: CouiColorTokens
        @Composable @ReadOnlyComposable get() = LocalCouiColors.current

    /** The active corner radii. */
    val shapes: CouiShapes
        @Composable @ReadOnlyComposable get() = LocalCouiShapes.current

    /** The active corner weights. */
    val cornerWeights: CouiCornerWeightsSet
        @Composable @ReadOnlyComposable get() = LocalCouiCornerWeights.current

    /**
     * Colours that individual components reference by name rather than through a theme attribute.
     *
     * See [CouiComponentColors] for why this is a separate set from [colors].
     */
    val componentColors: CouiComponentColors
        @Composable @ReadOnlyComposable get() = LocalCouiComponentColors.current
}

/**
 * Provides the COUI design system to [content].
 *
 * @param colors The colour tokens; defaults to the real ColorOS light/dark palette.
 * @param shapes The corner radii; defaults to the `_16_1` normal scale.
 * @param cornerWeights The corner weights; defaults to the `coui_round_corner_*_weight` values.
 * @param content The UI to theme.
 */
@Composable
fun CouiTheme(
    colors: CouiColorTokens = if (isSystemInDarkTheme()) CouiColorTokens.Dark else CouiColorTokens.Light,
    componentColors: CouiComponentColors =
        if (isSystemInDarkTheme()) CouiComponentColors.Dark else CouiComponentColors.Light,
    shapes: CouiShapes = CouiShapes(),
    cornerWeights: CouiCornerWeightsSet = CouiCornerWeightsSet(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCouiColors provides colors,
        LocalCouiComponentColors provides componentColors,
        LocalCouiShapes provides shapes,
        LocalCouiCornerWeights provides cornerWeights,
        content = content,
    )
}