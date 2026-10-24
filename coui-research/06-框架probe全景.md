# 06 · 框架 probe 全景：framework 层还有什么官方 UI 能力

> 扫描日期：2026-10-24。全部结论来自本会话实际执行的命令输出（本地 class 清单统计 + REA 反编译），未看到的数值一律标"未确认"。

---

## ① 扫描范围与方法

**对象（apks/）**：

| probe | 大小 | 类数 | 说明 |
|---|---|---|---|
| device-framework-probe.apk | 52MB | 14019 | AOSP framework.jar 全量 + 77 个 com.oplus 钩子 |
| framework-dex-probe.apk | 19MB | 14019 | 与 device-framework-probe **类清单逐字节相同**（已验证，二者是同一 dex 的不同包装，后续只需看一个） |
| hwui-probe.apk | 35MB | **0** | 见下文专节：无 dex，纯 native |
| services-dex-probe.apk | 17MB | 9481 | AOSP services.jar |
| oplus-services-dex-probe.apk | 12MB | 4405 | services.jar 的 oplus 扩展 dex，含 **com.coui.animatekit 103 类** |
| oplusfw-probe.apk | 5MB | 3507 | oplus-framework.jar 的 dex 包装 |
| oplus-fw-dex-probe.apk | 5MB | 3507 | 与 oplusfw-probe **类清单相同**（已验证） |
| oplus-framework.jar | 12MB | — | REA 只吃 APK，未解 jar 本体（按任务要求跳过） |

**方法**：先用 `recon/*.classes.json` 本地统计全部 7 个 probe 的顶层包分布（零加载成本），再对 oplus-fw-dex-probe（5MB，加载最快）和 oplus-services-dex-probe（12MB）做 REA `inspect-android-class` / `inspect-android-method` 深挖。执行过的 REA 命令约 25 次，产物在 `recon/rea/`。

**各 probe 顶层包分布**（前几名，仅列 UI 相关重点）：

- device-framework-probe：`com.android` 3383 | `android.hardware` 2052 | `android.media` 1059 | `android.app` 1053 | **`android.view` 620** | **`android.widget` 257** | **`android.graphics` 247** | **`android.text` 155** | `com.oplus` 77（几乎全是 AIDL 接口与反射工具，无视图）
- oplus-fw-dex-probe：**`com.oplus` 2000** | **`android.view` 266（大量 Oplus 挂钩类）** | `android.app` 194 | **`com.motion` 146（OLive 实况照片解码，非 UI 组件）** | `com.android` 144 | `android.widget` 61（全为 Oplus 滚动/文本扩展）
- services-dex-probe：`com.android` 6144（server 侧全量）+ 大量 kotlin/第三方压缩库，UI 价值低
- oplus-services-dex-probe：`com.android` 3957（其中 `server.wm` 891 类、471 个 Oplus 类）| **`com.coui` 103（animatekit 动画框架）** | `com.oplus` 239

---

## ② hwui-probe.apk 为什么是 0 类

用 REA `inspect-android-package` 直接确认（`recon/hwui-pkg.json`）：该 APK **只有一个 manifest 和两个 native 库，没有任何 dex**：

```
Archive:  hwui-probe.apk
   49784  AndroidManifest.xml
17441184  lib/arm64-v8a/libhwui.so
17441184  libhwui.so          ← 根目录重复一份
3 files
```

manifest 的包名是 `com.android.incallui`、版本 `17.6.5`——与 电话_17.6.5.apk 相同，显然是打包工具借用了一份现成 manifest 给这个"库探针"一个身份。**libhwui.so 是 ColorOS 定制的渲染引擎（HWUI）native 实现**，17MB， jadx/REA 的 Java 反编译完全用不上；要看它需要 native 逆向（Ghidra/IDA），超出本轮范围。它对 coui-core 的间接价值：圆角/阴影/模糊的最终实现都在这个 so 里，Java 层的 `com.oplus.graphics.Oplus*`（见下）只是它的 JNI 门面。

