
package io.github.wfc35286.coui.kmp.component

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.lang.reflect.Method
import androidx.compose.ui.graphics.Outline as ComposeOutline

@Composable
internal actual fun CouiPopupRootAlpha(visual: CouiPopupVisualState) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view, visual) {
        val root = view
        val previous = root.alpha
        val sync = ViewTreeObserver.OnPreDrawListener {
            root.alpha = visual.globalAlpha
            true
        }
        root.viewTreeObserver.addOnPreDrawListener(sync)
        onDispose {
            if (root.viewTreeObserver.isAlive) root.viewTreeObserver.removeOnPreDrawListener(sync)
            root.alpha = previous
        }
    }
}

internal actual fun createCouiPopupShape(radius: Dp, useBackgroundBlur: Boolean): Shape = AndroidCouiPopupShape(CouiPopupPlatform.Current.cornerRadiusDp.dp, useBackgroundBlur)

private class AndroidCouiPopupShape(private val radius: Dp, private val useBlur: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): ComposeOutline {
        val path = android.graphics.Path()
        val r = with(density) { radius.toPx() }
        CouiPopupPlatform.Current.path(path, RectF(0f, 0f, size.width, size.height), r, CouiPopupPlatform.Current.cornerWeight, useBlur, with(density) { 24.dp.toPx() })
        return ComposeOutline.Generic(path.asComposePath())
    }
}

private fun addPopupRoundRect(path: android.graphics.Path, bounds: RectF, radius: Float, weight: Float = CouiPopupPlatform.Current.cornerWeight) {
    CouiPopupPlatform.Current.path(path, bounds, radius, weight)
}

@Composable
internal actual fun CouiPopupMaterial(
    dark: Boolean,
    useBackgroundBlur: Boolean,
    fallbackColor: Color,
    shape: Shape,
    visual: CouiPopupVisualState,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context -> CouiPopupMaterialView(context) },
        modifier = modifier,
        update = { it.configure(dark, useBackgroundBlur, fallbackColor.toArgb(), visual) },
        onRelease = { it.releaseBlur() },
    )
}

