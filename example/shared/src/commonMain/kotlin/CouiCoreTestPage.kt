// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.component.CouiCard
import io.wfc35286.coui.kmp.component.CouiCardDefaults
import io.wfc35286.coui.kmp.component.CouiMenuPreference
import io.wfc35286.coui.kmp.component.CouiPopupItemType
import io.wfc35286.coui.kmp.component.CouiPopupListItem
import io.wfc35286.coui.kmp.component.CouiPopupListWindow
import io.wfc35286.coui.kmp.component.CouiPreferenceDefaults
import io.wfc35286.coui.kmp.component.CouiPreferenceItem
import io.wfc35286.coui.kmp.component.CouiPreferencePosition
import io.wfc35286.coui.kmp.component.CouiSectionSeekBar
import io.wfc35286.coui.kmp.component.CouiSeekBar
import io.wfc35286.coui.kmp.component.CouiSeekBarDefaults
import io.wfc35286.coui.kmp.component.CouiSeekBarMoveType
import io.wfc35286.coui.kmp.component.CouiSmallTitle
import io.wfc35286.coui.kmp.component.CouiStatusBarToggleSlider
import io.wfc35286.coui.kmp.component.CouiSwitch
import io.wfc35286.coui.kmp.component.CouiText
import io.wfc35286.coui.kmp.theme.CouiTheme
import io.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.wfc35286.coui.kmp.token.CouiTypography

/**
 * A component gallery with neutral placeholder content.
 *
 * Switch, list, menu and slider variants expose only local demo state. The
 * page uses **nothing from Miuix** - every colour, radius and spring comes from `coui-core`.
 */
