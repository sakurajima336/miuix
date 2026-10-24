// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.ui.text.TextStyle


internal actual fun couiPopupPlatformTextStyle(style: TextStyle): TextStyle = style

@androidx.compose.runtime.Composable
internal actual fun couiPopupVisibleWindowBounds(): androidx.compose.ui.unit.IntRect? = null
