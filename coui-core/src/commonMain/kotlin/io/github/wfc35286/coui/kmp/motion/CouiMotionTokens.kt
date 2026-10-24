// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion

/**
 * The minimum change considered visible, per animated property.
 *
 * These decide when a spring is allowed to stop. Compose calls the same idea a
 * `visibilityThreshold`, so these values plug straight into
 * [CouiSpringData.toSpringSpec].
 */
object CouiMinVisibleChange {
    /** The minimum visible change for alpha - exactly 1/256. */
    const val Alpha: Float = 0.00390625f

    /** The minimum visible change, in pixels. */
    const val Pixels: Float = 1.0f

    /** The minimum visible change for rotation, in degrees. */
    const val RotationDegrees: Float = 0.1f

    /** The minimum visible change for scale. */
    const val Scale: Float = 0.002f

    /** A multiplier applied to the minimum visible change before use. */
    const val ThresholdMultiplier: Float = 0.75f

    /** The minimum visible change for blur. */
    const val Blur: Float = 1.0f

    /** The minimum visible change for color. */
    const val Color: Float = 1.0f

    /** The default minimum visible change. */
    const val Default: Float = 0.001f

    /** The minimum visible change for width and height, in pixels. */
    const val HeightWidth: Float = 0.5f

    /** The minimum visible change for position and translation, in pixels. */
    const val Position: Float = 0.5f

    /** The minimum visible change for corner radius. */
    const val Radius: Float = 0.001f

    /** The minimum visible change shared by rotation and scale. */
    const val RotationScale: Float = 3.2552084E-4f

    /** The minimum visible change for scroll, in pixels. */
    const val Scroll: Float = 1.0f
}

/** Frame and duration constants shared by the motion system. */
object CouiMotionTiming {
    /** The delay between solver ticks, in milliseconds - the spring solver runs at 100 Hz. */
    const val FrameDelayMs: Int = 10

    /** Extra time, in milliseconds, kept running after the spring solver reports it has ended. */
    const val LogicEndExtraTimeMs: Int = 50

    /** Milliseconds per second. */
    const val MillisPerSecond: Int = 1000

    /** The blend duration, in milliseconds, used by the responsive spring motion. */
    const val ResponsiveSpringBlendDurationMs: Int = 250

    /** The divisor used to derive a spring's minimum visible change from its response. */
    const val ResponsiveSpringMinVisibleChangeDivisor: Float = 10000f
}

/**
 * The six press springs, one per press style and direction.
 *
 * Each press style has a distinct spring for the press and for the release, which is why a
 * button "gives" differently than it recovers.
 */
object CouiPressSprings {
    /** The press spring of the Jelly style - bounce 0.0, response 0.25. */
    val JellyPress: CouiSpringData = CouiSpringData(0.0f, 0.25f)

    /** The release (hand-up) spring of the Jelly style - bounce 0.6, response 0.5. */
    val JellyHandUp: CouiSpringData = CouiSpringData(0.6f, 0.5f)

    /** The press spring of the Candy style - bounce 0.0, response 0.3. */
    val CandyPress: CouiSpringData = CouiSpringData(0.0f, 0.3f)

    /** The release spring of the Candy style - bounce 0.5, response 0.5. */
    val CandyHandUp: CouiSpringData = CouiSpringData(0.5f, 0.5f)

    /** The press spring of the Enlarge style - bounce 0.0, response 0.2. */
    val EnlargePress: CouiSpringData = CouiSpringData(0.0f, 0.2f)

    /** The release spring of the Enlarge style - bounce 0.7, response 0.45. */
    val EnlargeHandUp: CouiSpringData = CouiSpringData(0.7f, 0.45f)
}

/**
 * The scalar parameters of the press-scale formula - the ranges it maps into.
 */
object CouiPressScaleRange {
    /** The lower bound of the scale range in the Enlarge style. */
    const val EnlargeMin: Float = 1.17f

    /** The upper bound of the scale range in the Enlarge style. */
    const val EnlargeMax: Float = 1.2f

    /** The aspect ratio at which the aspect-ratio channel activates. */
    const val AspectActivationThreshold: Float = 2.0f

    /** The aspect ratio treated as extreme by the aspect-ratio channel. */
    const val AspectExtremeRatio: Float = 15.0f

    /** The scale applied by the aspect-ratio channel at its base. */
    const val AspectBaseScale: Float = 0.85f

    /** The scale applied by the aspect-ratio channel at the extreme ratio. */
    const val AspectExtremeScale: Float = 1.0f

    /** The lower bound of the area channel's width, in dp. */
    const val MinAreaWidthDp: Float = 20f

    /** The upper bound of the area channel's width, in dp. */
    const val MaxAreaWidthDp: Float = 150f

    /** The lower bound of the default scale range. */
    const val ScaleRangeMin: Float = 0.85f

    /** The upper bound of the default scale range. */
    const val ScaleRangeMax: Float = 0.97f

    /** The hard floor the press physics clamps the target scale to. */
    const val TargetScaleMin: Float = 0.4f

    /** The hard ceiling the press physics clamps the shrink target scale to. */
    const val TargetScaleMaxShrink: Float = 1.0f

    /** The distance scale used when converting finger travel into impact velocity. */
    const val ImpactVelocityDistanceScale: Float = 80.0f
}

/**
 * The touch parameters every coui control shares.
 */
