
package io.github.wfc35286.coui.kmp.component

import kotlin.math.max
import kotlin.math.min

internal class CouiPopupLocateHelper(
    val domain: CouiPopupDomain = CouiPopupDomain(),
) {

    val submenuAnchorBounds = CouiPopupRect()

    private var anchorRowTop = 0
    private var anchorRowHeight = 0

    var statusBarMargin = 0                     // coui_popup_list_window_top_status_bar_margin = 16dp
    var navigationBarMargin = 0                 // ..._bottom_navigation_bar_margin = 32dp
    var minGapBetweenMainAndSubMenu = 0         // ..._min_gap_to_top = 12dp
    var mainMenuShrinkWidth = 0                 // ..._main_menu_shrink_width = 16dp
    var horizontalOverlap = 0                   // ..._horizontal_overlap_between_main_and_sub_menu = 14dp
    var verticalOverlap = 0                     // ..._vertical_overlap_between_main_and_sub_menu = 14dp
    var anchorGap = 0                           // ..._default_vertical_gap_to_anchor = 8dp
    var menuInnerPaddingVertical = 0            // coui_popup_list_padding_vertical = 8dp

    var smallScreen = false
    var isRtl = false

    fun setAnchorRow(rowTopInList: Int, rowHeight: Int, scrollY: Int) {
        anchorRowTop = rowTopInList
        anchorRowHeight = rowHeight
        val relativeTop = rowTopInList - scrollY
        submenuAnchorBounds.set(
            domain.mainMenu.left,
            domain.mainMenu.top + relativeTop,
            domain.mainMenu.right,
            domain.mainMenu.top + relativeTop + rowHeight,
        )
        domain.subMenuAnchor.set(submenuAnchorBounds)
    }

    fun dynamicAnchorTop(mainMenuTranslationY: Float): Int =
        (submenuAnchorBounds.top - mainMenuTranslationY).toInt()

    fun subMenuOffsetX(subMenuWidth: Int): Int {
        if (smallScreen) return domain.mainMenu.left
        val relocated = domain.mainMenuRelocated
        val available = domain.windowBarriers.takeIf { !it.isEmpty() } ?: domain.window
        if (isRtl) {
            val candidate = relocated.right - horizontalOverlap
            return if (candidate + subMenuWidth < available.right) candidate
            else relocated.left + horizontalOverlap - subMenuWidth
        }
        val candidate = relocated.left + horizontalOverlap - subMenuWidth
        return if (candidate > available.left) candidate else relocated.right - horizontalOverlap
    }

    fun subMenuOffsetY(subMenuHeight: Int, mainMenuTranslationY: Float = 0f): Int {
        val available = domain.windowBarriers.takeIf { !it.isEmpty() } ?: domain.window
        return if (smallScreen) {
            val anchorTop = subMenuAnchorTopAfterMainMenuRelocated(mainMenuTranslationY)
            if (anchorTop - verticalOverlap + subMenuHeight < available.bottom) {
                anchorTop - verticalOverlap
            } else {
                available.bottom - subMenuHeight
            }
        } else {
            val top = submenuAnchorBounds.top
            if (top + subMenuHeight < available.bottom) top else available.bottom - subMenuHeight
        }
    }

    fun subMenuAnchorTopAfterMainMenuRelocated(mainMenuTranslationY: Float = 0f): Int {
        val mainMenu = domain.mainMenu
        val relocated = domain.mainMenuRelocated
        val relativeTop = submenuAnchorBounds.top - mainMenu.top - mainMenuTranslationY.toInt()
        val scale = if (mainMenu.height() > 0 && relocated.height() > 0) {
            relocated.height().toFloat() / mainMenu.height().toFloat()
        } else {
            1f
        }
        return (relocated.top.toFloat() + scale * relativeTop).toInt()
    }

    fun relocateMainMenu() {
        val mainMenu = domain.mainMenu
        val relocated = domain.mainMenuRelocated
        val width = (mainMenu.width() * MAIN_MENU_SCALE_PERCENT).toInt()
        val height = (mainMenu.height() * MAIN_MENU_SCALE_PERCENT).toInt()
        if (smallScreen) {
            relocated.set(mainMenu.left, mainMenu.top, mainMenu.left + width, mainMenu.top + height)
            relocated.offset(relocateOffsetX(), relocateOffsetY())
        } else {
            val cx = mainMenu.centerX()
            val cy = mainMenu.centerY()
            relocated.set(cx - width / 2, cy - height / 2, cx - width / 2 + width, cy - height / 2 + height)
        }
    }

    private fun relocateOffsetX(): Int {
        val mainMenu = domain.mainMenu
        val relocated = domain.mainMenuRelocated
        val shrink = mainMenu.width() - relocated.width()
        val anchorCx = domain.anchor.centerX()
        val mainCx = mainMenu.centerX()
        return when {
            anchorCx < mainCx - 1 -> 0
            anchorCx > mainCx + 1 -> shrink
            else -> shrink / 2
        }
    }

    private fun relocateOffsetY(): Int {
        val mainMenu = domain.mainMenu
        val available = domain.windowBarriers.takeIf { !it.isEmpty() } ?: domain.window
        val subMenuHeight = domain.subMenu.height()
        val top = mainMenu.top
        if (top + minGapBetweenMainAndSubMenu + subMenuHeight < available.bottom) return 0
        return available.bottom - subMenuHeight - minGapBetweenMainAndSubMenu - top
    }

    fun completeAnchorSetup() {
        relocateMainMenu()
    }

    fun anchorRowHeight(): Int = anchorRowHeight

    private companion object {
        const val MAIN_MENU_SCALE_PERCENT = 0.9f
    }
}

internal fun couiLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

internal fun couiClamp(value: Int, minValue: Int, maxValue: Int): Int =
    max(minValue, min(maxValue, value))
