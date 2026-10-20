// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * The content that a COUI blur surface samples.
 *
 * On ColorOS the material is a **backdrop filter**: `RenderNode.setBackgroundRenderEffect` makes HWUI
 * blur whatever was painted *behind* the view, so the view never sees a texture - the framework does.
 * Compose has no such entry point, so the page behind the surface is recorded into a
 * [GraphicsLayer] and the blur is applied to that layer instead. That reproduces the material math
 * and, unlike a screenshot, follows every Compose transform (scroll, predictive back) for free.
 *
 * Put [Modifier.couiBlurSource] on the container whose content should be visible through the blur,
 * then hand the instance to [CouiBlurSurface].
 */
@Stable
class CouiBlurBackdrop internal constructor(
    internal val layer: GraphicsLayer,
) {
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

/** Creates a [CouiBlurBackdrop] backed by a fresh [GraphicsLayer]. */
@Composable
fun rememberCouiBlurBackdrop(): CouiBlurBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { CouiBlurBackdrop(layer) }
}

/** Records this composable's content into [backdrop] on every draw. */
fun Modifier.couiBlurSource(backdrop: CouiBlurBackdrop): Modifier = this then CouiBlurSourceElement(backdrop)

private class CouiBlurSourceElement(
    private val backdrop: CouiBlurBackdrop,
) : ModifierNodeElement<CouiBlurSourceNode>() {
    override fun create(): CouiBlurSourceNode = CouiBlurSourceNode(backdrop)

    override fun update(node: CouiBlurSourceNode) {
        if (node.backdrop !== backdrop) {
            node.backdrop.coordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "couiBlurSource"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean = other is CouiBlurSourceElement && other.backdrop === backdrop

    override fun hashCode(): Int = System.identityHashCode(backdrop)
}

private class CouiBlurSourceNode(
    var backdrop: CouiBlurBackdrop,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        backdrop.layer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
            this@draw.drawContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) backdrop.coordinates = coordinates
    }

    override fun onDetach() {
        backdrop.coordinates = null
    }
}

/**
 * Draws the recorded backdrop so that the pixel under [consumer] lands under the current origin.
 *
 * Both nodes live in the same Compose scene, so the relative offset is transform-free and stays
 * correct while the scene scales.
 */
internal fun DrawScope.drawCouiBlurBackdrop(
    backdrop: CouiBlurBackdrop,
    consumer: LayoutCoordinates?,
) {
    val layerCoordinates = backdrop.coordinates ?: return
    val consumerCoordinates = consumer ?: return
    val offset =
        try {
            layerCoordinates.localPositionOf(consumerCoordinates)
        } catch (_: Exception) {
            consumerCoordinates.positionInWindow() - layerCoordinates.positionInWindow()
        }
    withTransform({ translate(-offset.x, -offset.y) }) {
        drawLayer(backdrop.layer)
    }
}