// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.example

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.SquircleDefaults
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

// region Vector assets
//
// These path strings are copied verbatim from the ColorOS drawables, so the glyphs match the
// originals instead of being redrawn by hand. Each one records its source drawable and the
// viewport its coordinates are expressed in.

private const val CHECKBOX_VIEWPORT = 24f

/** `res/drawable/coui_btn_check_on_normal.xml` — checked box, 18x18 r4 in a 24x24 viewport. */
private const val CHECKBOX_ON_BOX =
    "M7,3L17,3A4,4 0,0 1,21 7L21,17A4,4 0,0 1,17 21L7,21A4,4 0,0 1,3 17L3,7A4,4 0,0 1,7 3z"

/** `res/drawable/coui_btn_check_off_normal.xml` — 16.4x16.4 r3.2, stroked with 1.3. */
private const val CHECKBOX_OFF_BOX =
    "M7,3.799L17,3.799A3.2,3.2 0,0 1,20.2 6.999L20.2,16.999A3.2,3.2 0,0 1,17 20.199" +
        "L7,20.199A3.2,3.2 0,0 1,3.8 16.999L3.8,6.999A3.2,3.2 0,0 1,7 3.799z"

/** The check mark inside `coui_btn_check_on_normal.xml`. */
private const val CHECKBOX_CHECK =
    "M10.831,14.318L17.033,8L18,8.985L11.315,15.796C11.047,16.068 10.614,16.068 10.347,15.796" +
        "L7,12.386L7.967,11.401L10.831,14.318Z"

/** `res/drawable/coui_btn_check_off_normal.xml` — border width, in viewport units. */
private const val CHECKBOX_OFF_STROKE = 1.3f

/** `res/drawable/coui_btn_radio_on.xml` — filled disc, radius 10. */
private const val RADIO_ON_DISC = "M12,12m-10,0a10,10 0,1 1,20 0a10,10 0,1 1,-20 0"

/** `res/drawable/coui_btn_radio_on.xml` — the white hole, radius 6. */
private const val RADIO_ON_HOLE = "M12,12m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"

/** `res/drawable/coui_btn_radio_off.xml` — stroked ring, radius 9.2. */
private const val RADIO_OFF_RING = "M12,12m-9.2,0a9.2,9.2 0,1 1,18.4 0a9.2,9.2 0,1 1,-18.4 0"

/** `res/drawable/coui_btn_radio_off.xml` — ring border width, in viewport units. */
private const val RADIO_OFF_STROKE = 1.3f

/** `res/drawable/coui_btn_next_normal.xml` — the chevron, a 12x24 viewport. */
private const val CHEVRON_VIEWPORT_H = 24f
private const val CHEVRON_PATH = "M5,6L10.646,11.646C10.842,11.842 10.842,12.158 10.646,12.354L5,18"
private const val CHEVRON_STROKE = 1.4f

/** `res/drawable/coui_search_view_icon_normal.xml` — a filled magnifier, 20x20. */
private const val SEARCH_VIEWPORT = 20f
private const val SEARCH_PATH =
    "M4.551,8.428C4.551,5.872 6.623,3.799 9.18,3.799C11.736,3.799 13.808,5.872 13.808,8.428" +
        "C13.808,10.984 11.736,13.056 9.18,13.056C6.623,13.056 4.551,10.984 4.551,8.428ZM9.18,2.199" +
        "C5.74,2.199 2.951,4.988 2.951,8.428C2.951,11.868 5.74,14.656 9.18,14.656" +
        "C10.539,14.656 11.796,14.221 12.82,13.482L16.758,17.42C17.149,17.811 17.782,17.811 18.173,17.42" +
        "C18.563,17.03 18.563,16.397 18.173,16.006L14.234,12.068C14.973,11.044 15.408,9.787 15.408,8.428" +
        "C15.408,4.988 12.62,2.199 9.18,2.199Z"

private fun pathOf(data: String): Path = PathParser().parsePathString(data).toPath()

/**
 * Draws [path], which is expressed in a [viewport] square, scaled to fill the current canvas.
 * Stroke widths are scaled the same way so a viewport-unit stroke keeps its visual weight.
 */
