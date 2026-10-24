// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.motion.CouiEasing
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextStyle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** The measured values of the tab strip, from the ColorOS tab layout resources. */
object CouiTabLayoutDefaults {
    /** The height of one tab row. */
    val TabHeight = 40.dp

    /** The height of the sliding indicator under the selected tab. */
    val IndicatorHeight = 2.dp

    /** The corner radius of the indicator; half its height, so it is a capsule. */
    val IndicatorCornerRadius = IndicatorHeight / 2

    /** The horizontal inset of the faint track line that runs the width of the strip. */
    val TrackInset = 24.dp

    /** The colour of the track line: black at 6% opacity. */
    val TrackColor = Color(0x0F000000)

    /** The widest one tab grows. */
    val TabMaxWidth = 128.dp

    /**
     * When the selection jumps over more than one tab, the indicator flies in from this distance
     * outside the target tab instead of travelling across.
     */
    val JumpFlyInDistance = 24.dp

    /** The longest the indicator travel takes. */
    const val MaxDurationMs = 300

    /** The base travel time before the per-tab distance is added. */
    const val BaseDurationMs = 150

    /** The travel time added per tab of distance. */
    const val PerTabDurationMs = 50

    /** The travel time for a move of [tabDistance] tabs. */
    fun travelDurationMs(tabDistance: Int): Int =
        minOf(MaxDurationMs, BaseDurationMs + abs(tabDistance) * PerTabDurationMs)

    /** The tab label style: 16sp, no weight change between states. */
    val labelStyle = CouiTextStyle(CouiFontSize.Scaled(16f))
}

/**
 * A [CouiTabLayout] component with COUI style.
 *
 * A text tab strip: a 2dp indicator slides between the selected tabs with a jelly travel -
 * 150ms plus 50ms per tab of distance, capped at 300ms, on the standard COUI movement curve -
 * while its width morphs between the two label widths and both labels cross-fade between the
 * secondary and primary neutral colours. A faint 2dp track line runs the width of the strip
 * under the indicator. A jump over more than one tab does not travel: the indicator flies in
 * from 24dp outside the target tab instead.
 *
 * @param tabs The tab labels, left to right.
 * @param selectedIndex The index of the selected tab.
 * @param onSelectedChange Called with the index of the tab the user tapped.
 * @param modifier The modifier to apply to the tab strip.
 * @param trackEnabled Whether the faint full-width track line is drawn under the indicator.
 */
@Composable
fun CouiTabLayout(
    tabs: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackEnabled: Boolean = true,
) {
    if (tabs.isEmpty()) return
    val colors = CouiTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val labelStyle = CouiTabLayoutDefaults.labelStyle.toTextStyle()
    val labelWidths = remember(tabs, labelStyle) {
        tabs.map { tab -> textMeasurer.measure(tab, labelStyle).size.width.toFloat() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidthPx = with(density) { maxWidth.toPx() }
        val indicator = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
        var lastSelected by remember { mutableIntStateOf(selectedIndex) }
        var initialized by remember { mutableStateOf(false) }

        fun leftOf(index: Int): Float =
            index * tabWidthPx + tabWidthPx / 2f - labelWidths.getOrElse(index) { 0f } / 2f

        LaunchedEffect(selectedIndex, tabWidthPx, labelWidths) {
            if (tabWidthPx <= 0f) return@LaunchedEffect
            val targetLeft = leftOf(selectedIndex)
            val targetWidth = labelWidths.getOrElse(selectedIndex) { 0f }
            if (!initialized) {
                initialized = true
                lastSelected = selectedIndex
                indicator.snapTo(Offset(targetLeft, targetWidth))
                return@LaunchedEffect
            }
            if (abs(selectedIndex - lastSelected) > 1) {
                // A jump does not cross the strip; it flies in from outside the target tab.
                val direction = sign((selectedIndex - lastSelected).toFloat())
                val flyIn = with(density) { CouiTabLayoutDefaults.JumpFlyInDistance.toPx() }
                indicator.snapTo(Offset(targetLeft - direction * flyIn, targetWidth))
            }
            val travel = CouiTabLayoutDefaults.travelDurationMs(selectedIndex - lastSelected)
            lastSelected = selectedIndex
            indicator.animateTo(
                targetValue = Offset(targetLeft, targetWidth),
                animationSpec = tween(durationMillis = travel, easing = CouiEasing.MoveEase),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().height(CouiTabLayoutDefaults.TabHeight)) {
                tabs.forEachIndexed { index, title ->
                    val selected = index == selectedIndex
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) colors.labelPrimary else colors.secondNeutral,
                        animationSpec = tween(
                            durationMillis = CouiTabLayoutDefaults.travelDurationMs(abs(index - lastSelected)),
                        ),
                        label = "couiTabLabelColor",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectedChange(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        CouiText(
                            text = title,
                            style = CouiTabLayoutDefaults.labelStyle,
                            color = labelColor,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CouiTabLayoutDefaults.IndicatorHeight),
            ) {
                if (trackEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CouiTabLayoutDefaults.TrackInset)
                            .height(CouiTabLayoutDefaults.IndicatorHeight)
                            .background(CouiTabLayoutDefaults.TrackColor),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicator.value.x.roundToInt(), 0) }
                        .width(with(density) { indicator.value.y.toDp() })
                        .height(CouiTabLayoutDefaults.IndicatorHeight)
                        .clip(RoundedCornerShape(CouiTabLayoutDefaults.IndicatorCornerRadius))
                        .background(colors.labelPrimary),
                )
            }
        }
    }
}
