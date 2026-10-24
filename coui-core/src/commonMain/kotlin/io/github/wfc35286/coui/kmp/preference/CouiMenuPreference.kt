// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography

/** COUIMenuPreference: a Settings row with a controlled single-choice popup and summary. */
@Composable
fun CouiMenuPreference(
    title: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    position: CouiPreferencePosition = CouiPreferencePosition.Single,
    useBackgroundBlur: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    // COUIMenuPreference keeps mPopupListItems alive and marks the selected entry inside
    // setValue(): chosen item checked=true, all others false. The popup renders the check
    // through MixedPopupListAdapter.setStateIcon, so the list must carry the marks.
    val items = remember(entries, selectedIndex) {
        entries.mapIndexed { index, entry -> CouiPopupListItem(entry, checked = index == selectedIndex) }
    }
    val hoverColor = CouiTheme.colors.hover
    val hover by animateFloatAsState(
        if (expanded && enabled) 1f else 0f,
        CouiSpringData(0f, 0.3f).toSpringSpec(),
        label = "couiMenuAnchorHover",
    )
    Box(
        modifier,
    ) {
        CouiPreferenceItem(
            title = title,
            modifier = Modifier.drawBehind {
                if (hover > 0f) drawRect(hoverColor.copy(alpha = hoverColor.alpha * hover))
            },
            onClick = { if (entries.isNotEmpty()) expanded = true },
            enabled = enabled,
            showDivider = showDivider,
            position = position,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CouiText(entries.getOrNull(selectedIndex).orEmpty(), CouiTypography.body, emphasis = CouiTextEmphasis.Secondary, enabled = enabled)
                    Spacer(Modifier.width(4.dp))
                    val color = CouiTheme.colors.labelTertiary
                    val indicator = remember {
                        Path().apply {
                            // res/drawable/coui_pop_up_next_normal.xml, 14 x 24 viewport.
                            moveTo(4.199f, 10f)
                            lineTo(8.328f, 5.413f)
                            cubicTo(8.526f, 5.192f, 8.872f, 5.192f, 9.071f, 5.413f)
                            lineTo(13.199f, 10f)
                            moveTo(13.199f, 14f)
                            lineTo(9.071f, 18.587f)
                            cubicTo(8.872f, 18.808f, 8.526f, 18.808f, 8.328f, 18.587f)
                            lineTo(4.199f, 14f)
                        }
                    }
                    val paint = remember(color) {
                        Paint().apply {
                            this.color = color
                            style = PaintingStyle.Stroke
                            strokeWidth = 1.4f
                            strokeCap = StrokeCap.Round
                        }
                    }
                    Canvas(Modifier.size(14.dp, 24.dp)) {
                        drawContext.canvas.save()
                        drawContext.canvas.scale(size.width / 14f, size.height / 24f)
                        drawContext.canvas.drawPath(indicator, paint)
                        drawContext.canvas.restore()
                    }
                }
            },
        )
        CouiPopupListWindow(
            expanded = expanded && enabled,
            items = items,
            selectedIndex = selectedIndex,
            onItemClick = onSelectedIndexChange,
            onDismissRequest = { expanded = false },
            // Official COUIPopupListWindow.show(View) calls
            // PopupMenuLocateHelper.setAnchor(view, MIN_VALUE, MIN_VALUE, root), which keeps the
            // anchor View's global visible rect and only collapses it to a point when explicit
            // offsets are supplied. So the default anchor is this whole row: the menu centres on
            // the row and sits below the row, not below the finger.
            touchPoint = null,
            useBackgroundBlur = useBackgroundBlur,
            paneTitle = title,
        )
    }
}
