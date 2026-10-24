// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.token
import androidx.compose.runtime.Immutable


/**
 * A theme entry that points at a style, drawable or asset rather than a value.
 *
 * @param light The reference in the light chain, or `null` when the light chain does not
 *   redefine the attribute and the widget default applies.
 * @param dark The reference in the dark chain.
 */
@Immutable
data class CouiThemeStyleRef(
    val light: String?,
    val dark: String?,
)

/**
 * The non-colour half of the COUI theme: corner dimens, component style references, asset names
 * and the theme identifier.
 *
 * The generated [CouiColorTokens] cover every colour attribute; these are the remaining entries,
 * which do not map to a Compose value but record which style or asset a component resolves to.
 *
 * A `null` side means the light or dark chain never redefines that attribute.
 */
object CouiThemeRefs {
    /** The corner dimens backing [CouiCornerScale]. */
    object Corners {
        /** The base corner step: 16dp. */
        const val L: String = "coui_round_corner_l"

        /** The pill corner step: 72dp. */
        const val Full: String = "coui_round_corner_full"
    }

    /** The component styles the theme installs, one entry per component. */
    object ComponentStyles {
        /** Only the dark chain redefines this style. */
        val Switch: CouiThemeStyleRef = CouiThemeStyleRef(
            light = null,
            dark = "COUISwitchStyle.Dark",
        )

        /** Shared by both chains. */
        val CheckBox: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.CompoundButton.COUICheckBox",
            dark = "Widget.COUI.CompoundButton.COUICheckBox",
        )

        /** Shared by both chains. */
        val ButtonColorfulDefault: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large",
            dark = "Widget.COUI.Button.Large",
        )

        /** Shared by both chains. */
        val ButtonColorfulLarge: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large",
            dark = "Widget.COUI.Button.Large",
        )

        /** Shared by both chains. */
        val ButtonColorfulWhite: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large.ButtonNew.Secondary.NoMargin",
            dark = "Widget.COUI.Button.Large.ButtonNew.Secondary.NoMargin",
        )

        /** The single-line text field style. */
        val EditTextLine: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Line",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Line",
        )

        /** The single-line text field style with the hint animation disabled. */
        val EditTextLineHintDisable: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Line.HintDisable",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Line.HintDisable",
        )

        /** The rectangular text field style. */
        val EditTextRectangle: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Rectangle",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Rectangle",
        )
    }

    /** Theme-level assets referenced by attribute rather than by style. */
    object Assets {
        /** The row chevron: 12 x 24dp with a 1.4px round-capped stroke. */
        const val ButtonNext: String = "res/drawable/coui_btn_next.xml"

        /** The rotating spinner asset in light mode. */
        const val RotatingSpinner: String = "coui_rotating_loading.json"

        /** The rotating spinner asset in dark mode. */
        const val RotatingSpinnerDark: String = "coui_rotating_loading_night.json"
    }

    /** The theme identifier for the blue accent variant. */
    const val BlueThemeIdentifier: Int = 4
}
