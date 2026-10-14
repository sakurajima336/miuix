// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.wfc35286.coui.kmp.component

/**
 * `com.coui.appcompat.spotlight.agsl.SpotLight.AGSL_SOURCE`, copied verbatim out of
 * `设置_17.0.0.apk` (`com.coui.appcompat.spotlight.agsl.SpotLight`, field `AGSL_SOURCE`).
 *
 * `u_lightPosAndHeight` packs `(x, y, radius, intensity)`, `u_lightColorAndAmbient` packs
 * `(r, g, b, ambientStrength)`, and `u_lightingParams` packs
 * `(diffuseStrength, lightHeight, noise, ditherN)` - the same vec4 packing the original uses to
 * keep the uniform count down. The caller supplies light position in pixels, after the original
 * normalized-position conversion. `COUISpotLightEffect` animates position/radius/intensity and lets
 * this shader do the lighting maths; the result is drawn with `BlendMode.OVERLAY`.
 */
internal const val COUI_SPOT_LIGHT_SHADER_SOURCE: String = """
        // ═════════════════════════════════════════════════════════════════════════════════
        // UNIFORMS - Vec4 打包优化
        // ═════════════════════════════════════════════════════════════════════════════════

        // Group 1: 光源位置、半径和强度 (lightPos.x, lightPos.y, lightRadius, intensity)
        // 动画时只更新这个 vec4（位置、半径、强度都会变化）
        uniform vec4 u_lightPosAndHeight;

        // Group 2: 光源颜色和环境光 (lightColor.r, lightColor.g, lightColor.b, ambientStrength)
        uniform vec4 u_lightColorAndAmbient;

        // Group 3: 光照与效果参数 (diffuseStrength, lightHeight, noise_exp, ditherN)
        uniform vec4 u_lightingParams;

        // Group 4: 分辨率（保持 vec2）
        uniform vec2 resolution;
        float luminance(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

        float max3(vec3 v) { return max(max(v.r, v.g), v.b); }

        // 简化的噪声函数
        float noise(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
        }

        float softLight(float A, float B) {
            float l = B - (1.0 - 2.0 * A) * B * (1.0 - B);
            float h = B + (2.0 * A - 1.0) * (sqrt(B) - B);
            return mix(l, h, step(0.5, A));
        }

        // 优化的 dither 函数（保留原始双哈希算法）
        float dither(vec2 coord, float ditherN) {
            float n1 = fract(sin(dot(coord, vec2(12.9898, 78.233))) * 43758.5453);
            float n2 = fract(sin(dot(coord + vec2(1.0), vec2(12.9898, 78.233))) * 43758.5453);
            return (n1 + n2 - 1.0) * ditherN / 255.0;  // 范围 [-1/255, +1/255]
        }

        // ═════════════════════════════════════════════════════════════════════════════════
        // MAIN SHADER - 完全内联优化
        // ═════════════════════════════════════════════════════════════════════════════════
        half4 main(float2 fragCoord) {
            // ─────────────────────────────────────────────────────────────────────────────
            // 1. 提取 uniform 参数（减少重复访问）
            // ─────────────────────────────────────────────────────────────────────────────
            float2 lightP = u_lightPosAndHeight.xy;
            float radius = u_lightPosAndHeight.z;
            float intensity = u_lightPosAndHeight.w;

            vec3 lightCol = u_lightColorAndAmbient.rgb;
            float ambientStr = u_lightColorAndAmbient.w;

            float diffuseStr = u_lightingParams.x;
            float lightH = u_lightingParams.y;
            float noise_exp = u_lightingParams.z;
            float ditherN = u_lightingParams.w;

            const float lightIntensity = 1.5;

            // ─────────────────────────────────────────────────────────────────────────────
            // 2. 距离计算和提前退出优化（避免 sqrt）
            // ─────────────────────────────────────────────────────────────────────────────
            vec2 delta = fragCoord - lightP;
            vec3 L = vec3(delta, lightH);
            float dist = length(L);
            L = normalize(L);

            vec3 N = vec3(0.0, 0.0, 1.0);
            float diff = max(dot(N, L), 0.0);
            vec3 lighting = (ambientStr + diffuseStr * diff) * lightCol;

            if(dist >= radius) {
                return half4(0.0, 0.0, 0.0, 0.0);
            }

            float att = smoothstep(1.0, 0.0, dist / radius) * step(dist, radius);

            lighting *= att * lightIntensity * intensity;
            float alpha = clamp(luminance(lighting), 0.0, 1.0);

            // 后处理总开关（step 替代提前返回分支，避免 warp divergence）
            float postOn = max(step(0.001, noise_exp), step(0.001, ditherN));
            vec3 postLighting = lighting;
            float postAlpha = alpha;

            // 噪声处理（step 替代分支）
            float noiseOn = step(0.001, noise_exp);
            vec2 uv_pos = fragCoord * 9.259e-4;  // 1.0 / 1080.0 预计算
            float n = clamp((noise(uv_pos) - 0.5) * noise_exp + 0.5, 0.0, 1.0);
            float mask = max3(postLighting);
            float fadeAlpha = clamp(softLight(n, mask), 0.0, 1.0);
            float noiseDelta = clamp(abs(fadeAlpha - mask), 0.0, 1.0);
            postLighting += vec3(noiseDelta) * noiseOn;

            // 抖动处理（step + mix 替代分支）
            float ditherOn = step(0.001, ditherN);
            float d = dither(fragCoord, ditherN);
            postLighting += vec3(d) * ditherOn;
            postAlpha = mix(postAlpha, clamp(postAlpha + d, 0.0, 1.0), ditherOn);

            lighting = mix(lighting, postLighting, postOn);
            alpha = mix(alpha, postAlpha, postOn);

            return half4(lighting, alpha);
        }
    """

/**
 * Creates the COUI spot-light shader, or `null` where runtime shaders are unavailable (below
 * API 33 on Android).
 */
internal expect fun createCouiSpotLightShader(): CouiSpotLightShader?

/**
 * Returns a brush for the current uniforms: Android reuses its live RuntimeShader brush;
 * Skiko creates an immutable shader snapshot after the uniforms have been updated.
 */
internal expect fun spotLightBrush(shader: CouiSpotLightShader): androidx.compose.ui.graphics.ShaderBrush

/**
 * The COUI spot-light shader.
 *
 * `COUISpotLightEffect` drives these uniforms and lets the fragment shader do the lighting
 * maths: a point light at `(x, y, lightHeight)` whose `radius` bounds its reach, an `intensity`
 * scale, an ambient + diffuse term, and optional noise/dither post-processing.
 */
internal interface CouiSpotLightShader {
    fun setLightPosition(x: Float, y: Float)
    fun setLightRadius(radius: Float)
    fun setIntensity(intensity: Float)
    fun setLightColor(r: Float, g: Float, b: Float)
    fun setAmbientStrength(strength: Float)
    fun setDiffuseStrength(strength: Float)
    fun setLightHeight(height: Float)
    fun setNoise(noise: Float)
    fun setDitherN(ditherN: Float)
    fun setResolution(width: Float, height: Float)
}
