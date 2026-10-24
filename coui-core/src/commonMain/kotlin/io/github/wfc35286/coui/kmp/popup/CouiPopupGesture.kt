// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


internal class CouiPopupPanelState(val motion: CouiPopupTouchMotion) {
    val rows = mutableStateMapOf<Int, Rect>()
    val titleLines = mutableStateMapOf<Int, Int>()
    var coordinates: LayoutCoordinates? = null
    var layoutOffset = Offset.Zero
    var items: List<CouiPopupListItem> = emptyList()
    var scroll: ScrollState? = null
    var talkback = false
    var parentIndex = -1
    var click: (Int) -> Unit = {}
    private val press = PopupSpringAxis(0f, 0.0001f)
    private val move = PopupSpringAxis(1f, 0.0001f)
    private var start by mutableStateOf(Rect.Zero)
    private var end by mutableStateOf(Rect.Zero)
    var initialized by mutableStateOf(false)
        private set
    var singleLine by mutableStateOf(true)
        private set
    private var pendingHandoff: Float? = null
    var requestFrame: () -> Unit = {}
    val pressProgress: Float get() = press.value.coerceIn(0f, 1f)
    val maskRect: Rect
        get() {
            val f = move.value
            return Rect(start.left + (end.left - start.left) * f, start.top + (end.top - start.top) * f, start.right + (end.right - start.right) * f, start.bottom + (end.bottom - start.bottom) * f)
        }
    val scrollOffset: Int get() = scroll?.value ?: 0
    val slideSelection: Boolean get() = scroll?.maxValue == 0 && !talkback

    fun local(root: LayoutCoordinates?, point: Offset): Offset? {
        val c = coordinates ?: return null
        if (root == null || !root.isAttached || !c.isAttached) return null
        return c.localPositionOf(root, point)
    }

    fun hit(root: LayoutCoordinates?, point: Offset): Int {
        val local = local(root, point) ?: return -1
        return rows.entries.firstOrNull { it.value.contains(local) }?.key ?: -1
    }

    fun valid(index: Int): Boolean = items.getOrNull(index)?.let { it.enabled && it.type == CouiPopupItemType.Default } == true

    fun row(index: Int, rect: Rect) {
        if (rows[index] != rect) rows[index] = rect
        if (index == 0) {
            pendingHandoff?.let { value ->
                pendingHandoff = null
                attach(index, animate = false)
                // Handing the highlight off to the sub menu: the sub menu's parent row mirrors
                // the parent panel's press progress, so the transferred highlight starts at the
                // given value and fades out - it is NOT locked on.
                press.snap(value)
                press.target = 0f
                requestFrame()
            }
        }
    }

    fun attach(index: Int, animate: Boolean = true) {
        val rect = rows[index] ?: return
        if (!valid(index)) return
        singleLine = titleLines[index] == 1
        if (!initialized || !animate) {
            start = rect
            end = rect
            move.snap(1f)
            initialized = true
        } else if (rect != end) {
            start = maskRect
            end = rect
            move.snap(0f)
            move.target = 1f
        }
        move.spring = io.github.wfc35286.coui.kmp.motion.CouiSpringData(0f, 0.15f)
        press.spring = io.github.wfc35286.coui.kmp.motion.CouiSpringData(0f, 0.3f)
        press.target = 1f
        requestFrame()
    }

    fun clear(immediate: Boolean = false) {
        press.target = 0f
        if (immediate) {
            press.snap(0f)
            initialized = false
        }
        requestFrame()
    }

    fun handoff(progress: Float) {
        pendingHandoff = progress.takeIf { it > 0f }
        rows[0]?.let { row(0, it) }
    }

    fun advance(dt: Float): Boolean {
        val moving = press.advance(dt) or move.advance(dt)
        if (press.value == 0f && press.target == 0f) initialized = false
        return moving
    }
}

private data class PopupLongPressTarget(val panel: CouiPopupPanelState, val index: Int, val anchor: Boolean)

