// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
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
import io.github.wfc35286.coui.kmp.graphics.CouiBackdrop
import io.github.wfc35286.coui.kmp.graphics.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.graphics.CouiSpotLightSpec
import io.github.wfc35286.coui.kmp.graphics.LocalCouiBackdrop
import io.github.wfc35286.coui.kmp.graphics.drawCouiSpotLight
import io.github.wfc35286.coui.kmp.graphics.rememberCouiSpotLightState
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
import io.github.wfc35286.coui.kmp.popup.PopupSpringAxis
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


/**
 * The measured values of [CouiAccessoryButton].
 *
 * The button always lays out as a fixed-size square: incoming size constraints are ignored.
 */
object CouiAccessoryButtonDefaults {
    /**
     * The button's size. The capsule of the floating bar is measured with the same value, so the
     * round button and the capsule line up exactly: their independently centred tops coincide.
     */
    val Size: Dp = 56.dp

    /**
     * The gap between the round button and the side panel next to it.
     *
     * It is the **only** spacing constant on that row: each configured side adds [Size] +
     * [Spacing] once and nothing else. The pill's own inner padding never enters the gap - the
     * panel background fills the full bounds, so the visible gap between the two fills is the
     * same value. Do **not** add it twice.
     */
    val Spacing: Dp = 12.dp

    /**
     * The box the icon is drawn into.
     *
     * A Compose `Painter` carries an intrinsic size only for vector painters, so the library fixes
     * the box instead of leaving the button's content unmeasured.
     */
    val IconSize: Dp = 24.dp

    /**
     * The circle's base fill. It is the base the blur material composites over, never a layer on
     * top of it, so a caller-supplied colour always takes effect.
     */
    @Composable
    fun backgroundColor(): Color = CouiTheme.colors.bar

    /**
     * The press-light preset: the same parameters the floating bar's capsule uses, i.e.
     * [CouiSpotLightDefaults.OpaqueMedium2].
     */
    fun spotLight(dark: Boolean): CouiSpotLightSpec = CouiSpotLightDefaults.OpaqueMedium2(dark)

