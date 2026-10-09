// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours that COUI components use directly, as opposed to the theme attributes in
 * [CouiColorTokens].
 *
 * The split is real, not stylistic: the `couiColor*` theme attributes cover the *system* palette,
 * but individual widgets also reference their own colour resources by name. A switch, for
 * example, reads `switch_unchecked_bar_color` and `switch_outer_circle_color`, neither of which
 * is reachable through `Theme.COUI.Main`.
 *
 * Every value below was read from the ColorOS 17.0.0 resource table, including the `-night`
 * qualifier where one exists.
 */
@Immutable
data class CouiComponentColors(
    /**
     * `?attr/couiColorControls` - the track when the switch is off.
     *
     * Note this is **translucent**, and it is *not* `switch_unchecked_bar_color`. `COUISwitch`
     * reads its track colours from theme attributes in `initResValue`
     * (`mBarUnCheckedColor = COUIContextUtil.getAttrColor(ctx, R.attr.couiColorControls)`), so the
     * off-track composites over whatever is behind it rather than being a flat grey.
     */
    val switchTrackOff: Color,

    /** `?attr/couiColorPressBackground` - the track when off and disabled. */
    val switchTrackOffDisabled: Color,

    /** `switch_outer_circle_color` - the thumb. Has no `-night` variant. */
    val switchThumb: Color,

    /** `switch_outer_circle_disable_color`. */
    val switchThumbDisabled: Color,

    /**
     * `coui_seekbar_background_selector.xml` - the seek bar's track.
     *
     * Both states resolve to `?attr/couiColorContainer12`, so the track does not change when the
     * bar is disabled; only the progress bar and thumb do.
     */
    val seekBarTrack: Color,

    /** `coui_color_white` - the seek bar's thumb. Has no `-night` variant. */
    val seekBarThumb: Color,

    /** `coui_seekbar_thumb_disable_color`. */
    val seekBarThumbDisabled: Color,

    /**
     * `coui_seekbar_thumb_shadow_color`.
     *
     * Only used by the gradient/shadow layers of `TexturedThumbRenderer`, i.e. while pressed.
     */
    val seekBarThumbShadow: Color,
) {
    companion object {
        /** The light values. */
        val Light: CouiComponentColors = CouiComponentColors(
            switchTrackOff = Color(0x29000000),
            switchTrackOffDisabled = Color(0x14000000),
            switchThumb = Color(0xFFFFFFFF),
            switchThumbDisabled = Color(0x8AFFFFFF),
            // `coui_color_container12` - 12% black.
            seekBarTrack = Color(0x1F000000),
            seekBarThumb = Color(0xFFFFFFFF),
            seekBarThumbDisabled = Color(0x8AFFFFFF),
            seekBarThumbShadow = Color(0x1A000000),
        )

        /** The `-night` values. */
        val Dark: CouiComponentColors = CouiComponentColors(
            switchTrackOff = Color(0x40FFFFFF),
            switchTrackOffDisabled = Color(0x26FFFFFF),
            switchThumb = Color(0xFFFFFFFF),
            switchThumbDisabled = Color(0x29FFFFFF),
            // `coui_color_container12_dark` - 20% white.
            seekBarTrack = Color(0x33FFFFFF),
            // `coui_color_white` has no night variant.
            seekBarThumb = Color(0xFFFFFFFF),
            seekBarThumbDisabled = Color(0x8AFFFFFF),
            // `coui_seekbar_thumb_shadow_color_dark`.
            seekBarThumbShadow = Color(0x33FFFFFF),
        )
    }
}