---

## ③ 新发现组件深挖

以下类在 coui-core（`io.github.wfc35286.coui.kmp.*`）中均无对应物。

### 3.1 com.coui.animatekit —— system_server 侧的"Compose 式"动画框架（103 类，价值最高）

- **位置**：oplus-services-dex-probe.apk（12MB）
- **构成**（类清单全量枚举 + 6 个类 inspect）：
  - `dsl/`：`AnimExtensionKt`、`COUIAnimationDSLBuilder`、`COUIViewAnimationDSLBuilder`、`COUISurfaceControlAnimationDSLBuilder`、`AnimationChain`、`PropertiesBuilder`
  - `curve/`：`IAnimCurve`、`COUICommonCurve`、`COUISpringCurve`、`Curves`（object）、`CurveType`
  - `state/`：`MutableState`、`RememberStateDelegate`、`StateProperty`、`ImplicitAnimationBuilder`、`ImplicitAnimationConfig`、各 `COUIxxState`
  - `target/`：`ViewAnimationTarget`、`SurfaceControlAnimationTarget`、`GenericAnimationTarget`
  - `strategy/`：`DefaultViewPropertyUpdateStrategy`、`OplusAsyncPropertyUpdateStrategy`、`MainThreadPropertyUpdateStrategy`
  - `manager/`、`modifier/`、`option/`、`util/COUISpringAnimationSet` 等

**这是把 Jetpack Compose 的动画心智模型（曲线抽象 + 状态驱动 + 隐式动画 + DSL）移植到 View/SurfaceControl 的官方实现**，而且 Kotlin 编写（大量 `KProperty`/`Function1` 签名可见）。

关键 API（均来自反编译源码，注明类+方法）：

```kotlin
// AnimExtensionKt —— 对外入口，一行 DSL 驱动任意 View / SurfaceControl
fun View.animateTo(builder: COUIViewAnimationDSLBuilder.() -> Unit)
fun SurfaceControl.animateTo(builder: COUISurfaceControlAnimationDSLBuilder.() -> Unit)
fun <T> animateToGeneric(target: T, builder: COUIAnimationDSLBuilder<T>.() -> Unit)

// Curves（object）—— 曲线工厂，带时长参数
Curves.easeInOut(300L)            // 默认 300ms（easeInOut$default 反编译证实：j=300）
Curves.cubicBezier(duration, x1,y1,x2,y2)
Curves.responsiveSpring(response, damping, velocity)
Curves.responseToStiffness(response) = (2π / response)²   // 反编译源码原文
```

物理弹簧用 **response（自然周期秒）/ dampingRatio 参数化**，不是 stiffness/1000 数值系——`RSSpringModel.CalculateSpringParameters()`（完整反编译）实现了欠阻尼/临界/过阻尼三段解析解，`coeffDecay_/coeffScale_/coeffDecayAlt_/coeffScaleAlt_` 四系数闭式外推，还带 `estimateDuration()` 收敛时长预估。这套解析解比逐帧数值积分精确且可跳帧。

隐式动画（Compose 同款心智模型）：

```kotlin
// ImplicitAnimationBuilder（反编译 alpha 方法原文）
fun alpha(state: RememberStateDelegate<Float>) {
    observedStates.add(state)
    modifierConfigs.add(ModifierConfig.AlphaWithState(state))
}
// scale(stateX, stateY) / translate(stateX, stateY) / setCurve(...) / setOnFinish(...)
```

`SurfaceControl` 侧支持 `bounds(rect, scaleType, transform, curve)` 与 `bindExternalTransactionIfNeeded`——即系统动画直接走 SurfaceFlinger transaction，不重排 View。`strategy/OplusAsyncPropertyUpdateStrategy` 暗示动画属性更新可发到非主线程。

