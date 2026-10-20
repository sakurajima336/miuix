// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiPressStyle
import io.github.wfc35286.coui.kmp.motion.couiPressFeedback
import io.github.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.github.wfc35286.coui.kmp.motion.rememberCouiPressMaskProgress
import io.github.wfc35286.coui.kmp.shape.CouiCapsulePathCache
import io.github.wfc35286.coui.kmp.shape.createCouiCapsuleShader
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTypography
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * `COUIButton$ButtonType` - the button variants COUI ships.
 *
 * The integer values are the ones the enum carries (`ButtonType.mValue`), so a style that says
 * `buttonType=secondary` resolves to the same variant here.
 */
enum class CouiButtonType(val value: Int) {
    /** `PRIMARY` - filled with the theme accent. */
    Primary(1),

    /** `SECONDARY` - filled with `couiColorContainer8`. */
    Secondary(2),

    /** `TRANSPARENT` - the "translate" variant, a translucent white over imagery. */
    Transparent(3),

    /** `AI_PAYMENT` - the dark navy AI-payment pill. */
    AiPayment(4),

    /** `AI_PAYMENT_SECONDARY`. */
    AiPaymentSecondary(5),
}

/**
 * The two sizes the standard styles define: `Widget.COUI.Button.Large` and
 * `Widget.COUI.Button.Small`.
 */
enum class CouiButtonSize {
    /** 44dp tall, 132dp minimum width, 16sp text. */
    Large,

    /** 28dp tall, 52dp minimum width, 14sp text. */
    Small,
}

/**
 * The measured values behind [CouiButton], all read from the ColorOS 17.0.0 resource table.
 *
 * Sources: `Widget.COUI.Button{.Large,.Small,.Large.ButtonNew,.Small.ButtonNew}` in
 * `res/values/styles.xml` and the `coui_btn_*` / `coui_*btn*` dimens.
 */
object CouiButtonDefaults {
    /** `coui_btn_large_height_min`. */
    val LargeHeight: Dp = 44.dp

    /** `coui_btn_large_width_min`. */
    val LargeMinWidth: Dp = 132.dp

    /** `Widget.COUI.Button.Large`'s `paddingLeft` / `paddingRight`. */
    val LargePaddingHorizontal: Dp = 12.dp

    /** `coui_btn_padding_vertical`. */
    val LargePaddingVertical: Dp = 8.dp

    /** `coui_btn_small_height_min`. */
    val SmallHeight: Dp = 28.dp

    /** `coui_btn_small_width_min`. */
    val SmallMinWidth: Dp = 52.dp

    /** `Widget.COUI.Button.Small`'s `paddingLeft` / `paddingRight`. */
    val SmallPaddingHorizontal: Dp = 12.dp

    /** `Widget.COUI.Button.Small`'s `paddingTop` / `paddingBottom`. */
    val SmallPaddingVertical: Dp = 4.dp

    /**
     * The aspect ratio at which `COUIPressAnimatorUnit.useCandyMaterialRealtime()` switches from
     * the jelly springs to the candy ones.
     */
    const val CandyAspectRatio: Float = 2f

    /** The padding for [CouiButtonSize.Large]. */
    val LargePadding: PaddingValues =
        PaddingValues(horizontal = LargePaddingHorizontal, vertical = LargePaddingVertical)

    /** The padding for [CouiButtonSize.Small]. */
    val SmallPadding: PaddingValues =
        PaddingValues(horizontal = SmallPaddingHorizontal, vertical = SmallPaddingVertical)

    /** The text style for [CouiButtonSize.Large] - `couiTextAppearanceButtonL` (16sp, medium). */
    val LargeTextStyle = CouiTypography.buttonLarge

    /** The text style for [CouiButtonSize.Small] - `couiTextAppearanceButton` (14sp, medium). */
    val SmallTextStyle = CouiTypography.button

    /** The minimum height for the given [size]. */
    fun minHeight(size: CouiButtonSize): Dp = when (size) {
        CouiButtonSize.Large -> LargeHeight
        CouiButtonSize.Small -> SmallHeight
    }

    /** The minimum width for the given [size]. */
    fun minWidth(size: CouiButtonSize): Dp = when (size) {
        CouiButtonSize.Large -> LargeMinWidth
        CouiButtonSize.Small -> SmallMinWidth
    }

    /** The padding for the given [size]. */
    fun padding(size: CouiButtonSize): PaddingValues = when (size) {
        CouiButtonSize.Large -> LargePadding
        CouiButtonSize.Small -> SmallPadding
    }

    /** The type-scale entry for the given [size]. */
    fun textStyle(size: CouiButtonSize) = when (size) {
        CouiButtonSize.Large -> LargeTextStyle
        CouiButtonSize.Small -> SmallTextStyle
    }

