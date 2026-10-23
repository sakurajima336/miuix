// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.effect.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.effect.CouiSpotLightSpec
import io.github.wfc35286.coui.kmp.effect.drawCouiSpotLight
import io.github.wfc35286.coui.kmp.effect.rememberCouiSpotLightState
import io.github.wfc35286.coui.kmp.motion.CouiDeformDeadZonePx
import io.github.wfc35286.coui.kmp.motion.CouiMinVisibleChange
import io.github.wfc35286.coui.kmp.motion.CouiPressScaleRange
import io.github.wfc35286.coui.kmp.motion.CouiPressSprings
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.CouiTouchCommon
import io.github.wfc35286.coui.kmp.motion.CouiTouchTiers
import io.github.wfc35286.coui.kmp.motion.couiCalculateScaleWithK
import io.github.wfc35286.coui.kmp.motion.couiComposeScale
import io.github.wfc35286.coui.kmp.motion.couiDeformScales
import io.github.wfc35286.coui.kmp.motion.couiHandUpSpring
import io.github.wfc35286.coui.kmp.motion.couiPressScale
import io.github.wfc35286.coui.kmp.motion.couiRubberBand
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The measured values of [CouiAccessoryButton], all from `coui_bottom_floating_*` resources.
 *
 * The official `COUIBottomFloatingAccessoryView` has **no size setter at all**: `onMeasure` calls
 * `setMeasuredDimension(mAccessorySize, mAccessorySize)` (smali `:2553-2557`), so a
 * `layout_width`/`layout_height` in XML is simply ignored.
 */
object CouiAccessoryButtonDefaults {
    /**
     * `mAccessorySize` = `coui_bottom_floating_navigation_bar_height` (dimen `0x7f070574` = 56dp,
     * read at `COUIBottomFloatingAccessoryView.smali:196-202`).
     *
     * The pill is measured with a **different** dimen of the same value (`0x7f0707c5` = 56dp on
     * `BaseBottomFloatingMenuView`), which is why the round button and the capsule line up
     * exactly: `capsuleH == mAccessorySize`, so their independently centred tops coincide
     * (`COUIBottomFloatingAccessoryNavigationBar.smali:1717-1726`).
     */
    val Size: Dp = 56.dp

    /**
     * `mSideViewSpacing` = `coui_bottom_floating_navigation_bar_Minimum_spacing_with_slide_view`
     * (dimen `0x7f0707c4` = 12dp, read at `COUIBottomFloatingAccessoryNavigationBar.smali:183`).
     *
     * It is the **only** spacing constant on that row: `layoutPositionsMediumScreen` adds
     * `mAccessorySize + mSideViewSpacing` once per configured side and nothing else
     * (`:797-805`, `:834-889`). The pill's own `mInnerPadding` (12dp) never enters the gap - the
     * panel background fills the full bounds (`caustic-java/BaseBottomFloatingMenuView.smali:3173-3190`),
     * so the visible gap between the two fills is the same 12dp. Do **not** add it twice.
     *
     * It happens to equal the pill's `coui_bottom_floating_navigation_bar_item_padding`
     * (`mInnerPadding`, also 12dp) but they are different resources and must not be conflated.
     */
    val Spacing: Dp = 12.dp

    /**
     * The box the icon is drawn into.
     *
     * [推导] The official has no icon size: `drawIcon` paints the drawable at its **intrinsic**
     * size (`COUIBottomFloatingAccessoryView.smali:705-830`) and the gallery hands it a 24dp
     * vector. A Compose `Painter` carries an intrinsic size only for vectors, so the library fixes
     * the box at the gallery's 24dp instead of leaving the button's content unmeasured.
     */
    val IconSize: Dp = 24.dp

    /**
     * `applyFallbackBackgroundColor()`: the non-material fill is the custom colour when
     * `setAccessoryBackgroundColor` was called, otherwise the `couiColorBar` attribute
     * (`COUIBottomFloatingAccessoryView.smali:294-322`).
     *
     * Note the official's own caveat: that method returns immediately when
     * `isMaterialQualityEnable()` is true, i.e. **on a material device the custom colour never
     * shows** - the framework paints the surface instead. This component always paints the fill
     * itself (it does not reproduce the accessory's blur material), so [backgroundColor] always
     * takes effect here.
     */
    @Composable
    fun backgroundColor(): Color = CouiTheme.colors.bar

