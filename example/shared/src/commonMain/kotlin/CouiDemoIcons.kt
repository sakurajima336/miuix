// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0


/** Neutral demo artwork. It carries no Settings application meaning or component specification. */
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
internal object CouiDemoIcons {
    val Placeholder: ImageVector = ImageVector.Builder(name = "Placeholder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        addPath(
            pathData = PathParser().parsePathString("M4,4H20V20H4Z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
        )
    }.build()

    // Existing COUI chevron affordance; content-independent, unlike business icons.
    val Chevron: ImageVector = ImageVector.Builder(name = "Chevron", defaultWidth = 12.dp, defaultHeight = 24.dp, viewportWidth = 12f, viewportHeight = 24f, autoMirror = true).apply {
        addPath(
            pathData = PathParser().parsePathString("M5,6L10.646,11.646C10.842,11.842 10.842,12.158 10.646,12.354L5,18").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()
}
