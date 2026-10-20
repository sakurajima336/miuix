// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    id("module.kotlin-jvm-toolchain")
    id("module.coui-publication")
    id("module.spotless")
}

couiPublication {
    description.set("COUI blur: the ColorOS material blur (RenderEffect chain + backdrop capture)")
}

/**
 * The COUI (ColorOS 17) blur material, reverse engineered from `com.coui.appcompat.COUIMaterialBlurEffect`
 * and `com.oplus.posteffect.*`.
 *
 * Deliberately free of any dependency on `coui-core` or Miuix: this module only needs Compose
 * foundation + ui, so a host can pull the blur in without the whole design system.
 */
kotlin {
    withSourcesJar(true)
    android {
        buildToolsVersion = BuildConfig.BUILD_TOOLS_VERSION
        compileSdk {
            version =
                release(BuildConfig.COMPILE_SDK) {
                    minorApiLevel = BuildConfig.COMPILE_SDK_MINOR
                }
        }
        minSdk = BuildConfig.MIN_SDK
        namespace = "io.github.wfc35286.coui.kmp.blur"
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    js {
        browser()
    }

    applyMiuixSourceSetHierarchy()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.foundation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
