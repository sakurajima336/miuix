// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.component.CouiText
import io.github.wfc35286.coui.kmp.effect.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.effect.couiSpotLight
import io.github.wfc35286.coui.kmp.effect.rememberCouiSpotLightState
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography
import kotlinx.coroutines.delay

/**
 * The spot light, shown on two surfaces:
 *
 * * a capsule driven by `Modifier.couiSpotLight` (installs its own gesture), and
 * * a card driven by a [io.github.wfc35286.coui.kmp.effect.CouiSpotLightState] that the probe
 *   presses **programmatically**, so a screenshot can be taken without a real finger.
 */
@Composable
fun CouiSpotLightProbe(autoPress: Boolean = true) {
    val cardLight = rememberCouiSpotLightState()
    var cardSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    if (autoPress) {
        LaunchedEffect(cardSize) {
            if (cardSize.width > 0) {
                delay(500)
                cardLight.press(Offset(cardSize.width * 0.3f, cardSize.height * 0.5f))
                delay(700)
                cardLight.moveTo(Offset(cardSize.width * 0.72f, cardSize.height * 0.5f))
            }
        }
    }
    CouiTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CouiTheme.colors.backgroundWithCard)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CouiText("Spot light", CouiTypography.listTitle)
            // Capsule: the modifier installs the gesture itself.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .couiSpotLight(
                            spec = CouiSpotLightDefaults.Capsule2Light,
                            shape = RoundedCornerShape(percent = 50),
                        )
                        .background(Color(0xFF2B2B2B), RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center,
            ) {
                CouiText("press me", CouiTypography.body, color = Color.White)
            }
            // Card: the state is driven from outside (what a component does).
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .onSizeChanged { cardSize = it }
                        .background(Color(0xFF1F1F1F), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                io.github.wfc35286.coui.kmp.effect.CouiSpotLightSurface(
                    state = cardLight,
                    spec = CouiSpotLightDefaults.OpaqueMedium2(dark = true),
                    shape = RoundedCornerShape(20.dp),
                )
                CouiText("auto press", CouiTypography.body, color = Color.White)
            }
            CouiText(
                "light follows the finger on its own spring (0.15 / 0.35)",
                CouiTypography.preferenceSummary,
                emphasis = CouiTextEmphasis.Secondary,
            )
        }
    }
}