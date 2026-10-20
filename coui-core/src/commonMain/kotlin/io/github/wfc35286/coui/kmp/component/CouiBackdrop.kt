// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.roundToInt

/**
 * A Compose-side capture of the page that sits *behind* a floating surface.
 *
 * This exists because the ColorOS material is a **window-level backdrop filter**
 * (`RenderNode.setBackgroundRenderEffect` -> Oplus HWUI `OplusUniBackdropFilterDrawable`), which the
 * official bar can use only because it is a plain `View` inside the Activity window: predictive back
 * transforms the *window*, so the sampler always sees the full page.
 *
 * In a Compose host the same effect has to be produced inside Compose, otherwise the interop host
 * stays put while the scene shrinks, samples past the scene layer and HWUI fills the rest with
 * black. Recording the page into a [GraphicsLayer] and blurring that layer reproduces the material
 * math *and* follows every Compose transform - including predictive back - by construction.
 *
 * Use [Modifier.couiBackdrop] on the container whose content should be visible through the pill and
 * pass the instance to components that accept one (for example
 * `CouiBottomFloatingNavigationBar(backdrop = ...)`).
 */
@Stable
class CouiBackdrop internal constructor(
    internal val layer: GraphicsLayer,
) {
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

/** Creates a [CouiBackdrop] backed by a fresh [GraphicsLayer]. */
@Composable
fun rememberCouiBackdrop(): CouiBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { CouiBackdrop(layer) }
}

/**
 * The default [CouiBackdrop] for floating surfaces in this subtree.
 *
 * Provide it once around the page (together with [Modifier.couiBackdrop] on that page) and every
 * COUI floating component that accepts a backdrop picks it up. This is the **preferred material
 * path**: the blur is produced inside Compose, so it survives Compose scene transforms
 * (predictive back) and works without the Oplus framework.
 */
val LocalCouiBackdrop: androidx.compose.runtime.ProvidableCompositionLocal<CouiBackdrop?> =
    androidx.compose.runtime.staticCompositionLocalOf { null }

/** Captures this composable's content into [backdrop] on every draw. */
fun Modifier.couiBackdrop(backdrop: CouiBackdrop): Modifier = this then CouiBackdropElement(backdrop)

private class CouiBackdropElement(
    private val backdrop: CouiBackdrop,
) : ModifierNodeElement<CouiBackdropNode>() {
    override fun create(): CouiBackdropNode = CouiBackdropNode(backdrop)

    override fun update(node: CouiBackdropNode) {
        if (node.backdrop !== backdrop) {
            node.backdrop.coordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "couiBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean = other is CouiBackdropElement && other.backdrop === backdrop

    override fun hashCode(): Int = System.identityHashCode(backdrop)
}

private class CouiBackdropNode(
    var backdrop: CouiBackdrop,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        // Re-record every frame: the layer must hold exactly what is on screen behind the pill,
        // including the navigation scene's current transform.
        backdrop.layer.record(size = size.toIntSizeCompat()) { this@draw.drawContent() }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) backdrop.coordinates = coordinates
    }

    override fun onDetach() {
        backdrop.coordinates = null
    }
}

private fun androidx.compose.ui.geometry.Size.toIntSizeCompat() =
    androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())

/**
 * Draws the recorded page so that it lands **1:1 on screen** under the pill, whatever the pill's
 * own transform is doing.
 *
 * [slot] is the pill's *untransformed* box (its layout slot). The drag and the press/deform scale
 * are applied inside that slot, which means:
 *
 * - the page-space position of the pill's untransformed top-left is `localPositionOf(slot)` and
 *   never changes while the pill is dragged - so it is a trustworthy anchor, unlike the transformed
 *   node's own coordinates;
 * - the drawing this runs in is the pill's **transformed** space: point `p` lands on screen at
 *   `base + t + c + s * (p - c)` with `c` the pill's centre. A real backdrop filter samples the
 *   page in *device* space and only clips the result, so to reproduce it we invert that:
 *
 *   ```
 *   p = (P - base - t - c) / s + c   ⟺   p = (P + d) / s,  d = -(base + t) - c * (1 - s)
 *   ```
 *
 *   i.e. `scale(1/s)` followed by `translate(d)`. Without this the sampled page scales and slides
 *   with the pill: while the bar is dragged it looks like the picture inside is glued to it and
 *   "breathes" with the press - the artefact this whole rewrite exists to remove.
 */
internal fun DrawScope.drawCouiBackdropLayer(
    backdrop: CouiBackdrop,
    origin: LayoutCoordinates?,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    val layerCoordinates = backdrop.coordinates ?: return
    val originCoordinates = origin ?: return
    // Where the **drawing origin** (this layer's local (0,0)) actually sits in the page. Measured,
    // not derived: the layer is an expanded child placed by a layout modifier inside the pill's
    // transformed box, and guessing where that placement ends up (or how many times the expansion
    // offset applies) got it wrong by exactly one margin - the backdrop then sampled 37px off,
    // which is what "偏移了" was. `localPositionOf` reports the *visual* position, so it already
    // contains the drag offset and the press/deform scale.
    val o = try {
        layerCoordinates.localPositionOf(originCoordinates)
    } catch (_: Exception) {
        originCoordinates.positionInWindow() - layerCoordinates.positionInWindow()
    }
    // A degenerate scale (or the flat fallback) must never divide by ~0.
    val sx = if (scaleX.isFinite() && scaleX > 0.01f) scaleX else 1f
    val sy = if (scaleY.isFinite() && scaleY > 0.01f) scaleY else 1f
    // The drawing space is `screen(q) = origin + s * q`, so the page lands 1:1 when `q = (P - o)/s`.
    withTransform({
        scale(1f / sx, 1f / sy, pivot = Offset.Zero)
        translate(-o.x, -o.y)
    }) {
        drawLayer(backdrop.layer)
    }
}

/**
 * Temporary switch for the drag-anchoring diagnostics: prints the sampling transform next to the
 * slot anchor and the drag amount, which is what tells "the page is pinned to the pill" apart from
 * "the page stays on screen" without having to eyeball a screenshot.
 */
internal object CouiBackdropProbe {
    const val enabled: Boolean = false
}
