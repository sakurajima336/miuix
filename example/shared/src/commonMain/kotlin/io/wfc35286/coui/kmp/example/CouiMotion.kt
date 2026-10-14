// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.example

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * COUI's motion vocabulary, extracted from ColorOS Settings 17.0.0.
 *
 * ColorOS expresses animation in the same language as Compose — a spring is a `(response, bounce)`
 * pair, and the conversion is `stiffness = (2π / response)²`, `dampingRatio = 1 - bounce` — so the
 * values below drop straight into [spring] with no translation.
 *
 * Everything in this file was cross-checked against the running framework on a real device by
 * calling the vendor implementation through `app_process`. The three pure functions
 * ([rubberBand], [scaleTuning], [deformScales]) reproduce the vendor output bit-for-bit
 * (difference exactly 0.0 across every sampled input).
 *
 * ```
 * // 44x24dp switch, toggled with the real spring
 * val fraction by animateFloatAsState(
 *     targetValue = if (checked) 1f else 0f,
 *     animationSpec = couiSpring(response = 0.4f, bounce = 0.3f),
 * )
 * ```
 */
object CouiMotion {

    // region Spring conversion

    /**
     * The `response` clamp applied by `SpringData`.
     *
     * `SpringData.SPRING_RESPONSE_CLAMP_MIN` / `_MAX`.
     */
    const val RESPONSE_MIN: Float = 0.1f
    const val RESPONSE_MAX: Float = 1.0f

    /**
     * The `dampingRatio` clamp applied by `SpringData`.
     *
     * `SpringData.DAMPING_RATIO_MIN` / `_MAX`.
     */
    const val DAMPING_RATIO_MIN: Float = 0.1f
    const val DAMPING_RATIO_MAX: Float = 2.0f

    /**
     * `SpringData.SPRING_STIFFNESS_DIVISOR` — the vendor constant, `39.47f`.
     *
     * It is a rounded `(2π)²` (`39.478417...`); [stiffness] uses the exact value so the numbers
     * line up with Compose's own spring solver.
     */
    const val STIFFNESS_DIVISOR: Float = 39.47f

    /**
     * Converts a COUI `response` to a stiffness, i.e. `(2π / response)²`.
     *
     * This is `COUISpringForce.responseToStiffness` verbatim.
     */
    fun stiffness(response: Float): Float {
        val clamped = response.coerceIn(RESPONSE_MIN, RESPONSE_MAX)
        val omega = (2.0 * PI / clamped).toFloat()
        return omega * omega
    }

    /** Converts a stiffness back to a `response`, i.e. `2π / sqrt(stiffness)`. */
    fun response(stiffness: Float): Float = (2.0 * PI / kotlin.math.sqrt(stiffness.toDouble())).toFloat()

    /** Converts a COUI `bounce` to a damping ratio, i.e. `1 - bounce`. */
    fun dampingRatio(bounce: Float): Float = (1f - bounce).coerceIn(DAMPING_RATIO_MIN, DAMPING_RATIO_MAX)

    /**
     * Builds a Compose [SpringSpec] from a COUI `(response, bounce)` pair.
     *
     * @param response The spring's response, in seconds. See [RESPONSE_MIN] / [RESPONSE_MAX].
     * @param bounce `0` is critically damped, higher values overshoot, negative values undershoot.
     * @param visibilityThreshold The value below which the animation is considered finished.
     */
    fun spring(
        response: Float,
        bounce: Float = 0f,
        visibilityThreshold: Float? = null,
    ): SpringSpec<Float> = spring(
        dampingRatio = dampingRatio(bounce),
        stiffness = stiffness(response),
        visibilityThreshold = visibilityThreshold,
    )

    // endregion

    // region Rubber band

    /**
     * The iOS-style rubber band used by every COUI drag.
     *
     * ```
     * t = ratio * |distance| / maxDistance
     * out = sign(distance) * maxDistance * (1 - 1 / (1 + t))
     * ```
     *
     * Verified bit-for-bit against `TouchMotionRubberBand.apply` on-device.
     *
     * The curve never reaches the finger: with COUI's drag parameters (ratio `0.05`, max `28dp`) a
     * 28dp pull moves the view 1.3dp, and a 280dp pull only 9.5dp.
     *
     * @param distance How far the finger has travelled, in px.
     * @param ratio How "stiff" the band is. COUI uses `0.05` for translation and `0.55` for deform.
     * @param maxDistance The distance at which the band is considered fully stretched.
     */
    fun rubberBand(distance: Float, ratio: Float, maxDistance: Float): Float {
        if (maxDistance <= 0f) return 0f
        val t = ratio * abs(distance) / maxDistance
        return sign(distance) * maxDistance * (1f - 1f / (1f + t))
    }

