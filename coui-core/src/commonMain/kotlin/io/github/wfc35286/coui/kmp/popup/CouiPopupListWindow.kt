// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.wfc35286.coui.kmp.basic.toTextStyle
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

enum class CouiPopupItemType {
    Default,
    Header,
    Divider,
    GroupDivider,
}

@Immutable
data class CouiPopupListItem(
    val title: String = "",
    val enabled: Boolean = true,
    val type: CouiPopupItemType = CouiPopupItemType.Default,
    val checked: Boolean = false,
    val checkable: Boolean = false,
    val groupId: Int = DEFAULT_GROUP_ID,
    val subItems: List<CouiPopupListItem> = emptyList(),
    val leadingIcon: Painter? = null,
    val iconTint: Color? = null,
    val description: String? = null,
    val hint: (@Composable () -> Unit)? = null,
) {
    companion object {
        const val DEFAULT_GROUP_ID: Int = -1
    }
}

internal fun List<CouiPopupListItem>.withGroupDividers(): List<CouiPopupListItem> = couiPopupWithGroupDividers(this)

internal fun couiPopupSubMenuItems(parent: CouiPopupListItem, compact: Boolean): List<CouiPopupListItem> = (if (compact) listOf(parent) + parent.subItems else parent.subItems).withGroupDividers()

internal fun List<CouiPopupListItem>.displayToDataIndex(): List<Int> {
    val map = ArrayList<Int>(size)
    var data = 0
    for (item in this) {
        map += if (item.type == CouiPopupItemType.Default) data++ else -1
    }
    return map
}

internal fun List<CouiPopupListItem>.dataToDisplayIndex(dataIndex: Int): Int {
    if (dataIndex < 0) return -1
    var data = 0
    forEachIndexed { position, item ->
        if (item.type == CouiPopupItemType.Default) {
            if (data == dataIndex) return position
            data++
        }
    }
    return -1
}

object CouiPopupListWindowDefaults {
    val MinWidth: Dp = 168.dp
    val MinWidthWithSubMenu: Dp = 196.dp
    val MaxWidth: Dp = 256.dp
    val ItemMinHeight: Dp = 40.dp
    val ItemContentMinHeight: Dp get() = ItemMinHeight - ItemPaddingVertical * 2
    val HeaderMinHeight: Dp = 32.dp
    val ItemPaddingHorizontal: Dp = 20.dp

    // Row horizontal padding is 20dp whether or not a leading icon is present; only the
    // icon's visibility decides whether the icon column is occupied.
    val ItemPaddingHorizontalNoIcon: Dp = 20.dp
    val ItemPaddingVertical: Dp = 8.dp
    val ItemIconSize: Dp = 20.dp
    val ItemIconMarginEnd: Dp = 12.dp
    val HintGap: Dp = 4.dp
    val HintMaxWidth: Dp = 40.dp
    val DescriptionTextStyle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f), lineSpacingMultiplier = 1.0f)
    val PaddingVertical: Dp = 8.dp
    val AnchorGap: Dp = 8.dp
    val HeaderPaddingTop: Dp = 4.dp
    val HeaderPaddingBottom: Dp = 8.dp
    val StateIconGap: Dp = 8.dp
    val ScreenMarginHorizontal: Dp = 16.dp
    val ScreenMarginTop: Dp = 16.dp
    val ScreenMarginBottom: Dp = 32.dp
    val CornerRadius: Dp = 28.dp
    val MaskRadiusVerticalMultiLine: Dp = 28.dp
    val MaskRadiusHorizontalIconOnly: Dp = 17.dp
    val MaskRadiusHorizontalWithText: Dp = 23.dp
    val CornerWeight: Float = 2.5f
    val BlurRadiusPx: Int = 160

    val DividerHeight: Dp = 0.5.dp
    val DividerMarginHorizontal: Dp = 20.dp
    val DividerMarginStartWithIcon: Dp = 52.dp
    val GroupDividerHeight: Dp = 4.dp
    val GroupDividerTotalHeight: Dp = 12.dp

    /** Vertical inset of the menu's scrollbar track from the panel's top and bottom. */
    val ScrollBarMarginVertical: Dp = 12.dp
    val CheckboxSize: Dp = 24.dp
    val CheckboxMarginEnd: Dp = 18.dp
    val ContentMinWidthWithCheckbox: Dp = 150.dp

    val SubMenuOverlap: Dp = 14.dp
    val SubMenuMinGap: Dp = 12.dp

    val CompactWindowWidth: Dp = 600.dp
    val SubMenuTransitionSpring: CouiSpringData = CouiSpringData(0f, 0.35f)
    val TextStyle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(15f), lineSpacingMultiplier = 1.1f)

    val HeaderTextStyle: CouiTextStyle = CouiTextStyle(CouiFontSize.Scaled(12f), androidx.compose.ui.text.font.FontWeight.Medium)

    val OpenPathSpring: CouiSpringData = CouiSpringData(0.13f, 0.29f)
    val OpenScaleSpring: CouiSpringData = CouiSpringData(0.2f, 0.4f)
    val OpenOpacitySpring: CouiSpringData = CouiSpringData(0f, 0.27f)
    val ClosePathSpring: CouiSpringData = CouiSpringData(0f, 0.26f)
    val CloseScaleSpring: CouiSpringData = CouiSpringData(0f, 0.21f)
    val CloseOpacitySpring: CouiSpringData = CouiSpringData(0f, 0.23f)
}

