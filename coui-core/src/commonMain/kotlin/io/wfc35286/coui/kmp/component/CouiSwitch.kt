// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.motion.CouiComponentSprings
import io.wfc35286.coui.kmp.motion.CouiMinVisibleChange
import io.wfc35286.coui.kmp.motion.couiSwitchThumbStretch
import io.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.wfc35286.coui.kmp.motion.toSpringSpec
import io.wfc35286.coui.kmp.shape.couiCapsulePath
import io.wfc35286.coui.kmp.shape.createCouiCapsuleShader
import io.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Defaults for [CouiSwitch].
 *
 * The geometry is what `COUISwitch.onMeasure` computes: it extends `SwitchCompat` and sizes
 * itself as `switchMinWidth x barHeight` plus `2 * coui_switch_padding`, where
 * `coui_switch_padding` is 0dp and the other two come from `COUISwitchStyle`
 * (`@dimen/bar_width` = 44dp, `@dimen/bar_height` = 24dp).
 */
object CouiSwitchDefaults {
    /** `bar_width` - the full track width. */
    val Width: Dp = 44.dp

    /** `bar_height` - the full track height. */
    val Height: Dp = 24.dp

    /** `outer_circle_width` - the thumb diameter. */
    val ThumbSize: Dp = 18.dp

    /** `circle_padding` - the gap between the thumb and the track edge. */
    val ThumbPadding: Dp = 3.dp

    /**
     * `COUISwitchParams.toggleSpring` - `(bounce 0.3, response 0.4)` - with the minimum visible
     * change of 0.1 that `COUISwitch.animateWhenStateChanged` passes.
     */
    val ToggleSpec: SpringSpec<Float> = CouiComponentSprings.SwitchToggle
        .toSpringSpec(CouiComponentSprings.SwitchToggleMinVisibleChange)

    /**
     * The toggle spring with its minimum visible change converted to a fraction of the travel.
     *
     * `COUISwitch.initToggleSpringAnim` calls `setMinimumVisibleChange(0.1f)` on an animator whose
     * value is `circleTranslation` **in pixels**, so 0.1 means a tenth of a pixel. Our animated
     * quantity is a normalised `0..1` fraction, so the threshold has to be divided by the travel
     * in pixels. Passing 0.1 straight through tells Compose to stop once it is within 10% of the
     * target, which cuts the spring's overshoot short - and with it the thumb-stretch deformation
     * that only happens while the spring is in flight.
     *
     * @param travelPx The thumb's travel in pixels (`switchMinWidth - 2 * circlePadding -
     *   outerCircleWidth`, i.e. `mDefaultTranslation`).
     */
    fun toggleSpec(travelPx: Float): SpringSpec<Float> = CouiComponentSprings.SwitchToggle
        .toSpringSpec(CouiComponentSprings.SwitchToggleMinVisibleChange / travelPx.coerceAtLeast(1f))

    /**
     * The drag spring, with the same pixel-to-fraction conversion applied.
     *
     * `COUISpringDragHelper` leaves the minimum visible change at `COUIDynamicAnimation`'s default
     * of `MIN_VISIBLE_CHANGE_PIXELS` (1.0px), again on a pixel-valued animator.
     *
     * @param travelPx The thumb's travel in pixels.
     */
    fun dragSpec(travelPx: Float): SpringSpec<Float> = CouiComponentSprings.SwitchDrag
        .toSpringSpec(CouiMinVisibleChange.Pixels / travelPx.coerceAtLeast(1f))

    /**
     * The spring a drag release snaps to its anchor with, converted from pixels to a fraction.
     *
     * `COUISpringDragHelper.dragTo(x, y, isAttach = true)` swaps in `responseAttach` /
     * `bounceAttach` (0.4 / 0.2) before animating to the anchor. It is bouncier than
     * [dragSpec], which is where the release overshoot comes from - but the overshoot is
     * rendered **without** deformation, because the drag callback never writes `mCircleScaleX`.
     *
     * @param travelPx The thumb's travel in pixels.
     */
    fun attachSpec(travelPx: Float): SpringSpec<Float> = CouiComponentSprings.SwitchAttach
        .toSpringSpec(CouiMinVisibleChange.Pixels / travelPx.coerceAtLeast(1f))

