// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

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
import io.github.wfc35286.coui.kmp.effect.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.effect.drawCouiSpotLight
import io.github.wfc35286.coui.kmp.effect.rememberCouiSpotLightState
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
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import kotlin.math.abs
import kotlin.math.max

/** `BaseBottomFloatingItem`'s red-dot modes. */
enum class CouiRedDotType(val value: Int) {
    None(0),
    Point(1),
    Number(2),
}

/**
 * One tab of a [CouiBottomFloatingNavigationBar], mirroring
 * `com.coui.appcompat.bottomfloatingnavigationbar.BaseBottomFloatingItem`.
 *
 * @param id The item id, reported back through selection.
 * @param title The label under the icon.
 * @param icon The icon. The official view never tints it - it relies on the caller handing it a
 *   stateful `Drawable`, and only forwards `setSelected` to the `ImageView`. Compose callers
 *   express the same thing with [iconTint].
 * @param iconTint An optional tint for [icon], resolved by the caller (e.g. from the selected
 *   state). `Color.Unspecified` leaves the painter untouched, which is what the official does.
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
     * `COUIBottomFloatingNavigationBarItem.isLargeIcon()` - `COUIBottomFloatingNavigationBarMenuView
     * .addItem` maps it straight onto `setItemStyle(2)` (`ITEM_STYLE_NAVI_LARGE_ICON`) instead of
     * `setItemStyle(1)` (`ITEM_STYLE_NAVI_VERTICAL`).
     *
     * The large-icon style is a different presentation, not a bigger 24dp icon:
     * `applyNaviLargeIconStyle` hides the icon view and centres a 10dp title, and the icon only
     * appears for the **selected** item, laid out by `layoutNaviLargeIcon` at
     * `coui_bottom_floating_navigation_bar_large_icon_size` (40dp) with its top pushed up by
     * `coui_bottom_floating_navigation_bar_large_icon_overflow` (14dp). That is the "icon riding on
     * top of the capsule" look.
     */
    val largeIcon: Boolean = false,
)

/** The measured values, all from `coui_bottom_floating_navigation_bar_*` resources. */
object CouiBottomFloatingNavigationBarDefaults {
    /** `coui_bottom_floating_navigation_bar_height` - the pill itself. */
    val Height: Dp = 56.dp

    /**
     * `coui_bottom_floating_navigation_bar_min_height_without_padding_vertical` =
     * `0x7f0707d2` = **88dp** in the gallery APK (`OppoGallery2` 17.10.4, MT workspace `6s0z2165`,
     * `COUIBottomFloatingAccessoryNavigationBar.smali:151-157` + `onMeasure:2115-2131`, single
     * `default` variant, no `-night` override).
     *
     * This used to be 72dp, which came from the software-store APK and made the whole row 16dp
     * short: the 56dp controls then sat in a 72dp box instead of the official 88dp one, so the
     * accessories looked oversized against the reserved row even though both are 56dp.
     * `48-GALLERY-ACCESSORY-GEOMETRY.md` §5.
     */
    val MinHeight: Dp = 88.dp

    /** `coui_bottom_floating_navigation_bar_padding_horizontal` - the gap to the screen edges. */
    val PaddingHorizontal: Dp = 16.dp

    /** `(MinHeight - Height) / 2`. */
    val PaddingVertical: Dp = (MinHeight - Height) / 2

    /** `coui_bottom_floating_navigation_bar_item_height`. */
    val ItemHeight: Dp = 48.dp

    /** `coui_bottom_floating_navigation_bar_item_max_width`. */
    val ItemMaxWidth: Dp = 70.dp

    /** `coui_bottom_floating_navigation_bar_item_space`. */
    val ItemSpace: Dp = 8.dp

    /** `coui_bottom_floating_navigation_bar_item_padding` - the pill's inner horizontal padding. */
    val InnerPadding: Dp = 12.dp

    /** `coui_floating_toolbar_item_icon_width` / `_height`. */
    val IconSize: Dp = 24.dp

