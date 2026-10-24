// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiCardDefaults
import io.github.wfc35286.coui.kmp.basic.CouiSmallTitle
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography

/**
 * Card, drawn with COUI's own corner geometry instead of `miuix.Card`.
 *
 * The radius comes from `coui_component_card_*_bg` (`<corners android:radius="16dp"/>`) and the
 * fill from `couiColorCardBackground`. The corner itself is the table-driven Bézier that
 * `COUIShapePath.getRoundRectPath` produces, so this is the same silhouette ColorOS draws.
 */
fun LazyListScope.cardSection() {
    item(key = "card") {
        CouiSmallTitle(text = "Card")
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            insideMargin = PaddingValues(16.dp),
        ) {
            CouiText(
                text = "Card",
                style = CouiTypography.listTitle,
            )
            CouiText(
                text = "COUI G2 corner, 16dp",
                style = CouiTypography.body,
                emphasis = CouiTextEmphasis.Secondary,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiCard(
                modifier = Modifier.weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = { println("Card click") },
            ) {
                CouiText(
                    text = "Clickable",
                    style = CouiTypography.listTitle,
                )
                CouiText(
                    text = "Pressed fill",
                    style = CouiTypography.body,
                    emphasis = CouiTextEmphasis.Secondary,
                )
            }
            CouiCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(16.dp),
            ) {
                CouiText(
                    text = "24dp",
                    style = CouiTypography.listTitle,
                )
                CouiText(
                    text = "Bigger corner,\nsame curve family",
                    style = CouiTypography.body,
                    emphasis = CouiTextEmphasis.Secondary,
                )
            }
        }
        // The card defaults are worth showing off: on ColorOS the radius is a themed token, and
        // the fill is `couiColorCardBackground` rather than the surrounding surface colour.
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            cornerRadius = CouiCardDefaults.CornerRadius,
            insideMargin = PaddingValues(16.dp),
        ) {
            CouiText(
                text = "Defaults",
                style = CouiTypography.listTitle,
            )
            CouiText(
                text = "CouiCardDefaults.CornerRadius = 16dp",
                style = CouiTypography.body,
                emphasis = CouiTextEmphasis.Secondary,
            )
        }
    }
}