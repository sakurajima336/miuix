// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTypography
import kotlinx.coroutines.launch

/** The motion and chrome values of the snackbar. */
object CouiSnackBarDefaults {
    /** The spring that shows the bar: 0.3s response, no bounce. */
    val ShowSpring = CouiSpringData(bounce = 0f, response = 0.3f)

    /** The spring that hides the bar: a drier 0.25s response, no bounce. */
    val HideSpring = CouiSpringData(bounce = 0f, response = 0.25f)

    /** Swiped further than this fraction of the bar's width, a release dismisses it. */
    const val DismissSwipeFraction = 1f / 8f

    /** The corner radius of the bar; fully rounded at its height. */
    val CornerRadius = 24.dp

    /** The message style. */
    val messageStyle = CouiTypography.body
}

/**
 * A [CouiSnackBar] component with COUI style.
 *
 * A transient message bar for the bottom of the screen. It rises on a 0.3s critically damped
 * spring and drops away on a drier 0.25s one; swipe it horizontally past one eighth of its
 * width and releasing dismisses it, otherwise it springs back to centre.
 *
 * The bar is a plain composable, not a window: the host places it at the bottom of its layout
 * and drives it with [visible].
 *
 * @param visible Whether the bar is shown. Setting it to `false` plays the drop travel and
 *   then removes the bar.
 * @param message The message to show.
 * @param onDismissRequest Called when the user swipes the bar away.
 * @param modifier The modifier to apply to the bar.
 */
@Composable
fun CouiSnackBar(
    visible: Boolean,
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var composed by remember { mutableStateOf(visible) }
    val progress = remember { Animatable(0f) }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            composed = true
            swipeOffset.snapTo(0f)
            progress.animateTo(1f, CouiSnackBarDefaults.ShowSpring.toSpringSpec())
        } else if (composed) {
            progress.animateTo(0f, CouiSnackBarDefaults.HideSpring.toSpringSpec())
            composed = false
        }
    }
    if (!composed) return

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .graphicsLayer {
                    translationY = (1f - progress.value) * (size.height + 32.dp.toPx())
                    translationX = swipeOffset.value
                    alpha = progress.value
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (swipeOffset.value + dragAmount).coerceIn(-barWidthPx, barWidthPx)
                            scope.launch { swipeOffset.snapTo(next) }
                        },
                        onDragEnd = {
                            scope.launch {
                                val threshold = barWidthPx * CouiSnackBarDefaults.DismissSwipeFraction
                                if (kotlin.math.abs(swipeOffset.value) > threshold) {
                                    onDismissRequest()
                                } else {
                                    swipeOffset.animateTo(0f, CouiSnackBarDefaults.ShowSpring.toSpringSpec())
                                }
                            }
                        },
                    )
                }
                .clip(RoundedCornerShape(CouiSnackBarDefaults.CornerRadius))
                .background(CouiTheme.colors.backgroundWithCard)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            CouiText(
                text = message,
                style = CouiSnackBarDefaults.messageStyle,
                color = CouiTheme.colors.labelPrimary,
            )
        }
    }
}
