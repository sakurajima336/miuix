// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.token

import androidx.compose.runtime.Immutable

/**
 * A `coui*` theme entry that points at a style, drawable or asset rather than a value.
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
 * The non-colour half of `Theme.COUI.Main`, transcribed from ColorOS 17.0.0 `com.android.settings`.
 *
 * The generated [CouiColorTokens] cover every `coui*` colour attribute; these are the remaining
 * entries - corner dimens, component style references, asset names and the theme identifier -
 * which do not map to a Compose value but do say where a component comes from. They are here so
 * a component can be traced back to the exact ColorOS attribute instead of a guess.
 *
 * A `null` side means the light or dark chain never redefines that attribute.
 */
object CouiThemeRefs {
    /** `couiRoundCornerL` / `couiRoundCornerFULL` - the dimens behind [CouiCornerScale]. */
    object Corners {
        /** `coui_round_corner_l` - 16dp, the base `l` step. */
        const val L: String = "coui_round_corner_l"

        /** `coui_round_corner_full` - 72dp, the pill step. */
        const val Full: String = "coui_round_corner_full"
    }

    /**
     * The component styles the theme installs. A Compose port reads the resolved values, not the
     * style name, so these are documentation of the official source of each component.
     */
    object ComponentStyles {
        /** `couiSwitchStyle` - only the dark chain redefines it. */
        val Switch: CouiThemeStyleRef = CouiThemeStyleRef(
            light = null,
            dark = "COUISwitchStyle.Dark",
        )

        /** `couiCheckBoxStyle` - shared by both chains. */
        val CheckBox: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.CompoundButton.COUICheckBox",
            dark = "Widget.COUI.CompoundButton.COUICheckBox",
        )

        /** `couiButtonColorfulDefaultStyle` - shared by both chains. */
        val ButtonColorfulDefault: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large",
            dark = "Widget.COUI.Button.Large",
        )

        /** `couiButtonColorfulLargeStyle` - shared by both chains. */
        val ButtonColorfulLarge: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large",
            dark = "Widget.COUI.Button.Large",
        )

        /** `couiButtonColorfulWhiteStyle` - shared by both chains. */
        val ButtonColorfulWhite: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.Button.Large.ButtonNew.Secondary.NoMargin",
            dark = "Widget.COUI.Button.Large.ButtonNew.Secondary.NoMargin",
        )

        /** `couiEditTextLineStyle`. */
        val EditTextLine: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Line",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Line",
        )

        /** `couiEditTextLineHintDisableStyle`. */
        val EditTextLineHintDisable: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Line.HintDisable",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Line.HintDisable",
        )

        /** `couiEditTextRectangleStyle`. */
        val EditTextRectangle: CouiThemeStyleRef = CouiThemeStyleRef(
            light = "Widget.COUI.EditText.HintAnim.Rectangle",
            dark = "Widget.COUI.EditText.Dark.HintAnim.Rectangle",
        )
    }

    /** Theme-level assets referenced by attribute rather than by style. */
    object Assets {
        /** `couiButtonNextStyle` - the row chevron; 12 x 24dp with a 1.4px round-capped stroke. */
        const val ButtonNext: String = "res/drawable/coui_btn_next.xml"

        /** `couiRotatingSpinnerJsonName` - light. */
        const val RotatingSpinner: String = "coui_rotating_loading.json"

        /** `couiRotatingSpinnerJsonName` - dark. */
        const val RotatingSpinnerDark: String = "coui_rotating_loading_night.json"
    }

    /**
     * `couiThemeIdentifier` - which accent variant the theme is. `4` is `couiBlueIdentifier`,
     * i.e. the blue accent this device ships.
     */
    const val BlueThemeIdentifier: Int = 4
}
