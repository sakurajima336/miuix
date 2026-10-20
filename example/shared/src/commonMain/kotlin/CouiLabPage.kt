// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBar
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBarDefaults
import io.github.wfc35286.coui.kmp.component.CouiBottomFloatingNavigationBarItem
import io.github.wfc35286.coui.kmp.component.CouiCard
import io.github.wfc35286.coui.kmp.component.CouiCardDefaults
import io.github.wfc35286.coui.kmp.component.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.component.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.component.CouiRedDotType
import io.github.wfc35286.coui.kmp.component.CouiSectionSeekBar
import io.github.wfc35286.coui.kmp.component.CouiSeekBar
import io.github.wfc35286.coui.kmp.component.CouiSwitch
import io.github.wfc35286.coui.kmp.component.CouiText
import io.github.wfc35286.coui.kmp.component.CouiToolbar
import io.github.wfc35286.coui.kmp.component.couiBackdrop
import io.github.wfc35286.coui.kmp.component.rememberCouiBackdrop
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiColorTokens
import io.github.wfc35286.coui.kmp.token.CouiComponentColors
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography
import kotlin.math.roundToInt

/**
 * The **component lab**: a neutral stage for judging any COUI component.
 *
 * Unlike the per-component pages this one owns nothing but the stage:
 *
 * - the page itself is intentionally empty - a background (solid colour or a user-picked photo), a
 *   brightness scrim on top of it, and the component under test;
 * - a draggable floating ball opens the stage settings (background photo, scrim strength, light/dark)
 *   and can be parked anywhere so it never covers what you are looking at;
 * - a chip row switches which component is on the stage, so the same backdrop/blur/contrast setup can
 *   be reused for every component without editing code.
 *
 * The stage is captured into a [CouiBackdrop], which is what floating components use for their
 * material: it keeps the blur alive through Compose scene transforms and works without the Oplus
 * framework.
 */
