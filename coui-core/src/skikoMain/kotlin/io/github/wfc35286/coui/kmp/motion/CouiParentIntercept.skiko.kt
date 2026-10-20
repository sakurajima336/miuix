// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion

import androidx.compose.runtime.Composable

/**
 * Skiko actual of [CouiParentIntercept].
 *
 * Skiko hosts do not use Android's `ViewParent` interception model, so there is nothing to do:
 * consuming the pointer change is already enough to keep a Compose `scrollable` ancestor out.
 */
@Composable
actual fun rememberCouiParentIntercept(): CouiParentIntercept = CouiParentIntercept.NoOp