    /**
     * The press light: `COUISpotLightEffect.TYPE_BOTTOM_NAVIGATION`
     * (`COUIBottomFloatingAccessoryView.smali:1693-1696`).
     *
     * `COUISpotLightEffect$SpotLightType` maps `TYPE_BOTTOM_NAVIGATION` to switch index `0x1b`
     * (`COUISpotLightEffect_WhenMappings.smali:398-406`), and `0x1b` is the very case
     * `TYPE_OPAQUE_MEDIUM_2` resolves to (`COUISpotLightEffect.smali:2553`) - the round button and
     * the pill therefore share one parameter object, which is why
     * [CouiSpotLightDefaults.OpaqueMedium2] is used here as well.
     */
    fun spotLight(dark: Boolean): CouiSpotLightSpec = CouiSpotLightDefaults.OpaqueMedium2(dark)

    /**
     * The press-scale target, `TouchAnimatorParamProvider.calculateScaleWithK` evaluated for the
     * button itself: `56 x 56 dp` is a square, so the aspect channel stays asleep and
     * `PressCalc.calculateUniformScaleBreakdown` returns the area channel alone.
     *
     * `area = 3136`, `t = (3136 - 400) / (22500 - 400) = 0.1238`, the `(0, 0, 0, 1)` press bezier
     * maps it to `0.497`, and `0.85 + 0.497 * (0.97 - 0.85) = 0.910` - the 0.910 the gallery's
     * round button measures (`41-GALLERY-TOP-BUTTONS.md` §"按压目标 scale").
     */
    fun pressScale(): Float =
        couiPressScale(
            width = Size.value,
            height = Size.value,
            minScale = CouiPressScaleRange.ScaleRangeMin,
            maxScale = CouiPressScaleRange.ScaleRangeMax,
        )
}

/**
 * The round accessory button that sits at either end of a floating bar.
 *
 * This is `com.coui.appcompat.bottomfloatingaccessorynavigationbar.COUIBottomFloatingAccessoryView`,
 * which - unlike everything else in that package - is a **plain `android.view.View`** that can be
 * used on its own: three standard constructors, `setClickable(true)`/`setFocusable(true)` set in
 * the constructor, a self-imposed 56dp square from `onMeasure`, and a null-safe effect host
 * (`46-ACCESSORY-BUTTON-API.md` §2). The container merely owns two of them.
 *
 * ## What it draws, bottom to top
 *
 * 1. **The caustic glow** - `updateOuterShadow()` -> `setCausticShadowEffect()` ->
 *    `setOutlineCausticShadow{Color,Layout,Params}` with **level 5**:
 *    `b = 11.199982dp`, `sdf = 20.0dp`, `l/t/r = 0`, colour `coui_caustic_color_2` =
 *    `#42ffffff` (`COUIBottomFloatingAccessoryView.smali:1768-1843`; the level-5 call sites are
 *    `:1773` (elevation 7), `:1779` (colour), `:1791` (l/t/r), `:1804` (b), `:1833` (sdf)).
 *    This is deliberately **not** the pill's level 4: the button's glow is more than twice as
 *    deep (11.2dp vs 4.9dp) and its band twice as wide (20dp vs 10.4dp).
 * 2. **The fill** - `couiColorBar`, or whatever [backgroundColor] the caller set.
 * 3. **The spot light** - `COUISpotLightEffectDrawable` clipped to the circle and painted by
 *    `onDraw` *under* the icon (`:1207-1233`). Not a ripple, not a state effect: the gallery's
 *    three controls each use a different press visual and this is the spot light one.
 * 4. **The icon** - no tint anywhere (`setTint` has zero hits in the class), so a tinted icon is a
 *    caller-side decision.
 *
 * ## Press feel
 *
 * `COUITouchMotionAnimator` with all three units on (`setDeformationEnable(true)`,
 * `setRubberEnable(true)`, `setPressScaleEnable(true)`, `:1289-1332`):
 *
 * * press scale follows `PressParams.jellyPressSpring` = `(bounce 0.0, response 0.25)`;
 * * the release spring is **not** a constant - `TouchMotionHandUpSpringResolver.resolve(w, h)`
 *   interpolates between `iolHandUpSpring` (0.65 / 0.42) and `iolHandUpSpringLargeArea`
 *   (0.65 / 0.45) by surface size, and a 56dp button sits at the small end, so it gets
 *   `(0.65, 0.42)`;
 * * the deform and rubber-band channels run on `iolDragSpring` = `(0.15, 0.15)` while the finger
 *   is down and on the same size-resolved hand-up spring on release.
 *
 * The three channels are summed, **not** multiplied: `TouchMotionCollector.put` accumulates
 * `mScaleX += value - 1` for both `PRESS_SCALE` and `DEFORM_SCALE`, which is exactly
 * [couiComposeScale].
 *
 * There is **no haptic feedback** on this control (`COUIBottomFloatingAccessoryView` never calls
 * `performHapticFeedback`; only the unrelated `COUIFloatingButton` does).
 *
 * @param onClick Called when the button is tapped.
 * @param modifier The modifier to apply. The button is **56dp square regardless of it**: the
 *   official `onMeasure` ignores the incoming spec, which [androidx.compose.foundation.layout.requiredSize]
 *   reproduces - a caller's `Modifier.size(...)` cannot stretch the circle.
 * @param enabled Whether the button reacts to touch.
 * @param backgroundColor The circle's fill. Defaults to `couiColorBar`.
 * @param contentDescription The accessibility label, i.e. `View.setContentDescription` (the
 *   official has no setter of its own for it either).
 * @param backdrop The page behind the circle, for the `TYPE_FRAMEWORK_BOTTOM_BAR` blur the
 *   official mounts through `setBackgroundRenderEffect`. Defaults to the subtree's
 *   [LocalCouiBackdrop]; `null` falls back to the flat fill.
 * @param content The circle's content, centred.
 */