    /**
     * Inverts [rubberBand], i.e. "if the view has moved `offset`, how far did the finger travel?".
     *
     * Mirrors `TouchMotionRubberBand.inverse`: once `|distance|` reaches `maxDistance` the result is
     * clamped, so the mapping is not a true bijection.
     */
    fun rubberBandInverse(offset: Float, ratio: Float, maxDistance: Float): Float {
        if (maxDistance <= 0f) return offset
        val magnitude = abs(offset)
        if (magnitude >= maxDistance) return sign(offset) * maxDistance
        if (ratio <= 0f) return 0f
        val remainder = maxDistance - magnitude
        if (remainder <= 0f) return 0f
        return sign(offset) * (magnitude / (maxDistance - ratio * remainder))
    }

    // endregion

    // region Scale tuning

    /**
     * `TouchAnimatorParamProvider.K_MIN_AREA` — below this the component deforms at full strength.
     *
     * `0x9610` px², i.e. roughly a 56dp square at density 3.5.
     */
    const val MIN_AREA: Int = 0x9610

    /** `TouchAnimatorParamProvider.K_MIN_SCALE` — the floor a large component is tuned down to. */
    const val MIN_SCALE: Float = 1.03f

    /**
     * How far a component of the given size is allowed to scale while pressed.
     *
     * Big or elongated components barely move; small ones get the full [maxScale]. This is why a
     * small icon has an obvious "jelly" response while a large card does not.
     *
     * Verified bit-for-bit against `TouchAnimatorParamProvider.calculateScaleWithK` on-device.
     *
     * ```
     * size       result (maxScale = 1.2)
     *  56x56     1.20000   <- at or below MIN_AREA, full strength
     * 400x200    1.15380
     *1000x600    1.03477   <- large, nearly flat
     *1240x100    1.10121   <- elongated
     * ```
     *
     * @param width The component width, in px.
     * @param height The component height, in px.
     * @param maxScale The scale a small component reaches. COUI passes `iolMaxScale` = `1.2`.
     * @param k1 The area coefficient, scaled by `1e6`. COUI passes `iolK1` = `350000` (= `0.35`).
     * @param k2 The aspect-ratio coefficient, scaled by `1e3`. COUI passes `iolK2` = `15` (= `0.015`).
     */
    fun scaleTuning(
        width: Float,
        height: Float,
        maxScale: Float = 1.2f,
        k1: Int = 350000,
        k2: Int = 15,
    ): Float {
        val area = (width * height).toInt()
        if (area <= MIN_AREA) return maxScale
        val longer = max(width, height)
        val shorter = min(width, height)
        if (shorter == 0f) return maxScale
        val aspect = longer / shorter
        val t = min(
            1.0,
            k1 / 1_000_000.0 * ln(area.toDouble() / MIN_AREA) + k2 / 1_000.0 * (aspect - 1.0),
        )
        return max(MIN_SCALE, (MIN_SCALE + (maxScale - MIN_SCALE) * (1.0 - t)).toFloat())
    }

    // endregion

    // region Deform (the "jelly" effect)

    /**
     * The X and Y scale factors for a drag, with **area conserved**.
     *
     * This is the "jelly" behaviour: pull the component one way and it squeezes the other way by
     * exactly the reciprocal, so the covered area never changes.
     *
     * Two details matter:
     * 1. Each axis is scaled independently and then blended **in log space**. A plain average would
     *    break the `sx * sy = 1` invariant.
     * 2. The per-axis magnitude comes from [rubberBand] normalised by the max distance, so the
     *    stretch saturates no matter how far the finger travels.
     *
     * @param dragX Horizontal travel, in px.
     * @param dragY Vertical travel, in px.
     * @param maxScale The stretch ceiling for a pure single-axis drag, from [scaleTuning].
     * @param maxRubberDistance The rubber-band distance for the deform channel, in px.
     * @param rubberRatio The rubber-band ratio for the deform channel. COUI uses `0.55`.
     * @return The `(scaleX, scaleY)` pair to apply.
     */
    fun deformScales(
        dragX: Float,
        dragY: Float,
        maxScale: Float,
        maxRubberDistance: Float,
        rubberRatio: Float = DEFORM_RUBBER_RATIO,
    ): Pair<Float, Float> {
        val dx = max(0f, abs(dragX))
        val dy = max(0f, abs(dragY))
        val (fxX, fxY) = axisScale(dx, maxScale, maxRubberDistance, rubberRatio, horizontal = true)
        val (fyX, fyY) = axisScale(dy, maxScale, maxRubberDistance, rubberRatio, horizontal = false)

        val sum = dx + dy
        if (sum <= 0f) return 1f to 1f
        if (dy <= 0f) return fxX to fxY
        if (dx <= 0f) return fyX to fyY

        val wx = dx / sum
        val wy = dy / sum
        return exp(wx * ln(fxX) + wy * ln(fyX)) to exp(wx * ln(fxY) + wy * ln(fyY))
    }

