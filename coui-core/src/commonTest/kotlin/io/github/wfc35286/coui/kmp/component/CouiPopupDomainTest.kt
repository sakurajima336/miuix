
package io.github.wfc35286.coui.kmp.component

import kotlin.test.Test
import kotlin.test.assertEquals

class CouiPopupDomainTest {

    @Test
    fun `enter pivot X keeps the edge that does not move`() {
        val d = CouiPopupDomain()
        d.mainMenu.set(100, 200, 500, 800)
        assertEquals(200, d.getMainMenuEnterPivotX())

        d.forcedHorizontalEdge = CouiPopupDomainConstants.FORCED_HORIZONTAL_START
        assertEquals(0, d.getMainMenuEnterPivotX())

        d.forcedHorizontalEdge = CouiPopupDomainConstants.FORCED_HORIZONTAL_END
        assertEquals(400, d.getMainMenuEnterPivotX())
    }

    @Test
    fun `center align uses the centre`() {
        val d = CouiPopupDomain()
        d.mainMenu.set(100, 200, 500, 800)
        d.mainMenuCenterAlign = true
        assertEquals(200, d.getMainMenuEnterPivotX())
        assertEquals(300, d.getMainMenuEnterPivotY())
    }

    @Test
    fun `auto corner pivot clamps the tangent point into the menu`() {
        val d = CouiPopupDomain()
        d.mainMenu.set(100, 200, 500, 800)
        d.pathAnchorMode = CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO
        d.lastPathAnchorX = 120f
        assertEquals(200, d.getMainMenuEnterPivotX())
    }

    @Test
    fun `popup side resolves from the anchor position`() {
        val d = CouiPopupDomain()
        d.mainMenu.set(0, 300, 400, 600)
        d.anchor.set(0, 0, 100, 100)
        assertEquals(CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_BELOW, d.getResolvedMainMenuPopupSide())
        d.anchor.set(0, 700, 100, 800)
        assertEquals(CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_ABOVE, d.getResolvedMainMenuPopupSide())
    }

    @Test
    fun `tangent coord picks the edge for the slot`() {
        val d = CouiPopupDomain()
        d.anchor.set(40, 10, 60, 90)
        d.mainMenu.set(0, 0, 400, 400)
        val start = FloatArray(2)
        d.computeMainMenuPopupPathAnchorOffset(0f, CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO, CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START, start)
        assertEquals(40f, start[0])
        val end = FloatArray(2)
        d.computeMainMenuPopupPathAnchorOffset(0f, CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO, CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END, end)
        assertEquals(60f, end[0])
        val center = FloatArray(2)
        d.computeMainMenuPopupPathAnchorOffset(0f, CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO, CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER, center)
        assertEquals(50f, center[0])
    }
}

class CouiPopupLocateHelperTest {

    private fun helper(): CouiPopupLocateHelper = CouiPopupLocateHelper().apply {
        horizontalOverlap = 14
        verticalOverlap = 14
        minGapBetweenMainAndSubMenu = 12
        mainMenuShrinkWidth = 16
        menuInnerPaddingVertical = 8
        smallScreen = true
    }

    @Test
    fun `relocate keeps the left edge when the anchor is on the left`() {
        val h = helper()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.anchor.set(20, 100, 120, 180)   // centreX 70 < 200
        h.relocateMainMenu()
        assertEquals(0, h.domain.mainMenuRelocated.left)
        assertEquals(360, h.domain.mainMenuRelocated.width())
    }

    @Test
    fun `relocate shifts everything when the anchor is on the right`() {
        val h = helper()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.anchor.set(280, 100, 380, 180)  // centreX 330 > 200
        h.relocateMainMenu()
        assertEquals(40, h.domain.mainMenuRelocated.left)
    }

    @Test
    fun `relocate splits the difference when the anchor is centred`() {
        val h = helper()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.domain.anchor.set(190, 100, 210, 180)  // centreX 200 == main centre
        h.relocateMainMenu()
        assertEquals(20, h.domain.mainMenuRelocated.left)
    }

    @Test
    fun `anchor row lands on the row that was clicked`() {
        val h = helper()
        h.domain.mainMenu.set(0, 0, 400, 800)
        h.setAnchorRow(rowTopInList = 5 * 120 + 8, rowHeight = 120, scrollY = 80)
        assertEquals(528, h.submenuAnchorBounds.top)
        assertEquals(648, h.submenuAnchorBounds.bottom)
    }

    @Test
    fun `anchor top is rescaled by the relocated height ratio`() {
        val h = helper()
        h.domain.mainMenu.set(0, 100, 400, 900)          // height 800
        h.domain.mainMenuRelocated.set(0, 100, 360, 820) // height 720
        h.setAnchorRow(rowTopInList = 408, rowHeight = 120, scrollY = 0)
        assertEquals(467, h.subMenuAnchorTopAfterMainMenuRelocated())
    }

    @Test
    fun `submenu X shares the main menu left edge on a small screen`() {
        val h = helper()
        h.domain.mainMenu.set(64, 0, 736, 1600)
        assertEquals(64, h.subMenuOffsetX(400))
    }
}
