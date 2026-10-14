// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.example

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color tokens of the ColorOS "COUI" design system, reconstructed from the resources of
 * `com.android.settings` 17.0.0 (ColorOS).
 *
 * Every value below was read out of that APK's `resources.arsc` — the `coui_color_*` entries —
 * so the light/dark pairs are the real ones, not eyeballed approximations.
 * Values are stored as 8-digit `AARRGGBB` hex.
 *
 * COUI's neutral text ramp is alpha-based rather than a set of distinct greys:
 * `labelPrimary` is 90% black, `labelSecondary` 54%, `labelTertiary` 26%, `labelQuaternary` 8%.
 * That is why most tokens below look like the same colour at different opacities.
 */
@Immutable
data class CouiColors(
    /** The theme accent, `couiColorPrimary`. In Settings it resolves to the brand blue. */
    val primary: Color,
    /** Switch track when off. `switch_unchecked_bar_color`. */
    val switchTrackOff: Color,
    /** Switch track when off and disabled. `switch_unchecked_inner_circle_disabled_color`. */
    val switchTrackOffDisabled: Color,
    /** Switch and seek-bar thumb fill. `switch_outer_circle_color`. */
    val switchThumb: Color,
    /** Thumb fill when disabled. `switch_outer_circle_disable_color`. */
    val switchThumbDisabled: Color,
    /** Seek bar inactive track. `coui_seekbar_background_selector`. */
    val seekBarTrack: Color,
    /** Seek bar inactive track when disabled. */
    val seekBarTrackDisabled: Color,
    /** Page background. `coui_color_background`. */
    val background: Color,
    /** Page background when cards are present. `coui_color_background_with_card`. */
    val backgroundWithCard: Color,
    /** Elevated background (dialogs, sheets). `coui_color_background_elevated`. */
    val backgroundElevated: Color,
    /** Card fill. `coui_color_card`. */
    val card: Color,
    /** Card fill while pressed. `coui_color_card_pressed`. */
    val cardPressed: Color,
    /** Primary text. `coui_color_label_primary`. */
    val labelPrimary: Color,
    /** Secondary text / summaries. `coui_color_label_secondary`. */
    val labelSecondary: Color,
    /** Tertiary text (disabled-ish, hints). `coui_color_label_tertiary`. */
    val labelTertiary: Color,
    /** Quaternary text (almost invisible). `coui_color_label_quaternary`. */
    val labelQuaternary: Color,
    /** Text drawn on a coloured surface. `coui_color_label_on_color`. */
    val labelOnColor: Color,
    /** Neutral "primary" (the near-black used for selected states). `coui_color_primary_neutral`. */
    val primaryNeutral: Color,
    /** Touch highlight for list items. `coui_color_press_background`. */
    val pressBackground: Color,
    /** Hover highlight. `coui_color_hover`. */
    val hover: Color,
    /** Hairline divider. `coui_color_divider`. */
    val divider: Color,
    /** Disabled content. `coui_color_disabled_neutral`. */
    val disabledNeutral: Color,
    /** Error. `coui_color_error`. */
    val error: Color,
    /** Dialog / sheet scrim. `coui_color_mask`. */
    val mask: Color,
    /** Brand blue. `coui_color_blue`. */
    val blue: Color,
    /** Brand green. `coui_color_green`. */
    val green: Color,
    /** Brand orange. `coui_color_orange`. */
    val orange: Color,
    /** Brand red. `coui_color_red`. */
    val red: Color,
    /** Brand yellow. `coui_color_yellow`. */
    val yellow: Color,
    /** Whether this is the light variant. */
    val isLight: Boolean,
) {
    companion object {
        /** The COUI light palette. */
        val Light = CouiColors(
            primary = Color(0xFF0080FF),
            switchTrackOff = Color(0xFFE5E5E5),
            switchTrackOffDisabled = Color(0xFFF2F2F2),
            switchThumb = Color(0xFFFFFFFF),
            switchThumbDisabled = Color(0x8AFFFFFF),
            seekBarTrack = Color(0x1F000000),
            seekBarTrackDisabled = Color(0x42000000),
            background = Color(0xFFFFFFFF),
            backgroundWithCard = Color(0xFFF0F1F2),
            backgroundElevated = Color(0xFFFFFFFF),
            card = Color(0xFFFFFFFF),
            cardPressed = Color(0xFFE6E6E6),
            labelPrimary = Color(0xE6000000),
            labelSecondary = Color(0x8A000000),
            labelTertiary = Color(0x42000000),
            labelQuaternary = Color(0x14000000),
            labelOnColor = Color(0xFFFFFFFF),
            primaryNeutral = Color(0xE6000000),
            pressBackground = Color(0x14000000),
            hover = Color(0x14000000),
            divider = Color(0x1F000000),
            disabledNeutral = Color(0x42000000),
            error = Color(0xFFDB382C),
            mask = Color(0x33000000),
            blue = Color(0xFF0080FF),
            green = Color(0xFF00BD13),
            orange = Color(0xFFFF7700),
            red = Color(0xFFDB382C),
            yellow = Color(0xFFFFB200),
            isLight = true,
        )

        /** The COUI dark palette. */
        val Dark = CouiColors(
            primary = Color(0xFF1A8CFF),
            switchTrackOff = Color(0xFF757575),
            switchTrackOffDisabled = Color(0x80757575),
            switchThumb = Color(0xFFFFFFFF),
            switchThumbDisabled = Color(0x29FFFFFF),
            seekBarTrack = Color(0x33FFFFFF),
            seekBarTrackDisabled = Color(0x4DFFFFFF),
            background = Color(0xFF000000),
            backgroundWithCard = Color(0xFF000000),
            backgroundElevated = Color(0xFF1E1E1E),
            card = Color(0x1AFFFFFF),
            cardPressed = Color(0x33FFFFFF),
            labelPrimary = Color(0xE6FFFFFF),
            labelSecondary = Color(0x8AFFFFFF),
            labelTertiary = Color(0x4DFFFFFF),
            labelQuaternary = Color(0x26FFFFFF),
            labelOnColor = Color(0xFFFFFFFF),
            primaryNeutral = Color(0xE6FFFFFF),
            pressBackground = Color(0x26FFFFFF),
            hover = Color(0x26FFFFFF),
            divider = Color(0x33FFFFFF),
            disabledNeutral = Color(0x4DFFFFFF),
            error = Color(0xFFFF6C61),
            mask = Color(0x99000000),
            blue = Color(0xFF1A8CFF),
            green = Color(0xFF24B232),
            orange = Color(0xFFF08222),
            red = Color(0xFFFF6C61),
            yellow = Color(0xFFE5A100),
            isLight = false,
        )
    }
}