@Composable
fun CouiAccessoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = CouiAccessoryButtonDefaults.backgroundColor(),
    contentDescription: String? = null,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    content: @Composable BoxScope.() -> Unit,
) {
    CouiAccessoryButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        contentDescription = contentDescription,
        backdrop = backdrop,
        content = content,
    )
}

/**
 * [CouiAccessoryButton] for the common case of a single icon.
 *
 * The icon change is the official `setIconDrawable(drawable, animate)`
 * (`COUIBottomFloatingAccessoryView.smali:2659`, animating branch at `:2357-2460`): the outgoing
 * icon fades out and shrinks to nothing while the incoming one grows in from nothing and fades in.
 *
 * @param onClick Called when the button is tapped.
 * @param icon The icon, or `null` for an empty circle. Pass a **remembered** painter - a fresh
 *   instance every composition is a different icon to this component and restarts the swap.
 * @param contentDescription The accessibility label.
 * @param modifier The modifier to apply. See [CouiAccessoryButton].
 * @param enabled Whether the button reacts to touch.
 * @param backgroundColor The circle's fill.
 * @param animateIconChange Whether to play the swap animation. `false` is
 *   `setIconDrawable(drawable)` / the deprecated overload, which applies the icon instantly.
 * @param backdrop The page behind the circle, for the `TYPE_FRAMEWORK_BOTTOM_BAR` blur the
 *   official mounts through `setBackgroundRenderEffect`. Defaults to the subtree's
 *   [LocalCouiBackdrop]; `null` falls back to the flat fill.
 */