@Composable
fun CouiCoreTestPage(
    padding: PaddingValues,
) {
    var menuIndex by remember { mutableIntStateOf(1) }
    val menuEntries = remember { List(7) { "Item ${it + 1}" } }
    var firstChecked by remember { mutableStateOf(true) }
    var secondChecked by remember { mutableStateOf(false) }
    var thirdChecked by remember { mutableStateOf(false) }
    var fourthChecked by remember { mutableStateOf(true) }
    var distanceValue by remember { mutableFloatStateOf(0.35f) }
    var clickValue by remember { mutableFloatStateOf(0.35f) }
    var fixedTrackValue by remember { mutableFloatStateOf(0.65f) }
    var sectionValue by remember { mutableIntStateOf(2) }

    CouiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Settings pages that host cards sit on couiColorBackgroundWithCard (#F0F1F2);
                // couiColorCardBackground is the white fill on top of it.
                .background(CouiTheme.colors.backgroundWithCard)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            CouiSmallTitle("Menu")
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                CouiMenuPreference(
                    title = "Title",
                    entries = menuEntries,
                    selectedIndex = menuIndex,
                    onSelectedIndexChange = { menuIndex = it },
                )
            }
            CouiSmallTitle("Mixed menu")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                CouiMixedMenuDemo()
            }
            CouiSmallTitle("List")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                CouiPreferenceItem(
                    title = "Title 1",
                    position = CouiPreferencePosition.First,
                    onClick = { thirdChecked = !thirdChecked },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiDemoIcons.Placeholder, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelPrimary)) },
                    trailing = { CouiSwitch(checked = thirdChecked, onCheckedChange = { thirdChecked = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 2",
                    position = CouiPreferencePosition.Middle,
                    onClick = { firstChecked = !firstChecked },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiDemoIcons.Placeholder, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelPrimary)) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CouiText(if (firstChecked) "Value A" else "Value B", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                            Spacer(Modifier.width(4.dp))
                            Image(CouiDemoIcons.Chevron, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary))
                        }
                    },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 3",
                    position = CouiPreferencePosition.Middle,
                    onClick = { secondChecked = !secondChecked },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiDemoIcons.Placeholder, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelPrimary)) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CouiText(if (secondChecked) "Value A" else "Value B", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                            Spacer(Modifier.width(4.dp))
                            Image(CouiDemoIcons.Chevron, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary))
                        }
                    },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 4",
                    position = CouiPreferencePosition.Last,
                    onClick = { fourthChecked = !fourthChecked },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiDemoIcons.Placeholder, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelPrimary)) },
                    trailing = { Image(CouiDemoIcons.Chevron, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary)) },
                )
            }

            CouiSmallTitle("Switch")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Row taps and control taps exercise the same local state.
                CouiPreferenceItem(
                    title = "Title 1",
                    position = CouiPreferencePosition.First,
                    summary = "Summary",
                    onClick = { firstChecked = !firstChecked },
                    trailing = { CouiSwitch(checked = firstChecked, onCheckedChange = { firstChecked = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 2",
                    position = CouiPreferencePosition.Middle,
                    onClick = { secondChecked = !secondChecked },
                    trailing = { CouiSwitch(checked = secondChecked, onCheckedChange = { secondChecked = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 3",
                    position = CouiPreferencePosition.Last,
                    onClick = { thirdChecked = !thirdChecked },
                    trailing = { CouiSwitch(checked = thirdChecked, onCheckedChange = { thirdChecked = it }) },
                )
            }

            CouiSmallTitle("Disabled")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CouiPreferenceItem(
                    title = "Title 1",
                    position = CouiPreferencePosition.First,
                    summary = "Summary",
                    enabled = false,
                    trailing = { CouiSwitch(checked = true, onCheckedChange = {}, enabled = false) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Title 2",
                    position = CouiPreferencePosition.Last,
                    enabled = false,
                    trailing = { CouiSwitch(checked = false, onCheckedChange = {}, enabled = false) },
                )
            }

            CouiSmallTitle("Switch")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CouiPreferenceItem(
                    title = "Title",
                    position = CouiPreferencePosition.Single,
                    summary = "Summary",
                    onClick = { fourthChecked = !fourthChecked },
                    trailing = { CouiSwitch(checked = fourthChecked, onCheckedChange = { fourthChecked = it }) },
                )
            }

            CouiSmallTitle("SeekBar")

            // Variant parameters stay in code; the component gallery uses placeholder copy.
            // Keep all four examples in one group, with independent values for direct comparison.
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    CouiText("Title 1", CouiTypography.body)
                    CouiText("Summary", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                    Spacer(Modifier.height(4.dp))
                    CouiSeekBar(
                        value = distanceValue,
                        onValueChange = { distanceValue = it },
                        moveType = CouiSeekBarMoveType.ByDistance,
                        moveDamping = 1f,
                        modifier = Modifier.fillMaxWidth().height(CouiSeekBarDefaults.MinHeight),
                    )
                    Spacer(Modifier.height(16.dp))
                    CouiText("Title 2", CouiTypography.body)
                    CouiText("Summary", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                    Spacer(Modifier.height(4.dp))
                    CouiSeekBar(
                        value = clickValue,
                        onValueChange = { clickValue = it },
                        moveType = CouiSeekBarMoveType.Default,
                        modifier = Modifier.fillMaxWidth().height(CouiSeekBarDefaults.MinHeight),
                    )
                    Spacer(Modifier.height(16.dp))
                    CouiText("Title 3", CouiTypography.body)
                    CouiText("Summary", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                    Spacer(Modifier.height(4.dp))
                    CouiSeekBar(
                        value = fixedTrackValue,
                        onValueChange = { fixedTrackValue = it },
                        moveType = CouiSeekBarMoveType.Default,
                        backgroundEnlargeScale = CouiStatusBarToggleSlider.EnlargeScale,
                        trackColor = CouiStatusBarToggleSlider.TrackColor,
                        thumbShadowSize = CouiStatusBarToggleSlider.ThumbShadowSize,
                        touchPaddingVertical = 6.dp,
                        modifier = Modifier.fillMaxWidth().height(CouiStatusBarToggleSlider.Height),
                    )
                    Spacer(Modifier.height(16.dp))
                    CouiText("Title 4", CouiTypography.body)
                    CouiText("Summary", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                    Spacer(Modifier.height(4.dp))
                    CouiSectionSeekBar(
                        value = sectionValue,
                        onValueChange = { sectionValue = it },
                        min = 0,
                        max = 4,
                        modifier = Modifier.fillMaxWidth().height(CouiSeekBarDefaults.MinHeight),
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

/**
 * Exercises the official mixed menu: checkable rows (multi-select), both divider item types,
 * a submenu row and a scrollable list that shows the COUIScrollBar.
 */
@Composable
private fun CouiMixedMenuDemo() {
    var expanded by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(List(4) { it % 2 == 0 }) }
    var subMenuChoice by remember { mutableStateOf(-1) }
    val items = remember(checked) {
        buildList {
            add(CouiPopupListItem("Title 1", checkable = true, checked = checked[0], groupId = 0))
            add(CouiPopupListItem("Title 2", checkable = true, checked = checked[1], groupId = 0))
            add(CouiPopupListItem("Title 3", checkable = true, checked = checked[2], groupId = 0))
            add(CouiPopupListItem("Title 4", checkable = true, checked = checked[3], groupId = 0))
            add(
                CouiPopupListItem(
                    title = "Title 5",
                    groupId = 1,
                    subItems = listOf(
                        CouiPopupListItem("Item A"),
                        CouiPopupListItem("Item B"),
                        CouiPopupListItem("Item C"),
                    ),
                ),
            )
            add(CouiPopupListItem("Title 6", groupId = 1))
            add(CouiPopupListItem("Title 7", groupId = 2))
            add(CouiPopupListItem("Title 8", groupId = 2))
            add(CouiPopupListItem("Title 9", groupId = 2))
            add(CouiPopupListItem("Title 10", groupId = 2))
            add(CouiPopupListItem("Title 11", groupId = 2))
            add(CouiPopupListItem("Title 12", groupId = 2))
            add(CouiPopupListItem("Title 13", groupId = 2))
            add(CouiPopupListItem("Title 14", groupId = 2))
        }
    }
    var popupTouchPoint by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                // Official anchors the popup to the touch point / anchor view:
                // `COUIPopupListWindow.showAtLocation(...)` feeds `PopupMenuLocateHelper$12.locateX`,
                // which centers the menu on `mAnchor.centerX`. Passing null made our menu fall back to
                // the window anchor, so it always opened at the same centered spot instead of where
                // the finger went down.
                popupTouchPoint = awaitFirstDown(requireUnconsumed = false).position
            }
        },
    ) {
        CouiPreferenceItem(
            title = "Title",
            position = CouiPreferencePosition.Single,
            onClick = { expanded = true },
            trailing = {
                CouiText(
                    if (subMenuChoice >= 0) "Item ${('A' + subMenuChoice)}" else "Value",
                    CouiTypography.body,
                    emphasis = CouiTextEmphasis.Secondary,
                )
            },
        )
        CouiPopupListWindow(
            expanded = expanded,
            items = items,
            selectedIndex = -1,
            onItemClick = { index ->
                // The popup reports data indices, excluding headers and divider rows.
                if (index in checked.indices) {
                    checked = checked.toMutableList().also { it[index] = !it[index] }
                } else {
                    expanded = false
                }
            },
            onDismissRequest = { expanded = false },
            onSubItemClick = { _, subIndex ->
                subMenuChoice = subIndex
                expanded = false
            },
            touchPoint = popupTouchPoint,
        )
    }
    // COUIScrollBar demo. The official popup list is a `COUITouchListView`, which builds a
    // `COUIScrollBar` (marginTop = marginBottom = coui_popup_list_window_scrollbar_vertical_padding,
    // 14dp track box, 48dp minimum thumb, 3dp capsule that widens to 6dp while dragged, 2s fade).
    // A list taller than the popup's maximum height is what makes it visible.
    var longMenuExpanded by remember { mutableStateOf(false) }
    var longMenuTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val longMenuItems = remember {
        List(30) { CouiPopupListItem("Long item ${it + 1}") }
    }
    Box(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                longMenuTouchPoint = awaitFirstDown(requireUnconsumed = false).position
            }
        },
    ) {
        CouiPreferenceItem(
            title = "COUIScrollBar",
            position = CouiPreferencePosition.Single,
            onClick = { longMenuExpanded = true },
            trailing = {
                CouiText(
                    "Long menu",
                    CouiTypography.body,
                    emphasis = CouiTextEmphasis.Secondary,
                )
            },
        )
        CouiPopupListWindow(
            expanded = longMenuExpanded,
            items = longMenuItems,
            selectedIndex = -1,
            onItemClick = { longMenuExpanded = false },
            onDismissRequest = { longMenuExpanded = false },
            touchPoint = longMenuTouchPoint,
        )
    }
}
