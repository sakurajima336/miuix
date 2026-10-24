// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign


/**
 * The classic iOS-style rubber band: resistance grows the further you pull, asymptotically
 * approaching [maxDistance] but never reaching it.
 *
 * ```
 * t = ratio * |d| / max
 * result = sign(d) * max * (1 - 1 / (1 + t))
 * ```
 *
 * @param distance How far the finger has travelled, in pixels. May be negative.
 * @param ratio The curve ratio; see [CouiTouchTiers.RubberCurveRatio] and
 *   [CouiTouchTiers.DeformRubberCurveRatio].
 * @param maxDistance The asymptote. A non-positive value yields `0`.
 */
fun couiRubberBand(distance: Float, ratio: Float, maxDistance: Float): Float {
    if (maxDistance <= 0f) return 0f
    val t = ratio * abs(distance) / maxDistance
    return sign(distance) * maxDistance * (1f - 1f / (1f + t))
}

/**
 * The inverse of [couiRubberBand].
 *
 * Recovers the finger distance that would have produced [offset]. Distances at or beyond
 * [maxDistance] are clamped rather than extrapolated.
 *
 * @param offset The rubber-banded offset.
 * @param ratio The curve ratio.
 * @param maxDistance The asymptote.
 */
fun couiRubberBandInverse(offset: Float, ratio: Float, maxDistance: Float): Float {
    if (maxDistance <= 0f || ratio <= 0f) return 0f
    val clamped = offset.coerceIn(-maxDistance, maxDistance)
    if (abs(clamped) >= maxDistance) return sign(clamped) * maxDistance
    val denominator = 1f - abs(clamped) / maxDistance
    if (denominator <= 0f) return sign(clamped) * maxDistance
    return sign(clamped) * maxDistance / ratio * (1f / denominator - 1f)
}

/**
 * The adaptive cap on how far a large surface is allowed to scale: small surfaces get the full
 * [maxScale], and the allowance tapers off as area and aspect ratio grow.
 *
 * ```
 * area = (int)(w * h)
 * if (area <= 38416) return maxScale
 * ratio = max(w, h) / min(w, h)
 * t = min(1, (k1/1e6) * ln(area / 38416) + (k2/1e3) * (ratio - 1))
 * return max(1.03, 1.03 + (maxScale - 1.03) * (1 - t))
 * ```
 *
 * @param width The surface width, in pixels.
 * @param height The surface height, in pixels.
 * @param maxScale The scale to use when the surface is small enough to afford it.
 * @param k1 The area coefficient, raw. Defaults to [CouiTouchCommon.K1] (350000, i.e. 0.35 after
 *   the 1e6 divisor).
 * @param k2 The aspect-ratio coefficient, raw. Defaults to [CouiTouchCommon.K2] (15, i.e. 0.015
 *   after the 1e3 divisor).
 */
fun couiCalculateScaleWithK(
    width: Float,
    height: Float,
    maxScale: Float,
    k1: Float = CouiTouchCommon.K1,
    k2: Float = CouiTouchCommon.K2,
): Float {
    val area = (width * height).toInt()
    if (area <= AreaThreshold) return maxScale
    val ratio = max(width, height) / min(width, height)
    val t = min(
        1f,
        (k1 / CouiTouchCommon.K1Divisor) * ln(area.toFloat() / AreaThreshold) +
            (k2 / CouiTouchCommon.K2Divisor) * (ratio - 1f),
    )
    return max(ScaleFloor, ScaleFloor + (maxScale - ScaleFloor) * (1f - t))
}

/** `196 * 196`, the area below which [couiCalculateScaleWithK] returns [maxScale] untouched. */
const val AreaThreshold: Int = 38416

/** The floor [couiCalculateScaleWithK] can never go below. */
const val ScaleFloor: Float = 1.03f