    /**
     * The button's fill, i.e. `COUIButton.mDrawableColor` / `mDisabledColor`.
     *
     * Those come straight out of the style: `drawableColor` and `disabledColor`. The `primary`
     * pair is `couiColorContainerTheme` / `couiColorDisable`, the `secondary` pair is
     * `couiColorContainer8` twice (the secondary button does *not* change its fill when
     * disabled - only its text fades).
     */
    @Composable
    fun containerColor(type: CouiButtonType, enabled: Boolean = true): Color {
        val colors = CouiTheme.colors
        return when (type) {
            CouiButtonType.Primary -> if (enabled) colors.containerTheme else colors.disable
            CouiButtonType.Secondary -> colors.container8
            CouiButtonType.Transparent -> TransparentContainer
            CouiButtonType.AiPayment -> if (enabled) colors.aiPaymentPrimary else colors.aiPaymentPrimaryDisabled
            CouiButtonType.AiPaymentSecondary ->
                if (enabled) colors.aiPaymentSecondary else colors.aiPaymentSecondaryDisabled
        }
    }

    /**
     * The button's text colour, i.e. the `coui_btn_*_text_color` `ColorStateList`s.
     *
     * The selectors only branch on `state_enabled`; `state_pressed` and `state_selected` resolve
     * to the same colour as the default branch, so pressing does not change the text colour.
     */
    @Composable
    fun contentColor(type: CouiButtonType, enabled: Boolean = true): Color {
        val colors = CouiTheme.colors
        return when (type) {
            CouiButtonType.Primary ->
                if (enabled) colors.labelOnColor else colors.couiBtnDefaultTextColorDisable

            CouiButtonType.Secondary ->
                if (enabled) colors.labelPrimary else colors.couiBtnSecondaryTextColorDisable

            // `coui_btn_tiny_translate_text_color`: `?couiColorLabelOnColor` when enabled,
            // `@color/coui_color_label_tertiary_dark` (#4DFFFFFF) when disabled.
            CouiButtonType.Transparent -> if (enabled) colors.labelOnColor else TransparentDisabledText

            CouiButtonType.AiPayment ->
                if (enabled) colors.aiPaymentPrimaryText else colors.aiPaymentPrimaryTextDisabled

            CouiButtonType.AiPaymentSecondary ->
                if (enabled) colors.aiPaymentSecondaryText else colors.aiPaymentSecondaryTextDisabled
        }
    }

    /** `coui_button_translate_color` - `#33FFFFFF`, no `-night` variant. */
    private val TransparentContainer = Color(0x33FFFFFF)

    /** `coui_color_label_tertiary_dark` - `#4DFFFFFF`, no `-night` variant. */
    private val TransparentDisabledText = Color(0x4DFFFFFF)
}

/**
 * A COUI button.
 *
 * This is the Compose counterpart of `com.coui.appcompat.button.COUIButton`, a
 * `AppCompatButton` subclass that draws its own background, press mask and stroke. The geometry
 * is a pill: both standard styles set `drawableRadius = -1.0dp`, and `getDrawableRadius()`
 * resolves a negative radius to `height / 2` on ColorOS 16 and later.
 *
 * Press feedback has two independent halves, exactly as in the original: a translucent
 * `couiColorPress` mask drawn over the fill (`COUIMaskEffectDrawable`, spring 0.3/0), and a
 * scale whose amount depends on the button's measured size (`PressCalc` /
 * `PressPhysicsEngine`). The spring used for the scale is picked by aspect ratio, matching
 * `COUIPressAnimatorUnit.useCandyMaterialRealtime()`.
 *
 * @param text The button label.
 * @param onClick Called when the button is clicked.
 * @param modifier The modifier to apply.
 * @param type Which of the COUI button variants to draw.
 * @param size Large (44dp) or Small (28dp).
 * @param enabled Whether the button can be interacted with.
 * @param haptic Whether to fire the granular haptic on touch down and up. The real button only
 *   does this when the style sets `needVibrate`, and every standard button style leaves it
 *   `false`, so this defaults to off.
 */
