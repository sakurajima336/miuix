// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalScrollBarApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import component.BackNavigationIcon
import io.github.wfc35286.coui.kmp.basic.CouiButton
import io.github.wfc35286.coui.kmp.basic.CouiButtonSize
import io.github.wfc35286.coui.kmp.basic.CouiButtonType
import io.github.wfc35286.coui.kmp.basic.CouiLoadingButton
import io.github.wfc35286.coui.kmp.basic.CouiTabLayout
import io.github.wfc35286.coui.kmp.example.CouiCard
import io.github.wfc35286.coui.kmp.example.CouiCategoryHeader
import io.github.wfc35286.coui.kmp.example.CouiCheckBox
import io.github.wfc35286.coui.kmp.example.CouiChevron
import io.github.wfc35286.coui.kmp.example.CouiChipRow
import io.github.wfc35286.coui.kmp.example.CouiDivider
import io.github.wfc35286.coui.kmp.example.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.example.CouiRadioButton
import io.github.wfc35286.coui.kmp.example.CouiSearchBar
import io.github.wfc35286.coui.kmp.example.CouiSeekBar
import io.github.wfc35286.coui.kmp.example.CouiSwitch
import io.github.wfc35286.coui.kmp.example.CouiTabRow
import io.github.wfc35286.coui.kmp.example.CouiTheme
import io.github.wfc35286.coui.kmp.overlay.CouiBottomSheet
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme
import utils.AdaptiveTopAppBar
import utils.BlurredBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop


private val CouiTabs = listOf("Item 1", "Item 2", "Item 3")
private val CouiChips = listOf("Item 1", "Item 2", "Item 3", "Item 4")

/**
 * A scratch page for the COUI control set in `coui/`.
 *
 * It renders every re-implemented ColorOS control against the real COUI palette so the geometry
 * and the tokens can be eyeballed side by side. This legacy prototype still uses the Miuix example shell; it is not a coui-core implementation.
 */
@Composable
fun CouiTestPage(
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                AdaptiveTopAppBar(
                    title = "COUI",
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        BackNavigationIcon(onClick = { navigator.pop() })
                    },
                )
            }
        },
    ) { innerPadding ->
        val contentPadding = pageContentPadding(
            innerPadding,
            padding,
            isWideScreen,
            extraTop = 12.dp,
            extraStart = if (isWideScreen) {
                0.dp
            } else {
                WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr)
            },
            extraEnd = WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(LayoutDirection.Ltr),
            extraBottom = 12.dp,
        )
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            CouiTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CouiTheme.colors.backgroundWithCard),
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.pageScrollModifiers(
                            appState.enableScrollEndHaptic,
                            appState.showTopAppBar,
                            topAppBarScrollBehavior,
                        ),
                        contentPadding = contentPadding,
                    ) {
                        item(key = "couiSwitch") { CouiSwitchSection() }
                        item(key = "couiSelect") { CouiSelectSection() }
                        item(key = "couiSeekBar") { CouiSeekBarSection() }
                        item(key = "couiTabs") { CouiTabsSection() }
                        item(key = "couiSearch") { CouiSearchSection() }
                        item(key = "couiList") { CouiListSection() }
                        item(key = "couiButtons") { CouiButtonSection() }
                        item(key = "couiNewLoading") { CouiNewLoadingSection() }
                        item(key = "couiJellyTabs") { CouiJellyTabsSection() }
                        item(key = "couiNewSheet") { CouiNewSheetSection() }
                        item(key = "couiPalette") { CouiPaletteSection() }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
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

@Composable
private fun CouiSwitchSection() {
    var firstChecked by rememberSaveable { mutableStateOf(true) }
    var secondChecked by rememberSaveable { mutableStateOf(false) }
    var thirdChecked by rememberSaveable { mutableStateOf(false) }

    CouiCategoryHeader(title = "Switch")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        // COUI rows toggle their switch when the row itself is tapped, not just the control.
        CouiPreferenceItem(
            title = "Title 1",
            summary = "Summary",
            onClick = { firstChecked = !firstChecked },
            showDivider = true,
            trailing = { CouiSwitch(checked = firstChecked, onCheckedChange = { firstChecked = it }) },
        )
        CouiPreferenceItem(
            title = "Title 2",
            summary = "Summary",
            onClick = { secondChecked = !secondChecked },
            showDivider = true,
            trailing = { CouiSwitch(checked = secondChecked, onCheckedChange = { secondChecked = it }) },
        )
        CouiPreferenceItem(
            title = "Title 3",
            summary = "Summary",
            enabled = false,
            trailing = { CouiSwitch(checked = thirdChecked, onCheckedChange = { thirdChecked = it }, enabled = false) },
        )
    }
}

