// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.token.CouiTypography
import kotlin.math.roundToInt

/** The side of the anchor a [CouiTooltip] sits on. */
enum class CouiTooltipDirection {
    /** Above the anchor, arrow pointing down. */
    Up,

    /** Below the anchor, arrow pointing up. */
    Down,

    /** Left of the anchor, arrow pointing right. */
    Left,

    /** Right of the anchor, arrow pointing left. */
    Right,
}

/** The chrome and motion values of the tooltip bubble. */
object CouiTooltipDefaults {
    /** The spring that scales the bubble in and out: 0.4s response with a light bounce. */
    val ScaleSpring = CouiSpringData(bounce = 0.2f, response = 0.4f)

    /** The spring that fades the bubble: 0.3s response with the same light bounce. */
    val AlphaSpring = CouiSpringData(bounce = 0.2f, response = 0.3f)

    /** The bubble fill: the COUI tips blue. */
    val BubbleColor = Color(0xFF0080FF)

    /** The text colour on the bubble. */
    val TextColor = Color.White

    /** The bubble's corner radius. */
    val CornerRadius = 12.dp

    /** The width of the arrow at its base. */
    val ArrowWidth = 10.dp

    /** The band the arrow sits in, between the bubble plate and the anchor. */
    val ArrowBandHeight = 13.dp

    /** The gap kept between the arrow tip and the anchor. */
    val AnchorGap = 4.dp

    /** The bubble's inner padding. */
    val ContentPadding = 13.dp

    /** The bubble text style. */
    val messageStyle = CouiTypography.body
}

/**
 * A [CouiTooltip] component with COUI style.
 *
 * A blue bubble with an arrow pointing at its anchor, shown in a popup window. The bubble
 * scales from the arrow - the pivot sits on the arrow tip, so the bubble appears to grow out
 * of the anchor - on a 0.4s spring with a light bounce, while its alpha runs a slightly
 * quicker 0.3s one. [direction] picks which side of the anchor the bubble sits on.
 *
 * @param visible Whether the tooltip is shown. Setting it to `false` plays the scale-down and
 *   then removes the tooltip.
 * @param text The tooltip message.
 * @param anchorBounds The bounds of the anchor view in window coordinates, as reported by
 *   `onGloballyPositioned { boundsInWindow() }`.
 * @param direction The side of the anchor the bubble sits on.
 * @param onDismissRequest Called when the tooltip is dismissed by the back gesture or a tap
 *   outside.
 * @param modifier The modifier to apply to the bubble.
 */
@Composable
fun CouiTooltip(
    visible: Boolean,
    text: String,
    anchorBounds: androidx.compose.ui.geometry.Rect?,
    direction: CouiTooltipDirection,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var composed by remember { mutableStateOf(visible) }
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            composed = true
            scale.animateTo(1f, CouiTooltipDefaults.ScaleSpring.toSpringSpec())
            alpha.animateTo(1f, CouiTooltipDefaults.AlphaSpring.toSpringSpec())
        } else if (composed) {
            scale.animateTo(0f, CouiTooltipDefaults.ScaleSpring.toSpringSpec())
            alpha.animateTo(0f, CouiTooltipDefaults.AlphaSpring.toSpringSpec())
            composed = false
        }
    }
    if (!composed || anchorBounds == null) return

    val transformOrigin = when (direction) {
        CouiTooltipDirection.Up -> TransformOrigin(0.5f, 1f)
        CouiTooltipDirection.Down -> TransformOrigin(0.5f, 0f)
        CouiTooltipDirection.Left -> TransformOrigin(1f, 0.5f)
        CouiTooltipDirection.Right -> TransformOrigin(0f, 0.5f)
    }
    val density = LocalDensity.current
    val positionProvider = remember(direction, density) {
        with(density) {
            CouiTooltipPositionProvider(
                direction = direction,
                gapPx = CouiTooltipDefaults.AnchorGap.roundToPx(),
                arrowPx = CouiTooltipDefaults.ArrowBandHeight.roundToPx(),
            )
        }
    }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = positionProvider,
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                    this.transformOrigin = transformOrigin
                }
                .drawBehind { drawBubble(direction) }
                .padding(
                    horizontal = when (direction) {
                        CouiTooltipDirection.Left, CouiTooltipDirection.Right -> CouiTooltipDefaults.ContentPadding + CouiTooltipDefaults.ArrowBandHeight
                        else -> CouiTooltipDefaults.ContentPadding
                    },
                    vertical = when (direction) {
                        CouiTooltipDirection.Up, CouiTooltipDirection.Down -> CouiTooltipDefaults.ContentPadding + CouiTooltipDefaults.ArrowBandHeight
                        else -> CouiTooltipDefaults.ContentPadding
                    },
                ),
        ) {
            CouiText(
                text = text,
                style = CouiTooltipDefaults.messageStyle,
                color = CouiTooltipDefaults.TextColor,
            )
        }
    }
}

