// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * Debug-only: draws the panel material **one layer at a time** on a black backdrop so each
 * contribution can be measured on its own.
 *
 * The official is never more than two layers: `drawPanelStroke` (the `ShadowEdge` shader, which
 * produces both the rim and the inner fog) then `drawTouchEffectDrawable` (the spot light). When
 * the whole thing is composited over a blurred page it is impossible to tell which layer is wrong;
 * here each one is isolated.
 *
 * Rows, top to bottom:
 * 1. `EDGE ONLY`   - `u_edge = 1`, `u_shadow = 0` (the rim)
 * 2. `SHADOW ONLY` - `u_edge = 0`, `u_shadow = 1` (the fog)
 * 3. `BOTH`        - what `drawPanelStroke` actually paints
 * 4. `EDGE, LIGHT ALPHA` - the light-theme `edgeAlpha` (0.8) for comparison
 *
 * The uniform tables under each row are the exact `u_edgeArray` / `u_shadowArray` pushed to the
 * shader, so a wrong by-value can be spotted directly instead of inferred from the picture.
 */
@Composable
fun CouiPanelLayerProbe(
    dark: Boolean = true,
    densityOverride: Float? = null,
) {
    val density = densityOverride ?: LocalDensity.current.density
    val base = if (dark) CouiFloatingMaterialDark else CouiFloatingMaterialLight
    val spec = remember(dark, density) {
        base.copy(edgeWidthPx = 2.0f * density, applyShadow = true, applyCausticShadow = false)
    }
    val shader = remember { runCatching { CouiShadowEdge.Shader() }.getOrNull() }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LayerRow(
            label = if (dark) "EDGE ONLY (dark alpha ${spec.edgeAlpha})" else "EDGE ONLY (light alpha ${spec.edgeAlpha})",
            shader = shader,
            spec = spec,
            density = density,
            edge = true,
            shadow = false,
        )
        LayerRow(
            label = "SHADOW ONLY (fadeIn ${spec.shadowFadeIn})",
            shader = shader,
            spec = spec,
            density = density,
            edge = false,
            shadow = true,
        )
        LayerRow(
            label = "BOTH = drawPanelStroke",
            shader = shader,
            spec = spec,
            density = density,
            edge = true,
            shadow = true,
        )
        LayerRow(
            label = "EDGE ONLY, light-theme alpha 0.8",
            shader = shader,
            spec = spec.copy(edgeAlpha = 0.8f),
            density = density,
            edge = true,
            shadow = false,
        )
        // `uAngle` flips `flag = mix(-1, 1, step(ratio, 0.5)) * dot(...)`, which swaps which
        // side of the capsule the bright arc lands on. Rendering both 0.0 (the official
        // `edgeAngle`) and 0.5 side by side settles the direction question by eye instead of by
        // argument - and without decoding anything.
        LayerRow(
            label = "EDGE ONLY, alpha 0.8, uAngle=+0.5 (flipped)",
            shader = shader,
            spec = spec.copy(edgeAlpha = 0.8f, edgeAngle = 0.5f),
            density = density,
            edge = true,
            shadow = false,
        )
        val e = CouiShadowEdgePacking.packEdge(1000f, 168f, 2.0f * density, spec.edgeAlpha, spec.edgeAngle, density)
        val s =
            CouiShadowEdgePacking.packShadow(
                1000f,
                168f,
                density,
                spec.shadowFadeScale,
                spec.shadowFadeIn,
                spec.shadowFadeOut,
            )
        ProbeText("u_edgeArray (w=1000 h=168 d=$density)")
        ProbeText(e.joinToString(", ") { "%.4f".format(it) })
        ProbeText("u_shadowArray")
        ProbeText(s.joinToString(", ") { "%.4f".format(it) })
        ProbeText("rim near=[${e[3]}, ${e[4]}] far=[${e[5]}, ${e[6]}] alpha=[${e[7]}, ${e[8]}]")
        ProbeText("rim fade=[${e[9]}, ${e[10]}, ${e[11]}, ${e[12]}]  (negative => smoothstep bounds flip)")
        ProbeText("fog uFadeAlpha=[${s[3]}, ${s[4]}, ${s[5]}] fadeIn=${s[6]} offset=[${s[7]}, ${s[8]}]")
    }
}

@Composable
private fun LayerRow(
    label: String,
    shader: CouiShadowEdge.Shader?,
    spec: CouiFloatingMaterialSpec,
    density: Float,
    edge: Boolean,
    shadow: Boolean,
) {
    ProbeText(label)
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .drawWithContent {
                // Neutral-grey capsule body on a black page: the fill marks the exact bounds
                // (so the measurement script can find them without hard-coded coordinates)
                // while staying dark enough that the rim's own alpha dominates.
                drawRect(Color(0xFF303030))
                if (shader != null) {
                    drawCouiShadowEdge(shader, spec, density, edgeEnabled = edge, shadowEnabled = shadow)
                }
            },
    )
}

@Composable
private fun ProbeText(text: String) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style =
        androidx.compose.ui.text.TextStyle(
            color = Color(0xFF7CFFB2),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        ),
    )
}