private class CouiPopupMaterialView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private var platform = CouiPopupPlatform.Current
    private val radius: Float get() = platform.cornerRadiusDp * density
    private val path = android.graphics.Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG)
    private var edgeShader: RuntimeShader? = null
    private var dark = false
    private var useBlur = false
    private var requestedBlur = false
    private var edgeEnabled = false
    private var lightEnabled = false
    private var edgeCornerType = -1
    private var fallback = android.graphics.Color.WHITE
    private var manager: Any? = null
    private var blurDrawable: Drawable? = null
    private var blurAttempted = false
    private var visual: CouiPopupVisualState? = null
    private var lightShader: AndroidCouiSpotLightShader? = null
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.OVERLAY }
    private var lastCorner = Float.NaN
    private var revealBounds: Rect? = null
    private var revealAlpha = 1f
    private var blurDrawFrames = 0
    private var choreographer: Choreographer? = null
    private var frameScheduled = false

    private val stateObserver = SnapshotStateObserver { command ->
        if (Looper.myLooper() == Looper.getMainLooper()) command() else post { command() }
    }
    private val onVisualChanged: (CouiPopupMaterialView) -> Unit = { it.scheduleFrame() }
    private val syncVisual = Choreographer.FrameCallback {
        frameScheduled = false
        if (isAttachedToWindow) {
            synchronizeFrame()
        }
    }

    private fun scheduleFrame() {
        if (!isAttachedToWindow || frameScheduled) return
        frameScheduled = true
        choreographer?.postFrameCallback(syncVisual)
    }



    private fun synchronizeFrame() {
        val state = visual ?: return
        val frame = Snapshot.global {
            var sample: CouiPopupFrame? = null
            stateObserver.observeReads(this, onVisualChanged) {
                sample = state.sampleFrame()
                state.touch.lightProgress
                state.touch.lightPosition
                state.touch.lightRadius
            }
            requireNotNull(sample).also { state.commitFrame(it) }
        }
        if (revealBounds != frame.revealBounds || revealAlpha != frame.outlineAlpha) {
            revealBounds = frame.revealBounds
            revealAlpha = frame.outlineAlpha
            rebuildPath()
            invalidateOutline()
        }
        pivotX = width / 2f
        pivotY = height / 2f
        translationX = frame.translation.x
        translationY = frame.translation.y
        scaleX = frame.scaleX
        scaleY = frame.scaleY
        state.useBackgroundBlur = useBlur
        // While the reveal slice has no height the panel paints nothing (empty outline clip and an
        // empty background bounds), but the view must stay VISIBLE and keep drawing: that is what
        // gives the framework blur drawable its warm-up frames. Marking it INVISIBLE instead meant
        // the first *revealed* frame was also the drawable's first frame, and the drawable emits
        // nothing yet - measured on device as the sub menu panel being fully transparent for two
        // frames (the page showed through where the panel was about to appear).
        alpha = frame.alpha
        visibility = VISIBLE
        val corner = radius * maxOf(frame.scaleX, frame.scaleY)
        manager?.let { root ->
            if (corner != lastCorner) {
                runCatching { invoke(root, "setCornerRadius", corner, corner, corner, corner) }
                    .onSuccess { lastCorner = corner }
            }
        }
        blurDrawable?.alpha = (alpha * 255f).toInt()
        edge.alpha = (alpha * 255f).toInt()
        invalidate()
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        setWillNotDraw(false)
        clipToOutline = true
        forceHasOverlappingRendering(false)
        isForceDarkAllowed = false
        alpha = 0f
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val bounds = revealBounds
                platform.outline(outline, width, height, radius, platform.cornerWeight, useBlur, 24f * density, bounds)
                outline.alpha = revealAlpha
            }
        }
        elevation = 30f * density
    }

    fun configure(dark: Boolean, useBlur: Boolean, fallback: Int, visual: CouiPopupVisualState) {
        val firstFrame = this.visual !== visual
        this.visual = visual
        val changed = this.dark != dark || this.requestedBlur != useBlur || this.fallback != fallback
        this.dark = dark
        this.requestedBlur = useBlur
        if (changed || !blurAttempted) this.useBlur = useBlur
        visual.useBackgroundBlur = this.useBlur
        this.fallback = fallback
        fill.color = fallback
        if (changed) {
            blurAttempted = false
            configureShadow()
        }
        if (isAttachedToWindow && (changed || !blurAttempted)) applyBlur()
        if (firstFrame) synchronizeFrame()
        scheduleFrame()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }
        choreographer = Choreographer.getInstance()
        stateObserver.start()
        scheduleFrame()
        configureShadow()
        // Official `RoundFrameLayout.onAttachedToWindow` (RoundFrameLayout.smali:1742-1820) attaches
        // the background blur synchronously inside the same callback, right after its
        // `isHardwareAccelerated()` guard, and never renders a solid panel first. Deferring it by a
        // frame (the previous `post { applyBlur() }`) made the first frame of the sub menu paint the
        // opaque `fill` fallback inside the anchor-row sized prototype outline - that is the
        // "colour block" that flashed for one frame where the official already shows blurred
        // material. Measured before the fix: CouiPopupFrames logged `fill=true` for the first
        // frames of a material; the official has no such frame.
        applyBlur()
        if (blurDrawable == null) {
            // Retry once when the window surface was not ready yet (a popup window that is still
            // being attached); the fallback then only covers that single frame.
            post { if (isAttachedToWindow && blurDrawable == null) applyBlur() }
        }
    }

    override fun onDetachedFromWindow() {
        choreographer?.removeFrameCallback(syncVisual)
        choreographer = null
        frameScheduled = false
        stateObserver.stop()
        stateObserver.clear()
        // Hide the RenderNode before clearing the blur surface or committed clip.
        // Both callbacks can run while Compose still retains its content layer.
        alpha = 0f
        visibility = INVISIBLE
        visual?.releaseFrame()
        releaseBlur()
        super.onDetachedFromWindow()
    }

    fun releaseBlur() {
        manager?.let { runCatching { invoke(it, "setBlurRadius", 0) } }
        background = null
        manager = null
        blurDrawable = null
        blurAttempted = false
        lastCorner = Float.NaN
    }

    private fun configureShadow() {
        elevation = 30f * density
        outlineAmbientShadowColor = android.graphics.Color.BLACK
        outlineSpotShadowColor = if (dark) 0x88AAAAAA.toInt() else 0x80000000.toInt()
        if (!platform.nativeShadow) return
        runCatching {
            val cls = Class.forName("com.oplus.view.OplusView")
            val nativeView = cls.getConstructor(View::class.java).newInstance(this)
            val color = android.graphics.Color.argb(if (dark) 150 else 48, 0, 0, 0)
            outlineAmbientShadowColor = color
            outlineSpotShadowColor = color
            elevation = (if (dark) 36f else 180f) * density
            invoke(nativeView, "setOverrideLightSourceGeometry", -1f, (if (dark) -1500f else -200f) * density, 10000f * density, (if (dark) 12000f else 2000f) * density, 0f)
        }.onFailure {
            elevation = 30f * density
            outlineAmbientShadowColor = android.graphics.Color.BLACK
            outlineSpotShadowColor = if (dark) 0x88AAAAAA.toInt() else 0x80000000.toInt()
        }
    }

    private fun applyBlur() {
        blurAttempted = true
        // (Re)installing the material means the platform drawable has to produce its first frame
        // again; until it does, the Compose base surface under this view keeps the panel from
        // showing the page through (see CouiPopupVisualState.materialLive).
        blurDrawFrames = 0
        visual?.materialLive = false
        useBlur = isHardwareAccelerated && requestedBlur
        visual?.useBackgroundBlur = useBlur
        edgeEnabled = requestedBlur && isHardwareAccelerated && Build.VERSION.SDK_INT >= 33
        lightEnabled = edgeEnabled
        configureEffects()
        rebuildPath()
        invalidateOutline()
        if (!useBlur || !platform.nativeShadow) {
            useBlur = false
            visual?.useBackgroundBlur = false
            configureEffects()
            rebuildPath()
            invalidateOutline()
            releaseBlur()
            blurAttempted = true
            invalidate()
            return
        }
        runCatching {
            val cls = Class.forName("com.oplus.view.ViewRootManager")
            val rootManager = manager ?: cls.getConstructor(View::class.java).newInstance(this)
            manager = rootManager
            val drawable = invoke(rootManager, "getBackgroundBlurDrawable") as? Drawable ?: error("Framework returned no blur drawable")
            val params = Class.forName("com.oplus.graphics.OplusBlurParam").getConstructor().newInstance()
            invoke(params, "setBlurType", 2)
            val hdr = Build.VERSION.SDK_INT >= 34 && runCatching { (display?.javaClass?.getMethod("getHdrSdrRatio")?.invoke(display) as? Float ?: 1f) > 1f }.getOrDefault(false)
            val blend = when {
                hdr && dark -> 0x990F0F0F.toInt()
                hdr -> 0xC8868686.toInt()
                dark -> 0x99262626.toInt()
                else -> 0xBFEEEEEE.toInt()
            }
            val mix = when {
                hdr && dark -> 0x50262626
                hdr -> 0xDC7A7A7A.toInt()
                dark -> 0x99262626.toInt()
                else -> 0x99CDCDCD.toInt()
            }
            invoke(
                params,
                "setMaterialParams",
                if (hdr) {
                    if (dark) 2 else 3
                } else {
                    4
                },
                colorFloats(blend),
                colorFloats(mix),
            )
            if (platform.smoothBlur) invoke(params, "setSmoothCornerWeight", platform.cornerWeight)
            if (platform.atLeast(38, 41)) invoke(params, "setSmoothCornerType", 1)
            invoke(rootManager, "setBlurParams", params)
            val corner = radius * maxOf(scaleX, scaleY)
            invoke(rootManager, "setCornerRadius", corner, corner, corner, corner)
            lastCorner = corner
            drawable.alpha = (alpha * 255f).toInt()
            invoke(rootManager, "setBlurRadius", CouiPopupListWindowDefaults.BlurRadiusPx)
            manager = rootManager
            blurDrawable = drawable
            // The Oplus blur drawable must stay the view's direct background: it registers itself
            // with the window blur system, and wrapping it in another drawable prevents it from
            // rendering. Keep this view as the only material owner of the panel surface.
            background = drawable
        }.onFailure {
            releaseBlur()
            useBlur = false
            visual?.useBackgroundBlur = false
            configureEffects()
            rebuildPath()
            invalidateOutline()
            blurAttempted = true
            Log.d("CouiPopupMaterial", "Opaque surface with portable effects: ${it.javaClass.simpleName}: ${it.message}")
        }
        invalidate()
    }

    private fun configureEffects() {
        val type = platform.cornerType(useBlur)
        if (edgeEnabled && (edgeShader == null || edgeCornerType != type)) {
            edgeShader = runCatching { RuntimeShader(popupShadowEdgeSource(type)) }.getOrElse {
                edgeEnabled = false
                Log.d("CouiPopupMaterial", "Edge shader unavailable: ${it.message}")
                null
            }
            edgeCornerType = type
        }
        if (lightEnabled && lightShader == null) {
            lightShader = runCatching {
                AndroidCouiSpotLightShader().apply {
                    setLightColor(1f, 1f, 1f)
                    setAmbientStrength(CouiPopupTouchDefaults.LightAmbient)
                    setDiffuseStrength(CouiPopupTouchDefaults.LightDiffuse)
                    setLightHeight(CouiPopupTouchDefaults.LightHeight)
                    setNoise(CouiPopupTouchDefaults.LightNoise)
                    setDitherN(CouiPopupTouchDefaults.LightDither)
                }
            }.getOrElse {
                lightEnabled = false
                null
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath()
        invalidateOutline()
        scheduleFrame()
    }
    private fun rebuildPath() {
        path.reset()
        val rect = revealBounds?.let { RectF(it.left, it.top, it.right, it.bottom) }
            ?: RectF(0f, 0f, width.toFloat(), height.toFloat())
        platform.path(path, rect, radius, platform.cornerWeight, useBlur, 24f * density)
        background?.bounds = android.graphics.Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
    }

    /**
     * Tells the panel's Compose base surface ([CouiPopupPanelBase]) that this view now paints real
     * material, so the base can hide itself and leave the platform material as the only surface.
     *
     * The framework blur drawable emits nothing for its first frames after [applyBlur] installs it
     * (measured on device: two frames at 102fps in which the sub menu panel was fully transparent
     * and the page showed through). It exposes no "first frame ready" callback, so this view's own
     * draw count is the signal. It is self-limiting: the base is the same material colour as the
     * platform panel, so one frame of overlap is invisible, while a missing one is the flash.
     */
    private fun markMaterialProgress() {
        val state = visual ?: return
        if (state.materialLive) return
        if (blurDrawable == null) {
            // No platform material here: the opaque fallback painted in onDraw IS the surface.
            if (!useBlur) state.materialLive = true
            return
        }
        blurDrawFrames++
        if (blurDrawFrames >= MATERIAL_LIVE_DRAWS) {
            state.materialLive = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // The official RoundFrameLayout paints only its `background`, which is the blur drawable
        // once `applyBlurBackground()` attaches it - before that the background is null and the
        // view paints NOTHING (no opaque fallback). Drawing a solid block here while the blur is
        // pending is what flashed for a frame when a sub menu opened (press -> solid slice ->
        // blur replaces it -> expansion), visible in a 60fps recording of the home page menus.
        // Keep the fill only for platforms where blur was requested but is genuinely unsupported.
        if (blurDrawable == null && !useBlur) canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        markMaterialProgress()
        if ((edgeEnabled || lightEnabled) && canvas.isHardwareAccelerated && width > 0 && height > 0) {
            val saved = canvas.save()
            if (edgeEnabled) {
                edgeShader?.let { shader ->
                    val w = width.toFloat()
                    val h = height.toFloat()
                    // Reverted: shaping the highlight on the reveal slice cut black notches into
                    // the panel edges, so the geometry stays on the full panel size.
                    shader.setFloatUniform("u_commonArray", floatArrayOf(w, h, 0f, 0f, w, h, radius, radius, radius, radius, platform.cornerWeight, 1f))
                    shader.setFloatUniform("u_shadow", 1f)
                    shader.setFloatUniform("u_edge", 1f)
                    shader.setFloatUniform("u_shadowArray", popupShadowUniform(w.coerceAtLeast(1f), h.coerceAtLeast(1f), density, dark))
                    shader.setFloatUniform("u_edgeArray", popupEdgeUniform(w.coerceAtLeast(1f), h.coerceAtLeast(1f), density, dark))
                    edge.shader = shader
                    canvas.drawRect(0f, 0f, w, h, edge)
                }
            }
            val motion = visual?.touch
            if (lightEnabled && motion != null && motion.lightProgress > 0f) {
                lightShader?.let { light ->
                    val position = motion.lightPosition
                    light.setLightPosition(position.x, position.y)
                    light.setLightRadius(motion.lightRadius)
                    light.setIntensity(CouiPopupTouchDefaults.lightEndIntensity(dark) * motion.lightProgress)
                    light.setResolution(width.toFloat(), height.toFloat())
                    lightPaint.shader = light.runtimeShader
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightPaint)
                }
            }
            canvas.restoreToCount(saved)
        }
    }

    private companion object {
        /**
         * Drawn frames with the blur drawable installed before the platform panel is trusted to be
         * opaque on its own. Measured on device: the drawable's first two frames emitted nothing.
         */
        const val MATERIAL_LIVE_DRAWS = 2
    }
}

private fun colorFloats(color: Int): FloatArray = floatArrayOf(
    android.graphics.Color.red(color) / 255f,
    android.graphics.Color.green(color) / 255f,
    android.graphics.Color.blue(color) / 255f,
    android.graphics.Color.alpha(color) / 255f,
)

private val methodCache = HashMap<String, Method>()

private fun invoke(target: Any, name: String, vararg args: Any): Any? {
    val key = name + "/" + args.size
    val cached = methodCache[key]
    val method: Method = if (cached != null && cached.declaringClass.isInstance(target)) {
        cached
    } else {
        val resolved = target.javaClass.methods.first { candidate ->
            candidate.name == name && candidate.parameterTypes.size == args.size &&
                candidate.parameterTypes.zip(args).all { (type, value) ->
                    when (type) {
                        Int::class.javaPrimitiveType -> value is Int
                        Float::class.javaPrimitiveType -> value is Float
                        Boolean::class.javaPrimitiveType -> value is Boolean
                        else -> type.isInstance(value)
                    }
                }
        }
        methodCache[key] = resolved
        resolved
    }
    return method.invoke(target, *args)
}

private fun popupShadowUniform(width: Float, height: Float, density: Float, dark: Boolean): FloatArray {
    val unit = maxOf(1f, density) * 0.34f
    return floatArrayOf(1f, 1f, 1f, 0.6f * if (dark) 0.1f else 0.9f, 0f, 0f, 86f * unit, 0f, 10f * unit, 1f + 600f * 0.1f * unit / width, 1f + 600f * 0.07f * unit / height, 4f, 0.7f, unit, 1f, 0f)
}

private fun popupEdgeUniform(width: Float, height: Float, density: Float, dark: Boolean): FloatArray {
    val lineWidth = (if (dark) 2f else 4f) * density
    val factor = lineWidth / 10f
    val unit = maxOf(1f, density) * 0.34f
    val actualW = maxOf(1f, width) / unit
    val actualH = maxOf(1f, height) / unit
    fun fade(start: Float, length: Float): Pair<Float, Float> {
        val referencePerimeter = 1600f
        val actualPerimeter = (actualW + actualH) * 2f
        val end = (start + length) * referencePerimeter
        return if (end > 600f) {
            (actualW + end - 600f - length * referencePerimeter) / actualPerimeter to length * referencePerimeter / actualPerimeter
        } else {
            val available = (actualW - (600f - end)) / actualPerimeter
            start * available / (start + length) to length * available / (start + length)
        }
    }
    val near = fade(0.18f, 0.35f)
    val far = fade(0.25f, 0.25f)
    val nearStart = maxOf(2f, 2f * factor)
    val farStart = maxOf(2f, 2f * factor)
    val alpha = if (dark) 0.4f else 0.9f
    return floatArrayOf(1f, 1f, 1f, nearStart, nearStart + maxOf(2f, 8f * factor), farStart, farStart + maxOf(2f, 2f * factor), 0.7f * alpha, 0.6f * alpha, near.first, near.first + near.second, far.first, far.first + far.second, 0f, 4f, 0.8f)
}

internal actual fun DrawScope.drawCouiPopupMask(bounds: Rect, color: Color, radius: Float, weight: Float) {
    val path = android.graphics.Path()
    addPopupRoundRect(path, RectF(bounds.left, bounds.top, bounds.right, bounds.bottom), radius, weight)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
    }
}

@Composable
internal actual fun couiPopupTouchExplorationEnabled(): Boolean = (LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true
