// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:property-naming") // Preserve the public defaults API names.

package io.github.wfc35286.coui.kmp.basic
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.graphics.CouiThumbRenderer
import io.github.wfc35286.coui.kmp.graphics.drawCouiTexturedThumb
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.rememberCouiParentIntercept
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.shape.couiCapsulePath
import io.github.wfc35286.coui.kmp.shape.createCouiCapsuleShader
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first

/**
 * Defaults for [CouiSeekBar].
 *
 * Every value here is the default an unattributed bar renders with, matching the ColorOS
 * material. A caller that wants the settings "亮度" look has to override several of them -
 * see [CouiSeekBarBrightness].
 */
object CouiSeekBarDefaults {
    /** The track height at rest. */
    val TrackHeight: Dp = 20.dp

    /**
     * The track's nominal corner radius.
     *
     * **Not used for drawing.** The drawn radius is derived from the track height at runtime
     * (half the current height); this value only feeds the smooth-round-corner path adapter
     * when the smooth style is enabled. Kept because it is part of the public attribute
     * surface - but do not use it as the drawn radius.
     */
    val TrackRadius: Dp = 2.dp

    /** The progress height. */
    val ProgressHeight: Dp = 20.dp

    /** The progress's nominal corner radius. Not used for drawing - see [TrackRadius]. */
    val ProgressRadius: Dp = 2.dp

    /**
     * The thumb's travel inset.
     *
     * It is **derived**, not an attribute: `trackHeight * backgroundEnlargeScale / 2`, i.e.
     * 10dp * 1.4 = 14dp on an unattributed bar. The thumb is offset by exactly this much from
     * each end, which is why the same number shows up as the progress rect's left end minus
     * half the progress height.
     */
    val ProgressPadding: Dp = 14.dp

    /**
     * The press-time progress inset - **not** the resting progress inset.
     *
     * The resting inset is the derived [ProgressPadding] above; this value is kept for
     * documentation so the two are not mistaken for each other.
     */
    val ProgressPaddingPressed: Dp = 4.dp

    /** The thumb's radius at rest. */
    val ThumbRadius: Dp = 6.dp

    /** The thumb's radius while pressed. */
    val ThumbMaxRadius: Dp = 8.dp

    /** The thumb shadow's blur radius. */
    val ThumbShadowSize: Dp = 2.dp

    /** The thumb shadow's vertical offset. */
    val ThumbShadowOffsetY: Dp = 2.dp

    /** The bar's minimum height; layout never goes below this. */
    val MinHeight: Dp = 36.dp

    /**
     * How far the **whole bar** may slide after the finger, in dp.
     *
     * The rubber band never reaches it: it asymptotically approaches this distance as the
     * finger travels, which is why the bar visibly lags further and further behind the thumb
     * instead of tracking it one-to-one.
     */
    val MaxStretchDistance: Dp = 28.dp

    /** The bar's maximum width. */
    val MaxWidth: Dp = 480.dp

    /**
     * The factor the track grows by while pressed.
     *
     * With the default 1.4, a 20dp track swells to 28dp on touch-down; callers that want a
     * static track pass 1.0 (see [CouiStatusBarToggleSlider]).
     */
    const val BackgroundEnlargeScale: Float = 1.4f

    /** The gradient layer's alpha at its bottom stop - the gradient is placed in reverse. */
    internal const val LayerATopAlpha: Int = 0xe6

    /** The gradient layer's alpha at its top stop. */
    internal const val LayerABottomAlpha: Int = 0x8c

    /** The LayerB bitmap's margin, in raw pixels (no density conversion). */
    const val LayerBInset: Float = 12f

    /** The LayerB layer's fill - `#77FFFFFF`. */
    val LayerBFill: Color = Color(0x77FFFFFF)

    /** The LayerB shadow's blur radius, in raw pixels. */
    const val LayerBShadowRadius: Float = 5f

