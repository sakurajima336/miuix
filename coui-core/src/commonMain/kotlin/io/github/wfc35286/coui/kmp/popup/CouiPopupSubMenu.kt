// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.wfc35286.coui.kmp.popup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.channels.Channel


internal data class CouiPopupSubMenuPlacement(
    val offset: IntOffset,
    val mainScale: Float,
    val mainPivotX: Float,
    val mainTranslationY: Float,
)

internal fun couiPopupSubMenuPlacement(
    locate: CouiPopupLocateHelper,
    main: Rect,
    anchorCenterX: Int,
    rowTop: Float,
    subSize: IntSize,
    available: IntRect,
    compact: Boolean,
    rtl: Boolean,
    overlap: Int,
    minGap: Int,
    anchorRowHeight: Float = 0f,
    innerPaddingPx: Int = 0,
): CouiPopupSubMenuPlacement {
    val helper = locate
    helper.smallScreen = compact
    helper.isRtl = rtl
    helper.horizontalOverlap = overlap
    helper.verticalOverlap = overlap
    helper.minGapBetweenMainAndSubMenu = minGap
    helper.menuInnerPaddingVertical = innerPaddingPx
    val d = helper.domain
    d.window.set(available.left, available.top, available.right, available.bottom)
    d.mainMenu.set(main.left.toInt(), main.top.toInt(), main.right.toInt(), main.bottom.toInt())
    d.anchor.set(anchorCenterX - 1, main.top.toInt(), anchorCenterX + 1, main.bottom.toInt())

    helper.setAnchorRow(rowTop.toInt(), anchorRowHeight.toInt().coerceAtLeast(0), scrollY = 0)
    d.subMenuAnchorIsFirstItem = compact

    val subWidth = min(subSize.width, abs(available.width))
    val availableHeight = abs(available.height)
    val subHeight = min(subSize.height, if (compact) availableHeight - minGap else availableHeight)

    d.subMenu.set(0, 0, subWidth, subHeight)
    helper.relocateMainMenu()

    val x = helper.subMenuOffsetX(subWidth)
    val y = helper.subMenuOffsetY(subHeight)
    d.subMenu.set(x, y, x + subWidth, y + subHeight)

    val relocated = d.mainMenuRelocated
    val mainScale = if (main.width > 0f) relocated.width().toFloat() / main.width else 0.9f
    val mainTranslation = if (compact) {
        val controller = CouiPopupSmallScreenController(helper)
        controller.minGapBetweenMainAndSubMenu = minGap
        controller.menuInnerPaddingVertical = innerPaddingPx
        controller.computeMainMenuTargetTranslationY()
    } else {
        0f
    }
    val pivot = when {
        !compact -> 0.5f
        main.left.toInt() == relocated.left -> 0f
        main.right.toInt() == relocated.right -> 1f
        else -> 0.5f
    }

    val clampedX = x.coerceIn(available.left, max(available.left, available.right - subWidth))
    val clampedY = y.coerceIn(available.top, max(available.top, available.bottom - subHeight))
    d.subMenu.set(clampedX, clampedY, clampedX + subWidth, clampedY + subHeight)
    return CouiPopupSubMenuPlacement(IntOffset(clampedX, clampedY), mainScale, pivot, mainTranslation)
}

internal fun couiPopupTransformedBounds(bounds: Rect, visual: CouiPopupVisualState): Rect {
    val center = bounds.center + visual.translation
    val halfWidth = bounds.width * visual.scaleX / 2f
    val halfHeight = bounds.height * visual.scaleY / 2f
    return Rect(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
}

internal class CouiPopupRevealShape(private val shape: Shape, private val bounds: Rect?) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val reveal = bounds ?: return shape.createOutline(size, layoutDirection, density)
        val outline = shape.createOutline(Size(reveal.width, reveal.height), layoutDirection, density)
        val path = Path().apply {
            when (outline) {
                is Outline.Generic -> addPath(outline.path)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Rectangle -> addRect(outline.rect)
            }
            translate(Offset(reveal.left, reveal.top))
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun DrivePopupTouchMotion(motion: CouiPopupTouchMotion) {
    val frames = remember { Channel<Unit>(Channel.CONFLATED) }
    DisposableEffect(motion) {
        motion.requestFrame = { frames.trySend(Unit) }
        onDispose { motion.requestFrame = {} }
    }
    LaunchedEffect(motion) {
        for (request in frames) {
            var last = withFrameNanos { it }
            do {
                var moving = false
                withFrameNanos { now ->
                    moving = motion.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            } while (moving)
        }
    }
}

@Composable
internal fun DrivePopupPanelMask(panel: CouiPopupPanelState) {
    val frames = remember { Channel<Unit>(Channel.CONFLATED) }
    DisposableEffect(panel) {
        panel.requestFrame = { frames.trySend(Unit) }
        onDispose { panel.requestFrame = {} }
    }
    LaunchedEffect(panel) {
        for (request in frames) {
            var last = withFrameNanos { it }
            do {
                var moving = false
                withFrameNanos { now ->
                    moving = panel.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            } while (moving)
        }
    }
}

@Composable
internal fun DrivePopupTransition(driver: CouiPopupTransitionDriver) {
    val frames = remember { Channel<Unit>(Channel.CONFLATED) }
    DisposableEffect(driver) {
        driver.requestFrame = { frames.trySend(Unit) }
        onDispose { driver.requestFrame = {} }
    }
    LaunchedEffect(driver) {
        for (request in frames) {
            var last = withFrameNanos { it }
            do {
                var moving = false
                withFrameNanos { now ->
                    moving = driver.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            } while (moving)
        }
    }
}