@Composable
fun CouiAccessoryButton(
    onClick: () -> Unit,
    icon: Painter?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = CouiAccessoryButtonDefaults.backgroundColor(),
    animateIconChange: Boolean = true,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
) {
    // `mIconDrawable` / `mOutgoingIconDrawable`: the incoming painter is swapped in immediately and
    // the previous one is kept alive only until its exit animation ends.
    var shown by remember { mutableStateOf(icon) }
    var outgoing by remember { mutableStateOf<Painter?>(null) }
    val incomingScale = remember { Animatable(1f) }
    val incomingAlpha = remember { Animatable(1f) }
    val outgoingScale = remember { Animatable(1f) }
    val outgoingAlpha = remember { Animatable(0f) }
    LaunchedEffect(icon, animateIconChange) {
        val previous = shown
        if (previous === icon) return@LaunchedEffect
        if (icon == null) {
            // `applyIconDrawableInstant(null)`: the official drops both layers.
            shown = null
            outgoing = null
            return@LaunchedEffect
        }
        if (!animateIconChange) {
            shown = icon
            outgoing = null
            incomingScale.snapTo(1f)
            incomingAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        outgoing = previous
        if (previous != null) {
            outgoingScale.snapTo(1f)
            outgoingAlpha.snapTo(1f)
        }
        // `startIconReplaceAnimation`: `mIconAlpha = 0`, `mIconScale = 0` in the full-effect branch,
        // then both springs are aimed at 1.0 (`:2418-2436`).
        incomingScale.snapTo(0f)
        incomingAlpha.snapTo(0f)
        shown = icon
        coroutineScope {
            if (previous != null) {
                launch { outgoingAlpha.animateTo(0f, CouiIconOutgoingAlphaSpring) }
                launch { outgoingScale.animateTo(0f, CouiIconScaleSpring) }
            }
            launch { incomingAlpha.animateTo(1f, CouiIconIncomingAlphaSpring) }
            launch { incomingScale.animateTo(1f, CouiIconScaleSpring) }
        }
        if (previous != null) outgoing = null
    }
    CouiAccessoryButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        contentDescription = contentDescription,
        backdrop = backdrop,
    ) {
        outgoing?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(CouiAccessoryButtonDefaults.IconSize)
                    .graphicsLayer {
                        scaleX = outgoingScale.value
                        scaleY = outgoingScale.value
                        alpha = outgoingAlpha.value
                    },
            )
        }
        shown?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(CouiAccessoryButtonDefaults.IconSize)
                    .graphicsLayer {
                        scaleX = incomingScale.value
                        scaleY = incomingScale.value
                        alpha = incomingAlpha.value
                    },
            )
        }
    }
}

/**
 * `applyIconReplaceSpringParams(true)` (`COUIBottomFloatingAccessoryView.smali:543-589`): the icon
 * swap's scale springs are `(bounce 0.4, response 0.5)`, the incoming alpha is `(0.0, 0.2)` and
 * the outgoing alpha is `(0.0, 0.15)`.
 *
 * The class also carries a "light" variant (`incoming (0.0, 0.15)`, `outgoing (0.0, 0.1)`) that is
 * selected by `EffectCapabilityHost.isFullMotionEffect()`; that host is supplied by the container
 * and has no equivalent here, so the full-effect set is always used.
 */
private val CouiIconScaleSpring =
    CouiSpringData(bounce = 0.4f, response = 0.5f).toSpringSpec(CouiMinVisibleChange.Scale)

/** @see CouiIconScaleSpring */
private val CouiIconIncomingAlphaSpring =
    CouiSpringData(bounce = 0f, response = 0.2f).toSpringSpec(CouiMinVisibleChange.Alpha)

/** @see CouiIconScaleSpring */
private val CouiIconOutgoingAlphaSpring =
    CouiSpringData(bounce = 0f, response = 0.15f).toSpringSpec(CouiMinVisibleChange.Alpha)

/**
 * The chrome every [CouiAccessoryButton] overload shares: the circle, the caustic glow, the fill,
 * the spot light and the press motion.
 */