    /** The LayerB shadow's vertical offset, in raw pixels. */
    const val LayerBShadowOffsetY: Float = 0.4f

    /** The LayerB shadow's stroke width, in raw pixels. */
    const val LayerBShadowStrokeWidth: Float = 10f

    /**
     * The spring LayerC fades **out** with when the finger goes down.
     *
     * Pressing down takes LayerC from 1 to 0 - the plain white circle fades away and the
     * gradient/shadow layers fade in underneath it.
     */
    val LayerCInSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * The spring LayerC fades back **in** with to 1.0 when the finger lifts, over the
     * still-fading gradient.
     */
    val LayerCOutSpec: SpringSpec<Float> = SpringBounce0Response030.toSpringSpec()

    /** The spring driving the gradient + shadow layers' alpha. */
    val TextureSpec: SpringSpec<Float> = SpringBounce0Response030.toSpringSpec()

    /**
     * The spring driving the track's height between [TrackHeight] and
     * `TrackHeight * backgroundEnlargeScale` - bounce 0.0, response 0.5.
     */
    val EnlargeSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * The spring the thumb grows with on press - bounce 0.3, response 0.5.
     *
     * The thumb grows from [ThumbRadius] to [ThumbMaxRadius] (6dp -> 8dp) on this spring, which
     * is **bouncier** than the track's [EnlargeSpec]; the extra bounce is what produces the
     * thumb's visible overshoot.
     */
    val ThumbScaleInSpec: SpringSpec<Float> = SpringBounce03Response050.toSpringSpec()

    /** The spring the thumb shrinks with on release - [ThumbScaleInSpec] with `bounce` dropped to `0f`. */
    val ThumbScaleOutSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * The spring the press glow rides towards the finger - bounce 0.15, response 0.35, with a
     * minimum visible change of `1e-4f`.
     *
     * The touch handler retargets it on every move, so the light lags the finger slightly
     * instead of snapping to it.
     */
    val FollowHandSpec: SpringSpec<Float> = CouiSpringData(bounce = 0.15f, response = 0.35f)
        .toSpringSpec(visibilityThreshold = 1e-4f)

    /**
     * The spring the thumb follows the finger with - bounce 0, response 0.15.
     *
     * It is retargeted at the finger-derived pixel on every move, so the thumb **springs** after
     * the finger rather than sticking to it. The response is deliberately stiff, which keeps the
     * bar feeling light under the finger.
     */
    val DragTransitionSpec: SpringSpec<Float> = SpringBounce0Response015.toSpringSpec()

    /**
     * The press glow's end radius, in shader pixels - the radius that makes the glow **local**.
     *
     * The light sits at the finger, so once the finger is more than this far from the bar there
     * is nothing left to light.
     */
    const val SpotLightRadius: Float = 500f

    /** The press glow's end intensity. */
    const val SpotLightIntensity: Float = 0.6f

    /** The ambient term of the glow's light model. */
    const val SpotLightAmbient: Float = 0.4f

    /** The diffuse term of the glow's light model. */
    const val SpotLightDiffuse: Float = 0.4f

    /** The progress bar's colour - the theme primary, dimmed when disabled. */
    @Composable
    fun progressColor(enabled: Boolean): Color {
        val colors = CouiTheme.colors
        return if (enabled) colors.primary else colors.primary.copy(alpha = 0.3f)
    }

    /** The track colour. */
    @Composable
    fun trackColor(): Color = CouiTheme.componentColors.seekBarTrack

    /** The thumb colour - white, or the dedicated disabled colour when disabled. */
    @Composable
    fun thumbColor(enabled: Boolean): Color {
        val componentColors = CouiTheme.componentColors
        return if (enabled) componentColors.seekBarThumb else componentColors.seekBarThumbDisabled
    }

