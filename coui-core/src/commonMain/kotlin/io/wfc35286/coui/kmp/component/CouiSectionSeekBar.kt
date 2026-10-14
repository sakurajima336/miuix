// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:property-naming") // Preserve public COUI defaults API names.

package io.wfc35286.coui.kmp.component

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.wfc35286.coui.kmp.motion.rememberCouiParentIntercept
import io.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Defaults for [CouiSectionSeekBar], read from `COUISectionSeekBar` / `SectionAnimationHandler` /
 * `SectionProgressRenderer` of `小布输入法_1.7.38.17-os.apk` (com.oplus.keyboard) and the sibling
 * copy in `设置_17.0.0.apk`.
 *
 * The row this reproduces is `res/nJ.xml` of the keyboard APK:
 *
 * ```
 * COUICustomListSelectedLinearLayout (paddingHorizontal 32dp)
 *  └ ConstraintLayout
 *     ├ TextView  tv_title      ("识别速度" / "笔迹粗细")
 *     ├ TextView  tv_type_start ("慢" / "细")
 *     ├ TextView  tv_type_end   ("快" / "粗")
 *     └ com.coui.appcompat.seekbar.COUISectionSeekBar  id=section_seek_bar
 * ```
 */
object CouiSectionSeekBarDefaults {
    /**
     * `coui_section_seekbar_tick_mark_radius` = **3.0dp**.
     *
     * `COUISectionSeekBar.initState` reads this dimen and stores it into
     * `SeekBarState.sectionMarkRadius`; `SectionProgressRenderer` then hands that value straight to
     * `Canvas.drawCircle`. It is a real density-scaled dimension - the earlier port treated it as a
     * raw pixel radius, which at density 2.75 drew a ~1px dot instead of an 8.25px mark.
     */
    val MarkRadius: Dp = 3.dp

    /**
     * `SectionAnimationHandler.DRAG_MOVE_RATIO` - while dragging, the thumb only travels this
     * fraction of the finger's movement. That damping is what makes the bar feel "detented": the
     * thumb lags, then flips to the next section.
     */
    const val DragMoveRatio: Float = 0.4f

    /** `SectionAnimationHandler.SNAP_POSITION_THRESHOLD` - half a section flips it. */
    const val SnapPositionThreshold: Float = 0.5f

    /** `SectionAnimationHandler.DRAG_SNAP_DURATION` - the flip animation while dragging. */
    const val DragSnapDurationMs: Int = 100

    /** `SectionAnimationHandler.RELEASE_SNAP_DURATION` - the settle animation on lift. */
    const val ReleaseSnapDurationMs: Int = 150

    /** `SectionAnimationHandler.PROGRESS_TRANSITION_DURATION` - programmatic `setProgress`. */
    const val ProgressTransitionDurationMs: Int = 200

    /** `COUISectionSeekBar.VELOCITY_COMPUTE_TIME` - the window the drag velocity is measured over. */
    const val VelocityComputeTimeMs: Int = 100

    /** `COUISectionSeekBar.DRAG_DIRECTION_THRESHOLD` - how far the finger must move to flip direction. */
    const val DragDirectionThreshold: Float = 0.5f

    /**
     * `COUISectionSeekBar.POSITION_BIAS`.
     *
     * Used by the keyboard's `getFingerTrackX` when the finger sits outside the track, so the
     * outermost section still wins.
     */
    const val PositionBias: Float = 0.1f

    /** The track's colour - same selector as the plain bar (`coui_seekbar_background_selector`). */
    @Composable
    fun trackColor(): Color = CouiTheme.componentColors.seekBarTrack

    /** The filled part - `coui_seekbar_progress_selector`. */
    @Composable
    fun progressColor(): Color = CouiTheme.colors.primary

    /**
     * `coui_seekbar_active_mark_selector` - the marks **left of** the thumb.
     *
     * The selector resolves to `?attr/couiColorLabelOnColor` when enabled, which is `#ffffff` in
     * both `Theme.COUI.Main` and `Theme.COUI.Main.Dark`. Disabled is
     * `coui_seekbar_active_mark_disable_color` = `#8affffff` light / `#29ffffff` night.
     */
    @Composable
    fun activeMarkColor(enabled: Boolean = true): Color = when {
        enabled -> Color(0xFFFFFFFF)
        isSystemInDarkTheme() -> Color(0x29FFFFFF)
        else -> Color(0x8AFFFFFF)
    }

