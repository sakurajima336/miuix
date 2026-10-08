// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

/**
 * The springs and scale range behind [couiPressFeedback].
 *
 * The defaults come from `PressEngineConfig`: a short, lightly bouncy spring for the press and a
 * slower, much bouncier one for the release, which is what gives COUI controls their
 * characteristic "snap back".
 *
 * @param pressSpring The spring used while the finger is down.
 * @param releaseSpring The spring used when the finger lifts.
 * @param minScale The scale applied to the smallest surfaces.
 * @param maxScale The scale applied to the largest surfaces. See [couiPressScale] for why a
 *   larger surface shrinks *less*.
 */
@Immutable
data class CouiPressStyle(
    val pressSpring: CouiSpringData,
    val releaseSpring: CouiSpringData,
    val minScale: Float,
    val maxScale: Float,
) {
    companion object {
        /** `PressEngineConfig`'s defaults. */
        val Default: CouiPressStyle = CouiPressStyle(
            pressSpring = CouiSpringData(
                bounce = CouiPressEngineDefaults.BouncePress,
                response = CouiPressEngineDefaults.ResponsePress,
            ),
            releaseSpring = CouiSpringData(
                bounce = CouiPressEngineDefaults.BounceRelease,
                response = CouiPressEngineDefaults.ResponseRelease,
            ),
            minScale = CouiPressEngineDefaults.MinScale,
            maxScale = CouiPressEngineDefaults.MaxScale,
        )

        /** `PressParams`' jelly springs: a crisper press, a springier release. */
        val Jelly: CouiPressStyle = CouiPressStyle(
            pressSpring = CouiPressSprings.JellyPress,
            releaseSpring = CouiPressSprings.JellyHandUp,
            minScale = CouiPressScaleRange.ScaleRangeMin,
            maxScale = CouiPressScaleRange.ScaleRangeMax,
        )

        /** `PressParams`' candy springs. */
        val Candy: CouiPressStyle = CouiPressStyle(
            pressSpring = CouiPressSprings.CandyPress,
            releaseSpring = CouiPressSprings.CandyHandUp,
            minScale = CouiPressScaleRange.ScaleRangeMin,
            maxScale = CouiPressScaleRange.ScaleRangeMax,
        )
    }
}

/**
 * Applies COUI's press feedback: the surface scales down while held, then springs back on
 * release.
 *
 * How far it shrinks is not a constant - it comes from [couiPressScale], which reads the actual
 * laid-out size, so a small button and a large card get visibly different amounts of movement
 * without either of them having to say so.
 *
 * The caller is responsible for feeding [interactionSource] the same source it gives to
 * `clickable`, otherwise the press state will never arrive.
 *
 * @param interactionSource The source whose press state drives the animation. A private one is
 *   created when omitted, which is only useful if something else also reports presses into it.
 * @param enabled When `false` the modifier does nothing at all.
 * @param style The springs and scale range to use.
 */
@Composable
fun Modifier.couiPressFeedback(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    style: CouiPressStyle = CouiPressStyle.Default,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val density = LocalDensity.current

    var size by remember { mutableStateOf(IntSize.Zero) }

    val targetScale = if (!enabled || !pressed) {
        1f
    } else {
        val widthDp = with(density) { size.width.toDp().value }
        val heightDp = with(density) { size.height.toDp().value }
        if (widthDp <= 0f || heightDp <= 0f) {
            1f
        } else {
            couiPressScale(
                width = widthDp,
                height = heightDp,
                minScale = style.minScale,
                maxScale = style.maxScale,
            )
        }
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = (if (pressed) style.pressSpring else style.releaseSpring)
            .toSpringSpec(CouiMinVisibleChange.Scale),
        label = "couiPressScale",
    )

    return this
        .onSizeChanged { size = it }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}