// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.motion
import androidx.compose.runtime.Composable


/**
 * A hook that lets a control keep a drag gesture when it is hosted inside a scroll container.
 *
 * Consuming the pointer change already stops a Compose `scrollable` ancestor from claiming the
 * gesture, but this control is meant to be dropped into *any* host, including a View-based
 * `ScrollView`. In that situation `consume()` does nothing for the parent, and only
 * `requestDisallowInterceptTouchEvent` on the parent will keep the drag.
 *
 * The control calls this as soon as a MOVE event passes the touch slop; from that moment it owns
 * the gesture and the enclosing scroll container can no longer take it away.
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
