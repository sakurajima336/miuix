// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import component.arrowSection
import LocalNavigator
import navigation.Route
import component.badgeSection
import component.basicComponentSection
import component.blurSection
import component.bottomSheetSection
import component.breadcrumbBarSection
import component.buttonSection
import component.cardSection
import component.checkboxSection
import component.colorPickerSection
import component.dialogSection
import component.dropdownSection
import component.numberPickerSection
import component.otherPageSection
import component.progressIndicatorSection
import component.radioButtonSection
import component.sliderSection
import component.snackbarSection
import component.spinnerSection
import component.switchSection
import component.tabRowSection
import component.textFieldSection
import component.tooltipSection
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.github.wfc35286.coui.kmp.component.CouiPopupListWindow
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

@Composable
fun MainPage(
    snackbarHostState: SnackbarHostState,
    padding: PaddingValues,
) {
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    var searchValue by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // The COUI sections on this page read their palette from `CouiTheme`; mirror the app's
    // color mode (AppTheme's `isInDarkTheme`) so both design systems stay in the same mode.
    val dark = ui.isInDarkTheme()
    io.github.wfc35286.coui.kmp.theme.CouiTheme(
        colors = if (dark) io.github.wfc35286.coui.kmp.token.CouiColorTokens.Dark
        else io.github.wfc35286.coui.kmp.token.CouiColorTokens.Light,
        componentColors = if (dark) io.github.wfc35286.coui.kmp.token.CouiComponentColors.Dark
        else io.github.wfc35286.coui.kmp.token.CouiComponentColors.Light,
    ) {
        MainPageContent(snackbarHostState, padding)
    }
}

