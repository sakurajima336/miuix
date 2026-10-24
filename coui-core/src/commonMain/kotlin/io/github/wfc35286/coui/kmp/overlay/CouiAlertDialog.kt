// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTypography

/** The chrome and motion values of the alert dialog. */
object CouiAlertDialogDefaults {
    /** The spring that shows the dialog: slow and heavy, no bounce. */
    val ShowSpring = CouiSpringData(bounce = 0f, response = 0.45f)

    /** The spring that hides the dialog: quick and dry, no bounce. */
    val HideSpring = CouiSpringData(bounce = 0f, response = 0.18f)

    /** The visible-change threshold of the show spring, in pixels. */
    const val ShowThresholdPx = 1f

    /** The visible-change threshold of the hide spring, in pixels. */
    const val HideThresholdPx = 10f

    /** The scale the dialog appears at; it grows to 1.0 as it settles in. */
    const val EnterScale = 0.8f

    /** The dim the dialog casts on the content behind it, at full progress. */
    val ScrimColor = Color(0x80000000)

    /** The radius of the dialog panel. */
    val CornerRadius = 24.dp

    /** The widest the dialog panel grows. */
    val MaxWidth = 336.dp

    /** The height of one button row. */
    val ButtonHeight = 52.dp

    /** The title style. */
    val titleStyle = CouiTypography.toolbarTitle

    /** The message style. */
    val messageStyle = CouiTypography.body
}

/**
 * A [CouiAlertDialog] component with COUI style.
 *
 * The centre-form confirmation dialog: a rounded panel over a dimmed page with a centred
 * title, a centred message, and a two-button footer row split by hairlines. The panel fades
 * in and scales from 0.8 to 1.0 on a 0.45s critically damped spring and dismisses on a drier
 * 0.18s one. The back gesture and a tap on the dimmed area both dismiss it through
 * [onDismissRequest].
 *
 * @param visible Whether the dialog is shown. Setting it to `false` plays the dismiss travel
 *   and then removes the dialog.
 * @param title The dialog title.
 * @param onDismissRequest Called when the user dismisses the dialog without confirming.
 * @param confirmText The label of the confirming button.
 * @param onConfirm Called when the confirming button is clicked.
 * @param modifier The modifier to apply to the dialog panel.
 * @param message The dialog message, shown under the title.
 * @param dismissText The label of the dismissing button; `null` hides the button and leaves a
 *   single full-width confirming button.
 */
@Composable
fun CouiAlertDialog(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = null,
) {
    var composed by remember { mutableStateOf(visible) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            composed = true
            progress.animateTo(1f, CouiAlertDialogDefaults.ShowSpring.toSpringSpec(CouiAlertDialogDefaults.ShowThresholdPx))
        } else if (composed) {
            progress.animateTo(0f, CouiAlertDialogDefaults.HideSpring.toSpringSpec(CouiAlertDialogDefaults.HideThresholdPx))
            composed = false
        }
    }
    if (!composed) return

    val colors = CouiTheme.colors
    val hairline = colors.divider
    // One physical pixel, never a fractional dp: on a 3x screen 0.5dp renders as a blurry 2px band.
    val hairlineDp = with(LocalDensity.current) { 1.toDp() }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(CouiAlertDialogDefaults.ScrimColor),
            )
            Box(
                modifier = modifier
                    .align(Alignment.Center)
                    .widthIn(max = CouiAlertDialogDefaults.MaxWidth)
                    .graphicsLayer {
                        val p = progress.value
                        alpha = p
                        val scale = CouiAlertDialogDefaults.EnterScale + (1f - CouiAlertDialogDefaults.EnterScale) * p
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(CouiAlertDialogDefaults.CornerRadius))
                    .background(colors.backgroundWithCard),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        CouiText(
                            text = title,
                            style = CouiAlertDialogDefaults.titleStyle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            CouiText(
                                text = message,
                                style = CouiAlertDialogDefaults.messageStyle,
                                emphasis = CouiTextEmphasis.Secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(hairlineDp)
                            .background(hairline),
                    )
                    Row(modifier = Modifier.fillMaxWidth().height(CouiAlertDialogDefaults.ButtonHeight)) {
                        if (dismissText != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { onDismissRequest() },
                                contentAlignment = Alignment.Center,
                            ) {
                                CouiText(
                                    text = dismissText,
                                    style = CouiAlertDialogDefaults.messageStyle,
                                    emphasis = CouiTextEmphasis.Secondary,
                                )
                            }
                            Box(modifier = Modifier.width(hairlineDp).fillMaxSize().background(hairline))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { onConfirm() },
                            contentAlignment = Alignment.Center,
                        ) {
                            CouiText(
                                text = confirmText,
                                style = CouiAlertDialogDefaults.messageStyle,
                                color = colors.labelPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
