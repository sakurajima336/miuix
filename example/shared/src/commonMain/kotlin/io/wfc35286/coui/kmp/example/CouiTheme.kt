// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.isInDarkTheme

/**
 * Layout constants of the COUI design system.
 *
 * All values were read from the `coui_*` dimension entries of ColorOS Settings 17.0.0
 * (`resources.arsc`), so they match the real component metrics.
 *
 * The switch metrics are the exception: `COUISwitch` draws itself in code and exposes no
 * dimensions, so those four numbers are reconstructed from the rendered component rather than
 * extracted. They are marked accordingly.
 */
object CouiDimens {
    // --- Preference list item (from res/layout/coui_preference.xml) ---
    /** `support_preference_min_height` — minimum height of a list row. */
    val PreferenceMinHeight: Dp = 48.dp

    /** `support_preference_title_padding_start` — leading inset of the row content. */
    val PreferencePaddingStart: Dp = 32.dp

    /** `support_preference_title_padding_end` — trailing inset of the row content. */
    val PreferencePaddingEnd: Dp = 32.dp

    /** `support_preference_title_padding_start` — leading inset of a row that has no icon. */
    val PreferencePaddingNoIcon: Dp = 16.dp

    /** `coui_preference_divider_default_horizontal_padding` — inset of the hairline divider. */
    val PreferenceDividerInset: Dp = 32.dp

    /** `coui_list_divider_height` — hairline thickness (0.33dp in the APK). */
    val DividerHeight: Dp = 0.33.dp

    /** `coui_preference_widget_switch_margin_left` — gap before a trailing widget. */
    val WidgetMarginStart: Dp = 16.dp

    /** `support_preference_widget_focus_jump_padding_top` / `_bottom`. */
    val WidgetPaddingVertical: Dp = 12.dp

    /** `coui_preference_status_text_max_width` — max width of the trailing status text. */
    val StatusTextMaxWidth: Dp = 162.dp

    // --- Category header (from res/layout/coui_preference_category_layout.xml) ---

    /** `support_preference_category_layout_title_margin_start_small`. */
    val CategoryTitleMarginStart: Dp = 16.dp

    /** `coui_category_title_pading_end_with_reddot_default`. */
    val CategoryTitleMarginEnd: Dp = 8.dp

    /** `support_preference_category_layout_widgetlayout_margin_horizontal`. */
    val CategoryWidgetMarginStart: Dp = 8.dp

    // --- Search bar (from res/layout/coui_search_view_animate_layout.xml) ---

    /** `coui_search_view_height` — height of the settings search field. */
    val SearchViewHeight: Dp = 52.dp

    /** `coui_search_view_icon_size`. */
    val SearchIconSize: Dp = 24.dp

    /** `coui_search_view_search_icon_margin`. */
    val SearchIconMargin: Dp = 12.dp

    /** Height of the compact search row in the app bar (`coui_search_view_animate_layout`). */
    val SearchRowHeight: Dp = 40.dp

    // --- Tab bar (from res/layout/sliding_tab_indicator_view.xml) ---

    /** `pager_tabs_selected_indicator_height` — thickness of the tab indicator. */
    val TabIndicatorHeight: Dp = 3.dp

    /** `pager_tabs_title_padding`. */
    val TabTitlePadding: Dp = 16.dp

    /** Tab title row height, from `coui_tab_layout_text`. */
    val TabHeight: Dp = 48.dp

    // --- Chip (from coui_chip_* dimens) ---

    /** `coui_chip_default_min_height`. */
    val ChipMinHeight: Dp = 28.dp

    /** `coui_chip_default_min_width`. */
    val ChipMinWidth: Dp = 52.dp

    /** `coui_chip_group_default_spacing_horizontal`. */
    val ChipSpacing: Dp = 8.dp

    // --- SeekBar ---
    // The canonical seek bar metrics live in `CouiSeekBarDefaults`. These aliases used to hold the
    // `coui_seekbar_intent_*` values (4dp / 4dp / 5dp), which belong to the *intent* variant and
    // are roughly a third of the real size. They now mirror the real track.

    /** `coui_seekbar_background_height` — the track is a full capsule, radius = height / 2. */
    val SeekBarTrackHeight: Dp = 20.dp

    /** `coui_seekbar_thumb_out_radius`. */
    val SeekBarThumbRadius: Dp = 6.dp

    /** `coui_seekbar_thumb_max_radius`. */
    val SeekBarThumbMaxRadius: Dp = 8.dp

    /** `coui_seekbar_view_min_height`. */
    val SeekBarHeight: Dp = 36.dp

    // --- Switch ---
    // `COUISwitch` draws itself in code; the numbers come from `COUISwitchStyle` plus the
    // `onMeasure` formula. See `CouiSwitchDefaults` for the live values.

    /** `bar_width`. */
    val SwitchWidth: Dp = 44.dp

    /** `bar_height`. */
    val SwitchHeight: Dp = 24.dp

    /** `outer_circle_width`. */
    val SwitchThumbSize: Dp = 18.dp

    /** `circle_padding`. */
    val SwitchThumbPadding: Dp = 3.dp

    // --- Checkbox / radio (from coui_btn_checkbox_* dimens) ---

    /** `coui_btn_checkbox_padding_left`. */
    val CheckboxPaddingStart: Dp = 6.dp

    /** `coui_checkbox_margin_between_text_drawable`. */
    val CheckboxTextSpacing: Dp = 8.dp

    /** Box size, from the `coui_btn_check` drawable bounds. */
    val CheckboxSize: Dp = 20.dp

