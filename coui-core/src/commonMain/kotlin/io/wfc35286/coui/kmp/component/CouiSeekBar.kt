// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.motion.CouiSpringData
import io.wfc35286.coui.kmp.motion.toSpringSpec
import io.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Defaults for [CouiSeekBar], read from `COUISeekBarCore.initState` and the `coui_seekbar_*`
 * resources of ColorOS 17.0.0.
 *
 * Every value here is the **default** an unattributed `COUISeekBar` gets, i.e. what
 * `com.oplus.settings.widget.SettingsSeekBar` (the "最低自动亮度" row) renders with. A caller that
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
     * `coui_seekbar_progress_padding_horizontal` - the gap between the progress bar's ends and
     * the track's ends, at rest.
     */
    val ProgressPadding: Dp = 14.dp

    /**
     * `coui_seekbar_progress_pressed_padding_horizontal` - the same gap while the thumb is held.
     *
     * `COUISeekBarCore` swaps to this when tracking starts, which is why the progress bar visibly
     * grows towards both ends the moment you press down.
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

    /** `TexturedThumbRenderer.LAYER_A_ALPHA_TOP` - the gradient's alpha at the top. */
    internal const val LayerATopAlpha: Int = 0xe6

    /** `TexturedThumbRenderer.LAYER_A_ALPHA_BOTTOM` - the gradient's alpha at the bottom. */
    internal const val LayerABottomAlpha: Int = 0x8c

    /**
     * `TexturedThumbRenderer` LayerB's bitmap margin.
     *
     * The smali allocates a `(2r + 2 * 12dp)²` ARGB bitmap and draws the disc inset by 12dp on
     * every side. Because the glow is clipped to the disc, this margin ends up **fully
     * transparent** - it is not a halo around the thumb. Kept for documentation; nothing in the
     * drawing path reads it.
     */
    val LayerBInset: Dp = 12.dp

    /** `TexturedThumbRenderer` LayerB's fill - `#77FFFFFF`, i.e. white at 46.7%. */
    val LayerBFill: Color = Color(0x77FFFFFF)

    /** `LAYER_B_SHADOW_RADIUS` = 5dp. */
    val LayerBShadowRadius: Dp = 5.dp

    /** `LAYER_B_SHADOW_DY` = 0.4dp. */
    val LayerBShadowOffsetY: Dp = 0.4f.dp

    /** `LAYER_B_SHADOW_STROKE_WIDTH` = 10dp. */
    val LayerBShadowStrokeWidth: Dp = 10.dp

    /**
     * `TexturedThumbRenderer.LAYER_C_SPRING_RESPONSE_IN` - the spring LayerC fades **out** with
     * when the finger goes down.
     *
     * The direction is worth spelling out because it reads backwards: `onPressDown` calls
     * `startLayerCAlphaAnim(1.0f, 0.0f, 0.5f)`, whose parameters are `(startValue, finalPosition,
     * response)`. So pressing down takes LayerC from 1 to 0 - the plain white circle fades away
     * and the gradient/shadow layers fade in underneath it.
     */
    val LayerCInSpec: SpringSpec<Float> = CouiSpringData(bounce = 0f, response = 0.5f).toSpringSpec()

    /**
     * `LAYER_C_SPRING_RESPONSE_OUT` - LayerC fades back **in** to 1.0 with this when the finger
     * lifts, over the still-fading gradient.
     */
    val LayerCOutSpec: SpringSpec<Float> = CouiSpringData(bounce = 0f, response = 0.3f).toSpringSpec()

    /** `TEXTURE_SPRING_RESPONSE` / `TEXTURE_SPRING_BOUNCE` - the gradient + shadow layers' alpha. */
    val TextureSpec: SpringSpec<Float> = CouiSpringData(bounce = 0f, response = 0.3f).toSpringSpec()

    /**
     * `COUISeekBarCore.mHeightSpringAnim`'s `COUISpringForce` - `bounce 0.0, response 0.5`.
     *
     * Drives the track's height between [TrackHeight] and
     * `TrackHeight * backgroundEnlargeScale`.
     */
    val EnlargeSpec: SpringSpec<Float> = CouiSpringData(bounce = 0f, response = 0.5f).toSpringSpec()

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
     * `setShadowLayer(5dp, 0, 0.4dp, -1)`, i.e. opaque **white**. This value belongs to the base
     * `StandardThumbRenderer.drawThumbRoundRect`, which `TexturedThumbRenderer` overrides.
     */
    @Composable
    fun thumbShadowColor(): Color = CouiTheme.componentColors.seekBarThumbShadow
}

