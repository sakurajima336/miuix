// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0


/**
 * A component gallery with neutral placeholder content.
 *
 * Switch, list, menu and slider variants expose only local demo state. The
 * page uses **nothing from Miuix** - every colour, radius and spring comes from `coui-core`.
 */
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import component.CouiBlurSection
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingBar
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBar
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarDefaults
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarItem
import io.github.wfc35286.coui.kmp.basic.CouiButton
import io.github.wfc35286.coui.kmp.basic.CouiButtonSize
import io.github.wfc35286.coui.kmp.basic.CouiCheckBox
import io.github.wfc35286.coui.kmp.basic.CouiCheckBoxState
import io.github.wfc35286.coui.kmp.basic.CouiCircularProgress
import io.github.wfc35286.coui.kmp.basic.CouiExpandableFloatingButton
import io.github.wfc35286.coui.kmp.basic.CouiFloatingButtonItem
import io.github.wfc35286.coui.kmp.basic.CouiHorizontalProgressBar
import io.github.wfc35286.coui.kmp.basic.CouiButtonType
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiCardDefaults
import io.github.wfc35286.coui.kmp.basic.CouiRedDotType
import io.github.wfc35286.coui.kmp.basic.CouiSectionSeekBar
import io.github.wfc35286.coui.kmp.basic.CouiSeekBar
import io.github.wfc35286.coui.kmp.basic.CouiSeekBarDefaults
import io.github.wfc35286.coui.kmp.basic.CouiLoadingButton
import io.github.wfc35286.coui.kmp.basic.CouiSeekBarMoveType
import io.github.wfc35286.coui.kmp.basic.CouiTabLayout
import io.github.wfc35286.coui.kmp.basic.CouiSmallTitle
import io.github.wfc35286.coui.kmp.basic.CouiStatusBarToggleSlider
import io.github.wfc35286.coui.kmp.basic.CouiSwitch
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.graphics.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.graphics.couiBackdrop
import io.github.wfc35286.coui.kmp.graphics.couiSpotLight
import io.github.wfc35286.coui.kmp.graphics.rememberCouiBackdrop
import io.github.wfc35286.coui.kmp.popup.CouiPopupItemType
import io.github.wfc35286.coui.kmp.popup.CouiPopupListItem
import io.github.wfc35286.coui.kmp.popup.CouiPopupListWindow
import io.github.wfc35286.coui.kmp.overlay.CouiAlertDialog
import io.github.wfc35286.coui.kmp.overlay.CouiBottomSheet
import io.github.wfc35286.coui.kmp.overlay.CouiSnackBar
import io.github.wfc35286.coui.kmp.overlay.CouiTooltip
import io.github.wfc35286.coui.kmp.overlay.CouiTooltipDirection
import io.github.wfc35286.coui.kmp.preference.CouiMenuPreference
import io.github.wfc35286.coui.kmp.preference.CouiPreferenceDefaults
import io.github.wfc35286.coui.kmp.preference.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.preference.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography
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
    var barIndex by remember { mutableIntStateOf(0) }
    var barVisible by remember { mutableStateOf(true) }
    // Which accessory slots are configured. The official decides this purely by whether the side
    // has an icon (`isAccessoryIconConfigured` -> `getIconDrawable() != null`), and the two sides
    // are completely independent - so "pill on the left, one button on the right" is just the
    // start slot left unconfigured.
    var accessories by remember { mutableIntStateOf(2) } // 0 = both, 1 = start only, 2 = end only
    val barIcon = rememberVectorPainter(CouiDemoIcons.Placeholder)
    // The official never tints the icon itself - it hands `setSelected` to the ImageView and
    // lets the caller's Drawable decide. The demo expresses the same thing with `iconTint`.
    CouiTheme {
        val pageBackdrop = rememberCouiBackdrop()
        val barItems = listOf("首页", "游戏", "软件", "榜单", "我的").mapIndexed { index, title ->
            CouiBottomFloatingNavigationBarItem(
                id = index,
                title = title,
                icon = barIcon,
                iconTint = CouiBottomFloatingNavigationBarDefaults.titleColor(index == barIndex),
                redDotType = if (index == 4) CouiRedDotType.Number else CouiRedDotType.None,
                redDotNumber = 15,
            )
        }
        // The page is captured (drawn content recorded once per frame) and the bar samples that
        // recording behind the pill - no second composition of the page.
        val scrollState = rememberScrollState()
        val pageContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Settings pages that host cards sit on couiColorBackgroundWithCard (#F0F1F2);
                    // couiColorCardBackground is the white fill on top of it.
                    .background(CouiTheme.colors.backgroundWithCard)
                    .verticalScroll(scrollState)
                    // Only the top inset is applied: the page has to run *under* the floating bar,
                    // otherwise there is nothing behind the pill to blur and the material reads as a
                    // flat colour no matter what.
                    .padding(
                        top = padding.calculateTopPadding() +
                            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    ),
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

                CouiSmallTitle("Button")

                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CouiButton(
                            text = "Button 1",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                        CouiButton(
                            text = "Button 2",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            type = CouiButtonType.Secondary,
                        )
                        CouiButton(
                            text = "Button 3",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CouiButton(text = "Small 1", onClick = {}, size = CouiButtonSize.Small)
                            CouiButton(
                                text = "Small 2",
                                onClick = {},
                                size = CouiButtonSize.Small,
                                type = CouiButtonType.Secondary,
                            )
                            CouiButton(
                                text = "Small 3",
                                onClick = {},
                                size = CouiButtonSize.Small,
                                enabled = false,
                            )
                        }
                    }
                }

                CouiSmallTitle("Loading button")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiLoadingDemo() }
                CouiSmallTitle("Jelly tabs")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiJellyTabsDemo() }
                CouiSmallTitle("Progress")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiProgressDemo() }
                CouiSmallTitle("Dialog and snackbar")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiDialogDemo() }
                CouiSmallTitle("Three-state checkbox")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiTriStateDemo() }
                CouiSmallTitle("Floating button and tooltip")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) { CouiFabTooltipDemo() }

                CouiSmallTitle("Bottom navigation bar")

                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                    CouiPreferenceItem(
                        title = "Bottom navigation bar",
                        position = CouiPreferencePosition.Middle,
                        summary = if (barVisible) "Shown" else "Hidden",
                        onClick = { barVisible = !barVisible },
                        trailing = { CouiSwitch(checked = barVisible, onCheckedChange = { barVisible = it }) },
                    )
                    // The two accessory slots are independent: leaving one unconfigured is exactly
                    // the official's `isAccessoryIconConfigured == false` for that side, and
                    // `layoutPositionsMediumScreen` then centres only the slots that exist.
                    CouiPreferenceItem(
                        title = "Accessory slots",
                        position = CouiPreferencePosition.Single,
                        summary = when (accessories) {
                            0 -> "Both sides"
                            1 -> "Start only"
                            else -> "End only"
                        },
                        onClick = { accessories = (accessories + 1) % 3 },
                        trailing = {
                            CouiSwitch(
                                checked = accessories == 0,
                                onCheckedChange = { accessories = if (it) 0 else 1 },
                            )
                        },
                    )
                }

                CouiBlurSection()
                CouiSmallTitle("Spot light")
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(72.dp)
                                .couiSpotLight(
                                    spec = CouiSpotLightDefaults.Capsule2Light,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                                )
                                .background(Color(0xFF2B2B2B), androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CouiText("按住拖动", CouiTypography.body, color = Color.White)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // The page is captured into `pageBackdrop`; the bar samples it for the material blur.
            // The bar itself is outside this layer so it can never sample itself.
            Box(modifier = Modifier.fillMaxSize().couiBackdrop(pageBackdrop)) {
                pageContent()
            }
            if (barVisible) {
                // A real app floats the bar over its content at the bottom of the window; the gallery
                // shows the same placement, above whatever the host Scaffold already inset for the
                // system navigation bar.
                // The gallery's own bar is the three-slot variant: a pill plus a round button on
                // each side. `COUIBottomFloatingAccessoryNavigationBar` builds exactly that, and
                // its accessories are fixed slots rather than injectable views - so this mirrors
                // it with `startAccessory` / `endAccessory` lambdas instead of accepting children.
                CouiBottomFloatingBar(
                    items = barItems,
                    selectedIndex = barIndex,
                    onSelect = { barIndex = it },
                    startAccessory = if (accessories == 2) {
                        null
                    } else {
                        {
                            icon(barIcon)
                            contentDescription("图集")
                            onClick { barIndex = 0 }
                        }
                    },
                    endAccessory = if (accessories == 1) {
                        null
                    } else {
                        {
                            icon(barIcon)
                            contentDescription("更多")
                            onClick { barIndex = 1 }
                        }
                    },
                    // The material's background blur: the bar samples the captured page by
                    // reference in its own draw pass, so scrolling and the predictive-back scene
                    // transform stay in lockstep with the pill - no frozen slice possible.
                    backdrop = pageBackdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
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


@Composable
private fun CouiLoadingDemo() {
    var loading by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CouiLoadingButton(
            text = "Submit",
            loading = loading,
            onClick = { loading = false },
            modifier = Modifier.fillMaxWidth(),
        )
        CouiButton(
            text = if (loading) "Stop" else "Start",
            onClick = { loading = !loading },
            type = CouiButtonType.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CouiJellyTabsDemo() {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4")
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        CouiTabLayout(
            tabs = tabs,
            selectedIndex = tabIndex,
            onSelectedChange = { tabIndex = it },
        )
        Box(modifier = Modifier.padding(16.dp)) {
            CouiText(tabs[tabIndex], CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
        }
    }
}

@Composable
private fun CouiProgressDemo() {
    var progress by remember { mutableFloatStateOf(0.4f) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CouiCircularProgress(progress = progress)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            CouiHorizontalProgressBar(progress = progress)
            Spacer(Modifier.height(12.dp))
            CouiSeekBar(value = progress, onValueChange = { progress = it })
        }
    }
}

@Composable
private fun CouiDialogDemo() {
    var showDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showSnackBar by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CouiButton(text = "Dialog", onClick = { showDialog = true }, modifier = Modifier.weight(1f))
            CouiButton(text = "Sheet", onClick = { showSheet = true }, type = CouiButtonType.Secondary, modifier = Modifier.weight(1f))
        }
        CouiButton(text = "Show snackbar", onClick = { showSnackBar = true }, modifier = Modifier.fillMaxWidth())
    }
    CouiAlertDialog(
        visible = showDialog,
        title = "Delete this file?",
        message = "The file will be removed from this device. This cannot be undone.",
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = { showDialog = false },
        onDismissRequest = { showDialog = false },
    )
    CouiBottomSheet(
        visible = showSheet,
        onDismissRequest = { showSheet = false },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            CouiText("Bottom sheet", CouiTypography.listTitle)
            Spacer(Modifier.height(8.dp))
            CouiText(
                "Drag the panel down past half its height, tap the scrim, or go back to dismiss.",
                CouiTypography.body,
                emphasis = CouiTextEmphasis.Secondary,
            )
            Spacer(Modifier.height(24.dp))
            CouiButton(text = "Done", onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth())
        }
    }
    CouiSnackBar(
        visible = showSnackBar,
        message = "Swipe this bar sideways to dismiss it",
        onDismissRequest = { showSnackBar = false },
    )
}

@Composable
private fun CouiTriStateDemo() {
    val checked = remember { mutableStateListOf(true, false, true) }
    val masterState = when {
        checked.all { it } -> CouiCheckBoxState.All
        checked.any { it } -> CouiCheckBoxState.Part
        else -> CouiCheckBoxState.None
    }
    val setAll: (Boolean) -> Unit = { next -> for (i in checked.indices) checked[i] = next }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        CouiPreferenceItem(
            title = "Select all",
            position = CouiPreferencePosition.First,
            onClick = { setAll(masterState != CouiCheckBoxState.All) },
            showDivider = true,
            trailing = { CouiCheckBox(state = masterState, onStateChange = { setAll(it == CouiCheckBoxState.All) }) },
        )
        listOf("Option 1", "Option 2", "Option 3").forEachIndexed { index, title ->
            CouiPreferenceItem(
                title = title,
                position = if (index == 2) CouiPreferencePosition.Last else CouiPreferencePosition.Middle,
                onClick = { checked[index] = !checked[index] },
                showDivider = index != 2,
                trailing = {
                    CouiCheckBox(
                        state = if (checked[index]) CouiCheckBoxState.All else CouiCheckBoxState.None,
                        onStateChange = { checked[index] = it == CouiCheckBoxState.All },
                    )
                },
            )
        }
    }
}

