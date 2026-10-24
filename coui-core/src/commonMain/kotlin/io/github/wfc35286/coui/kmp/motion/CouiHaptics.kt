// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import androidx.compose.runtime.Composable


/**
 * The haptic vocabulary of this library's controls.
 *
 * A control fires exactly one pulse per user gesture: the flag that arms feedback is set when the
 * gesture ends and **consumed** when the pulse fires - so a tap vibrates once, a drag release
 * vibrates once, and a programmatic state change never vibrates.
 *
 * The pulse itself is ColorOS-specific (`GRANULAR_SHORT_VIBRATE`, 0x12e), which has no counterpart
 * in Compose's `HapticFeedbackType`, so the actual pulse is platform-specific: see the androidMain
 * actual, which calls `View.performHapticFeedback(0x12e)` directly. Every other platform is a
 * no-op until someone wires an equivalent.
 */
interface CouiHaptics {
    /**
     * The short granular pulse (`GRANULAR_SHORT_VIBRATE` on ColorOS) that a real switch makes.
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
