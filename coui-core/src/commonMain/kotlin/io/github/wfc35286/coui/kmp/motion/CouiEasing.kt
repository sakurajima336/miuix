// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable


/**
 * A COUI cubic-Bézier curve descriptor.
 *
 * The control points are the exact interpolator values, not approximations fitted from
 * recordings.
 */
@Immutable
data class CouiBezierData(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
) {
    /** Converts to a Compose [Easing]. */
    fun toEasing(): Easing = CubicBezierEasing(x1, y1, x2, y2)

    companion object {
        /** The default curve, equivalent to CSS `ease`. */
        val Default: CouiBezierData = CouiBezierData(0.25f, 0.1f, 0.25f, 1.0f)
    }
}

/**
 * The standard easing curves of the COUI design system.
 *
 * [MoveEase] is the one to reach for by default: it is COUI's standard movement curve and shows
 * up across the component set.
 */
object CouiEasing {
    /** A symmetric ease-in-out curve, `(0.33, 0, 0.67, 1)` - identical to CSS `ease`. */
    val Ease: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

    /** `(0.3, 0, 0.1, 1)`. **COUI's standard movement curve.** */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** A decelerate curve, `(0.3, 0, 1, 1)`. */
    val OutEase: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** An accelerate curve, `(0, 0, 0.1, 1)`. */
    val InEase: Easing = CubicBezierEasing(0f, 0f, 0.1f, 1f)

    /** Linear interpolation. */
    val Linear: Easing = LinearEasing

    /**
     * `(0, 0, 0, 1)`. Maps both the area channel and the aspect-ratio channel of the
     * press-scale formula.
     */
    val PressMapping: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /**
     * Blends the hand-up spring between the small-area and large-area variants.
     */
    val HandUpArea: Easing = CubicBezierEasing(0.5502645f, 0f, 0.8915344f, 0.1878307f)
}
