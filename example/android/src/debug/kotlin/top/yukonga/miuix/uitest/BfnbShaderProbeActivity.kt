// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest

import android.graphics.RuntimeShader
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

/**
 * Debug host that reports whether the COUI panel-material shaders compile on this device, and
 * whether the Oplus framework entry points resolved. Reaches into coui-core reflectively so no
 * production surface has to be widened just for diagnostics.
 */
class BfnbShaderProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = buildReport()
        Log.i("BFNBProbe", report)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(
            TextView(this).apply {
                text = report
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 11f
                setPadding(24, 24, 24, 24)
            },
        )
        setContentView(root)
    }

    private fun buildReport(): String =
        buildString {
            appendLine("sdk=${android.os.Build.VERSION.SDK_INT}")
            appendLine(
                runCatching {
                    val m = Class.forName("io.github.wfc35286.coui.kmp.component.CouiShadowEdgeShaderKt")
                        .getMethod("couiShadowEdgeCompileReport")
                    m.invoke(null) as String
                }.getOrElse { "ShadowEdge: ${it.javaClass.name}: ${it.message}" },
            )
            appendLine(oplus())
        }

    private fun oplus(): String =
        buildString {
            val util =
                runCatching { Class.forName("com.oplus.view.material.OplusMaterialUtil") }
                    .getOrElse {
                        appendLine("OplusMaterialUtil ABSENT: ${it.javaClass.name}")
                        return@buildString
                    }
            appendLine("OplusMaterialUtil methods: ${util.declaredMethods.map { it.name }.distinct().sorted()}")
            for (n in
                listOf(
                    "com.oplus.view.material.OplusMaterialCornerParams",
                    "com.oplus.view.material.OplusMaterialEdgeParams",
                    "com.oplus.view.material.OplusMaterialShadowParams",
                    "com.oplus.view.material.OplusMaterialBaseParams",
                    "com.oplus.view.OplusView",
                )
            ) {
                val cls = runCatching { Class.forName(n) }.getOrNull()
                if (cls == null) {
                    appendLine("$n ABSENT")
                    continue
                }
                val ctors = cls.declaredConstructors.joinToString(", ") { it.parameterTypes.joinToString { p -> p.simpleName } }
                appendLine("$n ctors: [$ctors]")
            }
        }
}