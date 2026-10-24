// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.graphics
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.wfc35286.coui.kmp.graphics.CouiSpotLightShader
import io.github.wfc35286.coui.kmp.graphics.createCouiSpotLightShader
import io.github.wfc35286.coui.kmp.graphics.spotLightBrush
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * The COUI "spot light": a point light that appears under the finger when a surface is pressed and
 * then follows the finger while it moves.
 *
 * The light itself is the AGSL shader in [CouiSpotLightShader];
 * [startIntensity] → [endIntensity] and [startRadius] → [endRadius] are driven by a single
 * `progress` animated with a stiff spring (bounce 0, response 0.15), the position follows the
 * finger with **one spring per axis** (bounce 0.15, response 0.35), and [blendMode] selects how
 * the light is composited over the content ([BlendMode.Overlay], [BlendMode.Plus] or
 * [BlendMode.SrcOver]).
 */
@Immutable
data class CouiSpotLightSpec(
    /** How the light is composited over the content. */
    val blendMode: BlendMode = BlendMode.Overlay,
    /** The light intensity at `progress = 0` (always 0 in the COUI presets). */
    val startIntensity: Float = 0f,
    /** The light intensity at `progress = 1`. */
    val endIntensity: Float,
    /** The light radius at `progress = 0`, in pixels. */
    val startRadius: Float = 100f,
    /** The light radius at `progress = 1`, in pixels. */
    val endRadius: Float,
    /** The light's height above the surface, in pixels. */
    val lightHeight: Float = 200f,
    /** The ambient light strength. */
    val ambientStrength: Float = 0.4f,
    /** The diffuse light strength. */
    val diffuseStrength: Float = 0.4f,
    /** The dithering amount. */
    val dither: Float = 0f,
    /** The noise amount. */
    val noise: Float = 0.08f,
    /** The light colour; every COUI preset is white. */
    val color: Color = Color.White,
)

/**
 * Preset light parameters grouped by widget family: capsules, circles / round rects, the switch
 * thumb and chip tabs, and opaque / translucent / transparent surfaces.
 *
 * Members named `…Light` / `…Dark` are theme-specific; the `fun …(dark: Boolean)` forms resolve
 * both themes.
 */
object CouiSpotLightDefaults {
    // ---- capsule ---------------------------------------------------------------------------