private fun DrawScope.drawViewportPath(
    path: Path,
    viewport: Float,
    color: Color,
    alpha: Float = 1f,
    strokeWidth: Float? = null,
) {
    if (alpha <= 0f) return
    val factor = size.minDimension / viewport
    scale(factor, factor, pivot = Offset.Zero) {
        if (strokeWidth == null) {
            drawPath(path, color = color, alpha = alpha)
        } else {
            drawPath(
                path = path,
                color = color,
                alpha = alpha,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

// endregion

// region Switch

/**
 * Defaults for [CouiSwitch].
 *
 * The metrics are the ones `COUISwitch` computes in `onMeasure`: it extends `SwitchCompat` and
 * sizes itself as `switchMinWidth x barHeight` plus `2 * coui_switch_padding`, where
 * `coui_switch_padding` is 0dp and the other two come from `COUISwitchStyle`
 * (`@dimen/bar_width` = 44dp, `@dimen/bar_height` = 24dp).
 */
object CouiSwitchDefaults {
    /** `bar_width` — the full track width. */
    val Width: Dp = 44.dp

    /** `bar_height` — the full track height. */
    val Height: Dp = 24.dp

    /** `outer_circle_width` — the thumb diameter. */
    val ThumbSize: Dp = 18.dp

    /** `circle_padding` — the gap between the thumb and the track edge. */
    val ThumbPadding: Dp = 3.dp

    /**
     * The toggle spring, from `COUISwitchParams.toggleSpring` (bounce 0.3, response 0.4).
     *
     * `COUISwitch.animateWhenStateChanged` drives `circleTranslation` with exactly this spring and a
     * minimum visible change of `0.1`.
     */
    val ToggleSpec: SpringSpec<Float> =
        CouiMotion.spring(response = CouiMotion.Switch.ToggleSpring.first, bounce = CouiMotion.Switch.ToggleSpring.second, visibilityThreshold = 0.1f)

    /** The track colour for the given state. */
    @Composable
    fun trackColor(
        checked: Boolean,
        enabled: Boolean = true,
    ): Color {
        val colors = CouiTheme.colors
        return when {
            !enabled && checked -> colors.primary.copy(alpha = 0.3f)
            !enabled -> colors.switchTrackOffDisabled
            checked -> colors.primary
            else -> colors.switchTrackOff
        }
    }

    /** The thumb colour for the given state. */
    @Composable
    fun thumbColor(
        checked: Boolean,
        enabled: Boolean = true,
    ): Color {
        val colors = CouiTheme.colors
        return if (enabled) colors.switchThumb else colors.switchThumbDisabled
    }
}

/**
 * A COUI switch.
 *
 * Geometry and colours come from `COUISwitchStyle` in ColorOS Settings 17.0.0: a 44x24dp track
 * with an 18dp thumb inset by 3dp, leaving 20dp of travel, which is exactly the
 * `switchMinWidth - 2 * circlePadding - outerCircleWidth` the view computes.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Called with the new state when the user taps the switch.
 * @param modifier The modifier to apply to the switch.
 * @param enabled Whether the switch can be interacted with.
 */
@Composable
fun CouiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    io.wfc35286.coui.kmp.theme.CouiTheme {
        io.wfc35286.coui.kmp.component.CouiSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}
// endregion

// region CheckBox / RadioButton

/**
 * Defaults for [CouiCheckBox] and [CouiRadioButton].
 *
 * Both are 24dp vectors. The unchecked checkbox draws a 16.4dp rounded square with a 1.3 unit
 * stroke; the checked one grows to 18dp and fills with the accent colour, which is why these
 * controls appear to swell when toggled.
 */
object CouiSelectDefaults {
    /** The intrinsic size of both drawables. */
    val Size: Dp = 24.dp

    /** Gap between the control and its label. */
    val TextSpacing: Dp = 8.dp

    /** The accent used by the checked / selected state. */
    @Composable
    fun accentColor(enabled: Boolean = true): Color = if (enabled) CouiTheme.colors.primary else CouiTheme.colors.disabledNeutral

    /** The outline of the unchecked box and the unselected radio ring. */
    @Composable
    fun outlineColor(enabled: Boolean = true): Color = if (enabled) CouiTheme.colors.labelTertiary else CouiTheme.colors.disabledNeutral
}

/**
 * A COUI checkbox, ported from `coui_btn_check_on_normal.xml` / `coui_btn_check_off_normal.xml`.
 *
 * @param checked Whether the box is selected.
 * @param onCheckedChange Called with the new state when the user taps the box.
 * @param modifier The modifier to apply to the box.
 * @param enabled Whether the box can be interacted with.
 */
@Composable
fun CouiCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "CouiCheckBoxFraction",
    )
    val accentColor = CouiSelectDefaults.accentColor(enabled)
    val outlineColor = CouiSelectDefaults.outlineColor(enabled)
    val markColor = CouiTheme.colors.labelOnColor
    val boxPath = remember { pathOf(CHECKBOX_ON_BOX) }
    val outlinePath = remember { pathOf(CHECKBOX_OFF_BOX) }
    val checkPath = remember { pathOf(CHECKBOX_CHECK) }

    Canvas(
        modifier = modifier
            .size(CouiSelectDefaults.Size)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
    ) {
        // The filled box grows from the unchecked size (16.4/18) to its full size.
        val growth = lerp(0.911f, 1f, fraction)
        val factor = size.minDimension / CHECKBOX_VIEWPORT * growth
        val inset = (size.minDimension - CHECKBOX_VIEWPORT * factor) / 2f
        scale(factor, factor, pivot = Offset(inset, inset)) {
            drawPath(boxPath, color = accentColor, alpha = fraction)
        }
        drawViewportPath(
            path = outlinePath,
            viewport = CHECKBOX_VIEWPORT,
            color = outlineColor,
            alpha = 1f - fraction,
            strokeWidth = CHECKBOX_OFF_STROKE,
        )
        drawViewportPath(
            path = checkPath,
            viewport = CHECKBOX_VIEWPORT,
            color = markColor,
            alpha = fraction,
        )
    }
}

/**
 * A COUI radio button, ported from `coui_btn_radio_on.xml` / `coui_btn_radio_off.xml`.
 *
 * The selected state is a 10dp disc with a 6dp white hole punched through it, which reads as a
 * thick accent ring rather than the usual small centre dot.
 *
 * @param selected Whether this option is the selected one.
 * @param onClick Called when the user taps the button.
 * @param modifier The modifier to apply to the button.
 * @param enabled Whether the button can be interacted with.
 */
@Composable
fun CouiRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "CouiRadioFraction",
    )
    val accentColor = CouiSelectDefaults.accentColor(enabled)
    val outlineColor = CouiSelectDefaults.outlineColor(enabled)
    val coreColor = CouiTheme.colors.switchThumb
    val discPath = remember { pathOf(RADIO_ON_DISC) }
    val holePath = remember { pathOf(RADIO_ON_HOLE) }
    val ringPath = remember { pathOf(RADIO_OFF_RING) }

    Canvas(
        modifier = modifier
            .size(CouiSelectDefaults.Size)
            .clickable(enabled = enabled) { onClick() },
    ) {
        val growth = lerp(0.92f, 1f, fraction)
        val factor = size.minDimension / CHECKBOX_VIEWPORT * growth
        val inset = (size.minDimension - CHECKBOX_VIEWPORT * factor) / 2f
        scale(factor, factor, pivot = Offset(inset, inset)) {
            drawPath(discPath, color = accentColor, alpha = fraction)
        }
        drawViewportPath(
            path = ringPath,
            viewport = CHECKBOX_VIEWPORT,
            color = outlineColor,
            alpha = 1f - fraction,
            strokeWidth = RADIO_OFF_STROKE,
        )
        drawViewportPath(
            path = holePath,
            viewport = CHECKBOX_VIEWPORT,
            color = coreColor,
            alpha = fraction,
        )
    }
}

