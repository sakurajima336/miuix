// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.runtime.Composable


/**
 * Hooks the platform's predictive back gesture for the COUI menu.
 *
 * The menu lives in its own window (a Compose `Popup`), and a back gesture is delivered to the
 * **focused window**, not to the Activity: the Navigation Event dispatcher that
 * `LocalNavigationEventDispatcherOwner` exposes belongs to the Activity, so a `NavigationBackHandler`
 * registered from inside the popup content never sees the gesture (verified on device: swiping back
 * with the menu open did nothing at all). Each platform therefore has to attach to the window the
 * menu is actually in:
 *
 *  * Android: the popup window's own `OnBackInvokedDispatcher` (`View.findOnBackInvokedDispatcher`),
 *    which also reports the gesture progress on API 34+;
 *  * other platforms: the Navigation Event API, which is the same dispatcher there.
 *
 * The ColorOS menu has no back handling of its own, so this handler is our own adaptation.
 */
@Composable
internal expect fun CouiPopupBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onCompleted: () -> Unit,
)