    /**
     * The press-scale target, derived from the button's own size: a 56dp square takes the area
     * channel alone and lands at **0.910**.
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
 * It is a plain, self-contained control: a fixed 56dp square, clickable and focusable by default,
 * usable on its own - the floating bar merely owns two of them.
 *
 * ## What it draws, bottom to top
 *
 * 1. **The caustic glow** - an outline caustic shadow painted behind and **outside** the circle:
 *    11.2dp deep with a 20dp-wide band, tinted `#42ffffff`. It is deliberately deeper and wider
 *    than the pill's glow.
 * 2. **The fill** - [backgroundColor].
 * 3. **The spot light** - the press light, clipped to the circle and painted **under** the icon.
 *    Not a ripple, not a state effect: the spot light is this control's whole press visual.
 * 4. **The icon** - never tinted by the button; a tinted icon is a caller-side decision.
 *
 * ## Press feel
 *
 * Three motion channels run together and are **summed**, never multiplied (see [couiComposeScale]):
 *
 * * the press scale follows a light press spring on the way down;
 * * the release spring is **not** a constant - it is resolved from the control's size, and a 56dp
 *   button sits at the small-area end of the tier;
 * * the deform and rubber-band channels follow the finger with a loose drag spring while it is
 *   down, and use the same size-resolved spring on release.
 *
 * There is **no haptic feedback** on this control.
 *
 * @param onClick Called when the button is tapped.
 * @param modifier The modifier to apply. The button is **56dp square regardless of it**: incoming
 *   size constraints are ignored, which [androidx.compose.foundation.layout.requiredSize]
 *   reproduces - a caller's `Modifier.size(...)` cannot stretch the circle.
 * @param enabled Whether the button reacts to touch.
 * @param backgroundColor The circle's fill. Defaults to the theme's bar colour.
 * @param contentDescription The accessibility label.
 * @param backdrop The page behind the circle, sampled by the blur material. Defaults to the
 *   subtree's [LocalCouiBackdrop]; `null` falls back to the flat fill.
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
 * Icons swap with a two-way animation: the outgoing icon fades out and shrinks to nothing while
 * the incoming one grows in from nothing and fades in.
 *
 * @param onClick Called when the button is tapped.
 * @param icon The icon, or `null` for an empty circle. Pass a **remembered** painter - a fresh
 *   instance every composition is a different icon to this component and restarts the swap.
 * @param contentDescription The accessibility label.
 * @param modifier The modifier to apply. See [CouiAccessoryButton].
 * @param enabled Whether the button reacts to touch.
 * @param backgroundColor The circle's fill.
 * @param animateIconChange Whether to play the swap animation; `false` applies the icon instantly.
 * @param backdrop The page behind the circle, sampled by the blur material. Defaults to the
 *   subtree's [LocalCouiBackdrop]; `null` falls back to the flat fill.
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
    // The incoming painter is swapped in immediately and the previous one is kept alive only
    // until its exit animation ends.
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
            // An empty icon drops both layers.
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
        // The incoming icon starts fully transparent and collapsed, then both springs aim at 1.0.
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
 * The icon swap's springs: the scale is `(bounce 0.4, response 0.5)`, the incoming alpha is
 * `(0.0, 0.2)` and the outgoing alpha is `(0.0, 0.15)`. The full-effect set is always used.
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
    // The press scale is driven by raw pointer input, **not** by the click recogniser: it has no
    // move handler at all, and the only place that releases it is the end of the gesture
    // (ACTION_UP / ACTION_CANCEL). Deriving the press from
    // `clickable`'s interaction source instead makes the scale collapse the moment the finger
    // travels past the touch slop - the button shrinks, then pops back to 1.0 as the drag starts,
    // and only then does the deform kick in. Holding our own flag keeps the two channels
    // independent, which is what lets it deform *while* it is shrunk.
    var pressHeld by remember { mutableStateOf(false) }
    val spotLight = rememberCouiSpotLightState()
    val spotLightSpec = remember(dark) { CouiAccessoryButtonDefaults.spotLight(dark) }
    val density = LocalDensity.current

    // The press scale is a size-derived target, not a constant.
    val pressScale = remember { Animatable(1f) }
    val scaleTarget = if (enabled && pressHeld) CouiAccessoryButtonDefaults.pressScale() else 1f
    // A light press spring on the way down; on the way up the size-resolved hand-up spring of a
    // 56dp square, i.e. the small-area end of the tier.
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

    // The deform and rubber-band axes, advanced once per frame so the springs integrate normally
    // - see the same block in `CouiBottomFloatingNavigationBar`.
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
                // Incoming size constraints are ignored, so the caller's `Modifier.size(...)`
                // cannot change the circle.
                .requiredSize(CouiAccessoryButtonDefaults.Size)
                .then(
                    if (contentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    },
                )
                // The gesture is **not** consumed here: `clickable` owns the tap, this handler only
                // feeds the spot light and the deform/rubber axes.
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        // The spotlight position is normalised against the button bounds.
                        spotLight.press(Offset(down.position.x / w, down.position.y / h))
                        // The press scale goes down here and stays down until the gesture
                        // actually ends - see the `pressHeld` comment above.
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
                            // The dead zone is an OR over the raw axes, in raw pixels.
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
                        // The single place the press scale is allowed to spring back. Dragging
                        // never writes it.
                        pressHeld = false
                        // Release velocity: `clamp(pan / totalDist * |value - rest|, +-1000)`
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
                    // Additive composition, never `press * deform`.
                    val press = pressScale.value
                    scaleX = couiComposeScale(press, deformXAxis.value)
                    scaleY = couiComposeScale(press, deformYAxis.value)
                    translationX = rubberXAxis.value
                    translationY = rubberYAxis.value
                },
        contentAlignment = Alignment.Center,
    ) {
        // The chrome is a **sibling** of the content, never its ancestor.
        //
        // `couiAccessoryMaterial` hangs a blur `RenderEffect` on a `graphicsLayer`, and a Compose
        // layer blurs *everything drawn into it* - which, when the icon was a child of that same
        // node, included the icon. The blur must cover the surface only: the spot light and the
        // icon are always drawn unblurred above it. Splitting the chrome into its own
        // `matchParentSize` Box keeps the icon above the material.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    // The caustic glow is painted behind the fill and **outside** the circle -
                    // the outline shadow sits under the button's own content.
                    .couiAccessoryCaustic(dark)
                    .clip(CircleShape)
                    // A frosted material: the same blur preset the pill uses, applied to the
                    // circle and sampling the page behind it. When no backdrop is available it
                    // degrades to the flat fill.
                    // The fill is handed to the material instead of being painted here: it is the
                    // **base** the blur composites over, never a layer on top of it. Painting it
                    // after the material put it on top of the blur and flattened the circle to a
                    // solid disc.
                    .couiAccessoryMaterial(
                        dark = dark,
                        backdrop = backdrop,
                        backgroundColor = backgroundColor,
                        // Read at draw time: the layer above is the one that carries this scale, so
                        // the backdrop has to sample the page through its inverse or the blur slides
                        // with the button while it is pressed and dragged.
                        scaleX = { couiComposeScale(pressScale.value, deformXAxis.value) },
                        scaleY = { couiComposeScale(pressScale.value, deformYAxis.value) },
                    )
                    .drawWithContent {
                        // The press light is drawn before the content.
                        drawCouiSpotLight(spotLight, spotLightSpec, CircleShape)
                        this@drawWithContent.drawContent()
                    },
        )
        // The rim: a **third** sibling, above the material and below the icon.
        //
        // It cannot be appended to the chrome chain above. `couiAccessoryMaterial` ends with a
        // `graphicsLayer { renderEffect }`, and every modifier after that node draws *inside* the
        // layer - so a stroke hung there goes through the same blur kernel as the page behind the
        // button and all but disappears.
        Box(modifier = Modifier.matchParentSize().clip(CircleShape).couiAccessoryEdge(dark))
        content()
    }
}

/**
 * The circle's caustic glow.
 *
 * The real effect lives in the platform renderer, so it is drawn by the platform actual; on skiko
 * it degrades to the shape approximation the pill already uses.
 */
@Composable
internal expect fun Modifier.couiAccessoryCaustic(dark: Boolean): Modifier

/**
 * The circle's frosted material - the **same** blur preset the pill uses: blur radius 25,
 * `#B3666666` OVERLAY over `#99F5F5F5` LUMINOSITY, or `#80666666` / `#B3262626` in dark.
 *
 * What the material samples is the **page behind** the button, not the button's own content. A
 * Compose `graphicsLayer { renderEffect }` only sees its own layer, so the actual has to paint a
 * captured [CouiBackdrop] into the layer first - that is why [backdrop] is a parameter.
 *
 * When no backdrop is supplied - or the platform cannot apply a background blur - the actual
 * paints [backgroundColor] as the flat fill instead. A visible flat panel is the better
 * degradation.
 *
 * @param backgroundColor The circle's fill. It is the **base** the blur is composited over, never
 *   a layer on top of it: painting it above the material flattens the circle to a solid disc.
 * @param scaleX The circle's current horizontal scale, read **at draw time**. The backdrop layer
   draws inside that scaled space, so the page has to be sampled with the inverse - otherwise the
   picture inside the button is glued to it and slides/breathes while it is pressed and dragged,
   which is the artefact `drawCouiBackdropLayer` exists to remove.
 * @param scaleY The vertical counterpart of [scaleX].
 */
@Composable
internal expect fun Modifier.couiAccessoryMaterial(
    dark: Boolean,
    backdrop: CouiBackdrop?,
    backgroundColor: Color,
    scaleX: () -> Float = { 1f },
    scaleY: () -> Float = { 1f },
): Modifier

/**
 * The circle's rim highlight and inner glow.
 *
 * The rim paints on the background layer, i.e. above the blurred material and below the content -
 * it is never part of the button's own drawing.
 *
 * So this must be applied to a **sibling** of [couiAccessoryMaterial], never appended to it: a
 * Compose modifier added after that chain draws inside its blur `graphicsLayer` and would be
 * blurred away. On platforms without the AGSL shader this is a no-op.
 */
@Composable
internal expect fun Modifier.couiAccessoryEdge(dark: Boolean): Modifier
