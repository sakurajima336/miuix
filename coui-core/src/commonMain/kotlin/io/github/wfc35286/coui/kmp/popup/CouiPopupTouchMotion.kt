// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.wfc35286.coui.kmp.basic.rubberBand
import io.github.wfc35286.coui.kmp.basic.step
import io.github.wfc35286.coui.kmp.motion.CouiSpringData
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

internal object CouiPopupTouchDefaults {
    val SpotLightType = "TYPE_TRANSLUCENT_LARGE_1"
    val LightStartRadius = 100f
    val LightEndRadius = 800f
    val LightHeight = 200f
    val LightAmbient = 0.4f
    val LightDiffuse = 0.4f
    val LightNoise = 0.08f
    val LightDither = 0f
    val DeadZonePx = 5f
    val MaxStretchDp = 28f
    val RubberCurve = 0.05f
    val MaxDeformDistanceDp = 150f
    val DeformRubberCurve = 0.55f
    val DragSpring = CouiSpringData(0.15f, 0.15f)
    val FollowLightSpring = CouiSpringData(0.15f, 0.35f)
    val GlowSpring = CouiSpringData(0f, 0.15f)
    val ExtinguishSpring = CouiSpringData(0f, 0.4f)
    val HandUpAreaEasing = CubicBezierEasing(0.55026454f, 0f, 0.8915344f, 0.18783069f)

    fun lightEndIntensity(dark: Boolean): Float = if (dark) 0.1f else 0.6f

    fun maxScale(width: Float, height: Float): Float {
        val area = (width * height).toInt()
        val small = min(width, height)
        if (area <= 38416 || small == 0f) return 1.2f
        val ratio = max(width, height) / small
        val attenuation = min(0.35 * ln(area.toDouble() / 38416.0) + 0.015 * (ratio - 1f), 1.0)
        return max(1.03f, 1.03f + (1.2f - 1.03f) * (1.0 - attenuation).toFloat())
    }

    fun deformation(delta: Offset, maximum: Float, density: Float): Offset {
        val x = abs(delta.x)
        val y = abs(delta.y)
        if (x + y == 0f) return Offset(1f, 1f)
        val distance = MaxDeformDistanceDp * density
        val sx = 1f + rubberBand(x, distance, DeformRubberCurve) / distance * (maximum - 1f)
        val sy = 1f + rubberBand(y, distance, DeformRubberCurve) / distance * (maximum - 1f)
        if (y == 0f) return Offset(sx, 1f / sx)
        if (x == 0f) return Offset(1f / sy, sy)
        val wx = x / (x + y)
        val wy = y / (x + y)
        return Offset(
            exp((ln(sx.toDouble()).toFloat() * wx + ln((1f / sy).toDouble()).toFloat() * wy).toDouble()).toFloat(),
            exp((ln((1f / sx).toDouble()).toFloat() * wx + ln(sy.toDouble()).toFloat() * wy).toDouble()).toFloat(),
        )
    }
}

internal class PopupSpringAxis(initial: Float, private val minimumVisibleChange: Float) {
    var value by mutableFloatStateOf(initial)
    var velocity = 0f
    var target = initial
    var spring = CouiPopupTouchDefaults.DragSpring

    fun advance(dt: Float): Boolean = step(
        value,
        velocity,
        target,
        spring,
        dt,
        valueThreshold = minimumVisibleChange * 0.75f,
        velocityThreshold = minimumVisibleChange * 0.75f * 62.5f,
    ) { position, speed ->
        value = position
        velocity = speed
    }

    fun snap(position: Float) {
        value = position
        target = position
        velocity = 0f
    }
}

