// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:property-naming") // Preserve public COUI defaults API names.

package io.wfc35286.coui.kmp.component

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
import io.wfc35286.coui.kmp.motion.CouiSpringData
import io.wfc35286.coui.kmp.motion.rememberCouiParentIntercept
import io.wfc35286.coui.kmp.motion.toSpringSpec
import io.wfc35286.coui.kmp.shape.couiCapsulePath
import io.wfc35286.coui.kmp.shape.createCouiCapsuleShader
import io.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Defaults for [CouiSeekBar], read from `COUISeekBarCore.initState` and the `coui_seekbar_*`
 * resources of ColorOS 17.0.0.
 *
 * Every value here is the **default** an unattributed `COUISeekBar` gets, i.e. what
 * `com.oplus.settings.widget.SettingsSeekBar` renders with before preference-level overrides. A caller that
 * wants the settings "亮度" look has to override several of them - see [CouiSeekBarBrightness].
 */
object CouiSeekBarDefaults {
    /** `coui_seekbar_background_height` - the track height at rest. */
    val TrackHeight: Dp = 20.dp

    /**
     * `coui_seekbar_background_radius`.
     *
     * **Not used for drawing.** `COUISeekBarCore.getBackgroundRadiusForDraw()` returns
     * `curBackgroundHeight / 2`, and that is the value that reaches `drawRoundRect`; this attr
     * only feeds the G2 path adapter (`COUISeekBarCore$SmoothRoundCornerHelper`) when
     * `smoothStyleType == 1`. Kept here because it is part of the public attr surface - but do
     * not pass it to `drawRoundRect`, which is how this component first shipped an almost square
     * track.
     */
    val TrackRadius: Dp = 2.dp

    /** `coui_seekbar_progress_height`. */
    val ProgressHeight: Dp = 20.dp

    /** `coui_seekbar_progress_radius`. Not used for drawing - see [TrackRadius]. */
    val ProgressRadius: Dp = 2.dp

    /**
     * `COUISeekBarCore.initState`'s `paddingHorizontal` - the thumb's travel inset.
     *
     * It is **derived**, not an attribute: `backgroundHeight * backgroundEnlargeScale / 2`, i.e.
     * 10dp * 1.4 = 14dp on an unattributed bar. `HorizontalOrientationHandler.calculateProgressRect`
     * offsets the thumb by exactly this much, which is why the same number shows up as the blue
     * rect's left end minus half the progress height.
     */
    val ProgressPadding: Dp = 14.dp

    /**
     * `coui_seekbar_progress_pressed_padding_horizontal` - **not** used for the progress inset.
     *
     * The attribute exists and reads 4dp, but `initState` never assigns it to
     * `SeekBarState.paddingHorizontal` (which is the derived value above). Kept for documentation
     * so it is not mistaken for the press-time inset again.
     */
    val ProgressPaddingPressed: Dp = 4.dp

    /** `coui_seekbar_thumb_radius` - the thumb's radius at rest. */
    val ThumbRadius: Dp = 6.dp

    /** `coui_seekbar_thumb_max_radius` - the thumb's radius while pressed. */
    val ThumbMaxRadius: Dp = 8.dp

    /** `coui_seekbar_thumb_shadow_size`. */
    val ThumbShadowSize: Dp = 2.dp

    /** `coui_seekbar_shadow_offset_y`. */
    val ThumbShadowOffsetY: Dp = 2.dp

    /** `coui_seekbar_view_min_height` - `COUISeekBarCore.onMeasure` never goes below this. */
    val MinHeight: Dp = 36.dp

    /**
     * `TouchMotionTierParams.iolMaxStretchDistance` - how far the **whole bar** may slide after the
     * finger, in dp.
     *
     * `TouchMotionRubberBand.apply` never reaches it: the rubber band asymptotically approaches
     * `maxDistance` as the finger travels, which is why the bar visibly lags further and further
     * behind the thumb instead of tracking it one-to-one.
     */
    val MaxStretchDistance: Dp = 28.dp

    /** `coui_seekbar_view_max_width` - `couiSeekBarMaxWidth` defaults to this. */
    val MaxWidth: Dp = 480.dp

