
package io.wfc35286.coui.kmp.component

import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class CouiPopupRect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0,
) {
    fun set(l: Int, t: Int, r: Int, b: Int) {
        left = l; top = t; right = r; bottom = b
    }

    fun set(other: CouiPopupRect) = set(other.left, other.top, other.right, other.bottom)

    fun offset(dx: Int, dy: Int) {
        left += dx; top += dy; right += dx; bottom += dy
    }

    fun width(): Int = right - left
    fun height(): Int = bottom - top
    fun centerX(): Int = (left + right) / 2
    fun centerY(): Int = (top + bottom) / 2
    fun isEmpty(): Boolean = left >= right || top >= bottom

    fun toComposeRect(): Rect = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    override fun toString(): String = "Rect($left, $top - $right, $bottom)"
}

internal object CouiPopupDomainConstants {
    const val FORCED_HORIZONTAL_NONE = 0
    const val FORCED_HORIZONTAL_START = 1
    const val FORCED_HORIZONTAL_END = 2

    const val HORIZONTAL_ALIGN_LEFT = 0
    const val HORIZONTAL_ALIGN_CENTER = 1
    const val HORIZONTAL_ALIGN_RIGHT = 2

    const val MAIN_MENU_POPUP_SIDE_AUTO = 0
    const val MAIN_MENU_POPUP_SIDE_ABOVE = 1
    const val MAIN_MENU_POPUP_SIDE_BELOW = 2
    const val MAIN_MENU_POPUP_SIDE_LEFT = 3
    const val MAIN_MENU_POPUP_SIDE_RIGHT = 4

    private const val PATH_ANCHOR_EDGE_CLAMP_EPS = 0.5f
    const val PATH_ANCHOR_MODE_CENTER = 0
    const val PATH_ANCHOR_MODE_AUTO = 1
    const val PATH_ANCHOR_MODE_OVERRIDE_NONE = -1

    const val PATH_ANCHOR_SLOT_START = 0
    const val PATH_ANCHOR_SLOT_CENTER = 1
    const val PATH_ANCHOR_SLOT_END = 2
    const val PATH_ANCHOR_SLOT_OVERRIDE_NONE = -1

    const val EPS = PATH_ANCHOR_EDGE_CLAMP_EPS
}

internal class CouiPopupDomain {
    val window = CouiPopupRect()
    val anchor = CouiPopupRect()
    val mainMenu = CouiPopupRect()
    val mainMenuRelocated = CouiPopupRect()
    val subMenu = CouiPopupRect()
    val popup = CouiPopupRect()
    val subMenuAnchor = CouiPopupRect()
    val anchorOutsets = CouiPopupRect()
    val windowBarriers = CouiPopupRect()
    private val availableClipBounds = CouiPopupRect()

    var anchorView: Any? = null
    var forcedHorizontalEdge = CouiPopupDomainConstants.FORCED_HORIZONTAL_NONE
    var globalOffsetX = 0
    var globalOffsetY = 0
    var subMenuAnchorIsFirstItem = false
    var mainMenuCenterAlign = false
    var mainMenuPopupSide = CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_AUTO
    var mainMenuRotationWrapperActive = false
    var mainMenuRotationWrapperLeft = 0
    var mainMenuRotationWrapperTop = 0

    var lastPathAnchorX = 0f
    var lastPathAnchorY = 0f

    var pathAnchorMode = CouiPopupDomainConstants.PATH_ANCHOR_MODE_CENTER
    private var pathAnchorModeOverride = CouiPopupDomainConstants.PATH_ANCHOR_MODE_OVERRIDE_NONE
    var pathAnchorSlotOverride = CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
    private var resolvedPathAnchorSlot = CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
    private var pathAnchorAutoPaddingOverride = Float.NaN

    init {
        reset()
    }

