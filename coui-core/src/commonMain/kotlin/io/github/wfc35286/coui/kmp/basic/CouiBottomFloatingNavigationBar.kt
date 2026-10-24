// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.graphics.COUI_PANEL_EDGE_WIDTH_DP
import io.github.wfc35286.coui.kmp.graphics.CouiBackdrop
import io.github.wfc35286.coui.kmp.graphics.CouiFloatingMaterialDark
import io.github.wfc35286.coui.kmp.graphics.CouiFloatingMaterialLight
import io.github.wfc35286.coui.kmp.graphics.CouiFloatingMaterialSurface
import io.github.wfc35286.coui.kmp.graphics.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.graphics.LocalCouiBackdrop
import io.github.wfc35286.coui.kmp.graphics.drawCouiSpotLight
import io.github.wfc35286.coui.kmp.graphics.rememberCouiSpotLightState
import io.github.wfc35286.coui.kmp.motion.CouiDeformDeadZonePx
import io.github.wfc35286.coui.kmp.motion.CouiMinVisibleChange
import io.github.wfc35286.coui.kmp.motion.CouiPressEngineDefaults
import io.github.wfc35286.coui.kmp.motion.CouiPressMaskMinVisibleChange
import io.github.wfc35286.coui.kmp.motion.CouiPressMaskSpring
import io.github.wfc35286.coui.kmp.motion.CouiPressSprings
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.CouiTouchCommon
import io.github.wfc35286.coui.kmp.motion.CouiTouchTiers
import io.github.wfc35286.coui.kmp.motion.couiCalculateScaleWithK
import io.github.wfc35286.coui.kmp.motion.couiDeformScales
import io.github.wfc35286.coui.kmp.motion.couiHandUpSpring
import io.github.wfc35286.coui.kmp.motion.couiPressScale
import io.github.wfc35286.coui.kmp.motion.couiRubberBand
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.popup.PopupSpringAxis
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import kotlin.math.abs
import kotlin.math.max

/** The red-dot badge modes of a floating-bar tab. */
enum class CouiRedDotType(val value: Int) {
    None(0),
    Point(1),
    Number(2),
}

/**
 * One tab of a [CouiBottomFloatingNavigationBar].
 *
 * @param id The item id, reported back through selection.
 * @param title The label under the icon.
 * @param icon The icon. The bar itself never tints it - selected/unselected looks are the
 *   caller's job, expressed with [iconTint] or a stateful painter.
 * @param iconTint An optional tint for [icon], resolved by the caller (e.g. from the selected
 *   state). `Color.Unspecified` leaves the painter untouched.
 * @param redDotType Which badge to draw, if any.
 * @param redDotNumber The number shown when [redDotType] is [CouiRedDotType.Number].
 */
@Immutable
data class CouiBottomFloatingNavigationBarItem(
    val id: Int,
    val title: String,
    val icon: Painter,
    val iconTint: Color = Color.Unspecified,
    val redDotType: CouiRedDotType = CouiRedDotType.None,
    val redDotNumber: Int = 0,
    /**
     * The large-icon presentation: the icon rides on top of the capsule instead of sitting above
     * the label. The 24dp icon is hidden and the title becomes a centred 10dp label; the icon
     * only appears for the **selected** item, drawn at
     * [CouiBottomFloatingNavigationBarDefaults.LargeIconSize] with its top pushed up by
     * [CouiBottomFloatingNavigationBarDefaults.LargeIconOverflow].
     */
    val largeIcon: Boolean = false,
)

/** The measured values of [CouiBottomFloatingNavigationBar]. */
object CouiBottomFloatingNavigationBarDefaults {
    /** The pill's height. */
    val Height: Dp = 56.dp

    /** The height of the row the bar reserves, with the 56dp pill centred inside it. */
    val MinHeight: Dp = 88.dp

    /** The gap to the screen edges. */
    val PaddingHorizontal: Dp = 16.dp

    /** `(MinHeight - Height) / 2`. */
    val PaddingVertical: Dp = (MinHeight - Height) / 2

    /** The height of a tab. */
    val ItemHeight: Dp = 48.dp

    /** The maximum width of a single tab. */
    val ItemMaxWidth: Dp = 70.dp

    /** The gap between two tabs. */
    val ItemSpace: Dp = 8.dp

    /** The pill's inner horizontal padding. */
    val InnerPadding: Dp = 12.dp

    /** The width and height of a tab icon. */
    val IconSize: Dp = 24.dp

