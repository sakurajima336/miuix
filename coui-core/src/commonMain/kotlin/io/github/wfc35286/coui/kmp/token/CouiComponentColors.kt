// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.token
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color


/**
 * Colours that COUI components use directly, as opposed to the theme attributes in
 * [CouiColorTokens].
 *
 * The split is real, not stylistic: the theme colour attributes cover the *system* palette, but
 * individual widgets also reference their own component-local colours, which are not reachable
 * through the theme. Both the light and the dark (night) variants are provided.
 */
@Immutable
data class CouiComponentColors(
    /**
     * The track when the switch is off.
     *
     * Note this is **translucent**: the off-track composites over whatever is behind it rather
     * than being a flat grey.
     */
    val switchTrackOff: Color,

    /** The track when the switch is off and disabled. */
    val switchTrackOffDisabled: Color,

    /** The switch thumb. Has no dark (night) variant. */
    val switchThumb: Color,

    /** The switch thumb when disabled. */
    val switchThumbDisabled: Color,

    /**
     * The seek bar's track.
     *
     * The track does not change when the bar is disabled; only the progress bar and thumb do.
     */
    val seekBarTrack: Color,

    /** The seek bar's thumb. Has no dark (night) variant. */
    val seekBarThumb: Color,

    /** The seek bar's thumb when disabled. */
    val seekBarThumbDisabled: Color,

    /**
     * The seek bar's thumb shadow.
     *
     * Only used by the gradient/shadow layers of the textured thumb, i.e. while pressed.
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
            // The track: 12% black.
            seekBarTrack = Color(0x1F000000),
            seekBarThumb = Color(0xFFFFFFFF),
            seekBarThumbDisabled = Color(0x8AFFFFFF),
            seekBarThumbShadow = Color(0x1A000000),
        )

        /** The dark (night) values. */
        val Dark: CouiComponentColors = CouiComponentColors(
            switchTrackOff = Color(0x40FFFFFF),
            switchTrackOffDisabled = Color(0x26FFFFFF),
            switchThumb = Color(0xFFFFFFFF),
            switchThumbDisabled = Color(0x29FFFFFF),
            // The track: 20% white.
            seekBarTrack = Color(0x33FFFFFF),
            // The thumb has no dark variant.
            seekBarThumb = Color(0xFFFFFFFF),
            seekBarThumbDisabled = Color(0x8AFFFFFF),
            // The dark thumb shadow.
            seekBarThumbShadow = Color(0x33FFFFFF),
        )
    }
}
