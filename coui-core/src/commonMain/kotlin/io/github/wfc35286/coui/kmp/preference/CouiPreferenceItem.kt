// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.preference
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCardDefaults
import io.github.wfc35286.coui.kmp.basic.CouiDividerDefaults
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.basic.LocalCouiRowGroup
import io.github.wfc35286.coui.kmp.basic.drawCouiDivider
import io.github.wfc35286.coui.kmp.motion.couiStateMask
import io.github.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography

/** Default sizes for [CouiPreferenceItem], expressed relative to the surrounding card content. */
object CouiPreferenceDefaults {
    val MinHeight: Dp = 48.dp
    val HomepageMinHeight: Dp = 52.dp

    /** Horizontal content padding: the 32dp title inset minus the card's own 16dp outside margin. */
    val PaddingStart: Dp = 16.dp
    val PaddingEnd: Dp = 16.dp

    /** Vertical padding above and below the title/summary content. */
    val PaddingVertical: Dp = 10.dp

    /** Icon box size: the vector's intrinsic bounds, not the visible path's bounding box. */
    val IconSize: Dp = 24.dp
    val IconMarginEnd: Dp = 16.dp
    val WidgetMarginStart: Dp = 16.dp
}

/** A row's position within its card, controlling which edges receive head/tail padding. */
enum class CouiPreferencePosition {
    First,
    Middle,
    Last,
    Single,
}

/**
 * A settings-style preference row with press-state background feedback ([couiStateMask]).
 *
 * When [showDivider] is set, the divider starts at the title inset and ends at the content inset,
 * overlaying the next row's first pixel.
 */
@Composable
fun CouiPreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String = "",
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    minHeight: Dp = CouiPreferenceDefaults.MinHeight,
    position: CouiPreferencePosition = CouiPreferencePosition.Single,
    hapticFeedback: Boolean = true,
) {
    val density = LocalDensity.current
    // The head/tail padding belongs to the row itself, not to the parent card. The px->dp
    // conversion truncates (2dp at the reference density) rather than rounding.
    val edgePadding = with(density) { CouiCardDefaults.HeadOrTailPadding.toPx().toInt().toDp() }
    val headPadding = if (position == CouiPreferencePosition.First || position == CouiPreferencePosition.Single) edgePadding else 0.dp
    val tailPadding = if (position == CouiPreferencePosition.Last || position == CouiPreferencePosition.Single) edgePadding else 0.dp
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // Rows report their press state to the shared group so the dividers adjacent to a held
    // row can fade together.
    val rowGroup = LocalCouiRowGroup.current
    val rowIndex = remember(rowGroup) { rowGroup?.allocateIndex() ?: -1 }
    if (rowGroup != null && rowIndex >= 0) {
        DisposableEffect(rowGroup, rowIndex) {
            onDispose { rowGroup.setRowPressed(rowIndex, false) }
        }
        LaunchedEffect(rowGroup, rowIndex, pressed, enabled, onClick) {
            rowGroup.setRowPressed(rowIndex, pressed && enabled && onClick != null)
            rowGroup.syncDividerAlpha()
        }
    }
    val haptics = rememberCouiHaptics()
    val dividerColor = CouiTheme.colors.divider
    val titleInset = CouiPreferenceDefaults.PaddingStart +
        if (leading != null) CouiPreferenceDefaults.IconSize + CouiPreferenceDefaults.IconMarginEnd else 0.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (showDivider) {
                    drawCouiDivider(
                        color = dividerColor,
                        thickness = CouiDividerDefaults.Thickness,
                        start = titleInset,
                        end = CouiPreferenceDefaults.PaddingEnd,
                        y = size.height,
                        // Dividers adjacent to the held row follow the group's shared alpha.
                        alpha = if (rowGroup != null && rowGroup.isDividerAffected(rowIndex)) {
                            rowGroup.dividerAlpha.value
                        } else {
                            1f
                        },
                    )
                }
            }
            .couiStateMask(source, enabled && onClick != null)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = null, enabled = enabled) {
                        // The same granular short pulse other pressed controls fire.
                        if (hapticFeedback) haptics.granularShort()
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = minHeight + headPadding + tailPadding)
            .padding(
                start = CouiPreferenceDefaults.PaddingStart,
                end = CouiPreferenceDefaults.PaddingEnd,
                top = CouiPreferenceDefaults.PaddingVertical + headPadding,
                bottom = CouiPreferenceDefaults.PaddingVertical + tailPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(CouiPreferenceDefaults.IconSize), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(CouiPreferenceDefaults.IconMarginEnd))
        }
        Column(Modifier.weight(1f)) {
            CouiText(title, CouiTypography.listTitle, enabled = enabled, emphasis = CouiTextEmphasis.Primary)
            if (summary.isNotEmpty()) {
                CouiText(summary, CouiTypography.preferenceSummary, enabled = enabled, emphasis = CouiTextEmphasis.Secondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(CouiPreferenceDefaults.WidgetMarginStart))
            trailing()
        }
    }
}