    /**
     * `coui_seekbar_inactive_mark_selector` - the marks **right of** the thumb.
     *
     * `?attr/couiColorLabelTertiary` = `#42000000` light / `#4dffffff` night; disabled is
     * `coui_seekbar_inactive_mark_disable_color` = `#42000000` light / `#29ffffff` night.
     */
    @Composable
    fun inactiveMarkColor(enabled: Boolean = true): Color = when {
        enabled && isSystemInDarkTheme() -> Color(0x4DFFFFFF)
        enabled -> Color(0x42000000)
        isSystemInDarkTheme() -> Color(0x29FFFFFF)
        else -> Color(0x42000000)
    }

    /** `coui_seekbar_thumb_selector`. */
    @Composable
    fun thumbColor(): Color = CouiTheme.componentColors.seekBarThumb
}

/**
 * A COUI **sectioned** seek bar - the `com.coui.appcompat.seekbar.COUISectionSeekBar` of the
 * keyboard's handwriting settings ("识别速度" / "笔迹粗细").
 *
 * It is not a plain slider: every step is a detent. The pieces, all ported from smali:
 *
 * | behaviour | source |
 * |---|---|
 * | marks drawn at every step, hidden under the thumb | `SectionProgressRenderer.drawMarks` / `drawMarkIfVisible` |
 * | marks left of the thumb use the active colour, right of it the inactive one | `drawMarks` |
 * | anchored whole-section travel plus residual x0.6 | `COUISectionSeekBar.trackTouchEvent` |
 * | node transitions during dragging use a 100ms animation and haptic feedback | `startDragSnapAnimation`, `performFeedback()` |
 * | taps use a separate 200ms progress transition | `startClickTransitionAnim`, `startProgressTransitionAnim` |
 * | lifting settles onto the nearest section over 150ms | `snapToNearestSection` + `RELEASE_SNAP_DURATION` |
 *
 * @param value The current section, in `min..max`.
 * @param onValueChange Called once per committed section change.
 * @param modifier The modifier to apply.
 * @param min The lowest section.
 * @param max The highest section - there are `max - min + 1` marks.
 * @param enabled Whether the bar can be interacted with.
 */
