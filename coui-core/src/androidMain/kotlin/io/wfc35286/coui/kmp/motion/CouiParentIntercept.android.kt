// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Android actual of [CouiParentIntercept].
 *
 * `parent` is read on every call rather than captured, because the host may still be detached (no
 * parent yet) when the composable is first measured.
 */
private class AndroidCouiParentIntercept(private val view: View) : CouiParentIntercept {
    override fun setDisallowIntercept(disallow: Boolean) {
        view.parent?.requestDisallowInterceptTouchEvent(disallow)
    }
}

/** Android actual: backed by `ViewParent.requestDisallowInterceptTouchEvent`. */
@Composable
actual fun rememberCouiParentIntercept(): CouiParentIntercept {
    val view = LocalView.current
    return remember(view) { AndroidCouiParentIntercept(view) }
}