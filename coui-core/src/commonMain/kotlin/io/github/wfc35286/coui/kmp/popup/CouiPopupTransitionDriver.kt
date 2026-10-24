// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


internal class CouiPopupTransitionDriver(
    val locate: CouiPopupLocateHelper,
) {
    enum class Phase { Hidden, Measuring, Entering, Shown, Exiting, Dismissing }

    val domain: CouiPopupDomain get() = locate.domain

    val smallController = CouiPopupSmallScreenController(locate)
    val defaultController = CouiPopupDefaultScreenController(locate)

    private val menuUnit = CouiPopupMenuTransitionUnit()
    private val subUnit = CouiPopupSubMenuTransitionUnit()

    var index by mutableIntStateOf(-1)
        private set

    var phase by mutableStateOf(Phase.Hidden)
        private set

    var smallScreen = true

    var menuInnerPaddingVertical = 0

    var minGapBetweenMainAndSubMenu = 0

    var anchorGlobalTop: () -> Int = { domain.subMenuAnchor.top }
    var mainMenuViewTranslationY: () -> Float = { mainTranslationY }

    var requestFrame: () -> Unit = {}

    val mainProgress: Float get() = (menuUnit.currentProgressValue() / PROGRESS_SCALE).coerceIn(0f, 1f)
    val subProgress: Float get() = (subUnit.currentProgressValue() / PROGRESS_SCALE).coerceIn(0f, 1f)
    val visible: Boolean get() = index >= 0
    val entered: Boolean get() = phase == Phase.Shown
    val opening: Boolean get() = phase == Phase.Measuring || phase == Phase.Entering || entered

    var mainScaleX by mutableFloatStateOf(1f)
        private set
    var mainScaleY by mutableFloatStateOf(1f)
        private set
    var mainTranslationY by mutableFloatStateOf(0f)
        private set
    var mainContentAlpha by mutableFloatStateOf(1f)
        private set
    var mainPivotX by mutableFloatStateOf(0.5f)
        private set
    var mainPivotY by mutableFloatStateOf(0.5f)
        private set

    var subScaleX by mutableFloatStateOf(1f)
        private set
    var subScaleY by mutableFloatStateOf(1f)
        private set
    var subPivotX by mutableFloatStateOf(0.5f)
        private set
    var subPivotY by mutableFloatStateOf(0.5f)
        private set
    var subAlpha by mutableFloatStateOf(0f)
        private set
    var subTranslationY by mutableFloatStateOf(0f)
        private set

    var revealTop by mutableFloatStateOf(0f)
        private set
    var revealHeight by mutableFloatStateOf(0f)
        private set
    var revealActive by mutableStateOf(false)
        private set
    var outlineAlpha by mutableFloatStateOf(0f)
        private set

    private var pendingIndex = -1
    private var resumePhase = Phase.Hidden

    var pendingMainMenuTouchEventRestore = false

    var onResetSubMenuScroll: () -> Unit = {}

    var needReDispatchDownForNextEvent = false

    fun consumeMainMenuTouchRestore(): Boolean {
        if (!pendingMainMenuTouchEventRestore) return false
        pendingMainMenuTouchEventRestore = false
        return true
    }

    fun consumeReDispatchDown(): Boolean {
        if (!needReDispatchDownForNextEvent) return false
        needReDispatchDownForNextEvent = false
        return true
    }

    fun show(position: Int) {
        if (position < 0 || phase == Phase.Dismissing) return
        if (visible && position != index) {
            pendingIndex = position
            hide(resetScroll = false, animate = false)
            return
        }
        if (index == position && opening) {
            phase = Phase.Entering
            startEnter()
            return
        }
        if (!visible) {
            index = position
            phase = Phase.Measuring
            pendingEnter = false
            subMenuPreviewRest = -1f
            menuPreviewRest = -1f
            seedRevealForEnter()
        } else {
            phase = Phase.Entering
            startEnter()
        }
    }

    fun measured() {
        if (phase != Phase.Measuring || domain.mainMenu.isEmpty() ||
            domain.subMenu.isEmpty() || domain.subMenuAnchor.isEmpty()
        ) {
            return
        }
        // The sub menu is laid out while still clipped to the anchor-row prototype outline, and
        // only after that does the reveal spring start. The view is measured here, but the reveal
        // must wait for the platform material to actually produce pixels ([materialReady]): the
        // blur drawable emits nothing for its first frames, and starting the reveal on the
        // drawable's own first frame left the panel transparent for two frames - the page showed
        // through exactly where the sub menu was about to appear. The wait is invisible: with a
        // zero-height reveal slice the panel paints nothing at all.
        if (pendingEnter) return
        pendingEnter = true
        requestFrame()
    }

    /**
     * Whether the platform material of the panel being revealed has produced a frame yet. Wired by
     * the composition to `CouiPopupVisualState.materialLive`; defaults to true so a host without the
     * Android material view never stalls.
     */
    var materialReady: () -> Boolean = { true }

    /** Rest values captured when a predictive-back preview of the sub menu's close starts. */
    private var subMenuPreviewRest = -1f
    private var menuPreviewRest = -1f

    private var pendingEnter = false
    private var pendingEnterFrames = 0

    fun hide(resetScroll: Boolean = true, animate: Boolean = true) {
        if (!visible || phase == Phase.Dismissing) return
        if (phase == Phase.Exiting) {
            if (!animate) {
                menuUnit.skipToEnd()
                subUnit.skipToEnd()
                requestFrame()
            }
            return
        }
        if (phase == Phase.Measuring) {
            finishExit()
            return
        }
        if (resetScroll) {
            if (entered) pendingMainMenuTouchEventRestore = true
            onResetSubMenuScroll()
        }
        phase = Phase.Exiting
        if (smallScreen) {
            smallController.minGapBetweenMainAndSubMenu = minGapBetweenMainAndSubMenu
            smallController.menuInnerPaddingVertical = menuInnerPaddingVertical
            smallController.startSubMenuExit()
            subUnit.updateTranslateYTargets(smallController.subMenuTranslateStartY, smallController.subMenuTranslateEndY)
        }
        menuUnit.animateToSubMenuExitState(SUB_MENU_BOUNCE, enterResponse)
        subUnit.setStartValue(subUnit.currentProgressValue())
        subUnit.animateToExitState(SUB_MENU_BOUNCE, enterResponse)
        if (!animate) {
            menuUnit.skipToEnd()
            subUnit.skipToEnd()
            updateFrame()
        }
        requestFrame()
    }

    fun freeze() {
        if (!visible || phase == Phase.Dismissing) return
        resumePhase = phase
        phase = Phase.Dismissing
    }

    fun resume() {
        if (phase != Phase.Dismissing) return
        phase = resumePhase
        requestFrame()
    }

    private fun seedRevealForEnter() {
        outlineAlpha = 0f
        subScaleX = 1f
        subScaleY = 1f
        subAlpha = 0f
        subTranslationY = 0f
        revealActive = smallScreen
        revealTop = 0f
        revealHeight = 0f
    }

    /**
     * Establishes the anchor-row sized prototype reveal outline at full alpha *before* the enter
     * spring starts, so the first rendered frame is already the row-sized sliver rather than an
     * empty rectangle.
     */
    private fun applyPrototypeOutline() {
        if (!smallScreen) return
        revealActive = true
        revealTop = smallController.subMenuClippedTopStart.toFloat()
        revealHeight = smallController.subMenuClippedBottomStart.toFloat()
        outlineAlpha = 1f
    }

    /**
     * The sub menu grows out of the edge that faces the main menu, not out of its centre: the
     * pivot is 0 when the sub menu sits to the right of the main menu and 1 otherwise.
     */
    private fun subMenuEnterPivotX(): Float {
        val width = domain.subMenu.width()
        if (width <= 0) return 0.5f
        return if (domain.subMenu.left > domain.mainMenu.left) 0f else 1f
    }

    /**
     * The pivot is the anchor row's centre expressed in the sub menu's own coordinates,
     * normalised to 0..1.
     */
    private fun subMenuEnterPivotY(): Float {
        val height = domain.subMenu.height()
        if (height <= 0) return 0.5f
        val local = domain.subMenuAnchor.centerY() - domain.subMenu.top
        return (local.toFloat() / height).coerceIn(0f, 1f)
    }

    private fun startEnter() {
        if (smallScreen) {
            smallController.minGapBetweenMainAndSubMenu = minGapBetweenMainAndSubMenu
            smallController.menuInnerPaddingVertical = menuInnerPaddingVertical
            smallController.configMainMenuAnimationPropertiesForSubMenuEnter()
            smallController.configSubMenuAnimationPropertiesForSubMenuEnter()
            applyPrototypeOutline()
            smallController.setSubMenuView()
            smallController.setMainMenuSubTranslation(smallController.computeMainMenuTargetTranslationY())
            subUnit.configForSmallScreenEnter(
                yStart = smallController.subMenuTranslateStartY + menuUnit.currentTranslationY,
                yEnd = smallController.subMenuTranslateEndY,
                alphaStart = 1f,
                alphaEnd = 1f,
            )
            menuUnit.animateToSubMenuEnterState(
                scaleX = smallController.mainMenuScalePercent,
                scaleY = smallController.mainMenuScalePercent,
                translationY = smallController.mainMenuSubTranslation,
                alpha = smallController.mainMenuAlphaPercent,
                bounce = SUB_MENU_BOUNCE,
                response = enterResponse,
            )
        } else {
            defaultController.configMainMenuAnimationPropertiesForSubMenuEnter()
            defaultController.startMainMenuSubMenuEnter(menuUnit)
            defaultController.startSubMenuEnter(subUnit)
        }
        subUnit.setStartValue(subUnit.currentProgressValue())
        subUnit.animateToEnterState(SUB_MENU_BOUNCE, enterResponse)
        updateFrame()
        requestFrame()
    }

    fun advance(dt: Float): Boolean {
        if (phase == Phase.Hidden) {
            // The main menu's own spring may still be settling after the sub menu left; it keeps
            // running instead of snapping (see [finishExit]).
            if (!menuUnit.isRunning()) return false
            val moving = menuUnit.advance(dt)
            mainScaleX = menuUnit.currentScaleX
            mainScaleY = menuUnit.currentScaleY
            mainTranslationY = menuUnit.currentTranslationY
            mainContentAlpha = menuUnit.currentContentAlpha
            return moving
        }
        if (phase == Phase.Measuring) {
            if (!pendingEnter) return false
            // Wait for the platform material, but never longer than a bounded number of frames: if
            // the material view never reports progress (it was never drawn), the sub menu must still
            // open instead of stalling the menu.
            if (!materialReady() && pendingEnterFrames < MAX_MATERIAL_WAIT_FRAMES) {
                pendingEnterFrames++
                return true
            }
            pendingEnter = false
            pendingEnterFrames = 0
            phase = Phase.Entering
            startEnter()
            return true
        }
        if (phase == Phase.Dismissing) return false
        val movingMenu = menuUnit.advance(dt)
        val movingSub = subUnit.advance(dt)
        // Land the main menu exactly on its resting transform while the sub menu is still moving.
        // Reaching it at the natural end would put the text rasterisation switch on the first
        // still frame, where the sub menu owner's row would visibly step 1px.
        if (menuUnit.nearTarget(MAIN_SETTLE_EPSILON)) menuUnit.skipToEnd()
        updateFrame()
        if (!movingSub) {
            if (subUnit.currentProgressValue() <= 0f) finishExit() else phase = Phase.Shown
        }
        return movingMenu || movingSub
    }

    private fun updateFrame() {
        mainScaleX = menuUnit.currentScaleX
        mainScaleY = menuUnit.currentScaleY
        mainTranslationY = menuUnit.currentTranslationY
        mainContentAlpha = menuUnit.currentContentAlpha
        if (smallScreen) {
            val frame = smallController.onSubMenuProgressUpdate(
                progress = subProgress,
                anchorGlobalTop = { anchorGlobalTop() },
                mainMenuTranslationY = { mainMenuViewTranslationY() },
            )
            if (frame.translateY != subUnit.currentTranslateY && smallController.dynamicAnchorChanged) {
                subUnit.updateTranslateYTargets(frame.translateY, frame.translateYEnd)
            }
            revealTop = frame.revealTop.toFloat()
            revealHeight = (frame.revealBottom - frame.revealTop).toFloat()
            revealActive = true
            outlineAlpha = frame.outlineAlpha
            subScaleX = 1f
            subScaleY = 1f
            subPivotX = subMenuEnterPivotX()
            subPivotY = subMenuEnterPivotY()
            subAlpha = 1f
            subTranslationY = subUnit.currentTranslateY
            val width = domain.mainMenu.width()
            mainPivotX = if (width > 0) smallController.mainMenuEnterPivotX().toFloat() / width else 0.5f
            mainPivotY = 0f
        } else {
            revealActive = false
            revealTop = 0f
            revealHeight = domain.subMenu.height().toFloat()
            outlineAlpha = 1f
            subScaleX = subUnit.currentScaleX
            subScaleY = subUnit.currentScaleY
            subAlpha = subUnit.currentAlpha
            subTranslationY = 0f
            subPivotX = subMenuEnterPivotX()
            subPivotY = subMenuEnterPivotY()
            mainPivotX = 0.5f
            mainPivotY = 0.5f
        }
    }

    /**
     * Predictive back, sub menu open: park both transition units at a *fraction* of their open value
     * instead of animating them, so the panel follows the gesture with the same geometry a
     * [hide] would produce (the sub menu's reveal slice collapses back into the anchor row, its
     * translate and alpha follow, and the main menu grows back out of its 90% scale).
     */
    fun previewSubMenuClose(open: Float) {
        if (phase != Phase.Shown && phase != Phase.Entering) return
        if (subMenuPreviewRest < 0f) {
            subMenuPreviewRest = subUnit.currentProgressValue()
            menuPreviewRest = menuUnit.currentProgressValue()
        }
        val fraction = open.coerceIn(0f, 1f)
        subUnit.cancel()
        menuUnit.cancel()
        subUnit.setStartValue(subMenuPreviewRest * fraction)
        menuUnit.setStartValue(menuPreviewRest * fraction)
        updateFrame()
    }

    /** Undo [previewSubMenuClose] by springing both units back to the value they had before it. */
    fun endSubMenuPreview() {
        val subRest = subMenuPreviewRest
        val menuRest = menuPreviewRest
        subMenuPreviewRest = -1f
        menuPreviewRest = -1f
        if (subRest < 0f) return
        subUnit.startAnimation(SUB_MENU_BOUNCE, enterResponse, subRest)
        menuUnit.startAnimation(SUB_MENU_BOUNCE, enterResponse, menuRest)
        requestFrame()
    }

    private fun finishExit() {
        pendingEnter = false
        subMenuPreviewRest = -1f
        menuPreviewRest = -1f
        // Only the sub menu's own state is torn down here; the main menu's spring is deliberately
        // left running. Hard resetting it snapped the main menu's scale/alpha/translation back to
        // their resting values in a single frame while the spring was still a hair away from the
        // target, showing up as a very subtle pixel shift in the item text the moment the
        // animation ended. Keep it running instead (see [advance]).
        subUnit.reset(keepValue = false)
        if (smallScreen) smallController.onExitFinished()
        index = -1
        phase = Phase.Hidden
        needReDispatchDownForNextEvent = true
        subScaleX = 1f
        subScaleY = 1f
        subAlpha = 0f
        subTranslationY = 0f
        // The platform removes the sub menu view inside its exit callback. Compose
        // removal is deferred: keep its old layer empty until that removal is applied.
        revealActive = true
        revealTop = 0f
        revealHeight = 0f
        outlineAlpha = 0f
        if (pendingIndex >= 0) {
            val next = pendingIndex
            pendingIndex = -1
            show(next)
        }
    }

    private companion object {
        const val PROGRESS_SCALE = 10000f

        /**
         * Upper bound on how long the reveal waits for the platform material to produce its first
         * frame. The warm-up is typically two drawn frames; the cap only exists so a material view
         * that is never drawn cannot stall the menu.
         */
        const val MAX_MATERIAL_WAIT_FRAMES = 8

        const val SUB_MENU_BOUNCE = 0f
        const val SUB_MENU_RESPONSE = 0.35f

        /**
         * How close the main menu's spring must be to its target before it lands exactly on it.
         * 0.2% of the full progress: small enough to be invisible, large enough that the landing
         * happens several frames before the sub menu's own spring stops.
         */
        const val MAIN_SETTLE_EPSILON = 0.002f
    }

    private val enterResponse: Float
        get() = if (CouiPopupDebug32.slowMotion) SUB_MENU_RESPONSE * 5f else SUB_MENU_RESPONSE
}