@Composable
fun CouiSectionSeekBar(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 4,
    enabled: Boolean = true,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val trackHeight = with(density) { CouiSeekBarDefaults.TrackHeight.toPx() }
    val progressHeight = with(density) { CouiSeekBarDefaults.ProgressHeight.toPx() }
    val markRadiusPx = with(density) { CouiSectionSeekBarDefaults.MarkRadius.roundToPx().toFloat() }
    // `TexturedThumbRenderer`'s LayerB / shadow dimensions, shared with the plain bar's defaults.
    val thumbShadowRadius = with(density) { CouiSeekBarDefaults.ThumbShadowSize.toPx() }
    val thumbShadowOffsetY = with(density) { CouiSeekBarDefaults.ThumbShadowOffsetY.toPx() }
    val thumbShadowColor = CouiSeekBarDefaults.thumbShadowColor()
    val thumbRenderer = remember { CouiThumbRenderer() }
    val thumbRadiusRest = with(density) { CouiSeekBarDefaults.ThumbRadius.toPx() }
    val thumbRadiusPressed = with(density) { CouiSeekBarDefaults.ThumbMaxRadius.toPx() }
    val paddingHorizontal = trackHeight * CouiSeekBarDefaults.BackgroundEnlargeScale / 2f
    val maxBackgroundHeight = trackHeight * CouiSeekBarDefaults.BackgroundEnlargeScale
    val maxStretch = with(density) { CouiSeekBarDefaults.MaxStretchDistance.roundToPx().toFloat() }
    val press = remember { CouiSeekBarPressMotion() }
    val rubber = remember { CouiSeekBarRubberMotion() }
    val darkTheme = isSystemInDarkTheme()
    val lightRenderer = remember(darkTheme) { CouiSeekBarLightRenderer(darkTheme) }
    val callback by rememberUpdatedState(onValueChange)

    val trackColor = CouiSectionSeekBarDefaults.trackColor()
    val progressColor = CouiSectionSeekBarDefaults.progressColor()
    val activeMarkColor = CouiSectionSeekBarDefaults.activeMarkColor()
    val inactiveMarkColor = CouiSectionSeekBarDefaults.inactiveMarkColor()
    val thumbColor = CouiSectionSeekBarDefaults.thumbColor()

    val section = remember(min, max) { CouiSectionMotion(min, max) }
    // `performFeedback()` - the section bar ticks once per detent, both when a tap snaps the thumb
    // onto a section and when a drag crosses one.
    val haptics = rememberCouiHaptics()
    // `attemptClaimDrag()` - keeps the enclosing scroll container from stealing a drag.
    val intercept = rememberCouiParentIntercept()
    var barWidth by remember { mutableIntStateOf(0) }
    var barHeight by remember { mutableIntStateOf(0) }
    // Monotonic wake signal: pointer events never restart the running frame loop.
    var frameWake by remember { mutableIntStateOf(0) }
    // The last section this component reported upwards. `LaunchedEffect(value)` must not treat its
    // own report as an external `setProgress`.
    var reportedValue by remember { mutableIntStateOf(value) }

    // `setProgress` **from outside** goes through the 200ms transition animation
    // (`PROGRESS_TRANSITION_DURATION`).
    //
    // The `value != reportedValue` guard matters: without it every section this component commits
    // while dragging came straight back in as a new `value`, restarting a 200ms `transitionTo` on
    // top of the drag - two animations fighting over `thumbX`, a frame loop that never idled, and
    // the bar freezing under the finger.
    LaunchedEffect(value, barWidth) {
        if (!section.dragging && barWidth > 0 && value != reportedValue) {
            reportedValue = value
            section.transitionTo(
                value = value,
                length = (barWidth - 2f * paddingHorizontal).coerceAtLeast(1f),
                minSection = min,
            )
            // The 200ms programmatic transition needs the frame driver awake too.
            frameWake++
        }
    }

    // Once the width is known, park the thumb on the caller's section without animating - the
    // original does the same in `onSizeChanged` -> `syncSectionThumbXFromProgress`.
    LaunchedEffect(barWidth, min, max) {
        if (barWidth > 0) section.jumpTo(value, (barWidth - 2f * paddingHorizontal).coerceAtLeast(1f), min)
    }

    // One coroutine per section state. A MOVE wakes the parked driver, never cancels a frame.
    LaunchedEffect(section) {
        var lastFrame = 0L
        while (true) {
            val wakeBefore = frameWake
            var moving = false
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.064f)
                lastFrame = now
                moving = section.advance(dt)
                moving = press.advance(dt) || moving
                moving = rubber.advance(dt) || moving
            }
            // Park only when the springs have settled *and* no gesture asked for a frame while we
            // were drawing. Reading `frameWake` after the frame is what makes a MOVE that lands
            // mid-frame impossible to miss.
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
            .pointerInput(enabled, barWidth, barHeight, rtl, min, max) {
                if (!enabled || barWidth <= 0 || max <= min) return@pointerInput
                val start = paddingHorizontal
                val travel = (barWidth - 2f * paddingHorizontal).coerceAtLeast(1f)
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    val downTrackX = section.fingerTrackX(down.position.x, start, travel, rtl)
                    var started = false
                    var released = false
                    try {
                        press.down(down.position.x, down.position.y)
                        rubber.down(down.position.x, maxStretch)
                        frameWake++
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            velocity.addPosition(change.uptimeMillis, change.position)
                            press.lightPosition(change.position.x, change.position.y)
                            val fingerX = section.fingerTrackX(change.position.x, start, travel, rtl)
                            if (!change.pressed) {
                                val committed = if (started) {
                                    section.dragMove(fingerX, travel, min) {
                                        reportedValue = it
                                        callback(it)
                                        haptics.granularShort()
                                    }
                                    section.release(travel, min)
                                } else if (change.position.x in 0f..barWidth.toFloat() &&
                                    change.position.y in 0f..barHeight.toFloat()
                                ) {
                                    // Section.startClickTransitionAnim -> setProgress(target,true,true)
                                    // -> startProgressTransitionAnim: 200ms ValueAnimator (default ease).
                                    section.tapTo(fingerX, travel, min)
                                } else {
                                    null
                                }
                                if (committed != null) {
                                    reportedValue = committed
                                    callback(committed)
                                    if (!started) haptics.granularShort()
                                }
                                released = true
                                change.consume()
                                break
                            }
                            if (!started) {
                                if (abs(change.position.x - down.position.x) <= touchSlop) {
                                    frameWake++
                                    continue
                                }
                                started = true
                                section.dragging = true
                                intercept.setDisallowIntercept(true)
                                rubber.move(change.position.x, maxStretch)
                                // First trackTouchEvent only anchors and returns; no snap tween here.
                                val jumped = section.initDragAnchor(fingerX, travel, downTrackX, min)
                                if (jumped != null) {
                                    reportedValue = jumped
                                    callback(jumped)
                                    haptics.granularShort()
                                }
                            } else {
                                rubber.move(change.position.x, maxStretch)
                                section.dragMove(fingerX, travel, min) {
                                    reportedValue = it
                                    callback(it)
                                    haptics.granularShort()
                                }
                                section.setLastFingerTrackX(fingerX)
                            }
                            frameWake++
                            change.consume()
                        }
                    } finally {
                        if (!released && started) {
                            section.release(travel, min)?.let {
                                reportedValue = it
                                callback(it)
                            }
                        }
                        section.dragging = false
                        press.up(trackEnlarges = true)
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
            val rawBackgroundHeight = trackHeight + (maxBackgroundHeight - trackHeight) * press.enlarge
            // `COUISeekBarCore.drawBackground`:
            //
            // ```java
            // float h = state.getCurBackgroundHeight();
            // float r = state.getCurBackgroundRadius();
            // boolean midAnim = h != state.getBackgroundHeight() && h != mMaxBackgroundHeight;
            // if (midAnim) {                       // only while the spring is travelling
            //     float snapped = snapHeightToPixelGrid(h);   // == Math.round(h)
            //     state.setCurBackgroundHeight(snapped);
            //     state.setCurBackgroundRadius(snapped / 2f);
            // }
            // ... calculateBackgroundRect + drawInactiveTrack ...
            // if (midAnim) { state.setCurBackgroundHeight(h); state.setCurBackgroundRadius(r); }
            // ```
            //
            // The height is rounded to a **whole pixel** while the swell is in flight, and the float is
            // restored afterwards. Drawing the raw float (as this did) puts the track's top and bottom
            // edges on sub-pixel positions every frame, so the capsule shimmers and its centre drifts
            // during the grow/shrink - the official instead grows in clean one-pixel steps.
            val midEnlargeAnim = rawBackgroundHeight != trackHeight && rawBackgroundHeight != maxBackgroundHeight
            val curBackgroundHeight =
                if (midEnlargeAnim) rawBackgroundHeight.roundToInt().toFloat() else rawBackgroundHeight
            val backgroundRadius = curBackgroundHeight / 2f
            val backgroundInset = paddingHorizontal - curBackgroundHeight / 2f
            val backgroundLeft = backgroundInset
            val backgroundRight = size.width - backgroundInset
            val backgroundTop = centreY - backgroundRadius

            // Preserve the previously validated plain capsule used by the section bar.
            val trackSize = Size(backgroundRight - backgroundLeft, curBackgroundHeight)
            translate(backgroundLeft, backgroundTop) {
                // FIDELITY NOTE: the table-based G2 sampler degenerates at radius=halfHeight.
                // Use the plain round-rect fallback here; this is not evidence about libhwui's path.
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset.Zero,
                    size = trackSize,
                    cornerRadius = CornerRadius(backgroundRadius, backgroundRadius),
                )
            }

            val thumbRadius = thumbRadiusRest + (thumbRadiusPressed - thumbRadiusRest) * press.thumbScale
            // `section.thumbX` is *travel-local*: it runs 0..travel, where `travel` excludes the
            // `paddingHorizontal` inset on both sides. The thumb, the blue rect and the marks must all
            // be drawn in **view-local** space, so the inset has to be added back here. Drawing the
            // thumb without it (which this did) put the thumb exactly `paddingHorizontal` to the left of
            // its own mark.
            val travel = (size.width - 2f * paddingHorizontal).coerceAtLeast(1f)
            val thumbCentreX = paddingHorizontal + if (rtl) travel - section.thumbX else section.thumbX
            val progressR = progressHeight / 2f
            val progressTop = centreY - progressR
            val progressStartX = if (rtl) size.width - paddingHorizontal else paddingHorizontal
            val rectStart = if (rtl) thumbCentreX - progressR else progressStartX - progressR
            val rectEnd = if (rtl) progressStartX + progressR else thumbCentreX + progressR
            val progressWidth = (rectEnd - rectStart).coerceAtLeast(0f)
            if (progressWidth > 0f) {
                // Same reasoning as the track above - this rect's width is animated every frame.
                translate(rectStart, progressTop) {
                    drawRoundRect(
                        color = progressColor,
                        topLeft = Offset.Zero,
                        size = Size(progressWidth, progressHeight),
                        cornerRadius = CornerRadius(progressR, progressR),
                    )
                }
            }

            // `SectionProgressRenderer.drawMarks`: one circle per section, skipping the one hidden by
            // the thumb, active colour on the filled side.
            drawSectionMarks(
                min = min,
                max = max,
                thumbX = thumbCentreX,
                thumbRadius = thumbRadius,
                centreY = centreY,
                travelStart = paddingHorizontal,
                travelLength = (size.width - 2f * paddingHorizontal).coerceAtLeast(1f),
                rtl = rtl,
                canvasWidth = size.width,
                markRadius = markRadiusPx,
                activeColor = activeMarkColor,
                inactiveColor = inactiveMarkColor,
            )

            drawCouiTexturedThumb(
                renderer = thumbRenderer,
                centreX = thumbCentreX,
                centreY = centreY,
                radius = thumbRadius,
                layerAAlpha = press.textureAlpha,
                layerCAlpha = press.layerCAlpha,
                layerCColor = thumbColor,
                shadowRadius = thumbShadowRadius,
                shadowOffsetY = thumbShadowOffsetY,
                shadowColor = thumbShadowColor,
                enabled = enabled,
            )
            lightRenderer.draw(
                this,
                press,
                Rect(backgroundLeft, backgroundTop, backgroundRight, backgroundTop + curBackgroundHeight),
            )
        }
    }
}

