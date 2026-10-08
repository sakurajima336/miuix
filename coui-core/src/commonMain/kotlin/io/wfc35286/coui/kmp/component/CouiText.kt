// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.wfc35286.coui.kmp.theme.CouiTheme
import io.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.wfc35286.coui.kmp.token.CouiTextStyle
import io.wfc35286.coui.kmp.token.toTextUnit

/**
 * Resolves the colour a COUI text style uses for a given [emphasis] and enabled state.
 *
 * The real styles point at `ColorStateList` files rather than flat colours, so the
 * enabled/disabled pairing is part of the design system and is reproduced here:
 * titles fall back to `couiColorLabelTertiary` when disabled, and so do summaries.
 */
@Composable
@ReadOnlyComposable
fun couiTextColor(
    emphasis: CouiTextEmphasis,
    enabled: Boolean = true,
): Color {
    val colors = CouiTheme.colors
    return when (emphasis) {
        CouiTextEmphasis.Primary -> if (enabled) colors.labelPrimary else colors.labelTertiary
        CouiTextEmphasis.Secondary -> if (enabled) colors.secondNeutral else colors.labelTertiary
        CouiTextEmphasis.Tertiary -> colors.labelTertiary
        CouiTextEmphasis.Error -> colors.error
        CouiTextEmphasis.OnColor -> colors.labelOnColor
    }
}

/**
 * COUI's text primitive.
 *
 * This is deliberately a thin wrapper over [BasicText] rather than anything from Miuix or
 * Material: it is what lets the COUI component layer exist without depending on either. It
 * applies a [CouiTextStyle] from [io.wfc35286.coui.kmp.token.CouiTypography] plus the semantic
 * colour for the requested [emphasis].
 *
 * @param text The string to draw.
 * @param style The type-scale entry to use.
 * @param modifier The modifier to apply.
 * @param emphasis Which semantic colour role the text plays; defaults to
 *   [CouiTextEmphasis.Primary].
 * @param enabled Whether the text is enabled; disabled text falls back to
 *   `couiColorLabelTertiary` exactly as the real `ColorStateList`s do.
 * @param color An explicit override. When left unspecified the colour is derived from
 *   [emphasis] and [enabled].
 * @param maxLines The maximum number of lines to lay out.
 * @param overflow How to handle text that does not fit.
 * @param textAlign The horizontal alignment of the text.
 */
@Composable
fun CouiText(
    text: String,
    style: CouiTextStyle,
    modifier: Modifier = Modifier,
    emphasis: CouiTextEmphasis = CouiTextEmphasis.Primary,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val resolvedColor = if (color == Color.Unspecified) couiTextColor(emphasis, enabled) else color
    BasicText(
        text = text,
        modifier = modifier,
        style = style.toTextStyle(resolvedColor, textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Builds a Compose [TextStyle] from a COUI [CouiTextStyle].
 *
 * The `dp`-sourced entries in the type scale resolve against the current density here, which is
 * what makes them keep their fixed size instead of following the user's font-size setting.
 *
 * @param color The colour to apply.
 * @param textAlign The horizontal alignment; `BasicText` has no separate parameter for this, so
 *   it travels inside the style.
 */
@Composable
fun CouiTextStyle.toTextStyle(
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize.toTextUnit(),
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    lineHeight = lineHeight,
    textAlign = textAlign ?: TextAlign.Unspecified,
)