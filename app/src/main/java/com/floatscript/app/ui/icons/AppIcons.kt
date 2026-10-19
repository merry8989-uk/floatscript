package com.floatscript.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of icons FloatScript needs that are not part of the small
 * `material-icons-core` set. Keeping them here avoids pulling in the very large
 * `material-icons-extended` dependency (which adds ~35 MB to the build for a
 * dozen glyphs).
 */
object AppIcons {

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(pathData = addPathNodes("M6,5h4v14h-4z"), fill = SolidColor(Color.White))
            addPath(pathData = addPathNodes("M14,5h4v14h-4z"), fill = SolidColor(Color.White))
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(pathData = addPathNodes("M6,6h12v12h-12z"), fill = SolidColor(Color.White))
        }.build()
    }

    val Minus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(pathData = addPathNodes("M5,11h14v2h-14z"), fill = SolidColor(Color.White))
        }.build()
    }

    val DragHandle: ImageVector by lazy {
        ImageVector.Builder(
            name = "DragHandle",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(pathData = addPathNodes("M4,9h16v2h-16z"), fill = SolidColor(Color.White))
            addPath(pathData = addPathNodes("M4,13h16v2h-16z"), fill = SolidColor(Color.White))
        }.build()
    }

    val Videocam: ImageVector by lazy {
        ImageVector.Builder(
            name = "Videocam",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes("M4,7h10a2,2 0 0 1 2,2v6a2,2 0 0 1 -2,2h-10a2,2 0 0 1 -2,-2v-6a2,2 0 0 1 2,-2z"),
                fill = SolidColor(Color.White)
            )
            addPath(pathData = addPathNodes("M17,10l4,-2v8l-4,-2z"), fill = SolidColor(Color.White))
        }.build()
    }

    val RecordDot: ImageVector by lazy {
        ImageVector.Builder(
            name = "RecordDot",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes("M12,7a5,5 0 1 1 0,10a5,5 0 1 1 0,-10z"),
                fill = SolidColor(Color.White)
            )
        }.build()
    }
}
