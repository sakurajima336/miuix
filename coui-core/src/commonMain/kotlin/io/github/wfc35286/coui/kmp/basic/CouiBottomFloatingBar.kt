// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
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
import io.github.wfc35286.coui.kmp.graphics.CouiBackdrop
import io.github.wfc35286.coui.kmp.graphics.LocalCouiBackdrop
import io.github.wfc35286.coui.kmp.motion.CouiMinVisibleChange
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * What a [CouiBottomFloatingBar] accessory slot can say about its round button.
 *
 * The scope is deliberately tiny: it covers exactly the five things the container can do to one
 * of its two accessory slots - [icon], [contentDescription], [onClick], [backgroundColor] and
 * [visible].
 *
 * The scope is a **builder**: [CouiBottomFloatingBar] re-runs it on every composition. A host
 * that flips [visible] at runtime therefore has to do so from state it reads while composing
 * the bar.
 *
 * There is **no** "put my own View in the slot" capability: the container owns its two
 * accessory buttons and only places them, it never accepts an arbitrary child from outside.
 */
interface CouiAccessorySlotScope {
    /**
     * Sets the slot's icon.
     *
     * A slot with **no icon is not laid out at all**: it drops out of the layout and the pill
     * stays centred. Setting an icon also shows the slot; an explicit [visible] call always
     * wins, whatever the order.
     *
     * @param painter The icon. `null` clears it and drops the slot from the layout entirely.
     * @param animate Whether to play the swap animation.
     */
    fun icon(painter: Painter?, animate: Boolean = true)

    /** The slot button's accessibility label. */
    fun contentDescription(description: String?)

    /** The slot button's click callback. */
    fun onClick(onClick: () -> Unit)

    /** The slot button's fill. */
    fun backgroundColor(color: Color)

    /**
     * Shows or hides the slot.
     *
     * Hiding is not the same as clearing the icon: the icon is kept, so the button can come back
     * with the expansion springs. A hidden slot still **reserves its 12dp gap** - the button
     * collapses (scale, alpha and translation animate) but stays in the layout. Omit [icon]
     * entirely for a side that should not occupy space at all.
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
 * every composition, so the values are always fresh without any of them being Compose state. That
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
        // A freshly configured icon becomes visible unless the host already set visibility
        // explicitly, in which case the explicit call wins whatever the order.
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

    /** Whether the slot has an icon; only a side with an icon takes part in the layout. */
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
 * The show/hide springs for an accessory slot.
 *
 * The translation and scale channels share one spring (`bounce = 0.35`, `response = 0.55`);
 * the alpha channel uses a non-bouncing `0.35` fallback, because the melt-ball alpha effect is
 * a system render effect that this library does not reproduce.
 */
private val CouiAccessoryExpansionSpring = CouiSpringData(bounce = 0.35f, response = 0.55f)

/** @see CouiAccessoryExpansionSpring */
private val CouiAccessoryAlphaSpring =
    CouiSpringData(bounce = 0f, response = 0.35f).toSpringSpec(CouiMinVisibleChange.Alpha)

/**
 * A floating bar with the two accessory slots at its ends.
 *
 * The round button is an independent component ([CouiAccessoryButton]); the container only
 * *places* it, it never accepts an arbitrary child from outside.
 *
 * ## Geometry
 *
 * * **Three fixed slots, not a list.** The layout handles the pill plus the two named
 *   accessory slots, so a fourth accessory cannot exist.
 * * **One spacing constant: 12dp.** [accessorySpacing] is added once per configured side, and
 *   the *whole row* is centred. The pill's own 12dp inner padding is an **inner** inset and
 *   does not add to the gap: the panel background fills the pill's full bounds.
 * * **Each side is centred vertically on its own**, which only *looks* like top alignment
 *   because both boxes are 56dp tall.
 * * **A slot with no icon takes no space**, which is what keeps the pill centred when only one
 *   side is used.
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
 * @param accessorySpacing The gap between a slot and the pill.
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
     * Whether the pill (the tab capsule) is shown. `false` leaves only the two accessory
     * buttons.
     *
     * This is not a hide-with-space animation: the capsule leaves the layout entirely, like a
     * real `View.GONE`. In that state:
     *
     * * the buttons sit **hard against the edges** - the usual 12dp gap disappears instead of
     *   being redistributed;
     * * each side is vertically centred on the row, and the row keeps its full height reserve,
     *   it does not shrink to the button height;
     * * a side with no icon reserves nothing, so with only one accessory configured the row is
     *   not pushed to the opposite edge by a phantom slot.
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
            // The space the row reserves, with the 56dp controls centred inside it.
            .height(CouiBottomFloatingNavigationBarDefaults.MinHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (!pillVisible) {
            // Without the pill the two accessory slots sit hard against the edges: no spacing
            // between them and the edge padding, and the row keeps its full height reserve
            // rather than collapsing to the button height.
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
            // Reserve one accessory button plus the spacing once per configured side, then
            // centre what is left.
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
 * One accessory slot: the round button plus its show/hide springs.
 *
 * The collapsed state scales the button to `0.5`, fades it to `0`, and translates it towards
 * the pill by `accSize + spacing` so it ends up under the pill's edge. A slot that has an icon
 * but is not [CouiAccessorySlotScope.visible] therefore still reserves its space.
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
    // Without a pill there is nothing to slide *under*: the collapsed slot travels its own
    // width and leaves the row completely rather than stopping half-way.
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
            // Snap, no springs.
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
        // The button stays clickable even without a listener; a tap without one simply does
        // nothing.
        enabled = enabled,
        backgroundColor = background,
        animateIconChange = state.animateIcon,
        // The blur samples the page behind the button, not the bar's already-blurred pill.
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
