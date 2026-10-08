// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.motion.couiStateMask
import io.wfc35286.coui.kmp.shape.couiCardPath
import io.wfc35286.coui.kmp.shape.createCouiCardShader
import io.wfc35286.coui.kmp.theme.CouiTheme

/** C17 COUICardListSelectedItemLayout defaults, traced through getInnerRadius/updatePath. */
object CouiCardDefaults {
    /** coui_round_corner_xl_radius_normal_16_1; explicit G2 weight is 2.5. */
    val CornerRadius: Dp = 24.dp

    /** coui_list_card_head_or_tail_padding, at the group's two outside edges. */
    val HeadOrTailPadding: Dp = 2.dp
    val InsideMargin: PaddingValues = PaddingValues(0.dp)
    val HorizontalMargin: Dp = 16.dp

    /** HomepageTopCategory.mPaddingTop -> coui_preference_category_margintop_large. */
    val GroupSpacing: Dp = 16.dp

    @Composable
    fun colors(
        color: Color = CouiTheme.colors.cardBackground,
        contentColor: Color = CouiTheme.colors.labelPrimary,
        pressedColor: Color = CouiTheme.colors.cardPressed,
    ): CouiCardColors = CouiCardColors(color, contentColor, pressedColor)
}

@Immutable
data class CouiCardColors(val color: Color, val contentColor: Color, val pressedColor: Color)

/**
 * A C17 Settings list card. The n=2.5 HWUI mask clips both background and descendants.
 * Outside horizontal margins belong to the caller. Head/tail padding belongs to the first
 * and last preference rows, so their state masks reach the outside edges. [insideMargin]
 * is optional application content padding and defaults to zero.
 */
@Composable
fun CouiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CouiCardDefaults.CornerRadius,
    insideMargin: PaddingValues = CouiCardDefaults.InsideMargin,
    colors: CouiCardColors = CouiCardDefaults.colors(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val shader = remember { createCouiCardShader() }
    Column(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val radius = cornerRadius.roundToPx().toFloat()
                val path = couiCardPath(size, radius)
                val mask = shader?.brush(size, radius, Color.White)
                onDrawWithContent {
                    if (mask != null) {
                        drawRect(colors.color)
                        drawContent()
                        drawRect(mask, blendMode = BlendMode.DstIn)
                    } else {
                        clipPath(path) {
                            drawRect(colors.color)
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
            }
            .couiStateMask(source, enabled = onClick != null)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(insideMargin),
        content = content,
    )
}