    /** Small capsule: the light grows from 100 to 400 px with the light 80 px above the surface. */
    val Capsule1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 400f, lightHeight = 80f, ambientStrength = 0.1f, diffuseStrength = 0.5f, dither = 0f, noise = 0f)

    /** The dark-theme variant; identical to [Capsule1Light]. */
    val Capsule1Dark: CouiSpotLightSpec = Capsule1Light

    /** Large capsule: the light grows to 500 px with dithering and noise. */
    val Capsule2Light: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 500f, dither = 1f, noise = 0.3f)

    /** Same geometry as [Capsule2Light], but with [BlendMode.Plus] and a much weaker end intensity. */
    val Capsule2Dark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 500f, dither = 0f, noise = 0.1f)

    // ---- circle / round rect ---------------------------------------------------------------

    /** A circle, e.g. for a FAB. */
    val Circle1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.2f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** The dark-theme variant; identical to [Circle1Light]. */
    val Circle1Dark: CouiSpotLightSpec = Circle1Light

    /** A round rect. */
    val RoundRect1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 1.3f, endRadius = 600f, lightHeight = 120f, ambientStrength = 0.2f, diffuseStrength = 0.2f, dither = 1f, noise = 0.08f)

    /** The dark-theme variant; the light starts from radius 0. */
    val RoundRect1Dark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.4f, startRadius = 0f, endRadius = 600f, lightHeight = 100f, ambientStrength = 0.1f, diffuseStrength = 0.5f, dither = 1f, noise = 0.02f)

    // ---- widgets ---------------------------------------------------------------------------

    /** The switch thumb. */
    val SwitchLight: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 0.8f, endRadius = 200f, lightHeight = 100f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** The dark-theme variant; identical to [SwitchLight]. */
    val SwitchDark: CouiSpotLightSpec = SwitchLight

    /** A chip tab. */
    val ChipTabLight: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.2f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** The dark-theme variant: [BlendMode.SrcOver] with end intensity 0.3. */
    val ChipTabDark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.3f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    // ---- opaque / translucent / transparent families ----------------------------------------

    /** A small opaque surface. */
    fun OpaqueSmall1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.5f else 0.6f, endRadius = 500f)

    /** A small opaque surface. */
    fun OpaqueSmall2(dark: Boolean): CouiSpotLightSpec =
        if (dark) {
            CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 500f)
        } else {
            CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 500f)
        }

    /** A medium opaque surface - the seek bar's preset. */
    fun OpaqueMedium1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.8f else 0.6f, endRadius = 500f)

    /**
     * A medium opaque surface - the floating bottom navigation bar's preset.
     *
     * Both themes share `startRadius = 100`, `endRadius = 500`, `lightHeight = 200`,
     * `ambient = 0.4`, `diffuse = 0.4` and `noise = 0.08`; only `endIntensity`
     * (1.0 light / 0.15 dark), `blendMode` ([BlendMode.Overlay] light / [BlendMode.Plus] dark) and
     * `dither` (0 light / 1.0 dark) differ.
     */
    fun OpaqueMedium2(dark: Boolean): CouiSpotLightSpec =
        if (dark) {
            CouiSpotLightSpec(
                blendMode = BlendMode.Plus,
                endIntensity = 0.15f,
                endRadius = 500f,
                dither = 1f,
            )
        } else {
            CouiSpotLightSpec(
                endIntensity = 1.0f,
                endRadius = 500f,
                dither = 0f,
            )
        }

    /** A large opaque surface. */
    fun OpaqueLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 0.6f, endRadius = 800f)

    /** A large opaque surface. */
    fun OpaqueLarge2(dark: Boolean): CouiSpotLightSpec =
        if (dark) {
            CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 800f)
        } else {
            CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 800f)
        }

    /** A small translucent surface. */
    fun TranslucentSmall1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.4f, endRadius = 500f)

    /** A small translucent surface. */
    fun TranslucentSmall2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.4f, endRadius = 500f)

    /** A medium translucent surface. */
    fun TranslucentMedium2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.4f, endRadius = 500f)

    /** A large translucent surface. */
    fun TranslucentLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.1f else 0.6f, endRadius = 800f)

    /** A large translucent surface. */
    fun TranslucentLarge2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.1f else 0.4f, endRadius = 800f)

    /** A small transparent surface (a light-theme-only preset). */
    val TransparentSmall1: CouiSpotLightSpec = CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 400f)

    /** A small transparent surface. */
    val TransparentSmall2: CouiSpotLightSpec = CouiSpotLightSpec(endIntensity = 0.7f, endRadius = 400f)

    /** A medium transparent surface. */
    fun TransparentMedium1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.5f, endRadius = 500f)

    /** A medium transparent surface. */
    fun TransparentMedium2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.4f, endRadius = 500f)

    /** A large transparent surface. */
    fun TransparentLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.3f, endRadius = 800f)

    /** A transparent window surface, composited with [BlendMode.SrcOver]. */
    val TransparentWindowSmall1: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.32f, endRadius = 400f)

    /** A medium transparent window surface. */
    val TransparentWindowMedium1: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.32f, startRadius = 200f, endRadius = 500f)
}

/** The follow spring: one per axis, bounce 0.15 / response 0.35. */
private val CouiSpotLightFollowSpring = CouiSpringData(bounce = 0.15f, response = 0.35f).toSpringSpec(1e-4f)

/** The glow-up spring: bounce 0 / response 0.15. */
private val CouiSpotLightGlowSpring = CouiSpringData(bounce = 0f, response = 0.15f).toSpringSpec(1e-4f)

/**
 * The extinguish spring: bounce 0 / response 0.4, so releasing is noticeably slower than lighting
 * up.
 */
private val CouiSpotLightExtinguishSpring = CouiSpringData(bounce = 0f, response = 0.4f).toSpringSpec(1e-4f)

