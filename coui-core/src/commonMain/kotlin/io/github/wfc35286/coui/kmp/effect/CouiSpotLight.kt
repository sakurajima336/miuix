// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.wfc35286.coui.kmp.effect

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitFirstDown
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
import io.github.wfc35286.coui.kmp.component.CouiSpotLightShader
import io.github.wfc35286.coui.kmp.component.createCouiSpotLightShader
import io.github.wfc35286.coui.kmp.component.spotLightBrush
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import io.github.wfc35286.coui.kmp.motion.toSpringSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The COUI "spot light": a point light that appears under the finger when a surface is pressed and
 * then follows the finger while it moves.
 *
 * Transcribed from `com.coui.appcompat.spotlight.COUISpotLightEffect` +
 * `COUISpotLightEffectDrawable` (`设置_17.0.0.apk` / `软件商店_26.9.4_CN.apk`):
 *
 * * the light itself is the AGSL shader in `CouiSpotLightShader.kt` (`agsl.SpotLight.AGSL_SOURCE`);
 * * [startIntensity] → [endIntensity] and [startRadius] → [endRadius] are driven by a single
 *   `progress` that the official animates with `ensureGlowAnimation()` -
 *   `COUISpringForce(bounce = 0f, response = 0.15f)`;
 * * the position follows the finger with **one spring per axis** - `ensureFollowHandAnimation()`,
 *   `COUISpringForce(bounce = 0.15f, response = 0.35f)`, `minimumVisibleChange = 1e-4f`;
 * * [blendMode] is `SpotLightParams.mBlendMode` (`OVERLAY`, `PLUS` or `SRC_OVER`).
 */
@Immutable
data class CouiSpotLightSpec(
    /** `SpotLightParams.mBlendMode`. */
    val blendMode: BlendMode = BlendMode.Overlay,
    /** `SpotLightParams.mStartIntensity` - the value `progress = 0` maps to (always 0 in COUI). */
    val startIntensity: Float = 0f,
    /** `SpotLightParams.mEndIntensity` - the value `progress = 1` maps to. */
    val endIntensity: Float,
    /** `SpotLightParams.mStartRadius`, in pixels. */
    val startRadius: Float = 100f,
    /** `SpotLightParams.mEndRadius`, in pixels. */
    val endRadius: Float,
    /** `SpotLightParams.mLightHeight` - the light's height above the surface, in pixels. */
    val lightHeight: Float = 200f,
    /** `SpotLightParams.mAmbientStrength`. */
    val ambientStrength: Float = 0.4f,
    /** `SpotLightParams.mDiffuseStrength`. */
    val diffuseStrength: Float = 0.4f,
    /** `SpotLightParams.mDitherN`. */
    val dither: Float = 0f,
    /** `SpotLightParams.mNoise`. */
    val noise: Float = 0.08f,
    /** The shader's `u_lightColorAndAmbient.rgb`; every COUI preset is white. */
    val color: Color = Color.White,
)

/**
 * The `SpotLightParams` table of `COUISpotLightEffect`, decoded from its `<clinit>`.
 *
 * The official ships 27 `SpotLightType`s that resolve to 44 parameter objects (light/dark pairs).
 * Widget aliases whose mapping is already confirmed against the call sites are provided as
 * functions; the rest are exposed under their family names.
 */
object CouiSpotLightDefaults {
    // ---- capsule ---------------------------------------------------------------------------

