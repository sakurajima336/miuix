// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBar
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBarDefaults
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBarItem
import io.github.wfc35286.coui.kmp.component.CouiCard
import io.github.wfc35286.coui.kmp.component.CouiCardDefaults
import io.github.wfc35286.coui.kmp.component.CouiPopupListItem
import io.github.wfc35286.coui.kmp.component.CouiPopupListWindow
import io.github.wfc35286.coui.kmp.component.couiBackdrop
import io.github.wfc35286.coui.kmp.component.LocalCouiSceneTransition
import io.github.wfc35286.coui.kmp.component.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.component.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.component.CouiRedDotType
import io.github.wfc35286.coui.kmp.component.CouiSmallTitle
import io.github.wfc35286.coui.kmp.component.CouiSwitch
import io.github.wfc35286.coui.kmp.component.CouiText
import io.github.wfc35286.coui.kmp.component.CouiToolbar
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiColorTokens
import io.github.wfc35286.coui.kmp.token.CouiComponentColors
import io.github.wfc35286.coui.kmp.token.CouiTypography
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope

/**
 * The **dedicated** floating-navigation-bar test page.
 *
 * A scrolling body runs *under* the pill so the material's backdrop blur, the inner stroke and the
 * press/spotlight feedback can all be judged in place. A centred button opens a COUI popup menu
 * that reconfigures the bench live: page background (including a custom colour), item count,
 * per-item colouring, bar orientation, dark/light theme and the material/shadow bisect switches.
 */