@Composable
private fun CouiSelectSection() {
    var checkA by rememberSaveable { mutableStateOf(true) }
    var checkB by rememberSaveable { mutableStateOf(false) }
    var checkC by rememberSaveable { mutableStateOf(false) }
    var radioIndex by rememberSaveable { mutableIntStateOf(0) }

    CouiCategoryHeader(title = "Checkbox & Radio")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        CouiPreferenceItem(
            title = "Title 1",
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkA, onCheckedChange = { checkA = it }) },
        )
        CouiPreferenceItem(
            title = "Title 2",
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkB, onCheckedChange = { checkB = it }) },
        )
        CouiPreferenceItem(
            title = "Title 3",
            enabled = false,
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkC, onCheckedChange = { checkC = it }, enabled = false) },
        )
        listOf("Item 1", "Item 2", "Item 3").forEachIndexed { index, label ->
            CouiPreferenceItem(
                title = label,
                showDivider = index != 2,
                onClick = { radioIndex = index },
                trailing = {
                    CouiRadioButton(selected = radioIndex == index, onClick = { radioIndex = index })
                },
            )
        }
    }
}

@Composable
private fun CouiSeekBarSection() {
    var firstValue by remember { mutableFloatStateOf(0.35f) }
    var secondValue by remember { mutableFloatStateOf(0.7f) }

    CouiCategoryHeader(title = "SeekBar")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Title 1",
                    style = MiuixTheme.textStyles.main,
                    color = CouiTheme.colors.labelPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(firstValue * 100).toInt()}%",
                    style = MiuixTheme.textStyles.body2,
                    color = CouiTheme.colors.labelSecondary,
                )
            }
            CouiSeekBar(value = firstValue, onValueChange = { firstValue = it })
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Title 2",
                style = MiuixTheme.textStyles.main,
                color = CouiTheme.colors.labelPrimary,
            )
            CouiSeekBar(value = secondValue, onValueChange = { secondValue = it })
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Title 3",
                style = MiuixTheme.textStyles.main,
                color = CouiTheme.colors.labelSecondary,
            )
            CouiSeekBar(value = 0.5f, onValueChange = {}, enabled = false)
        }
    }
}

@Composable
private fun CouiTabsSection() {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var chipIndex by rememberSaveable { mutableIntStateOf(0) }

    CouiCategoryHeader(title = "Tabs")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            CouiTabRow(
                tabs = CouiTabs,
                selectedIndex = tabIndex,
                onSelectedChange = { tabIndex = it },
            )
            CouiDivider(inset = 0.dp)
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = CouiTabs[tabIndex],
                    style = MiuixTheme.textStyles.body2,
                    color = CouiTheme.colors.labelSecondary,
                )
            }
            CouiDivider(inset = 0.dp)
            Box(modifier = Modifier.padding(16.dp)) {
                CouiChipRow(
                    labels = CouiChips,
                    selectedIndex = chipIndex,
                    onSelectedChange = { chipIndex = it },
                )
            }
        }
    }
}

@Composable
private fun CouiSearchSection() {
    val searchState = rememberTextFieldState("")
    val colors = CouiTheme.colors

    CouiCategoryHeader(title = "Search")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            CouiSearchBar(
                state = searchState,
                hint = "Placeholder",
                onCancel = { searchState.edit { replace(0, length, "") } },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchState.text.isEmpty()) "Summary" else searchState.text.toString(),
                style = MiuixTheme.textStyles.body2,
                color = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun CouiListSection() {
    var masterSwitch by rememberSaveable { mutableStateOf(true) }

    CouiCategoryHeader(title = "List rows")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        CouiPreferenceItem(
            title = "Title 1",
            showDivider = true,
        )
        CouiPreferenceItem(
            title = "Title 2",
            summary = "Summary",
            showDivider = true,
        )
        CouiPreferenceItem(
            title = "Title 3",
            summary = "Summary",
            onClick = {},
            showDivider = true,
            trailing = { CouiChevron() },
        )
        CouiPreferenceItem(
            title = "Title 4",
            summary = "Summary",
            onClick = { masterSwitch = !masterSwitch },
            showDivider = true,
            trailing = { CouiSwitch(checked = masterSwitch, onCheckedChange = { masterSwitch = it }) },
        )
        CouiPreferenceItem(
            title = "Title 5",
            onClick = {},
            trailing = {
                Text(
                    text = "Value",
                    style = MiuixTheme.textStyles.body2,
                    color = CouiTheme.colors.labelSecondary,
                )
            },
        )
    }
}

