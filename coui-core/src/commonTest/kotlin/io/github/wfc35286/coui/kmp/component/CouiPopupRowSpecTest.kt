
package io.github.wfc35286.coui.kmp.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CouiPopupRowSpecTest {

    @Test
    fun `iconless row keeps the official twenty dp padding`() {
        val spec = couiPopupRowSpec(CouiPopupListItem("Title"))
        assertEquals(20, spec.paddingStart)
        assertEquals(20, spec.paddingEnd)
        assertFalse(spec.showLeadingIcon)
    }

    @Test
    fun `icon switches the padding to twenty dp`() {
        val item = CouiPopupListItem("Title", leadingIcon = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.Red))
        val spec = couiPopupRowSpec(item)
        assertEquals(20, spec.paddingStart)
        assertEquals(20, spec.paddingEnd)
        assertTrue(spec.showLeadingIcon)
    }

    @Test
    fun `neither hint nor state hides both gaps`() {
        val spec = couiPopupRowSpec(CouiPopupListItem("Title"))
        assertFalse(spec.titleGapVisible, "no 8dp gap when nothing follows")
        assertFalse(spec.hintGapVisible)
        assertFalse(spec.stateSlotVisible)
        assertFalse(spec.hintVisible)
    }

    @Test
    fun `submenu row lays out the eight dp gap only`() {
        val item = CouiPopupListItem("Title", subItems = listOf(CouiPopupListItem("Sub")))
        val spec = couiPopupRowSpec(item)
        assertTrue(spec.titleGapVisible)
        assertFalse(spec.hintGapVisible, "4dp gap needs BOTH hint and state")
        assertTrue(spec.stateSlotVisible)
        assertTrue(spec.stateSlotIsChevron)
    }

    @Test
    fun `checked row lays out the eight dp gap only`() {
        val spec = couiPopupRowSpec(CouiPopupListItem("Title", checked = true))
        assertTrue(spec.titleGapVisible)
        assertFalse(spec.hintGapVisible)
        assertTrue(spec.stateSlotVisible)
        assertFalse(spec.stateSlotIsChevron, "a check mark is not a chevron")
    }

    @Test
    fun `hint and state together lay out both gaps`() {
        val item = CouiPopupListItem(
            "Title",
            checked = true,
            hint = { },
        )
        val spec = couiPopupRowSpec(item)
        assertTrue(spec.titleGapVisible)
        assertTrue(spec.hintGapVisible, "4dp gap appears only when hint AND state coexist")
        assertTrue(spec.stateSlotVisible)
        assertTrue(spec.hintVisible)
    }

    @Test
    fun `hint alone shows the hint but no extra gap`() {
        val item = CouiPopupListItem("Title", hint = { })
        val spec = couiPopupRowSpec(item)
        assertTrue(spec.hintVisible)
        assertTrue(spec.titleGapVisible)
        assertFalse(spec.hintGapVisible)
        assertFalse(spec.stateSlotVisible)
    }

    @Test
    fun `disabled row hides its hint`() {
        val item = CouiPopupListItem("Title", enabled = false, hint = { })
        val spec = couiPopupRowSpec(item)
        assertFalse(spec.hintVisible, "setHint: !isEnable -> GONE")
    }

    @Test
    fun `state slot content follows chevron then check`() {
        val chevron = couiPopupRowSpec(CouiPopupListItem("T", subItems = listOf(CouiPopupListItem("S"))))
        assertEquals(CouiPopupStateSlot.Chevron, couiPopupStateSlotContent(chevron, checked = true))

        val check = couiPopupRowSpec(CouiPopupListItem("T", checked = true))
        assertEquals(CouiPopupStateSlot.Check, couiPopupStateSlotContent(check, checked = true))

        val empty = couiPopupRowSpec(CouiPopupListItem("T", checkable = true))
        assertEquals(CouiPopupStateSlot.Empty, couiPopupStateSlotContent(empty, checked = false))
    }

    @Test
    fun `description visibility tracks the text`() {
        assertFalse(couiPopupRowSpec(CouiPopupListItem("T")).descriptionVisible)
        assertFalse(couiPopupRowSpec(CouiPopupListItem("T", description = "")).descriptionVisible)
        assertTrue(couiPopupRowSpec(CouiPopupListItem("T", description = "d")).descriptionVisible)
    }

    @Test
    fun `title has three lines without a description and medium only for the anchor`() {
        val normal = couiPopupRowSpec(CouiPopupListItem("T"))
        assertEquals(3, normal.titleMaxLines)
        assertEquals(2, couiPopupRowSpec(CouiPopupListItem("T", description = "D")).titleMaxLines)
        assertFalse(normal.titleMedium)

        val anchor = couiPopupRowSpec(CouiPopupListItem("T"), isSubMenuAnchor = true)
        assertTrue(anchor.titleMedium)
    }

    @Test
    fun `group dividers are inserted only when groups change and the list is long enough`() {
        val flat = listOf(
            CouiPopupListItem("A"),
            CouiPopupListItem("B"),
            CouiPopupListItem("C"),
        )
        assertEquals(flat, couiPopupWithGroupDividers(flat))

        val oneGroup = listOf(
            CouiPopupListItem("A", groupId = 0),
            CouiPopupListItem("B", groupId = 0),
            CouiPopupListItem("C", groupId = 0),
        )
        assertEquals(oneGroup, couiPopupWithGroupDividers(oneGroup))

        val twoGroups = listOf(
            CouiPopupListItem("A", groupId = 0),
            CouiPopupListItem("B", groupId = 0),
            CouiPopupListItem("C", groupId = 1),
        )
        val out = couiPopupWithGroupDividers(twoGroups)
        assertEquals(4, out.size)
        assertEquals(CouiPopupItemType.GroupDivider, out[2].type)
        assertEquals("A", out[0].title)
        assertEquals("B", out[1].title)
        assertEquals("C", out[3].title)
    }

    @Test
    fun `short lists never get dividers`() {
        val two = listOf(
            CouiPopupListItem("A", groupId = 0),
            CouiPopupListItem("B", groupId = 1),
        )
        assertEquals(two, couiPopupWithGroupDividers(two))
    }

    @Test
    fun `submenu availability requires non empty default rows`() {
        assertFalse(couiPopupHasAvailableSubMenu(CouiPopupListItem("T")))
        assertTrue(
            couiPopupHasAvailableSubMenu(
                CouiPopupListItem("T", subItems = listOf(CouiPopupListItem("S"))),
            ),
        )
    }
}