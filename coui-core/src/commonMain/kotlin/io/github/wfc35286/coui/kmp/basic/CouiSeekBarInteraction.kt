// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic
import kotlin.math.abs
import kotlin.math.roundToInt


/** How the seek bar responds to taps and drags. */
enum class CouiSeekBarMoveType {
    /** Tap on release; animate the initial drag transition to the finger. */
    Default,

    /** Tap on release; apply move damping to the absolute finger coordinate. */
    ByFinger,

    /** Ignore taps; adjust the current value by travel after the drag threshold is crossed. */
    ByDistance,
}

/** The gesture state machine for one pointer sequence on a seek bar. */
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
            // For MOVE_BY_DISTANCE the event that crosses the touch slop does not yet change
            // the value; the delta tracking starts from the next event.
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
        // A tap only triggers a jump when the up event lands inside the view bounds.
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