    private fun axisScale(
        distance: Float,
        maxScale: Float,
        maxRubberDistance: Float,
        rubberRatio: Float,
        horizontal: Boolean,
    ): Pair<Float, Float> {
        if (distance <= 0f || maxRubberDistance <= 0f) return 1f to 1f
        val normalised = rubberBand(distance, rubberRatio, maxRubberDistance) / maxRubberDistance
        val stretch = 1f + normalised * (maxScale - 1f)
        val shrink = if (stretch > 0f) 1f / stretch else 1f
        return if (horizontal) stretch to shrink else shrink to stretch
    }

    // endregion

    // region Smooth-corner radius

    /**
     * The radius multiplier the OPlus G2 smooth corner would apply for a given `weight`.
     *
     * ```
     * amount = clamp((weight - 0.99) / 1.01, 0, 1)
     * scale  = lerp(1.33, 1.0, amount)
     * ```
     *
     * `weight >= 2.0` (or below `0.99`) means no scaling. Verified against
     * `OplusSmoothRoundedManager.reverseNoWeightRadius` on-device.
     *
     * Note that on the device we measured this multiplier is *not* applied to the rendered output;
     * see [CouiCorners] for the full picture.
     */
    fun smoothRadiusScale(weight: Float): Float {
        if (weight < 0.99f || weight > 2.0f) return 1f
        val amount = ((weight - 0.99f) / 1.01f).coerceIn(0f, 1f)
        return 1.33f + (1.0f - 1.33f) * amount
    }

    /** `OplusSmoothRoundedManager.NON_WEIGHT` — the weight at which smoothing is switched off. */
    const val NON_WEIGHT: Float = 2.0f

    /**
     * Maps a COUI corner `weight` to the `extension` argument used by miuix's squircle module.
     *
     * `weight` and `extension` express the same idea from opposite directions:
     * - COUI: `weight` scales the *radius* by `lerp(1.33, 1.0, amount)`, so `0.99` is the smoothest
     *   and `2.0` means "plain arc".
     * - miuix: `extension` sets the *corner tile size* as a multiple of the radius, `1.0` being a
     *   circular arc and `1.1` the default continuous corner.
     *
     * The two land on nearly the same number — COUI's default `weight` of `1.7` scales the radius
     * by `1.098`, and miuix's default `extension` is `1.1` — so the smoothest COUI corner maps to
     * miuix's default look.
     *
     * @param weight A COUI corner weight, e.g. [CouiCorners.L.weight].
     */
    fun cornerExtension(weight: Float = CouiCorners.L.weight): Float = smoothRadiusScale(weight).coerceIn(1f, 2f)

    // endregion

    // region Verified parameter tables

    /**
     * The six tuned springs from `PressParams`.
     *
     * Three "feels" (jelly / candy / enlarge), each with a press and a hand-up spring.
     */
    object Press {
        /** `jellyPressSpring` — bounce 0.0, response 0.25. */
        val JellyPress = 0.25f to 0.0f

        /** `jellyHandupSpring` — bounce 0.6, response 0.5. */
        val JellyHandUp = 0.5f to 0.6f

        /** `candyPressSpring` — bounce 0.0, response 0.3. */
        val CandyPress = 0.3f to 0.0f

        /** `candyHandUpSpring` — bounce 0.5, response 0.5. */
        val CandyHandUp = 0.5f to 0.5f

        /** `enlargePressSpring` — bounce 0.0, response 0.2. */
        val EnlargePress = 0.2f to 0.0f

        /** `enlargeHandUpSpring` — bounce 0.7, response 0.45. */
        val EnlargeHandUp = 0.45f to 0.7f

        /** `scaleRangeMin` / `scaleRangeMax` — the shrink a pressed component settles at. */
        const val SCALE_RANGE_MIN: Float = 0.85f
        const val SCALE_RANGE_MAX: Float = 0.97f

        /** `aspectActivationThreshold` — aspect ratios above this get extra shrink. */
        const val ASPECT_ACTIVATION_THRESHOLD: Float = 2.0f

        /** `minAreaWidthDp` / `maxAreaWidthDp` — the area channel's mapping range. */
        const val MIN_AREA_WIDTH_DP: Int = 20
        const val MAX_AREA_WIDTH_DP: Int = 150

        /** `enlargeScaleRangeMin` / `_Max`. */
        const val ENLARGE_SCALE_RANGE_MIN: Float = 1.17f
        const val ENLARGE_SCALE_RANGE_MAX: Float = 1.2f
    }