    /**
     * `COUISeekBarCore.getDefaultBackgroundEnlargeScale()` - the factor the track grows by while
     * pressed.
     *
     * This is the single most visible difference between the two sliders on the settings
     * brightness page: the "最低自动亮度" row leaves this at 1.4 and its 20dp track swells to
     * 28dp on touch-down, while the "亮度" row sets `couiSeekBarBackGroundEnlargeScale="1.0"`
     * and stays put.
     */
    const val BackgroundEnlargeScale: Float = 1.4f

    /** Original `LAYER_A_ALPHA_TOP` name; the reversed gradient places this stop at the bottom. */
    internal const val LayerATopAlpha: Int = 0xe6

    /** Original `LAYER_A_ALPHA_BOTTOM` name; `(0, height) -> (0, 0)` places this at the top. */
    internal const val LayerABottomAlpha: Int = 0x8c

    /** LayerB bitmap margin, in raw pixels (`const/high16 12.0f`, no density conversion). */
    const val LayerBInset: Float = 12f

    /** `TexturedThumbRenderer` LayerB's fill - `#77FFFFFF`. */
    val LayerBFill: Color = Color(0x77FFFFFF)

    /** `LAYER_B_SHADOW_RADIUS`, in raw pixels. */
    const val LayerBShadowRadius: Float = 5f

    /** `LAYER_B_SHADOW_DY`, in raw pixels. */
    const val LayerBShadowOffsetY: Float = 0.4f

    /** `LAYER_B_SHADOW_STROKE_WIDTH`, in raw pixels. */
    const val LayerBShadowStrokeWidth: Float = 10f

    /**
     * `TexturedThumbRenderer.LAYER_C_SPRING_RESPONSE_IN` - the spring LayerC fades **out** with
     * when the finger goes down.
     *
     * The direction is worth spelling out because it reads backwards: `onPressDown` calls
     * `startLayerCAlphaAnim(1.0f, 0.0f, 0.5f)`, whose parameters are `(startValue, finalPosition,
     * response)`. So pressing down takes LayerC from 1 to 0 - the plain white circle fades away
     * and the gradient/shadow layers fade in underneath it.
     */
    val LayerCInSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * `LAYER_C_SPRING_RESPONSE_OUT` - LayerC fades back **in** to 1.0 with this when the finger
     * lifts, over the still-fading gradient.
     */
    val LayerCOutSpec: SpringSpec<Float> = SpringBounce0Response030.toSpringSpec()

    /** `TEXTURE_SPRING_RESPONSE` / `TEXTURE_SPRING_BOUNCE` - the gradient + shadow layers' alpha. */
    val TextureSpec: SpringSpec<Float> = SpringBounce0Response030.toSpringSpec()

    /**
     * `COUISeekBarCore.mHeightSpringAnim`'s `COUISpringForce` - `bounce 0.0, response 0.5`.
     *
     * Drives the track's height between [TrackHeight] and
     * `TrackHeight * backgroundEnlargeScale`.
     */
    val EnlargeSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * `StandardAnimationHandler.startThumbScaleAnimation(true)`'s spring - `bounce 0.3f`,
     * `response 0.5f`.
     *
     * The thumb grows `curThumbRadius` -> `thumbMaxRadius` (6dp -> 8dp) on this spring, which is
     * **bouncier** than the track's. Driving both from one spring, as this component first did,
     * loses the overshoot you see on the thumb.
     */
    val ThumbScaleInSpec: SpringSpec<Float> = SpringBounce03Response050.toSpringSpec()

    /** `startThumbScaleAnimation(false)` - the same spring with `bounce` dropped to `0f`. */
    val ThumbScaleOutSpec: SpringSpec<Float> = SpringBounce0Response050.toSpringSpec()

    /**
     * `COUISpotLightEffect.ensureFollowHandAnimation` - `bounce 0.15f, response 0.35f`, with a
     * minimum visible change of `1e-4f`.
     *
     * The press glow rides this spring towards the finger, and the touch handler retargets it on
     * every move, so the light lags the finger slightly instead of snapping to it.
     */
    val FollowHandSpec: SpringSpec<Float> = CouiSpringData(bounce = 0.15f, response = 0.35f)
        .toSpringSpec(visibilityThreshold = 1e-4f)

    /**
     * `COUISeekBarAdvanced.initDragTransitionAnim` - `SpringForce(bounce = 0f, response = 0.15f)`.
     *
     * `trackTouchEvent` retargets `mDragTransitionAnim` at the finger-derived pixel on every move,
     * so the thumb **springs** after the finger rather than sticking to it. The response is 0.15 -
     * far stiffer than the 0.5 this component first used for dragging, which is exactly why the
     * official bar feels lighter under the finger.
     */
    val DragTransitionSpec: SpringSpec<Float> = SpringBounce0Response015.toSpringSpec()

