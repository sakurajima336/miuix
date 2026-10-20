// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.component

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Animatable
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
 * COUIScrollBar: an independent scrollbar drawn over the content, not the platform fast scroller.
 *
 * Geometry and behaviour read from `com.coui.appcompat.scrollbar.COUIScrollBar` (+ its `Builder`):
 *
 * - `Builder`: width = `coui_scrollbar_wight` 14dp (the *track box*), height (the minimum thumb
 *   length) = `coui_scrollbar_min_height` 48dp, margins default `coui_scrollbar_margin_top` 8dp /
 *   `coui_scrollbar_margin_bottom` 16dp, `isDynamicHeight` defaults to true.
 * - `makeDefaultThumbDrawable`: the visible thumb is a `GradientDrawable` capsule
 *   (`cornerRadius = width / 2`) wrapped in an `InsetDrawable(left = right = 5.5dp default /
 *   4dp pressed, top = marginTop, bottom = marginBottom)` inside a `StateListDrawable` with a
 *   pressed state. So at rest the capsule is 14 - 2*5.5 = **3dp** wide and it widens to
 *   14 - 2*4 = **6dp** while pressed/dragged. Normal and pressed share `coui_scrollbar_color`.
 * - `updateThumbRect`: `thumbHeight = max(minHeight, round(extent / range * viewHeight))`,
 *   `top = round((viewHeight - thumbHeight) * offset / (range - extent))`.
 * - `onTouchEventInternal`: the drag only starts when the bar is currently shown and the down point
 *   is inside the thumb rect; the list's own gesture is cancelled, the thumb switches to the pressed
 *   drawable, the fade callback is removed, and moving by `delta` shifts the thumb by `delta`
 *   (clamped to the track), scrolling the content accordingly. UP/CANCEL releases the pressed state
 *   and restarts the fade (`awakenScrollBars`).
 * - `updateThumbRect(int delta)` (drag): `newTop = clamp(oldTop + delta, 0, usable)`,
 *   `fraction = newTop / usable`, `target = round(available * fraction)`, and then
 *   `((AbsListView) view).smoothScrollBy(target - currentOffset, 0)` - i.e. the list *animates* to
 *   the new position instead of jumping, and because every MOVE event re-issues it, the content
 *   chases the thumb and keeps easing to its final position after the finger lifts.
 * - Fade timeout: `SCROLLER_FADE_TIMEOUT = 0x7d0` (2000 ms).
 *
 * The popup menu passes its own vertical padding (`coui_popup_list_window_scrollbar_vertical_padding`
 * = 12dp) as [marginTop]/[marginBottom] (`COUITouchListView.createCOUIScrollDelegate`).
 */
object CouiScrollBarDefaults {
    val Width: Dp = 14.dp
    val MinHeight: Dp = 48.dp
    val MarginTop: Dp = 8.dp
    val MarginBottom: Dp = 16.dp
    val InsetDefault: Dp = 5.5.dp
    val InsetPressed: Dp = 4.dp
    val FadeTimeoutMillis: Long = 0x7d0L

    /** color/coui_scrollbar_color: light #29000000, night #40ffffff. */
    val ThumbColorLight: Color = Color(0x29000000)
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

    // COUIScrollBar.updateThumbRect(int delta) drives the list with
    // `((AbsListView) view).smoothScrollBy(target - currentOffset, 0)`: an *animated* scroll that is
    // re-issued on every MOVE event. The content therefore chases the thumb while the finger is
    // down; the release below hands the remaining velocity to a fling so a quick drag keeps going.
    LaunchedEffect(dragTarget, dragging) {
        if (!dragging) return@LaunchedEffect
        val target = dragTarget ?: return@LaunchedEffect
        state.animateScrollTo(target)
    }

    // awakenScrollBars: any scroll restarts the 2000 ms fade; a drag keeps the bar fully shown.
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
        // COUIScrollBar.updateThumbRect: height = max(minHeight, extent/range * viewHeight).
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
                        // The official only starts a drag when the bar is visible and the down point
                        // is inside the thumb rect (COUIScrollBar.onTouchEventInternal).
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
                                // The official moves the thumb rect immediately and lets the list
                                // chase it with smoothScrollBy, so both the thumb and the animated
                                // target are tracked here (dispatchRawDelta would jump instead).
                                dragThumbTop = (dragThumbTop + delta).coerceIn(0f, usable)
                                if (usable > 0f) dragTarget = ((dragThumbTop / usable) * range).roundToInt()
                                lastY = change.position.y
                            }
                            change.consume()
                        }
                        dragging = false
                        pressed = false
                        // A quick drag keeps the content moving after the finger lifts: someone who
                        // throws the thumb expects the list to carry on, which is what the official
                        // list does for a finger flick. The bar's velocity is in thumb px/s and the
                        // content travels (range / usable) times that distance for the same thumb
                        // movement, so the fling velocity is scaled the same way.
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
                    // While dragging the thumb follows the finger immediately (the official moves the
                    // thumb rect in updateThumbRect); the list chases it with the animated scroll.
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