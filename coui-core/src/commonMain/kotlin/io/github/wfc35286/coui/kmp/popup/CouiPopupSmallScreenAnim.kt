// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import kotlin.math.max
import kotlin.math.min


internal class CouiPopupSmallScreenController(
    private val helper: CouiPopupLocateHelper,
) {
    val domain: CouiPopupDomain get() = helper.domain

    var minGapBetweenMainAndSubMenu = 0

    var menuInnerPaddingVertical = 0

    var mainMenuAlphaPercent = 0.3f
        private set

    var mainMenuScalePercent = 0.9f
        private set

    var subMenuClippedTopStart = 0
        private set
    var subMenuClippedTopEnd = 0
        private set
    var subMenuClippedBottomStart = 0
        private set
    var subMenuClippedBottomEnd = 0
        private set

    var subMenuTranslateStartY = 0f
        private set
    var subMenuTranslateEndY = 0f
        private set

    private var lastRealProgress = 0f

    var dynamicAnchorChanged = false
        private set

    var mainMenuSubTranslation = 0f
        private set

    fun configMainMenuAnimationPropertiesForSubMenuEnter() {
        mainMenuAlphaPercent = 0.3f
        val mainMenu = domain.mainMenu
        val relocated = domain.mainMenuRelocated
        mainMenuScalePercent = if (mainMenu.width() > 0) {
            relocated.width().toFloat() / mainMenu.width().toFloat()
        } else {
            0.9f
        }
    }

    fun mainMenuEnterPivotX(): Int {
        val mainMenu = domain.mainMenu
        val relocated = domain.mainMenuRelocated
        return when {
            mainMenu.left == relocated.left -> 0
            mainMenu.right == relocated.right -> mainMenu.width()
            else -> mainMenu.width() / 2
        }
    }

    fun mainMenuEnterPivotY(): Int = 0

    fun configSubMenuAnimationPropertiesForSubMenuEnter() {
        subMenuClippedTopStart = menuInnerPaddingVertical
        subMenuClippedTopEnd = 0
        val anchor = domain.subMenuAnchor
        val mainMenu = domain.mainMenu
        subMenuClippedBottomStart = min(anchor.height(), mainMenu.bottom - anchor.top).coerceAtLeast(0)
        subMenuClippedBottomEnd = domain.subMenu.height()
    }

    fun onSubMenuProgressUpdate(
        progress: Float,
        anchorGlobalTop: () -> Int = { domain.subMenuAnchor.top },
        mainMenuTranslationY: () -> Float = { 0f },
    ): CouiPopupSubMenuFrame {
        dynamicAnchorChanged = false
        if (progress < lastRealProgress) {
            val top = anchorGlobalTop() - mainMenuTranslationY().toInt()
            val newStart = (top - domain.subMenu.top - menuInnerPaddingVertical).toFloat()
            if (newStart != subMenuTranslateStartY) {
                subMenuTranslateStartY = newStart
                dynamicAnchorChanged = true
            }
        }
        lastRealProgress = progress
        val revealTop = couiConvertedFraction(
            subMenuClippedTopStart.toFloat(),
            subMenuClippedTopEnd.toFloat(),
            progress,
        ).toInt()
        val revealHeight = couiConvertedFraction(
            subMenuClippedBottomStart.toFloat(),
            subMenuClippedBottomEnd.toFloat(),
            progress,
        ).toInt()
        return CouiPopupSubMenuFrame(
            revealTop = revealTop,
            revealBottom = revealTop + revealHeight,
            translateY = subMenuTranslateStartY,
            translateYEnd = subMenuTranslateEndY,
            outlineAlpha = progress,
        )
    }

    fun dynamicSubMenuTranslateStartY(mainMenuTranslationY: Float = 0f): Float {
        val anchorTop = helper.dynamicAnchorTop(mainMenuTranslationY)
        return (anchorTop - domain.subMenu.top - menuInnerPaddingVertical).toFloat()
    }

    fun setSubMenuView() {
        subMenuTranslateStartY = (
            domain.subMenuAnchor.top - domain.subMenu.top - menuInnerPaddingVertical
            ).toFloat()
        subMenuTranslateEndY = 0f
    }

    fun computeMainMenuTargetTranslationY(): Float {
        if (domain.subMenu.isEmpty()) return 0f
        val mainTop = domain.mainMenu.top
        val subTop = domain.subMenu.top
        val gap = minGapBetweenMainAndSubMenu
        val raw = when {
            mainTop + gap > subTop -> (subTop - gap) - mainTop

            mainTop + gap > subTop + subMenuTranslateEndY.toInt() ->
                (subTop + subMenuTranslateEndY.toInt()) - (mainTop + gap)

            else -> 0
        }
        return clampMainMenuTranslationY(raw.toFloat())
    }

    fun clampMainMenuTranslationY(value: Float): Float {
        val floor = (domain.window.top + domain.windowBarriers.top - domain.mainMenu.top).toFloat()
        return max(floor, value)
    }

    fun clampSubMenuTranslationY(value: Float): Float {
        if (domain.subMenu.isEmpty()) return value
        val floor = (domain.window.top + domain.windowBarriers.top - domain.subMenu.top).toFloat()
        return max(floor, value)
    }

    fun startSubMenuExit() {
        subMenuTranslateStartY = (
            domain.subMenuAnchor.top - domain.subMenu.top - menuInnerPaddingVertical
            ).toFloat()
    }

    fun onExitFinished() {
        lastRealProgress = 0f
        mainMenuSubTranslation = 0f
    }

    fun setMainMenuSubTranslation(value: Float) {
        mainMenuSubTranslation = value
    }
}

internal data class CouiPopupSubMenuFrame(
    val revealTop: Int,
    val revealBottom: Int,
    val translateY: Float,
    val translateYEnd: Float,
    val outlineAlpha: Float,
)
