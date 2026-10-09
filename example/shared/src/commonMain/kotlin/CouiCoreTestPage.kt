// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import io.wfc35286.coui.kmp.component.CouiCard
import io.wfc35286.coui.kmp.component.CouiCardDefaults
import io.wfc35286.coui.kmp.component.CouiPreferenceDefaults
import io.wfc35286.coui.kmp.component.CouiPreferenceItem
import io.wfc35286.coui.kmp.component.CouiPreferencePosition
import io.wfc35286.coui.kmp.component.CouiSmallTitle
import io.wfc35286.coui.kmp.component.CouiSeekBar
import io.wfc35286.coui.kmp.component.CouiStatusBarToggleSlider
import io.wfc35286.coui.kmp.component.CouiSeekBarDefaults
import io.wfc35286.coui.kmp.component.CouiSwitch
import io.wfc35286.coui.kmp.component.CouiText
import io.wfc35286.coui.kmp.theme.CouiTheme
import io.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.wfc35286.coui.kmp.token.CouiTypography

/**
 * The COUI Settings card and switch playground.
 *
 * Deliberately small: a handful of switches, one pair of which is locked, plus a drag demo. The
 * page uses **nothing from Miuix** - every colour, radius and spring comes from `coui-core`.
 */
@Composable
fun CouiCoreTestPage(
    padding: PaddingValues,
) {
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(false) }
    var airplane by remember { mutableStateOf(false) }
    var autoRotate by remember { mutableStateOf(true) }
    var brightness by remember { mutableStateOf(0.65f) }
    var minBrightness by remember { mutableStateOf(0.35f) }

    CouiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Settings pages that host cards sit on couiColorBackgroundWithCard (#F0F1F2);
                // couiColorCardBackground is the white fill on top of it.
                .background(CouiTheme.colors.backgroundWithCard)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            CouiSmallTitle("设置列表")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = CouiCardDefaults.HorizontalMargin)) {
                CouiPreferenceItem(
                    title = "飞行模式",
                    position = CouiPreferencePosition.First,
                    onClick = { airplane = !airplane },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiSettingsIcons.Airplane, contentDescription = null) },
                    trailing = { CouiSwitch(checked = airplane, onCheckedChange = { airplane = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "WLAN",
                    position = CouiPreferencePosition.Middle,
                    onClick = { wifi = !wifi },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiSettingsIcons.Wifi, contentDescription = null) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CouiText(if (wifi) "已连接" else "未连接", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                            Spacer(Modifier.width(4.dp))
                            Image(CouiSettingsIcons.Next, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary))
                        }
                    },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "蓝牙",
                    position = CouiPreferencePosition.Middle,
                    onClick = { bluetooth = !bluetooth },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiSettingsIcons.Bluetooth, contentDescription = null) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CouiText(if (bluetooth) "已连接" else "未连接", CouiTypography.body, emphasis = CouiTextEmphasis.Secondary)
                            Spacer(Modifier.width(4.dp))
                            Image(CouiSettingsIcons.Next, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary))
                        }
                    },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "移动网络",
                    position = CouiPreferencePosition.Last,
                    onClick = { autoRotate = !autoRotate },
                    minHeight = CouiPreferenceDefaults.HomepageMinHeight,
                    leading = { Image(CouiSettingsIcons.MobileNetwork, contentDescription = null) },
                    trailing = { Image(CouiSettingsIcons.Next, contentDescription = null, colorFilter = ColorFilter.tint(CouiTheme.colors.labelTertiary)) },
                )
            }

            CouiSmallTitle("Switches")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Tapping anywhere on the row flips the switch, same as the real Settings rows.
                CouiPreferenceItem(
                    title = "Wi-Fi",
                    position = CouiPreferencePosition.First,
                    summary = "Tap the row or the switch",
                    onClick = { wifi = !wifi },
                    trailing = { CouiSwitch(checked = wifi, onCheckedChange = { wifi = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Bluetooth",
                    position = CouiPreferencePosition.Middle,
                    onClick = { bluetooth = !bluetooth },
                    trailing = { CouiSwitch(checked = bluetooth, onCheckedChange = { bluetooth = it }) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Airplane mode",
                    position = CouiPreferencePosition.Last,
                    onClick = { airplane = !airplane },
                    trailing = { CouiSwitch(checked = airplane, onCheckedChange = { airplane = it }) },
                )
            }

            CouiSmallTitle("Locked")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CouiPreferenceItem(
                    title = "Locked on",
                    position = CouiPreferencePosition.First,
                    summary = "Disabled, so the row does nothing",
                    enabled = false,
                    trailing = { CouiSwitch(checked = true, onCheckedChange = {}, enabled = false) },
                    showDivider = true,
                )
                CouiPreferenceItem(
                    title = "Locked off",
                    position = CouiPreferencePosition.Last,
                    enabled = false,
                    trailing = { CouiSwitch(checked = false, onCheckedChange = {}, enabled = false) },
                )
            }

            CouiSmallTitle("Drag")

            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CouiPreferenceItem(
                    title = "Auto-rotate screen",
                    position = CouiPreferencePosition.Single,
                    summary = "Drag the thumb left or right, then let go",
                    onClick = { autoRotate = !autoRotate },
                    trailing = { CouiSwitch(checked = autoRotate, onCheckedChange = { autoRotate = it }) },
                )
            }

            CouiSmallTitle("SeekBar")

            // ScreenMinBrightnessPreference - hosts a SettingsSeekBar, which extends COUISeekBar
            // with no attributes at all, so it gets the plain defaults.
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    CouiText("ScreenMinBrightnessPreference", CouiTypography.body)
                    Spacer(Modifier.height(4.dp))
                    CouiSeekBar(
                        value = minBrightness,
                        onValueChange = { minBrightness = it },
                        modifier = Modifier.fillMaxWidth().height(CouiSeekBarDefaults.MinHeight),
                    )
                }
            }

            // SettingsBrightnessPreference - hosts an OplusToggleSliderView, which inflates
            // status_bar_toggle_slider.xml.
            CouiCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    CouiText("SettingsBrightnessPreference", CouiTypography.body)
                    Spacer(Modifier.height(4.dp))
                    CouiSeekBar(
                        value = brightness,
                        onValueChange = { brightness = it },
                        backgroundEnlargeScale = CouiStatusBarToggleSlider.EnlargeScale,
                        trackColor = CouiStatusBarToggleSlider.TrackColor,
                        modifier = Modifier.fillMaxWidth().height(CouiStatusBarToggleSlider.Height),
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