    /**
     * The track colour at one end of the travel.
     *
     * On and off come from different places in the real implementation: the "on" colour is the
     * theme's `couiColorPrimary`, while the "off" colours are the switch's own resources
     * (`switch_unchecked_bar_color` and `switch_unchecked_inner_circle_disabled_color`).
     */
    @Composable
    fun trackColorAt(
        checked: Boolean,
        enabled: Boolean = true,
    ): Color {
        val colors = CouiTheme.colors
        val componentColors = CouiTheme.componentColors
        return when {
            !enabled && checked -> colors.primary.copy(alpha = 0.3f)
            !enabled -> componentColors.switchTrackOffDisabled
            checked -> colors.primary
            else -> componentColors.switchTrackOff
        }
    }

    /**
     * The track colour mid-animation.
     *
     * This is `ColorUtils.blendARGB(mBarUnCheckedColor, mBarCheckedColor, fraction)` from
     * `COUISwitch$2.setValue` - the track **cross-fades with the thumb's spring**, it does not
     * snap when the state flips. `setBarColor` itself is only a field write plus `invalidate()`,
     * so the interpolation has to happen in the animator's callback, which is exactly where the
     * original puts it.
     *
     * @param fraction The normalised travel, clamped to `0..1`. The original clamps for the
     *   colour and the inner-circle alpha even though it does *not* clamp for the stretch.
     */
    @Composable
    fun trackColor(
        fraction: Float,
        enabled: Boolean = true,
    ): Color = lerp(
        start = trackColorAt(checked = false, enabled = enabled),
        stop = trackColorAt(checked = true, enabled = enabled),
        fraction = fraction.coerceIn(0f, 1f),
    )

    /**
     * The thumb colour for the given state.
     *
     * `switch_outer_circle_color` / `switch_outer_circle_disable_color`.
     */
    @Composable
    fun thumbColor(
        checked: Boolean,
        enabled: Boolean = true,
    ): Color {
        val componentColors = CouiTheme.componentColors
        return if (enabled) componentColors.switchThumb else componentColors.switchThumbDisabled
    }
}

/**
 * A COUI switch.
 *
 * The thumb is a circle at both ends of the track and stretches into a capsule while travelling,
 * peaking at 1.3x at 34.7% of the travel. See [couiSwitchThumbStretch] for why that curve is
 * piecewise linear rather than the sinusoid it looks like.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Called with the new state when the user taps the switch.
 * @param modifier The modifier to apply to the switch.
 * @param enabled Whether the switch can be interacted with.
 */