/**
 * The two-channel press-scale formula.
 *
 * Channel one maps the surface **area** into `[minScale, maxScale]`. Channel two wakes up only
 * once the surface is at least [CouiPressScaleRange.AspectActivationThreshold] times as long as
 * it is wide, and pulls the scale back up towards
 * [CouiPressScaleRange.AspectExtremeScale] - so a long thin bar does not shrink as much as a
 * square of the same area. The two channels are averaged.
 *
 * @param width The surface width, in dp.
 * @param height The surface height, in dp.
 * @param minAreaWidth The area channel's lower bound, in dp; see
 *   [CouiPressScaleRange.MinAreaWidthDp].
 * @param maxAreaWidth The area channel's upper bound, in dp; see
 *   [CouiPressScaleRange.MaxAreaWidthDp].
 * @param minScale The scale at the largest area.
 * @param maxScale The scale at the smallest area.
 * @param useAreaOnly When `true`, the aspect-ratio channel is skipped entirely.
 * @return The target scale, clamped to
 *   [CouiPressScaleRange.TargetScaleMin]..[CouiPressScaleRange.TargetScaleMaxShrink].
 */
fun couiPressScale(
    width: Float,
    height: Float,
    minAreaWidth: Float = CouiPressScaleRange.MinAreaWidthDp,
    maxAreaWidth: Float = CouiPressScaleRange.MaxAreaWidthDp,
    minScale: Float = CouiPressScaleRange.ScaleRangeMin,
    maxScale: Float = CouiPressScaleRange.ScaleRangeMax,
    useAreaOnly: Boolean = false,
): Float {
    val area = width * height
    if (area <= 0f) return 1f

    val minArea = minAreaWidth * minAreaWidth
    val maxArea = maxAreaWidth * maxAreaWidth
    val clampedArea = area.coerceIn(minArea, maxArea)
    val t = if (maxArea - minArea <= 0f) 0f else (clampedArea - minArea) / (maxArea - minArea)
    val areaScale = minScale + CouiEasing.PressMapping.transform(t) * (maxScale - minScale)

    if (useAreaOnly) return areaScale.clampToTargetRange()

    val aspect = max(width, height) / min(width, height)
    if (aspect <= CouiPressScaleRange.AspectActivationThreshold) return areaScale.clampToTargetRange()

    val span = CouiPressScaleRange.AspectExtremeRatio - CouiPressScaleRange.AspectActivationThreshold
    val u = if (span <= 0f) {
        0f
    } else {
        ((aspect - CouiPressScaleRange.AspectActivationThreshold) / span).coerceIn(0f, 1f)
    }
    val aspectScale = CouiPressScaleRange.AspectBaseScale +
        CouiEasing.PressMapping.transform(u) *
        (CouiPressScaleRange.AspectExtremeScale - CouiPressScaleRange.AspectBaseScale)

    return ((areaScale + aspectScale) / 2f).clampToTargetRange()
}

private fun Float.clampToTargetRange(): Float =
    coerceIn(CouiPressScaleRange.TargetScaleMin, CouiPressScaleRange.TargetScaleMaxShrink)

/**
 * Turns a change in touch distance into an initial velocity for the release spring.
 *
 * @param d0 The distance at the previous sample.
 * @param d1 The distance at the current sample.
 * @param scale The press scale in effect.
 */
fun couiImpactVelocity(d0: Float, d1: Float, scale: Float): Float =
    -(d0 - d1) * scale * CouiPressScaleRange.ImpactVelocityDistanceScale

/**
 * The point in the switch's travel at which the thumb is stretched the most.
 *
 * The peak sits at **34.7%** of the travel, not at the halfway point.
 */
const val SwitchStretchPeakAt: Float = 0.34725847840309143f

/** The maximum thumb stretch, reached at [SwitchStretchPeakAt]. */
const val SwitchStretchPeak: Float = 1.3f

/** How much the thumb stretches, i.e. `SwitchStretchPeak - 1`. */
const val SwitchStretchAmplitude: Float = SwitchStretchPeak - 1f

/**
 * The thumb's horizontal stretch as a function of its travel.
 *
 * The thumb is a circle at both ends of the track and stretches into a capsule while travelling.
 * The curve is **piecewise linear**, with a peak of [SwitchStretchPeak] at [SwitchStretchPeakAt]
 * of the way across; a sinusoidal approximation would put the peak in the wrong place.
 *
 * @param fraction The normalised travel, `0` at one end and `1` at the other. Take
 *   `1 - fraction` first for RTL.
 */
fun couiSwitchThumbStretch(fraction: Float): Float {
    return if (fraction < SwitchStretchPeakAt) {
        1f + fraction / SwitchStretchPeakAt * SwitchStretchAmplitude
    } else {
        SwitchStretchPeak -
            (fraction - SwitchStretchPeakAt) / (1f - SwitchStretchPeakAt) * SwitchStretchAmplitude
    }
}