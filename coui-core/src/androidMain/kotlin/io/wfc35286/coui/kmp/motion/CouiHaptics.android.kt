// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Android actual of [CouiHaptics].
 *
 * The constant is `OplusHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE` (0x12e), the one
 * `COUISwitch.performFeedBack()` passes to `View.performHapticFeedback`. It only has the granular
 * meaning on ColorOS; elsewhere the framework treats the unknown id leniently (generic click), so
 * the call is safe on all Android devices.
 */
private class AndroidCouiHaptics(private val view: View) : CouiHaptics {
    override fun granularShort() {
        // 0x12e: OplusHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE - the pulse the real switch
        // fires at the end of setChecked(checked, fromUser = true).
        view.performHapticFeedback(0x12e)
    }

    override fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

/** Android actual: backed by [View.performHapticFeedback]. */
@Composable
actual fun rememberCouiHaptics(): CouiHaptics {
    val view = LocalView.current
    return remember(view) { AndroidCouiHaptics(view) }
}