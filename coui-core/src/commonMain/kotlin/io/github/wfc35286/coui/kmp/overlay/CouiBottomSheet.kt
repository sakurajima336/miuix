// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import kotlinx.coroutines.launch

/** The motion and chrome values of the bottom sheet. */
object CouiBottomSheetDefaults {
    /** The spring that shows the sheet: slow and heavy, no bounce. */
    val ShowSpring = CouiSpringData(bounce = 0f, response = 0.45f)

    /** The spring that hides a short sheet: quick and dry, no bounce. */
    val HideSpringSmall = CouiSpringData(bounce = 0f, response = 0.18f)

    /** The spring that hides a tall sheet (at least half the screen): no bounce. */
    val HideSpringLarge = CouiSpringData(bounce = 0f, response = 0.37f)

    /** The scale of the resting sheet; it grows to 1.0 as the sheet rises. */
    const val CollapsedScale = 0.8f

    /** The visible-change threshold of the show spring, in pixels. */
    const val ShowThresholdPx = 1f

    /**
     * The visible-change threshold of the hide spring, in pixels: a larger threshold lets the
     * dismiss spring settle sooner.
     */
    const val HideThresholdPx = 10f

    /** Dragged further down than this fraction of the panel, a release dismisses the sheet. */
    const val DismissDragFraction = 0.5f

    /** The dim the sheet casts on the content behind it, at full progress. */
    val ScrimColor = Color(0x80000000)

    /** The radius of the sheet's top corners. */
    val TopCornerRadius = 32.dp

    /** The elevation the sheet floats at. */
    val Elevation = 17.dp

    /** The gap the sheet keeps below the top of the screen. */
    val TopMargin = 40.dp

    /** The width of the grab bar at the top of the sheet. */
    val DragBarWidth = 28.dp

    /** The height of the grab bar. */
    val DragBarHeight = 4.dp

    /** The gap between the sheet's top edge and the grab bar. */
    val DragBarMarginTop = 8.dp
}

/**
 * A [CouiBottomSheet] component with COUI style.
 *
 * A bottom panel that rises over a dimmed page: the show travel is a 0.45s response, critically
 * damped spring; the dismiss travel is a drier 0.18s one that settles sooner. While the sheet
 * travels it is scaled between 0.8 and 1.0 in step with its progress, so the panel appears to
 * grow out of the bottom edge. Dragging the panel down follows the finger; releasing past half
 * the panel height dismisses it, otherwise it springs back.
 *
 * The sheet is focusable, so the back gesture and a tap on the dimmed area both dismiss it
 * through [onDismissRequest].
 *
 * @param visible Whether the sheet is shown. Setting it to `false` plays the dismiss travel and
 *   then removes the sheet.
 * @param onDismissRequest Called when the user dismisses the sheet - back gesture, tap on the
 *   scrim, or a drag released past the dismiss fraction.
 * @param modifier The modifier to apply to the sheet surface.
 * @param sheetContent The content of the sheet panel.
 */
@Composable
fun CouiBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    var composed by remember { mutableStateOf(visible) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var panelHeightPx by remember { mutableFloatStateOf(0f) }
    var parentHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            composed = true
            progress.animateTo(
                1f,
                CouiBottomSheetDefaults.ShowSpring.toSpringSpec(CouiBottomSheetDefaults.ShowThresholdPx),
            )
        } else if (composed) {
            // Tall sheets drop away on the slower spring, short ones on the quick one.
            val hideSpring = if (panelHeightPx >= parentHeightPx / 2f) {
                CouiBottomSheetDefaults.HideSpringLarge
            } else {
                CouiBottomSheetDefaults.HideSpringSmall
            }
            progress.animateTo(0f, hideSpring.toSpringSpec(CouiBottomSheetDefaults.HideThresholdPx))
            composed = false
        }
    }
    if (!composed) return

    val panelColor = CouiTheme.colors.backgroundWithCard

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().onSizeChanged { parentHeightPx = it.height.toFloat() }) {
            val panelMaxHeight = maxHeight - CouiBottomSheetDefaults.TopMargin
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(CouiBottomSheetDefaults.ScrimColor),
            )
            Box(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = panelMaxHeight)
                    .onSizeChanged { panelHeightPx = it.height.toFloat() }
                    .graphicsLayer {
                        val p = progress.value
                        translationY = (1f - p) * panelHeightPx
                        val scale = CouiBottomSheetDefaults.CollapsedScale + (1f - CouiBottomSheetDefaults.CollapsedScale) * p
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = CouiBottomSheetDefaults.Elevation.toPx()
                        shape = RoundedCornerShape(
                            topStart = CouiBottomSheetDefaults.TopCornerRadius,
                            topEnd = CouiBottomSheetDefaults.TopCornerRadius,
                        )
                        clip = true
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (panelHeightPx > 0f) {
                                    val next = (progress.value - dragAmount / panelHeightPx).coerceIn(0f, 1f)
                                    scope.launch { progress.snapTo(next) }
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (progress.value < CouiBottomSheetDefaults.DismissDragFraction) {
                                        onDismissRequest()
                                    } else {
                                        progress.animateTo(
                                            1f,
                                            CouiBottomSheetDefaults.ShowSpring.toSpringSpec(CouiBottomSheetDefaults.ShowThresholdPx),
                                        )
                                    }
                                }
                            },
                        )
                    }
                    .background(panelColor),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // The grab bar: a 28x4dp capsule, 8dp below the sheet's top edge.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = CouiBottomSheetDefaults.DragBarMarginTop)
                            .size(width = CouiBottomSheetDefaults.DragBarWidth, height = CouiBottomSheetDefaults.DragBarHeight)
                            .clip(RoundedCornerShape(CouiBottomSheetDefaults.DragBarHeight / 2))
                            .background(CouiTheme.colors.divider),
                    )
                    Column(modifier = Modifier.fillMaxWidth(), content = sheetContent)
                }
            }
        }
    }
}
