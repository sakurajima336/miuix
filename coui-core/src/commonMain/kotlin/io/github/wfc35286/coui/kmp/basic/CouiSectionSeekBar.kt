// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:property-naming") // Preserve public COUI defaults API names.

package io.github.wfc35286.coui.kmp.basic
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
import io.github.wfc35286.coui.kmp.graphics.CouiThumbRenderer
import io.github.wfc35286.coui.kmp.graphics.drawCouiTexturedThumb
import io.github.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.github.wfc35286.coui.kmp.motion.rememberCouiParentIntercept
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/**
 * Defaults for [CouiSectionSeekBar].
 *
 * The bar presents a bounded range of discrete sections, with a mark drawn at every section
 * boundary and a thumb that detents between them.
 */
object CouiSectionSeekBarDefaults {
    /** The radius of each section mark. A density-scaled dimension, not a raw pixel value. */
    val MarkRadius: Dp = 3.dp

    /**
     * While dragging, the thumb travels only this fraction of the finger's movement. That damping
     * is what makes the bar feel "detented": the thumb lags, then flips to the next section.
     */
    const val DragMoveRatio: Float = 0.4f

    /** Moving half a section's width flips the thumb to the neighbouring section. */
    const val SnapPositionThreshold: Float = 0.5f

    /** The duration of the flip animation while dragging. */
    const val DragSnapDurationMs: Int = 100

    /** The duration of the settle animation on lift. */
    const val ReleaseSnapDurationMs: Int = 150

    /** The duration of the animation when the value is changed programmatically. */
    const val ProgressTransitionDurationMs: Int = 200

    /** The window over which the drag velocity is measured. */
    const val VelocityComputeTimeMs: Int = 100

    /** How far the finger must move before the drag direction is considered flipped. */
    const val DragDirectionThreshold: Float = 0.5f

    /**
     * Nudges the section quantiser when the finger sits outside the track, so the outermost
     * section still wins.
     */
    const val PositionBias: Float = 0.1f

    /** The track colour. */
    @Composable
    fun trackColor(): Color = CouiTheme.componentColors.seekBarTrack

    /** The colour of the filled part of the track. */
    @Composable
    fun progressColor(): Color = CouiTheme.colors.primary

    /**
     * The colour of the marks **left of** the thumb (the filled side): white when enabled, a
     * dimmer white when disabled.
     */
    @Composable
    fun activeMarkColor(enabled: Boolean = true): Color = when {
        enabled -> Color(0xFFFFFFFF)
        isSystemInDarkTheme() -> Color(0x29FFFFFF)
        else -> Color(0x8AFFFFFF)
    }

    /**
     * The colour of the marks **right of** the thumb (the unfilled side): translucent black in the
     * light theme, translucent white in the dark theme, dimmer still when disabled.
     */
    @Composable
    fun inactiveMarkColor(enabled: Boolean = true): Color = when {
        enabled && isSystemInDarkTheme() -> Color(0x4DFFFFFF)
        enabled -> Color(0x42000000)
        isSystemInDarkTheme() -> Color(0x29FFFFFF)
        else -> Color(0x42000000)
    }

    /** The thumb colour. */
    @Composable
    fun thumbColor(): Color = CouiTheme.componentColors.seekBarThumb
}

