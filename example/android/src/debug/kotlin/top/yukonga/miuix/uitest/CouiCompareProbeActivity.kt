// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBar
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarDefaults
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarItem
import io.github.wfc35286.coui.kmp.basic.CouiRedDotType
import io.github.wfc35286.coui.kmp.graphics.couiBackdrop
import io.github.wfc35286.coui.kmp.graphics.rememberCouiBackdrop
import io.github.wfc35286.coui.kmp.theme.CouiTheme


/**
 * Dead-simple A/B probe: **one black page, the official bar on top, ours underneath**.
 *
 * Everything else in this folder measures one of the two in isolation against a synthetic page.
 * That is how the previous rounds ended up "aligned on paper, wrong on screen": the official was
 * measured while its material silently degraded to the flat fallback, and ours was measured over
 * a page whose colours hid the parts that mattered. Putting both on the same flat black backdrop,
 * in the same window, and grabbing the window with `PixelCopy` removes every one of those
 * confounds.
 *
 * The black page is deliberate: it is the only background on which the `ShadowEdge` output is the
 * *only* thing visible, so the rim's profile and the inner-shadow wash can be read straight off
 * the pixels.
 *
 * Output: `files/compare.png` (window copy) and `files/compare.txt` (what the reflection did).
 */
class CouiCompareProbeActivity : ComponentActivity() {
    private val tag = "CouiCompare"
    private val sb = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the status bar. The naive `systemUiVisibility` flags still work on every API we
        // care about and avoid pulling in more androidx plumbing than a debug probe needs.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val official = tryBuildOfficial()
        note("official = $official")