/** The mark drawing of `SectionProgressRenderer`. */
private fun DrawScope.drawSectionMarks(
    min: Int,
    max: Int,
    thumbX: Float,
    thumbRadius: Float,
    centreY: Float,
    travelStart: Float,
    travelLength: Float,
    rtl: Boolean,
    canvasWidth: Float,
    markRadius: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    if (max <= min) return
    val span = (max - min).toFloat()
    // `drawMarks` starts from the inactive colour and switches once it passes the thumb; in RTL
    // that means it starts on the inactive side.
    for (step in min..max) {
        val fraction = (step - min) / span
        val x = if (rtl) canvasWidth - travelStart - travelLength * fraction else travelStart + travelLength * fraction
        val onFilledSide = if (rtl) x > thumbX else x < thumbX
        val color = if (onFilledSide) activeColor else inactiveColor
        // `drawMarkIfVisible`: skip when the mark sits entirely under the thumb.
        if (x - markRadius > thumbX - thumbRadius &&
            x + markRadius < thumbX + thumbRadius
        ) {
            continue
        }
        // `SectionProgressRenderer` passes `state.sectionMarkRadius`, i.e. the
        // `coui_section_seekbar_tick_mark_radius` dimen (3dp), straight into `drawCircle`.
        drawCircle(color = color, radius = markRadius, center = Offset(x, centreY))
    }
}