/**
 * The mutable state of one spot light: where the finger is, and how far the glow has progressed.
 *
 * [press] springs the glow up and either snaps the light to the touch point or lets it travel
 * there from where the previous press left it, [moveTo] lets each axis chase the finger on its
 * own spring, [release] springs the glow back down (and blooms it out).
 *
 * [position] is in **normalized** coordinates (0..1): callers hand over `position / size`, never
 * raw pixels, and the drawing path multiplies them back by the surface resolution.
 */
@Stable
class CouiSpotLightState internal constructor(
    private val scope: CoroutineScope,
) {
    private val lightX = Animatable(0.5f)
    private val lightY = Animatable(0.5f)
    private val glow = Animatable(0f)

    internal val shader: CouiSpotLightShader? = createCouiSpotLightShader()

    /** True between [press] and [release]. */
    var pressed: Boolean by mutableStateOf(false)
        private set

    /**
     * True while the extinguish animation runs, i.e. between [release] and the next [press].
     *
     * While it is set the draw path adds an extra radius (`2 * endRadius`) scaled by
     * `1 - progress` to the light, so it **blooms outwards** as it fades instead of shrinking
     * back. See [drawCouiSpotLight].
     */
    var extinguishing: Boolean by mutableStateOf(false)
        private set

    /** Current light position, normalized to the drawing surface (`0..1` on both axes). */
    val position: Offset
        get() = Offset(lightX.value, lightY.value)

    /** Current glow progress, `0` (invisible) … `1` (fully expanded). */
    val progress: Float
        get() = glow.value

    /**
     * Presses the light down. [at] is a **normalized** position (0..1).
     *
     * Whether the light snaps under the finger or travels to it is decided by the current
     * progress:
     *
     *     if (progress != 0f) animate the position there;
     *     else snap it straight under the finger.
     *
     * Progress reaches exactly 0 only when the extinguish spring settles, which takes about
     * 0.8 s, while the light is already invisible after about 0.45 s. Pressing again inside that
     * ~350 ms window makes the light fly over from where it was left; pressing later makes it
     * light up in place. The position is never reset, and the follow spring starts from it.
     */
    fun press(at: Offset) {
        pressed = true
        extinguishing = false
        if (glow.value != 0f) {
            // Still inside the extinguish animation: carry the light over with the follow spring
            // (bounce 0.15 / response 0.35) instead of snapping it under the finger.
            scope.launch { lightX.animateTo(at.x, CouiSpotLightFollowSpring) }
            scope.launch { lightY.animateTo(at.y, CouiSpotLightFollowSpring) }
        } else {
            // progress == 0: a fresh light, placed straight under the finger.
            scope.launch {
                lightX.snapTo(at.x)
                lightY.snapTo(at.y)
            }
        }
        scope.launch { glow.animateTo(1f, CouiSpotLightGlowSpring) }
    }

    /** [at] is a **normalized** position (`0..1`). */
    fun moveTo(at: Offset) {
        if (!pressed) return
        scope.launch { lightX.animateTo(at.x, CouiSpotLightFollowSpring) }
        scope.launch { lightY.animateTo(at.y, CouiSpotLightFollowSpring) }
    }

    fun release() {
        pressed = false
        // The bloom radius only applies while this flag is set; it is cleared by the next press
        // (which springs the glow back up in 0.15s).
        extinguishing = true
        scope.launch { glow.animateTo(0f, CouiSpotLightExtinguishSpring) }
    }
}

/** Creates a [CouiSpotLightState] bound to this composition's coroutine scope. */
@Composable
fun rememberCouiSpotLightState(): CouiSpotLightState {
    val scope = rememberCoroutineScope()
    return remember(scope) { CouiSpotLightState(scope) }
}

/**
 * Paints the spot light of [state] on top of this modifier's content, clipped to [shape], and
 * tracks the finger so the light follows it while pressed.
 *
 * ```kotlin
 * Box(Modifier.couiSpotLight(CouiSpotLightDefaults.Capsule2Light, shape = RoundedCornerShape(50))) { … }
 * ```
 *
 * The gesture is installed by the modifier itself, so a plain surface gets the light with this
 * one call.
 *
 * The finger position is converted to the **normalized** space [CouiSpotLightState.position]
 * uses.
 */
