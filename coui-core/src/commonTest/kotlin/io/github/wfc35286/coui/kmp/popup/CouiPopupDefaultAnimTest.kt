
package io.github.wfc35286.coui.kmp.component

import kotlin.test.Test
import kotlin.test.assertEquals

class CouiPopupDefaultAnimTest {

    private fun fixture(): Pair<CouiPopupLocateHelper, CouiPopupDefaultScreenController> {
        val h = CouiPopupLocateHelper()
        val c = CouiPopupDefaultScreenController(h)
        return h to c
    }

    @Test
    fun `default screen dims less than compact`() {
        val (_, c) = fixture()
        c.configMainMenuAnimationPropertiesForSubMenuEnter()
        assertEquals(0.7f, c.mainMenuAlphaPercent)
        assertEquals(0.7f, CouiPopupDefaultScreenController.DEFAULT_MAIN_MENU_ALPHA_PERCENT)
        assertEquals(0.3f, CouiPopupSmallScreenController(CouiPopupLocateHelper()).mainMenuAlphaPercent)
    }

    @Test
    fun `default screen scale is the fixed constant not the relocated ratio`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)
        c.configMainMenuAnimationPropertiesForSubMenuEnter()
        assertEquals(0.9f, c.mainMenuScalePercent)
        assertEquals(0.9f, CouiPopupDefaultScreenController.DEFAULT_MAIN_MENU_SCALE_PERCENT)
    }

    @Test
    fun `default screen pivots from the centre`() {
        val (h, c) = fixture()
        h.domain.mainMenu.set(0, 0, 400, 800)
        assertEquals(200, c.mainMenuEnterPivotX())
        assertEquals(400, c.mainMenuEnterPivotY())
    }

    @Test
    fun `submenu pivot comes from the anchor centre`() {
        val (h, c) = fixture()
        h.domain.subMenu.set(100, 200, 500, 900)
        h.domain.subMenuAnchor.set(120, 260, 480, 380)
        assertEquals(200, c.subMenuEnterPivotX())
        assertEquals(120, c.subMenuEnterPivotY())
    }

    @Test
    fun `main menu enters by scaling to 0_9 and dimming to 0_7`() {
        val (_, c) = fixture()
        c.configMainMenuAnimationPropertiesForSubMenuEnter()
        val unit = CouiPopupMenuTransitionUnit()
        c.startMainMenuSubMenuEnter(unit)
        repeat(600) { unit.advance(1f / 60f) }
        assertEquals(0.9f, unit.currentScaleX, 0.001f)
        assertEquals(0.9f, unit.currentScaleY, 0.001f)
        assertEquals(0.7f, unit.currentContentAlpha, 0.001f)
        assertEquals(0f, unit.currentTranslationY, 0.001f)
    }

    @Test
    fun `submenu enters by scaling up from nothing`() {
        val (_, c) = fixture()
        val unit = CouiPopupSubMenuTransitionUnit()
        c.startSubMenuEnter(unit)
        repeat(600) { unit.advance(1f / 60f) }
        assertEquals(1f, unit.currentScaleX, 0.001f)
        assertEquals(1f, unit.currentScaleY, 0.001f)
        assertEquals(1f, unit.currentAlpha, 0.001f)
        assertEquals(0f, unit.currentTranslateY, 0.001f)
    }

    @Test
    fun `both exits return to unity`() {
        val (_, c) = fixture()
        val menu = CouiPopupMenuTransitionUnit()
        val sub = CouiPopupSubMenuTransitionUnit()
        c.startMainMenuSubMenuEnter(menu)
        c.startSubMenuEnter(sub)
        repeat(600) { menu.advance(1f / 60f); sub.advance(1f / 60f) }
        c.startSubMenuExit(menu, sub)
        repeat(600) { menu.advance(1f / 60f); sub.advance(1f / 60f) }
        assertEquals(1f, menu.currentScaleX, 0.001f)
        assertEquals(1f, menu.currentContentAlpha, 0.001f)
        assertEquals(0f, sub.currentScaleX, 0.001f)
        assertEquals(0f, sub.currentAlpha, 0.001f)
    }
}