/**
 * The detent state machine, a port of `SectionAnimationHandler`.
 *
 * Kept separate from the drawing so the numbers below can be read against the smali one by one.
 */
@Stable
internal class CouiSectionMotion(private val min: Int, private val max: Int) {
    var thumbX by mutableFloatStateOf(0f)
        private set
    var dragging = false
    var lastFingerTrackX = 0f
        private set

    /** The section currently committed to the caller. */
    private var currentSection = min
    private var committedSection = Int.MIN_VALUE

    private var thumbXVelocity = 0f
    private var initialised = false

    // `SectionAnimationHandler` drag fields.
    private var anchorSection = Int.MIN_VALUE
    private var anchorThumbX = 0f
    private var dragCurrentOffset = 0f
    private var dragDirection = 0
    private var dragMoveAnimValue = 0f
    private var dragMoveAnimStartX = 0f
    private var dragMoveAnimEndX = Float.NaN

    /**
     * True while the *drag* detent animator is running, as opposed to the release/transition snap.
     *
     * The two use different update formulas: the drag one keeps adding the newest finger residual
     * (`dragCurrentOffset * 0.6f`) so the thumb never stops following the finger, while the release
     * one is a plain tween onto the final section.
     */
    private var dragSnapActive = false

    /** The track length the running drag-snap is expressed in. */
    private var dragTrackLength = 0f

    private var snapFrom = 0f
    private var snapTo = 0f
    private var snapElapsed = 0f
    private var snapDuration = 0f

    private var progressTransition = false

    private fun sectionWidth(length: Float) = length / (max - min).toFloat()

    /** The pixel X of a section - `SeekBarState.progressToScale(section) * length`. */
    fun sectionX(section: Int, length: Float): Float {
        val width = sectionWidth(length)
        val raw = width * (section - min)
        return raw
    }

