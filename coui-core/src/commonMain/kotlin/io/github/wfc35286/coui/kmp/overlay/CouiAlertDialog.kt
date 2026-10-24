// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiFontSize
import io.github.wfc35286.coui.kmp.token.CouiTextEmphasis
import io.github.wfc35286.coui.kmp.token.CouiTextStyle

/** The measured values of the alert dialog, from the ColorOS dialog resources. */
object CouiAlertDialogDefaults {
    /** The widest the dialog panel grows. */
    val MaxWidth = 392.dp

    /** The tallest the dialog panel grows. */
    val MaxHeight = 632.dp

    /** The panel corner radius. */
    val CornerRadius = 16.dp

    /** The gap above the title. */
    val TitleMarginTop = 24.dp

    /** The gap between the title and the message. */
    val TitleMarginBottom = 6.dp

    /** The horizontal padding of the title and message block. */
    val HorizontalPadding = 24.dp

    /** The shortest one dialog button row is. */
    val ButtonMinHeight = 44.dp

    /** The vertical padding inside one button row. */
    val ButtonVerticalPadding = 4.dp

    /** The horizontal padding inside one button row. */
    val ButtonHorizontalPadding = 12.dp

    /** The horizontal padding around the button stack. */
    val ButtonAreaPaddingHorizontal = 16.dp

    /** The gap between the button stack and the panel bottom. */
    val ButtonAreaPaddingBottom = 16.dp

    /** The dim the dialog casts on the content behind it. */
    val ScrimColor = Color(0x33000000)

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

    /** The title style: 18sp semibold. */
    val titleStyle = CouiTextStyle(CouiFontSize.Scaled(18f), FontWeight.SemiBold)

    /** The message style: 13sp. */
    val messageStyle = CouiTextStyle(CouiFontSize.Scaled(13f))

    /** The button style: 16sp medium. */
    val buttonStyle = CouiTextStyle(CouiFontSize.Scaled(16f), FontWeight.Medium)
}

/**
 * A [CouiAlertDialog] component with COUI style.
 *
 * The centre confirmation dialog of ColorOS: a 392dp-wide rounded panel over a dimmed page,
 * with a centred semibold title, a centred secondary-coloured message, and the actions as a
 * vertical stack of full-width borderless text buttons - the dismissing action on top, the
 * confirming action at the bottom, in the error colour when it is destructive. The panel
 * fades in and scales from 0.8 to 1.0 on a 0.45s critically damped spring and dismisses on a
 * drier 0.18s one. The back gesture and a tap on the dimmed area dismiss through
 * [onDismissRequest].
 *
 * @param visible Whether the dialog is shown. Setting it to `false` plays the dismiss travel
 *   and then removes the dialog.
 * @param title The dialog title.
 * @param onDismissRequest Called when the user dismisses the dialog without confirming.
 * @param confirmText The label of the confirming button, at the bottom of the stack.
 * @param onConfirm Called when the confirming button is clicked.
 * @param modifier The modifier to apply to the dialog panel.
 * @param message The dialog message, shown under the title.
 * @param dismissText The label of the dismissing button above the confirming one. `null`
 *   leaves the confirming button alone.
 * @param destructive Whether the confirming button is drawn in the error colour, for
 *   destructive actions such as deleting.
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
    destructive: Boolean = false,
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
                    .heightIn(max = CouiAlertDialogDefaults.MaxHeight)
                    .graphicsLayer {
                        val p = progress.value
                        alpha = p
                        val scale = CouiAlertDialogDefaults.EnterScale + (1f - CouiAlertDialogDefaults.EnterScale) * p
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(CouiAlertDialogDefaults.CornerRadius))
                    .background(colors.backgroundElevated),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(CouiAlertDialogDefaults.TitleMarginTop))
                    Box(modifier = Modifier.padding(horizontal = CouiAlertDialogDefaults.HorizontalPadding)) {
                        CouiText(
                            text = title,
                            style = CouiAlertDialogDefaults.titleStyle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(CouiAlertDialogDefaults.TitleMarginBottom))
                    if (message != null) {
                        Box(modifier = Modifier.padding(horizontal = CouiAlertDialogDefaults.HorizontalPadding)) {
                            CouiText(
                                text = message,
                                style = CouiAlertDialogDefaults.messageStyle,
                                emphasis = CouiTextEmphasis.Secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(CouiAlertDialogDefaults.ButtonAreaPaddingBottom))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CouiAlertDialogDefaults.ButtonAreaPaddingHorizontal),
                    ) {
                        if (dismissText != null) {
                            DialogButton(
                                text = dismissText,
                                color = colors.labelPrimary,
                                onClick = onDismissRequest,
                            )
                        }
                        DialogButton(
                            text = confirmText,
                            color = if (destructive) colors.error else colors.labelPrimary,
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
    }
}

/** One borderless action row of the dialog's button stack. */
@Composable
private fun DialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CouiAlertDialogDefaults.ButtonMinHeight)
            .clickable(onClick = onClick)
            .padding(
                horizontal = CouiAlertDialogDefaults.ButtonHorizontalPadding,
                vertical = CouiAlertDialogDefaults.ButtonVerticalPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CouiText(
            text = text,
            style = CouiAlertDialogDefaults.buttonStyle,
            color = color,
        )
    }
}
