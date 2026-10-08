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
    description.set("COUI design-system foundation: tokens, shape, motion and theme")
}

/**
 * The COUI design-system foundation: tokens, shape, motion and theme.
 *
 * Deliberately has **no dependency on Miuix** — the point of this module is to be an
 * independent ColorOS/COUI layer that can sit on top of, or entirely replace, Miuix.
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
        namespace = "io.wfc35286.coui.kmp.core"
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
            api(libs.capsule)
        }
    }
}
