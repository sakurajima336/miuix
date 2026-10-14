
package io.wfc35286.coui.kmp.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.shape.CouiSquircleShape

@Composable
internal actual fun CouiPopupRootAlpha(visual: CouiPopupVisualState) = Unit

internal actual fun createCouiPopupShape(radius: Dp, useBackgroundBlur: Boolean): Shape = CouiSquircleShape(radius)

@Composable
internal actual fun CouiPopupMaterial(
    dark: Boolean,
    useBackgroundBlur: Boolean,
    fallbackColor: Color,
    shape: Shape,
    visual: CouiPopupVisualState,
    modifier: Modifier,
) {
    val revealedShape = CouiPopupRevealShape(shape, visual.contentReveal)
    Box(
        modifier.graphicsLayer {
            translationX = visual.translation.x
            translationY = visual.translation.y
            scaleX = visual.scaleX
            scaleY = visual.scaleY
            alpha = visual.alpha
        }.shadow(30.dp, revealedShape, clip = false).background(fallbackColor, revealedShape),
    )
}

internal actual fun DrawScope.drawCouiPopupMask(bounds: Rect, color: Color, radius: Float, weight: Float) {
    drawRoundRect(color, Offset(bounds.left, bounds.top), Size(bounds.width, bounds.height), CornerRadius(radius))
}

@Composable
internal actual fun couiPopupTouchExplorationEnabled(): Boolean = false