@Stable
internal class CouiPopupTouchMotion {
    private val stretchX = PopupSpringAxis(1f, 0.0005f)
    private val stretchY = PopupSpringAxis(1f, 0.0005f)
    private val rubberX = PopupSpringAxis(0f, 0.5f)
    private val rubberY = PopupSpringAxis(0f, 0.5f)
    private val glow = PopupSpringAxis(0f, 0.0001f)
    private val lightX = PopupSpringAxis(0f, 0.0001f)
    private val lightY = PopupSpringAxis(0f, 0.0001f)
    private var density = 1f
    private var width = 1f
    private var height = 1f
    private var maximumScale = 1.2f
    private var deformStart = Offset.Zero
    private var rubberStart = Offset.Zero
    var pressed = false
        private set
    var requestFrame: () -> Unit = {}
    val scaleX: Float get() = stretchX.value
    val scaleY: Float get() = stretchY.value
    val translationX: Float get() = rubberX.value
    val translationY: Float get() = rubberY.value
    val lightProgress: Float get() = glow.value.coerceIn(0f, 1f)
    val lightPosition: Offset get() = Offset(lightX.value * width, lightY.value * height)
    val lightRadius: Float
        get() {
            val p = lightProgress
            if (p == 0f) return 0f
            val r = CouiPopupTouchDefaults.LightStartRadius + (CouiPopupTouchDefaults.LightEndRadius - CouiPopupTouchDefaults.LightStartRadius) * p
            return if (pressed) r else r + 2f * CouiPopupTouchDefaults.LightEndRadius * (1f - p)
        }

    fun size(width: Int, height: Int, density: Float) {
        this.width = width.toFloat().coerceAtLeast(1f)
        this.height = height.toFloat().coerceAtLeast(1f)
        this.density = density
        maximumScale = CouiPopupTouchDefaults.maxScale(this.width, this.height)
    }

    fun down(point: Offset) {
        pressed = true
        val distance = (CouiPopupTouchDefaults.MaxStretchDp * density).roundToInt().toFloat()
        rubberStart = point - Offset(inverseRubber(rubberX.value, distance, CouiPopupTouchDefaults.RubberCurve), inverseRubber(rubberY.value, distance, CouiPopupTouchDefaults.RubberCurve))
        deformStart = point - Offset(effectiveDeform(scaleX), effectiveDeform(scaleY))
        listOf(stretchX, stretchY, rubberX, rubberY).forEach { axis ->
            axis.velocity = 0f
            axis.target = axis.value
            axis.spring = CouiPopupTouchDefaults.DragSpring
        }
        lightPosition(point)
        glow.target = 1f
        glow.spring = CouiPopupTouchDefaults.GlowSpring
        requestFrame()
    }

    fun move(point: Offset) {
        if (!pressed) return
        lightPosition(point)
        val delta = point - deformStart
        if (abs(delta.x) >= CouiPopupTouchDefaults.DeadZonePx || abs(delta.y) >= CouiPopupTouchDefaults.DeadZonePx) {
            val scales = CouiPopupTouchDefaults.deformation(delta, maximumScale, density)
            stretchX.target = scales.x
            stretchY.target = scales.y
        }
        val rubberDelta = point - rubberStart
        if (abs(rubberDelta.x) >= CouiPopupTouchDefaults.DeadZonePx || abs(rubberDelta.y) >= CouiPopupTouchDefaults.DeadZonePx) {
            val distance = (CouiPopupTouchDefaults.MaxStretchDp * density).roundToInt().toFloat()
            rubberX.target = rubberBand(rubberDelta.x, distance, CouiPopupTouchDefaults.RubberCurve)
            rubberY.target = rubberBand(rubberDelta.y, distance, CouiPopupTouchDefaults.RubberCurve)
        }
        requestFrame()
    }

    fun up(velocityPxPerSecond: Offset = Offset.Zero) {
        if (!pressed) return
        pressed = false
        val area = width * height / (density * density)
        val areaProgress = CouiPopupTouchDefaults.HandUpAreaEasing.transform(((area - 56f * 56f) / (100f * 100f - 56f * 56f)).coerceIn(0f, 1f))
        val response = 0.42f + (0.45f - 0.42f) * areaProgress
        val total = max(1f, CouiPopupTouchDefaults.MaxStretchDp * density)
        fun release(axis: PopupSpringAxis, rest: Float, speed: Float) {
            val initial = (speed * 0.0003f / total * abs(axis.value - rest)).coerceIn(-1000f, 1000f)
            axis.velocity = initial
            axis.spring = CouiSpringData(max(0.65f, abs(initial) / 1000f).coerceAtMost(1f), response)
            axis.target = rest
        }
        release(stretchX, 1f, velocityPxPerSecond.x)
        release(stretchY, 1f, velocityPxPerSecond.y)
        release(rubberX, 0f, velocityPxPerSecond.x)
        release(rubberY, 0f, velocityPxPerSecond.y)
        glow.target = 0f
        glow.spring = CouiPopupTouchDefaults.ExtinguishSpring
        requestFrame()
    }

