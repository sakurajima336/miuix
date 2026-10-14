
package io.wfc35286.coui.kmp.component

import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF

internal class CouiPopupPlatform(
    val sdk: Int,
    val subSdk: Int,
    val animationLevel: Int,
    val smoothStyle: Int,
) {
    val lowAnimation: Boolean get() = animationLevel !in 1..2
    val cornerRadiusDp: Float get() = CouiPopupListWindowDefaults.CornerRadius.value
    val cornerWeight: Float get() = CouiPopupListWindowDefaults.CornerWeight
    val nativeShadow: Boolean get() = atLeast(34, 2)
    val smoothBlur: Boolean get() = atLeast(34, 10)

    fun atLeast(version: Int, subVersion: Int): Boolean = sdk > version || (sdk == version && subSdk >= subVersion)

    private class PathApi(
        val cls: Class<*>,
        val ctor: java.lang.reflect.Constructor<*>,
        val addSmooth: java.lang.reflect.Method,
        val outlineCls: Class<*>,
        val outlineCtor: java.lang.reflect.Constructor<*>,
        val setSmooth: java.lang.reflect.Method,
    )

    private var legacyApi: PathApi? = null
    private var legacyResolved = false
    private var modernApi: PathApi? = null
    private var modernResolved = false

    /**
     * Resolving com.oplus.graphics.* by reflection used to happen on every frame (Class.forName +
     * getConstructor + getMethod, twice per frame: once in cornerType() and once in path()).
     * gfxinfo showed p99 = 150ms with 41.9% janky frames against the official's 38ms / 17.1%.
     * The handles never change for a given platform, so they are resolved once and cached.
     */
    private fun legacyPathApi(): PathApi? {
        if (!legacyResolved) {
            legacyResolved = true
            legacyApi = runCatching {
                val cls = Class.forName("com.oplus.graphics.OplusPath")
                val outlineCls = Class.forName("com.oplus.graphics.OplusOutline")
                PathApi(
                    cls,
                    cls.getConstructor(Path::class.java),
                    cls.getMethod("addSmoothRoundRect", RectF::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Path.Direction::class.java),
                    outlineCls,
                    outlineCls.getConstructor(Outline::class.java),
                    outlineCls.getMethod("setSmoothRoundRect", Rect::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType),
                )
            }.getOrNull()
        }
        return legacyApi
    }

    private fun modernPathApi(): PathApi? {
        if (!modernResolved) {
            modernResolved = true
            modernApi = runCatching {
                val cls = Class.forName("com.oplus.graphics.OplusPathAdapter")
                val outlineCls = Class.forName("com.oplus.graphics.OplusOutlineAdapter")
                val addSmooth = if (sdk > 37) {
                    cls.getMethod("addSmoothRoundRect", RectF::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Path.Direction::class.java)
                } else {
                    cls.getMethod("addSmoothRoundRect", RectF::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Path.Direction::class.java)
                }
                val setSmooth = if (sdk > 37) {
                    outlineCls.getMethod("setSmoothRoundRect", Rect::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                } else {
                    outlineCls.getMethod("setSmoothRoundRect", Rect::class.java, Float::class.javaPrimitiveType)
                }
                PathApi(cls, cls.getConstructor(Path::class.java, Int::class.javaPrimitiveType), addSmooth, outlineCls, outlineCls.getConstructor(Outline::class.java, Int::class.javaPrimitiveType), setSmooth)
            }.getOrNull()
        }
        return modernApi
    }

    fun cornerType(useBlur: Boolean): Int {
        if (lowAnimation || sdk < 34 || (useBlur && !smoothBlur)) return 0
        val legacy = smoothStyle == 0 || (useBlur && sdk == 37)
        return if (legacy) {
            if (legacyPathApi() != null) 3 else 0
        } else if (smoothStyle == 1) {
            if (modernPathApi() != null) 2 else 0
        } else {
            0
        }
    }

    fun path(path: Path, bounds: RectF, radius: Float, weight: Float, useBlur: Boolean = false, baseRadius: Float = radius) {
        val legacy = (smoothStyle == 0 && weight > 0f) || (useBlur && sdk == 37)
        val api = if (legacy) legacyPathApi() else if (smoothStyle == 1) modernPathApi() else null
        val success = api != null && runCatching {
            if (legacy) {
                val adapter = api.ctor.newInstance(path)
                api.addSmooth.invoke(adapter, bounds, radius, weight, Path.Direction.CW)
            } else {
                val adapter = api.ctor.newInstance(path, 1)
                if (sdk > 37) {
                    api.addSmooth.invoke(adapter, bounds, radius, radius, weight, Path.Direction.CCW)
                } else {
                    api.addSmooth.invoke(adapter, bounds, if (useBlur) radius else baseRadius, if (useBlur) radius else baseRadius, Path.Direction.CCW)
                }
            }
            true
        }.getOrDefault(false)
        if (!success) path.addRoundRect(bounds, radius, radius, Path.Direction.CW)
    }

    fun outline(outline: Outline, width: Int, height: Int, radius: Float, weight: Float, useBlur: Boolean, baseRadius: Float, revealBounds: androidx.compose.ui.geometry.Rect? = null) {
        val bounds = revealBounds?.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
            ?: Rect(0, 0, width, height)
        outline.setRoundRect(bounds, radius)
        val legacy = (smoothStyle == 0 && weight > 0f) || (useBlur && sdk == 37)
        val api = if (legacy) legacyPathApi() else if (smoothStyle == 1) modernPathApi() else null
        if (api == null) return
        runCatching {
            if (legacy) {
                val adapter = api.outlineCtor.newInstance(outline)
                api.setSmooth.invoke(adapter, bounds, radius, weight)
            } else {
                val adapter = api.outlineCtor.newInstance(outline, 1)
                if (sdk > 37) {
                    api.setSmooth.invoke(adapter, bounds, radius, weight)
                } else {
                    api.setSmooth.invoke(adapter, bounds, if (useBlur) radius else baseRadius)
                }
            }
        }
    }

    companion object {
        val Current: CouiPopupPlatform by lazy {
            val version = runCatching { Class.forName("com.oplus.os.OplusBuild\$VERSION") }.getOrNull()
            val sdk = runCatching { version?.getField("SDK_VERSION")?.getInt(null) ?: 0 }.getOrDefault(0)
            val sub = runCatching { version?.getField("SDK_SUB_VERSION")?.getInt(null) ?: 0 }.getOrDefault(0)
            val anim = propertyInt("persist.sys.oplus.anim_level", -1)
            val smooth = sdk >= 34 && (propertyInt("persist.sys.oplus.anim_level", 3) < 3 || propertyInt("persist.sys.oplus.upgrade_anim_level", 3) < 3) && propertyInt("persist.sys.oplus.default_smooth_weight", 170) != 200
            val style = when {
                !smooth -> -1
                sdk > 37 || (sdk == 37 && sub >= 1) -> 1
                sdk > 33 || (sdk == 33 && sub >= 34) -> 0
                else -> -1
            }
            CouiPopupPlatform(sdk, sub, anim, style)
        }
    }
}

private fun propertyInt(name: String, default: Int): Int = runCatching {
    (Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as? String)?.trim()?.toIntOrNull() ?: default
}.getOrDefault(default)
