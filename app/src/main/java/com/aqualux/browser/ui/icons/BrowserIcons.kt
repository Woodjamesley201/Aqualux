package com.aqualux.browser.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

object BrowserIcons {
    val Tabs: ImageVector = icon("Tabs") {
        moveTo(7f, 6f)
        horizontalLineToRelative(12f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        verticalLineToRelative(8f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
        horizontalLineTo(7f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        verticalLineTo(8f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
        close()
        moveTo(4f, 9f)
        horizontalLineTo(3.5f)
        arcTo(1.5f, 1.5f, 0f, false, false, 2f, 10.5f)
        verticalLineToRelative(7f)
        arcTo(1.5f, 1.5f, 0f, false, false, 3.5f, 19f)
        horizontalLineToRelative(12f)
        arcToRelative(1.5f, 1.5f, 0f, false, false, 1.5f, -1.5f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        arcToRelative(3f, 3f, 0f, false, true, -3f, -3f)
        close()
    }

    val Bookmark: ImageVector = icon("Bookmark") {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        lineTo(18f, 21f)
        lineTo(12f, 17f)
        lineTo(6f, 21f)
        close()
    }

    val Pin: ImageVector = icon("Pin") {
        moveTo(14f, 3f)
        lineTo(16f, 5f)
        lineTo(14.5f, 9.2f)
        lineTo(18f, 12f)
        verticalLineToRelative(1.2f)
        horizontalLineTo(13f)
        verticalLineTo(21f)
        lineTo(12f, 22f)
        lineTo(11f, 21f)
        verticalLineTo(13.2f)
        horizontalLineTo(6f)
        verticalLineTo(12f)
        lineTo(9.5f, 9.2f)
        lineTo(8f, 5f)
        lineTo(10f, 3f)
        close()
    }

    val Shield: ImageVector = icon("Shield") {
        moveTo(12f, 2f)
        lineTo(5f, 5f)
        verticalLineToRelative(6f)
        curveToRelative(0f, 4.4f, 2.9f, 8.4f, 7f, 9.5f)
        curveToRelative(4.1f, -1.1f, 7f, -5.1f, 7f, -9.5f)
        verticalLineTo(5f)
        close()
    }

    val Download: ImageVector = icon("Download") {
        moveTo(12f, 3f)
        verticalLineToRelative(10f)
        lineToRelative(3.2f, -3.2f)
        lineToRelative(1.4f, 1.4f)
        lineTo(12f, 17f)
        lineToRelative(-4.6f, -5.8f)
        lineToRelative(1.4f, -1.4f)
        lineTo(11f, 13f)
        verticalLineTo(3f)
        close()
        moveTo(5f, 19f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        close()
    }

    val Desktop: ImageVector = icon("Desktop") {
        moveTo(4f, 4f)
        horizontalLineToRelative(16f)
        arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
        verticalLineToRelative(10f)
        arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
        horizontalLineTo(13f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(2f)
        horizontalLineTo(8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(-2f)
        horizontalLineTo(4f)
        arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
        verticalLineTo(5f)
        arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
        close()
    }

    val LockOpen: ImageVector = icon("LockOpen") {
        moveTo(8f, 10f)
        verticalLineTo(7.5f)
        arcToRelative(4f, 4f, 0f, false, true, 7.8f, -1.2f)
        lineToRelative(-1.7f, 0.8f)
        arcTo(2.2f, 2.2f, 0f, false, false, 10f, 7.5f)
        verticalLineTo(10f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(10f)
        horizontalLineTo(6f)
        verticalLineTo(10f)
        close()
    }
}

private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector {
    val nodes = PathBuilder().apply(block).nodes
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(fill = SolidColor(Color.Black), pathData = nodes)
    }.build()
}
