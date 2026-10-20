// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.uitest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import component.CouiNavBarProbe

/**
 * The bar in the official's **second** presentation: `COUIBottomFloatingNavigationBarItem
 * .isLargeIcon() == true`, which the framework maps onto `setItemStyle(2)`
 * (`ITEM_STYLE_NAVI_LARGE_ICON`).
 *
 * Differences from the default vertical style, all from
 * `COUIBottomFloatingToolbarItemView.applyNaviLargeIconStyle` / `layoutNaviLargeIcon`:
 *
 * - the item lays out only a 10dp, gravity-centre title (the icon view is set `INVISIBLE`);
 * - the selected tab's icon is `coui_bottom_floating_navigation_bar_large_icon_size` (40dp),
 *   laid out at `top = -coui_bottom_floating_navigation_bar_large_icon_overflow` (-14dp), so it
 *   rides above the capsule's top edge (the item is `setClipChildren(false)`);
 * - the 24dp size, the 4dp top margin and the 2dp icon/title gap belong to the *vertical* style
 *   only, so they do not apply here.
 *
 * ```
 * adb shell am start -n top.yukonga.miuix.uitest/.CouiNavBarLargeIconProbeActivity
 * ```
 */
class CouiNavBarLargeIconProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CouiNavBarProbe(largeIconItems = true) }
    }
}