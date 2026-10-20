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
import component.CouiSpotLightProbe
import java.io.File
import java.io.FileOutputStream

/** Debug host for the spot light; writes its own window to files/spotlight.png. */
class CouiSpotLightProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CouiSpotLightProbe() }
        Handler(Looper.getMainLooper()).postDelayed({ captureWindow() }, 3000)
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
                        FileOutputStream(File(dir, "spotlight.png")).use {
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
