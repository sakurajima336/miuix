// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.runtime.Composable

/**
 * COUI's haptic vocabulary.
 *
 * The real switch fires exactly one pulse per user gesture: `handleActionUp` arms
 * `mEnableHaptICFeedback`, `setChecked` calls `performFeedBack()` at the end, and that method
 * **consumes** the flag - so a tap vibrates once, a drag release vibrates once, and a
 * programmatic `setChecked` never vibrates.
 *
 * The constant itself is OPlus-specific (`OplusHapticFeedbackConstants.GRANULAR_SHORT_VIBRATE`,
 * 0x12e), which has no counterpart in Compose's `HapticFeedbackType`, so the actual pulse is
 * platform-specific: see the androidMain actual, which calls
 * `View.performHapticFeedback(0x12e)` directly. Every other platform is a no-op until someone
 * wires an equivalent.
 */
interface CouiHaptics {
    /**
     * `COUISwitch.performFeedBack()` - `GRANULAR_SHORT_VIBRATE` on ColorOS, the short granular
     * pulse a real switch makes.
     */
    fun granularShort()

    /** A plain tick, for callers that want lighter feedback. */
    fun tick()

    /** The no-op used on platforms without a haptics backend. */
    companion object NoOp : CouiHaptics {
        override fun granularShort() = Unit
        override fun tick() = Unit
    }
}

/**
 * The platform haptics handle. Android returns a [CouiHaptics] backed by
 * `View.performHapticFeedback`; every other target returns [CouiHaptics.NoOp].
 */
@Composable
expect fun rememberCouiHaptics(): CouiHaptics