    /** `CAPSULE_PARAMS_1_LIGHT` - small capsule (radius 100 → 400, height 80). */
    val Capsule1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 400f, lightHeight = 80f, ambientStrength = 0.1f, diffuseStrength = 0.5f, dither = 0f, noise = 0f)

    /** `CAPSULE_PARAMS_1_DARK`. */
    val Capsule1Dark: CouiSpotLightSpec = Capsule1Light

    /** `CAPSULE_PARAMS_2_LIGHT` - large capsule (100 → 500, height 200, dither + noise). */
    val Capsule2Light: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 500f, dither = 1f, noise = 0.3f)

    /** `CAPSULE_PARAMS_2_DARK` - same geometry, `PLUS` blend and a much weaker end intensity. */
    val Capsule2Dark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 500f, dither = 0f, noise = 0.1f)

    // ---- circle / round rect ---------------------------------------------------------------

    /** `CIRCLE_PARAMS_1_LIGHT` - used by the FAB. */
    val Circle1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.2f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** `CIRCLE_PARAMS_1_DARK`. */
    val Circle1Dark: CouiSpotLightSpec = Circle1Light

    /** `ROUNDRECT_PARAMS_1_LIGHT`. */
    val RoundRect1Light: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 1.3f, endRadius = 600f, lightHeight = 120f, ambientStrength = 0.2f, diffuseStrength = 0.2f, dither = 1f, noise = 0.08f)

    /** `ROUNDRECT_PARAMS_1_DARK` - note the `startRadius = 0`. */
    val RoundRect1Dark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.4f, startRadius = 0f, endRadius = 600f, lightHeight = 100f, ambientStrength = 0.1f, diffuseStrength = 0.5f, dither = 1f, noise = 0.02f)

    // ---- widgets ---------------------------------------------------------------------------

    /** `SWITCH_PARAMS_LIGHT` - the switch thumb. */
    val SwitchLight: CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 0.8f, endRadius = 200f, lightHeight = 100f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** `SWITCH_PARAMS_DARK`. */
    val SwitchDark: CouiSpotLightSpec = SwitchLight

    /** `CHIP_TAB_PARAMS_LIGHT`. */
    val ChipTabLight: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.2f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    /** `CHIP_TAB_PARAMS_DARK` - `SRC_OVER`, end intensity 0.3. */
    val ChipTabDark: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.3f, endRadius = 600f, ambientStrength = 0.1f, diffuseStrength = 0.5f, noise = 0f)

    // ---- opaque / translucent / transparent families ----------------------------------------

    /** `TYPE_OPAQUE_SMALL_1_*`. */
    fun OpaqueSmall1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.5f else 0.6f, endRadius = 500f)

    /** `TYPE_OPAQUE_SMALL_2_*`. */
    fun OpaqueSmall2(dark: Boolean): CouiSpotLightSpec =
        if (dark) {
            CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 500f)
        } else {
            CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 500f)
        }

    /** `TYPE_OPAQUE_MEDIUM_1_*` - the seek bar's preset. */
    fun OpaqueMedium1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.8f else 0.6f, endRadius = 500f)

    /**
     * `TYPE_OPAQUE_MEDIUM_2_*` - the floating bottom navigation bar's preset
     * (`BaseBottomFloatingMenuView.initTouchEffectDrawable`).
     *
     * Both themes share `startRadius = 100`, `endRadius = 500`, `lightHeight = 200`,
     * `ambient = 0.4`, `diffuse = 0.4` and `noise = 0.08`; only `endIntensity`
     * (1.0 light / 0.15 dark), `blendMode` (`OVERLAY` light / `PLUS` dark) and `ditherN`
     * (0 light / 1.0 dark) differ. Read straight out of `COUISpotLightEffect.<clinit>`:
     * the light preset is built from the register set that
     * `TYPE_TRANSPARENT_SMALL_1_LIGHT` established - `startIntensity = 0`,
     * `ditherN = 0` - and the dark one from `TYPE_OPAQUE_SMALL_2_DARK`'s.
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

    /** `TYPE_OPAQUE_LARGE_1_*`. */
    fun OpaqueLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = 0.6f, endRadius = 800f)

    /** `TYPE_OPAQUE_LARGE_2_*`. */
    fun OpaqueLarge2(dark: Boolean): CouiSpotLightSpec =
        if (dark) {
            CouiSpotLightSpec(blendMode = BlendMode.Plus, endIntensity = 0.15f, endRadius = 800f)
        } else {
            CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 800f)
        }

    /** `TYPE_TRANSLUCENT_SMALL_1_*`. */
    fun TranslucentSmall1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.4f, endRadius = 500f)

    /** `TYPE_TRANSLUCENT_SMALL_2_*`. */
    fun TranslucentSmall2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.4f, endRadius = 500f)

    /** `TYPE_TRANSLUCENT_MEDIUM_2_*`. */
    fun TranslucentMedium2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.4f, endRadius = 500f)

    /** `TYPE_TRANSLUCENT_LARGE_1_*`. */
    fun TranslucentLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.1f else 0.6f, endRadius = 800f)

    /** `TYPE_TRANSLUCENT_LARGE_2_*`. */
    fun TranslucentLarge2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.1f else 0.4f, endRadius = 800f)

    /** `TYPE_TRANSPARENT_SMALL_1_*` (light only in the official table). */
    val TransparentSmall1: CouiSpotLightSpec = CouiSpotLightSpec(endIntensity = 1.0f, endRadius = 400f)

    /** `TYPE_TRANSPARENT_SMALL_2_LIGHT`. */
    val TransparentSmall2: CouiSpotLightSpec = CouiSpotLightSpec(endIntensity = 0.7f, endRadius = 400f)

    /** `TYPE_TRANSPARENT_MEDIUM_1_*`. */
    fun TransparentMedium1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.5f, endRadius = 500f)

    /** `TYPE_TRANSPARENT_MEDIUM_2_*`. */
    fun TransparentMedium2(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.3f else 0.4f, endRadius = 500f)

    /** `TYPE_TRANSPARENT_LARGE_1_*`. */
    fun TransparentLarge1(dark: Boolean): CouiSpotLightSpec =
        CouiSpotLightSpec(endIntensity = if (dark) 0.2f else 0.3f, endRadius = 800f)

    /** `TYPE_TRANSPARENT_WINDOW_SMALL_1` / `TYPE_TRANSPARENT_WINDOW_MEDIUM_1` (`SRC_OVER`). */
    val TransparentWindowSmall1: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.32f, endRadius = 400f)

    /** `TYPE_TRANSPARENT_WINDOW_MEDIUM_1`. */
    val TransparentWindowMedium1: CouiSpotLightSpec =
        CouiSpotLightSpec(blendMode = BlendMode.SrcOver, endIntensity = 0.32f, startRadius = 200f, endRadius = 500f)
}