**与 coui-core 对照**：coui-core 的 `motion/CouiSpring、CouiEasing、CouiMotionTokens` 是数据层；animatekit 提供的是"状态 → 隐式动画 → 曲线 → 目标代理"完整执行层，且 SurfaceControl target 思路对 KMP 无对应。**移植要点**：`response→stiffness = (2π/response)²` 换算、`Curves` 工厂的 300ms 默认时长、`RememberStateDelegate` 属性委托触发隐式动画的模式可以直接在 Compose 里用 `animate*AsState` 对等实现；RSSpringModel 的三段解析解可替换/校准 coui-core 的 CouiSpring 数值积分。**强烈值得移植为 motion 层参考**。

### 3.2 android.widget.OplusSpringOverScroller + SpringScrollHelper + COUISpringInterpolator —— framework 级滚动/回弹物理

- **位置**：oplus-fw-dex-probe.apk（`android.widget` 命名空间，直接替换 AOSP OverScroller 生态）
- **生态位**：device-framework-probe 里可见注入点：`OverScrollerSocExtImpl`、`ScrollerSocExtImpl`、`SplineOverScrollerSocExtImpl`、`IOplusOverScrollerExt`、`IOplusScrollViewExt` ——Oplus 通过 Ext 接口把 AOSP `OverScroller/Scroller` 内部滚动物理换成自己的实现。

**SpringScrollHelper**（inspect + 3 个方法完整反编译）——"起手弹簧"：手指拖动列表起步时用**临界阻尼解析弹簧**驱动位移：

```java
// android.widget.SpringScrollHelper.setupSpringScroll(int dx, int dy)  节选
double vNorm = velocityInDir / totalDistance;
this.mSpringDampingRatio = 1.0d;                       // 临界阻尼
this.mSpringOmega = Math.sqrt(springStiffness);        // stiffness 由 getDefaultSpringStiffness() 取
// mSpringType: 1=过阻尼 2=临界 3=欠阻尼；三段闭式解系数 mSpringCoeffA/B
this.mSpringValueThreshold = DEFAULT_PIXEL_THRESHOLD / totalDistance;
this.mSpringDuration = computeSpringConvergenceTime();
```

- `computeSpringConvergenceTime()`：按 `Choreographer.getFrameIntervalNanos()` 逐步外推，位移与速度双阈值收敛，上限 2000ms。
- 仅用于**纵向**（`Math.abs(dx) > Math.abs(dy)` 则关闭），起步速度取 `ViewRootImplExtImpl.getLastTouchGestureVelocity()`。
- 性能分级：`isLowPerformanceLevel()` 走 `DEFAULT_SPRING_STIFFNESS_LOW`（数值在 `<clinit>`，**未能反编译，未确认**；`<clinit>` 在 REA 里无法按名定位，已尝试多种写法）。

**OplusSpringOverScroller**（inspect + 4 方法）——fling 物理按**速度分带摩擦**，且**可由 RUS（远程配置）下发**：

```java
// setSpringConfigFromRus(String) 反编译原文（键序即配置格式 "midVt,highVt,lowFric,midFric,flingFric"）
case 0: setMidVelocityThreshold(...);  case 1: setHighVelocityThreshold(...);
case 2: setLowVelocityFriction(...);    case 3: setMidVelocityFriction(...);
case 4: OPLUS_FLING_FRICTION_NORMAL = ...;
```

内层 `ReboundOverScroller`（inspect，字段全表）暴露完整的分带模型：`FLING_VELOCITY_LOWEST/LOW/HIGH/HIGHEST`、`DELTA_POSITION_LOW/MID/HIGH`、`SLOW_FLING_BASE_FRICTION / MID_FLING_BASE_FRICTION`、`SPRING_BACK_TENSION / SPRING_BACK_FRICTION`、`GRAVITY`、状态机 `SPLINE/BALLISTIC/CUBIC/SPRING_BACK`，还有一个**微信专项优化**：`WECHAT_PACKAGE_NAME` / `mIsWeChatSnsTimelineUI` / `DELTA_POSITION_WECHAT`（对朋友圈时间线单独调参——系统级按应用白名单改滚动手感，这是 OEM 才能做的事）。摩擦带逻辑反编译原文：

