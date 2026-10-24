// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiEasing
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import io.github.wfc35286.coui.kmp.token.CouiTypography

/** The measured values of the floating button. */
object CouiFloatingButtonDefaults {
    /** The diameter of the main button. */
    val Size = 56.dp

    /** The scale the button shrinks to while held. */
    const val PressScale = 0.9f

    /** The press-down travel, in milliseconds. */
    const val PressDurationMs = 200

    /** The release travel back to full size, in milliseconds. */
    const val ResumeDurationMs = 340

    /** The rotation of the main button while its menu is open. */
    const val MenuOpenRotation = 45f

    /** The label pill's inner padding. */
    val LabelPadding = 12.dp
}

/**
 * A [CouiFloatingButton] component with COUI style.
 *
 * The round floating action button. Held down, it shrinks to 90% over 200ms on the standard
 * COUI movement curve; released, it eases back over 340ms.
 *
 * @param icon The icon painted in the centre of the button.
 * @param onClick Called when the button is clicked.
 * @param modifier The modifier to apply to the button.
 * @param enabled Whether the button can be interacted with.
 */
@Composable
fun CouiFloatingButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) CouiFloatingButtonDefaults.PressScale else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) {
                CouiFloatingButtonDefaults.PressDurationMs
            } else {
                CouiFloatingButtonDefaults.ResumeDurationMs
            },
            easing = CouiEasing.MoveEase,
        ),
        label = "couiFabPressScale",
    )
    val iconTint = if (enabled) colors.labelOnColor else colors.labelTertiary

    Box(
        modifier = modifier
            .size(CouiFloatingButtonDefaults.Size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (enabled) colors.containerTheme else colors.disable)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconTint),
        )
    }
}

/** One cascade menu entry of a [CouiExpandableFloatingButton]. */
@Immutable
data class CouiFloatingButtonItem(
    val label: String,
    val icon: Painter? = null,
)

/**
 * A [CouiExpandableFloatingButton] component with COUI style.
 *
 * A floating button whose tap fans out a cascade of labelled items above it and rotates the
 * main button 45 degrees while the menu is open; tapping the button again, picking an item,
 * or tapping the scrim under the menu closes it. The main button carries the same 90% / 200ms
 * press feedback as [CouiFloatingButton].
 *
 * @param icon The icon of the main button; it rotates while the menu is open.
 * @param items The cascade entries, bottom to top.
 * @param onItemSelected Called with the picked entry's index.
 * @param modifier The modifier to apply to the whole stack.
 * @param enabled Whether the button can be interacted with.
 */
@Composable
fun CouiExpandableFloatingButton(
    icon: Painter,
    items: List<CouiFloatingButtonItem>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) CouiFloatingButtonDefaults.MenuOpenRotation else 0f,
        animationSpec = tween(durationMillis = CouiFloatingButtonDefaults.PressDurationMs, easing = CouiEasing.MoveEase),
        label = "couiFabMenuRotation",
    )
    val colors = CouiTheme.colors

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(CircleShape)
                        .background(colors.backgroundWithCard)
                        .clickable {
                            expanded = false
                            onItemSelected(index)
                        }
                        .padding(
                            horizontal = CouiFloatingButtonDefaults.LabelPadding,
                            vertical = 8.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.icon?.let {
                        androidx.compose.foundation.Image(
                            painter = it,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Box(modifier = Modifier.size(8.dp))
                    }
                    CouiText(
                        text = item.label,
                        style = CouiTextStyle(CouiFontSize.Scaled(12f)),
                        color = colors.labelPrimary,
                    )
                }
            }
        }
        Box {
            CouiFloatingButton(
                icon = icon,
                onClick = { expanded = !expanded },
                enabled = enabled,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
            if (expanded) {
                // A scrim under the menu that eats the tap which closes it.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = false },
                )
            }
        }
    }
}
