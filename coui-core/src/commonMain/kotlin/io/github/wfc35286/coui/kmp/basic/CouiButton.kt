// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
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
 * The button variants.
 *
 * The integer values match the style attribute values, so a style that says
 * `buttonType=secondary` resolves to the same variant here.
 */
enum class CouiButtonType(val value: Int) {
    /** Filled with the theme accent. */
    Primary(1),

    /** Filled with the secondary container colour. */
    Secondary(2),

    /** A translucent white overlay for use over imagery. */
    Transparent(3),

    /** The dark navy AI-payment pill. */
    AiPayment(4),

    /** The secondary AI-payment pill. */
    AiPaymentSecondary(5),
}

/** The two standard button sizes. */
enum class CouiButtonSize {
    /** 44dp tall, 132dp minimum width, 16sp text. */
    Large,

    /** 28dp tall, 52dp minimum width, 14sp text. */
    Small,
}

/** The measured values behind [CouiButton]. */
object CouiButtonDefaults {
    /** The minimum height of a large button. */
    val LargeHeight: Dp = 44.dp

    /** The minimum width of a large button. */
    val LargeMinWidth: Dp = 132.dp

    /** The large button's horizontal padding. */
    val LargePaddingHorizontal: Dp = 12.dp

    /** The large button's vertical padding. */
    val LargePaddingVertical: Dp = 8.dp

    /** The minimum height of a small button. */
    val SmallHeight: Dp = 28.dp

    /** The minimum width of a small button. */
    val SmallMinWidth: Dp = 52.dp

    /** The small button's horizontal padding. */
    val SmallPaddingHorizontal: Dp = 12.dp

    /** The small button's vertical padding. */
    val SmallPaddingVertical: Dp = 4.dp

    /**
     * The aspect ratio at which the press scale switches from the jelly springs to the candy ones.
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
     * The button's fill.
     *
     * The primary button uses the theme accent and fades when disabled; the secondary button
     * keeps its container fill when disabled - only its text fades.
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
     * The button's text colour.
     *
     * The colour only depends on the enabled state - pressing does not change the text colour.
     */
    @Composable
    fun contentColor(type: CouiButtonType, enabled: Boolean = true): Color {
        val colors = CouiTheme.colors
        return when (type) {
            CouiButtonType.Primary ->
                if (enabled) colors.labelOnColor else colors.couiBtnDefaultTextColorDisable

            CouiButtonType.Secondary ->
                if (enabled) colors.labelPrimary else colors.couiBtnSecondaryTextColorDisable

            // Label-on colour when enabled, tertiary label when disabled.
            CouiButtonType.Transparent -> if (enabled) colors.labelOnColor else TransparentDisabledText

            CouiButtonType.AiPayment ->
                if (enabled) colors.aiPaymentPrimaryText else colors.aiPaymentPrimaryTextDisabled

            CouiButtonType.AiPaymentSecondary ->
                if (enabled) colors.aiPaymentSecondaryText else colors.aiPaymentSecondaryTextDisabled
        }
    }

    /** The translucent white fill of the [CouiButtonType.Transparent] variant. */
    private val TransparentContainer = Color(0x33FFFFFF)

    /** The disabled text colour of the [CouiButtonType.Transparent] variant. */
    private val TransparentDisabledText = Color(0x4DFFFFFF)
}

/**
 * A [CouiButton] component with COUI style.
 *
 * The geometry is a pill: the corner radius resolves to half the button's height, producing a
 * full-round silhouette.
 *
 * Press feedback has two independent halves: a translucent press mask drawn over the fill on a
 * response-0.3/bounce-0 spring, and a scale whose amount depends on the button's measured size.
 * The spring used for the scale is picked by aspect ratio (see [CandyAspectRatio]).
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
                // The silhouette is a full-round rectangle: a capsule whose straight-edge/cap
                // junction is softened, not a plain circular arc.
                val mask = maskProgress.value
                // The fill is painted first and the translucent press mask is composited over it.
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
 * Source-over compositing of two flat colours, matching
 * `androidx.core.graphics.ColorUtils.compositeColors(foreground, background)`.
 *
 * The press mask is composited into the fill with this exact integer arithmetic, so both layers
 * can be drawn once as a single flat colour.
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
