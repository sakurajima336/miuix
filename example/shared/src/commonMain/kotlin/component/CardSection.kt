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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.wfc35286.coui.kmp.component.CouiCard
import io.wfc35286.coui.kmp.component.CouiCardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Card, drawn with COUI's own corner geometry instead of `miuix.Card`.
 *
 * The radius comes from `coui_component_card_*_bg` (`<corners android:radius="16dp"/>`) and the
 * fill from `couiColorCardBackground`. The corner itself is the table-driven Bézier that
 * `COUIShapePath.getRoundRectPath` produces, so this is the same silhouette ColorOS draws.
 */
fun LazyListScope.cardSection() {
    item(key = "card") {
        SmallTitle(text = "Card")
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            insideMargin = PaddingValues(16.dp),
        ) {
            Text(
                text = "Card",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "COUI G2 corner, 16dp",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
                Text(
                    text = "Clickable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Pressed fill",
                    style = MiuixTheme.textStyles.paragraph,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            CouiCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "24dp",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Bigger corner,\nsame curve family",
                    style = MiuixTheme.textStyles.paragraph,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
            Text(
                text = "Defaults",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "CouiCardDefaults.CornerRadius = 16dp",
                style = MiuixTheme.textStyles.paragraph,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}