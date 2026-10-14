// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.runtime.Composable

/** No haptics backend on this platform yet - see the androidMain actual for the real thing. */
@Composable
actual fun rememberCouiHaptics(): CouiHaptics = CouiHaptics.NoOp
