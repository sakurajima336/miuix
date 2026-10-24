// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiScrollBar
import io.github.wfc35286.coui.kmp.basic.toTextStyle
import io.github.wfc35286.coui.kmp.token.CouiTextStyle

@Composable
internal fun CouiPopupListContent(
    items: List<CouiPopupListItem>,
    selectedIndex: Int,
    enabled: Boolean,
    dark: Boolean,
    shape: Shape,
    onItemClick: (Int) -> Unit,
    panelState: CouiPopupPanelState,
    modifier: Modifier = Modifier,
    onSubMenuItem: (Int) -> Unit = {},
    scrollState: ScrollState? = null,
    onRowBoundsChange: (Int, Rect) -> Unit = { _, _ -> },
    subMenuParentIndex: Int = -1,
    subMenuParentActive: Boolean = true,
    showScrollBar: Boolean = true,
    clipContent: Boolean = true,
) {
    val scroll = scrollState ?: rememberScrollState()
    val hasIcon = remember(items) { items.any { it.leadingIcon != null } }
    panelState.items = items
    panelState.scroll = scroll
    panelState.talkback = couiPopupTouchExplorationEnabled()
    panelState.parentIndex = subMenuParentIndex
    panelState.click = { index ->
        if (items[index].subItems.isNotEmpty()) onSubMenuItem(index) else onItemClick(index)
    }
    DrivePopupPanelMask(panelState)
    val currentClick by rememberUpdatedState(onItemClick)
    val currentSubMenu by rememberUpdatedState(onSubMenuItem)
    val maskColor = if (dark) Color(0x26FFFFFF) else Color(0x14000000)
    val dividerColor = Color(0x14000000)
    Box(
        modifier
            .then(if (clipContent) Modifier.clip(shape) else Modifier)
            .onGloballyPositioned { panelState.coordinates = it },
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (panelState.initialized && panelState.pressProgress > 0f) {
                val rect = panelState.maskRect
                // The press mask is inset by an 8dp horizontal margin. On single line rows
                // (shorter than twice the multi-line radius) a fixed radius reads visibly
                // squarer, so those rows keep the full capsule form instead.
                val margin = 8.dp.toPx()
                val inset = Rect(rect.left + margin, rect.top, rect.right - margin, rect.bottom)
                val radius = if (panelState.singleLine) inset.height / 2f else CouiPopupListWindowDefaults.MaskRadiusVerticalMultiLine.roundToPx().toFloat()
                val weight = if (panelState.singleLine) 1f else 3f
                drawCouiPopupMask(inset, maskColor.copy(alpha = maskColor.alpha * panelState.pressProgress), radius, weight)
            }
        }
        Column(
            Modifier
                .verticalScroll(scroll, enabled = enabled)
                .padding(vertical = CouiPopupListWindowDefaults.PaddingVertical),
        ) {
            items.forEachIndexed { index, item ->
                when (item.type) {
                    CouiPopupItemType.Divider -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(CouiPopupListWindowDefaults.DividerHeight)
                            .padding(horizontal = CouiPopupListWindowDefaults.DividerMarginHorizontal),
                    ) {
                        Canvas(Modifier.matchParentSize()) { drawRect(dividerColor) }
                    }

                    CouiPopupItemType.GroupDivider -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(CouiPopupListWindowDefaults.GroupDividerTotalHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxWidth().height(CouiPopupListWindowDefaults.GroupDividerHeight)) {
                            drawRect(dividerColor)
                        }
                    }

                    CouiPopupItemType.Header -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CouiPopupListWindowDefaults.ItemPaddingHorizontal)
                            .padding(top = CouiPopupListWindowDefaults.HeaderPaddingTop, bottom = CouiPopupListWindowDefaults.HeaderPaddingBottom),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicText(
                            text = item.title,
                            style = couiPopupPlatformTextStyle(
                                CouiPopupListWindowDefaults.HeaderTextStyle.toTextStyle(
                                    if (dark) Color(0x99FFFFFF) else Color(0x99000000),
                                    roundScaledFontSizeToPixel = true,
                                ),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    CouiPopupItemType.Default -> {
                        val source = remember { MutableInteractionSource() }
                        val textColor = if (item.enabled) {
                            if (dark) Color.White else Color.Black
                        } else {
                            if (dark) Color(0x4DFFFFFF) else Color(0x26000000)
                        }
                        val horizontal = if (hasIcon) {
                            CouiPopupListWindowDefaults.ItemPaddingHorizontal
                        } else {
                            CouiPopupListWindowDefaults.ItemPaddingHorizontalNoIcon
                        }
                        val hasStateSlot = item.subItems.isNotEmpty() || item.checkable || item.checked
                        val hint = item.hint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { row ->
                                    CouiPopupDebug32.title(panelState, index, row)
                                    panelState.coordinates?.takeIf { it.isAttached && row.isAttached }?.let { container ->
                                        val box = container.localBoundingBoxOf(row, clipBounds = false)
                                        panelState.row(index, box)
                                        onRowBoundsChange(index, box)
                                    }
                                }
                                .semantics { selected = index == selectedIndex }
                                .clickable(
                                    interactionSource = source,
                                    indication = null,
                                    enabled = enabled && item.enabled,
                                    role = Role.RadioButton,
                                    onClick = { if (item.subItems.isNotEmpty()) currentSubMenu(index) else currentClick(index) },
                                )
                                .padding(horizontal = horizontal, vertical = CouiPopupListWindowDefaults.ItemPaddingVertical)
                                .heightIn(min = CouiPopupListWindowDefaults.ItemContentMinHeight),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasIcon) {
                                Box(Modifier.size(CouiPopupListWindowDefaults.ItemIconSize)) {
                                    item.leadingIcon?.let { icon ->
                                        Image(
                                            painter = icon,
                                            contentDescription = null,
                                            colorFilter = item.iconTint?.let { ColorFilter.tint(it) },
                                            modifier = Modifier.matchParentSize(),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(CouiPopupListWindowDefaults.ItemIconMarginEnd))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    text = item.title,
                                    style = couiPopupPlatformTextStyle(
                                        CouiPopupListWindowDefaults.TextStyle.toTextStyle(
                                            color = textColor,
                                            roundScaledFontSizeToPixel = true,
                                        ),
                                    ).let { if (index == subMenuParentIndex && subMenuParentActive) it.copy(fontWeight = FontWeight.Medium) else it },
                                    onTextLayout = { panelState.titleLines[index] = it.lineCount },
                                    maxLines = if (item.description.isNullOrEmpty()) 3 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                item.description?.takeIf { it.isNotEmpty() }?.let { description ->
                                    BasicText(
                                        text = description,
                                        style = couiPopupPlatformTextStyle(
                                            CouiPopupListWindowDefaults.DescriptionTextStyle.toTextStyle(textColor, roundScaledFontSizeToPixel = true),
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (hint != null) {
                                Spacer(Modifier.width(CouiPopupListWindowDefaults.StateIconGap))
                                Box(Modifier.widthIn(max = CouiPopupListWindowDefaults.HintMaxWidth)) { hint() }
                                if (hasStateSlot) Spacer(Modifier.width(CouiPopupListWindowDefaults.HintGap))
                            } else if (hasStateSlot) {
                                Spacer(Modifier.width(CouiPopupListWindowDefaults.StateIconGap))
                            }
                            if (hasStateSlot) {
                                Box(Modifier.size(CouiPopupListWindowDefaults.CheckboxSize)) {
                                    if (item.subItems.isNotEmpty()) {
                                        CouiPopupSubMenuIndicator(dark = dark, enabled = item.enabled, modifier = Modifier.matchParentSize())
                                    } else {
                                        CouiMenuCheckbox(checked = item.checked, color = textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (scrollState != null && showScrollBar) {
            // The bar must NOT contribute to this Box's size: the menu wraps its content, and a
            // child with `fillMaxHeight` would force the Box up to its maximum constraint (which
            // pushed the sub menu panel down and hid the bar). `matchParentSize` overlays it on
            // the already measured content instead.
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                // The scroll bar sits 12dp in from the top and bottom of the panel.
                CouiScrollBar(
                    state = scrollState,
                    dark = dark,
                    marginTop = CouiPopupListWindowDefaults.ScrollBarMarginVertical,
                    marginBottom = CouiPopupListWindowDefaults.ScrollBarMarginVertical,
                )
            }
        }
    }
}

@Composable
internal fun CouiMenuCheckbox(checked: Boolean, color: Color) {
    if (checked) CouiMenuCheckmark(color)
}

@Composable
internal fun CouiPopupSubMenuIndicator(dark: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val color = when {
        !enabled && dark -> Color(0x4DFFFFFF)
        !enabled -> Color(0x26000000)
        dark -> Color.White
        else -> Color.Black
    }
    Canvas(modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(14.2f, 10f)
            lineTo(18.329f, 5.413f)
            cubicTo(18.527f, 5.192f, 18.873f, 5.192f, 19.072f, 5.413f)
            lineTo(23.2f, 10f)
            moveTo(23.2f, 14f)
            lineTo(19.072f, 18.587f)
            cubicTo(18.873f, 18.808f, 18.527f, 18.808f, 18.329f, 18.587f)
            lineTo(14.2f, 14f)
        }
        with(drawContext.canvas) {
            save()
            scale(size.width / 24f, size.height / 24f)
            drawPath(
                path,
                androidx.compose.ui.graphics.Paint().apply {
                    this.color = color
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = 1.4f
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                },
            )
            restore()
        }
    }
}

internal expect fun DrawScope.drawCouiPopupMask(bounds: Rect, color: Color, radius: Float, weight: Float)

@Composable
internal expect fun couiPopupTouchExplorationEnabled(): Boolean

internal expect fun couiPopupPlatformTextStyle(style: TextStyle): TextStyle