fun Modifier.couiSpotLight(
    spec: CouiSpotLightSpec,
    shape: Shape = RectangleShape,
    state: CouiSpotLightState,
): Modifier =
    this
        .pointerInput(state, spec) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                state.press(Offset(down.position.x / w, down.position.y / h))
                var pointer = down.id
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointer } ?: event.changes.firstOrNull()
                        if (change == null) break
                        pointer = change.id
                        if (change.pressed) {
                            state.moveTo(Offset(change.position.x / w, change.position.y / h))
                        } else {
                            break
                        }
                        change.consume()
                    }
                } finally {
                    state.release()
                }
            }
        }
        .drawWithContent {
            drawContent()
            drawCouiSpotLight(state, spec, shape)
        }

/** [couiSpotLight] with a state created for this composition. */
@Composable
fun Modifier.couiSpotLight(
    spec: CouiSpotLightSpec,
    shape: Shape = RectangleShape,
): Modifier {
    val state = rememberCouiSpotLightState()
    return this.couiSpotLight(spec, shape, state)
}

/**
 * Paints the light of [state] over this surface, for hosts that drive the state themselves (a
 * component's own gesture, an animation, a test).
 *
 * ```kotlin
 * Box(Modifier.background(panel, shape)) {
 *     CouiSpotLightSurface(state, CouiSpotLightDefaults.OpaqueMedium2(dark = true), shape)
 *     Content()
 * }
 * ```
 */
@Composable
fun CouiSpotLightSurface(
    state: CouiSpotLightState,
    spec: CouiSpotLightSpec,
    shape: Shape = RectangleShape,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawCouiSpotLight(state, spec, shape)
                },
    )
}

/**
 * Draws the light without installing a gesture, for components that already track the pointer
 * themselves (the seek bar, the floating navigation bar).
 *
 * [state] holds a **normalized** position; the shader multiplies it by [DrawScope.size] before it
 * reaches the GPU.
 */
internal fun DrawScope.drawCouiSpotLight(
    state: CouiSpotLightState,
    spec: CouiSpotLightSpec,
    shape: Shape,
) {
    val shader = state.shader ?: return
    val progress = state.progress
    if (progress <= 0.001f) return
    // While extinguishing, an extra radius of `2 * endRadius`, scaled by `1 - progress`, is added
    // on top of the start→end lerp. That is why the light keeps **expanding** while it fades:
    // 500 px -> 1100 px for the bar's preset (100 -> 500 lerp plus 1000 * (1 - progress)).
    val extinguishingExtra =
        if (state.extinguishing) spec.endRadius * 2f * (1f - progress) else 0f
    val radius =
        spec.startRadius + (spec.endRadius - spec.startRadius) * progress + extinguishingExtra
    val intensity = spec.startIntensity + (spec.endIntensity - spec.startIntensity) * progress
    shader.setLightColor(spec.color.red, spec.color.green, spec.color.blue)
    shader.setAmbientStrength(spec.ambientStrength)
    shader.setDiffuseStrength(spec.diffuseStrength)
    shader.setLightHeight(spec.lightHeight)
    shader.setNoise(spec.noise)
    shader.setDitherN(spec.dither)
    shader.setLightRadius(radius)
    shader.setIntensity(intensity)
    // Resolution first: the packed light-position uniform embeds position × resolution, so the
    // resolution must be set before the position.
    shader.setResolution(size.width, size.height)
    val p = state.position
    shader.setLightPosition(p.x, p.y)
    val brush = spotLightBrush(shader)
    val path = shape.toSpotLightPath(size, this)
    clipPath(path) {
        drawRect(brush = brush, topLeft = Offset.Zero, size = size, blendMode = spec.blendMode)
    }
}

private fun Shape.toSpotLightPath(size: Size, density: Density): Path =
    when (val outline = createOutline(size, LayoutDirection.Ltr, density)) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }