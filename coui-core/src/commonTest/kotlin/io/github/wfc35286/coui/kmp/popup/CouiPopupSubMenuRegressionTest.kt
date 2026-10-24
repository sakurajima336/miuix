// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CouiPopupSubMenuRegressionTest {
    private fun driver(): CouiPopupTransitionDriver {
        val helper = CouiPopupLocateHelper()
        helper.domain.window.set(0, 0, 1000, 1800)
        helper.domain.mainMenu.set(100, 600, 700, 1400)
        helper.domain.mainMenuRelocated.set(100, 600, 640, 1320)
        helper.domain.subMenu.set(100, 550, 700, 1050)
        helper.setAnchorRow(160, 100, 0)
        return CouiPopupTransitionDriver(helper).apply {
            smallScreen = true
            menuInnerPaddingVertical = 8
            minGapBetweenMainAndSubMenu = 12
            show(0)
            measured()
        }
    }

    private fun CouiPopupTransitionDriver.settle() {
        repeat(300) { if (!advance(1f / 60f)) return }
        error("submenu spring did not settle")
    }

    @Test
    fun `first exit frame restores the main menu gradually`() {
        val d = driver()
        d.settle()
        assertEquals(-62f, d.mainTranslationY)
        val scale = d.mainScaleX
        val alpha = d.mainContentAlpha
        val translation = d.mainTranslationY
        d.hide()
        assertEquals(scale, d.mainScaleX)
        assertEquals(alpha, d.mainContentAlpha)
        assertEquals(translation, d.mainTranslationY)
        d.advance(1f / 120f)
        assertTrue(d.mainScaleX > scale && d.mainScaleX < 1f)
        assertTrue(d.mainContentAlpha > alpha && d.mainContentAlpha < 1f)
        assertTrue(d.mainTranslationY > translation && d.mainTranslationY < 0f)
        d.settle()
        assertFalse(d.visible)
        assertEquals(1f, d.mainScaleX)
        assertEquals(1f, d.mainContentAlpha)
    }

    @Test
    fun `enter reversed twice retains its current reveal and scale`() {
        val d = driver()
        repeat(5) { d.advance(1f / 120f) }
        val enteringScale = d.mainScaleX
        val enteringHeight = d.revealHeight
        d.hide()
        assertEquals(enteringScale, d.mainScaleX)
        d.advance(1f / 120f)
        assertTrue(d.revealHeight < 500f)
        assertTrue(enteringHeight > 100f)
        val closingScale = d.mainScaleX
        val closingHeight = d.revealHeight
        d.show(0)
        assertEquals(closingScale, d.mainScaleX)
        assertEquals(closingHeight, d.revealHeight)
        d.settle()
        assertTrue(d.entered)
        assertEquals(500f, d.revealHeight)
        assertEquals(0.3f, d.mainContentAlpha, 0.0001f)
    }

    @Test
    fun `switching parent skips exit and waits for the new measurement`() {
        val d = driver()
        d.settle()
        d.show(3)
        d.advance(1f / 60f)
        assertEquals(3, d.index)
        assertEquals(CouiPopupTransitionDriver.Phase.Measuring, d.phase)
        assertEquals(0f, d.subAlpha)
        assertEquals(0f, d.revealHeight)
        d.domain.subMenu.set(100, 650, 700, 1450)
        d.locate.setAnchorRow(320, 100, 0)
        d.measured()
        d.settle()
        assertTrue(d.entered)
        assertEquals(800f, d.revealHeight)
    }

    @Test
    fun `unmeasured submenu never renders a full panel`() {
        val d = CouiPopupTransitionDriver(CouiPopupLocateHelper())
        d.show(1)
        d.measured()
        assertEquals(CouiPopupTransitionDriver.Phase.Measuring, d.phase)
        assertEquals(0f, d.subAlpha)
        assertEquals(0f, d.revealHeight)
        d.hide()
        assertFalse(d.visible)
    }

    @Test
    fun `progress spring is independent of refresh rate and dropped frames`() {
        fun sample(intervals: List<Float>): Float {
            val unit = CouiPopupSubMenuTransitionUnit()
            unit.animateToEnterState(0f, 0.35f)
            intervals.forEach { unit.advance(it) }
            return unit.currentProgressValue()
        }
        val sixty = sample(List(12) { 1f / 60f })
        assertEquals(sixty, sample(List(24) { 1f / 120f }), 0.01f)
        assertEquals(sixty, sample(listOf(0.08f, 0.12f)), 0.01f)
    }

    @Test
    fun `material and content sample the same reveal and transform`() {
        val visual = CouiPopupVisualState({ 1f }, { 1f }, { 1f }, { Offset.Zero }, CouiPopupTouchMotion())
        val reveal = Rect(0f, 6f, 600f, 180f)
        visual.panelSize = Offset(600f, 500f)
        visual.geometry = {
            CouiPopupPanelGeometry(scale = 0.9f, translation = Offset(0f, 80f), reveal = reveal)
        }
        val materialFrame = visual.sampleFrame()
        visual.commitFrame(materialFrame)
        assertEquals(materialFrame.translation, visual.translation)
        assertEquals(materialFrame.scaleX, visual.scaleX)
        assertEquals(materialFrame.scaleY, visual.scaleY)
        assertEquals(reveal, visual.contentReveal)
        assertEquals(materialFrame.contentAlpha, visual.contentAlpha)
    }

    @Test
    fun `rtl side submenu prefers the right side when there is room`() {
        val h = CouiPopupLocateHelper()
        h.domain.window.set(0, 0, 1800, 1200)
        h.domain.mainMenuRelocated.set(600, 200, 1000, 700)
        h.horizontalOverlap = 14
        h.isRtl = true
        assertEquals(986, h.subMenuOffsetX(400))
        h.domain.window.right = 1200
        assertEquals(214, h.subMenuOffsetX(400))
    }

    @Test
    fun `detaching an exited submenu never restores its full outline`() {
        val d = driver()
        d.settle()
        val visual = CouiPopupVisualState({ 1f }, { 1f }, { 1f }, { Offset.Zero }, CouiPopupTouchMotion())
        visual.geometry = {
            CouiPopupPanelGeometry(
                scale = d.subScaleX,
                alpha = d.subAlpha,
                reveal = if (d.revealActive) Rect(0f, d.revealTop, 600f, d.revealTop + d.revealHeight) else null,
            )
        }
        visual.commitFrame(visual.sampleFrame())
        d.hide()
        d.settle()
        visual.releaseFrame()
        assertFalse(d.visible)
        assertEquals(0f, visual.contentAlpha)
        assertEquals(0f, visual.contentReveal?.height)
        assertTrue(d.revealActive)
    }

    @Test
    fun `center based transform matches the official top pivot bounds`() {
        val visual = CouiPopupVisualState({ 1f }, { 1f }, { 1f }, { Offset.Zero }, CouiPopupTouchMotion())
        // Device sample: main (609,524)-(1192,1376) = 583x852, relocated 766 tall.
        // The official View uses pivotY=0 with translationY ~= 0; our center-based
        // RenderNode must reach the same visual bounds via a -43px translation.
        val bounds = Rect(609f, 524f, 1192f, 1376f)
        val relocatedHeight = 766f
        visual.panelSize = Offset(bounds.width, bounds.height)
        visual.geometry = { CouiPopupPanelGeometry(scale = relocatedHeight / bounds.height, pivot = Offset(0.5f, 0f)) }
        val transformed = couiPopupTransformedBounds(bounds, visual)
        assertEquals(bounds.top, transformed.top, 0.5f)
        assertEquals(relocatedHeight, transformed.height, 0.5f)
        assertEquals(bounds.center.x, transformed.center.x, 0.5f)
        // The center-based node needs the -43px translation the official View expresses
        // through its top pivot instead.
        assertEquals(-43f, visual.translation.y, 1f)
    }

    @Test
    fun `enter starts from the anchor row sized prototype outline`() {
        val d = driver()
        // Official configSubMenuAnimationPropertiesForSubMenuEnter:
        //   mSubMenuClippedTopStart    = mMenuInnerPaddingVertical        -> 8
        //   mSubMenuClippedBottomStart = min(anchor.height(), main.bottom - anchor.top) -> 100
        // and it calls setOverrideOutline(..., 1.0f) with that rect before the spring starts.
        assertTrue(d.visible)
        assertEquals(CouiPopupTransitionDriver.Phase.Entering, d.phase)
        assertTrue(d.revealActive)
        assertEquals(8f, d.revealTop)
        assertEquals(100f, d.revealHeight)
    }

    @Test
    fun `exiting submenu keeps the main menu interactive`() {
        val d = driver()
        d.settle()
        assertTrue(d.entered)
        d.hide()
        // Official onSubMenuStartToExit re-enables the main menu list as soon as the exit
        // starts, so the parent row can be tapped again before the spring settles.
        assertTrue(d.visible)
        assertFalse(d.opening)
        d.settle()
        assertFalse(d.visible)
        assertFalse(d.opening)
    }

    @Test
    fun `compact submenu includes the parent group boundary without sorting it`() {
        val parent = CouiPopupListItem(
            "Parent",
            groupId = 1,
            subItems = listOf(
                CouiPopupListItem("A"),
                CouiPopupListItem("B"),
                CouiPopupListItem("C"),
            ),
        )
        val compact = couiPopupSubMenuItems(parent, compact = true)
        assertEquals(listOf("Parent", "", "A", "B", "C"), compact.map { it.title })
        assertEquals(CouiPopupItemType.GroupDivider, compact[1].type)
        assertEquals(listOf("A", "B", "C"), couiPopupSubMenuItems(parent, compact = false).map { it.title })
        assertEquals(listOf(0, -1, 1, 2, 3), compact.displayToDataIndex())
    }

    @Test
    fun `available bounds include the visible system bar frame`() {
        val p = CouiPopupPositionProvider(null, 48, 48, 95, 24, CouiPopupPlacement())
        p.visibleWindow = androidx.compose.ui.unit.IntRect(0, 139, 1240, 2724)
        assertEquals(androidx.compose.ui.unit.IntRect(48, 187, 1192, 2629), p.availableBounds(androidx.compose.ui.unit.IntSize(1240, 2772)))
    }
}
