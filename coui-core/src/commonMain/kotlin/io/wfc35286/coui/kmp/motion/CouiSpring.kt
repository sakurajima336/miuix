// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * COUI's spring descriptor, a port of `com.coui.iola.SpringData`.
 *
 * COUI does not describe a spring with stiffness/damping directly - it uses the same
 * `(bounce, response)` language as Material 3, and converts at the edges. The conversion is
 * exactly the one Material uses, which is why a COUI spring and a Compose spring are the same
 * object once you translate:
 *
 * ```
 * stiffness    = (2 * PI / response) ^ 2
 * dampingRatio = 1 - bounce
 * ```
 *
 * @param bounce How bouncy the spring is. **Negative values are legal** - COUI ships
 *   `SpringData(-0.5f, 0.30f, 0.0f)` as its "stiff" preset. Clamped to
 *   [CouiSpringLimits.BounceMin]..[CouiSpringLimits.BounceMax].
 * @param response The period of one oscillation, in seconds. Clamped to
 *   [CouiSpringLimits.ResponseMin]..[CouiSpringLimits.ResponseMax].
 * @param velocity The initial velocity, in units per second. Defaults to 0.
 */
@Immutable
data class CouiSpringData(
    val bounce: Float,
    val response: Float,
    val velocity: Float = 0f,
) {
    /** `(2 * PI / response) ^ 2`, clamped to the range `COUISpringForce` allows. */
    val stiffness: Float
        get() = responseToStiffness(response)

    /** `1 - bounce`, clamped to the range `COUISpringForce` allows. */
    val dampingRatio: Float
        get() = (1f - bounce).coerceIn(CouiSpringLimits.DampingRatioMin, CouiSpringLimits.DampingRatioMax)

    companion object {
        /** `SpringData.DEFAULT` - `(0.0, 0.45)`. */
        val Default: CouiSpringData = CouiSpringData(0.0f, 0.45f)

        /** `SpringData.BOUNCY` - `(0.5, 0.40)`. */
        val Bouncy: CouiSpringData = CouiSpringData(0.5f, 0.40f)

        /** `SpringData.STIFF` - `(-0.5, 0.30)`. Note the negative bounce. */
        val Stiff: CouiSpringData = CouiSpringData(-0.5f, 0.30f)
    }
}

/**
 * The clamps `COUISpringForce` and `SpringData` apply before handing a spring to the physics
 * engine.
 */
object CouiSpringLimits {
    /** `SPRING_STIFFNESS_DIVISOR` - the rounded `(2 * PI) ^ 2`. */
    const val StiffnessDivisor: Float = 39.47f

    /** `SPRING_STIFFNESS_MIN`. */
    const val StiffnessMin: Float = 100f

    /** `SPRING_STIFFNESS_MAX`. */
    const val StiffnessMax: Float = 10000f

    /** `SPRING_RESPONSE_CLAMP_MIN`. */
    const val ResponseMin: Float = 0.1f

    /** `SPRING_RESPONSE_CLAMP_MAX`. */
    const val ResponseMax: Float = 1.0f

    /** `DAMPING_RATIO_MIN`. */
    const val DampingRatioMin: Float = 0.1f

    /** `DAMPING_RATIO_MAX`. Note this goes past 1, i.e. past critically damped. */
    const val DampingRatioMax: Float = 2.0f

    /** `DAMPING_BOUNCE_CLAMP_MIN`. */
    const val BounceMin: Float = -1.0f

    /** `DAMPING_BOUNCE_CLAMP_MAX`. */
    const val BounceMax: Float = 1.0f

    /** `VELOCITY_THRESHOLD_MULTIPLIER` - how a value threshold becomes a velocity threshold. */
    const val VelocityThresholdMultiplier: Float = 62.5f
}

/** `COUISpringForce.responseToStiffness()`. */
fun responseToStiffness(response: Float): Float {
    val clamped = response.coerceIn(CouiSpringLimits.ResponseMin, CouiSpringLimits.ResponseMax)
    val stiffness = (2f * PI.toFloat() / clamped) * (2f * PI.toFloat() / clamped)
    return stiffness.coerceIn(CouiSpringLimits.StiffnessMin, CouiSpringLimits.StiffnessMax)
}

/** `COUISpringForce.stiffnessToResponse()`. */
fun stiffnessToResponse(stiffness: Float): Float =
    2f * PI.toFloat() / sqrt(stiffness.coerceAtLeast(1e-6f))

/**
 * `COUISpringForce.setValueThreshold()`.
 *
 * COUI derives the velocity threshold from the value threshold with a fixed multiplier of
 * [CouiSpringLimits.VelocityThresholdMultiplier]; Compose only takes the value threshold, so
 * this is exposed for callers that need to reproduce the velocity side.
 */
fun couiVelocityThreshold(valueThreshold: Float): Float =
    kotlin.math.abs(valueThreshold) * CouiSpringLimits.VelocityThresholdMultiplier

/**
 * Converts a COUI spring into a Compose [SpringSpec].
 *
 * @param visibilityThreshold The minimum visible change; see [CouiMinVisibleChange] for the
 *   values COUI uses per property.
 */
fun CouiSpringData.toSpringSpec(visibilityThreshold: Float = 0.001f): SpringSpec<Float> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold,
)