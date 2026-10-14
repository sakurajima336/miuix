// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/*
 * COUI's corner geometry, reproduced **verbatim** from
 * `com.coui.appcompat.roundRect.COUIShapePath.getRoundRectPath(Path, RectF, float, Z, Z, Z, Z)`.
 *
 * ## Why this is not a squircle
 *
 * It is tempting to model the ColorOS corner as the superellipse that libhwui's "Altair"
 * branch draws on the GPU (`approxSdSquircle` / `sdSquircle`, driven by a `weight`). That is a
 * *different* code path: it is reached through
 * [com.coui.appcompat.roundRect.COUIShapePath.getSmoothRoundRectPath], which hands the shape to
 * `OplusPathAdapter.addSmoothRoundRect` and stores the smoothing parameters in
 * `SkRRect.fSmoothWeight` / `fSmoothType` for the GPU to consume.
 *
 * Most COUI widgets - `COUIButton`, `COUIChipDrawable`, `COUISegmentButtonLayout`,
 * `COUIRoundDrawable`, `COUIInputListSelectedItemLayout`, `COUICardListSelectedItemLayout`,
 * `COUICodeInputView`, `COUISearchViewAnimate`, ... - instead call
 * [COUIRoundRectUtil.getPath], which delegates to `getRoundRectPath`. That overload builds a
 * plain Bézier path on the CPU with **no native call at all**, so the silhouette is fully
 * reproducible in Compose.
 *
 * ## The geometry
 *
 * Each corner is a straight-edge `lineTo` followed by three `cubicTo` segments. All distances
 * are measured *from the corner* and scaled by `s = radius / 100`:
 *
 * ```
 * u = radius / min(halfWidth, halfHeight)
 * A = u > 0.5 ? 1 - min(1, (u - 0.5) / 0.4) * 0.1387784 : 1     // shrink
 * C = u > 0.6 ? 1 + min(1, (u - 0.6) / 0.3) * 0.0424540 : 1     // expand
 * ```
 *
 * `A` multiplies the *far* span `128.19·s` (where the straight edge stops), `C` multiplies the
 * *near* span `83.62·s` (the control point closest to the corner). The straight-edge endpoints
 * are clamped with `min(halfExtent, value)`; the cubic control points are **not** clamped.
 *
 * Verified on a real device (ColorOS 17, `getOSVersionCode() = 40`) by rendering the official
 * path and this one into bitmaps and comparing pixel by pixel: identical for every tested
 * combination, including `radius == 0`, `radius >= halfExtent`, non-square rects and 16x16 px
 * shapes. See `verify/verify_corner.py` and the `CornerPixelProbe` device probe.
 */

/**
 * `COUIShapePath`'s control-point table, in units of `radius / 100`.
 *
 * `128.19` is where the straight edge ends, `83.62` is the outermost control point of the
 * corner, and the rest walk around the quarter turn. They are literal constants in the
 * decompiled `cubicTo` sequence - not derived from an arc approximation.
 */
private val TABLE = floatArrayOf(
    128.19f, // [0] far span (straight edge end)
    83.62f, // [1] near span (first control point)
    67.45f, // [2]
    51.16f, // [3]
    34.86f, // [4]
    22.07f, // [5]
    13.36f, // [6]
    4.64f, // [7] last control point before the edge
)

/** `0x3e0e1bf0` - the shrink factor applied to the far span. */
private const val SHRINK = 0.1387784f

/** `0x3d2de440` - the expand factor applied to the near span. */
private const val EXPAND = 0.0424540f

/**
 * Appends a COUI rounded rectangle to this [Path].
 *
 * The result is bit-identical to
 * `COUIShapePath.getRoundRectPath(path, rect, radius)` on ColorOS 17.
 *
 * @param left Left edge, in pixels.
 * @param top Top edge, in pixels.
 * @param right Right edge, in pixels.
 * @param bottom Bottom edge, in pixels.
 * @param radius Corner radius, in pixels. Values at or above half the shorter side produce
 *   a pill.
 */
fun Path.couiRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    val w = right - left
    val h = bottom - top
    val halfW = w / 2f
    val halfH = h / 2f
    val m = min(halfW, halfH)

    // u = radius / min(halfW, halfH). ColorOS uses the *half* extents here, not the full
    // ones, so a radius of half the short side is already u = 1.
    val u = if (m != 0f) radius / m else 0f

    val a = if (u > 0.5f) 1f - min(1f, (u - 0.5f) / 0.4f) * SHRINK else 1f
    val c = if (u > 0.6f) 1f + min(1f, (u - 0.6f) / 0.3f) * EXPAND else 1f

    val s = radius / 100f
    val far = TABLE[0] * s * a
    val near = TABLE[1] * s * c
    val t2 = TABLE[2] * s
    val t3 = TABLE[3] * s
    val t4 = TABLE[4] * s
    val t5 = TABLE[5] * s
    val t6 = TABLE[6] * s
    val t7 = TABLE[7] * s

    // Only the straight-edge endpoints are clamped to the half extent.
    val ax = min(halfW, far)
    val ay = min(halfH, far)

    moveTo(left + halfW, top)
    lineTo(right - ax, top)

    // top-right
    cubicTo(right - near, top, right - t2, top + t7, right - t3, top + t6)
    cubicTo(right - t4, top + t5, right - t5, top + t4, right - t6, top + t3)
    cubicTo(right - t7, top + t2, right, top + near, right, top + ay)

    lineTo(right, bottom - ay)

    // bottom-right
    cubicTo(right, bottom - near, right - t7, bottom - t2, right - t6, bottom - t3)
    cubicTo(right - t5, bottom - t4, right - t4, bottom - t5, right - t3, bottom - t6)
    cubicTo(right - t2, bottom - t7, right - near, bottom, right - ax, bottom)

    lineTo(left + ax, bottom)

    // bottom-left
    cubicTo(left + near, bottom, left + t2, bottom - t7, left + t3, bottom - t6)
    cubicTo(left + t4, bottom - t5, left + t5, bottom - t4, left + t6, bottom - t3)
    cubicTo(left + t7, bottom - t2, left, bottom - near, left, bottom - ay)

    lineTo(left, top + ay)

    // top-left
    cubicTo(left, top + near, left + t7, top + t2, left + t6, top + t3)
    cubicTo(left + t5, top + t4, left + t4, top + t5, left + t3, top + t6)
    cubicTo(left + t2, top + t7, left + near, top, left + ax, top)

    close()
}
/**
 * A COUI rounded rectangle as a Compose [Shape].
 *
 * Drop-in replacement for `RoundedCornerShape` that produces ColorOS's actual corner geometry
 * (see [CouiSquirclePath]) instead of a circular arc.
 *
 * @param corner The corner radius, applied to all four corners.
 */
data class CouiSquircleShape(val corner: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { corner.toPx() }
        val path = Path().apply {
            couiRoundRect(0f, 0f, size.width, size.height, r)
        }
        return Outline.Generic(path)
    }
}

/**
 * A COUI pill: every corner fully rounded.
 *
 * Equivalent to [CouiSquircleShape] with a radius of half the shorter side, which ColorOS
 * treats as `isFullyRoundedToCircle` and draws with the same table (no special case).
 */
data class CouiPillSquircleShape(val radius: Dp = 0.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { radius.toPx() }
        val effective = if (r > 0f) r else min(size.width, size.height) / 2f
        val path = Path().apply {
            couiRoundRect(0f, 0f, size.width, size.height, effective)
        }
        return Outline.Generic(path)
    }
}