    fun reset() {
        window.set(0, 0, 0, 0)
        anchor.set(0, 0, 0, 0)
        mainMenu.set(0, 0, 0, 0)
        mainMenuRelocated.set(0, 0, 0, 0)
        subMenu.set(0, 0, 0, 0)
        popup.set(0, 0, 0, 0)
        subMenuAnchor.set(0, 0, 0, 0)
        anchorOutsets.set(0, 0, 0, 0)
        windowBarriers.set(0, 0, 0, 0)
        availableClipBounds.set(0, 0, 0, 0)
        globalOffsetX = 0
        globalOffsetY = 0
        subMenuAnchorIsFirstItem = false
        mainMenuCenterAlign = false
        mainMenuPopupSide = CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_AUTO
        pathAnchorMode = CouiPopupDomainConstants.PATH_ANCHOR_MODE_CENTER
        pathAnchorSlotOverride = CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
        resolvedPathAnchorSlot = CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
        lastPathAnchorX = 0f
        lastPathAnchorY = 0f
        pathAnchorModeOverride = CouiPopupDomainConstants.PATH_ANCHOR_MODE_OVERRIDE_NONE
        pathAnchorAutoPaddingOverride = Float.NaN
        forcedHorizontalEdge = CouiPopupDomainConstants.FORCED_HORIZONTAL_NONE
    }

    fun clearMainMenuPlacementState() {
        forcedHorizontalEdge = CouiPopupDomainConstants.FORCED_HORIZONTAL_NONE
    }

    fun clearMainMenuRotationLayout() {
        mainMenuRotationWrapperActive = false
        mainMenuRotationWrapperLeft = 0
        mainMenuRotationWrapperTop = 0
    }

    fun setMainMenuRotationLayout(left: Int, top: Int) {
        mainMenuRotationWrapperActive = true
        mainMenuRotationWrapperLeft = left
        mainMenuRotationWrapperTop = top
    }

    fun setPathAnchorAutoModeEnabled(enabled: Boolean) {
        pathAnchorModeOverride = if (enabled) {
            CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO
        } else {
            CouiPopupDomainConstants.PATH_ANCHOR_MODE_OVERRIDE_NONE
        }
    }

    fun setPathAnchorAutoPadding(padding: Float) {
        pathAnchorAutoPaddingOverride = padding
    }

    fun setPopupSide(side: Int) {
        mainMenuPopupSide = side
    }

    private fun isLayoutRtl(): Boolean = false // No Compose-side layout direction yet.

    private fun normalizePathAnchorSlot(slot: Int): Int = when (slot) {
        CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START
        CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END
        else -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
    }

    fun resolvePathAnchorMode(fraction: Float): Int =
        if (fraction <= 1f) CouiPopupDomainConstants.PATH_ANCHOR_MODE_CENTER
        else CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO

    private fun resolvePathAnchorModeWithOverride(fraction: Float): Int {
        if (pathAnchorModeOverride != CouiPopupDomainConstants.PATH_ANCHOR_MODE_OVERRIDE_NONE) {
            return pathAnchorModeOverride
        }
        return resolvePathAnchorMode(fraction)
    }

    fun resolvePathAnchorSlotOverride(fraction: Float): Int {
        val rounded = fraction.roundToInt()
        return when (rounded) {
            0 -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
            1 -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
            2 -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
            else -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE
        }
    }

    fun resolvePathAnchorAutoPadding(fraction: Float): Float {
        if (!pathAnchorAutoPaddingOverride.isNaN()) return pathAnchorAutoPaddingOverride
        return fraction
    }

    fun resolvePathAnchorSlot(mode: Int, override: Int, side: Int): Int {
        if (override != CouiPopupDomainConstants.PATH_ANCHOR_SLOT_OVERRIDE_NONE) {
            return normalizePathAnchorSlot(override)
        }
        if (mode == CouiPopupDomainConstants.PATH_ANCHOR_MODE_CENTER) {
            return CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
        }
        return inferPathAnchorSlotFromGeometry(side)
    }

