// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue


/**
 * Pins the two `ShadowEdge` uniform packers against the reference values transcribed straight out
 * of `设置_17.0.0.apk` (`com.coui.appcompat.shadowedge.GradientStrokeStyleMapper.toEdgeArray16`
 * and `InnerShadowStyleMapper.toShadowArray16`).
 *
 * The reference numbers come from `shadowedge_reference.py`, which is a line-for-line Python
 * transcription of the same smali - so a mismatch here means the Kotlin drifted, not that the
 * expectation moved.
 */
class CouiShadowEdgePackingTest {
    private fun assertClose(
        expected: FloatArray,
        actual: FloatArray,
        tol: Float = 1e-3f,
    ) {
        assertTrue(expected.size == actual.size, "size ${actual.size} != ${expected.size}")
        for (i in expected.indices) {
            assertTrue(
                abs(expected[i] - actual[i]) <= tol,
                "index $i: expected ${expected[i]} got ${actual[i]}",
            )
        }
    }

    @Test
    fun `packEdge matches the official mapper for a wide capsule`() {
        // 5 items on a 417dp screen -> ~376dp x 56dp pill, density 3.
        //
        // Decoded from `GradientStrokeStyleMapper.smali` + `GradientStrokeStyleKt.smali`
        // (2026-10-20 pass). The two things that used to be wrong and that these numbers pin:
        //
        //  * the tail is `out[4] += out[3]` / `out[6] += out[5]` (outer += inner), so the rim's
        //    outer bounds are the wide ones - 6.5 and 4.25, not 4.5 / 2.25;
        //  * `rectLineFadeForSize` drives both of its branches from **w** (the normalised width),
        //    which on this pill (wN = 1105.88, hN = 164.71) is what makes the pair `0.3565/0.5139`
        //    instead of the negative `-0.0139/0.1435` the `h` variant produced.
        val expected =
            floatArrayOf(
                1f, 1f, 1f,
                2f, 6.5f, 2f, 4.25f, // near=[inner,outer] far=[inner,outer]
                0.56f, 0.8f, // alpha light
                0.35646f, 0.51387f, 0.33679f, 0.47452f, // uRectLineFade
                0f, 4f, 0.8f,
            )
        val actual = CouiShadowEdgePacking.packEdge(1128f, 168f, 6f, 0.8f, 0f, 3f)
        assertClose(expected, actual)
    }

    @Test
    fun `packEdge dark alpha`() {
        val expected =
            floatArrayOf(
                1f, 1f, 1f,
                2f, 6.5f, 2f, 4.25f,
                0.105f, 0.15f,
                0.35646f, 0.51387f, 0.33679f, 0.47452f,
                0f, 4f, 0.8f,
            )
        val actual = CouiShadowEdgePacking.packEdge(1128f, 168f, 6f, 0.15f, 0f, 3f)
        assertClose(expected, actual)
    }

    @Test
    fun `packEdge rim widths are cumulative towards the outside`() {
        // The shader reads `rectLineNear = (out[3], out[4])` and `rectLineFar = (out[5], out[6])`
        // as `(inner, outer)` and then evaluates `smoothstep(-outer, -inner, d)`. Both bounds are
        // **distance-inside-the-edge**, so `outer` must be the larger of the two. Summing the
        // other pair (`out[5] += out[4]`, `out[3] += out[6]`) leaves a 0.25 px sliver on one line
        // and piles the rest onto the *inner* bound - the rim then renders as a hairline at the
        // wrong alpha, which is exactly the "highlight looks nothing like the official" state.
        val e = CouiShadowEdgePacking.packEdge(1128f, 168f, 6f, 0.15f, 0f, 3f)
        assertTrue(e[4] > e[3], "near: outer ${e[4]} must exceed inner ${e[3]}")
        assertTrue(e[6] > e[5], "far: outer ${e[6]} must exceed inner ${e[5]}")
        // Reference values straight out of the smali for `lineWidth = dp2px(2) = 6` at density 3.
        assertTrue(abs(e[3] - 2f) < 1e-3f, "nearInner = ${e[3]}")
        assertTrue(abs(e[4] - 6.5f) < 1e-3f, "nearOuter = ${e[4]}")
        assertTrue(abs(e[5] - 2f) < 1e-3f, "farInner = ${e[5]}")
        assertTrue(abs(e[6] - 4.25f) < 1e-3f, "farOuter = ${e[6]}")
    }

    @Test
    fun `packShadow matches the official mapper`() {
        val expected =
            floatArrayOf(
                1f, 1f, 1f,
                0.1f, 0f, 0f, // uFadeAlpha = (shadowFadeIn, 0, 0)
                122.4f, // fadeIn * scale
                0f, 20.4f, // fadeOffset * scale
                1.10851f, 1.09107f, // fadeScale-derived
                4f, 0.9f,
                20.4f, // fadeOut * scale
                4f, 0.7f,
            )
        val actual = CouiShadowEdgePacking.packShadow(1128f, 168f, 3f, 1f, 0.1f, 0f)
        assertClose(expected, actual)
    }

    @Test
    fun `packShadow dark peak alpha is 0_01`() {
        val actual = CouiShadowEdgePacking.packShadow(1128f, 168f, 3f, 1f, 0.01f, 0f)
        assertTrue(abs(actual[3] - 0.01f) < 1e-4f, "uFadeAlpha.x = ${actual[3]}")
        assertTrue(abs(actual[4]) < 1e-4f && abs(actual[5]) < 1e-4f, "uFadeAlpha.yz must stay 0")
    }

    @Test
    fun `packEdge rim fade is directional, not uniform`() {
        // `rectLineFadeForSize` has to produce a fade pair whose two halves differ, otherwise the
        // shader's `smoothstep(fade.y*distanceAll, fade.x*distanceAll, distance)` degenerates and
        // the rim wraps the whole capsule at one constant alpha - the ":一圈围着" bug.
        // The decisive term is `span0 = (baseW + baseH) * 2`; using the fade pair there makes
        // `scaled` fall below `baseW`, take the `t = (h - rest) / span1` branch and go negative.
        val e = CouiShadowEdgePacking.packEdge(1128f, 168f, 6f, 0.8f, 0f, 3f)
        assertTrue(e[9] != e[10], "near fade must have two distinct bounds, got ${e[9]} / ${e[10]}")
        assertTrue(e[11] != e[12], "far fade must have two distinct bounds, got ${e[11]} / ${e[12]}")
        // A uniform rim would need both bounds below zero; the official pair straddles zero.
        assertTrue(e[10] > 0f && e[12] > 0f, "fade must reach positive, got ${e[10]} / ${e[12]}")
    }
}
