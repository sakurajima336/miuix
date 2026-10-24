// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiMinVisibleChange
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * What a [CouiBottomFloatingBar] accessory slot can say about its round button.
 *
 * The scope is deliberately tiny: it mirrors **exactly** the five things the official container
 * lets a caller do to one of its two `COUIBottomFloatingAccessoryView` children
 * (`46-ACCESSORY-BUTTON-API.md` §1.2):
 *
 * | here | official |
 * |---|---|
 * | [icon] | `setStart/EndAccessoryIcon` / `setStart/EndAccessoryDrawable` (`:2364-2406`, `:2198-2240`) |
 * | [contentDescription] | `View.setContentDescription` - no COUI setter (`TabFragment.smali:3490/3550`) |
 * | [onClick] | `setOnStart/EndAccessoryClickListener` (`:2310`, `:2272`) |
 * | [backgroundColor] | `setStart/EndAccessoryBackgroundColor` (`:2354`, `:2188`) |
 * | [visible] | `showStart/EndAccessoryView` / `hideStart/EndAccessoryView` (`:2476`, `:1330`, `:2428`, `:1292`) |
 *
 * The scope is a **builder**: [CouiBottomFloatingBar] re-runs it on every composition, exactly the
 * way a host calls the official setters whenever it wants a side to change. A host that flips
 * [visible] at runtime therefore has to do so from state it reads while composing the bar (the
 * declarative equivalent of "call the setter and let the view re-lay-out").
 *
 * There is **no** "put my own View in the slot" capability, because the official has none either:
 * both children are `private final COUIBottomFloatingAccessoryView` fields created in the
 * container's constructor (`:37`, `:75`, `:219-230`), the container never
 * overrides `addView`/`removeView`, and `onLayout`/`onMeasure` only
 * touch those three named fields (`:1825-1902`, `:1993-2072`). A fourth child would
 * break the fixed `new int[3]` `mOnLayoutChildLefts` array (`:133-137`).
 */
interface CouiAccessorySlotScope {
    /**
     * Sets the slot's icon, i.e. `setStart/EndAccessoryDrawable(drawable, animate)`.
     *
     * A slot with **no icon is not laid out at all**: `isAccessoryIconConfigured(view)` is
     * literally `getIconDrawable() != null`
     * (`COUIBottomFloatingAccessoryNavigationBar.smali:676-689`) and
     * `isAccessoryVisible` short-circuits on it (`:702-717`), so an icon-less side
     * is `GONE` and the pill stays centred.
     *
     * Setting an icon also shows the slot - **the one place this API is more forgiving than the
     * official**, which needs a separate `showStartAccessoryView(true)` after
     * `setStartAccessoryDrawable` (that setter only forwards to `setIconDrawable`,
     * `:2375-2388`). An explicit [visible] call always wins, whatever the order.
     *
     * @param painter The icon. `null` clears it and drops the slot from the layout entirely.
     * @param animate Whether to play the swap animation.
     */
    fun icon(painter: Painter?, animate: Boolean = true)

    /** The slot button's accessibility label, i.e. `View.setContentDescription`. */
    fun contentDescription(description: String?)

    /** The slot button's click callback, i.e. `setOnStart/EndAccessoryClickListener`. */
    fun onClick(onClick: () -> Unit)

    /** The slot button's fill, i.e. `setStart/EndAccessoryBackgroundColor`. */
    fun backgroundColor(color: Color)

    /**
     * Shows or hides the slot, i.e. `showStart/EndAccessoryView(boolean)` /
     * `hideStart/EndAccessoryView(boolean)`.
     *
     * Hiding is not the same as clearing the icon: the icon is kept, so the button can come back
     * with the expansion springs. A hidden slot still **reserves its 12dp gap** - the official
     * keeps the child in the layout and only drives its `translationX`/`scale`/`alpha`
     * (`AccessorySideExpansionController.applyAlphaScaleState`). Omit [icon] entirely for a
     * side that should not occupy space at all.
     *
     * This is also the only way to keep a slot dark: the official's own `setStart/EndAccessoryIcon`
     * does **not** call `showStart/EndAccessoryView`, so a host that fills an icon and forgets the
     * show call keeps the side hidden.
     *
     * @param visible Whether the slot is shown.
     * @param animate Whether to run the expansion springs.
     */
    fun visible(visible: Boolean, animate: Boolean = true)
}