    /**
     * The thumb shadow's colour - `#1a000000` light, `#33ffffff` dark.
     *
     * Note this is **not** the colour the textured thumb renderer uses for its glow: that glow
     * is opaque **white**. This value belongs to the base thumb shadow only.
     */
    @Composable
    fun thumbShadowColor(): Color = CouiTheme.componentColors.seekBarThumbShadow
}

/**
 * A seek bar with click, drag and move-by-distance touch policies.
 *
 * [moveType] selects how the thumb tracks the finger: [CouiSeekBarMoveType.Default] springs the
 * thumb after the finger's position, while [CouiSeekBarMoveType.ByDistance] snaps it to the
 * position implied by the finger's travel. The distance mode has no hold timer: travel beyond
 * touch slop starts the drag.
 *
 * [moveFollowHand] is retained for existing callers: false selects ByDistance unless [moveType]
 * is explicitly supplied. [thumbShadowSize] and [touchPaddingVertical] expose the brightness
 * layout's 0dp shadow and 6dp vertical padding.
 */
@Composable
fun CouiSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundEnlargeScale: Float = CouiSeekBarDefaults.BackgroundEnlargeScale,
    trackColor: Color? = null,
    moveFollowHand: Boolean = true,
    moveType: CouiSeekBarMoveType = if (moveFollowHand) CouiSeekBarMoveType.Default else CouiSeekBarMoveType.ByDistance,
    moveDamping: Float = 1f,
    thumbShadowSize: Dp = CouiSeekBarDefaults.ThumbShadowSize,
    touchPaddingVertical: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val callback by rememberUpdatedState(onValueChange)
    val trackHeight = with(density) { CouiSeekBarDefaults.TrackHeight.toPx() }
    val progressHeight = with(density) { CouiSeekBarDefaults.ProgressHeight.toPx() }
    val thumbRadiusRest = with(density) { CouiSeekBarDefaults.ThumbRadius.toPx() }
    val thumbRadiusPressed = with(density) { CouiSeekBarDefaults.ThumbMaxRadius.toPx() }
    val shadowRadius = with(density) { thumbShadowSize.toPx() }
    val shadowY = with(density) { CouiSeekBarDefaults.ThumbShadowOffsetY.toPx() }
    val touchPadding = with(density) { touchPaddingVertical.toPx() }
    val maxStretch = with(density) { CouiSeekBarDefaults.MaxStretchDistance.roundToPx().toFloat() }
    val paddingHorizontal = trackHeight * backgroundEnlargeScale / 2f
    val maxBackgroundHeight = trackHeight * backgroundEnlargeScale
    val shadowColor = CouiSeekBarDefaults.thumbShadowColor()
    val progressColor = CouiSeekBarDefaults.progressColor(enabled)
    val resolvedTrackColor = trackColor ?: CouiSeekBarDefaults.trackColor()
    val thumbColor = CouiSeekBarDefaults.thumbColor(enabled)
    val thumbRenderer = remember { CouiThumbRenderer() }
    val darkTheme = isSystemInDarkTheme()
    val lightRenderer = remember(darkTheme) { CouiSeekBarLightRenderer(darkTheme) }
    // Each shape needs its own mutable shader instance.
    val trackCapsuleShader = remember { createCouiCapsuleShader() }
    val progressCapsuleShader = remember { createCouiCapsuleShader() }
    val motion = remember { CouiSeekBarMotion(value) }
    val press = remember { CouiSeekBarPressMotion() }
    val rubber = remember { CouiSeekBarRubberMotion() }
    val intercept = rememberCouiParentIntercept()
    var barWidth by remember { mutableIntStateOf(0) }
    var barHeight by remember { mutableIntStateOf(0) }
    var frameWake by remember { mutableIntStateOf(0) }
    var reportedValue by remember { mutableFloatStateOf(value.coerceIn(0f, 1f)) }

    LaunchedEffect(value) {
        val next = value.coerceIn(0f, 1f)
        if (!press.pressed && next != reportedValue) {
            reportedValue = next
            motion.animateTo(next, SpringBounce0Response030)
            frameWake++
        }
    }

    // MOVE only wakes an idle driver. Retargeting does not cancel a frame or lose spring velocity.
    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            val wakeBefore = frameWake
            var moving = false
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.064f)
                lastFrame = now
                moving = motion.advance(dt)
                moving = press.advance(dt) || moving
                moving = rubber.advance(dt) || moving
            }
            if (!moving && frameWake == wakeBefore) {
                lastFrame = 0L
                snapshotFlow { frameWake }.first { it != wakeBefore }
            }
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                barWidth = it.width
                barHeight = it.height
                press.lightBounds(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(enabled, barWidth, barHeight, rtl, moveType, moveDamping, paddingHorizontal, touchPadding) {
                if (!enabled || barWidth <= 0) return@pointerInput
                val travel = (barWidth - 2f * paddingHorizontal).coerceAtLeast(1f)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    motion.stopAtCurrent()
                    val gesture = CouiSeekBarGesture(
                        initialValue = motion.fraction,
                        start = paddingHorizontal,
                        travel = travel,
                        rtl = rtl,
                        moveType = moveType,
                        moveDamping = moveDamping,
                        touchSlop = viewConfiguration.touchSlop,
                        downX = down.position.x,
                    )
                    var released = false
                    try {
                        press.down(down.position.x, down.position.y)
                        rubber.down(down.position.x, maxStretch)
                        frameWake++
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            // A parent taking a vertical scroll cancels the click and still releases effects.
                            if (change.isConsumed) break
                            velocity.addPosition(change.uptimeMillis, change.position)
                            press.lightPosition(change.position.x, change.position.y)
                            if (!change.pressed) {
                                val target = gesture.up(
                                    change.position.x,
                                    change.position.y,
                                    barWidth.toFloat(),
                                    barHeight.toFloat(),
                                    touchPadding,
                                )
                                if (target != null) {
                                    reportedValue = target
                                    callback(target)
                                    if (gesture.dragging) {
                                        motion.snapFraction(target)
                                    } else {
                                        motion.animateTo(target, SpringBounce0Response030)
                                    }
                                }
                                released = true
                                change.consume()
                                break
                            }
                            val target = gesture.move(change.position.x)
                            if (gesture.dragging) {
                                intercept.setDisallowIntercept(true)
                                rubber.move(change.position.x, maxStretch)
                                if (target != null) {
                                    reportedValue = target
                                    callback(target)
                                    if (moveType == CouiSeekBarMoveType.Default) {
                                        motion.animateTo(target, SpringBounce0Response015)
                                    } else {
                                        motion.snapFraction(target)
                                    }
                                }
                                change.consume()
                            }
                            frameWake++
                        }
                    } finally {
                        // CANCEL must never synthesize a tap or leave a pressed track behind.
                        press.up(backgroundEnlargeScale > 1f)
                        rubber.up(
                            if (released) velocity.calculateVelocity().x else 0f,
                            maxStretch,
                            barWidth / density.density,
                            barHeight / density.density,
                        )
                        intercept.setDisallowIntercept(false)
                        frameWake++
                    }
                }
            },
    ) {
        if (barWidth <= 0) return@Canvas
        translate(rubber.shift, 0f) {
            val centreY = size.height / 2f
            val rawHeight = trackHeight + (maxBackgroundHeight - trackHeight) * press.enlarge
            // The in-flight height is temporarily snapped to the pixel grid.
            val height = if (rawHeight != trackHeight && rawHeight != maxBackgroundHeight) rawHeight.roundToInt().toFloat() else rawHeight
            val radius = height / 2f
            val inset = paddingHorizontal - radius
            val background = Rect(inset, centreY - radius, size.width - inset, centreY + radius)
            val trackSize = Size(background.width, height)
            translate(background.left, background.top) {
                if (trackCapsuleShader != null) {
                    drawRect(brush = trackCapsuleShader.brush(trackSize, resolvedTrackColor), size = trackSize)
                } else {
                    drawPath(couiCapsulePath(trackSize), resolvedTrackColor)
                }
            }
            val visualFraction = if (rtl) 1f - motion.fraction else motion.fraction
            val thumbX = paddingHorizontal + (size.width - 2f * paddingHorizontal) * visualFraction
            val progressR = progressHeight / 2f
            val progressStart = if (rtl) size.width - paddingHorizontal else paddingHorizontal
            val left = if (rtl) thumbX - progressR else progressStart - progressR
            val right = if (rtl) progressStart + progressR else thumbX + progressR
            val progressSize = Size((right - left).coerceAtLeast(0f), progressHeight)
            if (progressSize.width > 0f) {
                translate(left, centreY - progressR) {
                    if (progressCapsuleShader != null) {
                        drawRect(brush = progressCapsuleShader.brush(progressSize, progressColor), size = progressSize)
                    } else {
                        drawPath(couiCapsulePath(progressSize), progressColor)
                    }
                }
            }
            drawCouiTexturedThumb(
                renderer = thumbRenderer,
                centreX = thumbX,
                centreY = centreY,
                radius = thumbRadiusRest + (thumbRadiusPressed - thumbRadiusRest) * press.thumbScale,
                layerAAlpha = press.textureAlpha,
                layerCAlpha = press.layerCAlpha,
                layerCColor = thumbColor,
                shadowRadius = shadowRadius,
                shadowOffsetY = shadowY,
                shadowColor = shadowColor,
                enabled = enabled,
            )
            lightRenderer.draw(this, press, background)
        }
    }
}