/**
 * A COUI seek bar.
 *
 * Reproduces `com.coui.appcompat.seekbar.core.COUISeekBarCore`'s geometry and
 * `TexturedThumbRenderer`'s three-layer thumb:
 *
 * | layer | what it draws | when |
 * |---|---|---|
 * | A | a vertical white gradient, `e6 -> e6 @20% -> 8c` | while pressed |
 * | B | a `#77FFFFFF` disc with a blurred outline, inset 12dp | while pressed |
 * | C | a plain white disc | always; fades out on press |
 *
 * The track, by contrast, is deliberately boring: a capsule that springs from
 * [CouiSeekBarDefaults.TrackHeight] to `TrackHeight * backgroundEnlargeScale` on touch-down.
 *
 * @param value The current position, normalised to `0f..1f`.
 * @param onValueChange Called with the new position while dragging and on tap.
 * @param modifier The modifier to apply to the bar.
 * @param enabled Whether the bar can be interacted with.
 * @param backgroundEnlargeScale How far the track swells while pressed. Pass `1f` for the
 *   settings "亮度" row's behaviour, or leave the default `1.4f` for "最低自动亮度".
 */
@Composable
fun CouiSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundEnlargeScale: Float = CouiSeekBarDefaults.BackgroundEnlargeScale,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val trackHeight = with(density) { CouiSeekBarDefaults.TrackHeight.toPx() }
    val progressHeight = with(density) { CouiSeekBarDefaults.ProgressHeight.toPx() }
    val paddingRest = with(density) { CouiSeekBarDefaults.ProgressPadding.toPx() }
    val paddingPressed = with(density) { CouiSeekBarDefaults.ProgressPaddingPressed.toPx() }
    val thumbRadiusRest = with(density) { CouiSeekBarDefaults.ThumbRadius.toPx() }
    val thumbRadiusPressed = with(density) { CouiSeekBarDefaults.ThumbMaxRadius.toPx() }
    val layerBShadowRadius = with(density) { CouiSeekBarDefaults.LayerBShadowRadius.toPx() }
    val layerBStrokeWidth = with(density) { CouiSeekBarDefaults.LayerBShadowStrokeWidth.toPx() }

    // The three animated quantities of the press interaction, each with the spring its smali
    // counterpart uses. They are separate animators on purpose: COUI drives LayerC, the
    // gradient/shadow pair and the track height from three independent COUISpringAnimations, so
    // their slightly different responses are visible as the thumb settling into its pressed look.
    val enlarge = remember { Animatable(0f) }
    val textureAlpha = remember { Animatable(0f) }
    val layerCAlpha = remember { Animatable(1f) }

    // The thumb position, as a fraction. While dragging it follows the finger through a spring -
    // `COUISeekBarAdvanced.trackTouchEvent` does not bind the thumb to the pointer, it animates
    // towards it.
    val thumbFraction = remember { Animatable(value.coerceIn(0f, 1f)) }

    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var barWidth by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    // External changes animate, they do not snap. `COUISeekBarCore.setProgress` runs the value
    // through `startProgressTransitionAnim` when the change did not come from the user.
    LaunchedEffect(value) {
        if (!dragging) {
            thumbFraction.animateTo(
                targetValue = value.coerceIn(0f, 1f),
                animationSpec = CouiSeekBarDefaults.EnlargeSpec,
            )
        }
    }

    // Press state -> the three springs. Kept in one effect so they always start together, which
    // is what `handleMotionEventDown` does when it fires all of them in sequence.
    LaunchedEffect(pressed) {
        if (pressed) {
            enlarge.animateTo(1f, CouiSeekBarDefaults.EnlargeSpec)
        } else {
            enlarge.animateTo(0f, CouiSeekBarDefaults.EnlargeSpec)
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            // onPressDown: LayerC 1 -> 0, gradient/shadow 0 -> 1.
            textureAlpha.animateTo(1f, CouiSeekBarDefaults.TextureSpec)
        } else {
            textureAlpha.animateTo(0f, CouiSeekBarDefaults.TextureSpec)
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            layerCAlpha.animateTo(0f, CouiSeekBarDefaults.LayerCInSpec)
        } else {
            layerCAlpha.animateTo(1f, CouiSeekBarDefaults.LayerCOutSpec)
        }
    }

    val progressColor = CouiSeekBarDefaults.progressColor(enabled)
    val trackColor = CouiSeekBarDefaults.trackColor()
    val thumbColor = CouiSeekBarDefaults.thumbColor(enabled)

    Canvas(
        modifier = modifier
            .onSizeChanged { barWidth = it.width }
            .pointerInput(enabled, barWidth) {
                if (!enabled || barWidth <= 0) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                )
            }
            .pointerInput(enabled, barWidth) {
                if (!enabled || barWidth <= 0) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        pressed = true
                        dragging = true
                    },
                    onDragEnd = {
                        dragging = false
                        pressed = false
                    },
                    onDragCancel = {
                        dragging = false
                        pressed = false
                    },
                    onHorizontalDrag = { change, _ ->
                        // The bar spans `paddingRest` .. `width - paddingRest` at rest, and the
                        // thumb travels between the two ends of that span. `snapScaleToFingerPosition`
                        // in the original uses the same span regardless of the pressed padding.
                        val start = paddingRest
                        val end = barWidth - paddingRest
                        val span = (end - start).coerceAtLeast(1f)
                        val raw = ((change.position.x - start) / span).coerceIn(0f, 1f)
                        val target = if (rtl) 1f - raw else raw
                        onValueChange(target)
                        scope.launch {
                            thumbFraction.animateTo(target, CouiSeekBarDefaults.EnlargeSpec)
                        }
                    },
                )
            },
    ) {
        if (barWidth <= 0) return@Canvas

        val fraction = thumbFraction.value.coerceIn(0f, 1f)
        val centreY = size.height / 2f

        // The track: `mMaxBackgroundHeight = backgroundHeight * backgroundEnlargeScale`, lerped by
        // the enlarge spring.
        //
        // The corner radius is **always half the height**:
        // `COUISeekBarCore.getBackgroundRadiusForDraw()` returns exactly `curBackgroundHeight / 2`,
        // and `drawInactiveTrack` feeds that straight into
        // `canvas.drawRoundRect(rect, r, r, paint)` - or into the G2 `OplusPathAdapter` when
        // `smoothStyleType == 1`. The `coui_seekbar_background_radius` attr (2dp) never reaches
        // the draw call, so reading it as the radius yields an almost square track. That was this
        // component's first-pass bug, caught on device.
        val height = trackHeight + (trackHeight * backgroundEnlargeScale - trackHeight) * enlarge.value
        val radius = height / 2f
        val trackTop = centreY - height / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, height),
            cornerRadius = CornerRadius(radius, radius),
        )

        // The progress bar. Its inset springs from 14dp to 4dp on press (`progress_padding_horizontal`
        // -> `progress_pressed_padding_horizontal`), so the bar grows towards both ends.
        val padding = paddingRest + (paddingPressed - paddingRest) * enlarge.value
        val progressWidth = (size.width * fraction - padding).coerceAtLeast(0f)
        if (progressWidth > 0f) {
            val progressH = progressHeight + (trackHeight * backgroundEnlargeScale - progressHeight) * enlarge.value
            val progressTop = centreY - progressH / 2f
            // `StandardProgressRenderer.drawProgress` uses the same `height / 2` radius.
            val progressR = progressH / 2f
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(padding, progressTop),
                size = Size(progressWidth, progressH),
                cornerRadius = CornerRadius(progressR, progressR),
            )
        }

        // The thumb. Its radius grows 6dp -> 8dp while pressed, and its centre rides the fraction.
        val thumbRadius = thumbRadiusRest + (thumbRadiusPressed - thumbRadiusRest) * enlarge.value
        val thumbCentreX = paddingRest + (size.width - 2 * paddingRest) * fraction

        drawTexturedThumb(
            centreX = thumbCentreX,
            centreY = centreY,
            radius = thumbRadius,
            layerAAlpha = textureAlpha.value,
            layerBShadowRadius = layerBShadowRadius,
            layerBStrokeWidth = layerBStrokeWidth,
            layerBFill = CouiSeekBarDefaults.LayerBFill,
            layerCAlpha = layerCAlpha.value,
            layerCColor = thumbColor,
        )
    }
}

