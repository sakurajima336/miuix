// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.wfc35286.coui.kmp.component

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
import io.wfc35286.coui.kmp.theme.CouiTheme
import io.wfc35286.coui.kmp.token.CouiTextStyle
import io.wfc35286.coui.kmp.token.CouiTypography

/**
 * COUI's page top bar - the Compose counterpart of `com.coui.appcompat.toolbar.COUIToolbar`
 * as it is used by ColorOS Settings sub pages.
 *
 * Traced from `设置_17.0.0.apk` (ColorOS 17), not eyeballed:
 *
 * * The page chrome is `res/layout/coui_preference_list_with_appbar_fragment.xml`:
 *   `COUIDividerAppBarLayout` (background `coui_window_bg_with_card`, elevation 0) wrapping a
 *   `COUIToolbar` with `style=COUIToolBarInAppBarLayoutStyle` (0x7f160294), which sets
 *   `layout_height=@dimen/toolbar_min_height`, `paddingTop/Bottom=0dp`.
 * * `dimen/toolbar_min_height = 52dp`.
 * * The title uses `textAppearanceSecondTitle` -> `TextAppearance.COUI.Toolbar.SecondTitle`
 *   (0x7f160689): `textSize=18dp`, `textStyle=normal`, `fontFamily="sans-serif-medium"`,
 *   `textColor=?attr/couiColorPrimaryNeutral` - i.e. [CouiTypography.toolbarTitle] plus
 *   `couiColorPrimaryNeutral`.
 * * Measured on the device (1240x2772, density 2.975, dark theme so the glyphs are light):
 *   bar `y=141..296px` = 52.0dp; the back glyph is a "←" (shaft + head) with ~2dp stroke,
 *   ink `20.2 x 15.5dp`, its 24dp box at `x=24..48dp`; the title ink starts at `x=66dp`.
 *   The bar background is the page background, so no divider is drawn at rest.
 */
object CouiToolbarDefaults {
    /** `dimen/toolbar_min_height`. */
    val Height: Dp = 52.dp

    /** The 24dp glyph box; measured at `x=24..48dp` on the device. */
    val IconSize: Dp = 24.dp
    val IconStart: Dp = 24.dp

    /** Left edge of the title text box (measured: ink starts at 66dp). */
    val TitleStart: Dp = 66.dp

    /** Touch target of the back button; the icon is centred inside it. */
    val BackTouchWidth: Dp = 60.dp

    /** Stroke width of the "←" glyph. */
    val IconStrokeWidth: Dp = 2.dp
}

/**
 * A ColorOS page top bar: back arrow on the left, page title next to it.
 *
 * The bar paints nothing itself - like `COUIDividerAppBarLayout` it lets the page background
 * (`couiColorBackgroundWithCard` / `couiColorBackground`) show through.
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
 * The COUI back glyph: an "←" whose ink is 20.2 x 15.5dp inside the 24dp box, drawn with a
 * 2dp round-capped stroke (measured on the device).
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