/**
 * The values one accessory slot holds.
 *
 * Plain fields on purpose: the slot lambda is a *builder* that [CouiBottomFloatingBar] re-runs on
 * every composition - exactly like the official's setters, which the host calls whenever it wants
 * to change a side - so the values are always fresh without any of them being Compose state. That
 * also means writing them during composition cannot loop.
 */
internal class CouiAccessorySlot : CouiAccessorySlotScope {
    var painter: Painter? = null
        private set
    var animateIcon: Boolean = true
        private set
    var description: String? = null
        private set
    var clickAction: (() -> Unit)? = null
        private set
    var background: Color = Color.Unspecified
        private set
    var shown: Boolean = false
        private set
    var animateVisibility: Boolean = true
        private set

    /** Whether [visible] was called, so an explicit visibility always wins over [icon]'s default. */
    private var visibilitySet = false

    override fun icon(painter: Painter?, animate: Boolean) {
        this.painter = painter
        animateIcon = animate
        // `showStart/EndAccessoryView` is what makes a freshly configured icon appear - unless the
        // host already said otherwise, in which case the explicit call wins whatever the order.
        if (painter != null && !visibilitySet) shown = true
    }

    override fun contentDescription(description: String?) {
        this.description = description
    }

    override fun onClick(onClick: () -> Unit) {
        clickAction = onClick
    }

    override fun backgroundColor(color: Color) {
        background = color
    }

    override fun visible(visible: Boolean, animate: Boolean) {
        shown = visible
        animateVisibility = animate
        visibilitySet = true
    }

    /** The official's `isAccessoryIconConfigured`: only a side with an icon takes part. */
    val configured: Boolean get() = painter != null

    /** Clears everything so a lambda that stops setting a field also stops applying it. */
    fun reset() {
        painter = null
        animateIcon = true
        description = null
        clickAction = null
        background = Color.Unspecified
        shown = false
        animateVisibility = true
        visibilitySet = false
    }
}

/**
 * `AccessorySideExpansionController`'s show/hide springs.
 *
 * The controller drives three `COUISpringForce` instances
 * (`17-ACCESSORY-MELTBALL.md` §10, `#initTranslationAnim` L927 /
 * `#initScaleAnim` L807 / `#initAlphaAnim` L710):
 *
 * | channel | response | bounce |
 * |---|---|---|
 * | translationX | 0.55 | 0.35 |
 * | scale | 0.55 | 0.35 |
 * | alpha (non-melt-ball fallback) | 0.35 | 0.0 |
 *
 * Melt-ball replaces the alpha spring with a live overlap ratio; melting is a system
 * `RenderNode` effect that this library does not reproduce, so the non-melt-ball alpha
 * spring is used.
 */
private val CouiAccessoryExpansionSpring = CouiSpringData(bounce = 0.35f, response = 0.55f)

/** @see CouiAccessoryExpansionSpring */
private val CouiAccessoryAlphaSpring =
    CouiSpringData(bounce = 0f, response = 0.35f).toSpringSpec(CouiMinVisibleChange.Alpha)

