
package io.github.wfc35286.coui.kmp.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CouiPopupTransitionDriverTest {

    private fun fixture(small: Boolean = true): Pair<CouiPopupLocateHelper, CouiPopupTransitionDriver> {
        val h = CouiPopupLocateHelper()
        h.minGapBetweenMainAndSubMenu = 12
        h.menuInnerPaddingVertical = 8
        h.smallScreen = small
        val d = CouiPopupTransitionDriver(h)
        d.smallScreen = small
        d.menuInnerPaddingVertical = 8
        d.minGapBetweenMainAndSubMenu = 12
        return h to d
    }

    private fun fillDomain(h: CouiPopupLocateHelper) {
        h.domain.window.set(0, 0, 1080, 2000)
        h.domain.mainMenu.set(100, 400, 700, 1200)
        h.domain.mainMenuRelocated.set(100, 400, 640, 1200)
        h.domain.subMenu.set(100, 600, 700, 1000)
        h.setAnchorRow(rowTopInList = 120, rowHeight = 120, scrollY = 0)
    }

    private fun CouiPopupTransitionDriver.runUntilSettled(maxFrames: Int = 900): Int {
        var frames = 0
        while (frames < maxFrames && advance(1f / 60f)) frames++
        return frames
    }

    @Test
    fun `compact enter dims the main menu to 0_3 and keeps the submenu opaque`() {
        val (h, d) = fixture(small = true)
        d.show(2)
        assertEquals(CouiPopupTransitionDriver.Phase.Measuring, d.phase)
        fillDomain(h)
        d.measured()
        assertEquals(CouiPopupTransitionDriver.Phase.Entering, d.phase)
        d.runUntilSettled()
        assertEquals(CouiPopupTransitionDriver.Phase.Shown, d.phase)
        assertEquals(2, d.index)
        assertEquals(0.3f, d.mainContentAlpha, 0.01f)
        assertEquals(0.9f, d.mainScaleX, 0.01f)
        assertEquals(1f, d.subScaleX, 0.0001f)
        assertEquals(1f, d.subAlpha, 0.0001f)
        assertTrue(d.revealActive, "the compact branch must reveal through setOverrideOutline")
        assertEquals(1f, d.outlineAlpha, 0.0001f)
        assertEquals(0f, d.mainPivotX, 0.0001f)
        assertEquals(0f, d.mainPivotY, 0.0001f)
    }

    @Test
    fun `default enter scales the submenu from zero and dims to 0_7`() {
        val (h, d) = fixture(small = false)
        d.show(0)
        fillDomain(h)
        d.measured()
        assertEquals(0f, d.subScaleX, 0.0001f)
        assertEquals(0f, d.subAlpha, 0.0001f)
        d.runUntilSettled()
        assertEquals(0.9f, d.mainScaleX, 0.01f)
        assertEquals(0.7f, d.mainContentAlpha, 0.01f)
        assertEquals(1f, d.subScaleX, 0.0001f)
        assertEquals(1f, d.subAlpha, 0.0001f)
        assertTrue(!d.revealActive, "the side panel is not clipped")
        assertEquals(0.5f, d.mainPivotX, 0.0001f)
        assertEquals(0.5f, d.mainPivotY, 0.0001f)
    }

    @Test
    fun `exit collapses the submenu back onto its row and clears the state`() {
        val (h, d) = fixture(small = true)
        d.show(1)
        fillDomain(h)
        d.measured()
        d.runUntilSettled()
        d.hide()
        assertEquals(CouiPopupTransitionDriver.Phase.Exiting, d.phase)
        d.runUntilSettled()
        assertEquals(CouiPopupTransitionDriver.Phase.Hidden, d.phase)
        assertEquals(-1, d.index)
        assertEquals(0f, d.subTranslationY, 0.0001f)
        assertEquals(1f, d.mainScaleX, 0.0001f)
        assertEquals(1f, d.mainContentAlpha, 0.0001f)
    }

    @Test
    fun `switching rows hides first and shows the queued one`() {
        val (h, d) = fixture(small = true)
        d.show(0)
        fillDomain(h)
        d.measured()
        d.runUntilSettled()
        d.show(3)
        d.runUntilSettled()
        assertEquals(CouiPopupTransitionDriver.Phase.Measuring, d.phase)
        d.measured()
        d.runUntilSettled()
        assertEquals(3, d.index)
        assertEquals(CouiPopupTransitionDriver.Phase.Shown, d.phase)
    }

    @Test
    fun `compact start y is the anchor row minus the inner padding`() {
        val (h, d) = fixture(small = true)
        d.show(0)
        fillDomain(h)
        d.measured()
        assertEquals(-88f, d.smallController.subMenuTranslateStartY)
        assertEquals(0f, d.smallController.subMenuTranslateEndY)
        d.runUntilSettled()
        assertEquals(0f, d.subTranslationY, 0.0001f)
    }

    @Test
    fun `a reset exit arms both one-shot dispatch flags`() {
        val (h, d) = fixture(small = true)
        d.show(0)
        fillDomain(h)
        d.measured()
        d.runUntilSettled()
        d.hide()
        assertTrue(d.pendingMainMenuTouchEventRestore)
        d.runUntilSettled()
        assertTrue(d.needReDispatchDownForNextEvent)
        assertTrue(d.consumeMainMenuTouchRestore())
        assertTrue(!d.consumeMainMenuTouchRestore())
        assertTrue(d.consumeReDispatchDown())
        assertTrue(!d.consumeReDispatchDown())
    }

    @Test
    fun `switching rows does not arm the main-list restore flag`() {
        val (h, d) = fixture(small = true)
        d.show(0)
        fillDomain(h)
        d.measured()
        d.runUntilSettled()
        d.show(2)
        assertTrue(!d.pendingMainMenuTouchEventRestore)
    }
}