/** Animates the seek bar's progress with the click spring (0 / 0.3) and drag transition (0 / 0.15), in logical progress space. */
@Stable
internal class CouiSeekBarMotion(initialValue: Float) {
    var fraction by mutableFloatStateOf(initialValue.coerceIn(0f, 1f))
        private set
    private var velocity = 0f
    private var target = fraction
    private var spec = SpringBounce0Response030

    fun stopAtCurrent() {
        target = fraction
        velocity = 0f
    }

    fun animateTo(value: Float, spring: CouiSpringData) {
        target = value.coerceIn(0f, 1f)
        spec = spring
    }

    fun snapFraction(value: Float) {
        fraction = value.coerceIn(0f, 1f)
        stopAtCurrent()
    }

    fun advance(dt: Float): Boolean {
        if (dt <= 0f) return true
        return step(fraction, velocity, target, spec, dt) { v, vel ->
            fraction = v
            velocity = vel
        }
    }
}

/** Shared stopping tolerances for normalized seek-bar spring values. */
private const val SpringRestDistance = 1e-3f
private const val SpringRestVelocity = 1e-2f

/**
 * One step of a coui spring, in the `(bounce, response)` language [CouiSpringData] uses.
 *
 * The integration is the analytic solution for a damped harmonic oscillator rather than an Euler
 * step, so the result does not depend on the frame time - the same reason Compose's own springs
 * are analytic. [apply] writes the new value and velocity back into the holder.
 */