@Composable
fun CouiLabPage(
    padding: PaddingValues,
    onBack: (() -> Unit)? = null,
) {
    var dark by rememberSaveable { mutableStateOf(false) }
    var dim by rememberSaveable { mutableFloatStateOf(0f) }
    var component by rememberSaveable { mutableIntStateOf(0) }
    var background by remember { mutableStateOf<ImageBitmap?>(null) }
    var panelOpen by remember { mutableStateOf(false) }
    var ballOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    val stageBackdrop = rememberCouiBackdrop()
    val palette = if (dark) CouiColorTokens.Dark else CouiColorTokens.Light
    val componentPalette = if (dark) CouiComponentColors.Dark else CouiComponentColors.Light
    val themeBackground = palette.backgroundWithCard
    val pickImage = rememberLabImagePicker { background = it }

    val components = listOf("悬浮导航栏", "卡片 / 列表", "开关", "SeekBar", "分段 SeekBar")
    val chips = components

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Keep the ball inside the stage while dragging.
        val maxX = with(density) { (maxWidth - 56.dp).toPx() }
        val maxY = with(density) { (maxHeight - 56.dp).toPx() }
        val initialBall =
            Offset(
                x = maxX - with(density) { 20.dp.toPx() },
                y = maxY * 0.55f,
            )
        if (ballOffset == Offset.Zero) ballOffset = initialBall

        // ---- stage -------------------------------------------------------------------------
        // The capture subtree must NOT contain the floating component that consumes it: the pill
        // draws `drawLayer(capture)`, and if that draw were recorded back into the same layer the
        // RenderNode tree would cycle and HWUI would stack-overflow (SIGSEGV in prepareTreeImpl).
        // So this Box captures background + scrim + in-flow content only; floating components are
        // siblings further down, drawn on top.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(themeBackground)
                    .couiBackdrop(stageBackdrop),
        ) {
            if (background != null) {
                Image(
                    bitmap = background!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Brightness scrim: 0 = show the background as-is, 1 = almost black. This is what makes
            // a light or dark material easy to judge on an arbitrary photo.
            if (dim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim * 0.85f)))
            }
            CouiTheme(colors = palette, componentColors = componentPalette) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding() +
                                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            ),
                ) {
                    if (onBack != null) CouiToolbar(title = "COUI 实验室", onBack = onBack)
                    // Chip row: pick which component sits on the stage.
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chips.forEachIndexed { index, label ->
                            val selected = index == component
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (selected) palette.primary else palette.background,
                                        )
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                        ) { component = index }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                CouiText(
                                    text = label,
                                    style = CouiTypography.body,
                                    color = if (selected) Color.White else palette.labelPrimary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    // In-flow components (everything except the floating bar) live inside the
                    // capture: they are part of the backdrop the pill samples.
                    if (component != 0) {
                        StageContent(component = component, backdrop = stageBackdrop)
                    }
                }
            }
        }

        // ---- floating component (outside the capture) ---------------------------------------
        if (component == 0) {
            CouiTheme(colors = palette, componentColors = componentPalette) {
                StageContent(component = 0, backdrop = stageBackdrop)
            }
        }

        // ---- floating ball ----------------------------------------------------------------
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(ballOffset.x.roundToInt(), ballOffset.y.roundToInt()) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CouiTheme.colors.primary)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            ballOffset =
                                Offset(
                                    x = (ballOffset.x + drag.x).coerceIn(0f, maxX),
                                    y = (ballOffset.y + drag.y).coerceIn(0f, maxY),
                                )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { panelOpen = true }
                    },
            contentAlignment = Alignment.Center,
        ) {
            CouiText("设置", style = CouiTypography.body, color = Color.White)
        }

        // ---- settings panel ----------------------------------------------------------------
        if (panelOpen) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                        ) { panelOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    CouiPreferenceItem(
                        title = "背景图片",
                        summary = if (background == null) "未设置（纯色底）" else "已选择",
                        position = CouiPreferencePosition.First,
                        onClick = { panelOpen = false; pickImage() },
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "清除背景",
                        position = CouiPreferencePosition.Middle,
                        enabled = background != null,
                        onClick = { background = null },
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "亮度",
                        summary = "${(dim * 100).roundToInt()}%",
                        position = CouiPreferencePosition.Middle,
                        trailing = {
                            LabSlider(
                                value = dim,
                                onValueChange = { dim = it },
                                modifier = Modifier.width(160.dp),
                            )
                        },
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "深色主题",
                        position = CouiPreferencePosition.Middle,
                        trailing = { CouiSwitch(checked = dark, onCheckedChange = { dark = it }) },
                        showDivider = true,
                    )
                    CouiPreferenceItem(
                        title = "复位悬浮球",
                        position = CouiPreferencePosition.Last,
                        onClick = { ballOffset = initialBall },
                    )
                }
            }
        }
    }
}

/** A dependency-free slider so the lab does not need a component that is itself under test. */
@Composable
private fun LabSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = CouiTheme.colors.background
    val active = CouiTheme.colors.primary
    BoxWithConstraints(
        modifier = modifier.height(32.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(track))
        Box(
            Modifier
                .width(maxWidth * value.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(active),
        )
        Box(
            modifier =
                Modifier
                    .offset(x = maxWidth * value.coerceIn(0f, 1f) - 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(active)
                    .pointerInput(widthPx) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val next = (value + drag.x / widthPx).coerceIn(0f, 1f)
                            onValueChange(next)
                        }
                    }
                    .pointerInput(widthPx) {
                        detectTapGestures { onValueChange((it.x / widthPx).coerceIn(0f, 1f)) }
                    },
        )
    }
}

