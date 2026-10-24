// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max


/**
 * Which edge stays put while a surface deforms during a touch gesture.
 *
 * The anchor sits on the **opposite** side of the drag: pulling left pins the right edge and the
 * left edge stretches out. Values are the pivot expressed as a fraction of the surface, so they can
 * be fed straight into `graphicsLayer.transformOrigin` (or a `View`'s `pivotX/pivotY`).
 */
enum class CouiDeformAnchor(val originX: Float, val originY: Float) {
    /** Pins the centre of the top edge. */
    Top(0.5f, 0f),

    /** Pins the centre of the left edge. */
    Left(0f, 0.5f),

    /** Pins the centre of the bottom edge. */
    Bottom(0.5f, 1f),

    /** Centres the pivot; the fallback when no anchor has been resolved yet. */
    Center(0.5f, 0.5f),

    /** Pins the centre of the right edge. */
    Right(1f, 0.5f),
}

/**
 * Resolves the deform anchor for the current drag travel:
 *
 * - `|dx| == |dy|`: keep the anchor already in use (or pick top/bottom from the sign of `dy` when
 *   there is none yet);
 * - `|dx| > |dy|`: pulling left pins the right edge, pulling right pins the left edge;
 * - `|dx| < |dy|`: pulling up pins the bottom edge, pulling down pins the top edge.
 *
 * @return `null` when the finger has not moved at all; the caller keeps the previously resolved
 *   anchor in that case.
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
 * The area-conserving stretch applied while a surface is dragged.
 *
 * Each axis is stretched by its own distance and the two are blended in the **log domain** with
 * weights `|dx| / (|dx| + |dy|)` and `|dy| / (|dx| + |dy|)`. Every result satisfies `sx * sy == 1`,
 * so the pill keeps its area while it deforms - that is what makes the movement read as a squash
 * rather than a zoom.
 *
 * The per-axis stretch is:
 *
 * ```
 * r     = couiRubberBand(d, curveRatio, maxRubberPx)
 * along = 1 + r / maxRubberPx * (maxScale - 1)
 * perp  = 1 / along
 * ```
 *
 * @param dx Horizontal travel from the touch-down point, in pixels.
 * @param dy Vertical travel from the touch-down point, in pixels.
 * @param maxScale The ceiling from [couiCalculateScaleWithK] - the real cap for this surface.
 * @param maxRubberPx The maximum rubber-band distance, in pixels (150dp).
 * @param curveRatio The rubber-band curve ratio (0.55).
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
    // One axis only: the other is the exact reciprocal (a zero distance on one axis bypasses
    // the log-domain blend).
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
 * The touch dead zone before a deform starts, in **raw pixels**, not dp - the value is not run
 * through a density conversion, so it is 5px on every device.
 */
const val CouiDeformDeadZonePx: Float = 5f

/**
 * How the press scale and the deform stretch combine into the final scale.
 *
 * The two channels' deviations from 1.0 are **added**, so the result is
 * `1 + (press - 1) + (deform - 1)`. It is deliberately not `press * deform`: with a multiply,
 * pressing shrinks the surface and a simultaneous drag stretch has to fight that shrink, so the
 * deform barely shows up and the whole gesture reads as "shrink only".
 *
 * @param press The press channel's scale, `1.0` when not pressed.
 * @param deform The deform channel's scale for this axis, `1.0` when not dragging.
 */
fun couiComposeScale(press: Float, deform: Float): Float = 1f + (press - 1f) + (deform - 1f)

/**
 * The release spring, picked by surface size.
 *
 * The bounce is always `0.65` (the one from [CouiTouchTiers.HandUpSpring]); only the response
 * moves, from `0.42` for a control narrower than [CouiTouchTiers.HandUpSmallAreaRefWidthDp] (56dp) to
 * `0.45` at/after [CouiTouchTiers.HandUpLargeAreaRefWidthDp] (100dp). A 56dp round button
 * therefore releases slightly faster than a 376dp pill does.
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
    // The easing applied to t is within a rounding step of linear over the 0.03 response span,
    // so the interpolation is done directly here.
    return CouiSpringData(
        bounce = CouiTouchTiers.HandUpSpring.bounce,
        response = CouiTouchTiers.HandUpSpring.response +
            (CouiTouchTiers.HandUpSpringLargeArea.response - CouiTouchTiers.HandUpSpring.response) * t,
    )
}
