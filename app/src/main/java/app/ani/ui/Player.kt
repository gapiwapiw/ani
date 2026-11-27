package app.ani.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.ani.data.Show
import kotlinx.coroutines.delay

private const val DUR = 1450
private val SPEEDS = listOf("0.75×", "1×", "1.25×", "1.5×")
private val SPEED_VALUES = listOf(0.75f, 1f, 1.25f, 1.5f)

private fun fm(x: Int): String = "${x / 60}:${(x % 60).toString().padStart(2, '0')}"

@Composable
private fun PlayerBtn(icon: ImageVector, desc: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).tap(onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { AIcon(icon, C.Tx, size) }
}

@Composable
private fun MenuGroup(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    AText(title, Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), size = 12, weight = 500, color = C.Mut)
    options.forEachIndexed { i, o ->
        Row(
            Modifier.fillMaxWidth().clip(Shp.R2).tap { onSelect(i) }.padding(12.dp),
        ) { AText(o, Modifier.weight(1f), color = if (i == selected) C.Ac2 else C.Tx) }
    }
}

/** Static player UI. Playback is simulated with a timer until Media3 is wired in. */
@Composable
fun PlayerScreen(s: Show, subSize: Int, onClose: () -> Unit) {
    var t by remember { mutableIntStateOf(580) }
    var playing by remember { mutableStateOf(true) }
    var ui by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var quality by remember { mutableIntStateOf(0) }
    var subLang by remember { mutableIntStateOf(1) }
    var speed by remember { mutableIntStateOf(1) }

    LaunchedEffect(playing, speed) {
        while (playing) {
            delay((1000 / SPEED_VALUES[speed]).toLong())
            t = minOf(DUR, t + 1)
        }
    }
    // Controls hide after a pause in interaction.
    LaunchedEffect(ui, playing, menu, tick) {
        if (ui && playing && !menu) {
            delay(3000)
            ui = false
        }
    }
    BackHandler(enabled = menu) { menu = false }

    // Immersive fullscreen and keep screen on while the player is open.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctl?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            ctl?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }

    val subBottom by animateDpAsState(if (ui) 96.dp else 32.dp, motion(Motion.D3), label = "sub")

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF24201E), Color(0xFF0C0B0B))))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { o ->
                            t = if (o.x < size.width / 2) maxOf(0, t - 10) else minOf(DUR, t + 10)
                            tick++
                        },
                        onTap = { ui = !ui; tick++ },
                    )
                },
        )
        if (subLang != 0) {
            Box(Modifier.fillMaxSize().padding(bottom = subBottom), contentAlignment = Alignment.BottomCenter) {
                BasicText(
                    "You didn't have to come all this way.",
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    style = TextStyle(
                        color = Color(0xFFF2EEE8),
                        fontSize = subSize.sp,
                        lineHeight = (subSize * 1.3f).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(Color(0xE6000000), Offset(0f, 2f), 4f),
                    ),
                )
            }
        }
        AnimatedVisibility(ui, enter = fadeIn(motion(Motion.D3)), exit = fadeOut(motion(Motion.D3))) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x99000000), 0.22f to Color.Transparent,
                        0.7f to Color.Transparent, 1f to Color(0xA6000000),
                    ),
                ),
            ) {
                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBtn(AniIcons.Back, "Close player", onClose)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        AText(s.title, Modifier.weight(1f, fill = false), size = 16, weight = 500, maxLines = 1)
                        AText("Episode 6", Modifier.padding(start = 8.dp), color = C.Tx.copy(alpha = 0.7f), maxLines = 1)
                    }
                    IconBtn(AniIcons.Tune, "Options", { menu = true; tick++ })
                }
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerBtn(AniIcons.Rewind, "Back 10 seconds", 36.dp) { t = maxOf(0, t - 10); tick++ }
                    PlayerBtn(
                        if (playing) AniIcons.PauseFilled else AniIcons.PlayFilled,
                        "Play or pause", 56.dp,
                    ) { playing = !playing; tick++ }
                    PlayerBtn(AniIcons.Forward, "Forward 10 seconds", 36.dp) { t = minOf(DUR, t + 10); tick++ }
                }
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText(fm(t), Modifier.widthIn(min = 44.dp), align = TextAlign.Center)
                    AniSlider(t / DUR.toFloat(), { t = (it * DUR).toInt(); tick++ }, Modifier.weight(1f))
                    AText(fm(DUR), Modifier.widthIn(min = 44.dp), align = TextAlign.Center)
                    IconBtn(AniIcons.Expand, "Fill screen", { tick++ })
                }
            }
        }
        AnimatedVisibility(menu, enter = fadeIn(motion(Motion.D2)), exit = fadeOut(motion(Motion.D2))) {
            Box(Modifier.fillMaxSize().plainClick { menu = false }) {
                Column(
                    Modifier.align(Alignment.CenterEnd)
                        .animateEnterExit(
                            enter = slideInHorizontally(motion(Motion.D3)) { it / 16 },
                            exit = slideOutHorizontally(motion(Motion.D3)) { it / 16 },
                        )
                        .padding(8.dp).fillMaxHeight()
                        .fillMaxWidth(0.82f).widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(16.dp)).background(C.S1).hairBorder(RoundedCornerShape(16.dp))
                        .plainClick { }
                        .verticalScroll(rememberScrollState()).padding(8.dp),
                ) {
                    MenuGroup("Quality", listOf("Auto", "1080p", "720p"), quality) { quality = it }
                    MenuGroup("Subtitles", listOf("Off", "English", "Arabic"), subLang) { subLang = it }
                    MenuGroup("Speed", SPEEDS, speed) { speed = it }
                }
            }
        }
    }
}