    /** The size of the selected tab's icon in the large-icon style. */
    val LargeIconSize: Dp = 40.dp

    /**
     * How far the large icon rises **above the tab's box**. With a 48dp tab inside the 56dp pill
     * the icon therefore sits ~10dp above the capsule's top edge - that is what "the icon rides
     * on top of the capsule" is.
     */
    val LargeIconOverflow: Dp = 14.dp

    /** The tab's minimum width. */
    val ItemWidth: Dp = 44.dp

    /** The gap between two items. */
    val ItemViewSpace: Dp = 4.dp

    /** The horizontal padding inside a tab's background. */
    val ItemBgHorizontalPadding: Dp = 8.dp

    /** The gap between a tab's icon and title, in the horizontal styles. */
    val IconTitleGapHorizontal: Dp = 4.dp

    /** The space above a tab's icon. */
    val IconTopMargin: Dp = 4.dp

    /** The vertical gap between a tab's icon and its title. */
    val IconTitleGap: Dp = 2.dp

    /** The title size - a fixed 10dp that does not follow the user's font-size setting. */
    val TitleSize: CouiFontSize = CouiFontSize.Fixed(10f)

    /** The dot's diameter. */
    val RedDotDiameter: Dp = 6.dp

    /** How far the badge rises above the icon's top edge. */
    val RedDotTopOverflow: Dp = 4.dp

    /** The badge's right-hand overflow past the icon, for a 1-digit number. */
    val RedDotRightOverflow1Digit: Dp = 6.dp

    /** @see RedDotRightOverflow1Digit */
    val RedDotRightOverflow2Digit: Dp = 9.dp

    /** @see RedDotRightOverflow1Digit */
    val RedDotRightOverflow3Digit: Dp = 10.dp

    /** The numbered badge's minimum width. */
    val RedDotMinWidth: Dp = 12.dp

    /** The numbered badge's height. */
    val RedDotHeight: Dp = 16.dp

    /** The numbered badge's text size. */
    val RedDotTextSize: CouiFontSize = CouiFontSize.Fixed(9f)

    /** The badge's colour. */
    val RedDotColor: Color = Color(0xFFEB3B2F)

    /** The badge's text colour. */
    val RedDotTextColor: Color = Color(0xFFFFFFFF)

    /**
     * The flat panel colour used when the blur material is unavailable (the ColorOS material
     * framework is missing on non-ColorOS devices).
     */
    val FallbackPanelColorDark: Color = Color(0xFF333333)

    /** @see FallbackPanelColorDark */
    val FallbackPanelColorLight: Color = Color(0xFFF0F0F0)

    /** The fallback panel colour for the current theme. */
    @Composable
    fun fallbackPanelColor(): Color = if (CouiTheme.colors.background.luminance() < 0.5f) FallbackPanelColorDark else FallbackPanelColorLight

    /** The title's type style - 10dp fixed. */
    val TitleStyle: CouiTextStyle = CouiTextStyle(TitleSize)

    /** The numbered badge's type style. */
    val RedDotTextStyle: CouiTextStyle = CouiTextStyle(RedDotTextSize)

    /**
     * The title colour. The selected colour is the primary label colour, **not** the brand accent -
     * a ColorOS bottom bar does not highlight with the theme colour. An app that wants that passes
     * its own colour.
     */
    @Composable
    fun titleColor(selected: Boolean, enabled: Boolean = true): Color {
        val colors = CouiTheme.colors
        return when {
            !enabled -> colors.labelTertiary
            selected -> colors.labelPrimary
            else -> colors.labelPrimaryVariant
        }
    }

    /** The right-hand overflow of the badge for a [digits]-digit number. */
    fun redDotRightOverflow(digits: Int): Dp = when {
        digits <= 1 -> RedDotRightOverflow1Digit
        digits == 2 -> RedDotRightOverflow2Digit
        else -> RedDotRightOverflow3Digit
    }

    /**
     * The spotlight preset the bottom bar uses. See
     * [io.github.wfc35286.coui.kmp.graphics.CouiSpotLightDefaults.OpaqueMedium2] for the exact
     * parameters: radius 100→500 px, end intensity 1.0 (light) / 0.15 (dark), with a pure white
     * light colour.
     */
    val SpotLight: CouiSpotLightDefaults = CouiSpotLightDefaults
}