```java
// ReboundOverScroller.adjustFrictionByStartVelocity() 节选
if (|v| > MID_VELOCITY_ADJUST_FRICTION && |v| < MAX_VELOCITY_ADJUST_FRICTION)
    mConfig.mFriction = MID_FLING_BASE_FRICTION;
else if (|v| <= MID_VELOCITY_ADJUST_FRICTION) mConfig.mFriction = SLOW_FLING_BASE_FRICTION;
else mConfig.mFriction = mConfig.frictionFromOrigamiValue(OPLUS_FLING_FRICTION_NORMAL);
```

**COUISpringInterpolator**（inspect + getInterpolation/getOriginInterpolation 反编译）——把弹簧解包成 `TimeInterpolator`：构造 `(dampingRatio, stiffness)` 或 `(dampingRatio, stiffness, velocity, finalValue)`；`getOriginInterpolation(input)` 按欠/临界/过阻尼三段解析式（`sinh/cosh` 过阻尼分支可见），并用 `mCutRatio` 截断 + **按 `f(1.0)` 归一化**（`getInterpolation` 里 `origin/origin(1)`），保证插值曲线精确回到 1。

**移植要点**：coui-core 的 `CouiSpring` 是组件内弹簧；这组类给出的是**列表滚动三段手感**（起步弹簧 + 分带 fling + springBack）的官方参数化方式。KMP 下对应 `PagerAdapter/Scrollable` 的 flingBehavior 可移植：①临界阻尼起步弹簧；②速度分带摩擦（低/中/高三带 + RUS 可调的思路）；③springBack 用 `SPRING_BACK_TENSION/FRICTION`。具体常量值需在真机 dump RUS 后确认。

### 3.3 com.oplus.view.material.* —— "液态材质"参数族（metaball / caustic 阴影）

- **位置**：oplus-fw-dex-probe.apk，6 个类：`OplusMaterialBaseParams / CornerParams / EdgeParams / MetaBallParams / ShadowParams / Util`
- **已深挖**：`OplusMaterialMetaBallParams`（inspect 全量）——元球变形参数：`MODE_X / MODE_Y`（两个拉伸方向）、`mExtension`（拉伸量 px）、`mExtensionFactorX/Y`（拉伸系数）、`mIndex`（链中索引，`MAX_INDEX/MIN_INDEX`）、构造 `(int,int,int,float,float)`；`toString` 表明它会被序列化下传 native（HWUI）。`OplusMaterialCornerParams`：`CORNER_TYPE_NEW_G2` 常量（G2 连续曲率圆角，即"新曲率"方案）+ `mRadius/mWeight` 二参数。
- **OplusMaterialUtil**（inspect）API 面：`setBaseParams / setCornerParams / setEdgeParams / setShadowParams / setMetaBallParams / setMetaBallChain(×3 重载) / setMetaBallChainByRenderNode / setOutlineCausticShadowParams / setOutlineCausticShadowLayout / setOutlineCausticShadowColor`；约束常量 `MAX_CHAIN_ELEMENT_COUNT / MIN_CHAIN_ELEMENT_COUNT / MAX_OBSERVER_ELEMENT_COUNT`。
- **解读**：这是 Oplus 的"液态玻璃/流体圆角"官方实现——多个视图组成 metaball 链（相邻卡片在拖拽时互相融合拉伸），圆角用 G2 曲率权重，阴影是"焦散(caustic)阴影"。coui-core 已有 `CouiFloatingMaterial`（一个材质组件）和 `CouiSquircle`，但**没有 metaball 链式融合、没有 G2 权重、没有 caustic 阴影**。
- **移植要点**：Compose 侧可用 `RenderEffect`/AGSL shader 实现 metaball（coui-core 已有 `CouiSpotLightShader` 先例）；`extensionFactor` 的融合曲线与 caustic 阴影具体算法在 native（libhwui.so），Java 层只有参数门面——**参数语义可移植，算法需自研 shader**。默认值（`DEFAULT_EXTENSION` 等）在 clinit，未确认。

