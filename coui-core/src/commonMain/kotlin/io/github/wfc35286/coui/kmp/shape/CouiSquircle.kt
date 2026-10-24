// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.shape
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
 * and `G2` is curvature-continuous - the "continuous corner" look. [G2] is the default.
 */
enum class CouiContinuity {
    /** Sharp corners. */
    G0,

    /** A quarter-circle arc, no corner smoothing. */
    G1,

    /** Curvature-continuous. The COUI default. */
    G2,
}

/**
 * Builds the [Continuity] implementation for a [CouiContinuity] family.
 *
 * Backed by the Apache-2.0 `zone.ien.capsule:capsule` library, which generates G2-continuous
 * rounded rectangles from pure Bézier/arc geometry.
 *
 * The G2 silhouette is Capsule's own well-tested curve family, not a reproduction of the
 * ColorOS superellipse. [colorosProfile] is the single place to change when the profile is
 * tuned - nothing else in this module hard-codes curve numbers.
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
 * Capsule's `RoundedRectangle` default, which fits the curve so that the Bézier and the arc
 * end with matching curvature (`bezierCurvatureScale == arcCurvatureScale`, the condition
 * Capsule documents for exact G2 continuity).
 */
val colorosProfile: G2ContinuityProfile = G2ContinuityProfile.RoundedRectangle

/**
 * A COUI rounded rectangle: the drop-in replacement for `RoundedCornerShape`.
 *
 * Draws the COUI corner geometry - a table-driven cubic Bézier silhouette; see
 * [CouiSquirclePath] for the maths.
 *
 * @param corner The corner radius, applied to all four corners.
 */
fun CouiShape(corner: Dp): Shape = CouiSquircleShape(corner)

/**
 * Per-corner variant of [CouiShape].
 *
 * The underlying path builder supports only a single radius, so the largest non-zero radius
 * wins and is applied to all four corners; use [CouiShape] for an exact single-radius shape.
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
 * A radius at or above half the shorter side is treated as fully rounded, so the pill is just
 * [CouiShape] with a large radius.
 */
fun CouiPillShape(): Shape = CouiPillSquircleShape()

// ---------------------------------------------------------------------------------------------
// Legacy: the Capsule-backed G2 shape.
//
// Genuinely curvature-continuous, but a different curve family from [CouiShape] - see
// [couiContinuity]. Prefer [CouiShape].
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
