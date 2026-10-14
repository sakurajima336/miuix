// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

/**
 * The minimum change COUI considers visible, per animated property.
 *
 * These decide when a spring is allowed to stop. Compose calls the same idea a
 * `visibilityThreshold`, so these values plug straight into
 * [CouiSpringData.toSpringSpec].
 *
 * From `COUIDynamicAnimation` and `COUIProperty`.
 */
object CouiMinVisibleChange {
    /** `MIN_VISIBLE_CHANGE_ALPHA` - exactly 1/256. */
    const val Alpha: Float = 0.00390625f

    /** `MIN_VISIBLE_CHANGE_PIXELS`. */
    const val Pixels: Float = 1.0f

    /** `MIN_VISIBLE_CHANGE_ROTATION_DEGREES`. */
    const val RotationDegrees: Float = 0.1f

    /** `MIN_VISIBLE_CHANGE_SCALE`. */
    const val Scale: Float = 0.002f

    /** `COUIDynamicAnimation.THRESHOLD_MULTIPLIER`. */
    const val ThresholdMultiplier: Float = 0.75f

    /** `COUIProperty.BLUR_MIN_VISIBLE_CHANGE`. */
    const val Blur: Float = 1.0f

    /** `COUIProperty.COLOR_MIN_VISIBLE_CHANGE`. */
    const val Color: Float = 1.0f

    /** `COUIProperty.DEFAULT_MIN_VISIBLE_CHANGE`. */
    const val Default: Float = 0.001f

    /** `COUIProperty.HEIGHT_MIN_VISIBLE_CHANGE` / `WIDTH_MIN_VISIBLE_CHANGE`. */
    const val HeightWidth: Float = 0.5f

    /** `COUIProperty.POSITION_MIN_VISIBLE_CHANGE` / `TRANSLATION_MIN_VISIBLE_CHANGE`. */
    const val Position: Float = 0.5f

    /** `COUIProperty.RADIUS_MIN_VISIBLE_CHANGE`. */
    const val Radius: Float = 0.001f

    /** `COUIProperty.ROTATION_MIN_VISIBLE_CHANGE` / `SCALE_MIN_VISIBLE_CHANGE`. */
    const val RotationScale: Float = 3.2552084E-4f

    /** `COUIProperty.SCROLL_MIN_VISIBLE_CHANGE`. */
    const val Scroll: Float = 1.0f
}

/** Frame and duration constants, from `COUIAnimationHandler` and friends. */
object CouiMotionTiming {
    /** `COUIAnimationHandler.FRAME_DELAY_MS` - COUI ticks its spring solver at 100 Hz. */
    const val FrameDelayMs: Int = 10

    /** `COUISpringAnimation.LOGIC_END_EXTRA_TIME_MS`. */
    const val LogicEndExtraTimeMs: Int = 50

    /** `MILLISECONDS_PER_SECOND`. */
    const val MillisPerSecond: Int = 1000

    /** `COUIResponsiveSpringMotion.DEFAULT_BLEND_DURATION`. */
    const val ResponsiveSpringBlendDurationMs: Int = 250

    /** `COUIResponsiveSpringMotion.MIN_VISIBLE_CHANGE_DIVISOR`. */
    const val ResponsiveSpringMinVisibleChangeDivisor: Float = 10000f
}

/**
 * The six press springs, a port of `com.coui.appcompat.animation.touchanimation.PressParams`.
 *
 * Each press style has a distinct spring for the press and for the release, which is why a COUI
 * button "gives" differently than it recovers.
 */
object CouiPressSprings {
    /** `jellyPressSpring` - `(0.0, 0.25)`. */
    val JellyPress: CouiSpringData = CouiSpringData(0.0f, 0.25f)

    /** `jellyHandupSpring` - `(0.6, 0.5)`. */
    val JellyHandUp: CouiSpringData = CouiSpringData(0.6f, 0.5f)

    /** `candyPressSpring` - `(0.0, 0.3)`. */
    val CandyPress: CouiSpringData = CouiSpringData(0.0f, 0.3f)

    /** `candyHandUpSpring` - `(0.5, 0.5)`. */
    val CandyHandUp: CouiSpringData = CouiSpringData(0.5f, 0.5f)