/** `ensureFollowHandAnimation()`: one spring per axis, bounce 0.15 / response 0.35. */
private val CouiSpotLightFollowSpring = CouiSpringData(bounce = 0.15f, response = 0.35f).toSpringSpec(1e-4f)

/** `ensureGlowAnimation()`: bounce 0 / response 0.15 - the `glow()` direction. */
private val CouiSpotLightGlowSpring = CouiSpringData(bounce = 0f, response = 0.15f).toSpringSpec(1e-4f)

/**
 * `animate(false)` - the `extinguish()` direction. The official re-configures the very same
 * `COUISpringAnimation` with `bounce = 0` / `response = 0.4` before animating to 0, so releasing is
 * noticeably slower than lighting up.
 */
private val CouiSpotLightExtinguishSpring = CouiSpringData(bounce = 0f, response = 0.4f).toSpringSpec(1e-4f)

/**
 * The mutable state of one spot light: where the finger is, and how far the glow has progressed.
 *
 * Mirrors `COUISpotLightEffect`: [press] springs the glow up and either snaps the light to the
 * touch point or lets it travel there from where the previous press left it, [moveTo] lets each
 * axis chase the finger on its own spring, [release] springs the glow back down (and blooms it
 * out).
 *
 * [position] is in **normalized** coordinates (0..1), exactly like
 * `COUISpotLightEffect.mPositionX/Y` - the official divides the `MotionEvent` coordinates by the
 * drawable bounds before calling in (`setLightPositionByPointerIndex`), and
 * `SpotLightShader.flushIfDirty` multiplies them back by the resolution. Callers therefore hand
 * over `position / size`, never raw pixels.
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
     * `COUISpotLightEffect.mIsExtinguishAnimation`: while it is set the draw path adds
     * `mDiffuseExtinguishExtraRadius` (`2 * endRadius`) scaled by `1 - f` to the radius, so the light
     * **blooms outwards** as it fades instead of shrinking back. See `drawCouiSpotLight`.
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
     * [at] is a **normalized** position (0..1), matching
     * COUISpotLightEffectDrawable.setLightPositionByPointerIndex.
     *
     * Whether the light snaps under the finger or travels to it is decided by progress, exactly
     * like COUISpotLightEffect#setLightPosition:
     *
     *     if (mMoveAnimation && mProgress != 0f) animateToPosition(x, y);
     *     else { mPositionX = x; mPositionY = y; shader.setLightPosition(x, y); }
     *
     * mProgress reaches exactly 0 only when the extinguish spring settles - COUISpringAnimation
     * hard-assigns mValue = finalPosition at equilibrium - which takes about 0.8 s, while the light
     * is already invisible after about 0.45 s (intensity drops below 1%). Pressing again inside
     * that ~350 ms window makes the light fly over from where it was left; pressing later makes it
     * light up in place. mPositionX/Y are never reset, and the follow spring starts from them.
     *
     * A Compose Animatable also lands on its target exactly, so testing progress != 0 reproduces
     * both behaviours without an extra flag.
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
        // `extinguish()`: the bloom radius below only applies while this flag is set; it is cleared
        // by the next `press()` (which snaps the glow back up in 0.15s).
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
 * The official attaches the effect to a `View` and forwards its `onTouchEvent`; in Compose the
 * gesture is installed here, so a plain surface gets the same behaviour with one modifier.
 *
 * The finger position is converted to the **normalized** space the official uses - see
 * [CouiSpotLightState.position].
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
 * reaches the GPU, exactly like `SpotLightShader.flushIfDirty`.
 */
internal fun DrawScope.drawCouiSpotLight(
    state: CouiSpotLightState,
    spec: CouiSpotLightSpec,
    shape: Shape,
) {
    val shader = state.shader ?: return
    val progress = state.progress
    if (progress <= 0.001f) return
    // `COUISpotLightEffect#setProgress`:
    //     radius = startRadius + (endRadius - startRadius) * f
    //     if (mIsExtinguishAnimation && f > 0) radius += mDiffuseExtinguishExtraRadius * (1 - f)
    // with `mDiffuseExtinguishExtraRadius = 2 * endRadius` (`setRadiusRange`). That is why the light
    // keeps **expanding** while it fades: 500 px -> 1100 px for the bar's preset (100 -> 500 lerp
    // plus 1000 * (1 - f)).
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
    // Resolution first: the packed `u_lightPosAndHeight` vec4 embeds position × resolution, so the
    // official sets the resolution before the position (`onBoundsChange` → `setResolution`).
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