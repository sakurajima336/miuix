
package io.github.wfc35286.coui.kmp.component

import io.github.wfc35286.coui.kmp.motion.CouiSpringData

internal abstract class CouiPopupSpringProgressUnit {

    var value: Float = 0f
        protected set

    var spring: CouiSpringData = CouiSpringData(0f, 0.45f)
    private var target: Float = 0f
    private var velocity: Float = 0f

    protected abstract fun onProgressUpdate(progress: Float)

    fun currentProgressValue(): Float = value

    fun isRunning(): Boolean = animating

    private var animating = false

    fun startAnimation(bounce: Float, response: Float, targetValue: Float) {
        spring = CouiSpringData(bounce, response)
        target = targetValue
        animating = true
    }

    fun setStartValue(value: Float) {
        this.value = value
        onProgressUpdate(value / PROGRESS_SCALE)
    }

    fun cancel() {
        animating = false
    }

    open fun reset(keepValue: Boolean) {
        animating = false
        velocity = 0f
        if (!keepValue) value = 0f
    }

    fun skipToEnd() {
        if (!animating) return
        value = target
        velocity = 0f
        animating = false
        onProgressUpdate(value / PROGRESS_SCALE)
    }

    fun canSkipToEnd(): Boolean = animating

    /**
     * `SpringProgressUnit.canSkipToEnd()` / `skipToEnd()` (official) only fire when the spring is
     * already imperceptibly close to its target. Ours used to rely on the integrator's own rest
     * threshold, which lands the final value at the very end of the animation - i.e. on a frame
     * where nothing else moves any more, which is where the sub menu owner's row showed a clearly
     * visible 1px text rasterisation step. This exposes the same "close enough" test so the driver
     * can land exactly while the rest of the animation is still running.
     */
    fun nearTarget(epsilonFraction: Float): Boolean =
        animating && kotlin.math.abs(value - target) <= epsilonFraction * PROGRESS_SCALE

    fun advance(dtSeconds: Float): Boolean {
        if (!animating) return false
        animating = step(
            current = value,
            velocity = velocity,
            target = target,
            spec = spring,
            dt = dtSeconds.coerceAtLeast(0f),
            valueThreshold = VALUE_THRESHOLD,
            velocityThreshold = VALUE_THRESHOLD * 62.5f,
        ) { position, speed ->
            value = position
            velocity = speed
        }
        onProgressUpdate((value / PROGRESS_SCALE).coerceIn(0f, 1f))
        return animating
    }

    private companion object {
        const val PROGRESS_SCALE = 10000f

        const val VALUE_THRESHOLD = 0.75f
    }
}

internal class CouiPopupMenuTransitionUnit : CouiPopupSpringProgressUnit() {

    var currentScaleX = 1f
        private set
    var currentScaleY = 1f
        private set
    var currentTranslationY = 0f
        private set
    var currentContentAlpha = 1f
        private set

    private var targetScaleX = 1f
    private var targetScaleY = 1f
    private var targetTranslationY = 0f
    private var targetContentAlpha = 1f

    override fun onProgressUpdate(progress: Float) {
        currentScaleX = couiConvertedFraction(1f, targetScaleX, progress)
        currentScaleY = couiConvertedFraction(1f, targetScaleY, progress)
        currentTranslationY = couiConvertedFraction(0f, targetTranslationY, progress)
        currentContentAlpha = couiConvertedFraction(1f, targetContentAlpha, progress)
    }

    fun animateToSubMenuEnterState(
        scaleX: Float,
        scaleY: Float,
        translationY: Float,
        alpha: Float,
        bounce: Float,
        response: Float,
    ) {
        targetScaleX = scaleX
        targetScaleY = scaleY
        targetTranslationY = translationY
        targetContentAlpha = alpha
        startAnimation(bounce, response, PROGRESS_FULL)
    }

    fun animateToSubMenuExitState(bounce: Float, response: Float) {
        startAnimation(bounce, response, 0f)
    }

    override fun reset(keepValue: Boolean) {
        super.reset(keepValue)
        if (!keepValue) {
            currentScaleX = 1f; currentScaleY = 1f
            currentTranslationY = 0f; currentContentAlpha = 1f
        }
    }

    private companion object {
        const val PROGRESS_FULL = 10000f
    }
}

internal class CouiPopupSubMenuTransitionUnit : CouiPopupSpringProgressUnit() {

    var currentScaleX = 1f
        private set
    var currentScaleY = 1f
        private set
    var currentAlpha = 1f
        private set
    var currentTranslateY = 0f
        private set

    private var targetScaleStartX = 1f
    private var targetScaleStartY = 1f
    private var targetScaleEndX = 1f
    private var targetScaleEndY = 1f
    private var targetAlphaStart = 1f
    private var targetAlphaEnd = 1f
    private var targetTranslateYStart = 0f
    private var targetTranslateYEnd = 0f

    fun configForDefaultScreenEnter(scaleStart: Float, alphaStart: Float) {
        targetScaleStartX = scaleStart
        targetScaleStartY = scaleStart
        targetScaleEndX = 1f
        targetScaleEndY = 1f
        targetAlphaStart = alphaStart
        targetAlphaEnd = 1f
        targetTranslateYStart = 0f
        targetTranslateYEnd = 0f
    }

    fun configForSmallScreenEnter(
        yStart: Float,
        yEnd: Float,
        alphaStart: Float,
        alphaEnd: Float,
    ) {
        targetScaleStartX = 1f
        targetScaleStartY = 1f
        targetScaleEndX = 1f
        targetScaleEndY = 1f
        targetAlphaStart = alphaStart
        targetAlphaEnd = alphaEnd
        targetTranslateYStart = yStart
        targetTranslateYEnd = yEnd
        if (currentProgressValue() == 0f) {
            currentAlpha = alphaStart
            currentTranslateY = yStart
        }
    }

    fun updateTranslateYTargets(yStart: Float, yEnd: Float) {
        targetTranslateYStart = yStart
        targetTranslateYEnd = yEnd
    }

    fun animateToEnterState(bounce: Float, response: Float) {
        startAnimation(bounce, response, PROGRESS_FULL)
    }

    fun animateToExitState(bounce: Float, response: Float) {
        startAnimation(bounce, response, 0f)
    }

    override fun onProgressUpdate(progress: Float) {
        currentScaleX = couiConvertedFraction(targetScaleStartX, targetScaleEndX, progress)
        currentScaleY = couiConvertedFraction(targetScaleStartY, targetScaleEndY, progress)
        currentAlpha = couiConvertedFraction(targetAlphaStart, targetAlphaEnd, progress)
        currentTranslateY = couiConvertedFraction(targetTranslateYStart, targetTranslateYEnd, progress)
    }

    override fun reset(keepValue: Boolean) {
        super.reset(keepValue)
        if (!keepValue) {
            currentScaleX = 1f; currentScaleY = 1f
            currentAlpha = 1f; currentTranslateY = 0f
        }
    }

    private companion object {
        const val PROGRESS_FULL = 10000f
    }
}

internal fun couiConvertedFraction(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)