// endregion

// region SeekBar

/** Defaults for [CouiSeekBar], from `COUISeekBarStyle` in ColorOS Settings 17.0.0. */
object CouiSeekBarDefaults {
    /** `couiSeekBarMinHeight`. */
    val MinHeight: Dp = 36.dp

    /** `coui_seekbar_background_height` — the track thickness. */
    val TrackHeight: Dp = 20.dp

    /** `coui_seekbar_thumb_out_radius` — the thumb radius. The thumb is pure white. */
    val ThumbRadius: Dp = 6.dp

    /** `coui_seekbar_thumb_max_radius` — the outer radius while dragging. */
    val ThumbMaxRadius: Dp = 8.dp
}

/**
 * A COUI seek bar.
 *
 * The thumb is not a solid accent dot: it is an accent disc with a white core
 * (`coui_seekbar_thumb_in_scale_radius`), and it swells from 6dp to 8dp while dragging.
 *
 * @param value The current value.
 * @param onValueChange Called with the new value while the user drags or taps.
 * @param modifier The modifier to apply to the seek bar.
 * @param valueRange The range [value] is clamped to.
 * @param enabled Whether the seek bar can be interacted with.
 */
@Composable
fun CouiSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colors
    var trackWidth by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span <= 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun emitAt(x: Float) {
        if (trackWidth <= 0) return
        val f = (x / trackWidth).coerceIn(0f, 1f)
        onValueChange(valueRange.start + f * span)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CouiSeekBarDefaults.MinHeight)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = { emitAt(it.x) })
            }
            .pointerInput(enabled, valueRange, trackWidth) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        emitAt(it.x)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ -> emitAt(change.position.x) },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val trackHeight = CouiSeekBarDefaults.TrackHeight.toPx()
            val thumbRadius = (
                if (isDragging) CouiSeekBarDefaults.ThumbMaxRadius else CouiSeekBarDefaults.ThumbRadius
                ).toPx()
            val centerY = size.height / 2f
            val radius = trackHeight / 2f
            val accent = if (enabled) colors.primary else colors.disabledNeutral
            val track = if (enabled) colors.seekBarTrack else colors.seekBarTrackDisabled
            val thumbColor = if (enabled) colors.switchThumb else colors.switchThumbDisabled

            drawRoundRect(
                color = track,
                topLeft = Offset(0f, centerY - radius),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
            val thumbX = thumbRadius + (size.width - thumbRadius * 2f) * fraction
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, centerY - radius),
                size = Size(thumbX, trackHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
            // The thumb is smaller than the track, so the accent shows as a ring around it.
            drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(thumbX, centerY))
        }
    }
}

