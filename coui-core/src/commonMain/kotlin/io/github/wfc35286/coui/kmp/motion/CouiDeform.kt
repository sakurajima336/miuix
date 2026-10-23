// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * `COUITouchMotionAnimator$DeformAnchor` - which edge stays put while the surface deforms.
 *
 * The anchor sits on the **opposite** side of the drag: pulling left pins the right edge and the
 * left edge stretches out. Values are the pivot expressed as a fraction of the surface, so they can
 * be fed straight into `graphicsLayer.transformOrigin` (or a `View`'s `pivotX/pivotY`).
 */
enum class CouiDeformAnchor(val originX: Float, val originY: Float) {
    /** `TOP_CENTER`. */
    Top(0.5f, 0f),

    /** `LEFT_CENTER`. */
    Left(0f, 0.5f),

    /** `BOTTOM_CENTER`. */
    Bottom(0.5f, 1f),

    /** `CENTER` - the fallback when no anchor has been resolved yet. */
    Center(0.5f, 0.5f),

    /** `RIGHT_CENTER`. */
    Right(1f, 0.5f),
}

/**
 * `COUITouchMotionAnimator.resolveAutoDeformAnchor(dx, dy)`.
 *
 * Resolution is purely a function of the current travel:
 *
 * - `|dx| == |dy|`: keep the anchor already in use (or pick top/bottom from the sign of `dy` when
 *   there is none yet);
 * - `|dx| > |dy|`: pulling left pins the right edge, pulling right pins the left edge;
 * - `|dx| < |dy|`: pulling up pins the bottom edge, pulling down pins the top edge.
 *
 * @return `null` when the finger has not moved at all - the original returns `null` there too and
 *   the previously resolved anchor is kept.
 */
fun couiResolveDeformAnchor(dx: Float, dy: Float, current: CouiDeformAnchor?): CouiDeformAnchor? {
    val ax = abs(dx)
    val ay = abs(dy)
    if (ax == 0f && ay == 0f) return null
    if (ax == ay) return current ?: if (dy < 0f) CouiDeformAnchor.Bottom else CouiDeformAnchor.Top
    if (ax > ay) return if (dx < 0f) CouiDeformAnchor.Right else CouiDeformAnchor.Left
    return if (dy < 0f) CouiDeformAnchor.Bottom else CouiDeformAnchor.Top
}

/**
 * `COUIDeformAnimatorUnit.calculateScaleFactors(dx, dy)` - the area-conserving stretch.
 *
 * Each axis is stretched by its own distance and the two are blended in the **log domain** with
 * weights `|dx| / (|dx| + |dy|)` and `|dy| / (|dx| + |dy|)` (`blendAreaConservingScaleFactors`).
 * Every result satisfies `sx * sy == 1`, so the pill keeps its area while it deforms - that is what
 * makes the movement read as a squash rather than a zoom.
 *
 * The per-axis stretch is `calculateDirectionScale`:
 *
 * ```
 * r     = TouchMotionRubberBand.apply(d, curveRatio, maxRubberPx)
 * along = 1 + r / maxRubberPx * (maxScale - 1)
 * perp  = 1 / along
 * ```
 *
 * @param dx Horizontal travel from the touch-down point, in pixels.
 * @param dy Vertical travel from the touch-down point, in pixels.
 * @param maxScale The ceiling from [couiCalculateScaleWithK] - the real cap for this surface.
 * @param maxRubberPx `iolMaxDeformRubberDistance` in pixels (150dp).
 * @param curveRatio `iolDeformRubberCurveRatio` (0.55).
 */
fun couiDeformScales(
    dx: Float,
    dy: Float,
    maxScale: Float,
    maxRubberPx: Float,
    curveRatio: Float = CouiTouchTiers.DeformRubberCurveRatio,
): Offset {
    val ax = abs(dx)
    val ay = abs(dy)
    if (ax + ay <= 0f) return Offset(1f, 1f)
    if (maxRubberPx <= 0f || maxScale <= 1f) return Offset(1f, 1f)

    fun along(distance: Float): Float {
        if (distance <= 0f) return 1f
        val r = couiRubberBand(distance, curveRatio, maxRubberPx)
        val s = 1f + r / maxRubberPx * (maxScale - 1f)
        return if (s > 0f) s else 1f
    }

    val sx = along(ax)
    val sy = along(ay)
    // One axis only: the other is the exact reciprocal (`calculateDirectionScale` returns
    // `(along, 1 / along)` / `(1 / along, along)` and bails out early for a zero distance).
    if (ay == 0f) return Offset(sx, 1f / sx)
    if (ax == 0f) return Offset(1f / sy, sy)

    val total = ax + ay
    val wx = ax / total
    val wy = ay / total
    return Offset(
        exp((ln(sx) * wx + ln(1f / sy) * wy).toDouble()).toFloat(),
        exp((ln(1f / sx) * wx + ln(sy) * wy).toDouble()).toFloat(),
    )
}

/**
 * `iolDeadZone` as used by `COUIITouchMotionUnit.checkDeadZone` - **raw pixels**, not dp.
 *
 * `CommonParams.iolDeadZone` defaults to `5.0f` and is never run through `dp2px` by
 * `TouchAnimatorParamProvider`, so the dead zone is 5px on every density.
 */
const val CouiDeformDeadZonePx: Float = 5f

/**
 * How the press scale and the deform stretch combine into the final scale.
 *
 * `COUITouchMotionAnimator$TouchMotionCollector.put` accumulates about 1.0:
 *
 * ```
 * mScaleX += value - 1.0f      // for both PRESS_SCALE and DEFORM_SCALE
 * ```
 *
 * so the result is `1 + (press - 1) + (deform - 1)` - an **additive** mix of the two channels'
 * deviations from 1.0. It is emphatically not `press * deform`: with a multiply, pressing shrinks
 * the surface and a simultaneous drag stretch has to fight that shrink, so the deform barely shows
 * up and the whole gesture reads as "shrink only". Verified against the smali at
 * `TouchMotionCollector.smali` (`sub-float` then `add-float` on `mScaleX` / `mScaleY`).
 *
 * @param press The press channel's scale, `1.0` when not pressed.
 * @param deform The deform channel's scale for this axis, `1.0` when not dragging.
 */
fun couiComposeScale(press: Float, deform: Float): Float = 1f + (press - 1f) + (deform - 1f)

/**
 * `TouchMotionHandUpSpringResolver.resolve(w, h)` - the release spring, picked by surface size.
 *
 * The bounce is always `0.65`; only the response moves, from `0.42` for a control narrower than
 * `iolHandUpSmallAreaRefWidthDp` (56dp) to `0.45` at/after `iolHandUpLargeAreaRefWidthDp`
 * (100dp). A 56dp round button therefore releases slightly faster than a 376dp pill does - one of
 * the few places where the three controls in the gallery are genuinely not identical.
 *
 * @param widthDp The control's width, in dp.
 * @param heightDp The control's height, in dp.
 */
fun couiHandUpSpring(widthDp: Float, heightDp: Float): CouiSpringData {
    val reference = max(widthDp, heightDp)
    val span = CouiTouchTiers.HandUpLargeAreaRefWidthDp - CouiTouchTiers.HandUpSmallAreaRefWidthDp
    val t = if (span <= 0f) {
        1f
    } else {
        ((reference - CouiTouchTiers.HandUpSmallAreaRefWidthDp) / span).coerceIn(0f, 1f)
    }
    // The official runs t through a bezier first; over a 0.03 response span that is within a
    // rounding step of linear, so the interpolation is done directly here.
    return CouiSpringData(
        bounce = CouiTouchTiers.HandUpSpring.bounce,
        response = CouiTouchTiers.HandUpSpring.response +
            (CouiTouchTiers.HandUpSpringLargeArea.response - CouiTouchTiers.HandUpSpring.response) * t,
    )
}
