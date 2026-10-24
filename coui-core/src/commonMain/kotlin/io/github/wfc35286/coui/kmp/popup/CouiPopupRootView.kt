// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup

internal class CouiPopupRootViewState {
    companion object {
        const val CANCEL_EVENT_CENTER_FACTOR = 0.5f

        const val MAIN_AREA_DISMISS_DELAY_MS = 500L

        const val DEBUG_OVERLAY_MENU_ALPHA = 0.2f

        const val BP_MEDIUM_WINDOW_BASE_WIDTH_DP = 600f

        const val BP_EXPANDED_WINDOW_BASE_WIDTH_DP = 840f

        const val BP_MEDIUM_WINDOW_BASE_HEIGHT_DP = 480f
    }

    val domain = CouiPopupDomain()

    var mainMenuWidth = 0
        private set
    var mainMenuHeight = 0
        private set
    var subMenuWidth = 0
        private set
    var subMenuHeight = 0
        private set

    var subMenuEntered = false

    var hasSentDownToSubMenu = false

    var needReDispatchDownForNextEvent = false

    var pendingMainMenuTouchEventRestore = false

    var controllerIsSmallScreen = true
        private set

    fun selectController(windowWidthDp: Float) {
        controllerIsSmallScreen = windowWidthDp < BP_MEDIUM_WINDOW_BASE_WIDTH_DP
    }

    fun mainMenuLayoutRect(): CouiPopupRect = domain.mainMenu
    fun subMenuLayoutRect(): CouiPopupRect = domain.subMenu

    fun isPointInMainMenu(x: Float, y: Float): Boolean {
        val r = domain.mainMenu
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }

    fun isPointInSubMenu(x: Float, y: Float): Boolean {
        val r = domain.subMenu
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }

    fun isTouchPointInSubMenuRegion(x: Float, y: Float, attached: Boolean = true): Boolean =
        attached && isPointInSubMenu(x, y)

    fun transformToSubMenuCoords(x: Float, y: Float, out: FloatArray = FloatArray(2)): FloatArray {
        val r = domain.subMenu
        out[0] = x - r.left
        out[1] = y - r.top
        return out
    }

    fun setMainMenuSize(width: Int, height: Int) {
        mainMenuWidth = width
        mainMenuHeight = height
    }

    fun setSubMenuSize(width: Int, height: Int) {
        subMenuWidth = width
        subMenuHeight = height
    }

    fun hideSubMenuFromLongPress() {
        pendingMainMenuTouchEventRestore = true
    }

    fun restoreMainMenuTouchAfterLongPressOnUp(action: Int): Boolean {
        if (action == 1 && pendingMainMenuTouchEventRestore) {
            pendingMainMenuTouchEventRestore = false
            return true
        }
        return false
    }

    fun onHideSubMenu(resetScroll: Boolean) {
        if (resetScroll && subMenuEntered) {
            pendingMainMenuTouchEventRestore = true
        }
    }

    fun debugInspectionAlpha(): Float = DEBUG_OVERLAY_MENU_ALPHA

}