@Composable
internal fun Modifier.couiPopupGestures(
    main: CouiPopupPanelState,
    sub: CouiPopupPanelState,
    expanded: Boolean,
    subExpanded: Boolean,
    subEntered: Boolean,
    mainBounds: () -> Rect,
    subBounds: () -> Rect,
    onLongPress: (Boolean, Int) -> Unit,
    onHideSubMenu: () -> Unit,
    onDismiss: () -> Unit,
    consumeMainMenuTouchRestore: () -> Boolean = { false },
    consumeReDispatchDown: () -> Boolean = { false },
): Modifier {
    val currentExpanded = rememberUpdatedState(expanded)
    val currentSub = rememberUpdatedState(subExpanded)
    val currentEntered = rememberUpdatedState(subEntered)
    val currentMainBounds = rememberUpdatedState(mainBounds)
    val currentSubBounds = rememberUpdatedState(subBounds)
    val currentLong = rememberUpdatedState(onLongPress)
    val currentHide = rememberUpdatedState(onHideSubMenu)
    val currentDismiss = rememberUpdatedState(onDismiss)
    val haptics = io.github.wfc35286.coui.kmp.motion.rememberCouiHaptics()
    val longHaptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val root = androidx.compose.runtime.remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onGloballyPositioned { root[0] = it }.pointerInput(main, sub) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!currentExpanded.value) return@awaitEachGesture
                var active: CouiPopupPanelState? = null
                var candidate = -1
                var cancelled = false
                var clickBlocked = false
                var lastTriggered: PopupLongPressTarget? = null
                var detecting: PopupLongPressTarget? = null
                var detectionScroll = 0
                var detectionJob: Job? = null
                var dismissJob: Job? = null
                var anchorOrigin = down.position
                var mainAreaOrigin: Offset? = null
                var lastPoint = down.position
                var panelDown = down.position
                var panelScrollStart = 0
                var sequenceCancelled = false
                var lastSub = currentSub.value
                var lastEntered = currentEntered.value
                val velocity = VelocityTracker()
                fun cancelDetection() {
                    detectionJob?.cancel()
                    detectionJob = null
                    detecting = null
                }
                fun resolve(point: Offset): PopupLongPressTarget? {
                    val panel = if (currentSub.value && currentSubBounds.value().contains(point)) sub else main
                    val index = panel.hit(root[0], point)
                    if (!panel.valid(index)) return null
                    val anchor = panel === sub && index == panel.parentIndex
                    return if (anchor || (panel === main && panel.items[index].subItems.isNotEmpty())) PopupLongPressTarget(panel, index, anchor) else null
                }
                fun schedule(target: PopupLongPressTarget) {
                    if (detectionJob != null) return
                    detectionJob = launch {
                        delay(650L)
                        if (detecting == target && target.panel.scrollOffset == detectionScroll) {
                            lastTriggered = target
                            candidate = -1
                            clickBlocked = true
                            longHaptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            currentLong.value(target.anchor, target.index)
                            cancelDetection()
                            active?.clear()
                            active = null
                        }
                    }
                }
                fun detect(point: Offset) {
                    val target = detecting
                    if (target != null) {
                        if (target.panel.scrollOffset != detectionScroll || target.panel.hit(root[0], point) != target.index) {
                            cancelDetection()
                            return
                        }
                        if (target.anchor && lastTriggered?.anchor == false) {
                            if ((point - anchorOrigin).getDistanceSquared() > viewConfiguration.touchSlop * viewConfiguration.touchSlop) schedule(target)
                        } else {
                            schedule(target)
                        }
                        return
                    }
                    val next = resolve(point) ?: return
                    if (next == lastTriggered) return
                    detecting = next
                    detectionScroll = next.panel.scrollOffset
                    anchorOrigin = point
                    if (!next.anchor || lastTriggered?.anchor != false) schedule(next)
                }
                fun cancelPanel() {
                    active?.let {
                        it.motion.up()
                        it.clear()
                    }
                    active = null
                    candidate = -1
                }
                fun begin(panel: CouiPopupPanelState, point: Offset) {
                    active = panel
                    cancelled = sequenceCancelled
                    panelDown = point
                    panelScrollStart = panel.scrollOffset
                    candidate = panel.hit(root[0], point).takeIf { panel.valid(it) } ?: -1
                    panel.motion.down(point - panel.layoutOffset)
                    if (candidate >= 0) panel.attach(candidate)
                    clickBlocked = false
                    velocity.resetTracking()
                    velocity.addPosition(down.uptimeMillis, point)
                }
                fun updateMainArea(point: Offset) {
                    if (!currentEntered.value) return
                    val exclusive = currentMainBounds.value().contains(point) && !currentSubBounds.value().contains(point)
                    val origin = mainAreaOrigin
                    if (!exclusive) {
                        mainAreaOrigin = null
                        dismissJob?.cancel()
                        dismissJob = null
                    } else if (origin == null || (point - origin).getDistanceSquared() > viewConfiguration.touchSlop * viewConfiguration.touchSlop) {
                        mainAreaOrigin = origin ?: point
                        if (dismissJob == null) {
                            dismissJob = launch {
                                delay(500L)
                                if (currentEntered.value) currentHide.value()
                            }
                        }
                    }
                }
                val inSub = currentSub.value && currentSubBounds.value().contains(down.position)
                val inMain = currentMainBounds.value().contains(down.position)
                // Once the sub menu exit animation has finished, the *next* touch event drops the
                // main menu's stale press state so the same gesture keeps tracking the row under
                // the finger. Consuming the flag after `begin()` would cancel the freshly started
                // press, and the first tap after the exit would need a second tap to land.
                if (consumeReDispatchDown()) {
                    main.motion.up()
                    main.clear()
                }
                if (!inSub && !inMain) {
                    down.consume()
                    if (currentSub.value) currentHide.value() else currentDismiss.value()
                    clickBlocked = true
                } else if (inSub) {
                    begin(sub, down.position)
                } else if (!currentSub.value) {
                    begin(main, down.position)
                } else {
                    clickBlocked = true
                    mainAreaOrigin = down.position
                }
                detect(down.position)
                if (active?.slideSelection == true) down.consume()
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (event.changes.count { it.pressed } > 1) {
                            cancelled = true
                            sequenceCancelled = true
                            clickBlocked = true
                            cancelDetection()
                            cancelPanel()
                        }
                        if (consumeReDispatchDown()) {
                            // Mid gesture: the exit finished while the finger is down. The press
                            // state is reset so the row under the finger starts a fresh press
                            // instead of losing the gesture.
                            cancelPanel()
                            main.motion.up()
                            main.clear()
                            if (!currentSub.value && currentMainBounds.value().contains(change.position)) {
                                begin(main, change.position)
                            }
                            clickBlocked = false
                        }
                        lastPoint = change.position
                        velocity.addPosition(change.uptimeMillis, change.position)
                        if (!change.pressed) {
                            cancelDetection()
                            dismissJob?.cancel()
                            val panel = active
                            val speed = velocity.calculateVelocity()
                            panel?.motion?.up(Offset(speed.x, speed.y))
                            panel?.clear()
                            if (consumeMainMenuTouchRestore()) {
                                main.motion.up()
                                main.clear()
                            }
                            if (panel != null && !cancelled && !clickBlocked && panel.scrollOffset == panelScrollStart && candidate >= 0 && panel.valid(candidate)) {
                                panel.click(candidate)
                            } else if (currentSub.value && mainAreaOrigin != null) {
                                currentHide.value()
                            }
                            // Only claim the UP when this gesture really handled something. This
                            // listener runs in the Initial pass, so consuming here marks the event
                            // consumed for every later pass; the list's fling is decided in the Main
                            // pass, and a consumed UP cancels it.
                            if (!cancelled) change.consume()
                            break
                        }
                        if (currentSub.value != lastSub) {
                            cancelPanel()
                            lastSub = currentSub.value
                            clickBlocked = true
                        }
                        if (lastEntered && !currentEntered.value && !currentSub.value) {
                            lastTriggered = null
                            cancelDetection()
                        }
                        lastEntered = currentEntered.value
                        updateMainArea(lastPoint)
                        detect(lastPoint)
                        if (sequenceCancelled) {
                            change.consume()
                            continue
                        }
                        if (currentSub.value) {
                            if (active === main) cancelPanel()
                            if (currentEntered.value && currentSubBounds.value().contains(lastPoint)) {
                                if (active !== sub) {
                                    cancelPanel()
                                    begin(sub, lastPoint)
                                    clickBlocked = false
                                }
                            }
                            // While the sub menu is up it owns the whole touch stream and the main
                            // menu list ignores touches. So leaving the sub menu rect must NOT
                            // cancel the panel - the deform/rubber band keeps following the finger.
                        } else if (active === sub) {
                            cancelPanel()
                        } else if (active == null && currentMainBounds.value().contains(lastPoint) && sub.items.isEmpty()) {
                            begin(main, lastPoint)
                            clickBlocked = false
                        }
                        val panel = active
                        if (panel != null) {
                            panel.motion.move(lastPoint - panel.layoutOffset)
                            if (!panel.slideSelection && (panel.scrollOffset != panelScrollStart || (lastPoint - panelDown).getDistance() > viewConfiguration.touchSlop)) {
                                cancelled = true
                                candidate = -1
                                panel.clear()
                            } else if (panel.slideSelection && !cancelled) {
                                val index = panel.hit(root[0], lastPoint).takeIf { panel.valid(it) } ?: -1
                                if (index != candidate) {
                                    candidate = index
                                    if (index >= 0) {
                                        panel.attach(index)
                                        haptics.granularShort()
                                    } else {
                                        panel.clear()
                                    }
                                }
                            }
                            if (panel.slideSelection || clickBlocked) change.consume()
                        } else if (clickBlocked) {
                            change.consume()
                        }
                    }
                } finally {
                    cancelDetection()
                    dismissJob?.cancel()
                    main.motion.up()
                    sub.motion.up()
                    main.clear()
                    sub.clear()
                }
            }
        }
    }
}
