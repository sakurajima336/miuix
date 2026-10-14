// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.runtime.Composable

/**
 * The Compose-side counterpart of `COUISeekBarCore.attemptClaimDrag()`:
 *
 * ```java
 * // COUISeekBarCore.startDrag()
 * private final void attemptClaimDrag() {
 *     ViewParent p = getParent();
 *     if (p != null) p.requestDisallowInterceptTouchEvent(true);
 * }
 * ```
 *
 * `startDrag()` runs from `onTouchEvent` as soon as a MOVE passes `mTouchSlop`, and from that
 * moment the bar owns the gesture - the enclosing scroll container can no longer take it away.
 *
 * Consuming the pointer change already stops a Compose `scrollable` ancestor from claiming the
 * gesture, but this control is meant to be dropped into *any* host, including a View-based
 * `ScrollView` (the real Settings app is one). In that situation `consume()` does nothing for the
 * parent, and only `requestDisallowInterceptTouchEvent` will keep the drag.
 */
interface CouiParentIntercept {
    /**
     * `requestDisallowInterceptTouchEvent(disallow)` on the host's parent.
     *
     * Pass `true` while the bar owns a drag and `false` once it is done. The framework clears the
     * flag on the next ACTION_DOWN anyway, so releasing it is only hygiene.
     */
    fun setDisallowIntercept(disallow: Boolean)

    /** The no-op used on platforms without a View parent. */
    companion object NoOp : CouiParentIntercept {
        override fun setDisallowIntercept(disallow: Boolean) = Unit
    }
}

/**
 * The platform handle. Android walks up to the host `View`'s parent; every other target returns
 * [CouiParentIntercept.NoOp].
 */
@Composable
expect fun rememberCouiParentIntercept(): CouiParentIntercept