/**
 * The floating bar **with the two accessory slots** the official
 * `COUIBottomFloatingAccessoryNavigationBar` carries at its ends.
 *
 * This is the second half of the two-layer API from `46-ACCESSORY-BUTTON-API.md` §7: the
 * round button is an independent component ([CouiAccessoryButton]) and the container only *places*
 * it - exactly like the official, which news up both accessory views in its own constructor and
 * never accepts a View from outside.
 *
 * ## Geometry - every number here is code-side, none of it is a screenshot measurement
 *
 * * **Three fixed slots, not a list.** `mOnLayoutChildLefts = new int[3]` (`:133-137`)
 *   and `onLayout`/`onMeasure` only handle the three named fields, so a fourth
 *   accessory cannot exist.
 * * **One spacing constant: 12dp.** `mSideViewSpacing` (`:183`, dimen
 *   `0x7f0707c4`) is added once per configured side, so the row is
 *   `[start 56] 12 [pill] 12 [end 56]` and the *whole row* is centred
 *   (`layoutPositionsMediumScreen`, `:797-889`). The pill's own 12dp
 *   `mInnerPadding` is an **inner** inset and does not add to the gap: the panel background
 *   fills the pill's full bounds (`caustic-java/BaseBottomFloatingMenuView.smali:3173-3190`).
 *   The earlier "27.2dp" reading is retracted - it measured the level-5 glow's outer edge, not the
 *   view bounds (`41A-GALLERY-GEOMETRY.md` §勘误).
 * * **Each side is centred vertically on its own** (`:1717-1726`), which only *looks* like
 *   top alignment because both boxes are 56dp tall.
 * * **A slot with no icon is `GONE` and takes no space** (`:249`, `:252`,
 *   `:676-717`), which is what keeps the pill centred when only one side is used.
 *
 * A slot is a builder lambda, not a Composable: it only *describes* the side (icon, label,
 * callback, fill, visibility) and the bar turns that description into a [CouiAccessoryButton].
 *
 * @param items The tabs, left to right.
 * @param selectedIndex The index of the selected tab.
 * @param onSelect Called with the index the user tapped.
 * @param modifier The modifier to apply.
 * @param startAccessory The leading slot. Omit it (or leave its [CouiAccessorySlotScope.icon]
 *   unset) and that side is `GONE`.
 * @param endAccessory The trailing slot.
 * @param accessorySpacing The gap between a slot and the pill, i.e. `mSideViewSpacing`. The
 *   official has no setter for it - overriding it is a library extension, not official behaviour.
 * @param materialEnabled Whether the framework material is requested (see
 *   [CouiBottomFloatingNavigationBar]).
 * @param shadowEnabled Whether the material's inner shadow and the caustic glows are applied.
 * @param enabled Whether the bar can be interacted with.
 * @param dragSelectEnabled Hold the pill and slide left/right to move the selection.
 * @param dispatchClickWhenAlreadySelected Whether a tap on the already selected tab still reports.
 * @param backdrop Compose-side capture of the page behind the bar.
 */
