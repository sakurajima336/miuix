// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import component.CouiBlurSection
import io.github.wfc35286.coui.kmp.theme.CouiTheme


/**
 * Debug-only host for [CouiBlurSection] so the blur can be launched and screenshotted directly:
 *
 * ```
 * adb shell am start -n top.yukonga.miuix.uitest/.CouiBlurProbeActivity
 * ```
 */
class CouiBlurProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CouiTheme {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(CouiTheme.colors.backgroundWithCard)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 48.dp),
                ) {
                    CouiBlurSection()
                }
            }
        }
    }
}