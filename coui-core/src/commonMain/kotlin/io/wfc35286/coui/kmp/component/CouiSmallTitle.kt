// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.wfc35286.coui.kmp.token.CouiTextStyle
import io.wfc35286.coui.kmp.token.CouiTypography

/**
 * Defaults for [CouiSmallTitle], all taken from `Widget.COUI.List.Category.Title` and the layout
 * that uses it (`res/layout/coui_preference_category_layout.xml`).
 */
object CouiSmallTitleDefaults {
    /** `coui_common_category_text_padding_top`. */
    val TopMargin: Dp = 8.dp

    /** `coui_common_category_text_padding_bottom`. */
    val BottomMargin: Dp = 8.dp

    /**
     * `support_preference_category_layout_title_margin_start_small`.
     *
     * The style itself sets `paddingStart` to `coui_preference_category_text_padding_left`, which
     * is 0dp, so the whole leading inset comes from the layout margin.
     */
    val StartMargin: Dp = 16.dp

    /** `coui_category_title_pading_end_with_reddot_default`. */
    val EndMargin: Dp = 8.dp

    /** `coui_preference_category_text_height`. */
    val MinHeight: Dp = 16.dp
}

/**
 * A COUI section header: the small caption above a group of rows.
 *
 * Colour and type come straight from `Widget.COUI.List.Category.Title`:
 * `couiTextAppearanceSmallButton` (12sp, `sans-serif-medium`) in `couiColorSecondNeutral`.
 *
 * @param text The header text.
 * @param modifier The modifier to apply to the header.
 * @param style The type-scale entry; defaults to [CouiTypography.categoryTitle].
 */
@Composable
@NonRestartableComposable
fun CouiSmallTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: CouiTextStyle = CouiTypography.categoryTitle,
) {
    CouiText(
        text = text,
        style = style,
        modifier = modifier
            .padding(
                start = CouiSmallTitleDefaults.StartMargin,
                end = CouiSmallTitleDefaults.EndMargin,
                top = CouiSmallTitleDefaults.TopMargin,
                bottom = CouiSmallTitleDefaults.BottomMargin,
            )
            .defaultMinSize(minHeight = CouiSmallTitleDefaults.MinHeight),
        emphasis = CouiTextEmphasis.Secondary,
    )
}