@Composable
private fun StageContent(component: Int, backdrop: io.github.wfc35286.coui.kmp.component.CouiBackdrop?) {
    when (component) {
        0 -> {
            var selected by rememberSaveable { mutableIntStateOf(0) }
            val painters =
                listOf(
                    CouiLabIcons.Home,
                    CouiLabIcons.Game,
                    CouiLabIcons.App,
                    CouiLabIcons.Rank,
                    CouiLabIcons.Mine,
                ).map { rememberVectorPainter(it) }
            val items =
                listOf("首页", "游戏", "软件", "榜单", "我的").mapIndexed { index, title ->
                    CouiBottomFloatingNavigationBarItem(
                        id = index,
                        title = title,
                        icon = painters[index],
                        iconTint = CouiBottomFloatingNavigationBarDefaults.titleColor(index == selected),
                        redDotType = if (index == 4) CouiRedDotType.Number else CouiRedDotType.None,
                        redDotNumber = 15,
                    )
                }
            Box(Modifier.fillMaxSize()) {
                CouiBottomFloatingNavigationBar(
                    items = items,
                    selectedIndex = selected,
                    onSelected = { selected = it },
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        1 ->
            CouiCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin, vertical = 24.dp),
            ) {
                repeat(4) { row ->
                    CouiPreferenceItem(
                        title = "列表条目 ${row + 1}",
                        summary = "用来观察卡片、分隔线与背景的对比",
                        position =
                            when (row) {
                                0 -> CouiPreferencePosition.First
                                3 -> CouiPreferencePosition.Last
                                else -> CouiPreferencePosition.Middle
                            },
                        showDivider = row != 3,
                        trailing = {
                            CouiText(
                                text = "值",
                                style = CouiTypography.body,
                                emphasis = CouiTextEmphasis.Secondary,
                            )
                        },
                    )
                }
            }

        2 -> {
            var checked by rememberSaveable { mutableStateOf(true) }
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin, vertical = 24.dp)) {
                CouiPreferenceItem(
                    title = "开关",
                    position = CouiPreferencePosition.Single,
                    trailing = { CouiSwitch(checked = checked, onCheckedChange = { checked = it }) },
                )
            }
        }

        3 -> {
            var progress by rememberSaveable { mutableFloatStateOf(0.4f) }
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin, vertical = 24.dp)) {
                CouiPreferenceItem(
                    title = "SeekBar",
                    position = CouiPreferencePosition.Single,
                    trailing = {
                        CouiSeekBar(
                            value = progress,
                            onValueChange = { progress = it },
                            modifier = Modifier.width(220.dp),
                        )
                    },
                )
            }
        }

        else -> {
            var progress by rememberSaveable { mutableIntStateOf(2) }
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin, vertical = 24.dp)) {
                CouiPreferenceItem(
                    title = "分段 SeekBar",
                    position = CouiPreferencePosition.Single,
                    summary = "第 ${progress + 1} 格",
                    trailing = {
                        CouiSectionSeekBar(
                            value = progress,
                            onValueChange = { progress = it },
                            modifier = Modifier.width(220.dp),
                        )
                    },
                )
            }
        }
    }
}

/** 24dp outline glyphs for the lab's nav-bar preview. */
private object CouiLabIcons {
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
        "LabHome",
        "M3.4,10.6 L12,3.6 L20.6,10.6 L20.6,20.2 L14.6,20.2 L14.6,14.4 L9.4,14.4 L9.4,20.2 L3.4,20.2 Z",
    )
    val Game: ImageVector = icon(
        "LabGame",
        "M7.6,8.4 L16.4,8.4 C19,8.4 20.4,10 20.4,12.6 C20.4,15.2 19,16.8 16.4,16.8 " +
            "L7.6,16.8 C5,16.8 3.6,15.2 3.6,12.6 C3.6,10 5,8.4 7.6,8.4 Z " +
            "M8.4,11.2 L8.4,14.2 M6.9,12.7 L9.9,12.7 M15.4,12.1 L15.5,12.1 M17.1,13.8 L17.2,13.8",
    )
    val App: ImageVector = icon(
        "LabApp",
        "M4.2,4.2 L9.6,4.2 L9.6,9.6 L4.2,9.6 Z M14.4,4.2 L19.8,4.2 L19.8,9.6 L14.4,9.6 Z " +
            "M4.2,14.4 L9.6,14.4 L9.6,19.8 L4.2,19.8 Z M14.4,14.4 L19.8,14.4 L19.8,19.8 L14.4,19.8 Z",
    )
    val Rank: ImageVector = icon(
        "LabRank",
        "M4.4,14.2 L8.2,14.2 L8.2,20 L4.4,20 Z M10.1,4 L13.9,4 L13.9,20 L10.1,20 Z " +
            "M15.8,9.6 L19.6,9.6 L19.6,20 L15.8,20 Z",
    )
    val Mine: ImageVector = icon(
        "LabMine",
        "M12,4.6 C13.9,4.6 15.4,6.1 15.4,8 C15.4,9.9 13.9,11.4 12,11.4 C10.1,11.4 8.6,9.9 8.6,8 " +
            "C8.6,6.1 10.1,4.6 12,4.6 Z M5.2,19.6 C5.2,15.9 8.2,13.4 12,13.4 C15.8,13.4 18.8,15.9 18.8,19.6",
    )
}