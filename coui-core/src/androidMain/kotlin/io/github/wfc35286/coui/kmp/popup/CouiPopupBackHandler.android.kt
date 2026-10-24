// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView


/**
 * Android implementation of [CouiPopupBackHandler]: registers directly on the dispatcher of the popup
 * window the menu lives in.
 *
 * Three things have to line up, each verified on device (ColorOS 17, gesture navigation):
 *
 * 1. **The window.** A back gesture is delivered to the dispatcher of the *focused* window, and the
 *    menu's Compose `Popup` is its own window (`ty=APPLICATION_SUB_PANEL`, and `dumpsys window` shows
 *    it holding `mCurrentFocus`). `LocalView.current` inside the popup content is a view of that
 *    window, so `findOnBackInvokedDispatcher()` returns the right dispatcher; the Activity's window
 *    dispatcher is registered as well, because a host whose popup is not focusable delivers the
 *    gesture there instead.
 * 2. **The priority.** Compose's `Popup` registers its own `onDismissRequest` callback at
 *    `PRIORITY_OVERLAY` (system log: `OnBackInvokedCallbackInfo{mPriority=1000000,
 *    mIsAnimationCallback=false}`). The last callback at a given priority wins, and only an
 *    animation callback ever receives `onBackStarted`/`onBackProgressed` - with a lower priority the
 *    menu was dismissed by Compose on release and dragging did nothing.
 * 3. **The callback type.** `OnBackInvokedCallback` only has `onBackInvoked()`; the gesture progress
 *    and cancellation live in `OnBackAnimationCallback` (API 34+). On API 33 the menu still closes
 *    on back, just without a preview.
 */
@Composable
internal actual fun CouiPopupBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onCompleted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    val currentProgress by rememberUpdatedState(onProgress)
    val currentCancelled by rememberUpdatedState(onCancelled)
    val currentCompleted by rememberUpdatedState(onCompleted)
    DisposableEffect(view, activity, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        var registered = false
        var dispatchers: List<OnBackInvokedDispatcher> = emptyList()
        var callback: OnBackInvokedCallback? = null
        fun register() {
            if (registered) return
            val found =
                buildList {
                    view.findOnBackInvokedDispatcher()?.let(::add)
                    activity?.window?.decorView?.findOnBackInvokedDispatcher()?.let(::add)
                }.distinct()
            if (found.isEmpty()) return
            val cb =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    object : OnBackAnimationCallback {
                        override fun onBackStarted(backEvent: BackEvent) {
                            currentProgress(0f)
                        }

                        override fun onBackProgressed(backEvent: BackEvent) {
                            currentProgress(backEvent.progress)
                        }

                        override fun onBackCancelled() {
                            currentCancelled()
                        }

                        override fun onBackInvoked() {
                            currentCompleted()
                        }
                    }
                } else {
                    OnBackInvokedCallback { currentCompleted() }
                }
            found.forEach {
                it.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb)
            }
            registered = true
            dispatchers = found
            callback = cb
        }
        val attachListener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = register()

                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        view.addOnAttachStateChangeListener(attachListener)
        register()
        // Re-register one frame later (unregister + register) so a callback Compose adds from its own
        // attach handling cannot stay on top of ours.
        view.post {
            val cb = callback
            if (registered && cb != null) {
                dispatchers.forEach { it.unregisterOnBackInvokedCallback(cb) }
                registered = false
            }
            register()
        }
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            val cb = callback
            if (cb != null) dispatchers.forEach { it.unregisterOnBackInvokedCallback(cb) }
            registered = false
        }
    }
}

/** The Activity hosting this composition, unwrapping the popup's context wrappers. */
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