/**
 * A COUI **sectioned** seek bar with discrete detents.
 *
 * It is not a plain slider: every step is a detent. A mark is drawn at every section, skipped when
 * it is hidden under the thumb - marks on the filled side of the thumb use the active colour, the
 * others the inactive one. While dragging, the thumb follows the finger across whole sections with
 * a damped residual inside the current one; crossing a section runs a 100ms snap animation with
 * haptic feedback. A tap snaps the thumb onto the nearest section with a 200ms progress
 * transition, and lifting settles onto the nearest section over 150ms.
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
    // Thumb shadow dimensions, shared with the plain bar's defaults.
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
    // The section bar ticks once per detent, both when a tap snaps the thumb onto a section and
    // when a drag crosses one.
    val haptics = rememberCouiHaptics()
    // Keeps the enclosing scroll container from stealing a drag.
    val intercept = rememberCouiParentIntercept()
    var barWidth by remember { mutableIntStateOf(0) }
    var barHeight by remember { mutableIntStateOf(0) }
    // Monotonic wake signal: pointer events never restart the running frame loop.
    var frameWake by remember { mutableIntStateOf(0) }
    // The last section this component reported upwards. `LaunchedEffect(value)` must not treat its
    // own report as an external `setProgress`.
    var reportedValue by remember { mutableIntStateOf(value) }

    // A value change **from outside** goes through the 200ms transition animation.
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

    // Once the width is known, park the thumb on the caller's section without animating.
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
                                    // A tap uses the same 200ms progress transition as a
                                    // programmatic value change.
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
                                // The first move only anchors the drag and returns; no snap tween yet.
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
            // While the swell is in flight - neither at the rest height nor at the max - the drawn
            // background height is rounded to a **whole pixel** (the spring itself keeps the
            // float), and the radius is derived from the rounded height. Drawing the raw float
            // puts the track's top and bottom edges on sub-pixel positions every frame, so the
            // capsule shimmers and its centre drifts during the grow/shrink; the pixel snap makes
            // it grow in clean one-pixel steps.
            val midEnlargeAnim = rawBackgroundHeight != trackHeight && rawBackgroundHeight != maxBackgroundHeight
            val curBackgroundHeight =
                if (midEnlargeAnim) rawBackgroundHeight.roundToInt().toFloat() else rawBackgroundHeight
            val backgroundRadius = curBackgroundHeight / 2f
            val backgroundInset = paddingHorizontal - curBackgroundHeight / 2f
            val backgroundLeft = backgroundInset
            val backgroundRight = size.width - backgroundInset
            val backgroundTop = centreY - backgroundRadius

            // The section bar draws its track as a plain capsule.
            val trackSize = Size(backgroundRight - backgroundLeft, curBackgroundHeight)
            translate(backgroundLeft, backgroundTop) {
                // A plain round rect: capsule samplers degenerate when the radius equals half the
                // height.
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset.Zero,
                    size = trackSize,
                    cornerRadius = CornerRadius(backgroundRadius, backgroundRadius),
                )
            }

            val thumbRadius = thumbRadiusRest + (thumbRadiusPressed - thumbRadiusRest) * press.thumbScale
            // `section.thumbX` is *travel-local*: it runs 0..travel, where `travel` excludes the
            // `paddingHorizontal` inset on both sides. The thumb, the progress rect and the marks
            // are drawn in **view-local** space, so the inset has to be added back here - without
            // it the thumb lands `paddingHorizontal` to the left of its own mark.
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

            // One circle per section, skipping the one hidden by the thumb; the filled side uses
            // the active colour.
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

/** Draws one mark per section, active colour on the filled side of the thumb. */
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
    // Marks on the filled side of the thumb use the active colour, the rest the inactive one; in
    // RTL the filled side is on the right.
    for (step in min..max) {
        val fraction = (step - min) / span
        val x = if (rtl) canvasWidth - travelStart - travelLength * fraction else travelStart + travelLength * fraction
        val onFilledSide = if (rtl) x > thumbX else x < thumbX
        val color = if (onFilledSide) activeColor else inactiveColor
        // Skip the mark when it sits entirely under the thumb.
        if (x - markRadius > thumbX - thumbRadius &&
            x + markRadius < thumbX + thumbRadius
        ) {
            continue
        }
        drawCircle(color = color, radius = markRadius, center = Offset(x, centreY))
    }
}

