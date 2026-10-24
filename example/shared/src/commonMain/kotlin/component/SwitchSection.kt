// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiSwitch
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.preference.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.preference.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography


fun LazyListScope.switchSection() {
    item(key = "switch") {
        val switch = remember { mutableStateOf(false) }
        val switchTrue = remember { mutableStateOf(true) }
        val superSwitch = remember { mutableStateOf("false") }
        val superSwitchState = remember { mutableStateOf(false) }
        val superSwitchAnimState = remember { mutableStateOf(false) }
        // CouiSmallTitle is the `Widget.COUI.List.Category.Title` port (32dp start margin).
        io.github.wfc35286.coui.kmp.basic.CouiSmallTitle(text = "Switch")
        CouiCard(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CouiSwitch(
                    checked = switch.value,
                    onCheckedChange = { switch.value = it },
                )
                CouiSwitch(
                    checked = switchTrue.value,
                    onCheckedChange = { switchTrue.value = it },
                    modifier = Modifier.padding(start = 6.dp),
                )
                CouiSwitch(
                    checked = false,
                    onCheckedChange = { },
                    modifier = Modifier.padding(start = 6.dp),
                    enabled = false,
                )
                CouiSwitch(
                    checked = true,
                    onCheckedChange = { },
                    modifier = Modifier.padding(start = 6.dp),
                    enabled = false,
                )
            }
            CouiPreferenceItem(
                title = "Switch",
                summary = "Click to expand a Switch",
                position = CouiPreferencePosition.First,
                trailing = {
                    CouiSwitch(
                        checked = superSwitchAnimState.value,
                        onCheckedChange = { superSwitchAnimState.value = it },
                    )
                },
            )
            AnimatedVisibility(
                visible = superSwitchAnimState.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiPreferenceItem(
                    title = "Switch",
                    position = CouiPreferencePosition.First,
                    trailing = {
                        CouiSwitch(
                            checked = superSwitchState.value,
                            onCheckedChange = {
                                superSwitchState.value = it
                                superSwitch.value = "$it"
                            },
                        )
                    },
                    // The state text the miuix demo showed next to the switch.
                    summary = superSwitch.value,
                )
            }
            CouiPreferenceItem(
                title = "Disabled Switch",
                enabled = false,
                trailing = {
                    CouiSwitch(
                        checked = true,
                        onCheckedChange = { },
                        enabled = false,
                    )
                },
            )
        }
    }
}
