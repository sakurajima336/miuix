// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.blur

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One blend step of the COUI material blur.
 *
 * Mirrors `android.graphics.BlendMode` as far as ColorOS uses it: every preset in
 * `com.coui.appcompat.COUIMaterialBlurEffect` is built from these four modes only.
 */
enum class CouiBlendMode {
    /** `BlendMode.SRC_OVER` - the tint simply covers the blurred backdrop. */
    SrcOver,

    /** `BlendMode.OVERLAY` - keeps the backdrop's own light/dark structure. */
    Overlay,

    /** `BlendMode.COLOR_DODGE` - lightens, used for the light theme's "milky" wash. */
    ColorDodge,

    /** `BlendMode.LUMINOSITY` - takes only the luminance, used as the second layer. */
    Luminosity,
}

/**
 * The COUI material blur, transcribed from `com.coui.appcompat.COUIMaterialBlurEffect$BlurEffectParams`.
 *
 * The official chain (see `COUIMaterialBlurEffect$Companion.setViewBlurEffect`) is exactly:
 *
 * ```
 * RenderEffect e = RenderEffect.createBlurEffect(r, r, TileMode.MIRROR)
 * e = RenderEffect.createColorFilterEffect(BlendModeColorFilter(colorLayer1, blendModeLayer1), e)
 * e = RenderEffect.createColorFilterEffect(BlendModeColorFilter(colorLayer2, blendModeLayer2), e)
 * OplusViewBackgroundRenderEffect.setBackgroundRenderEffect(e, view)
 * ```
 *
 * so the parameters here are the only inputs the material has: a blur radius in **raw pixels**
 * (`getBlurRadius()` returns the stored float untouched - there is no dp conversion anywhere in the
 * official code) and two tinted layers.
 */
@Immutable
data class CouiBlurParams(
    /** Blur radius in pixels, `RenderEffect.createBlurEffect` units. */
    val blurRadiusPx: Float,
    /** First tint layer colour, applied on top of the blurred backdrop. */
    val colorLayer1: Color,
    /** Blend mode of the first tint layer. */
    val blendModeLayer1: CouiBlendMode,
    /** Second tint layer colour. */
    val colorLayer2: Color,
    /** Blend mode of the second tint layer. */
    val blendModeLayer2: CouiBlendMode,
) {
    /** True when the official code would call `setBackgroundRenderEffect(view, null)`. */
    val isNoEffect: Boolean
        get() = blurRadiusPx <= 0f && colorLayer1.alpha == 0f && colorLayer2.alpha == 0f

    companion object {
        /** `COUIMaterialBlurEffect.NO_EFFECT`. */
        val NoEffect: CouiBlurParams =
            CouiBlurParams(
                blurRadiusPx = 0f,
                colorLayer1 = Color.Transparent,
                blendModeLayer1 = CouiBlendMode.SrcOver,
                colorLayer2 = Color.Transparent,
                blendModeLayer2 = CouiBlendMode.SrcOver,
            )
    }
}

/**
 * The material presets of `com.coui.appcompat.COUIMaterialBlurEffect`, decoded from its `<clinit>`.
 *
 * The light theme is built from `COLOR_DODGE + LUMINOSITY` (a milky wash that keeps the page's
 * structure), the dark theme from `SRC_OVER/OVERLAY + LUMINOSITY`.
 */
object CouiBlurPresets {
    // ---- light theme -------------------------------------------------------------------------

    /** `FRAMEWORK_TOP_BAR_PARAMS_LIGHT` - material without blur (radius 0). */
    val TopBarLight: CouiBlurParams =
        CouiBlurParams(0f, Color(0x66333333), CouiBlendMode.ColorDodge, Color(0x99F5F5F5), CouiBlendMode.Luminosity)

    /** `FRAMEWORK_TOP_BAR_BLUR_PARAMS_LIGHT` - the top app bar with blur. */
    val TopBarBlurLight: CouiBlurParams =
        CouiBlurParams(20f, Color(0x66333333), CouiBlendMode.ColorDodge, Color(0x99F5F5F5), CouiBlendMode.Luminosity)

    /** `FRAMEWORK_SEARCH_BAR_NO_MATERIAL_LIGHT`. */
    val SearchBarNoMaterialLight: CouiBlurParams =
        CouiBlurParams(0f, Color(0xD6333333), CouiBlendMode.ColorDodge, Color(0x66F5F5F5), CouiBlendMode.Luminosity)

    /** `FRAMEWORK_BOTTOM_BAR_PARAMS_LIGHT` - the bottom navigation / toolbar material. */
    val BottomBarLight: CouiBlurParams =
        CouiBlurParams(25f, Color(0xB3666666), CouiBlendMode.ColorDodge, Color(0x99F5F5F5), CouiBlendMode.Luminosity)

    /** `FRAMEWORK_TOP_BAR_PARAMS1` - the faint variant used while content scrolls under the bar. */
    val TopBarFaintLight: CouiBlurParams =
        CouiBlurParams(25f, Color(0x1A6E6E6E), CouiBlendMode.ColorDodge, Color(0x24F1F1F1), CouiBlendMode.Luminosity)

    /** `CONTENT_SECONDARY_BUTTON_PARAMS_LIGHT`. */
    val SecondaryButtonLight: CouiBlurParams =
        CouiBlurParams(25f, Color(0xCCFFFFFF), CouiBlendMode.SrcOver, Color(0x00FFFFFF), CouiBlendMode.SrcOver)

    /** `CONTENT_TRANSPARENT_BUTTON_PARAMS_LIGHT`. */
    val TransparentButtonLight: CouiBlurParams =
        CouiBlurParams(25f, Color(0x3F333333), CouiBlendMode.Overlay, Color(0x3FFFFFFF), CouiBlendMode.Luminosity)