    /**
     * `COUISectionSeekBar.getFingerTrackX`:
     * `(primaryToHorizontalAbsoluteScale(x, length) * length).coerceIn(0f, length)`.
     *
     * Note this is the finger's **absolute** position on the track, not a delta from where it went
     * down - the whole drag maths below is expressed in that space.
     */
    fun fingerTrackX(x: Float, start: Float, length: Float, rtl: Boolean): Float {
        val raw = if (rtl) length - (x - start) else x - start
        return raw.coerceIn(0f, length)
    }

    /**
     * `SectionAnimationHandler.initDragAnchor(FFF)` - `(fingerTrackX, length, fingerDownTrackX)`.
     *
     * Two details matter and both were wrong in the first port:
     *
     * 1. `dragAnchorThumbX` is the **section's** pixel position,
     *    `state.progressToScale(anchorSection) * length` - not the finger's down position. The
     *    quantiser below measures `offset = fingerTrackX - dragAnchorThumbX` from the detent, so
     *    anchoring on the finger would shift every subsequent step by however far from the detent
     *    the finger landed.
     * 2. `dragDirection` is seeded here from `fingerTrackX >= dragAnchorThumbX`, so the very first
     *    MOVE already has a direction instead of waiting for `DRAG_DIRECTION_THRESHOLD`.
     */
    fun initDragAnchor(fingerTrackX: Float, length: Float, fingerDownTrackX: Float, minSection: Int): Int? {
        val width = sectionWidth(length)
        if (width <= 0f) return null
        val previous = currentSection
        anchorSection = ((fingerDownTrackX / width).roundToInt() + minSection).coerceIn(minSection, max)
        this.anchorThumbX = sectionX(anchorSection, length)
        dragDirection = if (fingerTrackX >= this.anchorThumbX) 1 else -1
        dragCurrentOffset = 0f
        dragMoveAnimValue = 0f
        dragMoveAnimStartX = this.anchorThumbX
        dragMoveAnimEndX = Float.NaN
        dragSnapActive = false
        // A fresh gesture must not inherit the previous lift's 150ms settle: while that snap was
        // still marked as running, `dragMove`'s follow-the-finger branch (`snapDuration <= 0f`) was
        // skipped, so the first moments of a new drag did not track the finger.
        snapDuration = 0f
        lastFingerTrackX = fingerTrackX
        // SectionAnimationHandler.initDragAnchor directly sets sectionThumbX to the anchor.
        thumbX = this.anchorThumbX
        thumbXVelocity = 0f
        currentSection = anchorSection
        committedSection = anchorSection
        return if (anchorSection != previous) anchorSection else null
    }

    /**
     * One MOVE of `COUISectionSeekBar.trackTouchEvent`.
     *
     * The thumb is `anchor + wholeSections + residual * 0.6f`: the sections themselves are
     * quantized (with `POSITION_BIAS` nudging in the direction of travel), and only the leftover
     * inside the current section is damped - the literal `0.6f` is in the smali.
     */
    fun dragMove(fingerTrackX: Float, length: Float, minSection: Int, onCommit: (Int) -> Unit) {
        val width = sectionWidth(length)
        if (width <= 0f) return

        // Direction detection, 0.5px threshold against the previous absolute position.
        val direction = when {
            fingerTrackX > lastFingerTrackX + DirectionThreshold -> 1
            fingerTrackX < lastFingerTrackX - DirectionThreshold -> -1
            else -> 0
        }
        if (direction != 0 && dragDirection == 0) {
            dragDirection = direction
        } else if (direction != 0 && direction == -dragDirection) {
            // `onDirectionReversed`:
            //
            // ```java
            // if (dragAnchorSection != state.getProgress()) {
            //     dragAnchorSection = state.getProgress();
            //     dragAnchorThumbX = state.progressToScale(...) * length;
            //     dragMoveAnimValue = 0f;
            // }
            // if (dragMoveAnimator != null) dragMoveAnimator.cancel();   // <-- this
            // ```
            //
            // The `cancel()` matters: the running detent tween is expressed against the **old**
            // `dragMoveAnimStartX`, so letting it finish after the anchor moved produced a visible
            // jump. Cancelling also stops the `thumbX` updates that the still-running snap was
            // suppressing through the `snapDuration <= 0f` guard.
            if (anchorSection != currentSection) {
                anchorSection = currentSection
                anchorThumbX = sectionX(currentSection, length)
                dragMoveAnimValue = 0f
            }
            snapDuration = 0f
            dragSnapActive = false
            dragDirection = direction
        }

        val offset = fingerTrackX - anchorThumbX
        val bias = if (offset >= 0f) offset + PositionBias else offset - PositionBias
        val steps = (bias / width).toInt()
        val quantized = steps * width
        dragCurrentOffset = offset

        val candidate = (anchorSection + steps).coerceIn(minSection, max)
        if (candidate != currentSection) {
            currentSection = candidate
            committedSection = candidate
            // `startDragSnapAnimation(anchorThumbX, anchorThumbX + quantized, dragMoveAnimValue, length)`.
            startDragSnap(anchorThumbX + quantized, length)
            onCommit(candidate)
        } else if (snapDuration <= 0f) {
            thumbX = (anchorThumbX + quantized + (offset - quantized) * SectionResidualRatio)
                .coerceIn(0f, length)
        }
    }

