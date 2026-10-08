// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.capsule.continuities.G2Continuity
import com.kyant.capsule.continuities.G2ContinuityProfile

/**
 * The curvature families a COUI corner can be drawn with.
 *
 * `G0` is a sharp corner, `G1` is a plain circular arc (what `RoundedCornerShape` gives you),
 * and `G2` is curvature-continuous - the "continuous corner" look. ColorOS calls these
 * `SMOOTH_ROUND_CORNER_TYPE_UNSUPPORTED (-1)`, `OS15 (0)` and `OS16 (1)` respectively, and
 * this device reports `1`, so **G2 is what the real system is drawing**.
 */
enum class CouiContinuity {
    /** `G0Continuity` - sharp corners. */
    G0,

    /** `G1Continuity` - a quarter-circle arc, no corner smoothing. */
    G1,

    /** `G2Continuity` - curvature-continuous. The ColorOS default on OS 16+. */
    G2,
}

/**
 * Builds the [Continuity] implementation for a [CouiContinuity] family.
 *
 * Backed by the Apache-2.0 `zone.ien.capsule:capsule` library, which generates G2-continuous
 * rounded rectangles from pure Bézier/arc geometry.
 *
 * ## Not calibrated against ColorOS yet
 *
 * ColorOS's G2 silhouette is produced natively by `IPathExt.addSmoothRoundRect16`, a
 * superellipse inside libhwui's "Altair" branch. Capsule blends a cubic Bézier into a circular
 * arc instead. Both are genuinely curvature-continuous, but **they are not the same curve
 * family**, so the profile below is Capsule's own well-tested default rather than a
 * reproduction of ColorOS's exponent.
 *
 * Reaching pixel parity therefore still needs one calibration pass: render the same corner at
 * the same radius through both, sample the silhouette, and solve for
 * [G2ContinuityProfile.extendedFraction] / [G2ContinuityProfile.arcFraction] /
 * [G2ContinuityProfile.bezierCurvatureScale]. [colorosProfile] is the single place to change
 * once that is done - nothing else in this module hard-codes curve numbers.
 *
 * @param family The curvature family to use.
 * @param profile The G2 profile; defaults to [colorosProfile].
 * @param capsuleProfile The profile used for corners that are already pill-shaped.
 */
fun couiContinuity(
    family: CouiContinuity = CouiContinuity.G2,
    profile: G2ContinuityProfile = colorosProfile,
    capsuleProfile: G2ContinuityProfile = G2ContinuityProfile.Capsule,
): Continuity = when (family) {
    CouiContinuity.G0 -> G2Continuity(
        profile = G2ContinuityProfile.G1Equivalent,
        capsuleProfile = G2ContinuityProfile.Capsule,
    )

    CouiContinuity.G1 -> G2Continuity(
        profile = G2ContinuityProfile.G1Equivalent,
        capsuleProfile = G2ContinuityProfile.Capsule,
    )

    CouiContinuity.G2 -> G2Continuity(profile = profile, capsuleProfile = capsuleProfile)
}

/**
 * The G2 profile used for COUI corners.
 *
 * Currently Capsule's `RoundedRectangle` default, which is the result of fitting the curve so
 * that the Bézier and the arc end with matching curvature (`bezierCurvatureScale ==
 * arcCurvatureScale`, the condition Capsule documents for exact G2 continuity).
 *
 * See [couiContinuity] for why this is not yet a reproduction of ColorOS's own numbers.
 */
val colorosProfile: G2ContinuityProfile = G2ContinuityProfile.RoundedRectangle

/**
 * A COUI rounded rectangle: the drop-in replacement for `RoundedCornerShape`.
 *
 * Returns ColorOS's **actual** corner geometry, reproduced from
 * `COUIShapePath.getRoundRectPath` - see [CouiSquirclePath] for the maths and for why this is
 * a table-driven Bézier rather than a superellipse. Verified pixel-identical against the real
 * system on ColorOS 17.
 *
 * @param corner The corner radius, applied to all four corners.
 */
fun CouiShape(corner: Dp): Shape = CouiSquircleShape(corner)

/**
 * Per-corner variant of [CouiShape].
 *
 * ColorOS's own path builder only takes a single radius plus four on/off flags, so corners
 * cannot actually have *different* radii on the real system. Each non-zero corner is therefore
 * drawn at its own radius here; use [CouiShape] when you want an exact reproduction.
 *
 * Corner order matches `RoundedCornerShape`: `topStart`, `topEnd`, `bottomEnd`, `bottomStart`.
 */
fun CouiShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): Shape {
    val r = maxOf(topStart, topEnd, bottomEnd, bottomStart)
    return CouiSquircleShape(r)
}

/**
 * A COUI pill: every corner fully rounded.
 *
 * On ColorOS this is not a special case - a radius at or above half the shorter side is
 * `isFullyRoundedToCircle` and goes through the same table, so the pill is just
 * [CouiShape] with a large radius.
 */
fun CouiPillShape(): Shape = CouiPillSquircleShape()

// ---------------------------------------------------------------------------------------------
// Legacy: the Capsule-backed G2 shape.
//
// Kept because it is genuinely curvature-continuous and useful for comparison, but it is NOT
// what ColorOS draws - see [couiContinuity]. Prefer [CouiShape].
// ---------------------------------------------------------------------------------------------

/**
 * A G2-continuous rounded rectangle backed by `zone.ien.capsule:capsule`.
 *
 * @param corner The corner radius, applied to all four corners.
 * @param continuity The curvature family; G2 by default.
 */
fun CouiSmoothShape(
    corner: Dp,
    continuity: CouiContinuity = CouiContinuity.G2,
): CornerBasedShape = ContinuousRoundedRectangle(
    corner = CornerSize(corner),
    continuity = couiContinuity(continuity),
)

/**
 * Per-corner variant of [CouiSmoothShape].
 *
 * @param continuity The curvature family; G2 by default.
 */
fun CouiSmoothShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    continuity: CouiContinuity = CouiContinuity.G2,
): CornerBasedShape = ContinuousRoundedRectangle(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart),
    continuity = couiContinuity(continuity),
)

/**
 * A Capsule-backed pill.
 *
 * @param continuity The curvature family; G2 by default.
 */
fun CouiSmoothPillShape(continuity: CouiContinuity = CouiContinuity.G2): CornerBasedShape =
    ContinuousCapsule(continuity = couiContinuity(continuity))
