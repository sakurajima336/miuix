// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.wfc35286.coui.kmp.motion.CouiSpringData
import io.wfc35286.coui.kmp.motion.toSpringSpec

/**
 * The press state shared by the rows of one [CouiCard].
 *
 * `COUIRecyclerDividerManager` watches the list's touch events and, while a row is held down,
 * springs the alpha of two dividers towards zero: the one after the pressed row and the one after
 * the row before it. Releasing - or moving far enough to start a scroll - springs them back.
 *
 * A Compose card has no list to watch, so the rows report their own press state here instead and
 * the dividers read it while drawing. Row indices come from composition order, which matches the
 * layout order of the rows inside the card; cards with a dynamic, reordered row list would need
 * an explicit index.
 */
@Stable
internal class CouiRowGroupState {
    private val pressedRows = mutableStateMapOf<Int, Boolean>()

    private var nextIndex = 0

    /** The animated multiplier applied to the affected dividers. `1` is the resting state. */
    val dividerAlpha: Animatable<Float, *> = Animatable(1f)

    /** `COUIRecyclerDividerManager.SPRING_RESPONSE` / `SPRING_BOUNCE`. */
    val dividerAlphaSpec = CouiSpringData(bounce = 0f, response = 0.15f).toSpringSpec(0.001f)

    fun allocateIndex(): Int = nextIndex++

    fun setRowPressed(index: Int, pressed: Boolean) {
        if (pressed) {
            pressedRows[index] = true
        } else {
            pressedRows.remove(index)
        }
    }

    fun anyRowPressed(): Boolean = pressedRows.isNotEmpty()

    /** Springs the shared divider alpha towards `0` while a row is held, and back to `1` after. */
    suspend fun syncDividerAlpha() {
        dividerAlpha.animateTo(if (anyRowPressed()) 0f else 1f, dividerAlphaSpec)
    }

    /** The lowest pressed row index, or `-1`. Mirrors the single `mPressDividerChildIndex`. */
    fun pressedIndex(): Int = pressedRows.keys.minOrNull() ?: -1

    /**
     * Whether the divider drawn under [rowIndex] is one of the two the manager fades out:
     * the line under the pressed row, and the line under the row above it.
     */
    fun isDividerAffected(rowIndex: Int): Boolean {
        val pressed = pressedIndex()
        return pressed >= 0 && (rowIndex == pressed || rowIndex == pressed - 1)
    }
}

internal val LocalCouiRowGroup = staticCompositionLocalOf<CouiRowGroupState?> { null }
