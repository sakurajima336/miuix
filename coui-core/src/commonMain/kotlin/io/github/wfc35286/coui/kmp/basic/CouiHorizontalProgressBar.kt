// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The measured values of the horizontal progress bar. */
object CouiHorizontalProgressBarDefaults {
    /** The height of the bar; the corners are half of it, so the bar is a capsule. */
    val Height = 4.dp

    /** The track colour: black at 5% opacity. */
    val TrackColor = Color(0x0C000000)

    /** The progress colour: the COUI green. */
    val ProgressColor = Color(0xFF2AD181)
}

/**
 * A [CouiHorizontalProgressBar] component with COUI style.
 *
 * A capsule track with a green fill that grows with [progress]. The fill grows from the leading
 * edge, following the layout direction.
 *
 * @param progress The progress to show, from 0.0 to 1.0. Values outside the range are clamped.
 * @param modifier The modifier to apply to the bar.
 * @param color The colour of the progress fill.
 * @param trackColor The colour of the track behind the fill.
 * @param height The height of the bar.
 */
@Composable
fun CouiHorizontalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CouiHorizontalProgressBarDefaults.ProgressColor,
    trackColor: Color = CouiHorizontalProgressBarDefaults.TrackColor,
    height: Dp = CouiHorizontalProgressBarDefaults.Height,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(color),
        )
    }
}