// endregion

// region Tabs & chips

/**
 * A COUI tab row: equal-width titles with a sliding indicator underneath, matching
 * `sliding_tab_selected_indicator` from ColorOS pager tabs.
 *
 * @param tabs The tab titles.
 * @param selectedIndex The index of the selected tab.
 * @param onSelectedChange Called with the index the user selected.
 * @param modifier The modifier to apply to the row.
 */
@Composable
fun CouiTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val colors = CouiTheme.colors
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "CouiTabIndicator",
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / tabs.size
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CouiDimens.TabHeight),
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectedChange(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            style = MiuixTheme.textStyles.body1,
                            color = if (index == selectedIndex) colors.labelPrimary else colors.labelSecondary,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CouiDimens.TabIndicatorHeight),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = tabWidth * animatedIndex)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = CouiDimens.TabTitlePadding),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                }
            }
        }
    }
}

/**
 * A COUI chip row: pill-shaped selectable labels.
 *
 * @param labels The chip labels.
 * @param selectedIndex The index of the selected chip, or `null` for no selection.
 * @param onSelectedChange Called with the index the user selected.
 * @param modifier The modifier to apply to the row.
 */
@Composable
fun CouiChipRow(
    labels: List<String>,
    selectedIndex: Int?,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CouiTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CouiDimens.ChipSpacing),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = CouiDimens.ChipMinWidth, minHeight = CouiDimens.ChipMinHeight)
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else colors.pressBackground)
                    .clickable { onSelectedChange(index) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (selected) colors.labelOnColor else colors.labelPrimary,
                )
            }
        }
    }
}

// endregion

// region Search bar

/**
 * A COUI search field, sized at the real `coui_search_view_height` of 52dp with the magnifier
 * from `coui_search_view_icon_normal.xml`.
 *
 * @param state The text field state driving the query.
 * @param modifier The modifier to apply to the bar.
 * @param hint The placeholder shown while the field is empty.
 * @param onCancel Called when the user taps the trailing cancel action; hidden when null.
 */
@Composable
fun CouiSearchBar(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    hint: String = "Search",
    onCancel: (() -> Unit)? = null,
) {
    val colors = CouiTheme.colors
    val searchPath = remember { pathOf(SEARCH_PATH) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CouiDimens.SearchViewHeight)
            .clip(CircleShape)
            .background(colors.pressBackground)
            .padding(horizontal = CouiDimens.SearchIconMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(CouiDimens.SearchIconSize)) {
            drawViewportPath(
                path = searchPath,
                viewport = SEARCH_VIEWPORT,
                color = colors.labelSecondary,
            )
        }
        Spacer(modifier = Modifier.width(CouiSelectDefaults.TextSpacing))
        BasicTextField(
            state = state,
            modifier = Modifier.weight(1f),
            textStyle = MiuixTheme.textStyles.body1.copy(color = colors.labelPrimary),
            cursorBrush = SolidColor(colors.primary),
            lineLimits = TextFieldLineLimits.SingleLine,
            decorator = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = hint,
                            style = MiuixTheme.textStyles.body1,
                            color = colors.labelTertiary,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (onCancel != null) {
            Spacer(modifier = Modifier.width(CouiSelectDefaults.TextSpacing))
            Text(
                text = "Cancel",
                style = MiuixTheme.textStyles.body1,
                color = colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

// endregion

// region List building blocks

/**
 * A COUI category header: a small caption above a group of [CouiPreferenceItem]s.
 *
 * @param title The header text.
 * @param modifier The modifier to apply to the header.
 * @param trailing Optional content aligned to the end of the header row.
 */
@Composable
fun CouiCategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = CouiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = CouiDimens.CategoryTitleMarginStart,
                end = CouiDimens.CategoryTitleMarginEnd,
                top = 16.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1,
            color = colors.labelSecondary,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(CouiDimens.CategoryWidgetMarginStart))
            trailing()
        }
    }
}

