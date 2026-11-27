package app.ani.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun icon(name: String, filled: Boolean, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    for (p in paths) {
        if (filled) {
            b.addPath(pathData = addPathNodes(p), fill = SolidColor(Color.Black))
        } else {
            b.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }
    return b.build()
}

/** 1.6 stroke line icons ported from the prototype's SVG symbols (circles/rects expanded to paths). */
object AniIcons {
    val Home = icon("home", false, "M3 10.5L12 3L21 10.5V20a1 1 0 0 1 -1 1H15V15H9V21H4a1 1 0 0 1 -1 -1Z")
    val Search = icon("search", false, "M3.5 11a7.5 7.5 0 1 0 15 0a7.5 7.5 0 1 0 -15 0", "M21 21L16.4 16.4")
    val Download = icon("download", false, "M12 4V15", "M7 11L12 16L17 11", "M5 20H19")
    val Sliders = icon(
        "sliders", false,
        "M4 7H14M18 7H20M4 17H6M10 17H20",
        "M13.8 7a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0",
        "M5.8 17a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0",
    )
    val Back = icon("back", false, "M19 12H5M12 19L5 12L12 5")
    val Heart = icon("heart", false, "M12 20S4.5 15.4 4.5 9.7A4.2 4.2 0 0 1 12 7A4.2 4.2 0 0 1 19.5 9.7C19.5 15.4 12 20 12 20Z")
    val HeartFilled = icon("heartFilled", true, "M12 20S4.5 15.4 4.5 9.7A4.2 4.2 0 0 1 12 7A4.2 4.2 0 0 1 19.5 9.7C19.5 15.4 12 20 12 20Z")
    val Plus = icon("plus", false, "M12 5V19M5 12H19")
    private const val PLAY = "M7 4.8V19.2A0.8 0.8 0 0 0 8.2 19.9L19.7 12.7A0.8 0.8 0 0 0 19.7 11.3L8.2 4.1A0.8 0.8 0 0 0 7 4.8Z"
    private const val PAUSE = "M7 4.5H9A1 1 0 0 1 10 5.5V18.5A1 1 0 0 1 9 19.5H7A1 1 0 0 1 6 18.5V5.5A1 1 0 0 1 7 4.5Z" +
        "M15 4.5H17A1 1 0 0 1 18 5.5V18.5A1 1 0 0 1 17 19.5H15A1 1 0 0 1 14 18.5V5.5A1 1 0 0 1 15 4.5Z"
    val Play = icon("play", false, PLAY)
    val PlayFilled = icon("playFilled", true, PLAY)
    val Pause = icon("pause", false, PAUSE)
    val PauseFilled = icon("pauseFilled", true, PAUSE)
    val Rewind = icon("rewind", false, "M4 12a8 8 0 1 0 2.6 -5.9L4 8.5", "M4 4V8.5H8.5")
    val Forward = icon("forward", false, "M20 12a8 8 0 1 1 -2.6 -5.9L20 8.5", "M20 4V8.5H15.5")
    val Check = icon("check", false, "M5 12.5L9.5 17L19 7.5")
    val Close = icon("close", false, "M6 6L18 18M18 6L6 18")
    val Tune = icon(
        "tune", false,
        "M4 8H13M19 8H20M4 16H5M11 16H20",
        "M13.6 8a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0",
        "M5.6 16a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0",
    )
    val Expand = icon("expand", false, "M4 9V4H9M20 9V4H15M4 15V20H9M20 15V20H15")
    val Film = icon(
        "film", false,
        "M5 4H19a2 2 0 0 1 2 2V18a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V6a2 2 0 0 1 2 -2Z",
        "M7 4V20M17 4V20M3 9H7M3 15H7M17 9H21M17 15H21",
    )
}

/** The wordmark from the prototype (calm palette), 160x100 viewport. */
val AniLogo: ImageVector = run {
    val b = ImageVector.Builder("ani", 160.dp, 100.dp, 160f, 100f)
    val strokes = listOf(
        "M12 62a20 20 0 1 0 40 0a20 20 0 1 0 -40 0",
        "M52 42V82",
        "M84 82V42",
        "M84 60a14 14 0 0 1 28 0V82",
        "M138 42V82",
    )
    for (p in strokes) {
        b.addPath(
            pathData = addPathNodes(p),
            stroke = SolidColor(C.Tx),
            strokeLineWidth = 12f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.addPath(
        pathData = addPathNodes("M26 52L43 62L26 72Z"),
        fill = SolidColor(C.Logo),
        stroke = SolidColor(C.Logo),
        strokeLineWidth = 4f,
        strokeLineJoin = StrokeJoin.Round,
    )
    b.addPath(
        pathData = addPathNodes("M129.5 21a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0"),
        fill = SolidColor(C.Logo),
    )
    b.build()
}