    private fun inferPathAnchorSlotFromGeometry(side: Int): Int {
        if (side == CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_LEFT ||
            side == CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_RIGHT
        ) {
            return when (getMainMenuHorizontalAlignment()) {
                CouiPopupDomainConstants.HORIZONTAL_ALIGN_LEFT -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START
                CouiPopupDomainConstants.HORIZONTAL_ALIGN_RIGHT -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END
                else -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
            }
        }
        return when {
            mainMenu.centerY() < anchor.centerY() -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START
            mainMenu.centerY() > anchor.centerY() -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END
            else -> CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER
        }
    }

    fun getMainMenuHorizontalAlignment(): Int = when (forcedHorizontalEdge) {
        CouiPopupDomainConstants.FORCED_HORIZONTAL_START ->
            if (isLayoutRtl()) CouiPopupDomainConstants.HORIZONTAL_ALIGN_RIGHT
            else CouiPopupDomainConstants.HORIZONTAL_ALIGN_LEFT
        CouiPopupDomainConstants.FORCED_HORIZONTAL_END ->
            if (isLayoutRtl()) CouiPopupDomainConstants.HORIZONTAL_ALIGN_LEFT
            else CouiPopupDomainConstants.HORIZONTAL_ALIGN_RIGHT
        else -> CouiPopupDomainConstants.HORIZONTAL_ALIGN_CENTER
    }

    fun isMainMenuBelowAnchor(): Boolean = mainMenu.top >= anchor.bottom

    fun getResolvedMainMenuPopupSide(): Int {
        if (mainMenuPopupSide != CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_AUTO) return mainMenuPopupSide
        return if (isMainMenuBelowAnchor()) {
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_BELOW
        } else {
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_ABOVE
        }
    }

    private fun effectiveAutoPadding(mode: Int, slot: Int, padding: Float, side: Int): Float {
        if (mode != CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO) return 0f
        if (slot == CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER) return 0f
        if (padding <= 0f) return 0f
        return when (side) {
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_LEFT,
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_RIGHT,
            -> {
                val span = min(anchor.height(), mainMenu.height())
                span * 0.5f - 0.5f
            }
            else -> {
                val span = min(anchor.width(), mainMenu.width())
                span * 0.5f - 0.5f
            }
        }
    }

    private fun tangentCoordForSlot(
        slot: Int,
        start: Float,
        center: Float,
        end: Float,
        padding: Float,
    ): Float = when (slot) {
        CouiPopupDomainConstants.PATH_ANCHOR_SLOT_START -> start + padding
        CouiPopupDomainConstants.PATH_ANCHOR_SLOT_END -> end - padding
        else -> center
    }

    private fun resolveTangentCoordOnHorizontalEdge(slot: Int, padding: Float, rtl: Boolean): Float {
        val outer = if (rtl) anchor.right.toFloat() else anchor.left.toFloat()
        val inner = if (rtl) anchor.left.toFloat() else anchor.right.toFloat()
        val raw = tangentCoordForSlot(slot, outer, anchor.centerX().toFloat(), inner, padding)
        val lo = min(outer, inner)
        val hi = max(outer, inner)
        return max(lo, min(hi, raw))
    }

    private fun resolveTangentCoordOnVerticalEdge(slot: Int, padding: Float, rtl: Boolean): Float {
        val start = anchor.top.toFloat()
        val end = anchor.bottom.toFloat()
        val raw = tangentCoordForSlot(slot, start, anchor.centerY().toFloat(), end, padding)
        val lo = min(start, end)
        val hi = max(start, end)
        return max(lo, min(hi, raw))
    }

    fun computeMainMenuPopupPathAnchorOffset(fraction: Float, mode: Int, slotOverride: Int, out: FloatArray) {
        pathAnchorMode = mode
        pathAnchorSlotOverride = slotOverride
        val side = getResolvedMainMenuPopupSide()
        val slot = resolvePathAnchorSlot(mode, slotOverride, side)
        resolvedPathAnchorSlot = slot
        val padding = effectiveAutoPadding(mode, slot, fraction, side)
        val rtl = isLayoutRtl()

        var x: Float
        var y: Float
        when (side) {
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_LEFT -> {
                x = anchor.left - fraction
                y = resolveTangentCoordOnVerticalEdge(slot, padding, rtl)
            }
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_RIGHT -> {
                x = anchor.right + fraction
                y = resolveTangentCoordOnVerticalEdge(slot, padding, rtl)
            }
            CouiPopupDomainConstants.MAIN_MENU_POPUP_SIDE_BELOW -> {
                x = resolveTangentCoordOnHorizontalEdge(slot, padding, rtl)
                y = anchor.top - fraction
            }
            else -> {
                x = resolveTangentCoordOnHorizontalEdge(slot, padding, rtl)
                y = anchor.bottom + fraction
            }
        }
        out[0] = x
        out[1] = y
    }

