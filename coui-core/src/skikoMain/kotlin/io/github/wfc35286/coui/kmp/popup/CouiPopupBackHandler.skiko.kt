// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Desktop/iOS/web implementation of [CouiPopupBackHandler].
 *
 * These platforms have no per-window back dispatcher, so the Navigation Event API used by the rest
 * of the project is enough here (and it is the only back source they have).
 */
@Composable
internal actual fun CouiPopupBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onCompleted: () -> Unit,
) {
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCancelled = onCancelled,
        onBackCompleted = onCompleted,
    )
    val transition = state.transitionState
    if (transition is NavigationEventTransitionState.InProgress) {
        val progress = transition.latestEvent.progress.coerceIn(0f, 1f)
        LaunchedEffect(progress) { onProgress(progress) }
    }
}