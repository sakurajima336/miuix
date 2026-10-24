// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Defaults for [CouiScrollBar].
 *
 * The visible thumb is a capsule inset inside a 14dp-wide track box: **3dp** wide at rest and
 * **6dp** wide while pressed or dragged. A drag only starts when the bar is currently shown and
 * the touch lands inside the thumb rect; while dragging, the content chases the thumb with an
 * *animated* scroll instead of jumping, so it keeps easing to its final position after the
 * finger lifts. Any scroll restarts the 2000 ms fade.
 *
 * A popup-style host can pass its own vertical padding (12dp) as [CouiScrollBar.marginTop] /
 * [CouiScrollBar.marginBottom].
 */
object CouiScrollBarDefaults {
    /** The width of the track box containing the thumb. */
    val Width: Dp = 14.dp

    /** The minimum thumb length. */
    val MinHeight: Dp = 48.dp

    /** Distance from the host's top edge to the thumb track. */
    val MarginTop: Dp = 8.dp

    /** Distance from the host's bottom edge to the thumb track. */
    val MarginBottom: Dp = 16.dp

    /** Horizontal inset of the capsule at rest (14dp track - 2 x 5.5dp = 3dp thumb). */
    val InsetDefault: Dp = 5.5.dp

    /** Horizontal inset of the capsule while pressed (14dp track - 2 x 4dp = 6dp thumb). */
    val InsetPressed: Dp = 4.dp

    /** How long the bar stays shown after the last scroll before fading out. */
    val FadeTimeoutMillis: Long = 0x7d0L

    /** Thumb colour in the light theme. */
    val ThumbColorLight: Color = Color(0x29000000)

    /** Thumb colour in the dark theme. */
    val ThumbColorDark: Color = Color(0x40FFFFFF)
}

/**
 * @param state the scrollable state to mirror and to drive while the thumb is dragged.
 * @param marginTop distance from the top of the host to the top of the thumb track.
 * @param marginBottom distance from the bottom of the host to the bottom of the thumb track.
 * @param dynamicHeight when false the thumb keeps [CouiScrollBarDefaults.MinHeight].
 */
@Composable
fun CouiScrollBar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    color: Color = if (dark) CouiScrollBarDefaults.ThumbColorDark else CouiScrollBarDefaults.ThumbColorLight,
    marginTop: Dp = CouiScrollBarDefaults.MarginTop,
    marginBottom: Dp = CouiScrollBarDefaults.MarginBottom,
    dynamicHeight: Boolean = true,
) {
    val range = state.maxValue
    if (range <= 0) return
    val alpha = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragThumbTop by remember { mutableFloatStateOf(0f) }
    var dragTarget by remember { mutableStateOf<Int?>(null) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // splineBasedDecay is the same fling friction the platform uses for scrollables.
    val decay = remember(density) { splineBasedDecay<Float>(density) }

    // The list is driven with an *animated* scroll (not a raw jump), re-issued as the thumb
    // moves: the content chases the thumb while the finger is down; the release below hands the
    // remaining velocity to a fling so a quick drag keeps going.
    LaunchedEffect(dragTarget, dragging) {
        if (!dragging) return@LaunchedEffect
        val target = dragTarget ?: return@LaunchedEffect
        state.animateScrollTo(target)
    }

    // Any scroll restarts the fade timeout; a drag keeps the bar fully shown.
    LaunchedEffect(state.value, range, dragging) {
        alpha.snapTo(1f)
        if (!dragging) {
            delay(CouiScrollBarDefaults.FadeTimeoutMillis)
            alpha.animateTo(0f)
        }
    }

    BoxWithConstraints(
        modifier
            .width(CouiScrollBarDefaults.Width)
            .fillMaxHeight()
            .padding(top = marginTop, bottom = marginBottom),
    ) {
        val widthPx = with(density) { CouiScrollBarDefaults.Width.toPx() }
        val trackHeight = constraints.maxHeight.toFloat()
        val extent = (state.viewportSize.takeIf { it > 0 } ?: trackHeight.toInt()).coerceAtLeast(1)
        val ratio = (extent.toFloat() / (extent + range)).coerceIn(0f, 1f)
        val minHeightPx = with(density) { CouiScrollBarDefaults.MinHeight.toPx() }
        // The thumb height tracks the visible fraction of the content, clamped to the minimum
        // thumb length.
        val thumbHeight = if (dynamicHeight) (ratio * trackHeight).coerceAtLeast(minHeightPx) else minHeightPx
        val usable = (trackHeight - thumbHeight).coerceAtLeast(0f)
        val insetDefault = with(density) { CouiScrollBarDefaults.InsetDefault.toPx() }
        val insetPressed = with(density) { CouiScrollBarDefaults.InsetPressed.toPx() }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(range, trackHeight, thumbHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // A drag only starts when the bar is visible and the down point is
                        // inside the thumb rect.
                        if (alpha.value <= 0.01f) return@awaitEachGesture
                        val top = usable * (state.value.toFloat() / range)
                        val inside = down.position.x >= size.width - widthPx &&
                            down.position.y >= top && down.position.y <= top + thumbHeight
                        if (!inside) return@awaitEachGesture
                        dragging = true
                        pressed = true
                        down.consume()
                        flingJob?.cancel()
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var lastY = down.position.y
                        dragThumbTop = usable * (state.value.toFloat() / range)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val delta = change.position.y - lastY
                            if (delta != 0f) {
                                // The thumb moves immediately and the list chases it with the
                                // animated scroll, so both the thumb position and the animated
                                // target are tracked here (dispatchRawDelta would jump instead).
                                dragThumbTop = (dragThumbTop + delta).coerceIn(0f, usable)
                                if (usable > 0f) dragTarget = ((dragThumbTop / usable) * range).roundToInt()
                                lastY = change.position.y
                            }
                            change.consume()
                        }
                        dragging = false
                        pressed = false
                        // A quick drag keeps the content moving after the finger lifts, as a
                        // flick on the content itself would. The bar's velocity is in thumb px/s
                        // and the content travels (range / usable) times that distance for the
                        // same thumb movement, so the fling velocity is scaled the same way.
                        val barVelocity = tracker.calculateVelocity().y
                        // ViewConfiguration.getScaledMinimumFlingVelocity() == 50dp/s.
                        if (usable > 0f && abs(barVelocity) > 50f * density.density) {
                            val contentVelocity = barVelocity / usable * range
                            flingJob = scope.launch {
                                var last = 0f
                                AnimationState(0f, contentVelocity).animateDecay(decay) {
                                    val step = value - last
                                    last = value
                                    state.dispatchRawDelta(step)
                                }
                            }
                        }
                    }
                }
                .drawBehind {
                    // While dragging the thumb follows the finger immediately; the list chases
                    // it with the animated scroll.
                    val top = if (dragging) dragThumbTop else usable * (state.value.toFloat() / range)
                    val inset = if (pressed) insetPressed else insetDefault
                    val left = size.width - widthPx + inset
                    val right = size.width - inset
                    if (right <= left) return@drawBehind
                    val thumbTop = top + inset
                    val thumbBottom = (top + thumbHeight - inset).coerceAtLeast(thumbTop)
                    drawRoundRect(
                        color = color.copy(alpha = color.alpha * alpha.value),
                        topLeft = Offset(left, thumbTop),
                        size = Size(right - left, thumbBottom - thumbTop),
                        cornerRadius = CornerRadius((right - left) / 2f, (right - left) / 2f),
                    )
                },
        )
    }
}