@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
fun CouiPopupListWindow(
    expanded: Boolean,
    items: List<CouiPopupListItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onSubItemClick: (Int, Int) -> Unit = { _, _ -> },
    touchPoint: Offset? = null,
    useBackgroundBlur: Boolean = true,
    minWidth: Dp = CouiPopupListWindowDefaults.MinWidth,
    maxWidth: Dp = CouiPopupListWindowDefaults.MaxWidth,
    paneTitle: String = "",
    debugAutoOpenSubMenu: Int = -1,
) {
    require(minWidth > 0.dp && maxWidth >= minWidth)
    val displayItems = remember(items) { items.withGroupDividers() }
    val displaySelectedIndex =
        remember(items, selectedIndex) { displayItems.dataToDisplayIndex(selectedIndex) }
    var retained by remember { mutableStateOf(false) }
    val path = remember { Animatable(0f) }
    val scale = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val currentClick by rememberUpdatedState(onItemClick)
    val currentSubItem by rememberUpdatedState(onSubItemClick)
    LaunchedEffect(expanded) {
        if (expanded) retained = true
        if (!retained) return@LaunchedEffect
        val target = if (expanded) 1f else 0f
        coroutineScope {
            launch {
                path.animateTo(target, (if (expanded) CouiPopupListWindowDefaults.OpenPathSpring else CouiPopupListWindowDefaults.ClosePathSpring).toSpringSpec())
            }
            launch {
                scale.animateTo(target, (if (expanded) CouiPopupListWindowDefaults.OpenScaleSpring else CouiPopupListWindowDefaults.CloseScaleSpring).toSpringSpec())
            }
            launch {
                opacity.animateTo(target, (if (expanded) CouiPopupListWindowDefaults.OpenOpacitySpring else CouiPopupListWindowDefaults.CloseOpacitySpring).toSpringSpec())
            }
        }
        if (!expanded) retained = false
    }
    if ((!expanded && !retained) || items.isEmpty()) return

    val density = LocalDensity.current
    val palette = CouiTheme.colors
    val dark = palette.labelPrimary.red > 0.5f
    val textColor = if (dark) Color.White else Color.Black
    // Solid panel colour shown before the blur blends in: #242424 in dark mode, white in
    // light mode.
    val fallbackColor = if (dark) Color(0xFF242424) else Color.White
    val textStyle = CouiPopupListWindowDefaults.TextStyle
    val textMeasurer = rememberTextMeasurer()
    val itemTextStyle = couiPopupPlatformTextStyle(textStyle.toTextStyle(textColor, roundScaledFontSizeToPixel = true))
    val headerTextStyle = couiPopupPlatformTextStyle(CouiPopupListWindowDefaults.HeaderTextStyle.toTextStyle(textColor, roundScaledFontSizeToPixel = true))
    val hasIcon = displayItems.any { it.leadingIcon != null }
    val measuredWidth = displayItems.maxOfOrNull { item ->
        if (item.type == CouiPopupItemType.Default || item.type == CouiPopupItemType.Header) {
            val titleWidth = textMeasurer.measure(item.title, if (item.type == CouiPopupItemType.Header) headerTextStyle else itemTextStyle).size.width
            val descriptionWidth = item.description
                ?.takeIf { it.isNotEmpty() }
                ?.let { textMeasurer.measure(it, couiPopupPlatformTextStyle(CouiPopupListWindowDefaults.DescriptionTextStyle.toTextStyle(textColor, roundScaledFontSizeToPixel = true))).size.width }
                ?: 0
            val stateSlot = item.type == CouiPopupItemType.Default && (item.subItems.isNotEmpty() || item.checked || item.checkable)
            maxOf(titleWidth, descriptionWidth) + with(density) {
                val horizontal = if (hasIcon) {
                    CouiPopupListWindowDefaults.ItemPaddingHorizontal
                } else {
                    CouiPopupListWindowDefaults.ItemPaddingHorizontalNoIcon
                }
                horizontal.roundToPx() * 2 +
                    if (hasIcon) {
                        CouiPopupListWindowDefaults.ItemIconSize.roundToPx() + CouiPopupListWindowDefaults.ItemIconMarginEnd.roundToPx()
                    } else {
                        0
                    } +
                    if (stateSlot) {
                        CouiPopupListWindowDefaults.StateIconGap.roundToPx() + CouiPopupListWindowDefaults.CheckboxSize.roundToPx()
                    } else {
                        0 +
                            if (item.hint != null) CouiPopupListWindowDefaults.HintGap.roundToPx() + CouiPopupListWindowDefaults.HintMaxWidth.roundToPx() else 0
                    }
            }
        } else {
            0
        }
    } ?: 0
    val effectiveMinWidth = if (minWidth == CouiPopupListWindowDefaults.MinWidth && displayItems.any { it.subItems.isNotEmpty() || it.leadingIcon != null }) {
        CouiPopupListWindowDefaults.MinWidthWithSubMenu.coerceAtMost(maxWidth)
    } else {
        minWidth
    }
    val width = with(density) { measuredWidth.toDp().coerceIn(effectiveMinWidth, maxWidth) }
    val placement = remember { CouiPopupPlacement() }
    val touchMotion = remember { CouiPopupTouchMotion() }
    val visual = remember { CouiPopupVisualState({ path.value }, { scale.value }, { opacity.value }, { placement.origin }, touchMotion) }
    val scrollState = remember { ScrollState(0) }
    val subScrollState = remember { ScrollState(0) }
    val subMenuScope = rememberCoroutineScope()
    // Plain holder, not snapshot state: the content lambda parks lambdas here and the gesture
    // evaluates them at gesture time, so animated bounds are read at their current values
    // instead of freezing at the last recomposition (and reading them in the body would
    // recompose the menu every frame).
    val gestureBounds = remember { arrayOfNulls<() -> Rect>(2) }
    val mainPanel = remember { CouiPopupPanelState(touchMotion) }
    val locate = remember { CouiPopupLocateHelper() }
    val menuTransition = remember { CouiPopupTransitionDriver(locate) }
    val subMenuIndex = menuTransition.index
    val subMenuExpanded = menuTransition.opening
    val subEntered = menuTransition.entered
    var subSize by remember { mutableStateOf(IntSize.Zero) }
    var subMeasuredIndex by remember { mutableStateOf(-1) }
    var subMenuRows by remember { mutableStateOf(emptyMap<Int, Rect>()) }
    val subTouchMotion = remember { CouiPopupTouchMotion() }
    val subVisual = remember { CouiPopupVisualState({ 1f }, { 1f }, { opacity.value }, { Offset.Zero }, subTouchMotion) }
    val subPanel = remember { CouiPopupPanelState(subTouchMotion) }
    fun hideSubMenu() {
        menuTransition.hide()
        subTouchMotion.resetTouchOnly()
        // Fade out the press mask on both the main menu rows and the sub menu's parent row.
        mainPanel.clear()
        subPanel.clear()
    }
    fun showSubMenu(index: Int) {
        if (displayItems.getOrNull(index)?.let { it.enabled && it.subItems.isNotEmpty() } != true) return
        // Only the sub menu's parent row keeps a locked highlight while the sub menu is up; the
        // main menu row's press mask fades out on its own, which is what makes the row's shadow
        // appear to grow into the sub menu. Locking the main row here as well would leave two
        // masks on screen at once.
        val press = mainPanel.pressProgress
        mainPanel.clear()
        touchMotion.resetTouchOnly()
        val fresh = !menuTransition.visible
        if (fresh) {
            subSize = IntSize.Zero
            subMeasuredIndex = -1
            subPanel.rows.clear()
            subPanel.clear(immediate = true)
            subPanel.handoff(press)
        }
        menuTransition.show(index)
    }
    LaunchedEffect(menuTransition.index) {
        if (!menuTransition.visible) {
            subPanel.clear(immediate = true)
            subPanel.items = emptyList()
            subPanel.rows.clear()
            subSize = IntSize.Zero
        }
    }
    LaunchedEffect(expanded, debugAutoOpenSubMenu) {
        if (expanded && debugAutoOpenSubMenu >= 0) {
            delay(if (CouiPopupDebug32.slowMotion) 2500L else 400L)
            showSubMenu(debugAutoOpenSubMenu)
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            menuTransition.resume()
            subVisual.inheritedPivot = null
        } else {
            menuTransition.freeze()
            if (menuTransition.visible && subSize.width > 0 && subSize.height > 0) {
                val mainPivot = visual.geometry?.invoke()?.pivot ?: visual.pivot
                val mainFrame = visual.sampleFrame()
                val subFrame = subVisual.sampleFrame()
                val mainCenter = mainPanel.layoutOffset + visual.panelSize / 2f + mainFrame.translation
                val worldPivot = mainCenter + Offset(
                    visual.panelSize.x * (mainPivot.x - 0.5f) * mainFrame.scaleX,
                    visual.panelSize.y * (mainPivot.y - 0.5f) * mainFrame.scaleY,
                )
                val subCenter = subPanel.layoutOffset + subVisual.panelSize / 2f + subFrame.translation
                subVisual.inheritedPivot = Offset(
                    0.5f + (worldPivot.x - subCenter.x) / subSize.width,
                    0.5f + (worldPivot.y - subCenter.y) / subSize.height,
                )
            }
        }
    }
    DrivePopupTransition(menuTransition)
    DrivePopupTouchMotion(touchMotion)
    DrivePopupTouchMotion(subTouchMotion)
    LaunchedEffect(expanded, touchMotion, subTouchMotion) {
        if (!expanded) {
            touchMotion.up()
            subTouchMotion.up()
        }
    }
    // Predictive back.
    //
    // It has to be attached to the *popup window's* back dispatcher, not the Activity's: the menu
    // lives in its own window and the system delivers the gesture to the focused window, so a
    // `NavigationBackHandler` on the inherited (Activity) dispatcher never fires. See
    // [CouiPopupBackHandler] for the per-platform implementations.
    //
    // The gesture drives the *same* animatables the normal close uses (`path` = the bezier pull
    // towards the anchor, `scale`, `opacity`), so the preview is the real dismissal instead of a
    // second animation that has to be kept in sync with it:
    //   * dragging back -> the three animatables follow the finger (1 - progress),
    //   * released past the threshold -> the normal dismiss runs (`onDismissRequest` semantics),
    //   * released before it -> springs back to fully open.
    // While a sub menu is open, back closes the sub menu first (same as tapping outside) and the
    // panel is not previewed, because that close is a spring on the sub menu's own transition unit.
    var backPreview by remember { mutableStateOf(false) }
    var backOpen by remember { mutableFloatStateOf(1f) }
    if (backPreview) {
        // Snap the open/close animatables to the gesture instead of animating them: the system
        // already moves the finger, a spring here would lag behind it.
        LaunchedEffect(backOpen) {
            path.snapTo(backOpen)
            scale.snapTo(backOpen)
            opacity.snapTo(backOpen)
        }
    }
    val visibleWindow = couiPopupVisibleWindowBounds()
    val positionProvider = remember(density, touchPoint) {
        CouiPopupPositionProvider(
            touchPoint = touchPoint,
            horizontalMargin = with(density) { CouiPopupListWindowDefaults.ScreenMarginHorizontal.roundToPx() },
            topMargin = with(density) { CouiPopupListWindowDefaults.ScreenMarginTop.roundToPx() },
            bottomMargin = with(density) { CouiPopupListWindowDefaults.ScreenMarginBottom.roundToPx() },
            anchorGap = with(density) { CouiPopupListWindowDefaults.AnchorGap.roundToPx() },
            placement = placement,
        )
    }
    positionProvider.visibleWindow = visibleWindow
    val shape = remember(visual.useBackgroundBlur) { createCouiPopupShape(CouiPopupListWindowDefaults.CornerRadius, visual.useBackgroundBlur) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = {
            if (expanded) {
                if (subMenuExpanded) hideSubMenu() else currentDismiss()
            }
        },
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        // Must be called *inside* the popup content: `LocalView` then belongs to the popup's own
        // window, which is the window the system delivers the back gesture to. Called from the
        // parent composition it registers on the Activity's window instead and the gesture never
        // reaches the popup.
        CouiPopupBackHandler(
            enabled = expanded,
            onProgress = { progress ->
                val open = (1f - progress).coerceIn(0f, 1f)
                if (subMenuExpanded) {
                    // Back closes the sub menu first, so preview *that*: the driver parks both
                    // transition units at `open` of their rest value, which collapses the reveal
                    // slice back into the anchor row exactly like the real exit would.
                    menuTransition.previewSubMenuClose(open)
                } else {
                    backOpen = open
                    backPreview = true
                }
            },
            onCancelled = {
                backPreview = false
                backOpen = 1f
                if (subMenuExpanded) {
                    menuTransition.endSubMenuPreview()
                } else {
                    subMenuScope.launch {
                        coroutineScope {
                            launch { path.animateTo(1f, CouiPopupListWindowDefaults.OpenPathSpring.toSpringSpec()) }
                            launch { scale.animateTo(1f, CouiPopupListWindowDefaults.OpenScaleSpring.toSpringSpec()) }
                            launch { opacity.animateTo(1f, CouiPopupListWindowDefaults.OpenOpacitySpring.toSpringSpec()) }
                        }
                    }
                }
            },
            onCompleted = {
                backPreview = false
                if (subMenuExpanded) hideSubMenu() else currentDismiss()
            },
        )
        CouiPopupRootAlpha(visual)
        BoxWithConstraints(
            Modifier.fillMaxSize().couiPopupGestures(
                main = mainPanel,
                sub = subPanel,
                expanded = expanded,
                subExpanded = menuTransition.opening,
                subEntered = subEntered,
                // Read at gesture time, not during composition: these change every frame while
                // the panel animates, and reading them in the composable body would recompose
                // the whole menu subtree on every frame.
                mainBounds = { gestureBounds[0]?.invoke() ?: Rect.Zero },
                subBounds = { gestureBounds[1]?.invoke() ?: Rect.Zero },
                onLongPress = { anchor, index -> if (anchor) hideSubMenu() else showSubMenu(index) },
                onHideSubMenu = { hideSubMenu() },
                onDismiss = { currentDismiss() },
                consumeMainMenuTouchRestore = { menuTransition.consumeMainMenuTouchRestore() },
                consumeReDispatchDown = { menuTransition.consumeReDispatchDown() },
            ),
        ) {
            val viewport = IntSize(constraints.maxWidth, constraints.maxHeight)
            SideEffect {
            }
            val menuWidth = width.coerceAtMost((this.maxWidth - CouiPopupListWindowDefaults.ScreenMarginHorizontal * 2).coerceAtLeast(1.dp))
            var panelSize by remember { mutableStateOf(IntSize.Zero) }
            val panelOffset = positionProvider.menuOffset(viewport, panelSize)
            val panelBounds = Rect(panelOffset.x.toFloat(), panelOffset.y.toFloat(), (panelOffset.x + panelSize.width).toFloat(), (panelOffset.y + panelSize.height).toFloat())
            val compact = this.maxWidth < CouiPopupListWindowDefaults.CompactWindowWidth
            val layoutDirection = LocalLayoutDirection.current
            val subParent = displayItems.getOrNull(subMenuIndex)
            val subItems = remember(subParent, compact) {
                subParent?.let { parent ->
                    couiPopupSubMenuItems(parent, compact)
                }.orEmpty()
            }
            val subWidth = menuWidth
            val margins = positionProvider.availableBounds(viewport)
            val availableHeight = with(density) { margins.height.coerceAtLeast(1).toDp() }
            val row = subMenuRows[subMenuIndex] ?: Rect.Zero
            val subSizeForPlacement = if (subMeasuredIndex == subMenuIndex && subSize.width > 0) {
                IntSize(with(density) { menuWidth.roundToPx() }, subSize.height)
            } else {
                IntSize.Zero
            }
            val paddingPx = with(density) { CouiPopupListWindowDefaults.PaddingVertical.roundToPx().toFloat() }
            menuTransition.smallScreen = compact
            menuTransition.menuInnerPaddingVertical = paddingPx.toInt()
            menuTransition.minGapBetweenMainAndSubMenu = with(density) { CouiPopupListWindowDefaults.SubMenuMinGap.roundToPx() }
            // The reveal must not start before the platform material of the sub menu panel has
            // produced its first frame; see CouiPopupTransitionDriver.measured.
            menuTransition.materialReady = { subVisual.materialLive }
            val subPlacement = couiPopupSubMenuPlacement(
                locate = locate,
                main = panelBounds,
                anchorCenterX = placement.anchor.center.x,
                rowTop = row.top,
                subSize = subSizeForPlacement,
                available = margins,
                compact = compact,
                rtl = layoutDirection == LayoutDirection.Rtl,
                overlap = with(density) { CouiPopupListWindowDefaults.SubMenuOverlap.roundToPx() },
                minGap = with(density) { CouiPopupListWindowDefaults.SubMenuMinGap.roundToPx() },
                anchorRowHeight = row.height,
                innerPaddingPx = paddingPx.toInt(),
            )
            val subOffset = subPlacement.offset
            val enterPivot = Offset(
                if (panelSize.width > 0) ((placement.anchor.center.x - panelOffset.x).toFloat() / panelSize.width).coerceIn(0f, 1f) else 0.5f,
                if (panelBounds.center.y > placement.anchor.center.y) 0f else 1f,
            )
            visual.panelSize = Offset(panelSize.width.toFloat(), panelSize.height.toFloat())
            mainPanel.layoutOffset = Offset(panelOffset.x.toFloat(), panelOffset.y.toFloat())
            subVisual.panelSize = Offset(subSize.width.toFloat(), subSize.height.toFloat())
            subVisual.parentInfluence = visual
            subVisual.minimumTranslationY = (margins.top - subOffset.y).toFloat()
            subPanel.layoutOffset = Offset(subOffset.x.toFloat(), subOffset.y.toFloat())
            val mainGeometry = rememberUpdatedState<() -> CouiPopupPanelGeometry> {
                CouiPopupPanelGeometry(
                    scale = menuTransition.mainScaleX,
                    contentAlpha = menuTransition.mainContentAlpha,
                    pivot = if (menuTransition.visible) {
                        Offset(menuTransition.mainPivotX, menuTransition.mainPivotY)
                    } else {
                        enterPivot
                    },
                    translation = Offset(0f, menuTransition.mainTranslationY),
                )
            }
            val childGeometry = rememberUpdatedState<() -> CouiPopupPanelGeometry> {
                CouiPopupPanelGeometry(
                    scale = menuTransition.subScaleX,
                    // When the sub menu exits, the background and the rows must vanish on the
                    // same frame. Compose removal lands a frame later; zero the content alpha
                    // here so the title cannot outlive the material and visibly shift.
                    alpha = if (!menuTransition.visible) 0f else menuTransition.subAlpha,
                    pivot = Offset(menuTransition.subPivotX, menuTransition.subPivotY),
                    translation = Offset(0f, menuTransition.subTranslationY),
                    reveal = if (!menuTransition.visible) {
                        Rect.Zero
                    } else if (menuTransition.revealActive) {
                        Rect(
                            0f,
                            menuTransition.revealTop,
                            subSize.width.toFloat(),
                            menuTransition.revealTop + menuTransition.revealHeight,
                        )
                    } else {
                        null
                    },
                    outlineAlpha = menuTransition.outlineAlpha,
                )
            }
            visual.geometry = { mainGeometry.value() }
            subVisual.geometry = { childGeometry.value() }
            menuTransition.anchorGlobalTop = {
                mainPanel.coordinates?.takeIf { it.isAttached }
                    ?.localToWindow(Offset(0f, row.top))?.y?.toInt()
                    ?: (panelOffset.y + row.top).toInt()
            }
            menuTransition.mainMenuViewTranslationY = {
                visual.translation.y - visual.panelSize.y *
                    (menuTransition.mainPivotY - 0.5f) * (1f - visual.scaleY)
            }
            SideEffect {
                menuTransition.onResetSubMenuScroll = { subMenuScope.launch { subScrollState.animateScrollTo(0) } }
                CouiPopupDebug32.main = visual
                CouiPopupDebug32.sub = subVisual
                CouiPopupDebug32.driver = menuTransition
                CouiPopupDebug32.open = { showSubMenu(displayItems.indexOfFirst { it.subItems.isNotEmpty() }) }
                CouiPopupDebug32.close = { hideSubMenu() }
                CouiPopupDebug32.dismiss = { currentDismiss() }
                CouiPopupDebug32.mainOffset = mainPanel.layoutOffset
                CouiPopupDebug32.subOffset = subPanel.layoutOffset
                CouiPopupDebug32.row = row
                CouiPopupDebug32.mainPanel = mainPanel
                CouiPopupDebug32.subPanel = subPanel
            }
            SideEffect {
                val anchorKnown = subMenuIndex < 0 || subMenuRows.containsKey(subMenuIndex)
                if (subMeasuredIndex == subMenuIndex && subSize != IntSize.Zero && anchorKnown) menuTransition.measured()
            }
            val subBounds = Rect(subOffset.x.toFloat(), subOffset.y.toFloat(), (subOffset.x + subSize.width).toFloat(), (subOffset.y + subSize.height).toFloat())
            // Parked, not computed here: evaluating these in the body reads
            // visual.translation/scaleX/scaleY, which change every animated frame, and would
            // recompose the whole menu subtree on every frame.
            gestureBounds[0] = { couiPopupTransformedBounds(panelBounds, visual) }
            gestureBounds[1] = { if (subItems.isNotEmpty()) subBounds else Rect.Zero }
            Box(
                modifier = modifier
                    .offset { panelOffset }
                    .width(menuWidth)
                    .heightIn(max = availableHeight)
                    .onSizeChanged {
                        panelSize = it
                        touchMotion.size(it.width, it.height, density.density)
                    }
                    .semantics { this.paneTitle = paneTitle },
            ) {
                CouiPopupPanelBase(visual, shape, fallbackColor)
                CouiPopupMaterial(
                    modifier = Modifier.matchParentSize(),
                    dark = dark,
                    useBackgroundBlur = useBackgroundBlur,
                    fallbackColor = fallbackColor,
                    shape = shape,
                    visual = visual,
                )
                CouiPopupListContent(
                    items = displayItems,
                    selectedIndex = displaySelectedIndex,
                    panelState = mainPanel,
                    scrollState = scrollState,
                    onSubMenuItem = { showSubMenu(it) },
                    onRowBoundsChange = { index, bounds ->
                        if (subMenuRows[index] != bounds) subMenuRows = subMenuRows + (index to bounds)
                    },
                    modifier = Modifier.graphicsLayer {
                        translationX = visual.translation.x
                        translationY = visual.translation.y
                        scaleX = visual.scaleX
                        scaleY = visual.scaleY
                        alpha = visual.contentAlpha
                        transformOrigin = TransformOrigin.Center
                        // Keep the rows inside one offscreen layer for the whole animation. Without
                        // this the layer is dropped the moment scale/alpha land exactly on their
                        // resting values, and the text is then rasterised through a different path,
                        // which visibly snaps a row by 1px on that frame.
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
                    // Re-enable the main menu list as soon as the sub menu exit starts, so the
                    // user can reverse it before the spring settles.
                    enabled = expanded && !menuTransition.opening,
                    dark = dark,
                    shape = shape,
                    onItemClick = { displayIndex ->
                        val dataIndex = displayItems.displayToDataIndex().getOrNull(displayIndex) ?: -1
                        if (dataIndex >= 0) currentClick(dataIndex)
                    },
                )
            }
            if (subItems.isNotEmpty()) {
                key(subMenuIndex) {
                    val subShape = remember(shape) { shape }
                    Box(
                        modifier = Modifier
                            .offset { subOffset }
                            .width(subWidth)
                            .heightIn(max = (availableHeight - if (compact) 0.dp else CouiPopupListWindowDefaults.SubMenuMinGap).coerceAtLeast(1.dp))
                            .onSizeChanged {
                                subSize = it
                                subMeasuredIndex = subMenuIndex
                                subTouchMotion.size(it.width, it.height, density.density)
                            },
                    ) {
                        CouiPopupPanelBase(subVisual, shape, fallbackColor)
                        CouiPopupMaterial(
                            modifier = Modifier.matchParentSize(),
                            dark = dark,
                            useBackgroundBlur = useBackgroundBlur,
                            fallbackColor = fallbackColor,
                            shape = shape,
                            visual = subVisual,
                        )
                        CouiPopupListContent(
                            items = subItems,
                            selectedIndex = -1,
                            panelState = subPanel,
                            scrollState = subScrollState,
                            showScrollBar = true,
                            clipContent = false,
                            enabled = expanded && menuTransition.visible,
                            dark = dark,
                            shape = subShape,
                            subMenuParentIndex = if (compact) 0 else -1,
                            subMenuParentActive = subMenuExpanded,
                            onSubMenuItem = {
                                if (compact && it == 0) {
                                    if (subMenuExpanded) hideSubMenu() else showSubMenu(subMenuIndex)
                                }
                            },
                            onItemClick = { displayIndex ->
                                if (compact && displayIndex == 0) {
                                    hideSubMenu()
                                } else {
                                    val index = subItems.displayToDataIndex()[displayIndex] - if (compact) 1 else 0
                                    val parentIndex = displayItems.displayToDataIndex()[subMenuIndex]
                                    if (index >= 0 && parentIndex >= 0) currentSubItem(parentIndex, index)
                                    hideSubMenu()
                                }
                            },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = subVisual.translation.x
                                    translationY = subVisual.translation.y
                                    scaleX = subVisual.scaleX
                                    scaleY = subVisual.scaleY
                                    alpha = subVisual.contentAlpha
                                    this.shape = CouiPopupRevealShape(subShape, subVisual.contentReveal)
                                    clip = true
                                    transformOrigin = TransformOrigin.Center
                                    // Same reason as the main menu's layer above: keep the rows
                                    // inside one offscreen layer for the whole animation. Dropping
                                    // it the moment alpha/scale land exactly on their resting values
                                    // re-rasterises the text through a different path, which steps
                                    // the text a pixel right after the collapse finishes.
                                    compositingStrategy = CompositingStrategy.Offscreen
                                },
                        )
                    }
                }
            }
        }
    }
}