    /**
     * `coui_bottom_floating_navigation_bar_large_icon_size`.
     *
     * `COUIBottomFloatingToolbarItemView.onMeasure` measures the icon with
     * `makeMeasureSpec(mLargeIconSize, EXACTLY)` in style `ITEM_STYLE_NAVI_LARGE_ICON` (2), and
     * `layoutNaviLargeIcon` lays it out at `(itemW - iconW) / 2`, `top = -mLargeIconOverflow`.
     */
    val LargeIconSize: Dp = 40.dp

    /**
     * `coui_bottom_floating_navigation_bar_large_icon_overflow` - how far the large icon rises
     * **above the item's box** (`layoutNaviLargeIcon`: `top = -mLargeIconOverflow`). With a 48dp
     * item inside the 56dp pill the icon therefore sits ~10dp above the capsule's top edge; that is
     * literally what "the icon rides on top of the capsule" is.
     */
    val LargeIconOverflow: Dp = 14.dp

    /** `coui_floating_toolbar_item_width` - the item's `setMinimumWidth`. */
    val ItemWidth: Dp = 44.dp

    /** `coui_floating_toolbar_item_view_space` - the gap between two items. */
    val ItemViewSpace: Dp = 4.dp

    /** `coui_bottom_floating_navigation_bar_item_bg_horizontal_padding`. */
    val ItemBgHorizontalPadding: Dp = 8.dp

    /** `coui_bottom_floating_navigation_bar_item_icon_title_gap_horizontal`. */
    val IconTitleGapHorizontal: Dp = 4.dp

    /** `coui_bottom_floating_navigation_bar_item_icon_top_margin`. */
    val IconTopMargin: Dp = 4.dp

    /** `coui_bottom_floating_navigation_bar_item_icon_title_gap_vertical`. */
    val IconTitleGap: Dp = 2.dp

    /**
     * The title size: `applyNaviVerticalStyle()` calls `dp2px(10.0f)` and then
     * `setTextSize(0, px)`, so it is **10dp** - a fixed size that does not follow the user's
     * font-size setting.
     */
    val TitleSize: CouiFontSize = CouiFontSize.Fixed(10f)

    /** `coui_floating_toolbar_red_dot_radius` - the dot's diameter. */
    val RedDotDiameter: Dp = 6.dp

    /** `coui_bottom_floating_navigation_bar_red_dot_top_overflow`. */
    val RedDotTopOverflow: Dp = 4.dp

    /** `coui_bottom_floating_navigation_bar_red_dot_right_overflow_{1,2,3}_digit`. */
    val RedDotRightOverflow1Digit: Dp = 6.dp

    /** @see RedDotRightOverflow1Digit */
    val RedDotRightOverflow2Digit: Dp = 9.dp

    /** @see RedDotRightOverflow1Digit */
    val RedDotRightOverflow3Digit: Dp = 10.dp

    /** `coui_hint_red_dot_navi_small_width`. */
    val RedDotMinWidth: Dp = 12.dp

    /** `coui_height` from `Widget.COUI.COUIHintRedDot`. */
    val RedDotHeight: Dp = 16.dp

    /** `coui_hint_red_dot_small_text_size`. */
    val RedDotTextSize: CouiFontSize = CouiFontSize.Fixed(9f)

    /** `coui_color_additional_red`. */
    val RedDotColor: Color = Color(0xFFEB3B2F)

    /** `coui_hint_red_dot_text_color`. */
    val RedDotTextColor: Color = Color(0xFFFFFFFF)

    /**
     * `BaseBottomFloatingMenuView.PANEL_FALLBACK_OVERLAY_COLOR_DARK` / `_LIGHT` - the flat panel the
     * official paints when the material is unavailable (`applyPanelBackground()`), and the colour we
     * fall back to when the Oplus framework is missing.
     */
    val FallbackPanelColorDark: Color = Color(0xFF333333)

    /** @see FallbackPanelColorDark */
    val FallbackPanelColorLight: Color = Color(0xFFF0F0F0)

    /** The fallback panel colour for the current theme (`isCOUIDarkThemeOrNightMode`). */
    @Composable
    fun fallbackPanelColor(): Color = if (CouiTheme.colors.background.luminance() < 0.5f) FallbackPanelColorDark else FallbackPanelColorLight

    /** The title's type style - 10dp fixed. */
    val TitleStyle: CouiTextStyle = CouiTextStyle(TitleSize)

    /** The numbered badge's type style. */
    val RedDotTextStyle: CouiTextStyle = CouiTextStyle(RedDotTextSize)

