// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.blur

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * A surface painted with the COUI (ColorOS) material: the backdrop is blurred with
 * `RenderEffect.createBlurEffect(r, r, MIRROR)` and washed with the two tint layers of [params],
 * then clipped to [shape].
 *
 * Usage:
 * ```
 * val backdrop = rememberCouiBlurBackdrop()
 * Box(Modifier.couiBlurSource(backdrop)) {          // page behind
 *     PageContent()
 *     CouiBlurSurface(
 *         modifier = Modifier.align(Alignment.BottomCenter).height(56.dp),
 *         backdrop = backdrop,
 *         params = CouiBlurPresets.BottomBarLight,
 *         shape = CouiCardShape(),
 *     ) { /* content on top of the material */ }
 * }
 * ```
 *
 * The official widget gets this from the framework (`OplusViewBackgroundRenderEffect`), which blurs
 * whatever is painted behind the view. Compose has no backdrop filter, so the recorded
 * [CouiBlurBackdrop] is blurred instead - same maths, and it keeps following the scene's transforms.
 */
@Composable
fun CouiBlurSurface(
    backdrop: CouiBlurBackdrop,
    params: CouiBlurParams,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    var consumer by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val clipped = if (shape != null) modifier.clip(shape) else modifier
    Box(clipped) {
        CouiBlurMaterial(
            params = params,
            modifier =
                Modifier
                    .matchParentSize()
                    .onGloballyPositioned { consumer = it },
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCouiBlurBackdrop(backdrop, consumer)
            }
        }
        content()
    }
}

/**
 * Applies the material's [RenderEffect] chain to [content].
 *
 * Android: `createBlurEffect` + two `createColorFilterEffect` layers, exactly like
 * `COUIMaterialBlurEffect$Companion.setViewBlurEffect`. Other targets fall back to a plain blur (or
 * nothing when the platform has no effect pipeline).
 */
@Composable
expect fun CouiBlurMaterial(
    params: CouiBlurParams,
    modifier: Modifier,
    content: @Composable () -> Unit,
)