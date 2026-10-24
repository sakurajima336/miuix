// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiSectionSeekBar
import io.github.wfc35286.coui.kmp.basic.CouiSeekBar
import io.github.wfc35286.coui.kmp.basic.CouiSmallTitle
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.preference.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.preference.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography

/**
 * Sliders, drawn with the COUI seek bars (`COUISeekBar` / `COUISectionSeekBar` smali ports).
 * Named after the ColorOS Settings rows they mirror (`ScreenMinBrightnessPreference` /
 * `SettingsBrightnessPreference`), per the component naming policy.
 */
fun LazyListScope.sliderSection() {
    item(key = "slider") {
        CouiSmallTitle(text = "Slider")
        CouiCard(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            var normalValue by remember { mutableFloatStateOf(0.3f) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CouiText(text = "Normal", style = CouiTypography.listTitle)
                    CouiText(
                        text = "${(normalValue * 100).toInt()}%",
                        style = CouiTypography.listAssignmentEnd,
                        emphasis = CouiTextEmphasis.Secondary,
                    )
                }
                CouiSeekBar(
                    value = normalValue,
                    onValueChange = { normalValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(20.dp),
                )
            }
            var brightnessValue by remember { mutableIntStateOf(2) }
            CouiPreferenceItem(
                title = "Brightness",
                position = CouiPreferencePosition.Last,
                summary = "Steps with key points (SectionSeekBar)",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CouiText(
                            text = "$brightnessValue/4",
                            style = CouiTypography.listAssignmentEnd,
                            emphasis = CouiTextEmphasis.Secondary,
                        )
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiSectionSeekBar(
                    value = brightnessValue,
                    onValueChange = { brightnessValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                )
            }
            var disabledValue by remember { mutableFloatStateOf(0.7f) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CouiText(
                        text = "Disabled",
                        style = CouiTypography.listTitle,
                        enabled = false,
                    )
                    CouiText(
                        text = "${(disabledValue * 100).toInt()}%",
                        style = CouiTypography.listAssignmentEnd,
                        emphasis = CouiTextEmphasis.Secondary,
                        enabled = false,
                    )
                }
                CouiSeekBar(
                    value = disabledValue,
                    onValueChange = { },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(20.dp),
                )
            }
        }
    }
}