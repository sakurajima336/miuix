// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiEasing
import io.github.wfc35286.coui.kmp.theme.CouiTheme

/** The three selection states of a [CouiCheckBox]. */
enum class CouiCheckBoxState(val value: Int) {
    /** Nothing selected. */
    None(0),

    /** Some of the group is selected: the box shows a dash. */
    Part(1),

    /** Everything is selected: the box shows the check mark. */
    All(2),
}

/** The measured values of the check box, from the ColorOS check-box vectors. */
object CouiCheckBoxDefaults {
    /** The side of the drawing canvas the 24-unit vector viewport maps onto. */
    val CanvasSize = 20.dp

    /** The viewport the vector paths are authored in. */
    const val Viewport = 24f

    /** The radius of the press halo behind the box. */
    val RippleRadius = 20.dp

    /** The glyph transition, in milliseconds. */
    const val GlyphDurationMs = 200

    /** The opacity the press halo reaches while held. */
    const val PressHaloAlpha = 0.12f

    /** The next state when the box is tapped. */
    fun nextState(state: CouiCheckBoxState): CouiCheckBoxState = when (state) {
        CouiCheckBoxState.None -> CouiCheckBoxState.All
        CouiCheckBoxState.Part -> CouiCheckBoxState.All
        CouiCheckBoxState.All -> CouiCheckBoxState.None
    }
}

/**
 * A [CouiCheckBox] component with COUI style.
 *
 * A rounded-square check box with the COUI three-state model, drawn with the original vector
 * geometry: the empty box is a 1.3-unit tertiary-coloured outline; the selected box is filled
 * with the theme colour and carries the check mark, and the half-selected box the same fill
 * with a 10x1.6-unit dash - the glyph cross-fading over 200ms on the standard COUI movement
 * curve. A 20dp-radius halo fades in behind the box while it is held.
 *
 * @param state The selection state to show.
 * @param onStateChange Called with the state the user picked by tapping.
 * @param modifier The modifier to apply to the check box.
 * @param enabled Whether the check box can be interacted with.
 */
@Composable
fun CouiCheckBox(
    state: CouiCheckBoxState,
    onStateChange: (CouiCheckBoxState) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val halo by animateFloatAsState(
        targetValue = if (pressed) CouiCheckBoxDefaults.PressHaloAlpha else 0f,
        animationSpec = tween(durationMillis = CouiCheckBoxDefaults.GlyphDurationMs),
        label = "couiCheckBoxHalo",
    )
    val glyph by animateFloatAsState(
        targetValue = if (state == CouiCheckBoxState.None) 0f else 1f,
        animationSpec = tween(
            durationMillis = CouiCheckBoxDefaults.GlyphDurationMs,
            easing = CouiEasing.MoveEase,
        ),
        label = "couiCheckBoxGlyph",
    )

    val fillColor = if (enabled) colors.containerTheme else colors.labelTertiary
    val outlineColor = colors.labelTertiary
    val glyphColor = if (enabled) colors.labelOnColor else colors.labelTertiary

    Box(
        modifier = modifier
            .size(CouiCheckBoxDefaults.RippleRadius * 2)
            .semantics { role = Role.Checkbox }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) { onStateChange(CouiCheckBoxDefaults.nextState(state)) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(CouiCheckBoxDefaults.CanvasSize)) {
            val s = size.width / CouiCheckBoxDefaults.Viewport
            if (halo > 0f) {
                drawCircle(
                    color = fillColor.copy(alpha = halo),
                    radius = with(this) { CouiCheckBoxDefaults.RippleRadius.toPx() },
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                )
            }
            if (glyph < 1f) {
                // The empty box: rounded rect (3.8, 3.8)-(20.2, 20.2), radius 3.2, stroke 1.3.
                drawRoundRect(
                    color = outlineColor.copy(alpha = 1f - glyph),
                    topLeft = Offset(3.8f * s, 3.8f * s),
                    size = Size(16.4f * s, 16.4f * s),
                    cornerRadius = CornerRadius(3.2f * s),
                    style = Stroke(width = 1.3f * s),
                )
            }
            if (glyph > 0f) {
                // The filled box: rounded rect (3, 3)-(21, 21), radius 4.
                drawRoundRect(
                    color = fillColor.copy(alpha = glyph),
                    topLeft = Offset(3f * s, 3f * s),
                    size = Size(18f * s, 18f * s),
                    cornerRadius = CornerRadius(4f * s),
                )
                if (state == CouiCheckBoxState.Part) {
                    // The half-selected dash: rect (7, 11.866) 10 x 1.6.
                    drawRect(
                        color = glyphColor.copy(alpha = glyph),
                        topLeft = Offset(7f * s, 11.866f * s),
                        size = Size(10f * s, 1.6f * s),
                    )
                } else {
                    drawPath(
                        path = checkMarkPath(s),
                        color = glyphColor.copy(alpha = glyph),
                        style = Fill,
                    )
                }
            }
        }
    }
}

/** The check mark, with the original vector's coordinates and curve. */
private fun checkMarkPath(s: Float): Path = Path().apply {
    moveTo(10.831f * s, 14.318f * s)
    lineTo(17.033f * s, 8f * s)
    lineTo(18f * s, 8.985f * s)
    lineTo(11.315f * s, 15.796f * s)
    cubicTo(
        11.047f * s, 16.068f * s,
        10.614f * s, 16.068f * s,
        10.347f * s, 15.796f * s,
    )
    lineTo(7f * s, 12.386f * s)
    lineTo(7.967f * s, 11.401f * s)
    lineTo(10.831f * s, 14.318f * s)
    close()
}