    /**
     * `SpotLightType.TYPE_OPAQUE_MEDIUM_1` - the preset `SpotLightEffectRenderer` installs.
     *
     * `mStartRadius 100 -> mEndRadius 500` (shader pixels), `mEndIntensity 0.6`,
     * `mAmbientStrength 0.4`, `mDiffuseStrength 0.4`, `mLightHeight 200`, `mNoise 0.08`,
     * `BlendMode.OVERLAY`.
     *
     * The radius is what makes the glow **local**: the light sits at the finger, so once the
     * finger is more than this far from the bar there is nothing left to light. The first version
     * ignored the radius entirely (and pinned y to the bar's centre), so its glow never went away.
     */
    const val SpotLightRadius: Float = 500f

    /** `mEndIntensity`. */
    const val SpotLightIntensity: Float = 0.6f

    /** `mAmbientStrength` / `mDiffuseStrength` - the light model's two terms. */
    const val SpotLightAmbient: Float = 0.4f

    /** `mDiffuseStrength`. */
    const val SpotLightDiffuse: Float = 0.4f

    /**
     * The progress bar's colour - `res/color-v23/coui_seekbar_progress_selector.xml`, which is
     * `?attr/couiColorDisable` when disabled and `?attr/couiColorContainerTheme` otherwise.
     */
    @Composable
    fun progressColor(enabled: Boolean): Color {
        val colors = CouiTheme.colors
        return if (enabled) colors.primary else colors.primary.copy(alpha = 0.3f)
    }

    /**
     * The track colour - `coui_seekbar_background_selector.xml`, which resolves to
     * `?attr/couiColorContainer12` in both states.
     */
    @Composable
    fun trackColor(): Color = CouiTheme.componentColors.seekBarTrack

    /**
     * The thumb colour - `coui_seekbar_thumb_selector.xml`:
     * `coui_color_white`, or `coui_seekbar_thumb_disable_color` when disabled.
     */
    @Composable
    fun thumbColor(enabled: Boolean): Color {
        val componentColors = CouiTheme.componentColors
        return if (enabled) componentColors.seekBarThumb else componentColors.seekBarThumbDisabled
    }

    /**
     * `coui_seekbar_thumb_shadow_color` - `#1a000000` light, `#33ffffff` dark.
     *
     * Note this is **not** the colour `TexturedThumbRenderer` uses for its glow: that one calls
     * `setShadowLayer(5px, 0, 0.4px, -1)`, i.e. opaque **white**. This value belongs to the base
     * `StandardThumbRenderer.drawThumbRoundRect`, which `TexturedThumbRenderer` overrides.
     */
    @Composable
    fun thumbShadowColor(): Color = CouiTheme.componentColors.seekBarThumbShadow
}

/**
 * Settings 17's COUISeekBar, with click, drag and MOVE_BY_DISTANCE touch policies.
 *
 * [moveType] uses the APK's MOVE_BY_DEFAULT / MOVE_BY_FINGER / MOVE_BY_DISTANCE values.
 * `ScreenMinBrightnessPreference.onBindViewHolder` explicitly chooses [CouiSeekBarMoveType.ByDistance]
 * with damping 1; `SettingsBrightnessPreference` keeps [CouiSeekBarMoveType.Default].
 * The distance mode has no hold timer: travel beyond touch slop starts the drag.
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
            // Core.drawBackground temporarily snaps in-flight height to the pixel grid.
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

/** ClickAnimationHandler (0 / 0.3) and drag transition (0 / 0.15), in logical progress space. */
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
 * One step of a COUI spring, in the `(bounce, response)` language [CouiSpringData] uses.
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
    if (moving) apply(newValue, newVelocity) else apply(target, 0f)
    return moving
}

