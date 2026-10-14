// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable

/**
 * COUI's cubic-Bézier descriptors, a port of `com.coui.iola.BezierData`.
 *
 * The control points below were read out of the interpolator classes rather than fitted from
 * recordings, so they are exact.
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
        /** `BezierData`'s no-arg default - CSS `ease`. */
        val Default: CouiBezierData = CouiBezierData(0.25f, 0.1f, 0.25f, 1.0f)
    }
}

/**
 * The easing curves COUI uses, all measured from the interpolator classes.
 *
 * [MoveEase] is the one to reach for by default: it is COUI's standard movement curve and shows
 * up across the component set.
 */
object CouiEasing {
    /** `COUIEaseInterpolator` - `(0.33, 0, 0.67, 1)`, identical to CSS `ease`. */
    val Ease: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

    /** `COUIMoveEaseInterpolator` - `(0.3, 0, 0.1, 1)`. **COUI's standard movement curve.** */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** `COUIOutEaseInterpolator` - `(0.3, 0, 1, 1)`, a decelerate curve. */
    val OutEase: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** `COUIInEaseInterpolator` - `(0, 0, 0.1, 1)`, an accelerate curve. */
    val InEase: Easing = CubicBezierEasing(0f, 0f, 0.1f, 1f)

    /** `COUILinearInterpolator`. */
    val Linear: Easing = LinearEasing

    /**
     * `PressParams.bezier` - `(0, 0, 0, 1)`. Maps both the area channel and the aspect-ratio
     * channel of the press-scale formula.
     */
    val PressMapping: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /**
     * `TouchMotionTierParams.iolHandUpAreaBezier` - `(0.5502645, 0, 0.8915344, 0.1878307)`.
     * Blends the hand-up spring between the small-area and large-area variants.
     */
    val HandUpArea: Easing = CubicBezierEasing(0.5502645f, 0f, 0.8915344f, 0.1878307f)
}