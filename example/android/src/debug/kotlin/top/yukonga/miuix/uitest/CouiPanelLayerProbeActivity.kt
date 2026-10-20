// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.wfc35286.coui.kmp.component.CouiPanelLayerProbe
import java.io.File
import java.io.FileOutputStream

/**
 * Debug host for [CouiPanelLayerProbe]: one material layer per row, on black.
 *
 * Runs full-screen with the status bar hidden, and snapshots **its own window** with PixelCopy.
 * A plain `screencap` keeps catching whatever is on top at that instant (the assistant's floating
 * window), which makes the measurement useless.
 */
class CouiPanelLayerProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CouiPanelLayerProbe(dark = true) }
        // `insetsController` is null until the window has a decor view, so this runs after
        // `setContent`.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ captureWindow() }, 2500)
    }

    private fun captureWindow() {
        val decor = window.decorView
        if (decor.width <= 0 || decor.height <= 0) return
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            window,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    runCatching {
                        val dir = getExternalFilesDir(null) ?: filesDir
                        FileOutputStream(File(dir, "layers.png")).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
                bitmap.recycle()
            },
            Handler(Looper.getMainLooper()),
        )
    }
}