object CouiTouchCommon {
    /** The dead zone, in dp: movement below this never starts a drag. */
    const val DeadZoneDp: Float = 5.0f

    /** The pivot value meaning "use the centre" for either axis. */
    const val PivotAuto: Float = -1.0f

    /** The maximum scale a press may reach. */
    const val MaxScale: Float = 1.2f

    /** The first touch-stiffness coefficient; divided by [K1Divisor] before use. */
    const val K1: Float = 350000f

    /** The second touch-stiffness coefficient; divided by [K2Divisor] before use. */
    const val K2: Float = 15f

    /** The divisor applied to [K1] before use. */
    const val K1Divisor: Float = 1e6f

    /** The divisor applied to [K2] before use. */
    const val K2Divisor: Float = 1e3f

    /** The minimum visible change while pressed. */
    const val PressMinVisibleChange: Float = 0.001f

    /** The minimum visible change on release (hand-up). */
    const val PressHandUpMinVisibleChange: Float = 1.0E-4f

    /** The minimum visible change for deformation. */
    const val DeformMinVisibleChange: Float = 5.0E-4f

    /** The minimum visible change for the rubber band. */
    const val RubberMinVisibleChange: Float = 0.5f
}

/**
 * The rubber-band and hand-up parameters for the layered (tiered) touch motion.
 */
object CouiTouchTiers {
    /** The maximum distance the rubber band may stretch the view. */
    const val MaxStretchDistance: Float = 28.0f

    /** The curvature ratio of the rubber band. */
    const val RubberCurveRatio: Float = 0.05f

    /** The maximum distance of the deform rubber band. */
    const val MaxDeformRubberDistance: Float = 150.0f

    /** The curvature ratio of the deform rubber band. */
    const val DeformRubberCurveRatio: Float = 0.55f

    /** The spring used while dragging - bounce 0.15, response 0.15. */
    val DragSpring: CouiSpringData = CouiSpringData(0.15f, 0.15f)

    /** The release spring for small areas - bounce 0.65, response 0.42. */
    val HandUpSpring: CouiSpringData = CouiSpringData(0.65f, 0.42f)

    /** The release spring for large areas - bounce 0.65, response 0.45. */
    val HandUpSpringLargeArea: CouiSpringData = CouiSpringData(0.65f, 0.45f)

    /** The maximum initial velocity of the release spring. */
    const val HandUpMaxInitialVelocity: Float = 1000.0f

    /** The reference width, in dp, below which an area counts as small. */
    const val HandUpSmallAreaRefWidthDp: Float = 56f

    /** The reference width, in dp, above which an area counts as large. */
    const val HandUpLargeAreaRefWidthDp: Float = 100f
}

/**
 * The defaults the press physics engine starts from.
 */
object CouiPressEngineDefaults {
    /** The default spring response while pressed. */
    const val ResponsePress: Float = 0.13f

    /** The default spring bounce while pressed. */
    const val BouncePress: Float = 0.18f

    /** The default spring response on release. */
    const val ResponseRelease: Float = 0.35f

    /** The default spring bounce on release. */
    const val BounceRelease: Float = 0.62f

    /** The default minimum press scale. */
    const val MinScale: Float = 0.88f

    /** The default maximum press scale. */
    const val MaxScale: Float = 0.97f

    /** The default press intensity. */
    const val Intensity: Float = 1.0f

    /** The default impact factor. */
    const val ImpactFactor: Float = 0.0f
}

/**
 * Per-control spring tables.
 */
object CouiComponentSprings {
    /**
     * The spring the switch uses for its thumb translation - bounce 0.3, response 0.4, with a
     * minimum visible change of 0.1.
     */
    val SwitchToggle: CouiSpringData = CouiSpringData(0.3f, 0.4f)

    /** The response of the switch's drag spring. */
    const val SwitchResponseDrag: Float = 0.3f

    /** The bounce of the switch's drag spring. */
    const val SwitchBounceDrag: Float = 0.0f

    /** The response of the spring that snaps the switch's thumb to the nearest anchor. */
    const val SwitchResponseAttach: Float = 0.4f

    /** The bounce of the spring that snaps the switch's thumb to the nearest anchor. */
    const val SwitchBounceAttach: Float = 0.2f

    /** The thumb's visibility threshold when the switch animates on a state change. */
    const val SwitchToggleMinVisibleChange: Float = 0.1f

    /**
     * The spring the switch's thumb follows the finger with while dragging - bounce 0.0,
     * response 0.3.
     *
     * The thumb is not bound to the finger: a spring animation per axis runs with this spring,
     * so the thumb **lags** behind the finger. That lag is the "resistance" you feel; there is
     * no rubber band.
     */
    val SwitchDrag: CouiSpringData = CouiSpringData(bounce = 0.0f, response = 0.3f)

    /**
     * The spring used to snap the switch's thumb to the nearest anchor once the finger lifts -
     * bounce 0.2, response 0.4.
     *
     * Bouncier than [SwitchDrag], which is why the release overshoots.
     */
    val SwitchAttach: CouiSpringData = CouiSpringData(bounce = 0.2f, response = 0.4f)

    /**
     * The switch's drag dead zone, in raw touch pixels.
     *
     * The thumb refuses to move until the finger has travelled this far from the down point, so
     * a tap that wobbles by a pixel or two does not start a drag.
     */
    const val SwitchTransformDistance: Float = 4.0f
}