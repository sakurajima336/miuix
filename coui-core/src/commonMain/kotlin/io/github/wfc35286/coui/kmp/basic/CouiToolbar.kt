// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import io.github.wfc35286.coui.kmp.token.CouiTypography


/**
 * Defaults for [CouiToolbar], matching the ColorOS page top bar: a 52dp bar, a 24dp back-glyph
 * box and the title starting 66dp from the left edge.
 */
object CouiToolbarDefaults {
    /** The bar height. */
    val Height: Dp = 52.dp

    /** The back glyph's drawing box. */
    val IconSize: Dp = 24.dp
    val IconStart: Dp = 24.dp

    /** Left edge of the title text box. */
    val TitleStart: Dp = 66.dp

    /** Touch target of the back button; the icon is centred inside it. */
    val BackTouchWidth: Dp = 60.dp

    /** Stroke width of the "←" glyph. */
    val IconStrokeWidth: Dp = 2.dp
}

/**
 * A page top bar: a back arrow on the left and the page title next to it.
 *
 * The bar paints no background of its own - the page background shows through, so no divider is
 * drawn at rest.
 *
 * @param title The page title.
 * @param onBack Invoked when the back arrow is clicked; pass `null` for a bar without a back
 *   arrow (the title then starts at [CouiToolbarDefaults.IconStart]).
 */
@Composable
fun CouiToolbar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleStyle: CouiTextStyle = CouiTypography.toolbarTitle,
) {
    val contentColor = CouiTheme.colors.primaryNeutral
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CouiToolbarDefaults.Height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(CouiToolbarDefaults.BackTouchWidth)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                CouiBackArrow(
                    color = contentColor,
                    modifier = Modifier
                        .padding(start = CouiToolbarDefaults.IconStart)
                        .size(CouiToolbarDefaults.IconSize),
                )
            }
        } else {
            Spacer(Modifier.width(CouiToolbarDefaults.IconStart))
        }
        CouiText(
            text = title,
            style = titleStyle,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                start = (CouiToolbarDefaults.TitleStart - CouiToolbarDefaults.BackTouchWidth).coerceAtLeast(0.dp),
            ),
        )
        Spacer(Modifier.weight(1f))
    }
}

/**
 * The back arrow glyph: an "←" filling most of its 24dp box, drawn with a round-capped stroke
 * whose width is [CouiToolbarDefaults.IconStrokeWidth].
 */
@Composable
fun CouiBackArrow(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val unit = size.width / 24f
        val stroke = Stroke(
            width = CouiToolbarDefaults.IconStrokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val tipX = 1.9f * unit
        val centerY = 12f * unit
        val shaftEndX = 22.1f * unit
        val headTopY = 4.3f * unit
        val headBottomY = 19.8f * unit
        val headArmX = 7.6f * unit

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(tipX, centerY),
            end = androidx.compose.ui.geometry.Offset(shaftEndX, centerY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(headArmX, headTopY)
            lineTo(tipX, centerY)
            lineTo(headArmX, headBottomY)
        }
        drawPath(head, color = color, style = stroke)
    }
}
