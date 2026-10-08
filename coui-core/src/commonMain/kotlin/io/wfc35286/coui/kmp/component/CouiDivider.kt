// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.theme.CouiTheme

object CouiDividerDefaults {
    /** COUIDividerItemDecoration: max(1, round(getDimension(coui_list_divider_height))). */
    val Thickness: Dp = 0.33000004.dp
    val Inset: Dp = 32.dp
}

/** A standalone pixel-aligned divider. Preference rows overlay it without adding row height. */
@Composable
@NonRestartableComposable
fun CouiDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = CouiDividerDefaults.Thickness,
    color: Color = CouiTheme.colors.divider,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
) {
    val height = with(LocalDensity.current) { thickness.roundToPx().coerceAtLeast(1).toDp() }
    Box(
        modifier = modifier.fillMaxWidth().height(height).drawBehind {
            drawCouiDivider(color, thickness, startIndent, endIndent, 0f)
        },
    )
}

internal fun DrawScope.drawCouiDivider(color: Color, thickness: Dp, start: Dp, end: Dp, y: Float) {
    val startPx = start.roundToPx().toFloat()
    val endPx = end.roundToPx().toFloat()
    val left = if (layoutDirection == LayoutDirection.Ltr) startPx else endPx
    val right = size.width - if (layoutDirection == LayoutDirection.Ltr) endPx else startPx
    if (right > left) {
        drawRect(color, Offset(left, y.toInt().toFloat()), Size(right - left, thickness.roundToPx().coerceAtLeast(1).toFloat()))
    }
}