### 3.4 com.oplus.view.OplusScrollBarEffect —— 悬浮滚动条物理

- inspect 全量 + `getDefaultValue` 反编译。构造 `(Context, IOplusScrollBarEffect.ViewCallback)`，被 device-framework 的 `IScrollBarDrawableExt` 注入 AOSP `ScrollBarDrawable`。
- 三个官方尺寸（`getDefaultValue(Resources)` 源码原文）：`mPadding = dp2px(density, 2)`、`mMinHeightOverScroll = dp2px(density, 4)`（overscroll 态最小 thumb 高 4dp）、`mMinHeightNormal = dp2px(density, 48)`（常态最小 48dp）。
- 行为方法：`getThumbLength(viewH, contentH, viewW, contentW)`、`getOverScrollLength`、`onOverScrolled`（overscroll 时 thumb 变短变粗）、`onTouchEvent`（按住时 `mIsTouchPressed`，thumb 展宽）、`getDrawRect/getOffsetX`。
- **对照**：coui-core 的 `CouiScrollBar` 是组件；这个是**尺寸/状态规则**——正常态 48dp 下限、overscroll 4dp、按下反馈，可直接作为 CouiScrollBar 的默认 token。值得移植（纯常量+几何，无 native 依赖）。

### 3.5 com.oplus.widget.popupwindow.OplusPopupListWindow —— CouiPopupListWindow 的 framework 孪生

- inspect 全量：`mListView/mListContainer/mBottomPanel/mAnchorRect/mPivotX/mPivotY/mPopupListMaxWidth/Height`、`mAlphaAnimationDuration/mAlphaAnimationInterpolator/mScaleAnimationDuration/mScaleAnimationInterpolator`、方法 `animateEnter/animateExit/calculatePivot/calculateWindowLocation/setListPortraitMaxHeight/setListLandscapeMaxHeight/addBottomPanel/setSelection`。
- **对照**：coui-core 已有 `CouiPopupListWindow` 全套（含 DefaultAnim/LocateHelper 等），本类是它的 framework 前身：入口动画 = alpha + scale（pivot 定在锚点侧），横竖屏分别限高。**不算新组件**，但验证了 coui-core 的实现路径与官方一致；`addBottomPanel`（列表下方加附加面板）是 coui-core 没有的小功能，可选吸收。

### 3.6 其他值得登记的新发现（清单级，未逐方法深挖）