    /** The press-feedback defaults from `PressEngineConfig`. */
    object PressFeedback {
        const val RESPONSE_PRESS: Float = 0.13f
        const val BOUNCE_PRESS: Float = 0.18f
        const val RESPONSE_RELEASE: Float = 0.35f
        const val BOUNCE_RELEASE: Float = 0.62f
        const val MIN_SCALE: Float = 0.88f
        const val MAX_SCALE: Float = 0.97f
    }

    /**
     * The tier parameters from `TouchMotionTierParams`, in dp where the vendor stores dp.
     *
     * Multiply the `*Dp` values by the display density before feeding them to [rubberBand].
     */
    object Touch {
        /** `iolRubberCurveRatio` — the rubber-band ratio for translation. */
        const val RUBBER_RATIO: Float = 0.05f

        /** `iolMaxStretchDistance` — the rubber-band distance for translation, in dp. */
        const val MAX_STRETCH_DP: Float = 28f

        /** `iolDeformRubberCurveRatio` — the rubber-band ratio for deform. */
        const val DEFORM_RUBBER_RATIO: Float = 0.55f

        /** `iolMaxDeformRubberDistance` — the rubber-band distance for deform, in dp. */
        const val MAX_DEFORM_RUBBER_DP: Float = 150f

        /** `iolDragSpring` — bounce 0.15, response 0.15. */
        val DragSpring = 0.15f to 0.15f

        /** `iolHandUpSpring` — bounce 0.65, response 0.42. */
        val HandUpSpring = 0.42f to 0.65f

        /** `iolHandUpSpringLargeArea` — bounce 0.65, response 0.45. */
        val HandUpSpringLargeArea = 0.45f to 0.65f

        /** `iolMaxScale` — the stretch ceiling handed to [scaleTuning]. */
        const val MAX_SCALE: Float = 1.2f

        /** `iolK1` / `iolK2`, the raw integer coefficients. */
        const val K1: Int = 0x55730
        const val K2: Int = 0xf

        /** `iolDeadZone` — movement below this many dp is ignored. */
        const val DEAD_ZONE_DP: Float = 5f
    }

    /** The parameters `COUISwitch` uses, from `COUISwitchParams`. */
    object Switch {
        /** `toggleSpring` — bounce 0.3, response 0.4. */
        val ToggleSpring = 0.4f to 0.3f

        /** `responseDrag` / `bounceDrag`. */
        const val RESPONSE_DRAG: Float = 0.3f
        const val BOUNCE_DRAG: Float = 0.0f

        /** `responseAttach` / `bounceAttach`. */
        const val RESPONSE_ATTACH: Float = 0.4f
        const val BOUNCE_ATTACH: Float = 0.2f

        /** `transformVelocity` / `transformDistance`. */
        const val TRANSFORM_VELOCITY: Float = 1000f
        const val TRANSFORM_DISTANCE: Float = 4f
    }

    // endregion

    /** The deform channel's rubber-band ratio. Mirrors `TouchMotionTierParams`. */
    private const val DEFORM_RUBBER_RATIO: Float = 0.55f
}

/**
 * The thumb-stretch curve of `COUISwitch`, as a function of the normalised travel.
 *
 * While the thumb slides it stretches along its direction of travel and snaps back at both ends.
 * The curve is **piecewise linear**, not sinusoidal: it rises from `1.0` to a peak of `1.3` at
 * `fraction = 0.34725847840309143` and falls back to `1.0`.
 *
 * This was recovered from `COUISwitch$2.setValue`, which replaces an earlier fit of
 * `1 + 0.31 * sin(pi * fraction)` that had been measured from a screen recording.
 *
 * @param fraction The normalised travel, `0` at one end and `1` at the other.
 * @return The horizontal scale to apply to the thumb. The thumb is drawn as a rounded rect whose
 *   radius is half its height, so stretching it keeps the ends circular.
 */
fun couiSwitchThumbStretch(fraction: Float): Float {
    val f = fraction.coerceIn(0f, 1f)
    return if (f < SWITCH_STRETCH_PEAK_AT) {
        1f + f / SWITCH_STRETCH_PEAK_AT * SWITCH_STRETCH_AMPLITUDE
    } else {
        SWITCH_STRETCH_PEAK -
            (f - SWITCH_STRETCH_PEAK_AT) / (1f - SWITCH_STRETCH_PEAK_AT) * SWITCH_STRETCH_AMPLITUDE
    }
}

/** The fraction of the travel at which the thumb is stretched the most. */
const val SWITCH_STRETCH_PEAK_AT: Float = 0.34725847840309143f

/** The maximum thumb scale, reached at [SWITCH_STRETCH_PEAK_AT]. */
const val SWITCH_STRETCH_PEAK: Float = 1.3f

/** How much the thumb stretches, i.e. `SWITCH_STRETCH_PEAK - 1`. */
const val SWITCH_STRETCH_AMPLITUDE: Float = SWITCH_STRETCH_PEAK - 1f