@Composable
private fun CouiFabTooltipDemo() {
    val directions = listOf(
        CouiTooltipDirection.Up,
        CouiTooltipDirection.Down,
        CouiTooltipDirection.Left,
        CouiTooltipDirection.Right,
    )
    val anchorBounds = remember { mutableStateListOf<androidx.compose.ui.geometry.Rect?>(null, null, null, null) }
    var tooltipIndex by remember { mutableIntStateOf(-1) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
        CouiText("Tap a tile to pin a tooltip on it", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            directions.forEachIndexed { index, direction ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CouiTheme.colors.background)
                        .onGloballyPositioned { anchorBounds[index] = it.boundsInWindow() }
                        .clickable { tooltipIndex = if (tooltipIndex == index) -1 else index },
                    contentAlignment = Alignment.Center,
                ) {
                    CouiText(direction.name.first().toString(), CouiTypography.listTitle)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
            CouiExpandableFloatingButton(
                icon = rememberVectorPainter(CouiDemoIcons.Placeholder),
                items = listOf(
                    CouiFloatingButtonItem(label = "Scan"),
                    CouiFloatingButtonItem(label = "Share"),
                    CouiFloatingButtonItem(label = "New"),
                ),
                onItemSelected = { },
            )
        }
    }
    if (tooltipIndex in directions.indices) {
        val index = tooltipIndex
        CouiTooltip(
            visible = true,
            text = "Tooltip on the ${directions[index].name.lowercase()} side",
            anchorBounds = anchorBounds[index],
            direction = directions[index],
            onDismissRequest = { tooltipIndex = -1 },
        )
    }
}
