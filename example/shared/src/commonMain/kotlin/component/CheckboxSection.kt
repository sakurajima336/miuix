// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.basic.CouiCard
import io.github.wfc35286.coui.kmp.basic.CouiSmallTitle
import io.github.wfc35286.coui.kmp.basic.CouiSwitch
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.preference.CouiPreferenceItem
import io.github.wfc35286.coui.kmp.preference.CouiPreferencePosition
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTypography

/**
 * Checkbox rows. coui-core has no standalone checkbox yet (the popup menu ships `CouiMenuCheckbox`
 * as a private helper), so this section mirrors the official Settings check row shape with
 * `CouiPreferenceItem` + a trailing `CouiSwitch`-sized state slot placeholder. FIDELITY NOTE: the
 * trailing control is a stand-in, not the COUI checkbox; port `coui_menu_ic_checkbox` when the
 * standalone control is built.
 */
fun LazyListScope.checkboxSection() {
    item(key = "checkbox") {
        var checked by remember { mutableStateOf(false) }
        var checkedEnd by remember { mutableStateOf(true) }
        var checkedDisabled by remember { mutableStateOf(true) }
        CouiSmallTitle(text = "Checkbox")
        CouiCard(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            CouiPreferenceItem(
                title = "Checkbox",
                summary = "State: $checked",
                position = CouiPreferencePosition.First,
                onClick = { checked = !checked },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        CouiText(
                            text = "$checked",
                            style = CouiTypography.listAssignmentEnd,
                            emphasis = CouiTextEmphasis.Secondary,
                        )
                        // Placeholder state slot until the standalone COUI checkbox lands.
                        CouiSwitch(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                },
            )
            CouiPreferenceItem(
                title = "Checkbox",
                position = CouiPreferencePosition.Last,
                enabled = false,
                onClick = { },
                trailing = {
                    Column {
                        CouiText(
                            text = "Disabled",
                            style = CouiTypography.listAssignmentEnd,
                            emphasis = CouiTextEmphasis.Secondary,
                        )
                        CouiSwitch(
                            checked = checkedEnd,
                            onCheckedChange = { checkedEnd = it },
                            enabled = false,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                },
            )
            // Keep the unused disabled state referenced so the demo state list stays symmetric.
            if (checkedDisabled) {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    CouiText(
                        text = "Disabled state is wired through `enabled` on each row.",
                        style = CouiTypography.listAssignmentTiny,
                        emphasis = CouiTextEmphasis.Secondary,
                    )
                }
            }
        }
    }
}