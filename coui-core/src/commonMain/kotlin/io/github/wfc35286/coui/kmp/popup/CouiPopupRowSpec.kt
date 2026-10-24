// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup

internal enum class CouiPopupRowKind {
    Default,

    Header,

    Horizontal,

    Alert,

    Custom,
}

internal data class CouiPopupRowSpec(
    val paddingStart: Int,
    val paddingEnd: Int,
    val showLeadingIcon: Boolean,
    val titleGapVisible: Boolean,
    val hintGapVisible: Boolean,
    val hintVisible: Boolean,
    val stateSlotVisible: Boolean,
    val stateSlotIsChevron: Boolean,
    val titleMedium: Boolean,
    val titleMaxLines: Int,
    val descriptionVisible: Boolean,
) {
    companion object {
        const val PADDING_WITH_ICON = 20

        const val PADDING_WITHOUT_ICON = 20
    }
}

internal fun couiPopupRowSpec(
    item: CouiPopupListItem,
    isSubMenuAnchor: Boolean = false,
): CouiPopupRowSpec {
    val hasSubMenu = item.subItems.isNotEmpty()
    val hasCheck = item.checked || item.checkable
    val hasHint = item.hint != null
    val hasState = hasSubMenu || hasCheck || item.checked

    val titleGap = hasHint || hasState
    val hintGap = hasHint && hasState

    val withIcon = item.leadingIcon != null
    return CouiPopupRowSpec(
        paddingStart = if (withIcon) CouiPopupRowSpec.PADDING_WITH_ICON else CouiPopupRowSpec.PADDING_WITHOUT_ICON,
        paddingEnd = if (withIcon) CouiPopupRowSpec.PADDING_WITH_ICON else CouiPopupRowSpec.PADDING_WITHOUT_ICON,
        showLeadingIcon = withIcon,
        titleGapVisible = titleGap,
        hintGapVisible = hintGap,
        hintVisible = hasHint && item.enabled,
        stateSlotVisible = hasState,
        stateSlotIsChevron = hasSubMenu,
        titleMedium = isSubMenuAnchor,
        titleMaxLines = if (item.description.isNullOrEmpty()) 3 else 2,
        descriptionVisible = !item.description.isNullOrEmpty(),
    )
}

internal fun couiPopupStateSlotContent(spec: CouiPopupRowSpec, checked: Boolean): CouiPopupStateSlot = when {
    spec.stateSlotIsChevron -> CouiPopupStateSlot.Chevron
    checked -> CouiPopupStateSlot.Check
    else -> CouiPopupStateSlot.Empty
}

internal enum class CouiPopupStateSlot { Chevron, Check, Empty }

internal fun couiPopupHasAvailableSubMenu(item: CouiPopupListItem): Boolean = item.subItems.isNotEmpty() && item.subItems.all { it.type == CouiPopupItemType.Default }

internal fun couiPopupWithGroupDividers(items: List<CouiPopupListItem>): List<CouiPopupListItem> {
    if (items.size < 3) return items
    if (items.map { it.groupId }.distinct().size < 2) return items
    // setItemList(list) passes sort=false; only adjacent group changes insert a divider.
    val sorted = items
    val out = ArrayList<CouiPopupListItem>(items.size + 4)
    var previous = sorted.first().groupId
    for (item in sorted) {
        if (item.groupId != previous) {
            out += CouiPopupListItem(type = CouiPopupItemType.GroupDivider)
            previous = item.groupId
        }
        out += item
    }
    return out
}
