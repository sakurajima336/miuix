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
import io.wfc35286.coui.kmp.example.CouiButton
import io.wfc35286.coui.kmp.example.CouiCard
import io.wfc35286.coui.kmp.example.CouiCategoryHeader
import io.wfc35286.coui.kmp.example.CouiCheckBox
import io.wfc35286.coui.kmp.example.CouiChevron
import io.wfc35286.coui.kmp.example.CouiChipRow
import io.wfc35286.coui.kmp.example.CouiDivider
import io.wfc35286.coui.kmp.example.CouiPreferenceItem
import io.wfc35286.coui.kmp.example.CouiRadioButton
import io.wfc35286.coui.kmp.example.CouiSearchBar
import io.wfc35286.coui.kmp.example.CouiSeekBar
import io.wfc35286.coui.kmp.example.CouiSwitch
import io.wfc35286.coui.kmp.example.CouiTabRow
import io.wfc35286.coui.kmp.example.CouiTheme
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

private val CouiTabs = listOf("General", "Display", "About")
private val CouiChips = listOf("All", "Recent", "Favorites", "Hidden")

/**
 * A scratch page for the COUI control set in `coui/`.
 *
 * It renders every re-implemented ColorOS control against the real COUI palette so the geometry
 * and the tokens can be eyeballed side by side. Nothing here touches the Miuix library API; the
 * page lives entirely in the example app.
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
    var wifi by rememberSaveable { mutableStateOf(true) }
    var bluetooth by rememberSaveable { mutableStateOf(false) }
    var airplane by rememberSaveable { mutableStateOf(false) }

    CouiCategoryHeader(title = "Switch")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        // COUI rows toggle their switch when the row itself is tapped, not just the control.
        CouiPreferenceItem(
            title = "Wi-Fi",
            summary = "Connected to Miuix-5G - tap the row to toggle",
            onClick = { wifi = !wifi },
            showDivider = true,
            trailing = { CouiSwitch(checked = wifi, onCheckedChange = { wifi = it }) },
        )
        CouiPreferenceItem(
            title = "Bluetooth",
            summary = "Off - tap the row to toggle",
            onClick = { bluetooth = !bluetooth },
            showDivider = true,
            trailing = { CouiSwitch(checked = bluetooth, onCheckedChange = { bluetooth = it }) },
        )
        CouiPreferenceItem(
            title = "Airplane mode",
            summary = "Disabled row",
            enabled = false,
            trailing = { CouiSwitch(checked = airplane, onCheckedChange = { airplane = it }, enabled = false) },
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
            title = "Auto-rotate",
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkA, onCheckedChange = { checkA = it }) },
        )
        CouiPreferenceItem(
            title = "Dark theme",
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkB, onCheckedChange = { checkB = it }) },
        )
        CouiPreferenceItem(
            title = "Disabled checkbox",
            enabled = false,
            showDivider = true,
            trailing = { CouiCheckBox(checked = checkC, onCheckedChange = { checkC = it }, enabled = false) },
        )
        listOf("Never sleep", "30 seconds", "1 minute").forEachIndexed { index, label ->
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
    var volume by remember { mutableFloatStateOf(0.35f) }
    var brightness by remember { mutableFloatStateOf(0.7f) }

    CouiCategoryHeader(title = "SeekBar")
    CouiCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Volume",
                    style = MiuixTheme.textStyles.main,
                    color = CouiTheme.colors.labelPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MiuixTheme.textStyles.body2,
                    color = CouiTheme.colors.labelSecondary,
                )
            }
            CouiSeekBar(value = volume, onValueChange = { volume = it })
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Brightness",
                style = MiuixTheme.textStyles.main,
                color = CouiTheme.colors.labelPrimary,
            )
            CouiSeekBar(value = brightness, onValueChange = { brightness = it })
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Disabled",
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
                    text = "Selected tab: ${CouiTabs[tabIndex]}",
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
                hint = "Search settings",
                onCancel = { searchState.edit { replace(0, length, "") } },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchState.text.isEmpty()) "Query is empty" else "Query: ${searchState.text}",
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
            title = "Title only",
            showDivider = true,
        )
        CouiPreferenceItem(
            title = "Title with summary",
            summary = "The summary sits under the title at 54% opacity",
            showDivider = true,
        )
        CouiPreferenceItem(
            title = "Navigates somewhere",
            summary = "Trailing chevron",
            onClick = {},
            showDivider = true,
            trailing = { CouiChevron() },
        )
        CouiPreferenceItem(
            title = "Master switch",
            summary = "Trailing switch, row tap toggles too",
            onClick = { masterSwitch = !masterSwitch },
            showDivider = true,
            trailing = { CouiSwitch(checked = masterSwitch, onCheckedChange = { masterSwitch = it }) },
        )
        CouiPreferenceItem(
            title = "Trailing status text",
            onClick = {},
            trailing = {
                Text(
                    text = "Enabled",
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CouiButton(text = "Primary", onClick = {})
            CouiButton(text = "Secondary", onClick = {}, primary = false)
            CouiButton(text = "Disabled", onClick = {}, enabled = false)
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