    /** `enlargePressSpring` - `(0.0, 0.2)`. */
    val EnlargePress: CouiSpringData = CouiSpringData(0.0f, 0.2f)

    /** `enlargeHandUpSpring` - `(0.7, 0.45)`. */
    val EnlargeHandUp: CouiSpringData = CouiSpringData(0.7f, 0.45f)
}

/**
 * The scalar half of `PressParams` - the ranges the press-scale formula maps into.
 */
object CouiPressScaleRange {
    /** `enlargeScaleRangeMin`. */
    const val EnlargeMin: Float = 1.17f

    /** `enlargeScaleRangeMax`. */
    const val EnlargeMax: Float = 1.2f

    /** `aspectActivationThreshold` - the aspect ratio at which the second channel wakes up. */
    const val AspectActivationThreshold: Float = 2.0f

    /** `aspectExtremeRatio`. */
    const val AspectExtremeRatio: Float = 15.0f

    /** `aspectBaseScale`. */
    const val AspectBaseScale: Float = 0.85f

    /** `aspectExtremeScale`. */
    const val AspectExtremeScale: Float = 1.0f

    /** `minAreaWidthDp` - the area channel's lower bound, in dp. */
    const val MinAreaWidthDp: Float = 20f

    /** `maxAreaWidthDp` - the area channel's upper bound, in dp. */
    const val MaxAreaWidthDp: Float = 150f

    /** `scaleRangeMin`. */
    const val ScaleRangeMin: Float = 0.85f

    /** `scaleRangeMax`. */
    const val ScaleRangeMax: Float = 0.97f

    /** `TARGET_SCALE_MIN` - the hard floor `PressPhysicsEngine` clamps to. */
    const val TargetScaleMin: Float = 0.4f

    /** `TARGET_SCALE_MAX_SHRINK`. */
    const val TargetScaleMaxShrink: Float = 1.0f

    /** `IMPACT_VELOCITY_DISTANCE_SCALE`. */
    const val ImpactVelocityDistanceScale: Float = 80.0f
}

/**
 * `CommonParams` - the touch parameters every COUI control shares.
 */
object CouiTouchCommon {
    /** `iolDeadZone`, in dp: movement below this never starts a drag. */
    const val DeadZoneDp: Float = 5.0f

    /** `iolPivotX` / `iolPivotY`. `-1` means "use the centre". */
    const val PivotAuto: Float = -1.0f

    /** `iolMaxScale`. */
    const val MaxScale: Float = 1.2f

    /** `iolK1`, divided by [K1Divisor] before use. */
    const val K1: Float = 350000f

    /** `iolK2`, divided by [K2Divisor] before use. */
    const val K2: Float = 15f

    /** `TouchAnimatorParamProvider.K1_FACTOR_DIVISOR`. */
    const val K1Divisor: Float = 1e6f

    /** `TouchAnimatorParamProvider.K2_FACTOR_DIVISOR`. */
    const val K2Divisor: Float = 1e3f

    /** `iolPressMinimumVisibleChange`. */
    const val PressMinVisibleChange: Float = 0.001f

    /** `iolPressHandUpMinimumVisibleChange`. */
    const val PressHandUpMinVisibleChange: Float = 1.0E-4f

    /** `iolDeformMinimumVisibleChange`. */
    const val DeformMinVisibleChange: Float = 5.0E-4f

    /** `iolRubberMinimumVisibleChange`. */
    const val RubberMinVisibleChange: Float = 0.5f
}

/**
 * `TouchMotionTierParams` - the rubber-band and hand-up parameters for the layered (tiered)
 * touch motion.
 */
object CouiTouchTiers {
    /** `iolMaxStretchDistance`. */
    const val MaxStretchDistance: Float = 28.0f

    /** `iolRubberCurveRatio`. */
    const val RubberCurveRatio: Float = 0.05f

    /** `iolMaxDeformRubberDistance`. */
    const val MaxDeformRubberDistance: Float = 150.0f

    /** `iolDeformRubberCurveRatio`. */
    const val DeformRubberCurveRatio: Float = 0.55f

    /** `iolDragSpring` - `(0.15, 0.15)`. */
    val DragSpring: CouiSpringData = CouiSpringData(0.15f, 0.15f)