    /**
     * The title colour, i.e. `resolveBottomNavDefaultTitleColorStateList()`.
     *
     * The selected colour is `couiColorLabelPrimary`, **not** the brand accent - a COUI bottom
     * bar does not highlight with the theme colour. An app that wants that (the market does)
     * passes its own colour.
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
     * `COUISpotLightEffect.TYPE_OPAQUE_MEDIUM_2` - the spotlight preset the bottom bar uses
     * (`initTouchEffectDrawable`), decoded from `COUISpotLightEffect.<clinit>`.
     *
     * `SpotLightParams(blendMode, startIntensity, endIntensity, startRadius, endRadius,
     *                 lightHeight, ambientStrength, diffuseStrength, ditherN, noise)`:
     *
     * | theme | blend     | startInt | endInt | startR | endR | height | ambient | diffuse | dither | noise |
     * |-------|-----------|----------|--------|--------|------|--------|---------|---------|--------|-------|
     * | light | OVERLAY   | 0        | 1.0    | 100    | 500  | 200    | 0.4     | 0.4     | 0      | 0.08  |
     * | dark  | PLUS      | 0        | 0.15   | 100    | 500  | 200    | 0.4     | 0.4     | 1.0    | 0.08  |
     *
     * The bar feeds the light colour as `resolveTouchEffectLightColor()` = `0xFFFFFFFF`, i.e. pure
     * white. See [io.github.wfc35286.coui.kmp.effect.CouiSpotLightDefaults.OpaqueMedium2].
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
 * The surface is the **real ColorOS material**, not an approximation: the official
 * `BaseBottomFloatingMenuView` hands a `RenderEffect` blur chain to
 * `OplusViewBackgroundRenderEffect` and lets `OplusMaterialUtil` draw the corner, the inner edge,
 * the inner shadow and the caustic drop shadow (see [CouiFloatingMaterialSurface] for the exact
 * call-for-call mapping). When the framework is missing, the official's own flat fallback panel
 * (`PANEL_FALLBACK_OVERLAY_COLOR_*`) is painted instead.
 *
 * Press feedback is **container-wide**, exactly as in the original: the whole pill scales down on a
 * spring (`COUITouchMotionAnimator` + `COUIPressAnimatorUnit`, candy branch because every item is at
 * least twice as wide as it is tall), it is dragged by `TouchMotionRubberBand`, and a white
 * spotlight follows the finger (`COUISpotLightEffectDrawable` with `TYPE_OPAQUE_MEDIUM_2`), clipped
 * to the pill and drawn **under** the items, as `onDraw` does. Selection only changes the label
 * colour - there is no selected highlight capsule in the official.
 *
 * @param items The tabs, left to right.
 * @param selectedIndex The index of the selected tab.
 * @param onSelected Called with the index the user tapped.
 * @param modifier The modifier to apply.
 * @param materialEnabled Whether the framework material is requested. `false` paints the official
 *   flat fallback panel, which is also what a non-ColorOS device gets.
 * @param shadowEnabled Whether the material's inner shadow (`setShadowParams`) and the caustic drop
 *   shadow (`setCausticShadowEffect`) are applied.
 * @param enabled Whether the bar can be interacted with.
 * @param dragSelectEnabled Hold the pill and slide left/right to move the selection
 *   (`COUIBottomFloatingNavigationBarMenuView#onMenuTouchMove`). The visual selection follows the
 *   finger frame by frame, but [onSelected] is only called **once**, when the finger lifts - the
 *   official never switches a page while the finger is still down.
 * @param dispatchClickWhenAlreadySelected `MenuView#setDispatchItemClickWhenAlreadySelected`:
 *   whether a tap on the **already selected** tab still reports through [onSelected]. The official
 *   defaults to `false` (a re-tap is swallowed); the market turns it on so a re-tap can scroll the
 *   current list back to the top.
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
     * The gap to the screen edges, i.e. `coui_bottom_floating_navigation_bar_padding_horizontal`
     * (`changePadding`). It belongs to the **container**, not to the pill: the official
     * `COUIBottomFloatingAccessoryNavigationBar` subtracts it once and then centres the whole
     * three-slot row inside what is left (`layoutPositionsMediumScreen`, `:826-828`).
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
     * to the Oplus window material (ColorOS only, and it has to be swapped for the flat panel while
     * a Compose scene transform runs).
     */
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
) {
    // A plain capsule instead of the runtime-shader `CouiPillShape`: that shape evaluates an AGSL
    // distance field per pixel, and as a `clip` it sits in front of everything the bar draws - it
    // was the single most expensive thing in the component. A pill is just `radius = height / 2`,
    // which `RoundedCornerShape(percent = 50)` produces natively.
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val interactionSource = remember { MutableInteractionSource() }
    // `COUISpotLightEffect` with `TYPE_OPAQUE_MEDIUM_2`: the shared spot light owns the shader,
    // the per-axis follow-hand springs and the glow spring.
    val spotLight = rememberCouiSpotLightState()
    var barSize by remember { mutableStateOf(Size.Zero) }
    // The bar tracks its own press state instead of the items' `interactionSource`: the official
    // `COUITouchMotionAnimator` reacts to a touch anywhere on the pill, including its padding.
    var barPressed by remember { mutableStateOf(false) }
    // `MenuView#mSelectedItemId` is the only selection the official view keeps, and
    // `onMenuTouchMove` writes it **immediately**: the pill highlights the tab under the finger
    // while the finger is still down, and the callback (i.e. the page switch) is deferred to
    // ACTION_UP. `selectedIndex` is the caller's controlled value here, so the transient drag
    // selection needs its own slot - [dragIndex] wins while a drag is in flight and is dropped as
    // soon as the caller's value catches up.
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
    // `COUIMaterialBlurEffect.FRAMEWORK_BOTTOM_BAR_PARAMS_*` (blur 25 raw px) +
    // `COUIMaterialStrokeEffect.FRAMEWORK_CAPSULE_PARAMS_12_*` (the stroke params).
    val isDark = CouiTheme.colors.background.luminance() < 0.5f
    val materialSpec =
        remember(isDark, shadowEnabled) {
            val base = if (isDark) CouiFloatingMaterialDark else CouiFloatingMaterialLight
            base.copy(
                // `StrokeEffectParams.getEdgeWidth(context)` is a plain `dp2px(edgeWidthDp)`, and
                // `edgeWidthDp` is **2.0 in both themes** for `FRAMEWORK_CAPSULE_PARAMS_12` - the
                // 0.8 / 0.15 pair that used to live here was the *edge alpha*, read out of the
                // wrong field. `GradientStrokeStyleMapper.toEdgeArray16` then divides this by the
                // preset's base rim width, so 2dp lands as a ~2px hairline, not a 12px band.
                edgeWidthPx = with(density) { COUI_PANEL_EDGE_WIDTH_DP.dp.toPx() },
                applyShadow = shadowEnabled,
                applyCausticShadow = shadowEnabled,
            )
        }
    // `TYPE_OPAQUE_MEDIUM_2`: radius 100→500 px, end intensity 1.0 (light) / 0.15 (dark).
    val spotLightSpec = remember(isDark) { CouiSpotLightDefaults.OpaqueMedium2(isDark) }
    // The official bar is driven by `COUITouchMotionAnimator`, not by `PressParams`: while the
    // finger is down it follows with `iolDragSpring` (0.15 / 0.15) and on release it springs back
    // with `iolHandUpSpringLargeArea` (0.65 / 0.45) - the bar is 376dp wide, far past
    // `iolHandUpLargeAreaRefWidthDp = 100dp`, so it takes the large-area tier. The target is
    // `TouchAnimatorParamProvider.calculateScaleWithK` evaluated for the pill itself.
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
    // `COUIPressAnimatorUnit.initPressEngineConfig()` overrides the `PressEngineConfig` class
    // defaults (0.13/0.18/0.35/0.62) whenever a `PressParams` is bound, and the navigation bar
    // binds one. Which pair applies is decided by `useCandyMaterialRealtime()`:
    //     min(w,h) > 0 && max(w,h)/min(w,h) > 2
    // The bar is 376x56 px - ratio 6.7 - so it takes the **candy** branch:
    //     candyPressSpring   = SpringData(bounce 0.0, response 0.3)
    //     candyHandUpSpring  = SpringData(bounce 0.5, response 0.5)
    // (`PressParamsExtKt.<clinit>`; `SpringData(bounce, response, velocity)`).
    // Using the engine defaults here made the press snap in roughly twice as fast as the official
    // - the "缩放过快" report - because response 0.13 is a much stiffer spring than 0.3.
    val pressSpring = CouiPressSprings.CandyPress
    // The release spring is **not** the jelly/candy constant: `TouchMotionHandUpSpringResolver`
    // interpolates it by control size, and a 376dp pill sits past the large-area reference, so it
    // gets `(0.65, 0.45)`. A 56dp round button gets `(0.65, 0.42)` instead - same bounce, faster
    // response - which is why the small controls snap back slightly quicker than the bar.
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
    // `COUIDeformAnimatorUnit`: while the finger is down the pill is also *stretched* along the
    // direction of travel and squeezed on the perpendicular axis, conserving area. The official
    // enables this next to the press scale and the rubber band
    // (`setDeformationEnable(true)` in `BaseBottomFloatingMenuView.initTouchMotionAnim`).
    //
    // `COUIDeformAnimatorUnit` / `COUIRubberAnimatorUnit` both run their own spring while the finger
    // is down: `iolDragSpring` = (response 0.15, bounce 0.15), and on release they hand over to
    // `iolHandUpSpringLargeArea` = (0.65, 0.45). The axes below advance **once per frame** - the
    // same way `COUISpringAnimation` ticks - so the spring integrates normally instead of being
    // cancelled and restarted on every move event (which is what made the pill stutter).
    val deformXAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val deformYAxis = remember { PopupSpringAxis(1f, CouiTouchCommon.DeformMinVisibleChange) }
    val rubberXAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    val rubberYAxis = remember { PopupSpringAxis(0f, CouiTouchCommon.RubberMinVisibleChange) }
    // One state write per frame drives every layer; the axis values themselves are plain fields.
    var motionTick by remember { mutableIntStateOf(0) }
    // The official **never** pins an edge here: `COUITouchMotionAnimator.updateAutoDeformAnchor`
    // returns immediately unless `paramProvider.isAutoDeformAnchorEnable()` is true, that flag is a
    // plain Java boolean defaulting to `false`, and `BaseBottomFloatingMenuView` never turns it on.
    // With no anchor the pivot stays at the centre and the pill scales uniformly. Pinning an edge
    // instead makes one side shrink while the other stays put, and because the resolved anchor
    // flips between LEFT_CENTER and RIGHT_CENTER while dragging sideways, the pill visibly jumps
    // from "left side shrinks" to "right side shrinks".
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
            // `coui_bottom_floating_navigation_bar_min_height_without_padding_vertical`: the
            // space the bar reserves, with the 56dp pill centred inside it. The official gets
            // the same result from the 88dp host container it is inflated into.
            .height(CouiBottomFloatingNavigationBarDefaults.MinHeight),
        contentAlignment = Alignment.Center,
    ) {
        // `BaseBottomFloatingMenuView.onMeasure`: every item is capped at `ItemMaxWidth` and the pill
        // is exactly as wide as its items until they stop fitting, at which point the pill takes the
        // whole available width and the items shrink evenly. With five items on a 360dp screen that
        // is `(327 - 24 - 32) / 5 = 54.3dp` per item, which is what the device measures.
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
        // The material travels with the pill: `COUITouchMotionAnimator` moves the whole surface,
        // backdrop included, so the host View has to be offset by exactly the same amount as the
        // content - otherwise the capsule slides while the blurred page stays put and the bar looks
        // like it is tearing apart.
        val barTranslation = {
            motionTick
            Offset(rubberXAxis.value, rubberYAxis.value)
        }
        // The gesture layer deliberately sits **outside** the material's transform. It used to
        // live on the `Row` inside it, where `change.position` is mapped through the press scale,
        // the deformation and the layout offset - so a slow drag fed the deformation back into
        // itself. A trace of a 160 px vertical swipe showed dx = -254 / +296 and dy = -387, i.e.
        // 2.4x the finger travel with a phantom horizontal component: that is the "length changes
        // every frame" jump. Here the coordinates are the raw, untransformed finger positions,
        // which is what the official `MotionEvent.getX()` sees on its own dispatch.
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
                        // `BaseBottomFloatingMenuView#findChildViewUnder`: a raw geometric hit
                        // test against the item rects - closed intervals, **no** touchSlop, no
                        // velocity, no snapping. The very same routine decides "which item was
                        // pressed" and "which item is under the finger now".
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
                        // `MenuView#dispatchTouchEvent(ACTION_DOWN)`: remember the pressed item
                        // and clear the pending slide target. A press that lands on the capsule
                        // but not on an item belongs to the container, so it is consumed; a press
                        // on an item stays with that item's own `clickable` (taps).
                        val downIndex = itemIndexAt(downPos)
                        if (downIndex < 0 && !down.isConsumed) down.consume()
                        // The selection the gesture starts from = the official's
                        // `mSelectedItemId` before any MOVE.
                        val downSelected =
                            if (dragIndex in items.indices) dragIndex else selectedIndexState.value
                        var pendingIndex = -1
                        var lastPos = downPos
                        var upChange: PointerInputChange? = null
                        // `COUISpotLightEffectDrawable.setLightPositionByPointerIndex`: the pill-local
                        // coordinates are **divided by the bounds** before they reach the effect,
                        // so the state (and hence the shader's `u_lightPosAndHeight`) works in
                        // normalized space. Feeding raw pixels here put the light hundreds of
                        // radii away from the finger, which is why the highlight never showed up
                        // where it was pressed.
                        val origin = Offset(left, top)
                        val pillW = pillWidthPx.coerceAtLeast(1f)
                        val pillH = pillHeightPx.coerceAtLeast(1f)
                        fun Offset.normalizeToPill(): Offset = Offset(((this.x - origin.x) / pillW), ((this.y - origin.y) / pillH))
                        spotLight.press(downPos.normalizeToPill())
                        barPressed = true
                        // `handleTouchDown`: the drag spring takes over from wherever the axes
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
                                // `MenuView#onMenuTouchMove`: hit test on every frame and move
                                // the highlight **immediately** - the tab under the finger turns
                                // selected (icon state + label colour) while the finger is still
                                // down. MOVE never reports to the host; it only records the
                                // pending target (`mPendingSlideSelectItemId`) for ACTION_UP.
                                if (dragSelectEnabled) {
                                    val hit = itemIndexAt(pos)
                                    val current =
                                        if (dragIndex in items.indices) dragIndex else downSelected
                                    if (hit >= 0 && hit != current) {
                                        pendingIndex = hit
                                        dragIndex = hit
                                    }
                                }
                                // Travel from the touch-down point, exactly what
                                // `COUIITouchMotionUnit.handleTouchMove` feeds `moveAnimate`.
                                val dx = pos.x - downPos.x
                                val dy = pos.y - downPos.y
                                // `iolDeadZone` is raw pixels (5.0f, never dp-scaled).
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
                                    // `COUIRubberAnimatorUnit`: the *target* is the
                                    // rubber-banded travel; the drag spring follows it.
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
                        // `MenuView#dispatchTouchEvent(ACTION_UP)`:
                        //   1. `performClickOnChildUnder(up)` - only when the item under the
                        //      lifted finger is **not** the pressed one (a plain tap is still
                        //      the item's own click);
                        //   2. `dispatchPendingSlideItemClickIfNeeded` - then the pending slide
                        //      target.
                        // Exactly one `onItemClick` leaves the library per gesture, and only
                        // here - that is why a drag switches the page on release, never while
                        // the finger is moving.
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
                        // `barOffsetX/Y` already hold the released position, and this spring
                        // animates those very values home - there is no second source to hand
                        // over to.
                        val releaseVelocity = velocityTracker.calculateVelocity()
                        lastVx = releaseVelocity.x
                        lastVy = releaseVelocity.y
                        spotLight.release()
                        barPressed = false
                        // `backAnimation()` / `COUIRubberAnimatorUnit.handleTouchUp`.
                        //
                        // `TouchMotionHandUpHelper.computeInitialVelocity(pan, totalDist,
                        // multiplier, clamp)` is
                        //     initial = clamp(pan / totalDist * multiplier, +-clamp)
                        // and, crucially, `totalDist` is **per unit**: the deform unit uses
                        // `iolMaxDeformRubberDistance` (150dp) while the rubber unit uses
                        // `iolMaxStretchDistance` (28dp). The multiplier is how far from rest
                        // the axis currently sits - for deform that is `|scale - 1|`, confirmed
                        // in `COUIDeformAnimatorUnit` (`sub-float p5, p2, 1.0f`).
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
                // Centre pivot, exactly like the official (`pivotX/pivotY` stay at the middle).
                transformOrigin = { Offset(0.5f, 0.5f) },
                shadowEnabled = shadowEnabled,
                dark = isDark,
                modifier =
                Modifier
                    .width(pillWidth)
                    .height(CouiBottomFloatingNavigationBarDefaults.Height),
                fallback = {
                    // `applyPanelBackground()`: a flat `ColorDrawable` - the official's own fallback.
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            // No `shadowElevation`: the official fallback is a bare
                            // `ColorDrawable` and draws no shadow of its own, and a Compose
                            // elevation here paints a grey halo *outside* the capsule - the
                            // "一圈灰蒙蒙的包裹" the probe kept showing.
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
                        // never drift apart. Applying it here as well doubled the rubber-band
                        // travel and squared the deformation scale - which is exactly the "two
                        // layers" the pill showed while being dragged.
                        .onSizeChanged { barSize = Size(it.width.toFloat(), it.height.toFloat()) }
                        .drawWithContent {
                            // `BaseBottomFloatingMenuView.onDraw`: the spotlight is painted by the
                            // view itself, i.e. **under** its children, clipped to the pill.
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
                            // While a drag is in flight the tab under the finger is the selected one -
                            // exactly what `MenuView#setSelectedItem` does on every MOVE.
                            selected = index == visualSelected,
                            enabled = enabled,
                            width = itemWidth,
                            // `MenuView$9#onClick` guards on `id != mSelectedItemId ||
                            // mDispatchItemClickWhenAlreadySelected`. The drag path reports its own
                            // target on release, so by default a re-tap of the selected tab is
                            // swallowed, exactly like the official.
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
 * `layoutNaviVertical` is the layout: the icon is centred horizontally and pinned to the top by
 * `mIconTopMargin`, then the label sits `mIconTitleGapVertical` below it, also centred.
 *
 * Note the item contributes **no** visual press state of its own - the official strips pressed
 * from the item's drawable state (`shouldStripPressedForContainerTouch()`) and lets the
 * container's scale + spotlight do the work.
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
        // `applyNaviLargeIconStyle()` (item style 2). The item box only carries the 10dp,
        // gravity-CENTRE title; the icon is `coui_bottom_floating_navigation_bar_large_icon_size`
        // (40dp) and `layoutNaviLargeIcon` places it at `(itemW - iconW) / 2, top = -overflow`,
        // i.e. it rides above the item box - which is why the item calls `setClipChildren(false)`
        // and the bar does not clip its items either. Only the selected tab shows it.
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
            // `COUIFadeMaskImageView` is deliberately **not** reproduced: re-reading the
            // official `onDraw` shows it is a no-op. It draws the icon first
            // (`invoke-super ... ImageView.onDraw`), then opens a *fresh* transparent layer
            // with `saveLayer(0, 0, w, h, null)` and only then paints the radial gradient
            // (`#00FFFFFF -> #33FFFFFF`) with `PorterDuff.DST_IN` - `DST_IN` composites
            // against an empty destination, so the result is fully transparent and the icon
            // is left untouched. To fade anything the order would have to be
            // `saveLayer -> draw the icon -> DST_IN -> restore`; both the market 26.9.4 and
            // the contacts 17.20.80 builds ship the same (ineffective) order.
            //
            // So the selected tab changes **only** through `state_selected`: the caller's
            // [CouiBottomFloatingNavigationBarItem.iconTint] (or a stateful icon drawable) is
            // the whole "selected icon" treatment. The earlier `DstIn` overlay here is what
            // made our selected icon visibly fade - which the official never does.
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
 * It overflows the icon: `coui_bottom_floating_navigation_bar_red_dot_right_overflow_*_digit`
 * pushes it past the right edge (the amount grows with the number of digits, so a wider capsule
 * stays optically centred) and `..._top_overflow` past the top edge. The item sets
 * `setClipChildren(false)`, which is why it can.
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
