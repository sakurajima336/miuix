// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CouiPopupAnimUnitsTest {

    @Test
    fun `progress starts normalised to zero`() {
        val unit = CouiPopupSubMenuTransitionUnit()
        assertEquals(0f, unit.currentProgressValue())
        assertEquals(1f, unit.currentScaleX)
        assertEquals(1f, unit.currentAlpha)
        assertEquals(0f, unit.currentTranslateY)
    }

    @Test
    fun `small screen enter pins scale to one and only moves y`() {
        val unit = CouiPopupSubMenuTransitionUnit()
        unit.configForSmallScreenEnter(yStart = 300f, yEnd = 0f, alphaStart = 1f, alphaEnd = 1f)
        assertEquals(300f, unit.currentTranslateY)
        assertEquals(1f, unit.currentAlpha)
        assertEquals(1f, unit.currentScaleX)
        assertEquals(1f, unit.currentScaleY)
    }

    @Test
    fun `default screen enter goes from scale and alpha zero to one`() {
        val unit = CouiPopupSubMenuTransitionUnit()
        unit.configForDefaultScreenEnter(scaleStart = 0f, alphaStart = 0f)
        assertEquals(1f, unit.currentScaleX)
        unit.animateToEnterState(0f, 0.35f)
        var dt = 1f / 60f
        repeat(600) { unit.advance(dt) }
        assertEquals(1f, unit.currentScaleX, 0.001f)
        assertEquals(1f, unit.currentAlpha, 0.001f)
        assertEquals(0f, unit.currentTranslateY, 0.001f)
    }

    @Test
    fun `small screen enter translates back to the end target`() {
        val unit = CouiPopupSubMenuTransitionUnit()
        unit.configForSmallScreenEnter(yStart = 300f, yEnd = 0f, alphaStart = 1f, alphaEnd = 1f)
        unit.animateToEnterState(0f, 0.35f)
        var dt = 1f / 60f
        repeat(600) { unit.advance(dt) }
        assertEquals(0f, unit.currentTranslateY, 0.001f)
        assertEquals(1f, unit.currentScaleX, 0.001f)
    }

    @Test
    fun `updateTranslateYTargets moves the start without restarting`() {
        val unit = CouiPopupSubMenuTransitionUnit()
        unit.configForSmallScreenEnter(yStart = 300f, yEnd = 0f, alphaStart = 1f, alphaEnd = 1f)
        unit.animateToEnterState(0f, 0.35f)
        unit.updateTranslateYTargets(120f, 0f)
        assertTrue(unit.isRunning())
        unit.advance(1f / 60f)
        assertTrue(unit.currentTranslateY.isFinite())
    }

    @Test
    fun `menu unit interpolates from one down to its targets`() {
        val unit = CouiPopupMenuTransitionUnit()
        unit.animateToSubMenuEnterState(0.9f, 0.9f, -24f, 0.3f, bounce = 0f, response = 0.35f)
        var dt = 1f / 60f
        repeat(600) { unit.advance(dt) }
        assertEquals(0.9f, unit.currentScaleX, 0.001f)
        assertEquals(0.3f, unit.currentContentAlpha, 0.001f)
        assertEquals(-24f, unit.currentTranslationY, 0.001f)
    }

    @Test
    fun `exit returns the menu unit to unity`() {
        val unit = CouiPopupMenuTransitionUnit()
        unit.animateToSubMenuEnterState(0.9f, 0.9f, -24f, 0.3f, bounce = 0f, response = 0.35f)
        var dt = 1f / 60f
        repeat(600) { unit.advance(dt) }
        unit.animateToSubMenuExitState(0f, 0.35f)
        repeat(600) { unit.advance(dt) }
        assertEquals(1f, unit.currentScaleX, 0.001f)
        assertEquals(1f, unit.currentContentAlpha, 0.001f)
        assertEquals(0f, unit.currentTranslationY, 0.001f)
    }

    @Test
    fun `reset without keep value restores the defaults`() {
        val unit = CouiPopupMenuTransitionUnit()
        unit.animateToSubMenuEnterState(0.9f, 0.9f, -24f, 0.3f, bounce = 0f, response = 0.35f)
        repeat(120) { unit.advance(1f / 60f) }
        unit.reset(keepValue = false)
        assertEquals(0f, unit.currentProgressValue())
        assertEquals(1f, unit.currentScaleX)
        assertEquals(1f, unit.currentContentAlpha)
    }

    @Test
    fun `converted fraction clamps outside zero one`() {
        assertEquals(5f, couiConvertedFraction(0f, 10f, 0.5f))
        assertEquals(0f, couiConvertedFraction(0f, 10f, -1f))
        assertEquals(10f, couiConvertedFraction(0f, 10f, 2f))
    }
}