internal inline fun step(
    current: Float,
    velocity: Float,
    target: Float,
    spec: CouiSpringData,
    dt: Float,
    valueThreshold: Float = SpringRestDistance,
    velocityThreshold: Float = SpringRestVelocity,
    apply: (Float, Float) -> Unit,
): Boolean {
    val omega = sqrt(spec.stiffness)
    val zeta = spec.dampingRatio
    val delta = current - target
    val newValue: Float
    val newVelocity: Float
    if (zeta < 1f) {
        // Under-damped.
        val omegaD = omega * sqrt(1f - zeta * zeta)
        val decay = exp(-zeta * omega * dt)
        val cosTerm = cos(omegaD * dt)
        val sinTerm = sin(omegaD * dt)
        newValue = target +
            decay * (delta * cosTerm + ((velocity + zeta * omega * delta) / omegaD) * sinTerm)
        newVelocity = decay *
            (
                velocity * cosTerm -
                    (omegaD * delta + zeta * omega * (velocity + zeta * omega * delta) / omegaD) * sinTerm
                )
    } else {
        // Critically damped (bounce == 0, the common case here).
        val decay = exp(-omega * dt)
        newValue = target + (delta + (velocity + omega * delta) * dt) * decay
        newVelocity = (velocity - omega * (velocity + omega * delta) * dt) * decay
    }
    val moving = abs(newValue - target) >= valueThreshold || abs(newVelocity) >= velocityThreshold
    // The minimum visible change is a **stop** condition, not a "snap to the target" one: when
    // the remaining distance and the speed are both below the threshold the animation ends and
    // the value simply stays where it is. Snapping to the target instead makes every
    // sub-threshold frame jump onto the raw target, and a target that follows the finger
    // (deformation / rubber band) therefore reproduces the touch jitter pixel by pixel.
    if (moving) apply(newValue, newVelocity) else apply(current, 0f)
    return moving
}

