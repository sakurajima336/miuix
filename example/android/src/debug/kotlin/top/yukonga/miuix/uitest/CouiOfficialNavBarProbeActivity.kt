// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity


/**
 * Debug-only: instantiates the **real** `COUIBottomFloatingNavigationBar` out of the installed
 * Settings APK and puts it on screen next to nothing but a banner, so its rim / fog / blur can be
 * measured directly.
 *
 * `createPackageContext(..., CONTEXT_INCLUDE_CODE)` loads Settings' dex into our process; the class
 * then resolves its own `com.support.bottomnavigation.R` through that context. This is a probe-only
 * trick - shipping code must never do it.
 *
 * The view is given a fixed 1167x213 px slot so its geometry matches the Compose probe exactly.
 */
class CouiOfficialNavBarProbeActivity : ComponentActivity() {
    private val tag = "OfficialNavBar"
    private val sb = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A page of hard-edged colour bands is the background the material has to sample. The
        // capsule was previously parked on a flat grey #303030 swatch (a measuring aid so the pixel
        // script could find its bounds) - which erased exactly the content the blur needs, so no
        // fog could ever appear. The slot stays transparent and the bands run under the capsule.
        val root = FrameLayout(this)
        setContentView(root)
        root.addView(
            object : View(this) {
                private val bands =
                    intArrayOf(
                        Color.rgb(0xE5, 0x39, 0x35), // red
                        Color.rgb(0x1E, 0x88, 0xE5), // blue
                        Color.rgb(0xFF, 0xFF, 0xFF), // white
                        Color.rgb(0x43, 0xA0, 0x47), // green
                        Color.rgb(0xFF, 0xC1, 0x07), // amber
                        Color.rgb(0x21, 0x21, 0x21), // near-black
                    )

                override fun onDraw(canvas: android.graphics.Canvas) {
                    val bandH = height.toFloat() / (bands.size * 2)
                    for (pass in 0..1) {
                        for (i in bands.indices) {
                            val p = android.graphics.Paint().apply { color = bands[i] }
                            canvas.drawRect(
                                0f,
                                (pass * bands.size + i) * bandH,
                                width.toFloat(),
                                (pass * bands.size + i + 1) * bandH,
                                p,
                            )
                        }
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        val banner =
            TextView(this).apply {
                text = "official bar (material forced on), over colour bands"
                setTextColor(Color.WHITE)
                textSize = 12f
                setBackgroundColor(Color.rgb(0x00, 0x00, 0x00))
            }
        root.addView(banner)

        val slot = FrameLayout(this) // transparent: the colour bands must show through
        root.addView(
            slot,
            FrameLayout.LayoutParams(1167, 213).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 120
            },
        )

        val ok = tryAttachOfficial(slot)
        if (!ok) {
            banner.text = "FAILED - see files/official.txt"
        }
        // Logcat's ring buffer loses the early lines on this device, so the verdict is written
        // to a file as well.
        runCatching {
            val dir = getExternalFilesDir(null) ?: filesDir
            java.io.File(dir, "official.txt").writeText(sb.toString())
        }
        // `screencap` keeps catching the assistant's floating window instead of this one, so the
        // bitmap is taken from our own window. A plain screencap is unusable for measurement here.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ capture() }, 2200)
    }

    private fun tryAttachOfficial(slot: ViewGroup): Boolean =
        try {
            val ctx =
                createPackageContext(
                    "com.android.settings",
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE,
                )
            val prefix = "com.coui.appcompat.bottomfloatingnavigationbar"
            val barCls = Class.forName("$prefix.COUIBottomFloatingNavigationBar", true, ctx.classLoader)
            note("class loaded: ${barCls.name}")

            // Report the constructor surface so a mismatch is visible rather than guessed.
            note(
                "ctors: " + barCls.declaredConstructors.joinToString { c ->
                    "(" + c.parameterTypes.joinToString { it.simpleName } + ")"
                },
            )
            note("methods: " + barCls.declaredMethods.joinToString(", ") { it.name })

            // The official view resolves COUI theme attributes (`coui_height` &c.) while building
            // its red dot - `COUIHintRedDotHelper.<init>` does `TypedArray.getDimensionPixelSize`
            // and throws `UnsupportedOperationException: Failed to resolve attribute` under a plain
            // DeviceDefault theme. So instantiate it against Settings' own `Theme.COUI.Main`.
            val themeId = ctx.resources.getIdentifier("Theme.COUI.Main", "style", "com.android.settings")
            note("Theme.COUI.Main id = 0x${themeId.toString(16)}")
            val themed = android.view.ContextThemeWrapper(ctx, themeId)

            val ctor = barCls.getConstructor(Context::class.java)
            val bar = ctor.newInstance(themed) as View
            note("instantiated: $bar")

            // `addItems(List<? extends BaseBottomFloatingItem>)` - build the items with the
            // official Builder so the view gets real `COUIBottomFloatingNavigationBarItem`s.
            val builderCls =
                Class.forName(
                    "$prefix.COUIBottomFloatingNavigationBarItem\$Builder",
                    true,
                    ctx.classLoader,
                )
            val setId = builderCls.getMethod("setId", Int::class.javaPrimitiveType)
            val setTitle = builderCls.getMethod("setTitle", String::class.java)
            val setIcon = builderCls.getMethod("setIcon", android.graphics.drawable.Drawable::class.java)
            val build = builderCls.getMethod("build")
            @Suppress("DEPRECATION")
            val icon = resources.getDrawable(android.R.drawable.ic_menu_camera, null)
            val titles = listOf("首页", "游戏", "软件", "榜单", "我的")
            val items = java.util.ArrayList<Any>()
            titles.forEachIndexed { i, t ->
                val b = builderCls.getConstructor().newInstance()
                setId.invoke(b, i)
                setTitle.invoke(b, t)
                if (icon != null) setIcon.invoke(b, icon)
                items.add(build.invoke(b))
            }
            note("items built: ${items.size}")
            runCatching {
                barCls.getMethod("addItems", java.util.List::class.java).invoke(bar, items)
                note("addItems done")
            }.onFailure { t ->
                note("addItems FAILED: ${t.javaClass.simpleName}: ${t.message}")
                var c: Throwable? = t
                var depth = 0
                while (c != null && depth < 6) {
                    note("   cause[$depth] ${c.javaClass.name}: ${c.message}")
                    c.stackTrace.take(6).forEach { note("        at $it") }
                    c = c.cause
                    depth++
                }
            }
            runCatching {
                barCls.getMethod("setIsDrawableMaterialEffect", Boolean::class.javaPrimitiveType)
                    .invoke(bar, true)
                note("setIsDrawableMaterialEffect(true)")
            }.onFailure { note("setIsDrawableMaterialEffect failed: ${it.message}") }

            // `isStrokeEffectEnabled()` / `isBlurEffectEnabled()` / `isCausticShadowEffectEnabled()`
            // all read the same field:
            //     if (mUseMaterialEffectAttr == FORCE_ENABLE) return true
            //     if (mUseMaterialEffectAttr == FORCE_DISABLE) return false
            //     return COUIMaterialStateManager.isXxxEnable()     <- false for a 3rd-party app
            // Default is FOLLOW_SYSTEM (value 0), so without this the whole material is skipped
            // and the bar renders as the flat fallback panel only.
            runCatching {
                val stateCls =
                    Class.forName(
                        "$prefix.BaseBottomFloatingMenuView\$MaterialEffectState",
                        true,
                        ctx.classLoader,
                    )
                val forceEnable = stateCls.getField("FORCE_ENABLE").get(null)
                note("FORCE_ENABLE = $forceEnable")
                barCls.getMethod("setUseMaterialEffectAttr", stateCls).invoke(bar, forceEnable)
                note("setUseMaterialEffectAttr(FORCE_ENABLE)")
            }.onFailure { note("setUseMaterialEffectAttr failed: ${it.message}") }

            // `COUIBottomFloatingNavigationBar` extends `ViewGroup` and is only a shell - the
            // material lives on the inner `COUIBottomFloatingNavigationBarMenuView`, which is the
            // real `BaseBottomFloatingMenuView` subclass. Writing `mUseMaterialEffectAttr` on the
            // shell silently fails, which is why the bar stayed a flat panel.
            val menuCls =
                Class.forName(
                    "$prefix.COUIBottomFloatingNavigationBarMenuView",
                    true,
                    ctx.classLoader,
                )
            var menu: View? = null
            for (i in 0 until (bar as ViewGroup).childCount) {
                val ch = bar.getChildAt(i)
                if (menuCls.isInstance(ch)) {
                    menu = ch
                    break
                }
            }
            note("menu view = $menu  (children=${(bar as ViewGroup).childCount})")

            if (menu != null) {
                // `isStrokeEffectEnabled()` / `isBlurEffectEnabled()` / `isCausticShadowEffectEnabled()`
                // all read:
                //     if (mUseMaterialEffectAttr == FORCE_ENABLE) return true
                //     if (mUseMaterialEffectAttr == FORCE_DISABLE) return false
                //     return COUIMaterialStateManager.isXxxEnable()   <- false for a 3rd-party app
                // `setUseMaterialEffectAttr` itself bails out on `checkThemeEnable(context)`, so the
                // field is written straight. `FORCE_ENABLE.value == 1`.
                runCatching {
                    var c: Class<*>? = menuCls
                    var written = false
                    while (c != null && !written) {
                        val f = runCatching { c!!.getDeclaredField("mUseMaterialEffectAttr") }.getOrNull()
                        if (f != null) {
                            f.isAccessible = true
                            note("mUseMaterialEffectAttr was ${f.getInt(menu)}")
                            f.setInt(menu, 1)
                            note("mUseMaterialEffectAttr := 1 (FORCE_ENABLE)")
                            written = true
                        }
                        c = c?.superclass
                    }
                    if (!written) note("mUseMaterialEffectAttr NOT FOUND on menu chain")
                }.onFailure { note("force write failed: ${it.message}") }

                // `mIsDrawableMaterialEffect` must be true as well, otherwise the two `if (!…)`
                // early-outs above are taken before the attr is even read.
                runCatching {
                    var c: Class<*>? = menuCls
                    while (c != null) {
                        val f = runCatching { c!!.getDeclaredField("mIsDrawableMaterialEffect") }.getOrNull()
                        if (f != null) {
                            f.isAccessible = true
                            f.setBoolean(menu, true)
                            note("mIsDrawableMaterialEffect := true")
                            break
                        }
                        c = c?.superclass
                    }
                }.onFailure { note("mIsDrawableMaterialEffect failed: ${it.message}") }

                // Rebuild: `initTexture()` creates the ShadowEdge drawable and pushes the
                // blur/stroke params; `initShadowEdgeDrawableIfNeeded()` gates on
                // `COUIMaterialEffect.isMaterialVersion()`.
                for (m in listOf("initShadowEdgeDrawableIfNeeded", "initTexture", "updateStrokeEffect", "updateBlurEffect", "initSpotLightEffect")) {
                    runCatching {
                        var c: Class<*>? = menuCls
                        var invoked = false
                        while (c != null && !invoked) {
                            val mm = runCatching { c!!.getDeclaredMethod(m) }.getOrNull()
                            if (mm != null) {
                                mm.isAccessible = true
                                mm.invoke(menu)
                                note("$m() ok")
                                invoked = true
                            }
                            c = c?.superclass
                        }
                        if (!invoked) note("$m() not found")
                    }.onFailure { note("$m() failed: ${it.cause?.javaClass?.name}: ${it.cause?.message}") }
                }

                // Report whether the shader and the spotlight actually came alive.
                for (f in listOf("mShadowEdgeDrawable", "mSpotLightEffectDrawable", "mShadowEdgeDrawable")) {
                    runCatching {
                        var c: Class<*>? = menuCls
                        while (c != null) {
                            val ff = runCatching { c!!.getDeclaredField(f) }.getOrNull()
                            if (ff != null) {
                                ff.isAccessible = true
                                note("$f = ${ff.get(menu)}")
                                break
                            }
                            c = c?.superclass
                        }
                    }
                }
            }
            (bar as View).invalidate()
            runCatching {
                barCls.getMethod("setSelectedItemId", Int::class.javaPrimitiveType).invoke(bar, 0)
                note("setSelectedItemId(0)")
            }.onFailure { note("setSelectedItemId failed: ${it.message}") }

            slot.addView(
                bar,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            note("attached")
            true
        } catch (t: Throwable) {
            note("FAILED: ${t.javaClass.name}: ${t.message}")
            t.stackTrace.take(8).forEach { note("   at $it") }
            false
        }

    private fun note(line: String) {
        sb.appendLine(line)
        Log.i(tag, line)
    }

    private fun capture() {
        val decor = window.decorView
        if (decor.width <= 0 || decor.height <= 0) return
        val bmp = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
        android.view.PixelCopy.request(
            window,
            bmp,
            { r ->
                if (r == android.view.PixelCopy.SUCCESS) {
                    runCatching {
                        val dir = getExternalFilesDir(null) ?: filesDir
                        java.io.FileOutputStream(java.io.File(dir, "official.png")).use {
                            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
                bmp.recycle()
            },
            android.os.Handler(android.os.Looper.getMainLooper()),
        )
    }
}