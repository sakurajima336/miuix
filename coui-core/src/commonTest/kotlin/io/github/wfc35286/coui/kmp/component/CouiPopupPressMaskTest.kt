package io.github.wfc35286.coui.kmp.component

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Official `COUIPopupListWindow.triggerShowSubMenu` (COUIPopupListWindow.smali:3321-3378) reads the
 * clicked row's mask progress and then calls `ListItemMaskEffectDrawable.setTouchExited()`; the sub
 * menu's parent row draws `mSharedDrawable.getChild()` (DefaultAdapter.smali:565-573), so the
 * highlight travels into the sub menu and then fades out. It is not locked on - the
 * `setHoverStateLocked(true, true, true)` call only exists in the *non* small-screen branch of
 * `setSubMenuGroupItemActivation` (COUIPopupListWindow.smali:2890+), which a phone never takes.
 */
class CouiPopupPressMaskTest {

    private fun panel(): CouiPopupPanelState = CouiPopupPanelState(CouiPopupTouchMotion()).apply {
        requestFrame = {}
        items = listOf(CouiPopupListItem("Title 5"), CouiPopupListItem("Item A"))
    }

    private fun CouiPopupPanelState.settle() {
        repeat(400) { advance(1f / 60f) }
    }

    @Test
    fun `a plain press lights the row mask`() {
        val p = panel()
        p.row(1, Rect(0f, 120f, 600f, 240f))
        p.attach(1)
        p.settle()
        assertTrue(p.pressProgress > 0.99f, "press mask should be lit, was ${p.pressProgress}")
    }

    @Test
    fun `handed off mask starts at the transferred progress and fades out`() {
        val main = panel()
        main.row(0, Rect(0f, 0f, 600f, 120f))
        main.attach(0)
        main.settle()
        val transferred = main.pressProgress
        assertTrue(transferred > 0.99f, "press mask should be lit, was $transferred")

        // The sub menu panel receives the progress, then its parent echo row lays out.
        val sub = panel()
        sub.handoff(transferred)
        sub.row(0, Rect(0f, 0f, 600f, 120f))
        assertEquals(transferred, sub.pressProgress, 0.01f, "handoff must start from the transferred value")

        sub.settle()
        assertEquals(0f, sub.pressProgress, 0.001f, "handed off mask must fade out, not stay lit")
    }

    @Test
    fun `handoff with a released press keeps the row dark`() {
        val sub = panel()
        sub.handoff(0f)
        sub.row(0, Rect(0f, 0f, 600f, 120f))
        sub.settle()
        assertEquals(0f, sub.pressProgress, 0.001f)
    }
}