// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

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
import io.wfc35286.coui.kmp.motion.couiStateMask
import io.wfc35286.coui.kmp.motion.rememberCouiHaptics
import io.wfc35286.coui.kmp.theme.CouiTheme
import io.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.wfc35286.coui.kmp.token.CouiTypography

/** Values are relative to the card: XML insets include its separate 16dp outside margin. */
object CouiPreferenceDefaults {
    val MinHeight: Dp = 48.dp
    val HomepageMinHeight: Dp = 52.dp

    /** support_preference_title_padding_start/end (32dp) minus card outside margin (16dp). */
    val PaddingStart: Dp = 16.dp
    val PaddingEnd: Dp = 16.dp

    /** support_preference_text_content_padding_top/bottom. */
    val PaddingVertical: Dp = 10.dp

    /** Settings homepage vector intrinsic bounds, not the visible path's bounding box. */
    val IconSize: Dp = 24.dp
    val IconMarginEnd: Dp = 16.dp
    val WidgetMarginStart: Dp = 16.dp
}

/** COUICardListHelper positions: 1=first, 2=middle, 3=last, 4=single. */
enum class CouiPreferencePosition {
    First,
    Middle,
    Last,
    Single,
}

/**
 * Settings preference content with COUIMaskEffectDrawable's background state feedback.
 * The divider starts at the title, ends at the content inset, and overlays the next row's
 * first pixel, matching COUIPreferenceItemDecoration.onDrawOver.
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
    // Official setPadding(position) adds these to the row, not to its parent card.
    // getDimensionPixelOffset truncates 2dp (5px at density 2.975), rather than rounding.
    val edgePadding = with(density) { CouiCardDefaults.HeadOrTailPadding.toPx().toInt().toDp() }
    val headPadding = if (position == CouiPreferencePosition.First || position == CouiPreferencePosition.Single) edgePadding else 0.dp
    val tailPadding = if (position == CouiPreferencePosition.Last || position == CouiPreferencePosition.Single) edgePadding else 0.dp
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // COUIRecyclerDividerManager fades the dividers around a held row; the rows of this card
    // report their press state to the shared group so the right two lines can be found.
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
                        // Both lines touching the held row spring to 0, as the manager does.
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
                        // GRANULAR_SHORT_VIBRATE (0x12e): the pulse COUIButton, COUISwitch and
                        // COUICheckBoxPreference all fire, and the one the row background util
                        // plays through COUIBackgroundAnimationUtil.performHapticFeedback().
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