@Composable
fun CouiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: CouiButtonType = CouiButtonType.Primary,
    size: CouiButtonSize = CouiButtonSize.Large,
    enabled: Boolean = true,
    haptic: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberCouiHaptics()
    val pressed by interactionSource.collectIsPressedAsState()

    // The press spring is chosen from the *measured* aspect ratio, so the size has to be known
    // before the modifier is built. Both springs share the same 0.85..0.97 scale range, so the
    // choice only affects the feel, never the target.
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }
    val pressStyle = remember(buttonSize) {
        val width = buttonSize.width.toFloat()
        val height = buttonSize.height.toFloat()
        if (width > 0f && height > 0f &&
            max(width, height) / min(width, height) >= CouiButtonDefaults.CandyAspectRatio
        ) {
            CouiPressStyle.Candy
        } else {
            CouiPressStyle.Jelly
        }
    }

    if (haptic) {
        LaunchedEffect(interactionSource, enabled) {
            if (!enabled) return@LaunchedEffect
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    // `onTouchEvent` calls `performHapticFeedback()` on ACTION_DOWN...
                    is PressInteraction.Press -> haptics.granularShort()
                    // ...and again on ACTION_UP / ACTION_CANCEL.
                    is PressInteraction.Release, is PressInteraction.Cancel -> haptics.granularShort()
                }
            }
        }
    }

    val maskProgress = rememberCouiPressMaskProgress(interactionSource, enabled)
    val containerColor = CouiButtonDefaults.containerColor(type, enabled)
    val contentColor = CouiButtonDefaults.contentColor(type, enabled)
    val pressMaskColor = CouiTheme.colors.press
    // The official silhouette is libhwui's `sdCapsule` distance field (see the drawBehind note).
    // The runtime shader is this project's exact port of it; the size-keyed path is the analytic
    // fallback for platforms that cannot run the shader.
    val shapeShader = remember { createCouiCapsuleShader() }
    val shapeCache = remember { CouiCapsulePathCache() }

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = CouiButtonDefaults.minWidth(size),
                minHeight = CouiButtonDefaults.minHeight(size),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .couiPressFeedback(
                interactionSource = interactionSource,
                enabled = enabled,
                style = pressStyle,
            )
            .onSizeChanged { buttonSize = it }
            .drawBehind {
                // Both standard button styles set `drawableRadius = -1.0dp`, and
                // `getDrawableRadius()` resolves a negative radius to `height / 2` on ColorOS 16+,
                // i.e. a full-round rectangle. libhwui's `g2RoundRectMask` selects the `sdCapsule`
                // shader once the radius reaches half the short side, so the official silhouette is
                // a capsule whose straight-edge/cap junction is softened - not a circular arc, and
                // not the COUIShapePath cubic table (that one is only reached when
                // `supportAddOnSmoothRound()` is false, which it is not on C17).
                val mask = maskProgress.value
                // `onDraw` paints the fill first and then composites the translucent press mask over
                // it; `getAnimatorColor()` does exactly this with `ColorUtils.compositeColors`.
                // Both layers are flat colours, so pre-compositing them is pixel-identical and lets
                // the silhouette be drawn once - which the shader path requires, because a single
                // instance's uniforms are mutable and two draws through it would overwrite each
                // other.
                val fill = if (mask > 0f) {
                    compositeOver(pressMaskColor.copy(alpha = pressMaskColor.alpha * mask), containerColor)
                } else {
                    containerColor
                }
                if (shapeShader != null) {
                    drawRect(brush = shapeShader.brush(this.size, fill))
                } else {
                    drawPath(shapeCache.pathFor(this.size), fill)
                }
            }
            .padding(CouiButtonDefaults.padding(size)),
        contentAlignment = Alignment.Center,
    ) {
        CouiText(
            text = text,
            style = CouiButtonDefaults.textStyle(size),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * `androidx.core.graphics.ColorUtils.compositeColors(foreground, background)` - source-over
 * compositing of two flat colours.
 *
 * `COUIButton.getAnimatorColor()` composites the mask over `mDrawableColor` with this exact
 * integer arithmetic, so pre-compositing the press mask into the fill here produces the same
 * colour the official ends up with, in one draw.
 */
private fun compositeOver(foreground: Color, background: Color): Color {
    val fgAlpha = (foreground.alpha * 255f).roundToInt().coerceIn(0, 255)
    val bgAlpha = (background.alpha * 255f).roundToInt().coerceIn(0, 255)
    if (fgAlpha == 0) return background
    val alpha = 0xFF - (0xFF - bgAlpha) * (0xFF - fgAlpha) / 0xFF
    if (alpha == 0) return Color.Transparent
    fun channel(fg: Float, bg: Float): Float {
        val fgComponent = (fg * 255f).roundToInt().coerceIn(0, 255)
        val bgComponent = (bg * 255f).roundToInt().coerceIn(0, 255)
        val value = (0xFF * fgComponent * fgAlpha + bgComponent * bgAlpha * (0xFF - fgAlpha)) / (alpha * 0xFF)
        return value / 255f
    }
    return Color(
        red = channel(foreground.red, background.red),
        green = channel(foreground.green, background.green),
        blue = channel(foreground.blue, background.blue),
        alpha = alpha / 255f,
    )
}
