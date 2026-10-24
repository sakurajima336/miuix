// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * The nine-step COUI corner scale.
 *
 * The steps are `xs / s / m / l / xl / xxl / xxxl / xxxxl / full`; each step carries a nominal
 * radius, a "16_1" normal radius, a "16_1" large radius and a smoothing weight.
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
 * Corner radii for each step of [CouiCornerScale].
 *
 * Each step has a base radius plus a `Normal` and a `Large` variant introduced with ColorOS
 * 16.1; the 16.1 variants are the live values on current systems.
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

    /** The standard corner radius on ColorOS 16.1+. */
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

    /** The "large" corner radius variant on ColorOS 16.1+. */
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

    /** The pill radius used when a shape is fully rounded. */
    val Full: Dp = 72.dp

    /** The smoothing weight for the given step. */
    fun weight(scale: CouiCornerScale): Float = when (scale) {
        CouiCornerScale.Xs, CouiCornerScale.S -> 1.5f
        CouiCornerScale.M, CouiCornerScale.L -> 0.99f
        CouiCornerScale.Xl, CouiCornerScale.Xxl, CouiCornerScale.Xxxl, CouiCornerScale.Xxxxl -> 1.1f
        CouiCornerScale.Full -> CouiCornerWeights.FullRound
    }
}

/**
 * The corner-weight constants used to decide how smooth a corner is drawn.
 *
 * Counter-intuitively, a smaller weight means a smoother corner; at or above [NonWeight] the
 * corner is a plain circular arc.
 */
object CouiCornerWeights {
    /** At or above this weight the corner is a plain circular arc. */
    const val NonWeight: Float = 2.0f

    /** The default G1 smoothing weight. */
    const val DefaultG1: Float = 1.70f

    /** The default G2 smoothing weight. */
    const val DefaultG2: Float = 2.45f

    /** The smoothing weight for a normal 16.1 corner. */
    const val Normal161: Float = 2.5f

    /** The smoothing weight for a large 16.1 corner. */
    const val Large161: Float = 3.0f

    /** The smoothing weight for the fully-rounded (pill) case. */
    const val FullRound: Float = 1.0f

    /** `LERP_START` - the radius multiplier at the smoothest weight. */
    const val LerpStart: Float = 1.33f

    /** `LERP_STOP` - the radius multiplier at [NonWeight]. */
    const val LerpStop: Float = 1.0f

    /** `START_WEIGHT` - the lower bound of the weight ramp. */
    const val StartWeight: Float = 0.99f
}

/**
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
 * Whether a corner counts as "already a circle or pill": its radius reaches half the shorter
 * side. In that case the real smoothing weight applies; otherwise the weight is clamped up to
 * [CouiCornerWeights.NonWeight], which is why mid-sized corners on a tall rectangle come out
 * as plain arcs.
 */
fun couiIsFullyRoundedToCircle(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
): Boolean = radius >= minOf(kotlin.math.abs(left - right), kotlin.math.abs(top - bottom)) / 2f