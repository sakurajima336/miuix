// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.theme.CouiTheme

/** The measured values of the circular progress ring. */
object CouiCircularProgressDefaults {
    /** The outer size of the ring. */
    val Size = 40.dp

    /** The empty margin kept around the ring. */
    val Padding = 4.dp

    /** The stroke width of the progress arc. */
    val StrokeWidth = 5.dp

    /**
     * The spring that tracks the drawn progress towards the target: critically damped with a
     * stiffness of 50, so the ring eases after every jump instead of snapping.
     */
    fun <T> progressSpring() = spring<T>(dampingRatio = 1f, stiffness = 50f)
}

/**
 * A [CouiCircularProgress] component with COUI style.
 *
 * A determinate progress ring: the track is a full circle and the progress arc starts at the
 * top and sweeps clockwise. The drawn progress follows the target on a critically damped
 * stiffness-50 spring, and the arc and track stroke widths are independent, so a caller can
 * thicken one without the other.
 *
 * @param progress The progress to show, from 0.0 to 1.0. Values outside the range are clamped.
 * @param modifier The modifier to apply to the ring.
 * @param color The colour of the progress arc.
 * @param trackColor The colour of the full-circle track.
 * @param strokeWidth The stroke width of the progress arc.
 * @param trackStrokeWidth The stroke width of the track.
 * @param size The outer size of the ring.
 */
@Composable
fun CouiCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CouiTheme.colors.hintNeutral,
    trackColor: Color = CouiTheme.colors.divider,
    strokeWidth: Dp = CouiCircularProgressDefaults.StrokeWidth,
    trackStrokeWidth: Dp = CouiCircularProgressDefaults.StrokeWidth,
    size: Dp = CouiCircularProgressDefaults.Size,
) {
    val target = progress.coerceIn(0f, 1f)
    val visualProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = CouiCircularProgressDefaults.progressSpring(),
        label = "couiCircularVisualProgress",
    )
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val inset = CouiCircularProgressDefaults.Padding.toPx()
        val stroke = strokeWidth.toPx()
        val trackStroke = trackStrokeWidth.toPx()
        val radius = (minOf(this.size.width, this.size.height) - inset * 2f - maxOf(stroke, trackStroke)) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = trackStroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = visualProgress * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
