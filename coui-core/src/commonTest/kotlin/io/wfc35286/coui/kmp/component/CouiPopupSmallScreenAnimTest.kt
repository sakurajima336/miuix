
package io.wfc35286.coui.kmp.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CouiPopupSmallScreenAnimTest {

    private fun fixture(): Pair<CouiPopupLocateHelper, CouiPopupSmallScreenController> {
        val h = CouiPopupLocateHelper()
        h.minGapBetweenMainAndSubMenu = 12
        h.menuInnerPaddingVertical = 8
        h.smallScreen = true
        val c = CouiPopupSmallScreenController(h)
        c.minGapBetweenMainAndSubMenu = 12
        c.menuInnerPaddingVertical = 8
        return h to c
    }

    @Test
    fun `reveal window starts one row tall and ends full height`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 100, 400, 900)      // 800 tall
        h.domain.subMenu.set(0, 400, 400, 1000)      // 600 tall
        h.setAnchorRow(rowTopInList = 2 * 120, rowHeight = 120, scrollY = 0)
        c.configSubMenuAnimationPropertiesForSubMenuEnter()
        assertEquals(8, c.subMenuClippedTopStart)
        assertEquals(0, c.subMenuClippedTopEnd)
        assertEquals(120, c.subMenuClippedBottomStart)
        assertEquals(600, c.subMenuClippedBottomEnd)

        val at0 = c.onSubMenuProgressUpdate(0f)
        assertEquals(8, at0.revealTop)
        assertEquals(128, at0.revealBottom)

        val at1 = c.onSubMenuProgressUpdate(1f)
        assertEquals(0, at1.revealTop)
        assertEquals(600, at1.revealBottom)
    }

    @Test
    fun `reveal grows monotonically so the unfold never jumps`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.subMenu.set(0, 400, 400, 1000)
        h.setAnchorRow(rowTopInList = 240, rowHeight = 120, scrollY = 0)
        c.configSubMenuAnimationPropertiesForSubMenuEnter()
        var previousHeight = -1
        var previousTop = Int.MAX_VALUE
        for (step in 0..10) {
            val frame = c.onSubMenuProgressUpdate(step / 10f)
            val height = frame.revealBottom - frame.revealTop
            assertTrue(height >= previousHeight, "height must not shrink: $height < $previousHeight")
            assertTrue(frame.revealTop <= previousTop, "top must not move down: ${frame.revealTop} > $previousTop")
            previousHeight = height
            previousTop = frame.revealTop
        }
    }

    @Test
    fun `bottom start clamps to the space below the anchor`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 300)
        h.domain.subMenu.set(0, 200, 400, 800)
        h.setAnchorRow(rowTopInList = 240, rowHeight = 120, scrollY = 0)
        c.configSubMenuAnimationPropertiesForSubMenuEnter()
        assertEquals(60, c.subMenuClippedBottomStart)
    }

    @Test
    fun `submenu translate start is anchored to the clicked row`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.subMenu.set(0, 400, 400, 1000)
        h.setAnchorRow(rowTopInList = 480, rowHeight = 120, scrollY = 0)
        c.setSubMenuView()
        assertEquals(72f, c.subMenuTranslateStartY)
        assertEquals(0f, c.subMenuTranslateEndY)
    }

    @Test
    fun `main menu target translation keeps the twelve dp gap`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 100, 400, 900)      // top 100
        h.domain.subMenu.set(0, 400, 400, 1000)      // top 400
        c.setSubMenuView()
        assertEquals(0f, c.computeMainMenuTargetTranslationY())
    }

    @Test
    fun `main menu is pushed when the submenu would overlap it`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 100, 400, 900)
        h.domain.subMenu.set(0, 105, 400, 700)
        c.setSubMenuView()
        assertEquals(-7f, c.computeMainMenuTargetTranslationY())
    }

    @Test
    fun `clamp never pushes the menu above the window barrier`() {
        val (h, c) = fixture()
        h.domain.window.set(0, 0, 1080, 2000)
        h.domain.windowBarriers.set(0, 50, 0, 0)
        h.domain.mainMenu.set(0, 100, 400, 900)
        assertEquals(-50f, c.clampMainMenuTranslationY(-200f))
        assertEquals(-20f, c.clampMainMenuTranslationY(-20f))
    }

    @Test
    fun `dynamic anchor applies the smali translation offset`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 100, 400, 900)
        h.domain.subMenu.set(0, 400, 400, 1000)
        h.setAnchorRow(rowTopInList = 240, rowHeight = 120, scrollY = 0)
        assertEquals(-28f, c.dynamicSubMenuTranslateStartY(mainMenuTranslationY = -40f))
        assertEquals(-68f, c.dynamicSubMenuTranslateStartY())
    }

    @Test
    fun `exit recomputes the start so the submenu collapses onto its row`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 100, 400, 900)
        h.domain.subMenu.set(0, 400, 400, 1000)
        h.setAnchorRow(rowTopInList = 240, rowHeight = 120, scrollY = 0)
        c.onSubMenuProgressUpdate(0.5f)
        c.startSubMenuExit()
        assertEquals(-68f, c.subMenuTranslateStartY)
    }

    @Test
    fun `exit finish clears the main menu push`() {
        val (_, c) = fixture()
        c.setMainMenuSubTranslation(-24f)
        c.onExitFinished()
        assertEquals(0f, c.mainMenuSubTranslation)
    }

    @Test
    fun `menu scale comes from the relocated ratio`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.mainMenuRelocated.set(0, 0, 360, 800)   // 400 -> 360, ratio 0.9
        c.configMainMenuAnimationPropertiesForSubMenuEnter()
        assertEquals(0.3f, c.mainMenuAlphaPercent)
        assertEquals(0.9f, c.mainMenuScalePercent, 0.0001f)
    }

    @Test
    fun `pivot keeps the edge that does not move`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)

        h.domain.mainMenuRelocated.set(0, 0, 360, 800)          // left unchanged
        assertEquals(0, c.mainMenuEnterPivotX())

        h.domain.mainMenuRelocated.set(40, 0, 400, 800)         // right unchanged
        assertEquals(400, c.mainMenuEnterPivotX())

        h.domain.mainMenuRelocated.set(20, 0, 380, 800)         // both moved
        assertEquals(200, c.mainMenuEnterPivotX())

        assertEquals(0, c.mainMenuEnterPivotY())
    }
}