/**
 * The `SettingsBrightnessPreference` slider, named after the layout it inflates.
 *
 * On ColorOS 17 that row is:
 *
 * ```
 * com.oplus.settings.feature.display.SettingsBrightnessPreference   (the preference)
 *   → com.oplus.settings.widget.OplusToggleSliderView               (RelativeLayout + ToggleSlider)
 *       → res/layout/status_bar_toggle_slider.xml                   (inflated in its constructor)
 *           → com.coui.appcompat.seekbar.COUISeekBar                (the actual bar)
 * ```
 *
 * `status_bar_toggle_slider.xml` pins that [CouiSeekBar] to values well away from the
 * unattributed default: no enlarge (`1.0`), no progress padding, no thumb shadow, an 18dp
 * radius, and a custom `android:thumb` drawable - which decodes to a **fully transparent
 * placeholder** (see `COUI-菜单弹窗与进度条深扒.md` §7.1), so the thumb is drawn entirely by
 * `TexturedThumbRenderer`. Those are the values below.
 *
 * Its sibling row, `ScreenMinBrightnessPreference`, hosts a `SettingsSeekBar` (which extends
 * `COUISeekBar` with **no** attributes at all) and therefore uses the plain
 * [CouiSeekBarDefaults]. The two differ most visibly on touch-down: this one stays put, that one
 * swells to 1.4x.
 */
object CouiStatusBarToggleSlider {
    /** `brightness_toggle_slider_height`. */
    val Height: Dp = 36.dp

    /** `brightness_toggle_slider_corner_radius` - `couiSeekBarBackground/ProgressRadius`. */
    val Radius: Dp = 18.dp

    /** `couiSeekBarBackGroundEnlargeScale="1.0"` - the track does **not** swell. */
    const val EnlargeScale: Float = 1.0f

    /** `couiSeekBarProgressPaddingHorizontal="0dp"`. */
    val ProgressPadding: Dp = 0.dp

    /** `couiSeekBarThumbShadowSize="0dp"`. */
    val ThumbShadowSize: Dp = 0.dp

    /**
     * `color/status_bar_qs_brightness_slider_bg_color` - the brightness row's track colour.
     *
     * Straight out of the APK's resource table:
     *
     * ```
     * resource 0x7f060dd8 color/status_bar_qs_brightness_slider_bg_color
     *     ()      #0f000000
     *     (night) #ff333333
     * ```
     *
     * The night value is **opaque** `#333333`, not a translucent black: a hard-coded `#0f000000`
     * disappears into the dark card, which is what the row looked like before this was read off
     * the device.
     */
    val TrackColor: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF333333) else Color(0x0F000000)

    /**
     * `color/status_bar_qs_brightness_slider_progress_color` - `#fffafafa` light, `#ff515151`
     * night.
     *
     * The layout only overrides `couiSeekBarBackgroundColor`, so the progress keeps the theme's
     * blue; this is here for completeness because the resource exists.
     */
    val ProgressColor: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF515151) else Color(0xFFFAFAFA)
}

/** Whether the given travel should be treated as a tap rather than a drag, mirroring the
 * `mTouchSlop` check `COUISeekBarAdvanced.onTouchEvent` performs before it commits to a drag. */
internal fun isWithinTouchSlop(deltaX: Float, touchSlop: Float): Boolean = abs(deltaX) < touchSlop

// ---------------------------------------------------------------------------------------------
// Shared spring specs.
//
// These used to be constructed inline inside the per-frame `advance` loops, which allocated a new
// `CouiSpringData` several times per frame for as long as the finger was down. On device that
// showed up as a GC storm (`gc_priority_optimize set UX fail-2` x8) and a multi-second freeze.
// They are immutable values, so one instance each is all that is needed.
// ---------------------------------------------------------------------------------------------

/** `mHeightSpringAnim` / `startThumbScaleAnimation(false)` - bounce 0, response 0.5. */
internal val SpringBounce0Response050: CouiSpringData = CouiSpringData(0f, 0.5f)

/** `startThumbScaleAnimation(true)` - bounce 0.3, response 0.5. */
internal val SpringBounce03Response050: CouiSpringData = CouiSpringData(0.3f, 0.5f)

/** `TexturedThumbRenderer`'s texture / LayerC-out springs - bounce 0, response 0.3. */
internal val SpringBounce0Response030: CouiSpringData = CouiSpringData(0f, 0.3f)

/** `COUISpotLightEffect.animate(true)` - bounce 0, response 0.15. */
internal val SpringBounce0Response015: CouiSpringData = CouiSpringData(0f, 0.15f)

/** `COUISpotLightEffect.animate(false)` - bounce 0, response 0.4. */
internal val SpringBounce0Response040: CouiSpringData = CouiSpringData(0f, 0.4f)
