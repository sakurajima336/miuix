// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
package component
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp as iconDp
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBar
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarDefaults
import io.github.wfc35286.coui.kmp.basic.CouiBottomFloatingNavigationBarItem
import io.github.wfc35286.coui.kmp.basic.CouiRedDotType
import io.github.wfc35286.coui.kmp.basic.CouiText
import io.github.wfc35286.coui.kmp.graphics.couiBackdrop
import io.github.wfc35286.coui.kmp.graphics.rememberCouiBackdrop
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiTypography


/** A neutral placeholder glyph (rounded square), so the probe does not depend on the gallery icons. */
private val CouiProbeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "probe",
        defaultWidth = 24.iconDp,
        defaultHeight = 24.iconDp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }.build()
}

/**
 * Host for the floating navigation bar on a **patterned dark page**.
 *
 * The pattern is the point: on a black page the blur has nothing to smear, so a backdrop that is
 * pinned to the pill and one that stays on the page look identical. A row of high-contrast colour
 * bars and thin white rules directly behind the pill makes that difference obvious in a
 * screenshot - and it is also what shows whether the blur is isotropic (the platform pass applied
 * once per axis) or smeared along the 45-degree sampling diagonal.
 */
@Composable
fun CouiNavBarProbe(largeIconItems: Boolean = false) {
    var index by remember { mutableIntStateOf(0) }
    val icon = rememberVectorPainter(CouiProbeIcon)
    CouiTheme {
        val backdrop = rememberCouiBackdrop()
        val items =
            listOf("首页", "游戏", "软件", "榜单", "我的").mapIndexed { i, title ->
                CouiBottomFloatingNavigationBarItem(
                    id = i,
                    title = title,
                    icon = icon,
                    iconTint = CouiBottomFloatingNavigationBarDefaults.titleColor(i == index),
                    redDotType = if (i == 4) CouiRedDotType.Number else CouiRedDotType.None,
                    redDotNumber = 15,
                    // `isLargeIcon()` - the second presentation the official item supports
                    // (`setItemStyle(2)`): a 10dp centred label with a 40dp icon raised 14dp above
                    // the capsule, shown for the selected tab only.
                    largeIcon = largeIconItems,
                )
            }
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
            // The captured page is a sibling, never a parent, of the pill.
            // Capture before the background: `drawContent()` only sees inner nodes, and the
            // material passes the sample's alpha straight through.
            Box(modifier = Modifier.fillMaxSize().couiBackdrop(backdrop).background(Color(0xFF1A1A1A))) {
                BackdropPattern()
            }
            CouiBottomFloatingNavigationBar(
                backdrop = backdrop,
                items = items,
                selectedIndex = index,
                onSelected = { index = it },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp),
            )
        }
    }
}

/**
 * High-contrast content the pill sits on top of: colour bars, thin white rules and text lines, all
 * far smaller than the blur kernel, so any directional smear reads as a streak instead of a wash.
 */
@Composable
private fun BackdropPattern() {
    Box(modifier = Modifier.fillMaxSize()) {
        BackdropPatternBody()
        // **The ruler.** A mid-grey square at a *known page position* - `(180dp, 148dp)`, i.e. inside
        // the pill and in the gap between two items. Grey (not white) so the material's wash cannot
        // saturate it: a saturated core destroys the centroid, which is how an earlier measurement
        // of this came out 40px wrong. Its blurred centroid inside the pill is directly comparable
        // with that known position, so a constant offset in the sampling shows up as a shift.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val half = 20.dp.toPx()
                        drawRect(
                            color = Color(0xFF808080),
                            topLeft = Offset(180.dp.toPx() - half, 148.dp.toPx() - half),
                            size = Size(half * 2f, half * 2f),
                        )
                    },
        )
    }
}

@Composable
private fun BackdropPatternBody() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Dark, empty area behind the pill (the ruler dot is the only thing there), then the colour
        // bars below it. The earlier 2dp stripe band lived here; it was for the "edge blur dies"
        // measurement and only adds noise now.
        Spacer(Modifier.height(340.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val colors =
                listOf(
                    Color(0xFFE0453A),
                    Color(0xFFF2A93B),
                    Color(0xFF43C463),
                    Color(0xFF3B8AC4),
                    Color(0xFF8A54C4),
                    Color(0xFFE0453A),
                    Color(0xFFF2A93B),
                    Color(0xFF43C463),
                )
            colors.forEach { c ->
                Box(modifier = Modifier.width(40.dp).height(220.dp).background(c))
            }
        }
        for (i in 0 until 6) {
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE8E8E8)))
            Spacer(Modifier.height(10.dp))
            CouiText(
                text = "条目${i + 1}条目${i + 1}条目${i + 1}条目${i + 1}条目${i + 1}",
                style = CouiTypography.body,
                color = Color(0xFFEDEDED),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .size(120.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(12.dp)),
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            CouiText(
                text = "PATTERN",
                style = CouiTypography.listTitle,
                color = Color(0xFFFFD54F),
                textAlign = TextAlign.Center,
            )
        }
    }
}