@Composable
private fun CouiButtonSection() {
    CouiCategoryHeader(title = "Buttons")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiButton(
                text = "Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            CouiButton(
                text = "Primary disabled",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            CouiButton(
                text = "Secondary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                type = CouiButtonType.Secondary,
            )
            CouiButton(
                text = "Secondary disabled",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                type = CouiButtonType.Secondary,
                enabled = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CouiButton(text = "Small", onClick = {}, size = CouiButtonSize.Small)
                CouiButton(
                    text = "Small 2nd",
                    onClick = {},
                    size = CouiButtonSize.Small,
                    type = CouiButtonType.Secondary,
                )
                CouiButton(text = "Small off", onClick = {}, size = CouiButtonSize.Small, enabled = false)
            }
        }
    }
}

@Composable
private fun CouiPaletteSection() {
    val colors = CouiTheme.colors

    CouiCategoryHeader(title = "Palette (${if (colors.isLight) "Light" else "Dark"})")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CouiSwatchRow(
                label = "Neutrals",
                swatches = listOf(
                    "primaryNeutral" to colors.primaryNeutral,
                    "labelSecondary" to colors.labelSecondary,
                    "labelTertiary" to colors.labelTertiary,
                    "divider" to colors.divider,
                    "press" to colors.pressBackground,
                ),
            )
            CouiSwatchRow(
                label = "Brand",
                swatches = listOf(
                    "blue" to colors.blue,
                    "green" to colors.green,
                    "orange" to colors.orange,
                    "red" to colors.red,
                    "yellow" to colors.yellow,
                ),
            )
            CouiSwatchRow(
                label = "Surfaces",
                swatches = listOf(
                    "background" to colors.background,
                    "withCard" to colors.backgroundWithCard,
                    "card" to colors.card,
                    "elevated" to colors.backgroundElevated,
                ),
            )
        }
    }
}

@Composable
private fun CouiSwatchRow(
    label: String,
    swatches: List<Pair<String, Color>>,
) {
    Column {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = CouiTheme.colors.labelSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.forEach { (name, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.footnote2,
                        color = CouiTheme.colors.labelTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiNewLoadingSection() {
    var loading by rememberSaveable { mutableStateOf(false) }

    CouiCategoryHeader(title = "Loading Button")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiLoadingButton(
                text = "Submit",
                loading = loading,
                onClick = { loading = false },
                modifier = Modifier.weight(1f),
            )
            CouiButton(
                text = if (loading) "Stop" else "Start",
                onClick = { loading = !loading },
                type = CouiButtonType.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CouiJellyTabsSection() {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4")

    CouiCategoryHeader(title = "Jelly Tabs")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            CouiTabLayout(
                tabs = tabs,
                selectedIndex = tabIndex,
                onSelectedChange = { tabIndex = it },
            )
            CouiDivider(inset = 0.dp)
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = tabs[tabIndex],
                    style = MiuixTheme.textStyles.body2,
                    color = CouiTheme.colors.labelSecondary,
                )
            }
        }
    }
}

@Composable
private fun CouiNewSheetSection() {
    var showSheet by rememberSaveable { mutableStateOf(false) }

    CouiCategoryHeader(title = "Bottom Sheet")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.padding(16.dp)) {
            CouiButton(
                text = "Show bottom sheet",
                onClick = { showSheet = true },
            )
        }
    }
    CouiBottomSheet(
        visible = showSheet,
        onDismissRequest = { showSheet = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Bottom sheet",
                style = MiuixTheme.textStyles.main,
                color = CouiTheme.colors.labelPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Drag the panel down past half its height, tap the scrim, or go back to dismiss. It scales from 0.8 to 1.0 while it rises.",
                style = MiuixTheme.textStyles.body2,
                color = CouiTheme.colors.labelSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CouiButton(
                text = "Done",
                onClick = { showSheet = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