/**
 * A COUI list row.
 *
 * The leading inset follows the real rule: 32dp when the row carries a leading icon and 16dp
 * when it does not, and the divider lines up with the title either way.
 *
 * @param title The row title.
 * @param modifier The modifier to apply to the row.
 * @param summary Optional secondary line under the title.
 * @param onClick Called when the row is tapped; the row is not clickable when null.
 * @param enabled Whether the row is enabled.
 * @param showDivider Whether to draw a hairline divider under the row.
 * @param withIcon Whether the row reserves the leading icon column.
 * @param trailing Optional trailing content, such as a [CouiSwitch] or a chevron.
 */
@Composable
fun CouiPreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String = "",
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    withIcon: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = CouiTheme.colors
    val inset = if (withIcon) CouiDimens.PreferencePaddingStart else CouiDimens.PreferencePaddingNoIcon
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
                .defaultMinSize(minHeight = CouiDimens.PreferenceMinHeight)
                .padding(
                    start = inset,
                    end = inset,
                    top = CouiDimens.WidgetPaddingVertical,
                    bottom = CouiDimens.WidgetPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.main,
                    color = if (enabled) colors.labelPrimary else colors.disabledNeutral,
                )
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.body2,
                        color = colors.labelSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(CouiDimens.WidgetMarginStart))
                trailing()
            }
        }
        if (showDivider) {
            CouiDivider(modifier = Modifier.padding(start = inset))
        }
    }
}

/**
 * The COUI chevron, ported from `coui_btn_next_normal.xml`.
 *
 * @param modifier The modifier to apply to the chevron.
 * @param tint The chevron colour.
 */
@Composable
fun CouiChevron(
    modifier: Modifier = Modifier,
    tint: Color = CouiTheme.colors.labelTertiary,
) {
    val path = remember { pathOf(CHEVRON_PATH) }
    Canvas(modifier = modifier.size(12.dp, 24.dp)) {
        val factor = size.height / CHEVRON_VIEWPORT_H
        scale(factor, factor, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = CHEVRON_STROKE, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * A COUI card: a rounded container that groups related rows.
 *
 * @param modifier The modifier to apply to the card.
 * @param content The rows inside the card.
 */
@Composable
fun CouiCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .squircleClip(CouiCorners.L.radiusNormal, CouiMotion.cornerExtension())
            .squircleBackground(CouiTheme.colors.card, CouiCorners.L.radiusNormal, CouiMotion.cornerExtension()),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * The COUI hairline divider.
 *
 * @param modifier The modifier to apply to the divider.
 * @param inset The leading inset, so the line starts under the title.
 */
@Composable
fun CouiDivider(
    modifier: Modifier = Modifier,
    inset: Dp = CouiDimens.PreferencePaddingNoIcon,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(CouiDimens.DividerHeight)
            .background(CouiTheme.colors.divider),
    )
}

/**
 * A COUI button: a full-width rounded action.
 *
 * @param text The button label.
 * @param onClick Called when the button is tapped.
 * @param modifier The modifier to apply to the button.
 * @param enabled Whether the button can be tapped.
 * @param primary Whether to use the filled accent style instead of the neutral style.
 */
@Composable
fun CouiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val colors = CouiTheme.colors
    val background = when {
        !enabled -> colors.disabledNeutral
        primary -> colors.primary
        else -> colors.pressBackground
    }
    val contentColor = if (primary && enabled) colors.labelOnColor else colors.labelPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CouiDimens.ButtonHeight)
            .squircleClip(CouiCorners.M.radiusNormal, CouiMotion.cornerExtension())
            .squircleBackground(background, CouiCorners.M.radiusNormal, CouiMotion.cornerExtension())
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.button,
            color = contentColor,
        )
    }
}

// endregion