| 类/包 | probe | 是什么 | 移植价值 |
|---|---|---|---|
| `com.oplus.widget.OplusHollowTextView` | oplus-fw | 空心/发光文字：`ARRT_IS_HOLLOW`（stroke 描边绘制）+ `ARRT_IS_GLOW`（`initGlow` 发光笔刷）双 attr | 中：Compose 里 `TextStyle(drawStyle=Stroke)` 即可，token 参考 |
| `com.oplus.widget.OplusViewPager` + `indicator.OplusPageIndicator/IPagerIndicator` | oplus-fw | framework 版 ViewPager：`MAX_SETTLE_DURATION/MIN_FLING_VELOCITY/DEFAULT_GUTTER_SIZE` 常量齐、`bindSplitMenuCallback`（与分屏菜单联动） | 中：KMP 用 Pager 即可，indicator 的槽位样式可参考 |
| `com.oplus.graphics.OplusBlurManager` | oplus-fw | SurfaceControl/View 背景模糊门面：`setBackgroundBlurRadius(Transaction, SurfaceControl, int, OplusBlurParam)` 3 重载 | 高（API 语义）：Compose 有 `Modifier.blur`，但"背景模糊+参数对象"对应 `BlurEffect` 用法参考 |
| `com.oplus.rtscroll.*`（39 类：RtScrollHelper/RtOverScroller/ScrollOptHelper/orientation 偏移助手） | oplus-fw | 实时（RenderThread）滚动框架：滚动计算离主线程，含横竖向 offset helper 与回调适配 | 高：对应 Compose 的 `Modifier.graphicsLayer` 滚动优化思路，架构可借鉴 |
| `com.oplus.view.envlight.EnvLightUtil/EnvLightSourceParams/EnvLightReceiverParams` | oplus-fw | 环境光感知（光照方向/强度参数注入渲染） | 低-中：coui-core 的 CouiSpotLight 可接收此类参数 |
| `com.oplus.view.viewdrag.*`（20+ 类：OplusViewDragManager/OplusImgDragShadowBuilder/OplusTextDragShadowBuilder/ComposeViewDragProcessor） | oplus-fw | 系统级跨应用拖拽（含 Compose View 的 drag processor、拖拽影子徽章 DragShadowBadgeManager） | 中：桌面级拖拽 UX 参考 |
| `com.oplus.view.OplusRenderNodeAnimator/OplusRenderNodeAnimatorSet/OplusRenderValueAnimator`、`android.view.OplusSurfaceAnimator` | oplus-fw | RenderNode 属性动画（绕过 View 重绘直接动画渲染属性） | 高（性能模式）：对应 Compose 的 `Modifier.graphicsLayer` 动画 |
| `com.oplus.flexiblewindow.*`（24 类 FlexibleTaskView/FlexibleWindowManager） | oplus-fw | 平板自由窗口/应用内嵌任务 | 低：windowing 层，非组件库 |
| `com.oplus.zoomwindow.*`（18 类） | oplus-fw | 缩放窗口（悬浮迷你窗）管理 | 低 |
| `com.oplus.screenshot.*`（60+ 类 OplusLongshot*/OplusScrollCapture*） | oplus-fw | 长截图/滚动截屏（ListView/RecyclerView/WebView 各自 capture helper） | 低：非组件，但 `IOplusScrollable` 接口契约可参考 |
| `android.app.dialog.OplusAlertDialogBuilder/OplusAlertDialogButtonBarLayout` | oplus-fw | framework 对话框的 oplus 版（按钮栏布局类） | 低：coui Preference/Dialog 已覆盖 |
| `android.view.springscroll.OplusScrollManager` + `IOplusScrollManager` | oplus-fw | springscroll 的 View 级管理入口 | 与 3.2 同族 |
| `com.oplus.animation.SpringAnimation/SpringForce/DynamicAnimation` | oplus-fw | androidx DynamicAnimation 的私有复制（SpringForce 字段名与 androidx 完全一致：STIFFNESS_HIGH/MEDIUM/LOW/VERY_LOW，DAMPING_RATIO_NO/MEDIUM/LOW/HIGH_BOUNCY；数值在 clinit 未确认，androidx 公开值分别为 10000/1500/200/50 与 1/0.75/0.5/0.25） | 低：与 CouiSpring 重复 |
| `com.oplus.util.OplusHapticFeedbackConstants`（oplus-fw）+ `android.os.vibrator.OplusPrebakedSegment/OplusVibrationEffectSegment`（device-fw） | 两处 | Oplus 自定义震动效果段（相比 AOSP 扩展预置波形） | 中：CouiHaptics 的效果枚举可扩充 |
| `android.graphics.OplusPalette / OplusSmartCutQuantizer / OplusBitmapColorEstimator` | 两处 | 图像主色/取色（Palette 增强版） | 中：动态取色主题（Wallpaper tint）参考 |

---

## ④ device-framework-probe / services 侧：值得记一笔的注入模式