@Composable
fun CouiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fraction = remember { Animatable(if (checked) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberCouiHaptics()
    var dragging by remember { mutableStateOf(false) }
    var trackWidth by remember { mutableIntStateOf(0) }
    var trackSize by remember { mutableStateOf(Size.Zero) }
    val capsuleShader = remember { createCouiCapsuleShader() }
    val trackPath = remember(trackSize) { couiCapsulePath(trackSize) }

    // Gesture closures capture whatever was in scope when their pointerInput block launched, and
    // that block only relaunches when its keys change - `checked` is not one of them. Without
    // this, a tap after a drag would call `onCheckedChange` with a stale value and the switch
    // would appear dead until the row was tapped instead.
    val currentChecked by rememberUpdatedState(checked)

    // The attach-animation flag, mirroring `mIsAttachAnimRunning`. While a drag release is
    // snapping to its anchor the stretch must stay off: `COUISwitch$3.onSizeChange`, the drag
    // callback, never touches `setCircleScaleX`, so the official release shows **no** trailing
    // deformation even though the thumb does overshoot.
    var attachAnimating by remember { mutableStateOf(false) }

    val thumbPx = with(LocalDensity.current) { CouiSwitchDefaults.ThumbSize.toPx() }
    val paddingPx = with(LocalDensity.current) { CouiSwitchDefaults.ThumbPadding.toPx() }

    // `mDefaultTranslation`: how far the thumb centre can travel, in pixels. The real class
    // computes it as `max(switchMinWidth - circlePadding - outerCircleWidth, 0)`, which for the
    // 44x24dp track is the same span the drag handler uses. Both springs' minimum visible changes
    // are expressed in these pixels, so everything else is derived from it.
    val travelPx = (trackWidth - 2 * paddingPx - thumbPx).coerceAtLeast(1f)

    // The single animation driver: every path (tap, external change, drag release) ends up here,
    // always targeting `checked`. The drag release selects the bouncier attach spring via
    // `attachAnimating`; a second coroutine would race this one and could leave the thumb
    // stranded away from `checked` (observed as "switch stuck off until the row was tapped").
    LaunchedEffect(checked, dragging, travelPx) {
        if (!dragging) {
            fraction.animateTo(
                targetValue = if (checked) 1f else 0f,
                animationSpec = if (attachAnimating) {
                    CouiSwitchDefaults.attachSpec(travelPx)
                } else {
                    CouiSwitchDefaults.toggleSpec(travelPx)
                },
            )
            attachAnimating = false
        }
    }

    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val travel = if (rtl) 1f - fraction.value else fraction.value
    val stretch = if (dragging || attachAnimating || !fraction.isRunning) {
        1f
    } else {
        couiSwitchThumbStretch(travel)
    }
    val trackColor = CouiSwitchDefaults.trackColor(travel, enabled)
    val thumbColor = CouiSwitchDefaults.thumbColor(checked, enabled)

    Canvas(
        modifier = modifier
            .size(CouiSwitchDefaults.Width, CouiSwitchDefaults.Height)
            .onSizeChanged {
                trackWidth = it.width
                trackSize = Size(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(enabled, trackWidth, rtl) {
                if (!enabled || trackWidth <= 0) return@pointerInput
                val startX = paddingPx + thumbPx / 2f
                val endX = trackWidth - paddingPx - thumbPx / 2f
                val span = (endX - startX).coerceAtLeast(1f)
                var downX = 0f
                fun targetAt(x: Float): Float {
                    val f = ((x - startX) / span).coerceIn(0f, 1f)
                    return if (rtl) 1f - f else f
                }
                val travelPx = (trackWidth - 2 * paddingPx - thumbPx).coerceAtLeast(1f)
                detectHorizontalDragGestures(
                    onDragStart = {
                        downX = it.x
                        dragging = true
                    },
                    onDragEnd = {
                        // `handleActionUp` + `springDrag(UP)`: snap to the nearest anchor with the
                        // attach spring (0.4 / 0.2) and no deformation. The LaunchedEffect below is
                        // the single animation driver; setting `attachAnimating` here just swaps
                        // the spring it uses. The state flips immediately (`onCheckedChange`),
                        // matching `setChecked(fromUser = true)` in the original.
                        dragging = false
                        attachAnimating = true
                        haptics.granularShort()
                        onCheckedChange(fraction.value > 0.5f)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        // `COUISpringDragHelper.dragTo` ignores movement inside a 4px dead zone.
                        if (abs(change.position.x - downX) >= CouiComponentSprings.SwitchTransformDistance) {
                            val target = targetAt(change.position.x)
                            scope.launch {
                                fraction.animateTo(target, CouiSwitchDefaults.dragSpec(travelPx))
                            }
                        }
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = {
                    // `handleClickToggle` → setChecked(fromUser = true) → performFeedBack().
                    haptics.granularShort()
                    onCheckedChange(!currentChecked)
                })
            },
    ) {
        // COUISwitch.initOutLine -> COUISwitch$1 -> OplusOutlineAdapter(style=1).
        // On C17's material branch the GradientDrawable has radius=0; the RenderNode's
        // full-round G2 outline shapes the track. Its weight=1 selects libhwui sdCapsule.
        // See COUI-Switch圆角调用链.md for smali and hardware-rendered comparisons.
        if (capsuleShader != null) {
            drawRect(brush = capsuleShader.brush(size, trackColor))
        } else {
            drawPath(path = trackPath, color = trackColor)
        }

        val thumbHeight = CouiSwitchDefaults.ThumbSize.toPx()
        val thumbWidth = thumbHeight * stretch
        val startCenterX = paddingPx + thumbHeight / 2f
        val endCenterX = size.width - paddingPx - thumbHeight / 2f
        val centerX = startCenterX + (endCenterX - startCenterX) * travel

        // drawOuterCircle itself uses ordinary roundRect geometry. The View's smooth
        // outline clips all content, including the thumb when the spring overshoots.

        val thumbLeft = centerX - thumbWidth / 2f
        val thumbTop = size.height / 2f - thumbHeight / 2f
        clipPath(trackPath) {
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbLeft, thumbTop),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbHeight / 2f, thumbHeight / 2f),
            )
        }
    }
}