/**
 * The ColorOS floating bottom navigation bar.
 *
 * A free-standing pill that floats above the gesture bar, holding icon-over-label tabs. The
 * geometry is `radius = height / 2`, so the silhouette is a capsule - the same routine as
 * `CouiButton`.
 *
 * The surface is the **real ColorOS material**: a `RenderEffect` blur chain that draws the corner,
 * the inner edge, the inner shadow and the caustic drop shadow (see [CouiFloatingMaterialSurface]
 * for the exact parameter mapping). When the ColorOS material framework is unavailable, the flat
 * fallback panel ([CouiBottomFloatingNavigationBarDefaults.fallbackPanelColor]) is painted instead.
 *
 * Press feedback is **container-wide**: the whole pill scales down on a spring (the candy-branch
 * springs, because the pill is far wider than it is tall), it is dragged by a rubber band, and a
 * white spotlight follows the finger, clipped to the pill and drawn **under** the items. Selection
 * only changes the label colour - there is no selected highlight capsule.
 *
 * @param items The tabs, left to right.
 * @param selectedIndex The index of the selected tab.
 * @param onSelected Called with the index the user tapped.
 * @param modifier The modifier to apply.
 * @param materialEnabled Whether the framework material is requested. `false` paints the flat
 *   fallback panel, which is also what a non-ColorOS device gets.
 * @param shadowEnabled Whether the material's inner shadow and the caustic drop shadow are applied.
 * @param enabled Whether the bar can be interacted with.
 * @param dragSelectEnabled Hold the pill and slide left/right to move the selection. The visual
 *   selection follows the finger frame by frame, but [onSelected] is only called **once**, when
 *   the finger lifts - a page never switches while the finger is still down.
 * @param dispatchClickWhenAlreadySelected Whether a tap on the **already selected** tab still
 *   reports through [onSelected]. Defaults to `false` (a re-tap is swallowed); turn it on e.g. to
 *   let a re-tap scroll the current list back to the top.
 */