        setContent {
            CouiTheme {
                val icon = rememberVectorPainter(ProbeIcon)
                // Our material path needs a backdrop to sample; on a black page that yields the
                // darkened, colour-filtered wash the official produces when it *can* sample.
                val backdrop = rememberCouiBackdrop()
                val items =
                    listOf("首页", "游戏", "软件", "榜单", "我的").mapIndexed { i, title ->
                        CouiBottomFloatingNavigationBarItem(
                            id = i,
                            title = title,
                            icon = icon,
                            iconTint = CouiBottomFloatingNavigationBarDefaults.titleColor(i == 0),
                            redDotType = if (i == 4) CouiRedDotType.Number else CouiRedDotType.None,
                            redDotNumber = 15,
                        )
                    }
                Box(modifier = Modifier.fillMaxWidth().background(ComposeColor.Black)) {
                    // The captured page is a **sibling** of the bars, never their parent: a parent
                    // would record the pill into its own backdrop and feed it back into the blur.
                    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black).couiBackdrop(backdrop))
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 120.dp)) {
                        // ---- official, reflected out of the Settings APK ---------------------------
                        Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                            if (official != null) {
                                AndroidView(
                                    factory = {
                                        (official.parent as? ViewGroup)?.removeView(official)
                                        official
                                    },
                                    modifier = Modifier.fillMaxWidth().height(72.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        // ---- ours -----------------------------------------------------------------
                        CouiBottomFloatingNavigationBar(
                            items = items,
                            selectedIndex = 0,
                            onSelected = {},
                            backdrop = backdrop,
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }

        runCatching {
            val dir = getExternalFilesDir(null) ?: filesDir
            java.io.File(dir, "compare.txt").writeText(sb.toString())
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ capture() }, 2500)
    }

    /**
     * Instantiate the real `COUIBottomFloatingNavigationBar` out of the installed Settings APK and
     * force its material on. Same sequence as `CouiOfficialNavBarProbeActivity`, condensed.
     */
    private fun tryBuildOfficial(): View? =
        try {
            val ctx =
                createPackageContext(
                    "com.android.settings",
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE,
                )
            val prefix = "com.coui.appcompat.bottomfloatingnavigationbar"
            val barCls = Class.forName("$prefix.COUIBottomFloatingNavigationBar", true, ctx.classLoader)
            val themeId = ctx.resources.getIdentifier("Theme.COUI.Main", "style", "com.android.settings")
            note("Theme.COUI.Main = 0x${themeId.toString(16)}")
            val themed = android.view.ContextThemeWrapper(ctx, themeId)
            val bar = barCls.getConstructor(Context::class.java).newInstance(themed) as View

            val builderCls =
                Class.forName("$prefix.COUIBottomFloatingNavigationBarItem\$Builder", true, ctx.classLoader)
            val setId = builderCls.getMethod("setId", Int::class.javaPrimitiveType)
            val setTitle = builderCls.getMethod("setTitle", String::class.java)
            val setIcon = builderCls.getMethod("setIcon", android.graphics.drawable.Drawable::class.java)
            val build = builderCls.getMethod("build")
            @Suppress("DEPRECATION")
            val icon = resources.getDrawable(android.R.drawable.ic_menu_camera, null)
            val items = java.util.ArrayList<Any>()
            listOf("首页", "游戏", "软件", "榜单", "我的").forEachIndexed { i, t ->
                val b = builderCls.getConstructor().newInstance()
                setId.invoke(b, i)
                setTitle.invoke(b, t)
                if (icon != null) setIcon.invoke(b, icon)
                items.add(build.invoke(b))
            }
            runCatching { barCls.getMethod("addItems", java.util.List::class.java).invoke(bar, items) }
                .onFailure { note("addItems failed: ${it.cause?.message}") }
            runCatching {
                barCls.getMethod("setIsDrawableMaterialEffect", Boolean::class.javaPrimitiveType)
                    .invoke(bar, true)
            }

            // The material lives on the inner menu view, not on the shell `ViewGroup`.
            val menuCls =
                Class.forName("$prefix.COUIBottomFloatingNavigationBarMenuView", true, ctx.classLoader)
            var menu: View? = null
            for (i in 0 until (bar as ViewGroup).childCount) {
                val ch = bar.getChildAt(i)
                if (menuCls.isInstance(ch)) {
                    menu = ch
                    break
                }
            }
            note("menu = $menu")
            if (menu != null) {
                writeIntField(menuCls, menu, "mUseMaterialEffectAttr", 1)
                writeBoolField(menuCls, menu, "mIsDrawableMaterialEffect", true)
                for (m in listOf("initShadowEdgeDrawableIfNeeded", "initTexture", "updateStrokeEffect", "updateBlurEffect")) {
                    runCatching {
                        var c: Class<*>? = menuCls
                        while (c != null) {
                            val mm = runCatching { c!!.getDeclaredMethod(m) }.getOrNull()
                            if (mm != null) {
                                mm.isAccessible = true
                                mm.invoke(menu)
                                note("$m() ok")
                                break
                            }
                            c = c?.superclass
                        }
                    }.onFailure { note("$m() failed: ${it.cause?.message}") }
                }
                note("mShadowEdgeDrawable = " + readField(menuCls, menu, "mShadowEdgeDrawable"))
            }
            runCatching {
                barCls.getMethod("setSelectedItemId", Int::class.javaPrimitiveType).invoke(bar, 0)
            }
            bar
        } catch (t: Throwable) {
            note("official FAILED: ${t.javaClass.name}: ${t.message}")
            null
        }

    private fun fieldOf(
        cls: Class<*>,
        name: String,
    ): java.lang.reflect.Field? {
        var c: Class<*>? = cls
        while (c != null) {
            val f = runCatching { c!!.getDeclaredField(name) }.getOrNull()
            if (f != null) {
                f.isAccessible = true
                return f
            }
            c = c?.superclass
        }
        return null
    }

    private fun writeIntField(
        cls: Class<*>,
        obj: Any,
        name: String,
        value: Int,
    ) {
        fieldOf(cls, name)?.setInt(obj, value)
        note("$name := $value")
    }

    private fun writeBoolField(
        cls: Class<*>,
        obj: Any,
        name: String,
        value: Boolean,
    ) {
        fieldOf(cls, name)?.setBoolean(obj, value)
        note("$name := $value")
    }

    private fun readField(
        cls: Class<*>,
        obj: Any,
        name: String,
    ): Any? = fieldOf(cls, name)?.get(obj)

    private fun note(line: String) {
        sb.appendLine(line)
        Log.i(tag, line)
    }

    private fun capture() {
        val decor = window.decorView
        if (decor.width <= 0 || decor.height <= 0) return
        val bmp =
            android.graphics.Bitmap.createBitmap(
                decor.width,
                decor.height,
                android.graphics.Bitmap.Config.ARGB_8888,
            )
        android.view.PixelCopy.request(
            window,
            bmp,
            { r ->
                if (r == android.view.PixelCopy.SUCCESS) {
                    runCatching {
                        val dir = getExternalFilesDir(null) ?: filesDir
                        java.io.FileOutputStream(java.io.File(dir, "compare.png")).use {
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

private val ProbeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "probe",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(ComposeColor.Black)) {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }.build()
}
