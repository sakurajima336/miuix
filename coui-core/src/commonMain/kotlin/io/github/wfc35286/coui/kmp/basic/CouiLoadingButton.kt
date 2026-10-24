// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/** The measured values of the loading dots. */
object CouiLoadingButtonDefaults {
    /** The radius of one dot. */
    val DotRadius = 1.dp

    /** The gap between two dots. */
    val DotSpacing = 2.dp

    /** The length of one full pulse cycle, in milliseconds. */
    const val PeriodMs = 1332f

    /** How much later each following dot starts its cycle, in milliseconds. */
    const val PhaseShiftMs = 333f

    /** The opacity a dot rests at between waves. */
    const val RestAlpha = 51f / 255f

    /** The intermediate opacity reached on the way to and from the peak. */
    const val MidAlpha = 127.5f / 255f

    /** The peak opacity of a wave. */
    const val PeakAlpha = 1f

    /** The opacity of dot [index] at [elapsedMs] into the animation. */
    fun dotAlpha(elapsedMs: Float, index: Int): Float {
        val phase = (elapsedMs + index * PhaseShiftMs) % PeriodMs
        return when {
            phase < 133f -> lerp(RestAlpha, MidAlpha, phase / 133f)
            phase < 200f -> lerp(MidAlpha, PeakAlpha, (phase - 133f) / 67f)
            phase < 467f -> PeakAlpha
            phase < 533f -> lerp(PeakAlpha, MidAlpha, (phase - 467f) / 66f)
            phase < 666f -> lerp(MidAlpha, RestAlpha, (phase - 533f) / 133f)
            else -> RestAlpha
        }
    }

    private fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction
}

/**
 * A [CouiLoadingButton] component with COUI style.
 *
 * A button that swaps its label for a pulsing three-dot loader while [loading] is true. Each dot
 * fades from 20% opacity up to full and back over a 1332ms cycle, the dots phase-shifted by
 * 333ms so the wave travels left to right. The button keeps its size while loading and swallows
 * clicks until the load finishes.
 *
 * @param text The button label, shown while not loading.
 * @param loading Whether the loader is shown in place of the label.
 * @param onClick Called when the button is clicked and not loading.
 * @param modifier The modifier to apply to the button.
 * @param type Which of the COUI button variants to draw.
 * @param size The standard button size.
 * @param enabled Whether the button can be interacted with.
 */
@Composable
fun CouiLoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: CouiButtonType = CouiButtonType.Primary,
    size: CouiButtonSize = CouiButtonSize.Large,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        CouiButton(
            text = if (loading) "" else text,
            onClick = onClick,
            type = type,
            size = size,
            enabled = enabled && !loading,
        )
        if (loading) {
            val dotColor = CouiButtonDefaults.contentColor(type, enabled)
            val elapsedMs by produceState(0f, loading) {
                if (loading) {
                    val start = withFrameNanos { it }
                    while (true) {
                        withFrameNanos { frame -> value = (frame - start) / 1_000_000f }
                    }
                }
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                val radius = CouiLoadingButtonDefaults.DotRadius.toPx()
                val spacing = CouiLoadingButtonDefaults.DotSpacing.toPx()
                val totalWidth = radius * 6f + spacing * 2f
                val centerY = this.size.height / 2f
                val startX = (this.size.width - totalWidth) / 2f + radius
                for (index in 0..2) {
                    val alpha = CouiLoadingButtonDefaults.dotAlpha(elapsedMs, index)
                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(startX + index * (radius * 2f + spacing), centerY),
                    )
                }
            }
        }
    }
}