/**
 * The detent state machine: anchors a drag on a section, quantises the finger's travel to whole
 * sections, and animates the snaps between them.
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

    // Drag state fields.
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

    /** The pixel X of a section within the track. */
    fun sectionX(section: Int, length: Float): Float {
        val width = sectionWidth(length)
        val raw = width * (section - min)
        return raw
    }

    /**
     * Maps a view-local x to the finger's **absolute** position on the track, coerced into
     * `0..length`.
     *
     * Note this is absolute, not a delta from where the finger went down - the whole drag maths
     * below is expressed in that space.
     */
    fun fingerTrackX(x: Float, start: Float, length: Float, rtl: Boolean): Float {
        val raw = if (rtl) length - (x - start) else x - start
        return raw.coerceIn(0f, length)
    }

    /**
     * Anchors a drag on the section nearest the finger's down position.
     *
     * Two details matter:
     *
     * 1. The anchor is the **section's** pixel position, not the finger's down position. The
     *    quantiser in [dragMove] measures `offset = fingerTrackX - anchorThumbX` from the detent,
     *    so anchoring on the finger would shift every subsequent step by however far from the
     *    detent the finger landed.
     * 2. The drag direction is seeded here from the finger's position relative to the anchor, so
     *    the very first move already has a direction instead of waiting for the direction
     *    threshold.
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
        // A fresh gesture must not inherit the previous lift's 150ms settle: while that snap is
        // still marked as running, `dragMove`'s follow-the-finger branch (`snapDuration <= 0f`) is
        // skipped, so the first moments of a new drag would not track the finger.
        snapDuration = 0f
        lastFingerTrackX = fingerTrackX
        // The thumb jumps straight to the anchor.
        thumbX = this.anchorThumbX
        thumbXVelocity = 0f
        currentSection = anchorSection
        committedSection = anchorSection
        return if (anchorSection != previous) anchorSection else null
    }

    /**
     * One move event of a drag.
     *
     * The thumb is `anchor + wholeSections + residual * 0.6f`: whole sections are quantised (with
     * [PositionBias] nudging in the direction of travel), and only the leftover inside the current
     * section is damped.
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
            // Direction reversal: re-anchor on the current section and abandon the running detent
            // tween.
            //
            // Abandoning it matters: the running tween is expressed against the **old** start
            // position, so letting it finish after the anchor moved produces a visible jump.
            // Stopping it also unblocks the `thumbX` updates that the still-running snap was
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
            // Snap onto the newly reached section.
            startDragSnap(anchorThumbX + quantized, length)
            onCommit(candidate)
        } else if (snapDuration <= 0f) {
            thumbX = (anchorThumbX + quantized + (offset - quantized) * SectionResidualRatio)
                .coerceIn(0f, length)
        }
    }

    /**
     * Settles onto the nearest section when the finger lifts.
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

    /** Records the finger's latest track position; the drag loop owns this. */
    fun setLastFingerTrackX(value: Float) {
        lastFingerTrackX = value
    }

    /**
     * A programmatic value change: animates onto the target section over
     * [CouiSectionSeekBarDefaults.ProgressTransitionDurationMs].
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

    /** A tap uses the same 200ms transition as a programmatic value change. */
    fun tapTo(fingerTrackX: Float, length: Float, minSection: Int): Int? {
        val target = ((fingerTrackX / sectionWidth(length)).roundToInt() + minSection).coerceIn(minSection, max)
        if (target == currentSection) return null
        transitionTo(target, length, minSection)
        return target
    }

    /** Parks the thumb on a section with no animation. */
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
     * Starts the detent snap after the thumb crosses into a new section.
     *
     * The animation tweens `dragMoveAnimValue` from wherever it currently is to the quantised
     * travel over [CouiSectionSeekBarDefaults.DragSnapDurationMs], and the frame update recomputes
     * **every frame**:
     *
     * ```
     * thumbX = clamp(dragMoveAnimStartX
     *              + dragMoveAnimValue   * 0.4f    // the tweened detent travel
     *              + dragCurrentOffset   * 0.6f,   // the *newest* finger residual
     *              0f, length)
     * ```
     *
     * The second term is the whole point of the design: [dragMove] keeps refreshing
     * `dragCurrentOffset` from the live finger position, so the thumb keeps following the finger
     * *while* the detent animation runs. Tweening straight onto the detent would pin the thumb for
     * the full 100ms and make a held finger feel stuck.
     *
     * Bails out early in two cases: the end position is already reached, or the same end position
     * is already being animated towards.
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
        // The animated value is the **quantised travel** (`endX - startX`), not an absolute
        // position: the frame update adds it to `dragMoveAnimStartX`. Tweening to the absolute
        // `endX` would throw the thumb far to the right on every section crossing.
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
            // The drag snap uses a cubic ease-out (see [cubicEase]).
            val eased = if (progressTransition) {
                // The progress transition uses the default accelerate/decelerate ease.
                ((1f - cos(PI.toFloat() * t)) / 2f)
            } else {
                cubicEase(t)
            }
            val animated = snapFrom + (snapTo - snapFrom) * eased
            if (dragSnapActive) {
                // The tweened travel contributes 0.4 and the *live* finger residual 0.6, so the
                // thumb keeps tracking the finger for the whole snap instead of parking on the
                // detent.
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

/** The in-section residual applied on top of the detented travel; the rest comes from [CouiSectionSeekBarDefaults.DragMoveRatio]. */
private const val SectionResidualRatio = 0.6f

/** How far the finger must move between events for the drag direction to flip. */
private const val DirectionThreshold = 0.5f

/** Nudges the section quantiser in the direction of travel. */
private const val PositionBias = 0.1f

/**
 * The ease-out the drag detent snap uses: a cubic Bezier from (0,0) to (1,1) with control points
 * (0,0) and (0.25,1).
 *
 * Evaluating it at a time fraction needs the x-parameter solved first, so this bisects for it
 * (24 steps is well below a pixel of error at any duration this is used with).
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