/**
 * The brightness-row slider, named after the layout it is modelled on.
 *
 * It pins [CouiSeekBar] to values well away from the [CouiSeekBarDefaults] baseline: no enlarge
 * (1.0), no progress padding, no thumb shadow, and an 18dp radius. The track stays put on
 * touch-down, unlike the default bar which swells to 1.4x.
 */
object CouiStatusBarToggleSlider {
    /** The slider row's total height. */
    val Height: Dp = 36.dp

    /** The track and progress corner radius. */
    val Radius: Dp = 18.dp

    /** The track does **not** swell on press. */
    const val EnlargeScale: Float = 1.0f

    /** The thumb's travel inset - 0dp. */
    val ProgressPadding: Dp = 0.dp

    /** The thumb shadow's size - 0dp. */
    val ThumbShadowSize: Dp = 0.dp

    /**
     * The brightness row's track colour.
     *
     * `#0f000000` light, **opaque** `#ff333333` night: the opaque night value keeps the track
     * visible against the dark card.
     */
    val TrackColor: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF333333) else Color(0x0F000000)

    /**
     * The brightness row's progress colour - `#fffafafa` light, `#ff515151` night.
     *
     * Provided for completeness: the modelled layout does not override the progress colour, so
     * the progress keeps the theme's colour.
     */
    val ProgressColor: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF515151) else Color(0xFFFAFAFA)
}

/** Whether the given travel should be treated as a tap rather than a drag - the touch-slop
 * check performed before committing to a drag. */
internal fun isWithinTouchSlop(deltaX: Float, touchSlop: Float): Boolean = abs(deltaX) < touchSlop

// ---------------------------------------------------------------------------------------------
// Shared spring specs.
//
// Immutable spring values constructed once and reused, so the per-frame animation loops do not
// allocate.
// ---------------------------------------------------------------------------------------------

/** A bounce-0, response-0.5 spring - the track height and the thumb shrink. */
internal val SpringBounce0Response050: CouiSpringData = CouiSpringData(0f, 0.5f)

/** A bounce-0.3, response-0.5 spring - the thumb growth. */
internal val SpringBounce03Response050: CouiSpringData = CouiSpringData(0.3f, 0.5f)

/** A bounce-0, response-0.3 spring - the texture alpha, LayerC fade-out and value-driven progress animation. */
internal val SpringBounce0Response030: CouiSpringData = CouiSpringData(0f, 0.3f)

/** A bounce-0, response-0.15 spring - the drag transition and the glow fade-in. */
internal val SpringBounce0Response015: CouiSpringData = CouiSpringData(0f, 0.15f)

/** A bounce-0, response-0.4 spring - the glow fade-out. */
internal val SpringBounce0Response040: CouiSpringData = CouiSpringData(0f, 0.4f)