/** Draws the bubble plate plus the arrow on the anchor-facing side, in one pass. */
private fun DrawScope.drawBubble(direction: CouiTooltipDirection) {
    val corner = CornerRadius(CouiTooltipDefaults.CornerRadius.toPx())
    val band = CouiTooltipDefaults.ArrowBandHeight.toPx()
    val halfArrow = CouiTooltipDefaults.ArrowWidth.toPx() / 2f
    val color = CouiTooltipDefaults.BubbleColor
    when (direction) {
        CouiTooltipDirection.Up -> {
            // The arrow sits at the bottom edge, centred.
            drawRoundRect(color = color, size = Size(size.width, size.height - band), cornerRadius = corner)
            drawTriangle(
                color = color,
                a = Offset(size.width / 2f, size.height),
                b = Offset(size.width / 2f - halfArrow, size.height - band),
                c = Offset(size.width / 2f + halfArrow, size.height - band),
            )
        }
        CouiTooltipDirection.Down -> {
            drawRoundRect(color = color, topLeft = Offset(0f, band), size = Size(size.width, size.height - band), cornerRadius = corner)
            drawTriangle(
                color = color,
                a = Offset(size.width / 2f, 0f),
                b = Offset(size.width / 2f - halfArrow, band),
                c = Offset(size.width / 2f + halfArrow, band),
            )
        }
        CouiTooltipDirection.Left -> {
            drawRoundRect(color = color, size = Size(size.width - band, size.height), cornerRadius = corner)
            drawTriangle(
                color = color,
                a = Offset(size.width, size.height / 2f),
                b = Offset(size.width - band, size.height / 2f - halfArrow),
                c = Offset(size.width - band, size.height / 2f + halfArrow),
            )
        }
        CouiTooltipDirection.Right -> {
            drawRoundRect(color = color, topLeft = Offset(band, 0f), size = Size(size.width - band, size.height), cornerRadius = corner)
            drawTriangle(
                color = color,
                a = Offset(0f, size.height / 2f),
                b = Offset(band, size.height / 2f - halfArrow),
                c = Offset(band, size.height / 2f + halfArrow),
            )
        }
    }
}

private fun DrawScope.drawTriangle(color: Color, a: Offset, b: Offset, c: Offset) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        close()
    }
    drawPath(path, color)
}

/** Places the popup so the bubble's arrow tip meets the anchor's edge, with the anchor gap. */
private class CouiTooltipPositionProvider(
    private val direction: CouiTooltipDirection,
    private val gapPx: Int,
    private val arrowPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize,
    ): androidx.compose.ui.unit.IntOffset {
        val horizontalCenter = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val verticalCenter = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val spacing = gapPx + arrowPx
        return when (direction) {
            CouiTooltipDirection.Up ->
                androidx.compose.ui.unit.IntOffset(horizontalCenter, anchorBounds.top - popupContentSize.height - spacing)
            CouiTooltipDirection.Down ->
                androidx.compose.ui.unit.IntOffset(horizontalCenter, anchorBounds.bottom + spacing)
            CouiTooltipDirection.Left ->
                androidx.compose.ui.unit.IntOffset(anchorBounds.left - popupContentSize.width - spacing, verticalCenter)
            CouiTooltipDirection.Right ->
                androidx.compose.ui.unit.IntOffset(anchorBounds.right + spacing, verticalCenter)
        }
    }
}
