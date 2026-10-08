// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.wfc35286.coui.kmp.theme.CouiTheme
import kotlin.math.pow
import kotlin.math.roundToInt

/** ListSelectedItemLayout's COUIMaskEffectDrawable(type=container), with scale disabled. */
@Composable
internal fun Modifier.couiStateMask(source: MutableInteractionSource, enabled: Boolean): Modifier {
    val progress = remember { Animatable(0f) }
    var entered by remember { mutableStateOf(false) }
    var finishEnter by remember { mutableStateOf(false) }
    val currentEntered by rememberUpdatedState(entered)
    val currentFinishEnter by rememberUpdatedState(finishEnter)
    val spec = remember { CouiSpringData(bounce = 0f, response = 0.3f).toSpringSpec(0.0001f) }
    LaunchedEffect(source, enabled) {
        entered = false
        finishEnter = false
        if (enabled) {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        entered = true
                        finishEnter = false
                    }

                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        entered = false
                        // animateToProgressUntil(0, 7000): continue entering before returning.
                        finishEnter = progress.value < 0.7f
                    }
                }
            }
        }
    }
    // One animation driver. Retargeting preserves velocity and cannot race another writer.
    LaunchedEffect(enabled, entered, finishEnter) {
        if (!enabled) {
            progress.snapTo(0f)
        } else {
            progress.animateTo(if (entered || finishEnter) 1f else 0f, spec) {
                if (!currentEntered && currentFinishEnter && value > 0.7f) finishEnter = false
            }
        }
    }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val hover by animateFloatAsState(if (enabled && hovered) 1f else 0f, spec, label = "couiHoverMask")
    val focus by animateFloatAsState(if (enabled && focused) 1f else 0f, spec, label = "couiFocusMask")
    val colors = CouiTheme.colors
    return drawBehind {
        if (hover > 0f) drawRect(couiMaskColor(colors.hover, hover))
        if (focus > 0f) drawRect(couiMaskColor(colors.focus, focus))
        if (progress.value > 0f) drawRect(couiMaskColor(colors.press, progress.value))
    }
}

// StateEffectAnimator uses android.animation.ArgbEvaluator from transparent black.
// Gamma-correct RGB interpolation matters for the white mask in the dark theme.
private fun couiMaskColor(end: Color, fraction: Float): Color {
    val argb = end.toArgb()
    val p = fraction.coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val c = (argb ushr shift) and 255
        return (255f * ((c / 255f).pow(2.2f) * p).pow(1f / 2.2f)).roundToInt().coerceIn(0, 255)
    }
    val alpha = (((argb ushr 24) and 255) * p).roundToInt()
    return Color((alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0))
}