    /** `iolHandUpSpring` - `(0.65, 0.42)`. */
    val HandUpSpring: CouiSpringData = CouiSpringData(0.65f, 0.42f)

    /** `iolHandUpSpringLargeArea` - `(0.65, 0.45)`. */
    val HandUpSpringLargeArea: CouiSpringData = CouiSpringData(0.65f, 0.45f)

    /** `iolHandUpMaxInitialVelocity`. */
    const val HandUpMaxInitialVelocity: Float = 1000.0f

    /** `iolHandUpSmallAreaRefWidthDp`. */
    const val HandUpSmallAreaRefWidthDp: Float = 56f

    /** `iolHandUpLargeAreaRefWidthDp`. */
    const val HandUpLargeAreaRefWidthDp: Float = 100f
}

/**
 * `PressEngineConfig` - the defaults the press physics engine starts from.
 */
object CouiPressEngineDefaults {
    /** `DEFAULT_RESPONSE_PRESS`. */
    const val ResponsePress: Float = 0.13f

    /** `DEFAULT_BOUNCE_PRESS`. */
    const val BouncePress: Float = 0.18f

    /** `DEFAULT_RESPONSE_RELEASE`. */
    const val ResponseRelease: Float = 0.35f

    /** `DEFAULT_BOUNCE_RELEASE`. */
    const val BounceRelease: Float = 0.62f

    /** `DEFAULT_MIN_SCALE`. */
    const val MinScale: Float = 0.88f

    /** `DEFAULT_MAX_SCALE`. */
    const val MaxScale: Float = 0.97f

    /** `DEFAULT_INTENSITY`. */
    const val Intensity: Float = 1.0f

    /** `DEFAULT_IMPACT_FACTOR`. */
    const val ImpactFactor: Float = 0.0f
}

/**
 * Per-control spring tables. These come from the `*Params` classes that COUI's designers tune
 * through OPPO's internal `olivelink` platform.
 */
object CouiComponentSprings {
    /**
     * `COUISwitchParams.toggleSpring` - `(0.3, 0.4)`.
     *
     * The switch uses this for the thumb translation, with a minimum visible change of 0.1.
     */
    val SwitchToggle: CouiSpringData = CouiSpringData(0.3f, 0.4f)

    /** `COUISwitchParams.responseDrag` / `bounceDrag`. */
    const val SwitchResponseDrag: Float = 0.3f

    /** `COUISwitchParams.bounceDrag`. */
    const val SwitchBounceDrag: Float = 0.0f

    /** `COUISwitchParams.responseAttach`. */
    const val SwitchResponseAttach: Float = 0.4f

    /** `COUISwitchParams.bounceAttach`. */
    const val SwitchBounceAttach: Float = 0.2f

    /** `COUISwitch.animateWhenStateChanged` uses this as the thumb's visibility threshold. */
    const val SwitchToggleMinVisibleChange: Float = 0.1f

    /**
     * `COUISwitchParams.bounceDrag` / `responseDrag` - the spring the thumb follows the finger
     * with while dragging.
     *
     * `COUISwitch` does not bind the thumb to the finger: `COUISpringDragHelper` runs a
     * `COUISpringAnimation` per axis with this spring, so the thumb **lags** behind the finger.
     * That lag is the "resistance" you feel; there is no rubber band (`mMaxOverDistance` is 0).
     */
    val SwitchDrag: CouiSpringData = CouiSpringData(bounce = 0.0f, response = 0.3f)

    /**
     * `COUISwitchParams.bounceAttach` / `responseAttach` - the spring used to snap to the nearest
     * anchor once the finger lifts. Bouncier than [SwitchDrag], which is why the release
     * overshoots.
     */
    val SwitchAttach: CouiSpringData = CouiSpringData(bounce = 0.2f, response = 0.4f)

    /**
     * `COUISwitchParams.transformDistance` - the dead zone, in raw touch pixels.
     *
     * `COUISpringDragHelper.dragTo` refuses to move until the finger has travelled this far from
     * the down point, so a tap that wobbles by a pixel or two does not start a drag.
     */
    const val SwitchTransformDistance: Float = 4.0f
}