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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

/** The measured values of the check box. */
object CouiCheckBoxDefaults {
    /** The side of the box. */
    val BoxSize = 20.dp

    /** The corner radius of the box. */
    val CornerRadius = 6.dp

    /** The stroke width of the empty box's border and the glyph. */
    val StrokeWidth = 2.dp

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
 * A rounded-square check box with the COUI three-state model: [CouiCheckBoxState.None] draws
 * only the border, [CouiCheckBoxState.All] fills the box and draws the check mark, and
 * [CouiCheckBoxState.Part] - the "some of the group is selected" state - fills the box and
 * draws a dash. The glyph cross-fades on a 200ms movement curve, and a 20dp-radius halo fades
 * in behind the box while it is held.
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

    val borderColor =
        if (enabled) colors.labelSecondaryVariant else colors.labelTertiary
    val fillColor = if (enabled) colors.containerTheme else colors.labelTertiary
    val glyphColor = if (enabled) colors.labelOnColor else colors.background

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
        Canvas(
            modifier = Modifier.size(CouiCheckBoxDefaults.RippleRadius * 2),
        ) {
            if (halo > 0f) {
                drawCircle(
                    color = fillColor.copy(alpha = halo),
                    radius = CouiCheckBoxDefaults.RippleRadius.toPx(),
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                )
            }
            val boxSide = CouiCheckBoxDefaults.BoxSize.toPx()
            val topLeft = Offset((this.size.width - boxSide) / 2f, (this.size.height - boxSide) / 2f)
            val boxSize = Size(boxSide, boxSide)
            val corner = CornerRadius(CouiCheckBoxDefaults.CornerRadius.toPx())
            val stroke = CouiCheckBoxDefaults.StrokeWidth.toPx()

            if (glyph < 1f) {
                drawRoundRect(
                    color = borderColor.copy(alpha = 1f - glyph),
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                    style = Stroke(width = stroke),
                )
            }
            if (glyph > 0f) {
                drawRoundRect(
                    color = fillColor.copy(alpha = glyph),
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                )
                val glyphAlpha = glyph.coerceIn(0f, 1f)
                if (state == CouiCheckBoxState.Part) {
                    // The half-selected dash: a horizontal bar across the middle.
                    drawLine(
                        color = glyphColor.copy(alpha = glyphAlpha),
                        start = Offset(topLeft.x + boxSide * 0.28f, topLeft.y + boxSide / 2f),
                        end = Offset(topLeft.x + boxSide * 0.72f, topLeft.y + boxSide / 2f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                } else {
                    // The check mark, trimmed so it draws on with the transition.
                    val inset = boxSide * 0.22f
                    val path = Path().apply {
                        moveTo(topLeft.x + inset, topLeft.y + boxSide * 0.55f)
                        lineTo(topLeft.x + boxSide * 0.44f, topLeft.y + boxSide - inset)
                        lineTo(topLeft.x + boxSide - inset, topLeft.y + inset)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val partial = Path()
                    measure.getSegment(0f, measure.length * glyphAlpha, partial, true)
                    drawPath(
                        path = partial,
                        color = glyphColor.copy(alpha = glyphAlpha),
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