// The panel surface paints an opaque material colour first and only then swaps in the background
// blur. The blur emits nothing for its first frames, so this base paints the material colour
// underneath until `CouiPopupVisualState.materialLive` reports that the platform material has
// produced a frame; from then on the base is gone and the panel is exactly the platform material.

@Composable
private fun BoxScope.CouiPopupPanelBase(
    visual: CouiPopupVisualState,
    shape: Shape,
    color: Color,
) {
    if (visual.materialLive) return
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                val frame = visual.sampleFrame()
                val empty = frame.revealBounds != null && frame.revealBounds.height <= 0f
                translationX = frame.translation.x
                translationY = frame.translation.y
                scaleX = frame.scaleX
                scaleY = frame.scaleY
                alpha = if (empty) 0f else frame.alpha
                this.shape = CouiPopupRevealShape(shape, frame.revealBounds)
                clip = true
                transformOrigin = TransformOrigin.Center
            }
            .background(color),
    )
}

@Composable
internal fun CouiMenuCheckmark(color: Color) {
    val path = remember {
        Path().apply {
            moveTo(9f, 12.435f)
            lineTo(13.388f, 17f)
            lineTo(23f, 7f)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        with(drawContext.canvas) {
            save()
            scale(size.width / 24f, size.height / 24f)
            drawPath(
                path,
                androidx.compose.ui.graphics.Paint().apply {
                    this.color = color.copy(alpha = color.alpha * 0.9f)
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = 1.6f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                },
            )
            restore()
        }
    }
}

internal class CouiPopupPlacement {
    var origin: Offset by mutableStateOf(Offset.Zero)
    var anchor: IntRect by mutableStateOf(IntRect.Zero)
}

internal class CouiPopupPositionProvider(
    private val touchPoint: Offset?,
    private val horizontalMargin: Int,
    private val topMargin: Int,
    private val bottomMargin: Int,
    private val anchorGap: Int,
    private val placement: CouiPopupPlacement,
) : PopupPositionProvider {
    var visibleWindow: IntRect? = null

    fun availableBounds(windowSize: IntSize): IntRect {
        val visible = visibleWindow ?: IntRect(0, 0, windowSize.width, windowSize.height)
        val left = min(horizontalMargin, windowSize.width / 2)
        val right = max(left, windowSize.width - horizontalMargin)
        val top = (visible.top + topMargin).coerceIn(0, windowSize.height)
        val bottom = (visible.bottom - bottomMargin).coerceIn(top, windowSize.height)
        return IntRect(left, top, right, bottom)
    }

    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        placement.anchor = touchPoint?.let {
            val x = anchorBounds.left + it.x.toInt()
            val y = anchorBounds.top + it.y.toInt()
            IntRect(x, y, x, y)
        } ?: anchorBounds
        return IntOffset.Zero
    }

    fun menuOffset(windowSize: IntSize, popupContentSize: IntSize): IntOffset {
        val anchor = placement.anchor
        val available = availableBounds(windowSize)
        val left = available.left
        val right = available.right
        val top = available.top
        val bottom = available.bottom
        val x = (anchor.center.x - popupContentSize.width / 2).coerceIn(left, max(left, right - popupContentSize.width))
        val below = max(anchor.bottom + anchorGap, top)
        val above = min(anchor.top - anchorGap, bottom)
        val y = when {
            below + popupContentSize.height <= bottom -> below
            above - popupContentSize.height >= top -> above - popupContentSize.height
            else -> top
        }
        placement.origin = Offset(anchor.center.x - x - popupContentSize.width / 2f, anchor.center.y - y - popupContentSize.height / 2f)
        return IntOffset(x, y)
    }
}

