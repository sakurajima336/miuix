// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.wfc35286.coui.kmp.effect.CouiSpotLightDefaults
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import kotlin.math.abs
import kotlin.math.max

/** Shared `touchAnim/releaseAnim`, textured-thumb and spotlight animations for both seek bars. */
@Stable
internal class CouiSeekBarPressMotion {
    var pressed = false
        private set
    var enlarge by mutableFloatStateOf(0f)
        private set
    var thumbScale by mutableFloatStateOf(0f)
        private set
    var textureAlpha by mutableFloatStateOf(0f)
        private set
    var layerCAlpha by mutableFloatStateOf(1f)
        private set
    var glowAlpha by mutableFloatStateOf(0f)
        private set
    var glowX by mutableFloatStateOf(0f)
        private set
    var glowY by mutableFloatStateOf(0f)
        private set

    private var enlargeVelocity = 0f
    private var thumbVelocity = 0f
    private var textureVelocity = 0f
    private var layerCVelocity = 0f
    private var glowVelocity = 0f
    private var lightXVelocity = 0f
    private var lightYVelocity = 0f
    private var lightTargetX = 0f
    private var lightTargetY = 0f
    private var lightWidth = 1f
    private var lightHeight = 1f
    private var heightTarget = 0f
    private var pendingRelease = false
    private val followHand = CouiSpringData(0.15f, 0.35f)

    fun down(x: Float, y: Float) {
        pressed = true
        pendingRelease = false
        heightTarget = 1f
        // StandardAnimationHandler cancels/restarts the radius spring. Core.touchAnim always
        // invokes startThumbScaleAnimation(true), including a down on the empty track.
        thumbVelocity = 0f
        lightPosition(x, y)
    }

    fun lightBounds(width: Float, height: Float) {
        lightWidth = width.coerceAtLeast(1f)
        lightHeight = height.coerceAtLeast(1f)
    }

    fun lightPosition(x: Float, y: Float) {
        lightTargetX = x
        lightTargetY = y
        // COUISpotLightEffect.setLightPosition snaps when mProgress == 0, before glow().
        // Only an already visible light follows the touch through the position springs.
        if (glowAlpha == 0f) {
            glowX = x
            glowY = y
            lightXVelocity = 0f
            lightYVelocity = 0f
        }
    }

    // setProgress: release expands the radius by 2 * endRadius * (1 - progress).
    // At exactly zero the original resets both intensity and radius to zero.
    val lightRadius: Float
        get() {
            val progress = glowAlpha.coerceIn(0f, 1f)
            if (progress == 0f) return 0f
            val radius = 100f + 400f * progress
            return if (pressed) radius else radius + 1000f * (1f - progress)
        }

    fun up(trackEnlarges: Boolean) {
        pressed = false
        thumbVelocity = 0f
        // COUISeekBar.getMinEnlargeHeight = (maxHeight - height) / 2. Below that gate,
        // Core.releaseAnim defers to checkPendingReleaseAndPost on EVERY height update.
        pendingRelease = trackEnlarges && heightTarget == 1f && enlarge < 0.5f
        if (!pendingRelease) heightTarget = 0f
    }

    fun advance(dt: Float): Boolean {
        if (dt <= 0f) return true
        // FloatValueHolder default minimumVisibleChange=1, valueThreshold=0.75,
        // velocityThreshold=valueThreshold*62.5. Height animation uses 0..100, not 0..1.
        val heightMoving = step(
            enlarge,
            enlargeVelocity,
            heightTarget,
            SpringBounce0Response050,
            dt,
            valueThreshold = 0.0075f,
            velocityThreshold = 0.46875f,
        ) { v, vel ->
            enlarge = v
            enlargeVelocity = vel
        }
        var moving = heightMoving
        if (pendingRelease && (enlarge >= 0.5f || !heightMoving)) {
            pendingRelease = false
            heightTarget = 0f
            // initEnlargeAnim$lambda$0 calls checkPendingReleaseAndPost on each frame.
            // The end listener is only a fallback; waiting for it causes a long full-size pulse.
            moving = true
        }
        val scaleSpec = if (pressed) SpringBounce03Response050 else SpringBounce0Response050
        moving = step(thumbScale, thumbVelocity, if (pressed) 1f else 0f, scaleSpec, dt) { v, vel ->
            thumbScale = v
            thumbVelocity = vel
        } || moving
        moving = step(textureAlpha, textureVelocity, if (pressed) 1f else 0f, SpringBounce0Response030, dt) { v, vel ->
            textureAlpha = v
            textureVelocity = vel
        } || moving
        val plainDiscSpec = if (pressed) SpringBounce0Response050 else SpringBounce0Response030
        moving = step(layerCAlpha, layerCVelocity, if (pressed) 0f else 1f, plainDiscSpec, dt) { v, vel ->
            layerCAlpha = v
            layerCVelocity = vel
        } || moving
        val lightSpec = if (pressed) SpringBounce0Response015 else SpringBounce0Response040
        // Glow animates 0..10000 with FloatPropertyCompat's default 0.75 value threshold.
        moving = step(
            glowAlpha,
            glowVelocity,
            if (pressed) 1f else 0f,
            lightSpec,
            dt,
            valueThreshold = 0.000075f,
            velocityThreshold = 0.0046875f,
        ) { v, vel ->
            glowAlpha = v
            glowVelocity = vel
        } || moving
        // The original follows normalized x/y, with minimumVisibleChange=1e-4.
        // Keeping pixels here is equivalent after scaling both equilibrium thresholds.
        if (glowAlpha == 0f) {
            // setProgress(0) skips the two follow-hand springs to their final positions.
            glowX = lightTargetX
            glowY = lightTargetY
            lightXVelocity = 0f
            lightYVelocity = 0f
        } else {
            moving = step(
                glowX,
                lightXVelocity,
                lightTargetX,
                followHand,
                dt,
                valueThreshold = lightWidth * 0.000075f,
                velocityThreshold = lightWidth * 0.0046875f,
            ) { v, vel ->
                glowX = v
                lightXVelocity = vel
            } || moving
            moving = step(
                glowY,
                lightYVelocity,
                lightTargetY,
                followHand,
                dt,
                valueThreshold = lightHeight * 0.000075f,
                velocityThreshold = lightHeight * 0.0046875f,
            ) { v, vel ->
                glowY = v
                lightYVelocity = vel
            } || moving
        }
        return moving
    }
}

