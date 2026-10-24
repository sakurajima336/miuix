// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CouiPopupRootViewStateTest {

    @Test
    fun `controller selection follows the six hundred dp width breakpoint`() {
        val s = CouiPopupRootViewState()
        s.selectController(413f)            // a phone in portrait
        assertTrue(s.controllerIsSmallScreen)

        s.selectController(599.9f)
        assertTrue(s.controllerIsSmallScreen)

        s.selectController(600f)            // exactly at the breakpoint -> NOT compact
        assertFalse(s.controllerIsSmallScreen)

        s.selectController(924f)            // the same phone in landscape
        assertFalse(s.controllerIsSmallScreen)
    }

    @Test
    fun `hit tests use the domain rects`() {
        val s = CouiPopupRootViewState()
        s.domain.mainMenu.set(100, 200, 500, 800)
        s.domain.subMenu.set(100, 810, 500, 1400)

        assertTrue(s.isPointInMainMenu(300f, 400f))
        assertTrue(s.isPointInMainMenu(100f, 200f))     // inclusive edges
        assertFalse(s.isPointInMainMenu(99f, 400f))
        assertFalse(s.isPointInMainMenu(300f, 801f))

        assertTrue(s.isPointInSubMenu(300f, 900f))
        assertFalse(s.isPointInSubMenu(300f, 800f))
    }

    @Test
    fun `submenu region test respects attachment`() {
        val s = CouiPopupRootViewState()
        s.domain.subMenu.set(0, 0, 100, 100)
        assertTrue(s.isTouchPointInSubMenuRegion(50f, 50f, attached = true))
        assertFalse(s.isTouchPointInSubMenuRegion(50f, 50f, attached = false))
    }

    @Test
    fun `transform subtracts the submenu origin`() {
        val s = CouiPopupRootViewState()
        s.domain.subMenu.set(100, 200, 500, 900)
        val out = s.transformToSubMenuCoords(150f, 260f)
        assertEquals(50f, out[0])
        assertEquals(60f, out[1])
    }

    @Test
    fun `hiding with reset scroll marks the touch restore only after entering`() {
        val s = CouiPopupRootViewState()
        s.onHideSubMenu(resetScroll = true)
        assertFalse(s.pendingMainMenuTouchEventRestore, "nothing to restore before the submenu entered")

        s.subMenuEntered = true
        s.onHideSubMenu(resetScroll = true)
        assertTrue(s.pendingMainMenuTouchEventRestore)
    }

    @Test
    fun `restore flag is consumed on up only`() {
        val s = CouiPopupRootViewState()
        s.hideSubMenuFromLongPress()
        assertTrue(s.pendingMainMenuTouchEventRestore)

        assertFalse(s.restoreMainMenuTouchAfterLongPressOnUp(2))
        assertTrue(s.pendingMainMenuTouchEventRestore)

        assertTrue(s.restoreMainMenuTouchAfterLongPressOnUp(1))
        assertFalse(s.pendingMainMenuTouchEventRestore)
    }

    @Test
    fun `layout rects come straight from the domain`() {
        val s = CouiPopupRootViewState()
        s.domain.mainMenu.set(10, 20, 30, 40)
        s.domain.subMenu.set(50, 60, 70, 80)
        assertEquals(10, s.mainMenuLayoutRect().left)
        assertEquals(40, s.mainMenuLayoutRect().bottom)
        assertEquals(50, s.subMenuLayoutRect().left)
        assertEquals(80, s.subMenuLayoutRect().bottom)
    }

    @Test
    fun `constants match the smali`() {
        assertEquals(500L, CouiPopupRootViewState.MAIN_AREA_DISMISS_DELAY_MS)
        assertEquals(0.5f, CouiPopupRootViewState.CANCEL_EVENT_CENTER_FACTOR)
        assertEquals(0.2f, CouiPopupRootViewState.DEBUG_OVERLAY_MENU_ALPHA)
        assertEquals(600f, CouiPopupRootViewState.BP_MEDIUM_WINDOW_BASE_WIDTH_DP)
        assertEquals(840f, CouiPopupRootViewState.BP_EXPANDED_WINDOW_BASE_WIDTH_DP)
        assertEquals(480f, CouiPopupRootViewState.BP_MEDIUM_WINDOW_BASE_HEIGHT_DP)
    }
}