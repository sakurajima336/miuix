// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.token
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color


/**
 * The complete ColorOS colour palette used across the coui components.
 *
 * COUI's neutrals are alpha-based (one ink colour at many opacities) instead of a set of
 * distinct greys, which is why so many entries look like the same colour at different alphas.
 */
@Immutable
data class CouiColorTokens(
    /** The text colour of a default button in its disabled state. */
    val couiBtnDefaultTextColorDisable: Color,
    /** The drawable tint of a button in its disabled state. */
    val couiBtnDrawableColorDisabled: Color,
    /** The inner fill of a checkbox that is checked but disabled. */
    val couiBtnPartCheckInnerColorOnDisabled: Color,
    /** The fill colour of a radio button in its on state. */
    val couiBtnRadioColorOn: Color,
    /** The text colour of a secondary button in its disabled state. */
    val couiBtnSecondaryTextColorDisable: Color,
    /** An additional accent colour. */
    val additional: Color,
    /** The message text colour in AI payment dialogs. */
    val aiPaymentDialogMessage: Color,
    /** The title text colour in AI payment dialogs. */
    val aiPaymentDialogTitle: Color,
    /** The primary background of AI payment content. */
    val aiPaymentPrimary: Color,
    /** The disabled variant of the AI payment primary background. */
    val aiPaymentPrimaryDisabled: Color,
    /** The text colour on AI payment primary surfaces. */
    val aiPaymentPrimaryText: Color,
    /** The disabled variant of the AI payment primary text colour. */
    val aiPaymentPrimaryTextDisabled: Color,
    /** The secondary background of AI payment content. */
    val aiPaymentSecondary: Color,
    /** The disabled variant of the AI payment secondary background. */
    val aiPaymentSecondaryDisabled: Color,
    /** The text colour on AI payment secondary surfaces. */
    val aiPaymentSecondaryText: Color,
    /** The disabled variant of the AI payment secondary text colour. */
    val aiPaymentSecondaryTextDisabled: Color,
    /** The assistant accent colour. */
    val assist: Color,
    /** The azure accent colour. */
    val azure: Color,
    /** A variant of [azure]. */
    val azureVariant: Color,
    /** The default window background. */
    val background: Color,
    /** The background of elevated surfaces. */
    val backgroundElevated: Color,
    /** The background of elevated surfaces on pages that host cards. */
    val backgroundElevatedWithCard: Color,
    /** The background of the window's top area. */
    val backgroundTop: Color,
    /** The background on pages that host cards. */
    val backgroundWithCard: Color,
    /** The background of top and bottom bars. */
    val bar: Color,
    /** The blue accent colour. */
    val blue: Color,
    /** A variant of [blue]. */
    val blueVariant: Color,
    /** The background of the bottom bar. */
    val bottomBar: Color,
    /** The fill colour of cards. */
    val card: Color,
    /** The background colour of a card. */
    val cardBackground: Color,
    /** The fill colour of a card while pressed. */
    val cardPressed: Color,
    /** A neutral container overlay at 12% alpha. */
    val container12: Color,
    /** A neutral container overlay at 16% alpha. */
    val container16: Color,
    /** A neutral container overlay at 4% alpha. */
    val container4: Color,
    /** A neutral container overlay at 8% alpha. */
    val container8: Color,
    /** The container fill for azure-tinted content. */
    val containerAzure: Color,
    /** The container fill for blue-tinted content. */
    val containerBlue: Color,
    /** The container fill for cyan-tinted content. */
    val containerCyan: Color,
    /** The container fill for green-tinted content. */
    val containerGreen: Color,
    /** The container fill for mint-tinted content. */
    val containerMint: Color,
    /** The container fill for orange-tinted content. */
    val containerOrange: Color,
    /** The container fill for purple-tinted content. */
    val containerPurple: Color,
    /** The container fill for red-tinted content. */
    val containerRed: Color,
    /** The solid neutral container fill. */
    val containerSolid: Color,
    /** The theme-coloured container fill. */
    val containerTheme: Color,
    /** A semi-transparent (halftone) theme-coloured container fill. */
    val containerThemeHalftone: Color,
    /** The container fill for violet-tinted content. */
    val containerViolet: Color,
    /** The container fill for yellow-tinted content. */
    val containerYellow: Color,
    /** The neutral fill of controls such as sliders and switch tracks. */
    val controls: Color,
    /** The cyan accent colour. */
    val cyan: Color,
    /** A variant of [cyan]. */
    val cyanVariant: Color,
    /** The disabled variant of the theme colour. */
    val disable: Color,
    /** The neutral colour for disabled content. */
    val disabledNeutral: Color,
    /** The colour of dividers. */
    val divider: Color,
    /** The error accent colour. */
    val error: Color,
    /** The disabled variant of [error]. */
    val errorDisabled: Color,
    /** The background colour behind error text. */
    val errorTextBg: Color,
    /** The solid neutral fill colour. */
    val fillSolid: Color,
    /** The thin (4%-alpha) neutral fill colour. */
    val fillThin: Color,
    /** The focus highlight colour. */
    val focus: Color,
    /** The outline colour drawn around focused content. */
    val focusOutline: Color,
    /** The green accent colour. */
    val green: Color,
    /** A variant of [green]. */
    val greenVariant: Color,
    /** The neutral hint text colour. */
    val hintNeutral: Color,
    /** The overlay colour applied while hovered. */
    val hover: Color,
    /** The label colour used on top of theme-coloured surfaces. */
    val labelOnColor: Color,
    /** The primary label colour. */
    val labelPrimary: Color,
    /** A variant of [labelPrimary]. */
    val labelPrimaryVariant: Color,
    /** The quaternary label colour. */
    val labelQuaternary: Color,
    /** The secondary label colour. */
    val labelSecondary: Color,
    /** A variant of [labelSecondary]. */
    val labelSecondaryVariant: Color,
    /** The tertiary label colour. */
    val labelTertiary: Color,
    /** The theme-coloured label colour. */
    val labelTheme: Color,
    /** The link colour; an alias of [labelTheme]. */
    val link: Color,
    /** The scrim (mask) colour drawn over content. */
    val mask: Color,
    /** The mint accent colour. */
    val mint: Color,
    /** A variant of [mint]. */
    val mintVariant: Color,
    /** The content colour drawn on top of [primary]. */
    val onPrimary: Color,
    /** The content colour drawn on top of [secondary]; an alias of [primaryText]. */
    val onSecondary: Color,
    /** The orange accent colour. */
    val orange: Color,
    /** A variant of [orange]. */
    val orangeVariant: Color,
    /** The overlay colour applied while pressed. */
    val press: Color,
    /** The background colour applied while pressed. */
    val pressBackground: Color,
    /** The theme primary colour. */
    val primary: Color,
    /** The neutral primary colour. */
    val primaryNeutral: Color,
    /** The primary text colour. */
    val primaryText: Color,
    /** The primary text colour used on popups. */
    val primaryTextOnPopup: Color,
    /** The purple accent colour. */
    val purple: Color,
    /** A variant of [purple]. */
    val purpleVariant: Color,
    /** The quaternary neutral colour. */
    val quarternaryNeutral: Color,
    /** The red accent colour. */
    val red: Color,
    /** A variant of [red]. */
    val redVariant: Color,
    /** The ripple colour used while pressed. */
    val ripplePressBackground: Color,
    /** The secondary neutral colour. */
    val secondNeutral: Color,
    /** The secondary theme colour. */
    val secondary: Color,
    /** The success accent colour. */
    val success: Color,
    /** The surface colour. */
    val surface: Color,
    /** The surface colour of the window's top area. */
    val surfaceTop: Color,
    /** The surface colour on pages that host cards. */
    val surfaceWithCard: Color,
    /** The accent colour for tips. */
    val tips: Color,
    /** The violet accent colour. */
    val violet: Color,
    /** A variant of [violet]. */
    val violetVariant: Color,
    /** The yellow accent colour. */
    val yellow: Color,
    /** A variant of [yellow]. */
    val yellowVariant: Color,
    /** The default text colour. */
    val couiDefaultTextColor: Color,
    /** The background of an edit text's clear button; an alias of [labelTertiary]. */
    val couiEditTextDeleteBgColorNormal: Color,
    /** The primary text colour. */
    val couiPrimaryTextColor: Color,
) {
    companion object {
        /** The light colour scheme. */
        val Light: CouiColorTokens = CouiColorTokens(
            couiBtnDefaultTextColorDisable = Color(0x8AFFFFFF),
            couiBtnDrawableColorDisabled = Color(0x14000000),
            couiBtnPartCheckInnerColorOnDisabled = Color(0xFFFAFAFA),
            couiBtnRadioColorOn = Color(0x8AFFFFFF),
            couiBtnSecondaryTextColorDisable = Color(0x42000000),
            additional = Color(0xFF0080FF),
            aiPaymentDialogMessage = Color(0xE6000000),
            aiPaymentDialogTitle = Color(0xE6000000),
            aiPaymentPrimary = Color(0xFF001D38),
            aiPaymentPrimaryDisabled = Color(0x4D001D38),
            aiPaymentPrimaryText = Color(0xFFFFFFFF),
            aiPaymentPrimaryTextDisabled = Color(0x8AFFFFFF),
            aiPaymentSecondary = Color(0x14000000),
            aiPaymentSecondaryDisabled = Color(0x14000000),
            aiPaymentSecondaryText = Color(0xE6000000),
            aiPaymentSecondaryTextDisabled = Color(0x42000000),
            assist = Color(0xFF5C93E5),
            azure = Color(0xFF00A1FF),
            azureVariant = Color(0xFF45ABE5),
            background = Color(0xFFFFFFFF),
            backgroundElevated = Color(0xFFFFFFFF),
            backgroundElevatedWithCard = Color(0xFFF0F1F2),
            backgroundTop = Color(0xFFFFFFFF),
            backgroundWithCard = Color(0xFFF0F1F2),
            bar = Color(0xFFFAFAFA),
            blue = Color(0xFF0080FF),
            blueVariant = Color(0xFF5C93E5),
            bottomBar = Color(0xFFFAFAFA),
            card = Color(0xFFFFFFFF),
            cardBackground = Color(0xFFFFFFFF),
            cardPressed = Color(0xFFE6E6E6),
            container12 = Color(0x1F000000),
            container16 = Color(0x29000000),
            container4 = Color(0x0A000000),
            container8 = Color(0x14000000),
            containerAzure = Color(0xFFCEE5EB),
            containerBlue = Color(0xFFC9DAE4),
            containerCyan = Color(0xFFBEDADD),
            containerGreen = Color(0xFFBADECA),
            containerMint = Color(0xFFBADEDB),
            containerOrange = Color(0xFFFAE7D6),
            containerPurple = Color(0xFFE0D6EB),
            containerRed = Color(0xFFEEDADC),
            containerSolid = Color(0xFFEBEBEB),
            containerTheme = Color(0xFF0080FF),
            containerThemeHalftone = Color(0x260080FF),
            containerViolet = Color(0xFFD6D5EE),
            containerYellow = Color(0xFFF6E7C6),
            controls = Color(0x29000000),
            cyan = Color(0xFF1DBACC),
            cyanVariant = Color(0xFF39AFBD),
            disable = Color(0x4D0080FF),
            disabledNeutral = Color(0x42000000),
            divider = Color(0x1F000000),
            error = Color(0xFFDB382C),
            errorDisabled = Color(0x4DDB382C),
            errorTextBg = Color(0x4DE82A18),
            fillSolid = Color(0xFFEBEBEB),
            fillThin = Color(0x0A000000),
            focus = Color(0x1F0080FF),
            focusOutline = Color(0x660080FF),
            green = Color(0xFF00BD13),
            greenVariant = Color(0xFF4BBD57),
            hintNeutral = Color(0x42000000),
            hover = Color(0x14000000),
            labelOnColor = Color(0xFFFFFFFF),
            labelPrimary = Color(0xE6000000),
            labelPrimaryVariant = Color(0xB8000000),
            labelQuaternary = Color(0x14000000),
            labelSecondary = Color(0x8A000000),
            labelSecondaryVariant = Color(0x66000000),
            labelTertiary = Color(0x42000000),
            labelTheme = Color(0xFF0080FF),
            link = Color(0xFF0080FF),
            mask = Color(0x33000000),
            mint = Color(0xFF00CC88),
            mintVariant = Color(0xFF39BD91),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFF0080FF),
            orange = Color(0xFFFF7700),
            orangeVariant = Color(0xFFE58F45),
            press = Color(0x1F000000),
            pressBackground = Color(0x14000000),
            primary = Color(0xFF0080FF),
            primaryNeutral = Color(0xE6000000),
            primaryText = Color(0xFF0080FF),
            primaryTextOnPopup = Color(0xFF0080FF),
            purple = Color(0xFFA723FF),
            purpleVariant = Color(0xFFB873E5),
            quarternaryNeutral = Color(0x14000000),
            red = Color(0xFFEB3B2F),
            redVariant = Color(0xFFE56E65),
            ripplePressBackground = Color(0x99B4B4B4),
            secondNeutral = Color(0x8A000000),
            secondary = Color(0x260080FF),
            success = Color(0xFF00BD13),
            surface = Color(0xFFFFFFFF),
            surfaceTop = Color(0xFFFFFFFF),
            surfaceWithCard = Color(0xFFF0F1F2),
            tips = Color(0xFF0080FF),
            violet = Color(0xFF5A47FF),
            violetVariant = Color(0xFF7E73E5),
            yellow = Color(0xFFFFB200),
            yellowVariant = Color(0xFFE5AF2E),
            couiDefaultTextColor = Color(0xFF32CEB2),
            couiEditTextDeleteBgColorNormal = Color(0x42000000),
            couiPrimaryTextColor = Color(0xFF000000),
        )

        /** The dark colour scheme. */
        val Dark: CouiColorTokens = CouiColorTokens(
            couiBtnDefaultTextColorDisable = Color(0x28FFFFFF),
            couiBtnDrawableColorDisabled = Color(0x26FFFFFF),
            couiBtnPartCheckInnerColorOnDisabled = Color(0xFF050505),
            couiBtnRadioColorOn = Color(0xFF050505),
            couiBtnSecondaryTextColorDisable = Color(0x28FFFFFF),
            additional = Color(0xFF1A8CFF),
            aiPaymentDialogMessage = Color(0xE6FFFFFF),
            aiPaymentDialogTitle = Color(0xE6FFFFFF),
            aiPaymentPrimary = Color(0xFFDCE7FF),
            aiPaymentPrimaryDisabled = Color(0x4DDCE7FF),
            aiPaymentPrimaryText = Color(0xE6000000),
            aiPaymentPrimaryTextDisabled = Color(0x42000000),
            aiPaymentSecondary = Color(0x1AFFFFFF),
            aiPaymentSecondaryDisabled = Color(0x1AFFFFFF),
            aiPaymentSecondaryText = Color(0xE6FFFFFF),
            aiPaymentSecondaryTextDisabled = Color(0x42FFFFFF),
            assist = Color(0xFF5C93E5),
            azure = Color(0xFF209DE5),
            azureVariant = Color(0xFF3D98CC),
            background = Color(0xFF000000),
            backgroundElevated = Color(0xFF1E1E1E),
            backgroundElevatedWithCard = Color(0xFF1E1E1E),
            backgroundTop = Color(0xFF333333),
            backgroundWithCard = Color(0xFF000000),
            bar = Color(0xFF1E1E1E),
            blue = Color(0xFF1A8CFF),
            blueVariant = Color(0xFF5C93E5),
            bottomBar = Color(0xFFFAFAFA),
            card = Color(0x1AFFFFFF),
            cardBackground = Color(0x1AFFFFFF),
            cardPressed = Color(0x33FFFFFF),
            container12 = Color(0x33FFFFFF),
            container16 = Color(0x40FFFFFF),
            container4 = Color(0x1AFFFFFF),
            container8 = Color(0x1AFFFFFF),
            containerAzure = Color(0xFF373F42),
            containerBlue = Color(0xFF323739),
            containerCyan = Color(0xFF343E3D),
            containerGreen = Color(0xFF353E36),
            containerMint = Color(0xFF394342),
            containerOrange = Color(0xFF39332C),
            containerPurple = Color(0xFF3A3540),
            containerRed = Color(0xFF3A3030),
            containerSolid = Color(0xFF262626),
            containerTheme = Color(0xFF1A8CFF),
            containerThemeHalftone = Color(0x401A8CFF),
            containerViolet = Color(0xFF3A3A43),
            containerYellow = Color(0xFF373530),
            controls = Color(0x40FFFFFF),
            cyan = Color(0xFF25A9B8),
            cyanVariant = Color(0xFF329DA8),
            disable = Color(0x4D1A8CFF),
            disabledNeutral = Color(0x4DFFFFFF),
            divider = Color(0x33FFFFFF),
            error = Color(0xFFFF6C61),
            errorDisabled = Color(0x4DDB382C),
            errorTextBg = Color(0xFFEA3939),
            fillSolid = Color(0xFF262626),
            fillThin = Color(0x14FFFFFF),
            focus = Color(0x331A8CFF),
            focusOutline = Color(0x665C9DFF),
            green = Color(0xFF24B232),
            greenVariant = Color(0xFF43A84E),
            hintNeutral = Color(0x4DFFFFFF),
            hover = Color(0x26FFFFFF),
            labelOnColor = Color(0xFFFFFFFF),
            labelPrimary = Color(0xE6FFFFFF),
            labelPrimaryVariant = Color(0xB8FFFFFF),
            labelQuaternary = Color(0x26FFFFFF),
            labelSecondary = Color(0x8AFFFFFF),
            labelSecondaryVariant = Color(0x66FFFFFF),
            labelTertiary = Color(0x4DFFFFFF),
            labelTheme = Color(0xFF1A8CFF),
            link = Color(0xFF1A8CFF),
            mask = Color(0x99000000),
            mint = Color(0xFF1AB883),
            mintVariant = Color(0xFF32A881),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFF5C9DFF),
            orange = Color(0xFFF08222),
            orangeVariant = Color(0xFFCC7F3D),
            press = Color(0x33FFFFFF),
            pressBackground = Color(0x26FFFFFF),
            primary = Color(0xFF1A8CFF),
            primaryNeutral = Color(0xE6FFFFFF),
            primaryText = Color(0xFF5C9DFF),
            primaryTextOnPopup = Color(0xFF5C9DFF),
            purple = Color(0xFFAC33FF),
            purpleVariant = Color(0xFFA466CC),
            quarternaryNeutral = Color(0x26FFFFFF),
            red = Color(0xFFEB493D),
            redVariant = Color(0xFFCC615A),
            ripplePressBackground = Color(0x80FFFFFF),
            secondNeutral = Color(0x8AFFFFFF),
            secondary = Color(0x401A8CFF),
            success = Color(0xFF24B232),
            surface = Color(0xFF1E1E1E),
            surfaceTop = Color(0xFF333333),
            surfaceWithCard = Color(0xFF1E1E1E),
            tips = Color(0xFF1A8CFF),
            violet = Color(0xFF7161FF),
            violetVariant = Color(0xFF7E73E5),
            yellow = Color(0xFFE5A100),
            yellowVariant = Color(0xFFC29427),
            couiDefaultTextColor = Color(0xFF30C67E),
            couiEditTextDeleteBgColorNormal = Color(0x4DFFFFFF),
            couiPrimaryTextColor = Color(0xFFFFFFFF),
        )
    }
}