/** `COUIRubberAnimatorUnit`: rubber-band drag and a separate velocity-aware hand-up spring. */
@Stable
internal class CouiSeekBarRubberMotion {
    var shift by mutableFloatStateOf(0f)
        private set
    private var velocity = 0f
    private var target = 0f
    private var dragStartX = 0f
    private var spec = DragSpring

    fun down(x: Float, maxDistance: Float) {
        // Re-grabbing a returning bar preserves its current displacement (inverseRubberBand).
        val magnitude = abs(shift).coerceAtMost(maxDistance * 0.9999f)
        val effective = if (maxDistance > 0f) {
            magnitude * maxDistance / (0.05f * (maxDistance - magnitude))
        } else {
            0f
        }
        dragStartX = x - if (shift < 0f) -effective else effective
        target = shift
        velocity = 0f
        spec = DragSpring
    }

    fun move(x: Float, maxDistance: Float) {
        spec = DragSpring
        target = rubberBand(x - dragStartX, maxDistance)
    }

    fun up(velocityPxPerSecond: Float, maxDistance: Float, widthDp: Float, heightDp: Float) {
        // COUIVelocityTrackerHelper: computeCurrentVelocity(1), then multiply by 0.3.
        val eventVelocity = velocityPxPerSecond * 0.0003f
        val initial = if (maxDistance > 0f) {
            (eventVelocity / maxDistance * abs(shift)).coerceIn(-1000f, 1000f)
        } else {
            0f
        }
        val areaFraction = ((widthDp * heightDp - 56f * 56f) / (100f * 100f - 56f * 56f)).coerceIn(0f, 1f)
        val areaProgress = HandUpAreaEasing.transform(areaFraction)
        val response = 0.42f + (0.45f - 0.42f) * areaProgress
        val bounce = max(0.65f, abs(initial) / 1000f).coerceAtMost(1f)
        spec = CouiSpringData(bounce, response)
        // applyHandUpSpring overwrites the drag velocity when useNormalizedHandUpVelocity=true.
        velocity = initial
        target = 0f
    }

    fun advance(dt: Float): Boolean {
        if (dt <= 0f) return true
        return step(shift, velocity, target, spec, dt) { v, vel ->
            shift = v
            velocity = vel
        }
    }

    private companion object {
        val DragSpring = CouiSpringData(0.15f, 0.15f)
        val HandUpAreaEasing = CubicBezierEasing(0.55026454f, 0f, 0.8915344f, 0.18783069f)
    }
}

/** `TouchMotionRubberBand.apply`: raw pixels in, raw pixels out; 28dp is converted by the caller. */
internal fun rubberBand(delta: Float, maxDistance: Float, curveRatio: Float = 0.05f): Float {
    if (maxDistance <= 0f) return 0f
    val a = curveRatio * abs(delta) / maxDistance
    val magnitude = (1f - 1f / (1f + a)) * maxDistance
    return magnitude * if (delta < 0f) -1f else 1f
}

/** Per-control shader/brush/path, also used by the section seek bar. */
internal class CouiSeekBarLightRenderer(darkTheme: Boolean) {
    // Settings 17's `TYPE_OPAQUE_MEDIUM_1` preset; all lengths are raw pixels.
    private val spec = CouiSpotLightDefaults.OpaqueMedium1(darkTheme)
    private val endIntensity = spec.endIntensity
    private val shader = createCouiSpotLightShader()?.apply {
        setLightColor(spec.color.red, spec.color.green, spec.color.blue)
        setAmbientStrength(spec.ambientStrength)
        setDiffuseStrength(spec.diffuseStrength)
        setLightHeight(spec.lightHeight)
        setNoise(spec.noise)
        setDitherN(spec.dither)
    }
    private val clip = Path()

    fun draw(scope: DrawScope, motion: CouiSeekBarPressMotion, background: Rect): Unit = with(scope) {
        val light = shader ?: return
        if (motion.glowAlpha == 0f) return
        light.setLightPosition(motion.glowX, motion.glowY)
        light.setLightRadius(motion.lightRadius)
        light.setIntensity(endIntensity * motion.glowAlpha.coerceIn(0f, 1f))
        light.setResolution(size.width, size.height)
        // Android reuses its live RuntimeShader brush; Skiko snapshots the updated uniforms.
        val lightBrush = spotLightBrush(light)
        // SpotLightEffectRenderer clips with ordinary addRoundRect, radius=backgroundHeight/2.
        clip.reset()
        clip.addRoundRect(RoundRect(background, CornerRadius(background.height / 2f)))
        clipPath(clip) {
            drawRect(brush = lightBrush, topLeft = Offset.Zero, size = size, blendMode = BlendMode.Overlay)
        }
    }
}