    /**
     * `handleMotionEventUp` -> `handleFastMoveSnap` / `snapToNearestSection`.
     *
     * Returns the section to report if it differs from the last one already reported.
     */
    fun release(length: Float, minSection: Int): Int? {
        val width = sectionWidth(length)
        if (width <= 0f) return null
        val candidate = (thumbX / width).roundToInt() + minSection
        val target = candidate.coerceIn(minSection, max)
        val targetX = sectionX(target, length)
        if (abs(thumbX - targetX) < CouiSectionSeekBarDefaults.SnapPositionThreshold) {
            dragSnapActive = false
            snapDuration = 0f
            thumbX = targetX
            thumbXVelocity = 0f
        } else {
            startSnap(targetX, CouiSectionSeekBarDefaults.ReleaseSnapDurationMs)
        }
        currentSection = target
        return if (target != committedSection) {
            committedSection = target
            target
        } else {
            null
        }
    }

    /** Remembers what the caller was last told, so [release] does not repeat it. */
    fun markCommitted(section: Int) {
        committedSection = section
        currentSection = section
    }

    /** `setFastMoving`/`setLastFingerTrackX` - the drag loop owns these. */
    fun setLastFingerTrackX(value: Float) {
        lastFingerTrackX = value
    }

    /**
     * `setProgress` from outside - `snapToNearestSection` on the
     * `PROGRESS_TRANSITION_DURATION` (200ms) path.
     */
    fun transitionTo(value: Int, length: Float, minSection: Int) {
        if (length <= 0f || max <= min) return
        val target = value.coerceIn(minSection, max)
        val targetX = sectionX(target, length)
        currentSection = target
        committedSection = target
        startSnap(targetX, CouiSectionSeekBarDefaults.ProgressTransitionDurationMs)
        progressTransition = true
    }

    /** Tap path uses the same 200ms transition as setProgress(..., true, true). */
    fun tapTo(fingerTrackX: Float, length: Float, minSection: Int): Int? {
        val target = ((fingerTrackX / sectionWidth(length)).roundToInt() + minSection).coerceIn(minSection, max)
        if (target == currentSection) return null
        transitionTo(target, length, minSection)
        return target
    }

    /** `syncSectionThumbXFromProgress`: park on a section with no animation. */
    fun jumpTo(value: Int, travel: Float, minSection: Int) {
        if (travel <= 0f || max <= min) return
        currentSection = value.coerceIn(minSection, max)
        committedSection = currentSection
        thumbX = sectionX(currentSection, travel)
        thumbXVelocity = 0f
        dragSnapActive = false
        dragMoveAnimValue = 0f
        dragMoveAnimEndX = Float.NaN
        dragCurrentOffset = 0f
        snapDuration = 0f
        initialised = true
    }

    private fun startSnap(target: Float, durationMs: Int) {
        progressTransition = false
        dragSnapActive = false
        snapFrom = thumbX
        snapTo = target
        snapElapsed = 0f
        snapDuration = durationMs / 1000f
    }

