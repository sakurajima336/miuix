
package io.github.wfc35286.coui.kmp.component

import androidx.compose.ui.text.TextStyle

internal actual fun couiPopupPlatformTextStyle(style: TextStyle): TextStyle = style

@androidx.compose.runtime.Composable
internal actual fun couiPopupVisibleWindowBounds(): androidx.compose.ui.unit.IntRect? = null
