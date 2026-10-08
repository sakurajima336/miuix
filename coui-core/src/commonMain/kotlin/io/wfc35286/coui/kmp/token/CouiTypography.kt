// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.token

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A font size, remembering whether the original resource was expressed in `sp` or `dp`.
 *
 * This distinction is real and easy to get wrong: ColorOS mixes both in the same type scale.
 * `TextAppearance.COUI` is 15sp, but `TextAppearance.COUI.Preference.Description` is 12sp while
 * `TextAppearance.COUI.List.Assignment.Tiny` is 12**dp**. Sizes written in `dp` deliberately do
 * not follow the user's font-size setting, so collapsing both into `sp` would silently break
 * large-font accessibility behaviour.
 */
@Immutable
sealed interface CouiFontSize {
    /** Came from `sp`: scales with the user's font-size preference. */
    @JvmInline
    value class Scaled(val sp: Float) : CouiFontSize

    /** Came from `dp`: deliberately fixed, does not scale with the user's font-size preference. */
    @JvmInline
    value class Fixed(val dp: Float) : CouiFontSize
}

/** Resolves a [CouiFontSize] against the current density. */
@Composable
fun CouiFontSize.toTextUnit(): TextUnit = when (this) {
    is CouiFontSize.Scaled -> sp.sp
    is CouiFontSize.Fixed -> with(LocalDensity.current) { dp.toSp() }
}

/**
 * One entry of the COUI type scale.
 *
 * @param fontSize The size, keeping its original `sp`/`dp` semantics.
 * @param fontWeight `sans-serif` maps to [FontWeight.Normal], `sans-serif-medium` to
 *   [FontWeight.Medium], and `textStyle="bold"` to [FontWeight.Bold].
 * @param fontFamily Always [FontFamily.SansSerif]: the ColorOS styles use the generic
 *   `sans-serif` / `sans-serif-medium` families, not a bundled font, so the platform resolves
 *   them the same way it does for the real Settings app.
 * @param letterSpacing `TextUnit.Unspecified` unless the source style set one.
 * @param lineHeight `TextUnit.Unspecified` unless the source style set one.
 */
@Immutable
data class CouiTextStyle(
    val fontSize: CouiFontSize,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val lineHeight: TextUnit = TextUnit.Unspecified,
)

/**
 * The COUI type scale, transcribed from every `TextAppearance.COUI.*` style in the ColorOS
 * 17.0.0 resource table.
 *
 * Each entry names the style it came from. The styles were resolved by following their `parent`
 * chains to the root and merging child-over-parent, so these are the sizes a real `TextView`
 * would end up with, not just the values written on the leaf style.
 *
 * ## Which colour goes with which style
 *
 * The text colours in the real styles are `ColorStateList` files, not flat colours. Resolving
 * them gives:
 *
 * | style family | enabled | disabled |
 * |---|---|---|
 * | title (`coui_preference_title_color`) | `couiColorLabelPrimary` | `couiColorLabelTertiary` |
 * | summary (`coui_preference_secondary_text_color`) | `couiColorSecondNeutral` | `couiColorLabelTertiary` |
 *
 * `CouiText` applies exactly that mapping when you pass [CouiTextEmphasis], so callers do not
 * have to remember it.
 */
object CouiTypography {
    /** `TextAppearance.COUI` - the base body style. */
    val body: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(15f))

    /** `TextAppearance.COUI.List.Title` - 16sp, `sans-serif-medium`. */
    val listTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** `TextAppearance.COUI.List.FocusTitle` - 16sp, `sans-serif-medium`. */
    val listFocusTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** `TextAppearance.COUI.List.WarnTitle` - 16sp, `sans-serif-medium`, error-coloured. */
    val listWarnTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** `TextAppearance.COUI.List.Assignment` - 13sp, `sans-serif`. */
    val listAssignment: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** `TextAppearance.COUI.List.Assignment.End` - 13sp, `sans-serif`. */
    val listAssignmentEnd: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** `TextAppearance.COUI.List.Assignment.Tiny` - 12**dp**, `sans-serif`. */
    val listAssignmentTiny: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(12f))

    /** `TextAppearance.COUI.Preference.Summary` - 13sp, `sans-serif`. */
    val preferenceSummary: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** `TextAppearance.COUI.Preference.Description` - 12sp, `sans-serif`. */
    val preferenceDescription: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f))

    /**
     * `couiTextAppearanceSmallButton` - what `Widget.COUI.List.Category.Title` uses for the
     * section headers above a group of rows: 12sp, `sans-serif-medium`.
     */
    val categoryTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f), FontWeight.Medium)

    /** `TextAppearance.COUI.AppCompatSupport.Toolbar.Title` - 18sp, `sans-serif-medium`. */
    val toolbarTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(18f), FontWeight.Medium)

    /** `TextAppearance.COUI.Toolbar.SecondTitle` - 18**dp**, `sans-serif-medium`. */
    val toolbarSecondTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(18f), FontWeight.Medium)

    /** `TextAppearance.COUI.AppCompatSupport.Toolbar.SubTitle` - 13**dp**, `sans-serif`. */
    val toolbarSubtitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(13f))

    /** `TextAppearance.COUI.Toolbar.LargestTitle` - 22**dp**, bold. */
    val toolbarLargestTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(22f), FontWeight.Bold)

    /** `TextAppearance.COUI.DialogWindowTitle` - 16sp. */
    val dialogWindowTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f))

    /** `TextAppearance.COUI.WindowTitle` - 20sp. */
    val windowTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(20f))

    /** `TextAppearance.COUI.Widget.PopupMenu.Small` - 14sp. */
    val popupMenuSmall: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(14f))

    /** `TextAppearance.COUI.Widget.PopupMenu.Large` - 18sp. */
    val popupMenuLarge: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(18f))
}

/**
 * Which semantic colour a piece of text should take.
 *
 * Mirrors the `ColorStateList` files the real styles point at, so the enabled/disabled pairs
 * stay together instead of being picked per call site.
 */
enum class CouiTextEmphasis {
    /** `couiColorLabelPrimary`, falling back to `couiColorLabelTertiary` when disabled. */
    Primary,

    /** `couiColorSecondNeutral`, falling back to `couiColorLabelTertiary` when disabled. */
    Secondary,

    /** `couiColorLabelTertiary` regardless of state. */
    Tertiary,

    /** `couiColorError`. */
    Error,

    /** `couiColorLabelOnColor` - text drawn on a filled surface. */
    OnColor,
}