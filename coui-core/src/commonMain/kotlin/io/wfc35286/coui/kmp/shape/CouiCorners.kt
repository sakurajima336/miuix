// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The nine-step COUI corner scale.
 *
 * ColorOS names its corners `xs / s / m / l / xl / xxl / xxxl / xxxxl / full` and each step
 * carries four numbers: a nominal size, a nominal radius, a "16_1" normal radius, a "16_1"
 * large radius, and a smoothing weight. All of them are reproduced verbatim below from
 * `coui_round_corner_*` in the ColorOS 17.0.0 resource table - none of them are eyeballed.
 */
enum class CouiCornerScale {
    Xs,
    S,
    M,
    L,
    Xl,
    Xxl,
    Xxxl,
    Xxxxl,

    /** The pill / fully-rounded step. It has no radius or weight of its own. */
    Full,
}

/**
 * Corner radii, straight out of `coui_round_corner_*`.
 *
 * | step | `_radius` | `_radius_normal_16_1` | `_radius_large_16_1` | `_weight` |
 * |---|---|---|---|---|
 * | xs | 4 | 5 | 5 | 1.5 |
 * | s | 8 | 10 | 11 | 1.5 |
 * | m | 9 | 15 | 17 | 0.99 |
 * | l | 13 | 19 | 23 | 0.99 |
 * | xl | 16 | 24 | 28 | 1.1 |
 * | xxl | 19 | 29 | 34 | 1.1 |
 * | xxxl | 22 | 34 | 39 | 1.1 |
 * | xxxxl | 25 | 38 | 45 | 1.1 |
 *
 * The `_radius` column is the pre-16.1 value; on this device (`getOSVersionCode() = 40`,
 * i.e. past the 0x25 threshold) the `_16_1` columns are the live ones.
 */
object CouiCornerRadius {
    val Xs: Dp = 4.dp
    val S: Dp = 8.dp
    val M: Dp = 9.dp
    val L: Dp = 13.dp
    val Xl: Dp = 16.dp
    val Xxl: Dp = 19.dp
    val Xxxl: Dp = 22.dp
    val Xxxxl: Dp = 25.dp

    /** `coui_round_corner_*_radius_normal_16_1` - the standard size on ColorOS 16.1+. */
    object Normal {
        val Xs: Dp = 5.dp
        val S: Dp = 10.dp
        val M: Dp = 15.dp
        val L: Dp = 19.dp
        val Xl: Dp = 24.dp
        val Xxl: Dp = 29.dp
        val Xxxl: Dp = 34.dp
        val Xxxxl: Dp = 38.dp
    }

    /** `coui_round_corner_*_radius_large_16_1` - the "large" variant on ColorOS 16.1+. */
    object Large {
        val Xs: Dp = 5.dp
        val S: Dp = 11.dp
        val M: Dp = 17.dp
        val L: Dp = 23.dp
        val Xl: Dp = 28.dp
        val Xxl: Dp = 34.dp
        val Xxxl: Dp = 39.dp
        val Xxxxl: Dp = 45.dp
    }

    /** `coui_round_corner_full` - the pill radius used when a shape is fully rounded. */
    val Full: Dp = 72.dp

    /** `coui_round_corner_<scale>_weight`. */
    fun weight(scale: CouiCornerScale): Float = when (scale) {
        CouiCornerScale.Xs, CouiCornerScale.S -> 1.5f
        CouiCornerScale.M, CouiCornerScale.L -> 0.99f
        CouiCornerScale.Xl, CouiCornerScale.Xxl, CouiCornerScale.Xxxl, CouiCornerScale.Xxxxl -> 1.1f
        CouiCornerScale.Full -> CouiCornerWeights.FullRound
    }
}

/**
 * The corner-weight constants ColorOS uses to decide how smooth a corner is drawn.
 *
 * These come from `OplusSmoothRoundedManager` and `RoundCornerUtil`; the default G1/G2 weights
 * are read from system properties and fall back to the values below, which were confirmed on
 * this device via an on-device probe.
 */
object CouiCornerWeights {
    /** `NON_WEIGHT`. At or above this the corner is a plain circular arc. */
    const val NonWeight: Float = 2.0f

    /** `DEFAULT_G1_WEIGHT` - `persist.sys.oplus.default_smooth_weight` (0xaa) / 100. */
    const val DefaultG1: Float = 1.70f

    /** `DEFAULT_G2_WEIGHT` - `persist.sys.oplus.default_g2_weight` (0xf5) / 100. */
    const val DefaultG2: Float = 2.45f

    /** `RoundCornerUtil.WEIGHT_16_1_NORMAL`. */
    const val Normal161: Float = 2.5f

    /** `RoundCornerUtil.WEIGHT_16_1_LARGE`. */
    const val Large161: Float = 3.0f

    /** `RoundCornerUtil.OS_16_1_FULL_ROUND_SMOOTH_WEIGHT` - the pill case. */
    const val FullRound: Float = 1.0f

    /** `LERP_START` - the radius multiplier at the smoothest weight. */
    const val LerpStart: Float = 1.33f

    /** `LERP_STOP` - the radius multiplier at [NonWeight]. */
    const val LerpStop: Float = 1.0f

    /** `START_WEIGHT` - the lower bound of the weight ramp. */
    const val StartWeight: Float = 0.99f
}

/**
 * `OplusSmoothRoundedManager.reverseNoWeightRadius(radius, weight)`.
 *
 * Converts a "smooth" radius back into the radius that a plain `addRoundRect` would need in
 * order to look the same size. Counter-intuitively, **a smaller weight means a smoother
 * corner** - [CouiCornerWeights.NonWeight] (2.0) is the boundary at which smoothing is off.
 *
 * The multiplier ramps linearly from 1.33 at weight 0.99 down to 1.0 at weight 2.0, and is
 * flat outside that range.
 */
fun couiReverseNoWeightRadius(radius: Float, weight: Float): Float {
    if (weight < CouiCornerWeights.StartWeight || weight > CouiCornerWeights.NonWeight) {
        return radius
    }
    val amount = ((weight - CouiCornerWeights.StartWeight) /
        (CouiCornerWeights.NonWeight - CouiCornerWeights.StartWeight)).coerceIn(0f, 1f)
    return radius * (CouiCornerWeights.LerpStart +
        (CouiCornerWeights.LerpStop - CouiCornerWeights.LerpStart) * amount)
}

/**
 * `OplusSmoothRoundedManager.isFullyRoundedToCircle(...)`.
 *
 * A corner counts as "already a circle or pill" once its radius reaches half the shorter side.
 * ColorOS only forwards the real weight to the native path in that case; otherwise it clamps
 * the weight up to [CouiCornerWeights.NonWeight], which is why mid-sized corners on a tall
 * rectangle come out as plain arcs.
 */
fun couiIsFullyRoundedToCircle(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
): Boolean = radius >= minOf(kotlin.math.abs(left - right), kotlin.math.abs(top - bottom)) / 2f