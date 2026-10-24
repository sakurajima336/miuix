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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiCardDefaults
import io.github.wfc35286.coui.kmp.basic.CouiSmallTitle
import io.github.wfc35286.coui.kmp.basic.CouiSwitch
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.blur.CouiBlurPresets
import io.github.wfc35286.coui.kmp.blur.CouiBlurSurface
import io.github.wfc35286.coui.kmp.blur.couiBlurSource
import io.github.wfc35286.coui.kmp.blur.rememberCouiBlurBackdrop
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography



/**
 * The COUI material blur (`coui-blur`), shown over a deliberately loud backdrop so the two tint
 * layers are visible: the light preset dodges the colour towards white, the dark one overlays.
 *
 * Everything the surface needs is the recorded backdrop plus [CouiBlurPresets]; there is no
 * framework call and no Oplus class involved, so it behaves the same on every Android device.
 */
@Composable
fun CouiBlurSection() {
    var dark by remember { mutableStateOf(false) }
    val backdrop = rememberCouiBlurBackdrop()
    CouiSmallTitle("Blur")
    CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))) {
                // The page behind the material: recorded once per frame, then sampled by the surface.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .couiBlurSource(backdrop)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E88E5), Color(0xFFE91E63), Color(0xFFFFB300)),
                                ),
                            ),
                ) {
                    // Deliberately behind the material: sharp glyph edges are the fastest way to
                    // tell a real blur from a tint-only fallback.
                    CouiText(
                        "backdrop",
                        CouiTypography.listTitle,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                CouiBlurSurface(
                    backdrop = backdrop,
                    params = if (dark) CouiBlurPresets.BottomBarDark else CouiBlurPresets.BottomBarLight,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.72f).height(72.dp),
                    shape = RoundedCornerShape(36.dp),
                ) {
                    CouiText(
                        if (dark) "BottomBarDark" else "BottomBarLight",
                        CouiTypography.body,
                        emphasis = CouiTextEmphasis.Primary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            CouiSwitch(checked = dark, onCheckedChange = { dark = it })
            CouiText(
                "blurRadiusPx = " + (if (dark) CouiBlurPresets.BottomBarDark else CouiBlurPresets.BottomBarLight).blurRadiusPx,
                CouiTypography.body,
                emphasis = CouiTextEmphasis.Secondary,
                color = CouiTheme.colors.labelSecondary,
            )
        }
    }
}