@Composable
fun CouiBottomFloatingBar(
    items: List<CouiBottomFloatingNavigationBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    startAccessory: (CouiAccessorySlotScope.() -> Unit)? = null,
    endAccessory: (CouiAccessorySlotScope.() -> Unit)? = null,
    accessorySpacing: Dp = CouiAccessoryButtonDefaults.Spacing,
    materialEnabled: Boolean = true,
    shadowEnabled: Boolean = true,
    enabled: Boolean = true,
    dragSelectEnabled: Boolean = true,
    dispatchClickWhenAlreadySelected: Boolean = false,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    /**
     * Whether the pill (the tab capsule) is shown. `false` is the official's `menuView == GONE`
     * state, which leaves only the two accessory buttons.
     *
     * This is **not** `COUIBottomFloatingNavigationBar.showMenuView`/`hideMenuView`: those animate
     * alpha/scale and end on `INVISIBLE`, so the capsule keeps its slot and the row keeps its
     * centring (`MenuView.doMenuViewShowOrHideAnim` + `checkAllAnimationsEnded`,
     * `COUIBottomFloatingNavigationBar.smali:3242-3261` / `:4091-4110` / `:490-507`). Here the
     * capsule leaves the layout entirely, exactly like a real `View.GONE`.
     *
     * Geometry in that state, from `COUIBottomFloatingAccessoryNavigationBar#onLayout`'s GONE
     * branch (`:1682-1690` + `layoutAccessoryIfVisible:742-768`):
     *
     * * the buttons sit **hard against the edges** - `paddingLeft` on the left, `right - 56dp` on
     *   the right. `mSideViewSpacing` is not referenced on that path at all, so the usual 12dp gap
     *   disappears instead of being redistributed;
     * * each side is vertically centred on the row, which is still **88dp** tall - `onMeasure`
     *   keeps the `min_height_without_padding_vertical` max (`:2115-2131`), it does not shrink to
     *   the button height;
     * * a side with no icon reserves nothing, so with only one accessory configured the row is not
     *   pushed to the opposite edge by a phantom slot.
     *
     * The official exposes this state only through `getMenuView().setVisibility(GONE)` - there is
     * no setter for it, and nothing in the container ever writes `GONE` itself
     * (`49-ACCESSORY-ONLY-MODE.md`).
     */
    pillVisible: Boolean = true,
) {
    val start = remember { CouiAccessorySlot() }
    val end = remember { CouiAccessorySlot() }
    start.reset()
    startAccessory?.invoke(start)
    end.reset()
    endAccessory?.invoke(end)

    val startConfigured = start.configured
    val endConfigured = end.configured

    BoxWithConstraints(
        modifier =
        modifier
            .padding(horizontal = CouiBottomFloatingNavigationBarDefaults.PaddingHorizontal)
            // `coui_bottom_floating_navigation_bar_min_height_without_padding_vertical`: the
            // space the row reserves, with the 56dp controls centred inside it.
            .height(CouiBottomFloatingNavigationBarDefaults.MinHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (!pillVisible) {
            // The official's `menuView == GONE` branch. `COUIBottomFloatingAccessoryNavigationBar
            // #onLayout` tests `mMenuView.getVisibility() == GONE` (`:1682-1690`) and then lays out
            // nothing but the two accessories through `layoutAccessoryIfVisible` (`:742-768`):
            //
            //   left  = paddingLeft                       (start)
            //   left  = right - paddingRight - 56dp       (end)
            //   top   = (height - 56dp) / 2 + paddingTop  (both)
            //
            // `mSideViewSpacing` has **zero** references on that path, so the 12dp gap is gone and
            // the buttons sit hard against the edges. The row keeps its 88dp reserve: `onMeasure`
            // still takes the `min_height_without_padding_vertical` max (`:2115-2131`), it does not
            // collapse to the button height.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (startConfigured) {
                    CouiAccessorySlotView(
                        state = start,
                        enabled = enabled,
                        spacing = 0.dp,
                        leading = true,
                        backdrop = backdrop,
                    )
                } else {
                    Spacer(Modifier.width(CouiAccessoryButtonDefaults.Size))
                }
                if (endConfigured) {
                    CouiAccessorySlotView(
                        state = end,
                        enabled = enabled,
                        spacing = 0.dp,
                        leading = false,
                        backdrop = backdrop,
                    )
                } else {
                    Spacer(Modifier.width(CouiAccessoryButtonDefaults.Size))
                }
            }
        } else {
            // `layoutPositionsMediumScreen`: reserve `accSize + spacing` once per
            // configured side, then centre what is left.
            var reserve = 0.dp
            if (startConfigured) reserve += CouiAccessoryButtonDefaults.Size + accessorySpacing
            if (endConfigured) reserve += accessorySpacing + CouiAccessoryButtonDefaults.Size
            val itemSpace = CouiBottomFloatingNavigationBarDefaults.ItemSpace
            val idealPillWidth =
                CouiBottomFloatingNavigationBarDefaults.ItemMaxWidth * items.size +
                    CouiBottomFloatingNavigationBarDefaults.InnerPadding * 2 +
                    itemSpace * (items.size - 1).coerceAtLeast(0)
            val pillWidth = minOf((maxWidth - reserve).coerceAtLeast(0.dp), idealPillWidth)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (startConfigured) {
                    CouiAccessorySlotView(
                        state = start,
                        enabled = enabled,
                        spacing = accessorySpacing,
                        leading = true,
                        backdrop = backdrop,
                    )
                    Spacer(Modifier.width(accessorySpacing))
                }
                CouiBottomFloatingNavigationBar(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelected = onSelect,
                    // The row already owns the screen-edge padding and the 88dp reserve, so the pill
                    // must not add either of them a second time.
                    modifier = Modifier.width(pillWidth),
                    horizontalPadding = 0.dp,
                    materialEnabled = materialEnabled,
                    shadowEnabled = shadowEnabled,
                    enabled = enabled,
                    dragSelectEnabled = dragSelectEnabled,
                    dispatchClickWhenAlreadySelected = dispatchClickWhenAlreadySelected,
                    backdrop = backdrop,
                )
                if (endConfigured) {
                    Spacer(Modifier.width(accessorySpacing))
                    CouiAccessorySlotView(
                        state = end,
                        enabled = enabled,
                        spacing = accessorySpacing,
                        leading = false,
                        backdrop = backdrop,
                    )
                }
            }
        }
    }
}

