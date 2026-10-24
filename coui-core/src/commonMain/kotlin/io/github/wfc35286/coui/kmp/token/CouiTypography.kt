// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.token
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt


/**
 * A font size that remembers whether it was authored in `sp` or `dp`.
 *
 * The distinction matters and is easy to get wrong: the COUI type scale mixes both. Sizes written
 * in `dp` deliberately do not follow the user's font-size setting, so collapsing both into `sp`
 * would silently break large-font accessibility behaviour.
 */
@Immutable
sealed interface CouiFontSize {
    /** Authored in `sp`: scales with the user's font-size preference. */
    @JvmInline
    value class Scaled(val sp: Float) : CouiFontSize

    /** Authored in `dp`: deliberately fixed, does not scale with the user's font-size preference. */
    @JvmInline
    value class Fixed(val dp: Float) : CouiFontSize
}

/** Resolves a [CouiFontSize] against the current density. */
@Composable
fun CouiFontSize.toTextUnit(): TextUnit = when (this) {
    is CouiFontSize.Scaled -> sp.sp

    // A bare `Float.toSp()` is the **px -> sp** overload; wrapping the value in [Dp] selects
    // the dp -> sp conversion, which is what a dp-authored size means.
    is CouiFontSize.Fixed -> with(LocalDensity.current) { Dp(dp).toSp() }
}

/**
 * Resolves a [CouiFontSize] to whole-pixel precision, the way an integral text size does on the
 * platform: Compose would otherwise keep the fractional sp value, which can change the measured
 * line box by a pixel.
 */
@Composable
fun CouiFontSize.toRoundedTextUnit(): TextUnit = when (this) {
    is CouiFontSize.Scaled -> with(LocalDensity.current) {
        (sp * density * fontScale).roundToInt().toFloat().div(density * fontScale).sp
    }

    // A bare `Float.toSp()` is the **px -> sp** overload; wrapping the value in [Dp] selects
    // the dp -> sp conversion, which is what a dp-authored size means.
    is CouiFontSize.Fixed -> with(LocalDensity.current) { Dp(dp).toSp() }
}

/**
 * One entry of the COUI type scale.
 *
 * @param fontSize The size, keeping its original `sp`/`dp` semantics.
 * @param fontWeight The weight; medium and bold weights map to [FontWeight.Medium] and
 *   [FontWeight.Bold].
 * @param fontFamily Always [FontFamily.SansSerif]: the styles use the generic `sans-serif`
 *   families, not a bundled font.
 * @param letterSpacing [TextUnit.Unspecified] unless a style sets one.
 * @param lineHeight [TextUnit.Unspecified] unless a style sets one.
 */
@Immutable
data class CouiTextStyle(
    val fontSize: CouiFontSize,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val lineSpacingMultiplier: Float = 1f,
)

/**
 * The COUI type scale: one entry per text style, with its size, weight and line spacing
 * multiplier.
 *
 * ## Which colour goes with which style
 *
 * Titles and summaries carry paired enabled/disabled colours that are resolved together:
 *
 * | style family | enabled | disabled |
 * |---|---|---|
 * | title | the primary label colour | the tertiary label colour |
 * | summary | the secondary neutral colour | the tertiary label colour |
 *
 * [CouiText] applies exactly that mapping when you pass [CouiTextEmphasis], so callers do not
 * have to remember it.
 */
object CouiTypography {
    /** The base body style: 15sp. */
    val body: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(15f))

    /** Popup list item text: 15sp with line spacing x1.1. */
    val popupListItem: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(15f), lineSpacingMultiplier = 1.1f)

    /** List row title: 16sp, medium. */
    val listTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** Focused list row title: 16sp, medium. */
    val listFocusTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** Warning list row title: 16sp, medium, error-coloured. */
    val listWarnTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)

    /** List assignment text: 13sp. */
    val listAssignment: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** End-aligned list assignment text: 13sp. */
    val listAssignmentEnd: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** Tiny list assignment text: 12dp, so a fixed size. */
    val listAssignmentTiny: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(12f))

    /** Preference summary line: 13sp. */
    val preferenceSummary: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** Preference description text: 12sp. */
    val preferenceDescription: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f))

    /** Button label: 14sp, medium, line spacing x1.1. */
    val button: CouiTextStyle = CouiTextStyle(
        fontSize = CouiFontSize.Scaled(14f),
        fontWeight = FontWeight.Medium,
        lineSpacingMultiplier = 1.1f,
    )

    /** Large button label: 16sp, medium, line spacing x1.2. */
    val buttonLarge: CouiTextStyle = CouiTextStyle(
        fontSize = CouiFontSize.Scaled(16f),
        fontWeight = FontWeight.Medium,
        lineSpacingMultiplier = 1.2f,
    )

    /** Section header above a group of rows: 12sp, medium. */
    val categoryTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f), FontWeight.Medium)

    /** Toolbar title: 18sp, medium. */
    val toolbarTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(18f), FontWeight.Medium)

    /** Secondary toolbar title: 18dp, so a fixed size, medium. */
    val toolbarSecondTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(18f), FontWeight.Medium)

    /** Toolbar subtitle: 13dp, so a fixed size. */
    val toolbarSubtitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(13f))

    /** The largest toolbar title: 22dp, so a fixed size, bold. */
    val toolbarLargestTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Fixed(22f), FontWeight.Bold)

    /** Dialog window title: 16sp. */
    val dialogWindowTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(16f))

    /** Window title: 20sp. */
    val windowTitle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(20f))

    /** Small popup menu item text: 14sp. */
    val popupMenuSmall: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(14f))

    /** Large popup menu item text: 18sp. */
    val popupMenuLarge: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(18f))
}

/**
 * Which semantic colour a piece of text should take.
 *
 * Each option pairs the enabled and disabled colours together so they are not picked apart
 * per call site.
 */
enum class CouiTextEmphasis {
    /** The primary label colour, falling back to the tertiary label colour when disabled. */
    Primary,

    /** The secondary neutral colour, falling back to the tertiary label colour when disabled. */
    Secondary,

    /** The tertiary label colour regardless of state. */
    Tertiary,

    /** The error colour. */
    Error,

    /** The on-colour for text drawn on a filled surface. */
    OnColor,
}