@Composable
private fun CouiAccessoryButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    backgroundColor: Color,
    contentDescription: String?,
    backdrop: CouiBackdrop?,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = CouiTheme.colors.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    // `COUIPressAnimatorUnit` is driven by `COUIITouchMotionUnit.handleTouchEvent`, **not** by the
    // click recogniser: the official press scale has no MOVE handler at all
    // (`COUIPressAnimatorUnit.moveAnimate` returns immediately), and the only place that releases
    // it is `handleTouchUp`, reached from ACTION_UP / ACTION_CANCEL. Deriving the press from
    // `clickable`'s interaction source instead makes the scale collapse the moment the finger
    // travels past the touch slop - the button shrinks, then pops back to 1.0 as the drag starts,
    // and only then does the deform kick in. Holding our own flag keeps the two channels
    // independent, which is what lets it deform *while* it is shrunk.
    var pressHeld by remember { mutableStateOf(false) }
    val spotLight = rememberCouiSpotLightState()
    val spotLightSpec = remember(dark) { CouiAccessoryButtonDefaults.spotLight(dark) }
    val density = LocalDensity.current

    // `COUIPressAnimatorUnit`: the press scale is a size-derived target, not a constant.
    val pressScale = remember { Animatable(1f) }
    val scaleTarget = if (enabled && pressHeld) CouiAccessoryButtonDefaults.pressScale() else 1f
    // `PressParams.jellyPressSpring` on the way down; on the way up
    // `TouchMotionHandUpSpringResolver.resolve(56, 56)`, i.e. the small-area end of the tier.
    val pressSpring = CouiPressSprings.JellyPress
    val releaseSpring = remember {
        couiHandUpSpring(
            widthDp = CouiAccessoryButtonDefaults.Size.value,
            heightDp = CouiAccessoryButtonDefaults.Size.value,
        )
    }
    LaunchedEffect(pressHeld, scaleTarget, enabled) {
        pressScale.animateTo(
            targetValue = scaleTarget,
            animationSpec = (if (pressHeld) pressSpring else releaseSpring)
                .toSpringSpec(CouiMinVisibleChange.Scale),
        )
    }

    // `COUIDeformAnimatorUnit` + `COUIRubberAnimatorUnit`, advanced once per frame exactly like
    // `COUISpringAnimation` ticks - see the same block in `CouiBottomFloatingNavigationBar`.
    val deformXAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val deformYAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val rubberXAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    val rubberYAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    var motionTick by remember { mutableIntStateOf(0) }
    val maxStretchPx = with(density) { CouiTouchTiers.MaxStretchDistance.dp.toPx() }
    val maxDeformPx = with(density) { CouiTouchTiers.MaxDeformRubberDistance.dp.toPx() }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt =
                    if (last == 0L) {
                        0.016f
                    } else {
                        ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
                    }
                last = now
                var moving = deformXAxis.advance(dt)
                moving = deformYAxis.advance(dt) || moving
                moving = rubberXAxis.advance(dt) || moving
                moving = rubberYAxis.advance(dt) || moving
                if (moving) motionTick++
            }
        }
    }

    Box(
        modifier =
            modifier
                // `setMeasuredDimension(mAccessorySize, mAccessorySize)`: the incoming constraints
                // are ignored, so the caller's `Modifier.size(...)` cannot change the circle.
                .requiredSize(CouiAccessoryButtonDefaults.Size)
                .then(
                    if (contentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    },
                )
                // The gesture is **not** consumed here: `clickable` owns the tap, this handler only
                // feeds the spot light and the deform/rubber axes (the official does the same in
                // `dispatchTouchEvent`, which runs `mTouchMotionAnimator` and then `super`).
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        // `setLightPositionByPointerIndex` normalises against the drawable bounds.
                        spotLight.press(Offset(down.position.x / w, down.position.y / h))
                        // `handleTouchDown`: the press scale goes down here and stays down until
                        // the gesture actually ends - see the `pressHeld` comment above.
                        pressHeld = true
                        for (axis in listOf(deformXAxis, deformYAxis, rubberXAxis, rubberYAxis)) {
                            axis.spring = CouiTouchTiers.DragSpring
                            axis.target = axis.value
                        }
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            spotLight.moveTo(
                                Offset(change.position.x / w, change.position.y / h),
                            )
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            // `checkDeadZone` is an OR over the raw axes, in raw pixels.
                            if (
                                abs(dx) >= CouiDeformDeadZonePx ||
                                    abs(dy) >= CouiDeformDeadZonePx
                            ) {
                                val scales =
                                    couiDeformScales(
                                        dx = dx,
                                        dy = dy,
                                        maxScale =
                                            couiCalculateScaleWithK(
                                                width = size.width.toFloat(),
                                                height = size.height.toFloat(),
                                                maxScale = CouiTouchCommon.MaxScale,
                                            ),
                                        maxRubberPx = maxDeformPx,
                                    )
                                deformXAxis.target = scales.x
                                deformYAxis.target = scales.y
                                rubberXAxis.target =
                                    couiRubberBand(dx, CouiTouchTiers.RubberCurveRatio, maxStretchPx)
                                rubberYAxis.target =
                                    couiRubberBand(dy, CouiTouchTiers.RubberCurveRatio, maxStretchPx)
                            }
                        }
                        val velocity = velocityTracker.calculateVelocity()
                        spotLight.release()
                        // `handleTouchUp`: the single place the press scale is allowed to spring
                        // back. Dragging never writes it.
                        pressHeld = false
                        // `applyHandUpSpring`: `initial = clamp(pan / totalDist * |value - rest|, +-1000)`
                        // with the *per-unit* total distance (150dp for deform, 28dp for the rubber band).
                        fun releaseAxis(axis: PopupSpringAxis, rest: Float, pan: Float, rangePx: Float) {
                            val denominator = rangePx.coerceAtLeast(1f)
                            axis.velocity =
                                (pan / denominator * abs(axis.value - rest)).coerceIn(-1000f, 1000f)
                            axis.spring = releaseSpring
                            axis.target = rest
                        }
                        releaseAxis(deformXAxis, 1f, velocity.x, maxDeformPx)
                        releaseAxis(deformYAxis, 1f, velocity.y, maxDeformPx)
                        releaseAxis(rubberXAxis, 0f, velocity.x, maxStretchPx)
                        releaseAxis(rubberYAxis, 0f, velocity.y, maxStretchPx)
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .graphicsLayer {
                    motionTick
                    // `TouchMotionCollector.put`: additive, never `press * deform`.
                    val press = pressScale.value
                    scaleX = couiComposeScale(press, deformXAxis.value)
                    scaleY = couiComposeScale(press, deformYAxis.value)
                    translationX = rubberXAxis.value
                    translationY = rubberYAxis.value
                }
                // `setCausticShadowEffect()`: painted behind the fill and **outside** the circle -
                // the render node draws the outline shadow under the view's own content.
                .couiAccessoryCaustic(dark)
                .clip(CircleShape)
                // `setViewBlurEffect(this, TYPE_FRAMEWORK_BOTTOM_BAR)`: the same blur preset the
                // pill uses, applied to the circle and sampling the page behind it. When no
                // backdrop is available it degrades to the flat fill, which is what a non-material
                // device shows anyway.
                .couiAccessoryMaterial(dark = dark, backdrop = backdrop)
                .background(backgroundColor)
                .drawWithContent {
                    // `onDraw`: the touch effect is drawn before the icon.
                    drawCouiSpotLight(spotLight, spotLightSpec, CircleShape)
                    this@drawWithContent.drawContent()
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The circle's caustic glow - level 5 of `setOutlineCausticShadowLayout`/`Params`.
 *
 * The real effect lives in `libhwui.so` (`SkCanvas::onDrawCausticShadow`), so it is drawn by the
 * platform actual; on skiko it degrades to the shape approximation the pill already uses.
 */
@Composable
internal expect fun Modifier.couiAccessoryCaustic(dark: Boolean): Modifier

/**
 * The circle's frosted material - the **same** `TYPE_FRAMEWORK_BOTTOM_BAR` blur the pill uses.
 *
 * `COUIBottomFloatingAccessoryView` calls `COUIMaterialEffect.setViewBlurEffect(this,
 * TYPE_FRAMEWORK_BOTTOM_BAR)` on itself (`:2073-2075`, fanned out from `initMaterialAppearance`
 * `:1161-1173` and refreshed by `onMaterialStateChange(type == 2)` `:2511-2535`) - it never goes
 * through its parent, and a `setViewBlurEffect` call on the container is a zero hit. The preset is
 * the pill's own (`MaterialBlurEffect.smali:178-188`): blur radius 25, `#B3666666` OVERLAY over
 * `#99F5F5F5` LUMINOSITY, or `#80666666` / `#B3262626` in dark.
 *
 * The one thing that differs from a plain `RenderEffect` is **what it samples**: the official
 * mounts it through `OplusViewBackgroundRenderEffect.setBackgroundRenderEffect(effect, view)`
 * (`COUIMaterialEffect.smali:381`), i.e. it reads the *page behind* the view, not the view's own
 * content. A Compose `graphicsLayer { renderEffect }` only sees its own layer, so the actual has
 * to paint a captured [CouiBackdrop] into the layer first - that is why [backdrop] is a parameter.
 *
 * When no backdrop is supplied - or the version gates the official applies twice more
 * (`isMaterialVersion()`, `canApplyBackgroundRenderEffect()`) are not met - the actual paints the
 * flat `couiColorBar` fill instead. The official does **not** fall back (it just returns and
 * draws nothing), which is a real defect there; a visible flat panel is the better degradation.
 */
@Composable
internal expect fun Modifier.couiAccessoryMaterial(
    dark: Boolean,
    backdrop: CouiBackdrop?,
): Modifier