@Composable
fun CouiBottomNavBarTestPage(
    padding: PaddingValues,
    onBack: (() -> Unit)? = null,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    // The page captures itself into a GraphicsLayer so the pill can blur it *inside* Compose. That
    // is what keeps the material alive through predictive back and on devices without the Oplus
    // material framework (see `CouiBackdrop`).
    val backdrop = io.github.wfc35286.coui.kmp.component.rememberCouiBackdrop()
    var materialEnabled by rememberSaveable { mutableStateOf(true) }
    var shadowEnabled by rememberSaveable { mutableStateOf(true) }
    // Bench knobs driven from the popup menu.
    var itemCount by rememberSaveable { mutableIntStateOf(5) }
    // 0 = theme default, 1 = per-item palette, 2 = custom single colour.
    var itemColorMode by rememberSaveable { mutableIntStateOf(0) }
    var customItemColorArgb by rememberSaveable { mutableIntStateOf(0xFF7C4DFF.toInt()) }
    // 0 = follow the app, 1 = force light, 2 = force dark.
    var themeMode by rememberSaveable { mutableIntStateOf(0) }
    val appDark = ui.isInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(appDark) }
    // Keep following the app until the menu pins a mode.
    LaunchedEffect(appDark) {
        if (themeMode == 0) darkTheme = appDark
    }
    // Page background: 0 = backgroundWithCard, 1 = background, 2 = black, 3 = custom.
    var backgroundMode by rememberSaveable { mutableIntStateOf(0) }
    var customBackgroundArgb by rememberSaveable { mutableIntStateOf(0xFF10243E.toInt()) }

    val iconPainters = listOf(
        CouiNavBarIcons.Home,
        CouiNavBarIcons.Game,
        CouiNavBarIcons.App,
        CouiNavBarIcons.Rank,
        CouiNavBarIcons.Mine,
    ).map { rememberVectorPainter(it) }
    val itemTitles = remember(itemCount) {
        val pool = listOf("首页", "游戏", "软件", "榜单", "我的")
        List(itemCount) { pool[it % pool.size] }
    }
    val perItemPalette = listOf(
        Color(0xFF5B8DEF),
        Color(0xFF48C08C),
        Color(0xFFEBB344),
        Color(0xFFE9605A),
        Color(0xFF9A6BDF),
    )
    val barItems = itemTitles.mapIndexed { index, title ->
        val tint = when (itemColorMode) {
            1 -> perItemPalette[index % perItemPalette.size]
            2 -> Color(customItemColorArgb)
            else -> CouiBottomFloatingNavigationBarDefaults.titleColor(index == selected)
        }
        CouiBottomFloatingNavigationBarItem(
            id = index,
            title = title,
            icon = iconPainters[index % iconPainters.size],
            iconTint = tint,
            redDotType = if (index == itemCount - 1 && itemCount >= 2) CouiRedDotType.Number else CouiRedDotType.None,
            redDotNumber = 15,
        )
    }

    // The popup menu. Item order mirrors the bench knobs; checkable rows show their state.
    var menuExpanded by remember { mutableStateOf(false) }
    var menuTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val menuItems = listOf(
        CouiPopupListItem("背景：卡片色", checkable = true, checked = backgroundMode == 0),
        CouiPopupListItem("背景：页面色", checkable = true, checked = backgroundMode == 1),
        CouiPopupListItem("背景：纯黑", checkable = true, checked = backgroundMode == 2),
        CouiPopupListItem("背景：自定义", checkable = true, checked = backgroundMode == 3),
        CouiPopupListItem("分隔"),
        CouiPopupListItem("条目：3 个", checkable = true, checked = itemCount == 3),
        CouiPopupListItem("条目：4 个", checkable = true, checked = itemCount == 4),
        CouiPopupListItem("条目：5 个", checkable = true, checked = itemCount == 5),
        CouiPopupListItem("条目：7 个", checkable = true, checked = itemCount == 7),
        CouiPopupListItem("分隔"),
        CouiPopupListItem("图标色：主题", checkable = true, checked = itemColorMode == 0),
        CouiPopupListItem("图标色：逐项", checkable = true, checked = itemColorMode == 1),
        CouiPopupListItem("图标色：自定义", checkable = true, checked = itemColorMode == 2),
        CouiPopupListItem("分隔"),
        CouiPopupListItem("分隔"),
        CouiPopupListItem("主题：暗色", checkable = true, checked = themeMode == 2 || (themeMode == 0 && darkTheme)),
        CouiPopupListItem("主题：浅色", checkable = true, checked = themeMode == 1 || (themeMode == 0 && !darkTheme)),
        CouiPopupListItem("分隔"),
        CouiPopupListItem("框架材质", checkable = true, checked = materialEnabled),
        CouiPopupListItem("框架阴影", checkable = true, checked = shadowEnabled),
    )
    // Group ids make the "分隔" pseudo-rows real group headers, same as the official mixed menu.
    val groupedMenuItems = menuItems.map { item ->
        if (item.title == "分隔") CouiPopupListItem(title = item.title, groupId = menuItems.indexOf(item)) else item
    }

    fun handleMenuClick(index: Int) {
        when (menuItems.getOrNull(index)?.title) {
            "背景：卡片色" -> backgroundMode = 0
            "背景：页面色" -> backgroundMode = 1
            "背景：纯黑" -> backgroundMode = 2
            "背景：自定义" -> backgroundMode = 3
            "条目：3 个" -> { itemCount = 3; selected = selected.coerceAtMost(2) }
            "条目：4 个" -> { itemCount = 4; selected = selected.coerceAtMost(3) }
            "条目：5 个" -> { itemCount = 5; selected = selected.coerceAtMost(4) }
            "条目：7 个" -> { itemCount = 7; selected = selected.coerceAtMost(6) }
            "图标色：主题" -> itemColorMode = 0
            "图标色：逐项" -> itemColorMode = 1
            "图标色：自定义" -> itemColorMode = 2
            "主题：暗色" -> { themeMode = 2; darkTheme = true }
            "主题：浅色" -> { themeMode = 1; darkTheme = false }
            "框架材质" -> materialEnabled = !materialEnabled
            "框架阴影" -> shadowEnabled = !shadowEnabled
            else -> menuExpanded = false
        }
    }

    // The bench's own palette. It must be resolved from `darkTheme` and **not** read through
    // `CouiTheme.colors` here: this line runs outside the `CouiTheme { }` below, so the read would
    // pick up the host app's palette - which is exactly how the page ended up with white (dark-theme)
    // labels on the light `backgroundWithCard`.
    val palette = if (darkTheme) CouiColorTokens.Dark else CouiColorTokens.Light
    val componentPalette = if (darkTheme) CouiComponentColors.Dark else CouiComponentColors.Light
    val pageBackground = when (backgroundMode) {
        0 -> palette.backgroundWithCard
        1 -> palette.background
        2 -> Color.Black
        else -> Color(customBackgroundArgb)
    }

    // The colorOS material is a backdrop sampler inside an interop View; it must not be painted
    // while the navigation scene is transformed (predictive back), otherwise the pill samples past
    // the shrinking scene layer and HWUI fills that part with black. `miuix-nav` knows exactly when
    // that happens, so the page forwards its `isRunning` into the COUI composition local.
    val navTransition = LocalNavTransitionScope.current
    androidx.compose.runtime.CompositionLocalProvider(
        LocalCouiSceneTransition provides { navTransition.isRunning },
        io.github.wfc35286.coui.kmp.component.LocalCouiBackdrop provides backdrop,
    ) {
    CouiTheme(colors = palette, componentColors = componentPalette) {
        val scrollState = rememberScrollState()
        val pageContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBackground)
                    .then(Modifier.couiBackdrop(backdrop))
                    .verticalScroll(scrollState)
                    .padding(
                        top = padding.calculateTopPadding() +
                            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    ),
            ) {
                if (onBack != null) {
                    CouiToolbar(title = "悬浮导航栏", onBack = onBack)
                } else {
                    CouiText(
                        text = "悬浮导航栏",
                        style = CouiTypography.windowTitle,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                // The bench button. Anchors the popup to the touch point, exactly like the
                // official `showAtLocation(anchor)` flow.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CouiTheme.colors.primary)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    menuTouchPoint = awaitFirstDown(requireUnconsumed = false).position
                                    menuExpanded = true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CouiText("菜单", style = CouiTypography.body, color = Color.White)
                    }
                }
                CouiSmallTitle("当前状态")
                CouiCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin),
                ) {
                    CouiPreferenceItem(
                        title = "条目 ${itemCount}",
                        summary = "背景模式 $backgroundMode · 图标色模式 $itemColorMode",
                        position = CouiPreferencePosition.First,
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "框架材质",
                        summary = if (materialEnabled) "Oplus 材质" else "回退面板",
                        position = CouiPreferencePosition.Middle,
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "选中",
                        summary = "index $selected",
                        position = CouiPreferencePosition.Last,
                    )
                }
                // Enough filler that the pill always has content behind it, in every background.
                repeat(4) { group ->
                    CouiSmallTitle("填充内容 ${group + 1}")
                    CouiCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin),
                    ) {
                        repeat(3) { row ->
                            CouiPreferenceItem(
                                title = "条目 ${group * 3 + row + 1}",
                                position = if (row == 2) CouiPreferencePosition.Last else CouiPreferencePosition.Middle,
                                summary = "用来给悬浮胶囊提供模糊背景",
                                trailing = { CouiSwitch(checked = row % 2 == 0, onCheckedChange = {}) },
                                showDivider = row != 2,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(120.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Keep the last touch point fresh: the popup anchors where the finger lands.
                        awaitFirstDown(requireUnconsumed = false)
                    }
                },
        ) {
            pageContent()
            // The bar: the official component is horizontal, floating above the gesture bar.
            CouiBottomFloatingNavigationBar(
                items = barItems,
                selectedIndex = selected,
                onSelected = { selected = it },
                materialEnabled = materialEnabled,
                shadowEnabled = shadowEnabled,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            CouiPopupListWindow(
                expanded = menuExpanded,
                items = groupedMenuItems,
                selectedIndex = -1,
                onItemClick = ::handleMenuClick,
                onDismissRequest = { menuExpanded = false },
                touchPoint = menuTouchPoint,
            )
        }
    }
    }
}

