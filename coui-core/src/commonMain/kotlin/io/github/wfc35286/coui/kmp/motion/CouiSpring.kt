// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.sqrt


/**
 * A spring descriptor in the COUI motion language. Springs are not described with
 * stiffness/damping directly - the same `(bounce, response)` language as Material 3 is used, and
 * the conversion happens at the edges:
 *
 * ```
 * stiffness    = (2 * PI / response) ^ 2
 * dampingRatio = 1 - bounce
 * ```
 *
 * @param bounce How bouncy the spring is. **Negative values are legal** (the stiff preset uses
 *   `-0.5`). Clamped to [CouiSpringLimits.BounceMin]..[CouiSpringLimits.BounceMax].
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
    /** `(2 * PI / response) ^ 2`, clamped to the range [CouiSpringLimits] allows. */
    val stiffness: Float
        get() = responseToStiffness(response)

    /** `1 - bounce`, clamped to the range [CouiSpringLimits] allows. */
    val dampingRatio: Float
        get() = (1f - bounce).coerceIn(CouiSpringLimits.DampingRatioMin, CouiSpringLimits.DampingRatioMax)

    companion object {
        /** The default spring: bounce `0.0`, response `0.45`. */
        val Default: CouiSpringData = CouiSpringData(0.0f, 0.45f)

        /** The bouncy spring: bounce `0.5`, response `0.40`. */
        val Bouncy: CouiSpringData = CouiSpringData(0.5f, 0.40f)

        /** The stiff spring: bounce `-0.5`, response `0.30`. Note the negative bounce. */
        val Stiff: CouiSpringData = CouiSpringData(-0.5f, 0.30f)
    }
}

/**
 * The clamp ranges applied to a spring's raw parameters before it is handed to the physics engine.
 */
object CouiSpringLimits {
    /** The rounded `(2 * PI) ^ 2` used to convert response to stiffness. */
    const val StiffnessDivisor: Float = 39.47f

    /** The minimum stiffness. */
    const val StiffnessMin: Float = 100f

    /** The maximum stiffness. */
    const val StiffnessMax: Float = 10000f

    /** The minimum response, in seconds. */
    const val ResponseMin: Float = 0.1f

    /** The maximum response, in seconds. */
    const val ResponseMax: Float = 1.0f

    /** The minimum damping ratio. */
    const val DampingRatioMin: Float = 0.1f

    /**
     * The maximum damping ratio. Note this goes past 1, i.e. past critically damped.
     */
    const val DampingRatioMax: Float = 2.0f

    /** The minimum bounce. */
    const val BounceMin: Float = -1.0f

    /** The maximum bounce. */
    const val BounceMax: Float = 1.0f

    /** How a value threshold becomes a velocity threshold. */
    const val VelocityThresholdMultiplier: Float = 62.5f
}

/** Converts a spring response, in seconds, to stiffness, applying the clamp ranges. */
fun responseToStiffness(response: Float): Float {
    val clamped = response.coerceIn(CouiSpringLimits.ResponseMin, CouiSpringLimits.ResponseMax)
    val stiffness = (2f * PI.toFloat() / clamped) * (2f * PI.toFloat() / clamped)
    return stiffness.coerceIn(CouiSpringLimits.StiffnessMin, CouiSpringLimits.StiffnessMax)
}

/** Converts a stiffness back to a spring response, in seconds. */
fun stiffnessToResponse(stiffness: Float): Float =
    2f * PI.toFloat() / sqrt(stiffness.coerceAtLeast(1e-6f))

/**
 * Derives a velocity threshold from a value threshold using the fixed
 * [CouiSpringLimits.VelocityThresholdMultiplier]; Compose only takes the value threshold, so
 * this is exposed for callers that need to reproduce the velocity side.
 */
fun couiVelocityThreshold(valueThreshold: Float): Float =
    kotlin.math.abs(valueThreshold) * CouiSpringLimits.VelocityThresholdMultiplier

/**
 * Converts a COUI spring into a Compose [SpringSpec].
 *
 * @param visibilityThreshold The minimum visible change; see [CouiMinVisibleChange] for the
 *   values used per property.
 */
fun CouiSpringData.toSpringSpec(visibilityThreshold: Float = 0.001f): SpringSpec<Float> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold,
)