    fun resetTouchOnly() {
        up()
        clearLight()
    }

    fun clearLight() {
        glow.snap(0f)
        requestFrame()
    }

    private fun lightPosition(point: Offset) {
        lightX.target = point.x / width
        lightY.target = point.y / height
        lightX.spring = CouiPopupTouchDefaults.FollowLightSpring
        lightY.spring = CouiPopupTouchDefaults.FollowLightSpring
        if (glow.value == 0f) {
            lightX.snap(lightX.target)
            lightY.snap(lightY.target)
        }
    }

    fun advance(dt: Float): Boolean {
        var moving = false
        for (axis in listOf(stretchX, stretchY, rubberX, rubberY, glow, lightX, lightY)) {
            moving = axis.advance(dt) || moving
        }
        if (glow.value == 0f) {
            lightX.snap(lightX.target)
            lightY.snap(lightY.target)
        }
        return moving
    }

    private fun effectiveDeform(scale: Float): Float {
        if (scale == 1f || maximumScale == 1f) return 0f
        val distance = CouiPopupTouchDefaults.MaxDeformDistanceDp * density
        val effective = min((scale - 1f) / (maximumScale - 1f), 1f) * distance
        return inverseRubber(effective, distance, CouiPopupTouchDefaults.DeformRubberCurve)
    }

    private fun inverseRubber(value: Float, distance: Float, ratio: Float): Float {
        val a = abs(value)
        if (distance <= 0f) return value
        if (a >= distance) return sign(value) * distance
        return value.sign * a * distance / (ratio * (distance - a))
    }
}

@Immutable
internal data class CouiPopupFrame(
    val translation: Offset,
    val scaleX: Float,
    val scaleY: Float,
    val alpha: Float,
    val revealBounds: Rect? = null,
    val outlineAlpha: Float = 1f,
    val contentAlpha: Float = alpha,
)

internal data class CouiPopupPanelGeometry(
    val scale: Float,
    val alpha: Float = 1f,
    val contentAlpha: Float = 1f,
    val pivot: Offset = Offset(0.5f, 0.5f),
    val translation: Offset = Offset.Zero,
    val reveal: Rect? = null,
    val outlineAlpha: Float = 1f,
)