@Composable
private fun MainPageContent(
    snackbarHostState: SnackbarHostState,
    padding: PaddingValues,
) {
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    var searchValue by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val notExpanded by remember { derivedStateOf { !expanded } }
    val onCancelSearch = remember {
        {
            expanded = false
            searchValue = ""
        }
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    var selectedIndex1 by remember { mutableIntStateOf(0) }
    var selectedIndex2 by remember { mutableIntStateOf(1) }
    var selectedIndex3 by remember { mutableIntStateOf(2) }
    val optionItems = remember(
        selectedIndex1,
        selectedIndex2,
        selectedIndex3,
    ) {
        listOf(
            DropdownEntry(
                items = listOf("Selection A-1", "Selection A-2")
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex1 == index,
                            onClick = { selectedIndex1 = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf("Selection B-1", "Selection B-2", "Selection B-3")
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex2 == index,
                            onClick = { selectedIndex2 = index },
                        )
                    },
            ),
            DropdownEntry(
                items = listOf("Selection C-1", "Selection C-2", "Selection C-3", "Selection C-4")
                    .mapIndexed { index, text ->
                        DropdownItem(
                            text = text,
                            selected = selectedIndex3 == index,
                            onClick = { selectedIndex3 = index },
                        )
                    },
            ),
        )
    }
    var cascadingSortIndex by remember { mutableIntStateOf(0) }
    var cascadingViewIndex by remember { mutableIntStateOf(0) }
    var cascadingFilterIndex by remember { mutableIntStateOf(0) }
    var cascadingCollapseOnSelection by remember { mutableStateOf(false) }
    val cascadingEntries = remember(
        cascadingSortIndex,
        cascadingViewIndex,
        cascadingFilterIndex,
        cascadingCollapseOnSelection,
    ) {
        val sortLabels = listOf("Sort by capture date", "Sort by date added")
        val viewLabels = listOf("Group by date", "Compact")
        val filterLabels = listOf("All items", "Camera album")
        listOf(
            DropdownEntry(
                items = sortLabels.mapIndexed { idx, label ->
                    DropdownItem(
                        text = label,
                        selected = cascadingSortIndex == idx,
                        onClick = { cascadingSortIndex = idx },
                    )
                },
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "Collapse On Selection",
                        selected = cascadingCollapseOnSelection,
                        onClick = {
                            cascadingCollapseOnSelection = !cascadingCollapseOnSelection
                        },
                    ),
                    DropdownItem(
                        text = "View mode",
                        children = viewLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = cascadingViewIndex == idx,
                                onClick = { cascadingViewIndex = idx },
                            )
                        },
                    ),
                    DropdownItem(
                        text = "Filter",
                        children = filterLabels.mapIndexed { idx, label ->
                            DropdownItem(
                                text = label,
                                selected = cascadingFilterIndex == idx,
                                onClick = { cascadingFilterIndex = idx },
                            )
                        },
                    ),
                ),
            ),
        )
    }
    var multiSelectedItems by remember {
        mutableStateOf(
            setOf(
                "Multi selection A-1",
                "Multi selection B-2",
                "Multi selection B-3",
            ),
        )
    }
    val multiSelectItems = remember(multiSelectedItems) {
        listOf(
            DropdownEntry(
                items = listOf(
                    "Multi selection A-1",
                    "Multi selection A-2",
                ).map { text ->
                    DropdownItem(
                        text = text,
                        selected = text in multiSelectedItems,
                        onClick = {
                            multiSelectedItems =
                                if (text in multiSelectedItems) {
                                    multiSelectedItems - text
                                } else {
                                    multiSelectedItems + text
                                }
                        },
                    )
                },
            ),
            DropdownEntry(
                items = listOf(
                    "Multi selection B-1",
                    "Multi selection B-2",
                    "Multi selection B-3",
                ).map { text ->
                    DropdownItem(
                        text = text,
                        selected = text in multiSelectedItems,
                        onClick = {
                            multiSelectedItems =
                                if (text in multiSelectedItems) {
                                    multiSelectedItems - text
                                } else {
                                    multiSelectedItems + text
                                }
                        },
                    )
                },
            ),
        )
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                AdaptiveTopAppBar(
                    title = "Home",
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    actions = {
                        // The three COUI menus below keep the original demo semantics, mapped onto
                        // `CouiPopupListItem`: miuix `selected` -> `checked + checkable` (checkmark),
                        // miuix `children` -> `subItems` (the COUI sub menu), and multi-select items
                        // keep their checked state instead of closing the menu.
                        TooltipBox(text = "Options") {
                            var menuExpanded by remember { mutableStateOf(false) }
                            var menuAnchor by remember { mutableStateOf<Offset?>(null) }
                            Box(Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    menuAnchor = awaitFirstDown(requireUnconsumed = false).position
                                }
                            }) {
                                Icon(
                                    imageVector = MiuixIcons.Tune,
                                    contentDescription = "Tune",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { menuExpanded = true },
                                )
                                if (menuAnchor != null) {
                                    // One COUI menu per original entry; the cascading second entry
                                    // becomes a main menu whose "View mode"/"Filter" rows carry the
                                    // sub menus as `subItems` (the COUI cascading equivalent).
                                    val sortItems = cascadingEntries[0].items.map { item ->
                                        io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                            title = item.text,
                                            checkable = true,
                                            checked = item.selected,
                                        )
                                    }
                                    val cascade = cascadingEntries[1].items
                                    val collapseItem = io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                        title = cascade[0].text,
                                        checkable = true,
                                        checked = cascade[0].selected,
                                    )
                                    val viewItem = io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                        title = cascade[1].text,
                                        subItems = cascade[1].children.orEmpty().map { child ->
                                            io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                                title = child.text,
                                                checkable = true,
                                                checked = child.selected,
                                            )
                                        },
                                    )
                                    val filterItem = io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                        title = cascade[2].text,
                                        subItems = cascade[2].children.orEmpty().map { child ->
                                            io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                                title = child.text,
                                                checkable = true,
                                                checked = child.selected,
                                            )
                                        },
                                    )
                                    val menuItems = buildList {
                                        add(sortItems.first())
                                        add(sortItems.last())
                                        add(viewItem)
                                        add(filterItem)
                                        add(collapseItem)
                                    }
                                    CouiPopupListWindow(
                                        expanded = menuExpanded,
                                        items = menuItems,
                                        selectedIndex = -1,
                                        onSubItemClick = { _, subIndex ->
                                            if (menuItems.getOrNull(subIndex)?.title == cascade[1].text) {
                                                cascadingViewIndex = subIndex
                                            } else {
                                                cascadingFilterIndex = subIndex
                                            }
                                        },
                                        onItemClick = { index ->
                                            when (menuItems.getOrNull(index)?.title) {
                                                sortItems.first().title -> cascadingSortIndex = 0
                                                sortItems.last().title -> cascadingSortIndex = 1
                                                collapseItem.title -> {
                                                    cascadingCollapseOnSelection = !cascadingCollapseOnSelection
                                                    if (cascadingCollapseOnSelection) menuExpanded = false
                                                }
                                            }
                                        },
                                        onDismissRequest = { menuExpanded = false },
                                        touchPoint = menuAnchor,
                                    )
                                }
                            }
                        }
                        TooltipBox(text = "Sort") {
                            var sortExpanded by remember { mutableStateOf(false) }
                            var sortAnchor by remember { mutableStateOf<Offset?>(null) }
                            Box(Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    sortAnchor = awaitFirstDown(requireUnconsumed = false).position
                                }
                            }) {
                                Icon(
                                    imageVector = MiuixIcons.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { sortExpanded = true },
                                )
                                if (sortAnchor != null) {
                                    // The original popup shows the three selections as one list
                                    // where the chosen row carries a checkmark; group ids keep the
                                    // divider between the A / B / C groups.
                                    var group = 0
                                    val menuItems = optionItems.flatMapIndexed { groupId, entry ->
                                        group = groupId
                                        entry.items.map { item ->
                                            io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                                title = item.text,
                                                checkable = true,
                                                checked = item.selected,
                                                groupId = groupId,
                                            )
                                        }
                                    }
                                    val clickTargets = optionItems.flatMap { entry ->
                                        entry.items.mapNotNull { it.onClick }
                                    }
                                    CouiPopupListWindow(
                                        expanded = sortExpanded,
                                        items = menuItems,
                                        selectedIndex = -1,
                                        onItemClick = { index ->
                                            // The display list adds dividers; map back by title.
                                            menuItems.getOrNull(index)?.let { clicked ->
                                                optionItems.forEach { entry ->
                                                    entry.items.firstOrNull { it.text == clicked.title }?.onClick?.invoke()
                                                }
                                            }
                                        },
                                        onDismissRequest = { sortExpanded = false },
                                        touchPoint = sortAnchor,
                                    )
                                }
                            }
                        }
                        TooltipBox(text = "Select all") {
                            var selectExpanded by remember { mutableStateOf(false) }
                            var selectAnchor by remember { mutableStateOf<Offset?>(null) }
                            Box(Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    selectAnchor = awaitFirstDown(requireUnconsumed = false).position
                                }
                            }) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = "SelectAll",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { selectExpanded = true },
                                )
                                if (selectAnchor != null) {
                                    val menuItems = multiSelectItems.flatMap { entry ->
                                        entry.items.map { item ->
                                            io.github.wfc35286.coui.kmp.component.CouiPopupListItem(
                                                title = item.text,
                                                checkable = true,
                                                checked = item.selected,
                                            )
                                        }
                                    }
                                    CouiPopupListWindow(
                                        expanded = selectExpanded,
                                        items = menuItems,
                                        selectedIndex = -1,
                                        onItemClick = { index ->
                                            // Multi-select: toggle and keep the menu open, like the
                                            // original dropdown.
                                            menuItems.getOrNull(index)?.let { item ->
                                                multiSelectedItems =
                                                    if (item.title in multiSelectedItems) {
                                                        multiSelectedItems - item.title
                                                    } else {
                                                        multiSelectedItems + item.title
                                                    }
                                            }
                                        },
                                        onDismissRequest = { selectExpanded = false },
                                        touchPoint = selectAnchor,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val contentPadding = pageContentPadding(innerPadding, padding, isWideScreen)
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.pageScrollModifiers(
                    appState.enableScrollEndHaptic,
                    appState.showTopAppBar,
                    topAppBarScrollBehavior,
                ),
                contentPadding = contentPadding,
            ) {
                item(key = "searchbar") {
                    SmallTitle(text = "SearchBar")
                    SearchBar(
                        modifier = Modifier.padding(bottom = 12.dp),
                        inputField = {
                            InputField(
                                query = searchValue,
                                onQueryChange = { searchValue = it },
                                onSearch = { expanded = false },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                label = "Search",
                            )
                        },
                        outsideEndAction = {
                            Text(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = onCancelSearch,
                                    ),
                                text = "Cancel",
                                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                                color = MiuixTheme.colorScheme.primary,
                            )
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        Column {
                            repeat(4) { idx ->
                                val resultText = "Suggestion $idx"
                                BasicComponent(
                                    title = resultText,
                                    onClick = {
                                        searchValue = resultText
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (notExpanded) {
                    basicComponentSection()
                    switchSection()
                    checkboxSection()
                    radioButtonSection()
                    buttonSection()
                    tabRowSection()
                    breadcrumbBarSection()
                    arrowSection()
                    dialogSection()
                    bottomSheetSection()
                    dropdownSection()
                    spinnerSection()
                    snackbarSection(snackbarHostState)
                    textFieldSection()
                    progressIndicatorSection()
                    sliderSection()
                    cardSection()
                    tooltipSection()
                    badgeSection()
                    numberPickerSection()
                    colorPickerSection()
                    blurSection()
                    otherPageSection()
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
            )
        }
    }
}