/**
 * `TexturedThumbRenderer.drawTexturedThumb`.
 *
 * The three layers are drawn in the original's order and with its alpha plumbing:
 *
 * 1. `fillTextureRect` - the layer rect is the square inscribed in the thumb circle, i.e. a
 *    square of side `2 * radius` centred on the thumb.
 * 2. If the gradient's alpha is non-zero, `saveLayerAlpha` over that rect, then LayerA and
 *    LayerB, then restore. So the two layers share one alpha, they do not each fade separately.
 * 3. LayerC is drawn **outside** that group, with its own alpha - which is why the white disc
 *    can still be fully opaque while the gradient is invisible.
 */
private fun DrawScope.drawTexturedThumb(
    centreX: Float,
    centreY: Float,
    radius: Float,
    layerAAlpha: Float,
    layerBShadowRadius: Float,
    layerBStrokeWidth: Float,
    layerBFill: Color,
    layerCAlpha: Float,
    layerCColor: Color,
) {
    val left = centreX - radius
    val top = centreY - radius
    val bottom = centreY + radius
    val rectSize = Size(radius * 2f, radius * 2f)
    val corner = CornerRadius(radius, radius)

    val groupAlpha = (layerAAlpha * 255f).roundToInt().coerceIn(0, 255)
    if (groupAlpha > 0) {
        // LayerA - `LinearGradient(0, 0, 0, height, [e6, e6, 8c], [0.0, 0.2, 1.0], CLAMP)`.
        // The gradient is vertical and anchored to the layer rect, so it travels with the thumb.
        val gradient = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = CouiSeekBarDefaults.LayerATopAlpha / 255f),
                Color.White.copy(alpha = CouiSeekBarDefaults.LayerATopAlpha / 255f),
                Color.White.copy(alpha = CouiSeekBarDefaults.LayerABottomAlpha / 255f),
            ),
            start = Offset(left, top),
            end = Offset(left, bottom),
        )
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(left, top),
            size = rectSize,
            cornerRadius = corner,
            alpha = groupAlpha / 255f,
        )

        // LayerB - `getOrCreateLayerBBitmap`: a `size x size` ARGB bitmap (`size = 2r + 24dp`)
        // holding a `#77FFFFFF` disc plus the **inner** half of a white glow.
        //
        // The glow is built by clipping to the disc and then drawing a 10dp STROKE oval whose
        // paint carries `setShadowLayer(radius = 5dp, dx = 0, dy = 0.4dp, color = -1)` - and `-1`
        // is opaque **white**, not black. Clipping discards the outer half of the stroke, so what
        // survives is a soft white ring hugging the inside of the disc. Outside the disc there is
        // nothing at all; the bitmap's 12dp margin stays fully transparent.
        //
        // Compose's common API has no `Paint#setShadowLayer`, so the blurred ring is approximated
        // with concentric strokes whose alpha tapers inwards. This is the one place in this
        // component that is an approximation rather than a port.
        drawCircle(
            color = layerBFill,
            radius = radius,
            center = Offset(centreX, centreY),
            alpha = groupAlpha / 255f,
        )
        // The stroke is 10dp wide and sits centred on the rim, so 5dp of it lands inside; the
        // shadow layer blurs a further 5dp inwards. That 10dp is how deep the glow reaches.
        val glowDepth = layerBStrokeWidth / 2f + layerBShadowRadius
        val glowSteps = 6
        for (step in 0 until glowSteps) {
            val t = (step + 1) / glowSteps.toFloat()
            val ringRadius = radius - glowDepth * (1f - t)
            val ringAlpha = 0.55f * t * (groupAlpha / 255f)
            if (ringRadius <= 0f || ringAlpha <= 0f) continue
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = Offset(centreX, centreY),
                style = Stroke(width = layerBStrokeWidth / glowSteps),
            )
        }
    }

    // LayerC - the plain disc. `drawLayerC` bails out entirely when its alpha rounds to 0.
    val layerCIntAlpha = (layerCAlpha * 255f).roundToInt().coerceIn(0, 255)
    if (layerCIntAlpha > 0) {
        drawRoundRect(
            color = layerCColor,
            topLeft = Offset(left, top),
            size = rectSize,
            cornerRadius = corner,
            alpha = layerCIntAlpha / 255f,
        )
    }
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
}

/** Whether the given travel should be treated as a tap rather than a drag, mirroring the
 * `mTouchSlop` check `COUISeekBarAdvanced.onTouchEvent` performs before it commits to a drag. */
internal fun isWithinTouchSlop(deltaX: Float, touchSlop: Float): Boolean = abs(deltaX) < touchSlop