internal class CouiPopupVisualState(
    private val path: () -> Float,
    private val scale: () -> Float,
    private val opacity: () -> Float,
    private val origin: () -> Offset,
    val touch: CouiPopupTouchMotion,
) {
    var useBackgroundBlur: Boolean by mutableStateOf(true)

    /**
     * Whether the platform material of this panel has actually produced pixels yet.
     *
     * The Android panel surface only starts emitting its blur material a frame or two after the
     * background drawable is installed, and during that window it paints nothing, so the page
     * behind the popup shows straight through where the panel is about to appear. The panel
     * therefore keeps an opaque material-coloured fallback surface under the blur until this
     * flips true; [CouiPopupPanelBase] hides that fallback once the real material is live, so
     * the settled panel shows only the platform material.
     */
    var materialLive: Boolean by mutableStateOf(false)
    var panelScale: Float by mutableFloatStateOf(1f)
    var panelAlpha: Float by mutableFloatStateOf(1f)

    var panelContentAlpha: Float by mutableFloatStateOf(1f)
    var outlineAlpha: Float by mutableFloatStateOf(1f)
    var pivot: Offset by mutableStateOf(Offset(0.5f, 0.5f))
    var panelSize: Offset by mutableStateOf(Offset.Zero)
    var minimumTranslationY: Float by mutableFloatStateOf(Float.NEGATIVE_INFINITY)
    var parentInfluence: CouiPopupVisualState? = null

    var geometry: (() -> CouiPopupPanelGeometry)? = null
    var inheritedPivot: Offset? by mutableStateOf(null)
    var panelTranslation: Offset by mutableStateOf(Offset.Zero)
    var revealBounds: Rect? by mutableStateOf(null)
    private var committedFrame: CouiPopupFrame? by mutableStateOf(null)

    val globalAlpha: Float get() = opacity().coerceIn(0f, 1f)
    val contentAlpha: Float get() = sourceAlpha() * currentGeometry().contentAlpha
    val globalScaleX: Float get() = scale().coerceAtLeast(0f) * touch.scaleX
    val globalScaleY: Float get() = scale().coerceAtLeast(0f) * touch.scaleY
    val globalTranslation: Offset
        get() {
            // Paired with sourceTranslation's centerCorrection: during the window open/close
            // the two cancel, so the panel grows out of the anchor point instead of being
            // pushed off-screen. Removing either half moves the start position.
            val wave = scale().coerceAtLeast(0f)
            val correction = Offset(
                panelSize.x * (0.5f - currentGeometry().pivot.x) * (1f - wave),
                panelSize.y * (0.5f - currentGeometry().pivot.y) * (1f - wave),
            )
            return couiPopupBezierOffset(origin(), path()) + Offset(touch.translationX, touch.translationY) + correction
        }

    // The background bounds, the outline and the content must be sampled from the same live
    // geometry on the same frame: reading committedFrame here made the content one frame behind
    // the material, so the panel and its rows were not one piece.
    val translation: Offset get() = sourceTranslation()
    val scaleX: Float get() = sourceScaleX()
    val scaleY: Float get() = sourceScaleY()
    val alpha: Float get() = sourceAlpha()
    val contentReveal: Rect? get() = currentGeometry().reveal

    fun sampleFrame(): CouiPopupFrame {
        val local = currentGeometry()
        val sx = globalScaleX * (parentInfluence?.globalScaleX ?: 1f) * local.scale
        val sy = globalScaleY * (parentInfluence?.globalScaleY ?: 1f) * local.scale
        val a = globalAlpha * local.alpha
        return CouiPopupFrame(
            sourceTranslation(local, sx, sy),
            sx,
            sy,
            a,
            local.reveal,
            local.outlineAlpha,
            a * local.contentAlpha,
        )
    }

    fun commitFrame(frame: CouiPopupFrame) {
        committedFrame = frame
    }

    fun releaseFrame() {
        committedFrame = null
    }

    private fun currentGeometry(): CouiPopupPanelGeometry = geometry?.invoke() ?: CouiPopupPanelGeometry(
        panelScale,
        panelAlpha,
        panelContentAlpha,
        pivot,
        panelTranslation,
        revealBounds,
        outlineAlpha,
    )

    private fun sourceTranslation(): Offset = sourceTranslation(currentGeometry(), sourceScaleX(), sourceScaleY())

    private fun sourceTranslation(local: CouiPopupPanelGeometry, sx: Float, sy: Float): Offset {
        val parent = parentInfluence
        val global = globalTranslation + (parent?.globalTranslation ?: Offset.Zero)
        val actualPivot = inheritedPivot ?: local.pivot
        val centerCorrection = Offset(
            // Convert a platform-style View pivot to our center-based node. With a pivotY of 0
            // the panel keeps its top edge while shrinking, so its center moves up; this sign
            // reproduces that for our center-based node.
            panelSize.x * (actualPivot.x - 0.5f) * (1f - sx),
            panelSize.y * (actualPivot.y - 0.5f) * (1f - sy),
        )
        val result = global + local.translation + centerCorrection
        return Offset(result.x, max(minimumTranslationY, result.y))
    }
    private fun sourceScaleX(): Float = globalScaleX * (parentInfluence?.globalScaleX ?: 1f) * currentGeometry().scale
    private fun sourceScaleY(): Float = globalScaleY * (parentInfluence?.globalScaleY ?: 1f) * currentGeometry().scale
    private fun sourceAlpha(): Float = globalAlpha * currentGeometry().alpha
}