    /** `coui_preference_checkbox_margin_right` (negative in the APK; kept positive here). */
    val CheckboxMarginEnd: Dp = 10.dp

    // --- Cards & buttons (from the coui_*_preference layouts) ---

    /** `coui_preference_card_margin_horizontal`. */
    val CardMarginHorizontal: Dp = 16.dp

    /**
     * Corner radius of a COUI card.
     *
     * Measured on-device: a settings card renders a 55.5px arc at density 476, i.e. 18.7dp, which
     * matches `coui_round_corner_l_radius_normal_16_1` (19dp) within the anti-aliasing bias.
     */
    val CardCornerRadius: Dp = CouiCorners.L.radiusNormal

    /** `coui_delete_alert_dialog_button_height`. */
    val ButtonHeight: Dp = 48.dp

    /** `coui_dialog_layout_margin_horizontal`. */
    val DialogMarginHorizontal: Dp = 16.dp
}

/**
 * The COUI corner-radius scale.
 *
 * ColorOS does not use ad-hoc radii: every corner comes from a named step (`xs` … `xxxxl`, plus
 * `full`). Each step carries three numbers:
 *
 * - `Radius` — the pre-QPR1 value (`coui_round_corner_<step>_radius`)
 * - `RadiusNormal` / `RadiusLarge` — the Android 16 QPR1 values, selected by screen width
 * - `Weight` — the smooth-corner weight
 *
 * ### About `Weight`
 *
 * The weight drives the OPlus *G2 smooth corner* (a superellipse). Its Java-side effect is to
 * scale the radius by `lerp(1.33, 1.0, (weight - 0.99) / 1.01)`; `2.0` means "no smoothing".
 * All COUI steps use 0.99 / 1.1 / 1.5, i.e. they all *ask* for smoothing.
 *
 * On the device we tested (realme RMX3820, Android 17) the smoothing is not observable in the
 * rendered output — settings cards measure as plain circular arcs with sub-pixel residuals. The
 * G2 path is also gated to shapes that are already pills (`radius >= min(w, h) / 2`), so ordinary
 * rounded rectangles never take it. [CouiDimens.CardCornerRadius] therefore uses a plain radius;
 * see [CouiMotion.smoothRadiusScale] if you need the weighted value.
 */
object CouiCorners {
    /** One step of the corner scale. */
    data class Step(
        val radius: Dp,
        val radiusNormal: Dp,
        val radiusLarge: Dp,
        val weight: Float,
    )

    /** `coui_round_corner_xs`. */
    val XS = Step(4.dp, 5.dp, 5.dp, 1.5f)

    /** `coui_round_corner_s`. */
    val S = Step(8.dp, 10.dp, 11.dp, 1.5f)

    /** `coui_round_corner_m`. */
    val M = Step(9.dp, 15.dp, 17.dp, 0.99f)

    /** `coui_round_corner_l`. */
    val L = Step(13.dp, 19.dp, 23.dp, 0.99f)

    /** `coui_round_corner_xl`. */
    val XL = Step(16.dp, 24.dp, 28.dp, 1.1f)

    /** `coui_round_corner_xxl`. */
    val XXL = Step(19.dp, 29.dp, 34.dp, 1.1f)

    /** `coui_round_corner_xxxl`. */
    val XXXL = Step(22.dp, 34.dp, 39.dp, 1.1f)

    /** `coui_round_corner_xxxxl`. */
    val XXXXL = Step(25.dp, 38.dp, 45.dp, 1.1f)

    /** `coui_round_corner_full` — used for pill-shaped containers. */
    val Full: Dp = 72.dp

    /** Every step, ordered smallest to largest. */
    val All: List<Step> = listOf(XS, S, M, L, XL, XXL, XXXL, XXXXL)

    // --- Standalone radii that are not part of the scale ---

    /** `coui_alert_dialog_bg_radius`. */
    val AlertDialog: Dp = 22.dp

    /** `coui_dialog_background_corner_radius`. */
    val Dialog: Dp = 24.dp

    /** `coui_dialog_background_corner_radius_rotate` — landscape dialogs use half of [Dialog]. */
    val DialogRotate: Dp = 12.dp

    /** `coui_snack_bar_radius`. */
    val SnackBar: Dp = 16.dp

    /** `coui_toptips_view_radius_os16`. */
    val TopTips: Dp = 20.dp
}

/** The composition local holding the active [CouiColors]. */
val LocalCouiColors = staticCompositionLocalOf { CouiColors.Light }

/**
 * Provides the COUI color palette to [content].
 *
 * The palette follows the app's light/dark mode by default, so it can be nested anywhere inside
 * the Miuix theme without extra wiring:
 *
 * ```kotlin
 * CouiTheme {
 *     CouiSwitch(checked = checked, onCheckedChange = { checked = it })
 * }
 * ```
 *
 * @param dark Whether to use [CouiColors.Dark]. Defaults to the app's current dark-mode state.
 * @param content The content that reads the palette through [CouiTheme.colors].
 */
@Composable
fun CouiTheme(
    dark: Boolean = isInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) CouiColors.Dark else CouiColors.Light
    CompositionLocalProvider(
        LocalCouiColors provides colors,
        content = content,
    )
}

/**
 * Entry point for the COUI design tokens, mirroring how `MiuixTheme` exposes its colour scheme.
 */
object CouiTheme {
    /** The active COUI palette. */
    val colors: CouiColors
        @Composable @ReadOnlyComposable
        get() = LocalCouiColors.current
}