@Composable
fun CouiBottomFloatingNavigationBar(
    items: List<CouiBottomFloatingNavigationBarItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    materialEnabled: Boolean = true,
    shadowEnabled: Boolean = true,
    enabled: Boolean = true,
    dragSelectEnabled: Boolean = true,
    dispatchClickWhenAlreadySelected: Boolean = false,
    /**
     * The gap to the screen edges. It belongs to the **container**, not to the pill: the whole
     * row is inset once and then centred inside what is left.
     *
     * A host that already owns that padding - [CouiBottomFloatingBar] does, because its accessory
     * slots sit *outside* the pill but inside the same padded row - passes `0.dp` here so the
     * inset is not applied twice.
     */
    horizontalPadding: Dp = CouiBottomFloatingNavigationBarDefaults.PaddingHorizontal,
    /**
     * Optional Compose-side capture of the page behind the bar (see [rememberCouiBackdrop]).
     *
     * When provided, the material is rendered from that capture **inside Compose**, so the blur
     * follows the navigation scene transform and keeps working through predictive back - and it
     * also works on devices without the ColorOS material framework. Without it the bar falls back
     * to the window-level material of the platform (ColorOS only, and it has to be swapped for
     * the flat panel while a Compose scene transform runs).
     */
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
) {
    // A plain capsule (`radius = height / 2`) rather than a runtime-shader shape: a shader clip
    // sits in front of everything the bar draws and evaluates a distance field per pixel, while
    // `RoundedCornerShape(percent = 50)` produces a pill natively and is far cheaper.
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val interactionSource = remember { MutableInteractionSource() }
    // The shared spot light owns the shader, the per-axis follow-hand springs and the glow spring.
    val spotLight = rememberCouiSpotLightState()
    var barSize by remember { mutableStateOf(Size.Zero) }
    // The bar tracks its own press state instead of the items' `interactionSource`: press feedback
    // reacts to a touch anywhere on the pill, including its padding.
    var barPressed by remember { mutableStateOf(false) }
    // The pill highlights the tab under the finger while the finger is still down, and the
    // callback (i.e. the page switch) is deferred to the lift. `selectedIndex` is the caller's
    // controlled value here, so the transient drag selection needs its own slot - [dragIndex]
    // wins while a drag is in flight and is dropped as soon as the caller's value catches up.
    var dragIndex by remember { mutableIntStateOf(-1) }
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val onSelectedState = rememberUpdatedState(onSelected)
    val visualSelected = if (dragIndex in items.indices) dragIndex else selectedIndex
    // Drop the transient selection once the controlled value agrees, so a programmatic page change
    // (or a host that ignores the drag) always wins over a stale drag.
    LaunchedEffect(selectedIndex, dragIndex) {
        if (dragIndex >= 0 && dragIndex == selectedIndex) dragIndex = -1
    }
    val density = LocalDensity.current
    // The material spec: blur radius 25 raw px, plus the capsule stroke params.
    val isDark = CouiTheme.colors.background.luminance() < 0.5f
    val materialSpec =
        remember(isDark, shadowEnabled) {
            val base = if (isDark) CouiFloatingMaterialDark else CouiFloatingMaterialLight
            base.copy(
                // The stroke edge width: 2dp, which the stroke mapper divides by the preset's
                // base rim width and so lands as a ~2px hairline, not a 12px band.
                edgeWidthPx = with(density) { COUI_PANEL_EDGE_WIDTH_DP.dp.toPx() },
                applyShadow = shadowEnabled,
                applyCausticShadow = shadowEnabled,
            )
        }
    // The spotlight preset: radius 100→500 px, end intensity 1.0 (light) / 0.15 (dark).
    val spotLightSpec = remember(isDark) { CouiSpotLightDefaults.OpaqueMedium2(isDark) }
    // While the finger is down the pill follows with a loose drag spring (0.15 / 0.15) and on
    // release it springs back with the large-area hand-up spring (0.65 / 0.45) - the bar is
    // ~376dp wide, far past the 100dp large-area reference. The target is derived from the
    // pill's own size.
    val pressScale = remember { Animatable(1f) }
    val scaleTarget =
        if (enabled && barPressed && barSize != Size.Zero) {
            couiPressScale(
                width = with(density) { barSize.width.toDp().value },
                height = with(density) { barSize.height.toDp().value },
                minScale = CouiPressEngineDefaults.MinScale,
                maxScale = CouiPressEngineDefaults.MaxScale,
            )
        } else {
            1f
        }
    // The press springs are the candy-branch pair, chosen by the same rule the touch engine uses:
    //     min(w,h) > 0 && max(w,h)/min(w,h) > 2
    // The bar is 376x56 px - ratio 6.7 - so it takes the **candy** branch:
    //     candyPressSpring   = SpringData(bounce 0.0, response 0.3)
    //     candyHandUpSpring  = SpringData(bounce 0.5, response 0.5)
    // The stiffer engine defaults would make the press snap in roughly twice as fast.
    val pressSpring = CouiPressSprings.CandyPress
    // The release spring is **not** a constant: [couiHandUpSpring] interpolates it by control
    // size, and a 376dp pill sits past the large-area reference, so it gets `(0.65, 0.45)`. A
    // 56dp round button gets `(0.65, 0.42)` instead - same bounce, faster response - which is why
    // the small controls snap back slightly quicker than the bar.
    val releaseSpring = remember(barSize) {
        val density = density.density
        couiHandUpSpring(
            widthDp = barSize.width / density,
            heightDp = barSize.height / density,
        )
    }
    LaunchedEffect(barPressed, scaleTarget, enabled) {
        pressScale.animateTo(
            targetValue = scaleTarget,
            animationSpec = (if (barPressed) pressSpring else releaseSpring)
                .toSpringSpec(CouiMinVisibleChange.Scale),
        )
    }
    // While the finger is down the pill is also *stretched* along the direction of travel and
    // squeezed on the perpendicular axis, conserving area, on top of the press scale and the
    // rubber band.
    //
    // The deform and rubber axes run a loose drag spring (response 0.15, bounce 0.15) while the
    // finger is down, and hand over to the large-area hand-up spring (0.65, 0.45) on release. The
    // axes below advance **once per frame** so the spring integrates normally instead of being
    // cancelled and restarted on every move event (which made the pill stutter).
    val deformXAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val deformYAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val rubberXAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    val rubberYAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    // One state write per frame drives every layer; the axis values themselves are plain fields.
    var motionTick by remember { mutableIntStateOf(0) }
    // The deform pivot stays at the centre - no edge is pinned. Pinning an edge instead makes one
    // side shrink while the other stays put, and because the pinned anchor flips between the left
    // and right edge while dragging sideways, the pill visibly jumps from "left side shrinks" to
    // "right side shrinks".
    val maxStretchPx = with(density) { CouiTouchTiers.MaxStretchDistance.dp.toPx() }
    val maxDeformPx = with(density) { CouiTouchTiers.MaxDeformRubberDistance.dp.toPx() }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt =
                    if (last == 0L) {
                        0.016f
                    } else {
                        ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
                    }
                last = now
                var moving = deformXAxis.advance(dt)
                moving = deformYAxis.advance(dt) || moving
                moving = rubberXAxis.advance(dt) || moving
                moving = rubberYAxis.advance(dt) || moving
                if (moving) motionTick++
            }
        }
    }

    BoxWithConstraints(
        modifier =
        modifier
            .padding(horizontal = horizontalPadding)
            // The space the bar reserves, with the 56dp pill centred inside it.
            .height(CouiBottomFloatingNavigationBarDefaults.MinHeight),
        contentAlignment = Alignment.Center,
    ) {
        // Every item is capped at `ItemMaxWidth` and the pill is exactly as wide as its items
        // until they stop fitting, at which point the pill takes the whole available width and
        // the items shrink evenly. With five items on a 360dp screen that is
        // `(327 - 24 - 32) / 5 = 54.3dp` per item.
        val itemCount = items.size
        val itemSpace = CouiBottomFloatingNavigationBarDefaults.ItemSpace
        val innerPadding = CouiBottomFloatingNavigationBarDefaults.InnerPadding
        val totalSpace = itemSpace * (itemCount - 1).coerceAtLeast(0)
        val idealWidth =
            CouiBottomFloatingNavigationBarDefaults.ItemMaxWidth * itemCount + innerPadding * 2 + totalSpace
        val pillWidth = minOf(maxWidth, idealWidth)
        val itemWidth =
            if (itemCount > 0) {
                (pillWidth - innerPadding * 2 - totalSpace) / itemCount
            } else {
                CouiBottomFloatingNavigationBarDefaults.ItemMaxWidth
            }
        // The material travels with the pill: the whole surface moves, backdrop included, so the
        // host layer has to be offset by exactly the same amount as the content - otherwise the
        // capsule slides while the blurred page stays put and the bar looks like it is tearing
        // apart.
        val barTranslation = {
            motionTick
            Offset(rubberXAxis.value, rubberYAxis.value)
        }
        // The gesture layer deliberately sits **outside** the material's transform: inside it,
        // `change.position` is mapped through the press scale, the deformation and the layout
        // offset - so a slow drag feeds the deformation back into itself, and the tracked travel
        // overshoots the finger with a phantom component. Here the coordinates are the raw,
        // untransformed finger positions.
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(enabled, dragSelectEnabled, pillWidth, itemWidth, items.size) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pillWidthPx = pillWidth.toPx()
                        val pillHeightPx = CouiBottomFloatingNavigationBarDefaults.Height.toPx()
                        val left = (size.width - pillWidthPx) / 2f
                        val top = (size.height - pillHeightPx) / 2f
                        val downPos = down.position
                        val inside =
                            downPos.x >= left && downPos.x <= left + pillWidthPx &&
                                downPos.y >= top && downPos.y <= top + pillHeightPx
                        if (!inside) return@awaitEachGesture
                        // A raw geometric hit test against the item rects - closed intervals,
                        // **no** touchSlop, no velocity, no snapping. The same routine decides
                        // "which item was pressed" and "which item is under the finger now".
                        val inner = CouiBottomFloatingNavigationBarDefaults.InnerPadding.toPx()
                        val space = CouiBottomFloatingNavigationBarDefaults.ItemSpace.toPx()
                        val itemW = itemWidth.toPx()
                        fun itemIndexAt(pos: Offset): Int {
                            if (pos.y < top || pos.y > top + pillHeightPx) return -1
                            for (i in items.indices) {
                                val x0 = left + inner + i * (itemW + space)
                                if (pos.x >= x0 && pos.x <= x0 + itemW) return i
                            }
                            return -1
                        }
                        // Remember the pressed item and clear the pending slide target. A press
                        // that lands on the capsule but not on an item belongs to the container,
                        // so it is consumed; a press on an item stays with that item's own
                        // `clickable` (taps).
                        val downIndex = itemIndexAt(downPos)
                        if (downIndex < 0 && !down.isConsumed) down.consume()
                        // The selection the gesture starts from, before any move.
                        val downSelected =
                            if (dragIndex in items.indices) dragIndex else selectedIndexState.value
                        var pendingIndex = -1
                        var lastPos = downPos
                        var upChange: PointerInputChange? = null
                        // The pill-local coordinates are **divided by the bounds** before they
                        // reach the effect, so the light position works in normalized space.
                        // Feeding raw pixels puts the light hundreds of radii away from the
                        // finger.
                        val origin = Offset(left, top)
                        val pillW = pillWidthPx.coerceAtLeast(1f)
                        val pillH = pillHeightPx.coerceAtLeast(1f)
                        fun Offset.normalizeToPill(): Offset = Offset(((this.x - origin.x) / pillW), ((this.y - origin.y) / pillH))
                        spotLight.press(downPos.normalizeToPill())
                        barPressed = true
                        // On touch-down the drag spring takes over from wherever the axes
                        // currently are, so a still-running release keeps its velocity.
                        for (axis in listOf(deformXAxis, deformYAxis, rubberXAxis, rubberYAxis)) {
                            axis.spring = CouiTouchTiers.DragSpring
                            axis.target = axis.value
                        }
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, downPos)
                        var lastVx = 0f
                        var lastVy = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change?.pressed == true) {
                                val pos = change.position
                                lastPos = pos
                                spotLight.moveTo(pos.normalizeToPill())
                                velocityTracker.addPosition(change.uptimeMillis, pos)
                                // Hit test on every frame and move the highlight **immediately** -
                                // the tab under the finger turns selected while the finger is
                                // still down. A move never reports to the host; it only records
                                // the pending target for the lift.
                                if (dragSelectEnabled) {
                                    val hit = itemIndexAt(pos)
                                    val current =
                                        if (dragIndex in items.indices) dragIndex else downSelected
                                    if (hit >= 0 && hit != current) {
                                        pendingIndex = hit
                                        dragIndex = hit
                                    }
                                }
                                // Travel from the touch-down point.
                                val dx = pos.x - downPos.x
                                val dy = pos.y - downPos.y
                                // The dead zone is raw pixels (5.0f, never dp-scaled).
                                if (
                                    abs(dx) >= CouiDeformDeadZonePx ||
                                    abs(dy) >= CouiDeformDeadZonePx
                                ) {
                                    val scales =
                                        couiDeformScales(
                                            dx = dx,
                                            dy = dy,
                                            maxScale =
                                            couiCalculateScaleWithK(
                                                width = barSize.width,
                                                height = barSize.height,
                                                maxScale = CouiTouchCommon.MaxScale,
                                            ),
                                            maxRubberPx = maxDeformPx,
                                        )
                                    deformXAxis.target = scales.x
                                    deformYAxis.target = scales.y
                                    // The *target* is the rubber-banded travel; the drag spring
                                    // follows it.
                                    rubberXAxis.target =
                                        couiRubberBand(
                                            dx,
                                            CouiTouchTiers.RubberCurveRatio,
                                            maxStretchPx,
                                        )
                                    rubberYAxis.target =
                                        couiRubberBand(
                                            dy,
                                            CouiTouchTiers.RubberCurveRatio,
                                            maxStretchPx,
                                        )
                                }
                            } else {
                                // ACTION_UP / ACTION_CANCEL: remember where the finger lifted -
                                // the dispatch decision is made right below.
                                lastPos = change?.position ?: lastPos
                                upChange = change
                                break
                            }
                        }
                        // On the lift:
                        //   1. the item under the lifted finger is clicked **only** when it is
                        //      not the pressed one (a plain tap is still the item's own click);
                        //   2. then the pending slide target, if any.
                        // Exactly one selection leaves the library per gesture, and only here -
                        // that is why a drag switches the page on release, never while the finger
                        // is moving.
                        if (dragSelectEnabled) {
                            val upIndex = itemIndexAt(lastPos)
                            val target =
                                when {
                                    upIndex >= 0 && upIndex != downIndex -> upIndex
                                    upIndex < 0 && pendingIndex >= 0 -> pendingIndex
                                    else -> -1
                                }
                            if (target >= 0) {
                                // Consume the lift so the pressed item's own `clickable` cannot
                                // report a second time for the same gesture (`clickable` treats
                                // a consumed up as a cancelled tap).
                                upChange?.consume()
                                dragIndex = target
                                onSelectedState.value(target)
                            } else {
                                // Back on the pressed tab (or no slide at all): hand the gesture
                                // back to that item's `clickable`, so a tap still reports and a
                                // single gesture can never report twice.
                                dragIndex = -1
                            }
                        }
                        // The axes already hold the released position, and this spring animates
                        // those very values home - there is no second source to hand over to.
                        val releaseVelocity = velocityTracker.calculateVelocity()
                        lastVx = releaseVelocity.x
                        lastVy = releaseVelocity.y
                        spotLight.release()
                        barPressed = false
                        // Release velocity:
                        //     initial = clamp(pan / totalDist * multiplier, +-clamp)
                        // where `totalDist` is **per unit**: 150dp for the deform axis and 28dp
                        // for the rubber axis. The multiplier is how far from rest the axis
                        // currently sits - for deform that is `|scale - 1|`.
                        val panVx = lastVx
                        val panVy = lastVy
                        val deformRangePx = maxDeformPx
                        fun releaseAxis(
                            axis: PopupSpringAxis,
                            rest: Float,
                            pan: Float,
                            rangePx: Float,
                        ) {
                            val denominator = rangePx.coerceAtLeast(1f)
                            axis.velocity =
                                (pan / denominator * abs(axis.value - rest))
                                    .coerceIn(-1000f, 1000f)
                            axis.spring = CouiTouchTiers.HandUpSpringLargeArea
                            axis.target = rest
                        }
                        releaseAxis(deformXAxis, 1f, panVx, deformRangePx)
                        releaseAxis(deformYAxis, 1f, panVy, deformRangePx)
                        releaseAxis(rubberXAxis, 0f, panVx, maxStretchPx)
                        releaseAxis(rubberYAxis, 0f, panVy, maxStretchPx)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            CouiFloatingMaterialSurface(
                spec = materialSpec,
                enabled = materialEnabled,
                backdrop = backdrop,
                translation = barTranslation,
                // The material has to travel and deform with the content, otherwise the blurred
                // surface tears away from the pill while the finger drags. Same two layers as the
                // content: press centred, deformation anchored.
                pressScale = { pressScale.value },
                scaleX = {
                    motionTick
                    deformXAxis.value
                },
                scaleY = {
                    motionTick
                    deformYAxis.value
                },
                // Centre pivot: the pivot stays at the middle of the pill.
                transformOrigin = { Offset(0.5f, 0.5f) },
                shadowEnabled = shadowEnabled,
                dark = isDark,
                modifier =
                Modifier
                    .width(pillWidth)
                    .height(CouiBottomFloatingNavigationBarDefaults.Height),
                fallback = {
                    // A flat panel - the fallback surface.
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            // No `shadowElevation`: the fallback draws no shadow of its own,
                            // and a Compose elevation here paints a grey halo *outside* the
                            // capsule.
                            .graphicsLayer {
                                shape = pillShape
                                shadowElevation = 0f
                                clip = false
                            }
                            .background(
                                color = CouiBottomFloatingNavigationBarDefaults.fallbackPanelColor(),
                                shape = pillShape,
                            ),
                    )
                },
            ) {
                Row(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        // **All** transform (translation / press scale / deformation) is applied by
                        // `CouiFloatingMaterialSurface`, once, so the material and the content can
                        // never drift apart. Applying it here as well would double the rubber-band
                        // travel and square the deformation scale.
                        .onSizeChanged { barSize = Size(it.width.toFloat(), it.height.toFloat()) }
                        .drawWithContent {
                            // The spotlight is painted **under** the items, clipped to the pill.
                            drawCouiSpotLight(spotLight, spotLightSpec, pillShape)
                            this@drawWithContent.drawContent()
                        }
                        .padding(horizontal = CouiBottomFloatingNavigationBarDefaults.InnerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            Spacer(Modifier.width(CouiBottomFloatingNavigationBarDefaults.ItemSpace))
                        }
                        CouiBottomFloatingNavigationBarItemView(
                            item = item,
                            // While a drag is in flight the tab under the finger is the selected
                            // one.
                            selected = index == visualSelected,
                            enabled = enabled,
                            width = itemWidth,
                            // A re-tap of the selected tab is swallowed by default;
                            // [dispatchClickWhenAlreadySelected] opts back in. The drag path
                            // reports its own target on release.
                            onClick = {
                                if (dispatchClickWhenAlreadySelected || index != visualSelected) {
                                    onSelectedState.value(index)
                                }
                            },
                            interactionSource = interactionSource,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One tab.
 *
 * The layout: the icon is centred horizontally and pinned to the top by
 * [CouiBottomFloatingNavigationBarDefaults.IconTopMargin], then the label sits
 * [CouiBottomFloatingNavigationBarDefaults.IconTitleGap] below it, also centred.
 *
 * The item contributes **no** visual press state of its own - the container's scale and spotlight
 * do the work.
 */
@Composable
private fun CouiBottomFloatingNavigationBarItemView(
    item: CouiBottomFloatingNavigationBarItem,
    selected: Boolean,
    enabled: Boolean,
    width: Dp,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
) {
    val titleColor = CouiBottomFloatingNavigationBarDefaults.titleColor(selected, enabled)
    if (item.largeIcon) {
        // Large-icon style: the item box only carries the 10dp, centre-aligned title; the 40dp
        // icon is centred horizontally and placed above the item box, i.e. it rides above the
        // item's bounds - which is why the item must not clip its children, and neither does the
        // bar. Only the selected tab shows it.
        Box(
            modifier =
            Modifier
                .width(width)
                .height(CouiBottomFloatingNavigationBarDefaults.ItemHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        ) {
            if (selected) {
                Box(
                    modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -CouiBottomFloatingNavigationBarDefaults.LargeIconOverflow)
                        .size(CouiBottomFloatingNavigationBarDefaults.LargeIconSize),
                ) {
                    Image(
                        painter = item.icon,
                        contentDescription = null,
                        colorFilter =
                        item.iconTint.takeIf { it != Color.Unspecified }?.let { ColorFilter.tint(it) },
                    )
                    CouiBottomFloatingNavigationBarRedDot(
                        item = item,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
            CouiText(
                text = item.title,
                style = CouiBottomFloatingNavigationBarDefaults.TitleStyle,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    Column(
        modifier =
        Modifier
            .width(width)
            .height(CouiBottomFloatingNavigationBarDefaults.ItemHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CouiBottomFloatingNavigationBarDefaults.IconTopMargin))
        Box(
            modifier =
            Modifier
                .size(CouiBottomFloatingNavigationBarDefaults.IconSize),
            // No fade overlay is applied on selection: compositing a fade mask against an empty
            // destination layer leaves the icon untouched, so such an overlay draws nothing.
            //
            // The selected tab changes **only** through the tint: the caller's
            // [CouiBottomFloatingNavigationBarItem.iconTint] (or a stateful icon painter) is the
            // whole "selected icon" treatment.
        ) {
            Image(
                painter = item.icon,
                contentDescription = null,
                colorFilter = item.iconTint.takeIf { it != Color.Unspecified }?.let { ColorFilter.tint(it) },
            )
            CouiBottomFloatingNavigationBarRedDot(
                item = item,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        Spacer(Modifier.height(CouiBottomFloatingNavigationBarDefaults.IconTitleGap))
        CouiText(
            text = item.title,
            style = CouiBottomFloatingNavigationBarDefaults.TitleStyle,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The badge, anchored to the icon's top-end.
 *
 * It overflows the icon: the right-hand overflow pushes it past the right edge (the amount grows
 * with the number of digits, so a wider capsule stays optically centred) and the top overflow past
 * the top edge. The item does not clip its children, which is why it can.
 */
@Composable
private fun CouiBottomFloatingNavigationBarRedDot(
    item: CouiBottomFloatingNavigationBarItem,
    modifier: Modifier = Modifier,
) {
    if (item.redDotType == CouiRedDotType.None) return
    val digits =
        when (item.redDotType) {
            CouiRedDotType.Number -> item.redDotNumber.coerceAtLeast(0).toString().length
            else -> 1
        }
    val overflow = CouiBottomFloatingNavigationBarDefaults.redDotRightOverflow(digits)
    // `offset`, not `padding`: the badge is deliberately pushed *outside* the icon, and Compose
    // rejects negative padding outright ("Padding must be non-negative").
    val offsetX = overflow / 2
    val offsetY = -CouiBottomFloatingNavigationBarDefaults.RedDotTopOverflow / 2
    when (item.redDotType) {
        CouiRedDotType.Point ->
            Box(
                modifier =
                modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(CouiBottomFloatingNavigationBarDefaults.RedDotDiameter)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(CouiBottomFloatingNavigationBarDefaults.RedDotColor),
            )

        else ->
            Box(
                modifier =
                modifier
                    .offset(x = offsetX, y = offsetY)
                    .widthIn(min = CouiBottomFloatingNavigationBarDefaults.RedDotMinWidth)
                    .height(CouiBottomFloatingNavigationBarDefaults.RedDotHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(CouiBottomFloatingNavigationBarDefaults.RedDotColor)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                CouiText(
                    text = item.redDotNumber.toString(),
                    style = CouiBottomFloatingNavigationBarDefaults.RedDotTextStyle,
                    color = CouiBottomFloatingNavigationBarDefaults.RedDotTextColor,
                    maxLines = 1,
                )
            }
    }
}