- **Ext/Soc-Ext 注入模式**：AOSP 类与 Oplus 实现解耦的通用手法——`IAbsListViewExt/IEditorExt/IPopupWindowExt/IOplusScrollerExt/IOverScrollerWrapper` + `*ExtImpl/*SocExtImpl`。`android.common.OplusFeatureList/OplusFeatureManager/OplusFrameworkFactory` 是特性开关总线。意味着：**在 framework.probe 里看到的 Oplus 行为 = 真机上 AOSP 控件实际行为**，比看原版 AOSP 源码更接近 ColorOS 真实手感。
- **文本编辑**：`OplusEditorCursorAnimationController / OplusEditorInsertionHandleDrawable / OplusEditorSelectionHandleAnimationController / OplusMagnifierHooksImpl / OplusFloatingToolbarUtil`——光标手柄动画、放大镜、文本浮动工具条的官方定制点。
- **services 侧**（oplus-services-dex-probe）：`com.android.server.wm` 891 类中 471 个 Oplus 类（flexible/floating/bracket/darkmode mask 等窗口形态管理，属 windowing，非组件库）；`statusbar` 侧 `OplusStatusBarManagerServiceExtImpl`。services-dex-probe 本体基本是 AOSP + kotlin 标准库，UI 组件价值低。

---

## ⑤ 汇总表

| 组件/能力 | APK | 优先级（对 coui-core） |
|---|---|---|
| com.coui.animatekit（DSL+曲线+状态+隐式动画+SurfaceControl target） | oplus-services-dex-probe | ★★★ |
| OplusSpringOverScroller + SpringScrollHelper + COUISpringInterpolator（滚动三段手感） | oplus-fw-dex-probe | ★★★ |
| OplusMaterial*（metaball 链/G2 圆角/caustic 阴影参数族） | oplus-fw-dex-probe | ★★★ |
| rtscroll（RenderThread 滚动框架） | oplus-fw-dex-probe | ★★ |
| OplusScrollBarEffect（滚动条尺寸/状态规则：2/4/48dp） | oplus-fw-dex-probe | ★★ |
| OplusRenderNodeAnimator 族（渲染属性动画） | oplus-fw-dex-probe | ★★ |
| OplusBlurManager（背景模糊门面） | oplus-fw-dex-probe | ★★ |
| OplusHollowTextView（空心/发光文字） | oplus-fw-dex-probe | ★ |
| OplusViewPager + PageIndicator | oplus-fw-dex-probe | ★ |
| viewdrag 拖拽族 / EnvLight / 取色族 / 震动段 | oplus-fw / device-fw | ★ |
| OplusPopupListWindow（framework 孪生，对照用） | oplus-fw-dex-probe | 对照 |
| hwui-probe 的 libhwui.so（材质算法真正的家） | hwui-probe | native，后续单独立项 |

**新发现组件计数（coui-core 无对应）**：animatekit 框架算 4 个子能力（DSL/曲线/物理弹簧解析解/隐式动画）+ 滚动物理 1 + 材质族 1 + rtscroll 1 + ScrollBarEffect 1 + RenderNodeAnimator 1 + Blur 1 + HollowText 1 + ViewPager/Indicator 1 + EnvLight 1 = **12**。

## ⑥ 未解决问题

1. **`<clinit>` 不可反编译**：REA `inspect-android-method` 对 `<clinit>` 一律报 "Method ... was not found"（已试 `<clinit>`、overload 0、`clinit` 三种写法），因此 SpringForce 各 stiffness/damping 数值、ReboundOverScroller 的摩擦/速度阈值常量、COUISpringInterpolator 的 DEFAULT_* 数值均为**未确认**。替代路径：真机 `dumpsys` 或 RUS 配置 dump。
2. hwui-probe 无 dex，libhwui.so（17MB native）未做任何 native 逆向——metaball/caustic/模糊的实际算法不可见。
3. oplus-framework.jar 本体按任务要求跳过（以 oplus-fw-dex-probe 代替，类清单一致已验证）。
4. device-framework-probe 与 framework-dex-probe、oplusfw-probe 与 oplus-fw-dex-probe 的 manifest/资源差异未核对（类清单完全一致，预期 dex 相同）。
5. services-dex-probe 中 `com.android.server` 类名扁平（6071 类都在 `com.android.server` 顶层前缀下，子包统计无区分度），其 UI 相关性按类名抽样判断，未逐一核实。
