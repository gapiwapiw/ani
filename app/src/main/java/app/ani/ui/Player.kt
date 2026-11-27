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
import androidx.compose.runtime.produceState
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
import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.ani.Graph
import app.ani.data.Cue
import app.ani.data.EpisodeRef
import app.ani.data.Loaded
import app.ani.data.Playback
import app.ani.data.SeasonRef
import app.ani.data.ShowGroup
import app.ani.data.Subs
import kotlinx.coroutines.delay

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

private val SPEEDS = listOf("0.75×", "1×", "1.25×", "1.5×", "2×")
private val SPEED_VALUES = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

private fun fm(ms: Long): String {
    val x = (ms / 1000).toInt().coerceAtLeast(0)
    return if (x >= 3600) "${x / 3600}:${(x % 3600 / 60).toString().padStart(2, '0')}:${(x % 60).toString().padStart(2, '0')}"
    else "${x / 60}:${(x % 60).toString().padStart(2, '0')}"
}

private sealed interface Load {
    data object Busy : Load
    data class Ok(val l: Loaded) : Load
    data class Fail(val msg: String) : Load
}

@OptIn(UnstableApi::class)
private fun buildPlayer(ctx: Context, l: Loaded, startMs: Long, speed: Float): ExoPlayer {
    val p = ExoPlayer.Builder(ctx)
        .setMediaSourceFactory(DefaultMediaSourceFactory(Playback.dataSourceFactory(l.info.headers)))
        .build()
    p.setMediaItem(MediaItem.fromUri(l.info.m3u8))
    p.setPlaybackSpeed(speed)
    p.prepare()
    if (startMs > 0) p.seekTo(startMs)
    p.playWhenReady = true
    return p
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(g: ShowGroup, season: SeasonRef, startEp: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val st = Graph.store
    val prefs = st.prefs
    val mode = prefs.mode
    var ep by remember { mutableStateOf(startEp) }
    var attempt by remember { mutableIntStateOf(0) }
    var ui by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var fill by remember { mutableStateOf(false) }
    var quality by remember { mutableIntStateOf(0) }
    var subIdx by remember { mutableIntStateOf(-1) }
    var speed by remember { mutableIntStateOf(SPEED_VALUES.indexOf(prefs.speed).coerceAtLeast(1)) }
    var playing by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var playErr by remember { mutableStateOf<String?>(null) }
    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }
    var cues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var subText by remember { mutableStateOf("") }

    val epList by produceState<List<EpisodeRef>>(emptyList(), season.id, mode) {
        value = runCatching { Graph.repo.episodes(season.id, mode) }.getOrDefault(emptyList())
    }

    var load by remember { mutableStateOf<Load>(Load.Busy) }
    LaunchedEffect(ep, mode, attempt) {
        load = Load.Busy
        playErr = null
        load = try {
            Load.Ok(Playback.load(g, season, ep, mode))
        } catch (e: Exception) {
            Load.Fail(e.message ?: "Could not load this episode")
        }
    }
    val loaded = (load as? Load.Ok)?.l

    val player = remember(loaded) {
        loaded?.let {
            val w = st.hist[season.id]?.w?.get(ep)
            val start = if (w != null && w.second > 0 && w.first / w.second < 0.92 && w.first > 5) (w.first * 1000).toLong() else 0L
            buildPlayer(ctx, it, start, SPEED_VALUES[speed])
        }
    }

    fun saveProgress() {
        val p = player ?: return
        val d = p.duration
        if (d > 0) st.progress(g, season.id, ep, p.currentPosition / 1000.0, d / 1000.0)
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val l = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { playing = playWhenReady }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    saveProgress()
                    val i = epList.indexOfFirst { it.number == ep }
                    if (i >= 0 && i + 1 < epList.size) ep = epList[i + 1].number
                }
            }
            override fun onPlayerError(error: PlaybackException) { playErr = error.message ?: "Playback error" }
        }
        player.addListener(l)
        onDispose {
            saveProgress()
            player.removeListener(l)
            player.release()
        }
    }

    // Position polling, progress saving and subtitle lookup.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        var n = 0
        while (true) {
            posMs = p.currentPosition
            durMs = p.duration.coerceAtLeast(0L)
            val t = posMs
            val txt = cues.filter { t >= it.s && t < it.e }.joinToString("\n") { it.text }
            if (txt != subText) subText = txt
            if (++n % 40 == 0) saveProgress()
            delay(250)
        }
    }

    // Subtitles: pick the preferred language when a source loads, then read the chosen file.
    LaunchedEffect(loaded) {
        val l = loaded ?: return@LaunchedEffect
        subIdx = if (prefs.subLang == "Off") -1 else {
            val want = prefs.subLang.lowercase()
            val i = l.tracks.indexOfFirst { it.label.lowercase().contains(want) || it.lang.lowercase().contains(want) }
            if (i >= 0) i else if (l.tracks.isNotEmpty()) 0 else -1
        }
        quality = 0
    }
    LaunchedEffect(loaded, subIdx) {
        val l = loaded ?: return@LaunchedEffect
        cues = emptyList()
        subText = ""
        if (subIdx in l.tracks.indices) cues = Subs.load(l.tracks[subIdx].url, l.info.referer).orEmpty()
    }
    LaunchedEffect(player, quality, loaded) {
        val p = player ?: return@LaunchedEffect
        val h = loaded?.info?.qualities?.getOrNull(quality - 1)?.height
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, if (quality > 0 && h != null && h > 0) h else Int.MAX_VALUE)
            .build()
    }
    LaunchedEffect(player, speed) { player?.setPlaybackSpeed(SPEED_VALUES[speed]) }

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
    val subSize = prefs.subSize
    val title = g.name

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        useController = false
                        subtitleView?.visibility = View.GONE
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = {
                    it.player = player
                    it.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().pointerInput(player) {
                detectTapGestures(
                    onDoubleTap = { o ->
                        val p = player ?: return@detectTapGestures
                        val d = p.duration.coerceAtLeast(0L)
                        val to = if (o.x < size.width / 2) p.currentPosition - 10_000 else p.currentPosition + 10_000
                        p.seekTo(to.coerceIn(0L, if (d > 0) d else Long.MAX_VALUE))
                        tick++
                    },
                    onTap = { ui = !ui; tick++ },
                )
            },
        )
        if (subText.isNotEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = subBottom), contentAlignment = Alignment.BottomCenter) {
                BasicText(
                    subText,
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
        val status = when {
            load is Load.Fail -> (load as Load.Fail).msg
            playErr != null -> playErr
            else -> null
        }
        if (status != null) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AText("Couldn’t play this episode", size = 16, weight = 500)
                AText(status, color = C.Mut, align = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Try again", { attempt++ }, ghost = true)
                    Btn("Close", onClose, ghost = true)
                }
            }
        } else if (load is Load.Busy || buffering) {
            AText("Loading…", Modifier.align(Alignment.Center), color = C.Tx.copy(alpha = 0.7f))
        }
        AnimatedVisibility(ui || status != null, enter = fadeIn(motion(Motion.D3)), exit = fadeOut(motion(Motion.D3))) {
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
                        AText(title, Modifier.weight(1f, fill = false), size = 16, weight = 500, maxLines = 1)
                        AText("Episode $ep", Modifier.padding(start = 8.dp), color = C.Tx.copy(alpha = 0.7f), maxLines = 1)
                    }
                    IconBtn(AniIcons.Tune, "Options", { menu = true; tick++ })
                }
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerBtn(AniIcons.Rewind, "Back 10 seconds", 36.dp) {
                        player?.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L)); tick++
                    }
                    PlayerBtn(
                        if (playing) AniIcons.PauseFilled else AniIcons.PlayFilled,
                        "Play or pause", 56.dp,
                    ) { player?.let { it.playWhenReady = !it.playWhenReady }; tick++ }
                    PlayerBtn(AniIcons.Forward, "Forward 10 seconds", 36.dp) {
                        player?.let { p ->
                            val d = p.duration.coerceAtLeast(0L)
                            p.seekTo((p.currentPosition + 10_000).coerceIn(0L, if (d > 0) d else Long.MAX_VALUE))
                        }
                        tick++
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText(fm(posMs), Modifier.widthIn(min = 44.dp), align = TextAlign.Center)
                    AniSlider(
                        if (durMs > 0) (posMs / durMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        { f -> if (durMs > 0) { player?.seekTo((f * durMs).toLong()); posMs = (f * durMs).toLong() }; tick++ },
                        Modifier.weight(1f),
                    )
                    AText(fm(durMs), Modifier.widthIn(min = 44.dp), align = TextAlign.Center)
                    IconBtn(AniIcons.Expand, "Fill screen", { fill = !fill; tick++ })
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
                    val qs = listOf("Auto") + (loaded?.info?.qualities?.map { it.label } ?: emptyList())
                    MenuGroup("Quality", qs, quality) { quality = it }
                    val ss = listOf("Off") + (loaded?.tracks?.map { it.label.ifBlank { it.lang } } ?: emptyList())
                    MenuGroup("Subtitles", ss, subIdx + 1) { subIdx = it - 1 }
                    MenuGroup("Speed", SPEEDS, speed) { speed = it }
                }
            }
        }
    }
}