/** Neutral 24dp outline glyphs for the tabs, drawn locally so the page has no icon dependency. */
private object CouiNavBarIcons {
    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    val Home: ImageVector = icon(
        "CouiNavHome",
        "M3.4,10.6 L12,3.6 L20.6,10.6 L20.6,20.2 L14.6,20.2 L14.6,14.4 L9.4,14.4 L9.4,20.2 L3.4,20.2 Z",
    )
    val Game: ImageVector = icon(
        "CouiNavGame",
        "M7.6,8.4 L16.4,8.4 C19,8.4 20.4,10 20.4,12.6 C20.4,15.2 19,16.8 16.4,16.8 " +
            "L7.6,16.8 C5,16.8 3.6,15.2 3.6,12.6 C3.6,10 5,8.4 7.6,8.4 Z " +
            "M8.4,11.2 L8.4,14.2 M6.9,12.7 L9.9,12.7 M15.4,12.1 L15.5,12.1 M17.1,13.8 L17.2,13.8",
    )
    val App: ImageVector = icon(
        "CouiNavApp",
        "M4.2,4.2 L9.6,4.2 L9.6,9.6 L4.2,9.6 Z M14.4,4.2 L19.8,4.2 L19.8,9.6 L14.4,9.6 Z " +
            "M4.2,14.4 L9.6,14.4 L9.6,19.8 L4.2,19.8 Z M14.4,14.4 L19.8,14.4 L19.8,19.8 L14.4,19.8 Z",
    )
    val Rank: ImageVector = icon(
        "CouiNavRank",
        "M4.4,14.2 L8.2,14.2 L8.2,20 L4.4,20 Z M10.1,4 L13.9,4 L13.9,20 L10.1,20 Z " +
            "M15.8,9.6 L19.6,9.6 L19.6,20 L15.8,20 Z",
    )
    val Mine: ImageVector = icon(
        "CouiNavMine",
        "M12,4.6 C13.9,4.6 15.4,6.1 15.4,8 C15.4,9.9 13.9,11.4 12,11.4 C10.1,11.4 8.6,9.9 8.6,8 " +
            "C8.6,6.1 10.1,4.6 12,4.6 Z M5.2,19.6 C5.2,15.9 8.2,13.4 12,13.4 C15.8,13.4 18.8,15.9 18.8,19.6",
    )
}