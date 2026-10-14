
package io.wfc35286.coui.kmp.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextMotion

internal actual fun couiPopupPlatformTextStyle(style: TextStyle): TextStyle = style.copy(
    platformStyle = PlatformTextStyle(includeFontPadding = true),
    // Official Compose guidance (Quick guide to Animations in Compose): "When animating scale,
    // translation, or rotation of text, set the `textMotion` parameter on TextStyle to
    // TextMotion.Animated. This ensures smoother transitions between text animations."
    // `TextMotion.Static` (the default) pixel-snaps the glyph positions for crispness; the popup's
    // rows are scaled and translated by the sub menu transition, so the run re-snapped the moment
    // the scale landed on exactly 1.0 - measured in a 60fps recording as a 1px step of the sub menu
    // owner's row (band top 406 -> 407, then staying there). `Animated` turns on subpixel text
    // positioning, so the glyph run is identical during and after the animation.
    textMotion = TextMotion.Animated,
)

/** PopupMenuLocateHelper.prepareWindowAndAnchor uses getWindowVisibleDisplayFrame. */
@androidx.compose.runtime.Composable
internal actual fun couiPopupVisibleWindowBounds(): androidx.compose.ui.unit.IntRect? {
    val view = androidx.compose.ui.platform.LocalView.current
    var frame by androidx.compose.runtime.remember(view) {
        androidx.compose.runtime.mutableStateOf<androidx.compose.ui.unit.IntRect?>(null)
    }
    androidx.compose.runtime.DisposableEffect(view) {
        val rect = android.graphics.Rect()
        val location = IntArray(2)
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            view.getWindowVisibleDisplayFrame(rect)
            view.rootView.getLocationOnScreen(location)
            val next = androidx.compose.ui.unit.IntRect(
                rect.left - location[0], rect.top - location[1],
                rect.right - location[0], rect.bottom - location[1],
            )
            if (next != frame) frame = next
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        listener.onGlobalLayout()
        onDispose {
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return frame
}
