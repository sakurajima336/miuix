// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import kotlin.math.abs
import kotlin.math.roundToInt

/** `COUISeekBarCore.MOVE_BY_DEFAULT / MOVE_BY_FINGER / MOVE_BY_DISTANCE`. */
enum class CouiSeekBarMoveType {
    /** Tap on release; animate the initial drag transition to the finger. */
    Default,

    /** Tap on release; apply move damping to the absolute finger coordinate. */
    ByFinger,

    /** Ignore taps; adjust the current value by travel after the drag threshold is crossed. */
    ByDistance,
}

/** Touch policy from `COUISeekBarAdvanced.onTouchEvent` and the core's two trackTouch methods. */
internal class CouiSeekBarGesture(
    initialValue: Float,
    private val start: Float,
    private val travel: Float,
    private val rtl: Boolean,
    val moveType: CouiSeekBarMoveType,
    moveDamping: Float,
    private val touchSlop: Float,
    private val downX: Float,
) {
    var dragging: Boolean = false
        private set
    var target: Float = initialValue.coerceIn(0f, 1f)
        private set
    private var lastX = downX
    private val damping = if (moveDamping == 0f) 1f else moveDamping.coerceIn(0f, 1f)

    fun move(x: Float): Float? {
        if (!dragging) {
            if (abs(x - downX) <= touchSlop) return null
            dragging = true
            lastX = x
            // Core.onTouchEvent records mLastX and skips the first update for MOVE_BY_DISTANCE.
            if (moveType == CouiSeekBarMoveType.ByDistance) return null
        }
        val next = when (moveType) {
            CouiSeekBarMoveType.Default -> fractionAt(x)

            CouiSeekBarMoveType.ByFinger -> fractionAt(((x - lastX) * damping + lastX).roundToInt().toFloat())

            CouiSeekBarMoveType.ByDistance -> {
                val delta = (x - lastX) * damping / travel
                (target + if (rtl) -delta else delta).coerceIn(0f, 1f)
            }
        }
        if (next != target) lastX = x
        target = next
        return next
    }

    fun up(x: Float, y: Float, width: Float, height: Float, paddingVertical: Float): Float? {
        if (dragging) return move(x) ?: target
        // Advanced.onTouchEvent only starts a click transition for ACTION_UP inside the view.
        if (moveType == CouiSeekBarMoveType.ByDistance || x !in 0f..width ||
            y !in paddingVertical..(height - paddingVertical)
        ) {
            return null
        }
        target = fractionAt(x)
        return target
    }

    private fun fractionAt(x: Float): Float {
        val physical = ((x - start) / travel).coerceIn(0f, 1f)
        return if (rtl) 1f - physical else physical
    }
}