    /**
     * `SectionAnimationHandler.startDragSnapAnimation(FFFF)`.
     *
     * The animator tweens `dragMoveAnimValue` from wherever it currently is to `endX` over
     * [CouiSectionSeekBarDefaults.DragSnapDurationMs], and its update listener recomputes **every
     * frame**:
     *
     * ```
     * sectionThumbX = clamp(dragMoveAnimStartX
     *                      + dragMoveAnimValue   * 0.4f    // the tweened detent travel
     *                      + dragCurrentOffset   * 0.6f,   // the *newest* finger residual
     *                      0f, length)
     * ```
     *
     * The second term is the whole point of the design: `trackTouchEvent` keeps refreshing
     * `dragCurrentOffset` from the live finger position, so the thumb keeps following the finger
     * *while* the detent animation runs. Tweening straight onto the detent - which this used to do -
     * pins the thumb for the full 100ms and is what made a held finger feel stuck.
     *
     * `startDragSnapAnimation` also bails out early in two cases, both reproduced here: the end
     * position is already reached, or the same end position is already being animated towards.
     */
    private fun startDragSnap(endX: Float, length: Float) {
        if (thumbX == endX) return
        if (snapDuration > 0f && dragSnapActive && dragMoveAnimEndX == endX) return
        dragMoveAnimStartX = anchorThumbX
        dragMoveAnimEndX = endX
        dragTrackLength = length
        dragSnapActive = true
        progressTransition = false
        snapFrom = dragMoveAnimValue
        // **`endX - startX`, not `endX`.** The smali builds the animator as
        // `ValueAnimator.ofFloat(p3, p2 - p1)` where `p1 = dragMoveAnimStartX` and
        // `p2 = dragMoveAnimEndX`, so the animated value runs up to the *quantised travel* - the
        // distance, not an absolute position. Tweening to the absolute `endX` made every section
        // crossing throw the thumb far to the right, which is the "it jumps around while sliding"
        // symptom.
        snapTo = endX - dragMoveAnimStartX
        snapElapsed = 0f
        snapDuration = CouiSectionSeekBarDefaults.DragSnapDurationMs / 1000f
    }

    fun advance(dt: Float): Boolean {
        if (!initialised) {
            thumbX = 0f
            initialised = true
        }
        if (dt <= 0f) return true
        var moving = false

        if (snapDuration > 0f) {
            snapElapsed += dt
            val t = (snapElapsed / snapDuration).coerceIn(0f, 1f)
            // `DRAG_SNAP_INTERPOLATOR` = PathInterpolatorCompat.create(0f, 0f, 0.25f, 1f).
            val eased = if (progressTransition) {
                // ValueAnimator's default AccelerateDecelerateInterpolator; no custom
                // interpolator is installed by startProgressTransitionAnim.
                ((1f - cos(PI.toFloat() * t)) / 2f)
            } else {
                cubicEase(t)
            }
            val animated = snapFrom + (snapTo - snapFrom) * eased
            if (dragSnapActive) {
                // The drag detent's update listener: the tweened travel contributes 0.4, and the
                // *live* finger residual contributes 0.6 - so the thumb keeps tracking the finger
                // for the whole 100ms instead of parking on the detent.
                dragMoveAnimValue = animated
                thumbX = (
                    dragMoveAnimStartX +
                        animated * CouiSectionSeekBarDefaults.DragMoveRatio +
                        dragCurrentOffset * SectionResidualRatio
                    )
                    .coerceIn(0f, dragTrackLength)
            } else {
                thumbX = animated
            }
            if (t >= 1f) {
                if (!dragSnapActive) {
                    thumbX = snapTo
                }
                dragSnapActive = false
                snapDuration = 0f
                thumbXVelocity = 0f
            } else {
                moving = true
            }
        }
        return moving
    }
}

/** `SectionAnimationHandler.DRAG_MOVE_RATIO` is 0.4; the in-section residual here is the smali's literal 0.6. */
private const val SectionResidualRatio = 0.6f

/** `COUISectionSeekBar.DRAG_DIRECTION_THRESHOLD`. */
private const val DirectionThreshold = 0.5f

/** `COUISectionSeekBar.POSITION_BIAS` - nudges the quantizer in the direction of travel. */
private const val PositionBias = 0.1f

/**
 * `PathInterpolatorCompat.create(0f, 0f, 0.25f, 1f)` - the ease-out the section snap uses.
 *
 * A cubic Bezier from (0,0) to (1,1) with control points (0,0) and (0.25,1). Evaluating it at a
 * time fraction needs the x-parameter solved first, so this bisects for it (24 steps is well below
 * a pixel of error at any duration this is used with).
 */
private fun cubicEase(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    var low = 0f
    var high = 1f
    repeat(24) {
        val s = (low + high) / 2f
        if (bezierAxis(s, 0f, 0.25f) < x) low = s else high = s
    }
    return bezierAxis((low + high) / 2f, 0f, 1f)
}

/** One axis of a cubic Bezier with the endpoints pinned to 0 and 1. */
private fun bezierAxis(s: Float, control1: Float, control2: Float): Float {
    val u = 1f - s
    return 3f * u * u * s * control1 + 3f * u * s * s * control2 + s * s * s
}