    fun computeMainMenuPopupPathAnchorOffset(fraction: Float, out: FloatArray) {
        val mode = resolvePathAnchorModeWithOverride(fraction)
        val override = resolvePathAnchorSlotOverride(fraction)
        computeMainMenuPopupPathAnchorOffset(fraction, mode, override, out)
    }

    fun getMainMenuEnterPivotX(): Int {
        if (mainMenuCenterAlign) return mainMenu.centerX() - mainMenu.left
        val edge = forcedHorizontalEdge
        if (edge != CouiPopupDomainConstants.FORCED_HORIZONTAL_START &&
            edge != CouiPopupDomainConstants.FORCED_HORIZONTAL_END
        ) {
            if (useAutoCornerPivotX()) {
                val raw = lastPathAnchorX - mainMenu.left
                return max(0, min(mainMenu.width(), raw.roundToInt()))
            }
            return mainMenu.width() / 2
        }
        val width = mainMenu.width()
        return if (edge == CouiPopupDomainConstants.FORCED_HORIZONTAL_START) {
            if (isLayoutRtl()) width else 0
        } else {
            if (isLayoutRtl()) 0 else width
        }
    }

    private fun useAutoCornerPivotX(): Boolean {
        if (pathAnchorMode != CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO) return false
        if (resolvedPathAnchorSlot == CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER) return false
        return !mainMenu.isEmpty()
    }

    fun getMainMenuEnterPivotY(): Int {
        if (mainMenuCenterAlign) return mainMenu.centerY() - mainMenu.top
        if (useAutoCornerPivotY()) {
            val raw = lastPathAnchorY - mainMenu.top
            return max(0, min(mainMenu.height(), raw.roundToInt()))
        }
        return mainMenu.height() / 2
    }

    private fun useAutoCornerPivotY(): Boolean {
        val side = getResolvedMainMenuPopupSide()
        if (pathAnchorMode != CouiPopupDomainConstants.PATH_ANCHOR_MODE_AUTO) return false
        if (resolvedPathAnchorSlot == CouiPopupDomainConstants.PATH_ANCHOR_SLOT_CENTER) return false
        return !mainMenu.isEmpty()
    }

    fun getSubMenuEnterPivotX(): Int = subMenuAnchor.centerX() - subMenu.left
    fun getSubMenuEnterPivotY(): Int = subMenuAnchor.centerY() - subMenu.top

    fun getAnchorRealRect(out: CouiPopupRect) {
        out.set(anchor)
        if (anchorView == null) return
        out.offset(-globalOffsetX, -globalOffsetY)
    }

    fun getAvailableRect(out: CouiPopupRect) {
        out.set(windowBarriers)
        if (out.isEmpty()) out.set(window)
    }

    fun getAvailableRectWidth(): Int = windowBarriers.width().takeIf { it > 0 } ?: window.width()
    fun getAvailableRectHeight(): Int = windowBarriers.height().takeIf { it > 0 } ?: window.height()

    fun computeMainMenuClippedToAvailable(out: CouiPopupRect): Boolean {
        getAvailableRect(out)
        if (out.isEmpty()) return false
        val clipped = CouiPopupRect(
            max(out.left, mainMenu.left),
            max(out.top, mainMenu.top),
            min(out.right, mainMenu.right),
            min(out.bottom, mainMenu.bottom),
        )
        out.set(clipped)
        return !clipped.isEmpty()
    }
}
