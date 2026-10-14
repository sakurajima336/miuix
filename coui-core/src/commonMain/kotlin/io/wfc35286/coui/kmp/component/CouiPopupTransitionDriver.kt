
package io.wfc35286.coui.kmp.component

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
            seedRevealForEnter()
        } else {
            phase = Phase.Entering
            startEnter()
        }
    }

    fun measured() {
        if (phase != Phase.Measuring || domain.mainMenu.isEmpty() ||
            domain.subMenu.isEmpty() || domain.subMenuAnchor.isEmpty()
        ) return
        phase = Phase.Entering
        startEnter()
    }

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
     * Official `SmallScreenAnimationController.configSubMenuAnimationPropertiesForSubMenuEnter`
     * ends with
     * `setOverrideOutline(0, mSubMenuClippedTopStart, subMenu.width,
     *  mSubMenuClippedTopStart + mSubMenuClippedBottomStart, 1.0f)`.
     *
     * That is the anchor-row sized prototype outline, established at full alpha *before* the
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
     * Official `PopupMenuDomain.getSubMenuEnterPivotX()`:
     * `if (mSubMenu.left > mMainMenu.left) return 0; return mSubMenu.width();`
     * i.e. the sub menu grows out of the edge that faces the main menu, not out of its centre.
     */
    private fun subMenuEnterPivotX(): Float {
        val width = domain.subMenu.width()
        if (width <= 0) return 0.5f
        return if (domain.subMenu.left > domain.mainMenu.left) 0f else 1f
    }

    /**
     * Official `PopupMenuDomain.getSubMenuEnterPivotY()`:
     * `mSubMenuAnchor.centerY() - mSubMenu.top`, converted to our 0..1 pivot.
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
            // The main menu's own spring may still be settling after the sub menu left; the official
            // keeps `mMenuTransitionUnit` running instead of snapping it (see `finishExit`).
            if (!menuUnit.isRunning()) return false
            val moving = menuUnit.advance(dt)
            mainScaleX = menuUnit.currentScaleX
            mainScaleY = menuUnit.currentScaleY
            mainTranslationY = menuUnit.currentTranslationY
            mainContentAlpha = menuUnit.currentContentAlpha
            return moving
        }
        if (phase == Phase.Measuring || phase == Phase.Dismissing) return false
        val movingMenu = menuUnit.advance(dt)
        val movingSub = subUnit.advance(dt)
        // Land the main menu exactly on its resting transform while the sub menu is still moving.
        // Reaching it at the natural end instead put the text rasterisation switch on the first
        // still frame, where the sub menu owner's row visibly stepped 1px (measured in a 60fps
        // screen recording, band top 406 -> 407 and staying there).
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

    private fun finishExit() {
        // Official `COUIPopupMenuRootView$1.onSubMenuExited` only tears the sub menu view down
        // (`removeSubMenuView`) and runs the pending show; nothing resets the main menu's own
        // `mMenuTransitionUnit` spring there. Hard resetting it here snapped the main menu's
        // scale/alpha/translation back to their resting values in a single frame while its spring
        // was still a hair away from the target - that is the "very subtle pixel shift" the item
        // text showed the moment the animation ended. Keep it running instead (see `advance`).
        subUnit.reset(keepValue = false)
        if (smallScreen) smallController.onExitFinished()
        index = -1
        phase = Phase.Hidden
        needReDispatchDownForNextEvent = true
        subScaleX = 1f
        subScaleY = 1f
        subAlpha = 0f
        subTranslationY = 0f
        // Official onSubMenuExited removes the view in the animation callback. Compose
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

        const val SUB_MENU_BOUNCE = 0f
        const val SUB_MENU_RESPONSE = 0.35f

        /**
         * How close the main menu's spring must be to its target before it lands exactly on it.
         * 0.2% of the full progress: small enough to be invisible, large enough that the landing
         * happens several frames before the sub menu's own spring stops (measured: the sub menu is
         * still sliding out at that point).
         */
        const val MAIN_SETTLE_EPSILON = 0.002f
    }

    private val enterResponse: Float
        get() = if (CouiPopupDebug32.slowMotion) SUB_MENU_RESPONSE * 5f else SUB_MENU_RESPONSE
}