internal fun couiPopupBezierOffset(origin: Offset, t: Float): Offset {
    val dx = -origin.x
    val dy = -origin.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < 1f) return origin * (1f - t)
    val gravity = 130f * ((distance - 30f) / 370f).coerceIn(0f, 1f) * min(abs(dx), abs(dy)) / (max(abs(dx), abs(dy)) + 0.0001f)
    var nx = -dy / distance
    var ny = dx / distance
    if (ny < 0f) {
        nx = -nx
        ny = -ny
    }
    if (abs(ny) < 0.001f) {
        nx = 0f
        ny = 1f
    }
    val arc = Offset(nx * gravity, ny * gravity)
    val p1 = origin * 0.65f + arc
    val p2 = origin * 0.35f + arc
    val u = 1f - t
    return origin * (u * u * u) + p1 * (3f * u * u * t) + p2 * (3f * u * t * t)
}

@Composable
internal expect fun CouiPopupRootAlpha(visual: CouiPopupVisualState)

internal expect fun createCouiPopupShape(radius: Dp, useBackgroundBlur: Boolean): Shape

@Composable
internal expect fun CouiPopupMaterial(
    dark: Boolean,
    useBackgroundBlur: Boolean,
    fallbackColor: Color,
    shape: Shape,
    visual: CouiPopupVisualState,
    modifier: Modifier = Modifier,
)

@Composable
internal expect fun couiPopupVisibleWindowBounds(): IntRect?
