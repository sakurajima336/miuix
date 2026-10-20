// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import component.CouiNavBarProbe
import java.io.File
import java.io.FileOutputStream

/**
 * Debug-only host for the floating navigation bar.
 *
 * It snapshots **its own window** with PixelCopy (a plain `screencap` also grabs the Operit
 * assistant overlay) and writes a **burst** of frames to `getExternalFilesDir()/navbar_%02d.png`.
 * The burst is what makes the drag measurable: with `adb shell input swipe` running in parallel the
 * frames show whether the blurred page stays put on screen while the pill travels over it, or
 * whether the sample slides along with the pill.
 *
 * ```
 * adb shell am start -n top.yukonga.miuix.uitest/.CouiNavBarProbeActivity
 * adb shell input swipe 300 470 900 470 2000
 * adb pull /sdcard/Android/data/top.yukonga.miuix.uitest/files/
 * ```
 */
class CouiNavBarProbeActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var frame = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CouiNavBarProbe() }
        // 40 frames at 100 ms: covers a 2 s drag from start to the release spring.
        handler.postDelayed({ captureBurst() }, 1500)
    }

    private fun captureBurst() {
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
                        FileOutputStream(File(dir, "navbar_%02d.png".format(frame))).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
                bitmap.recycle()
                frame++
                if (frame < 40 && !isFinishing) handler.postDelayed({ captureBurst() }, 100L)
            },
            handler,
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}