    /** `CONTENT_SEGMENT_BUTTON_PARAMS_LIGHT`. */
    val SegmentButtonLight: CouiBlurParams =
        CouiBlurParams(25f, Color(0x7F333333), CouiBlendMode.ColorDodge, Color(0x99EFEFEF), CouiBlendMode.Luminosity)

    /** `TYPE_CONTENT_CHIP_TAB_SELECTED_PARAMS_LIGHT`. */
    val ChipTabSelectedLight: CouiBlurParams =
        CouiBlurParams(40f, Color(0x66A3A1A1), CouiBlendMode.ColorDodge, Color(0x99F5F5F5), CouiBlendMode.Luminosity)

    /** `TYPE_CONTENT_CHIP_TAB_PARAMS_LIGHT`. */
    val ChipTabLight: CouiBlurParams =
        CouiBlurParams(40f, Color(0x144E4E4E), CouiBlendMode.ColorDodge, Color(0x14606060), CouiBlendMode.Luminosity)

    /** `TYPE_CONTENT_CHIP_TAB_SELECTED_NO_BLUR_PARAMS_LIGHT`. */
    val ChipTabSelectedNoBlurLight: CouiBlurParams =
        CouiBlurParams(0f, Color(0x66A3A3A3), CouiBlendMode.ColorDodge, Color(0xCEF5F5F5), CouiBlendMode.Luminosity)

    /** `TYPE_CONTENT_CHIP_TAB_NO_BLUR_PARAMS_LIGHT`. */
    val ChipTabNoBlurLight: CouiBlurParams =
        CouiBlurParams(0f, Color(0x9A9B9B9B), CouiBlendMode.ColorDodge, Color(0x9ACECECE), CouiBlendMode.Luminosity)

    // ---- dark theme --------------------------------------------------------------------------

    /** `FRAMEWORK_TOP_BAR_PARAMS_DARK`. */
    val TopBarDark: CouiBlurParams =
        CouiBlurParams(0f, Color(0xCC262626), CouiBlendMode.SrcOver, Color(0xB37F7F7F), CouiBlendMode.Overlay)

    /** `FRAMEWORK_TOP_BAR_BLUR_PARAMS_DARK`. */
    val TopBarBlurDark: CouiBlurParams =
        CouiBlurParams(20f, Color(0xCC262626), CouiBlendMode.SrcOver, Color(0xB37F7F7F), CouiBlendMode.Overlay)

    /** `FRAMEWORK_SEARCH_BAR_NO_MATERIAL_DARK`. */
    val SearchBarNoMaterialDark: CouiBlurParams =
        CouiBlurParams(0f, Color(0xCC262626), CouiBlendMode.SrcOver, Color(0xB37F7F7F), CouiBlendMode.Overlay)

    /** `FRAMEWORK_BOTTOM_BAR_PARAMS_DARK`. */
    val BottomBarDark: CouiBlurParams =
        CouiBlurParams(25f, Color(0x80666666), CouiBlendMode.Overlay, Color(0xB3262626), CouiBlendMode.Luminosity)

    /** `CONTENT_SECONDARY_BUTTON_PARAMS_DARK`. */
    val SecondaryButtonDark: CouiBlurParams =
        CouiBlurParams(25f, Color(0x33FFFFFF), CouiBlendMode.SrcOver, Color(0x00FFFFFF), CouiBlendMode.SrcOver)

    /** `CONTENT_TRANSPARENT_BUTTON_PARAMS_DARK`. */
    val TransparentButtonDark: CouiBlurParams =
        CouiBlurParams(25f, Color(0x3F333333), CouiBlendMode.Overlay, Color(0x3FFFFFFF), CouiBlendMode.Luminosity)

    /** `CONTENT_SEGMENT_BUTTON_PARAMS_DARK`. */
    val SegmentButtonDark: CouiBlurParams =
        CouiBlurParams(25f, Color(0x7F7F7F7F), CouiBlendMode.Overlay, Color(0x99262626), CouiBlendMode.Luminosity)

    /** `TYPE_CONTENT_CHIP_TAB_SELECTED_PARAMS_DARK`. */
    val ChipTabSelectedDark: CouiBlurParams =
        CouiBlurParams(40f, Color(0xA6505050), CouiBlendMode.Luminosity, Color(0x4CA7A7A7), CouiBlendMode.Overlay)

    /** `TYPE_CONTENT_CHIP_TAB_PARAMS_DARK`. */
    val ChipTabDark: CouiBlurParams =
        CouiBlurParams(40f, Color(0x33B2B2B2), CouiBlendMode.Luminosity, Color(0x33303030), CouiBlendMode.Overlay)

    /** `TYPE_CONTENT_CHIP_TAB_SELECTED_NO_BLUR_PARAMS_DARK`. */
    val ChipTabSelectedNoBlurDark: CouiBlurParams =
        CouiBlurParams(0f, Color(0xDA888888), CouiBlendMode.Luminosity, Color(0x80A8A8A8), CouiBlendMode.Overlay)

    /** `TYPE_CONTENT_CHIP_TAB_NO_BLUR_PARAMS_DARK`. */
    val ChipTabNoBlurDark: CouiBlurParams =
        CouiBlurParams(0f, Color(0xEE474747), CouiBlendMode.Luminosity, Color(0xCE8D8D8D), CouiBlendMode.Overlay)

    /** Picks the light/dark variant of a preset pair. */
    fun select(
        dark: Boolean,
        light: CouiBlurParams,
        darkParams: CouiBlurParams,
    ): CouiBlurParams = if (dark) darkParams else light
}