/**
 * One accessory slot: the round button plus the show/hide springs the official's
 * `AccessorySideExpansionController` runs.
 *
 * The collapsed state is the controller's - scale `0.5`, alpha `0`, translated
 * towards the pill by `accSize + spacing` so it ends up under the pill's edge
 * (`mFakeBounds`). A slot that has an icon but is not
 * [CouiAccessorySlotScope.visible] therefore still reserves its space, exactly like the official.
 */
@Composable
private fun CouiAccessorySlotView(
    state: CouiAccessorySlot,
    enabled: Boolean,
    spacing: Dp,
    leading: Boolean,
    backdrop: CouiBackdrop?,
) {
    val shown = state.shown
    val scale = remember { Animatable(if (shown) 1f else 0.5f) }
    val alpha = remember { Animatable(if (shown) 1f else 0f) }
    val travel = remember { Animatable(0f) }
    // Without a pill there is nothing to slide *under*: the official's `mFakeBounds` target is the
    // capsule, so with the capsule GONE the collapsed slot travels its own width instead of
    // `accSize + spacing` and leaves the row completely rather than stopping half-way.
    val collapsedTravel = (CouiAccessoryButtonDefaults.Size + spacing).value
    // The leading side slides right (into the pill), the trailing one slides left.
    val travelTarget = if (shown) {
        0f
    } else if (leading) {
        collapsedTravel
    } else {
        -collapsedTravel
    }
    val animate = state.animateVisibility
    LaunchedEffect(shown, animate) {
        if (!animate) {
            // `applyAlphaScaleState(active, animate = false)`: snap, no springs.
            scale.snapTo(if (shown) 1f else 0.5f)
            alpha.snapTo(if (shown) 1f else 0f)
            travel.snapTo(travelTarget)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                scale.animateTo(
                    if (shown) 1f else 0.5f,
                    CouiAccessoryExpansionSpring.toSpringSpec(CouiMinVisibleChange.Scale),
                )
            }
            launch { alpha.animateTo(if (shown) 1f else 0f, CouiAccessoryAlphaSpring) }
            launch {
                travel.animateTo(
                    travelTarget,
                    CouiAccessoryExpansionSpring.toSpringSpec(CouiMinVisibleChange.Pixels),
                )
            }
        }
    }
    val background =
        if (state.background == Color.Unspecified) {
            CouiAccessoryButtonDefaults.backgroundColor()
        } else {
            state.background
        }
    CouiAccessoryButton(
        onClick = { state.clickAction?.invoke() },
        icon = state.painter,
        contentDescription = state.description,
        // The official button is `setClickable(true)` regardless of whether a listener was set; a
        // tap without one simply does nothing.
        enabled = enabled,
        backgroundColor = background,
        animateIconChange = state.animateIcon,
        // The official accessory mounts its own `setBackgroundRenderEffect`, so the blur samples
        // the page behind the button - not the bar's already-blurred pill.
        backdrop = backdrop,
        modifier =
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
            translationX = travel.value
        },
    )
}
