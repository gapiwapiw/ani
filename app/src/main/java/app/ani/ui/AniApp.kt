package app.ani.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import app.ani.Graph
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import app.ani.data.SeasonRef
import app.ani.data.ShowGroup
import app.ani.data.toShow
import kotlinx.coroutines.delay

private val NAV: List<Pair<String, ImageVector>> = listOf(
    "Home" to AniIcons.Home,
    "Search" to AniIcons.Search,
    "Downloads" to AniIcons.Download,
    "Settings" to AniIcons.Sliders,
)

@Composable
private fun Header(tab: Int, q: String, onQ: (String) -> Unit, dub: Int, onDub: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    ) {
        when (tab) {
            0 -> Image(AniLogo, "ani", Modifier.size(width = 42.dp, height = 26.dp))
            1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchField(q, onQ, Modifier.weight(1f))
                Seg(listOf("Sub", "Dub"), dub, onDub)
            }
            2 -> AText("Downloads", size = 22, weight = 600, lh = 1.2f)
            else -> AText("Settings", size = 22, weight = 600, lh = 1.2f)
        }
    }
}

@Composable
private fun BottomNav(tab: Int, dlBadge: Int, onTab: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.Bg)
            .drawBehind { drawRect(C.Ln, Offset.Zero, Size(size.width, 1.dp.toPx())) }
            .navigationBarsPadding(),
    ) {
        NAV.forEachIndexed { i, (label, icon) ->
            val on = tab == i
            val col by animateColorAsState(if (on) C.Tx else C.Mut, motion(Motion.D2), label = "nav")
            Column(
                Modifier.weight(1f).tap { onTab(i) }
                    .drawBehind {
                        if (on) drawRect(C.Ac2, Offset(size.width / 2 - 12.dp.toPx(), 0f), Size(24.dp.toPx(), 2.dp.toPx()))
                    }
                    .padding(top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    AIcon(icon, col, 22.dp)
                    if (i == 2 && dlBadge > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-4).dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(RoundedCornerShape(8.dp)).background(C.S3).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) { AText("$dlBadge", size = 12, lh = 1.3f) }
                    }
                }
                AText(label, size = 12, color = col)
            }
        }
    }
}

private class PlayTarget(val g: ShowGroup, val season: SeasonRef, val ep: String)

@Composable
private fun rememberOnline(): Boolean {
    val ctx = LocalContext.current
    var online by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = true }
            override fun onLost(network: Network) { online = false }
        }
        try { cm.registerDefaultNetworkCallback(cb) } catch (e: Exception) { }
        onDispose { try { cm.unregisterNetworkCallback(cb) } catch (e: Exception) { } }
    }
    return online
}

@Composable
fun AniApp() {
    val st = Graph.store
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<PlayTarget?>(null) }
    var dlTarget by remember { mutableStateOf<PlayTarget?>(null) }
    var dlLoaded by remember { mutableStateOf<app.ani.data.Loaded?>(null) }
    var dlErr by remember { mutableStateOf<String?>(null) }
    var dlBusy by remember { mutableStateOf(false) }
    val dlItems by app.ani.data.Downloads.items.collectAsState()
    LaunchedEffect(dlTarget) {
        dlLoaded = null; dlErr = null; dlBusy = false
        val d = dlTarget ?: return@LaunchedEffect
        try {
            dlLoaded = app.ani.data.Playback.load(d.g, d.season, d.ep, st.prefs.mode)
        } catch (e: Exception) {
            dlErr = e.message ?: "Could not check this episode"
        }
    }
    val online = rememberOnline()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mode = st.prefs.mode

    var toastMsg by remember { mutableStateOf("") }
    var toastOn by remember { mutableStateOf(false) }
    var toastSeq by remember { mutableIntStateOf(0) }
    val toast: (String) -> Unit = { toastMsg = it; toastSeq++ }
    LaunchedEffect(toastSeq) {
        if (toastSeq > 0) {
            toastOn = true
            delay(1800)
            toastOn = false
        }
    }

    // Back closes the player, then the show page, then exits.
    BackHandler(enabled = dlTarget != null || target != null || openKey != null) {
        if (dlTarget != null) dlTarget = null else if (target != null) target = null else openKey = null
    }

    val shownGroup = openKey?.let { st.group(it) }
    val open = { g: ShowGroup -> st.touchRecent(g); openKey = g.key }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Header(tab, q, { q = it }, if (mode == "dub") 1 else 0) { st.updatePrefs(st.prefs.copy(mode = if (it == 1) "dub" else "sub")) }
            if (!online) {
                Box(Modifier.fillMaxWidth().background(C.S2).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AText("You are offline. Downloaded episodes still play.", size = 12, color = C.Mut)
                }
            }
            Box(Modifier.weight(1f)) {
                Crossfade(tab, animationSpec = motion(Motion.D3), label = "tab") { t ->
                    when (t) {
                        0 -> HomeScreen(open)
                        1 -> SearchScreen(q, mode, { q = it }, open)
                        2 -> DownloadsScreen { key -> st.group(key)?.let(open) }
                        else -> SettingsScreen(toast)
                    }
                }
            }
            BottomNav(tab, dlItems.count { it.active }) { tab = it }
        }

        AnimatedVisibility(
            shownGroup != null,
            enter = slideInHorizontally(motion(Motion.D3)) { it },
            exit = slideOutHorizontally(motion(Motion.D3)) { it },
        ) {
            // Keep the last group while the exit animation runs.
            val g = remember(shownGroup) { shownGroup }
            if (g != null) {
                ShowScreen(
                    g = g,
                    mode = mode,
                    onMode = { st.updatePrefs(st.prefs.copy(mode = it)) },
                    onBack = { openKey = null },
                    onWatch = { s, ep -> target = PlayTarget(g, s, ep) },
                    onDownload = { s, ep -> dlTarget = PlayTarget(g, s, ep) },
                    toast = toast,
                )
            }
        }

        DownloadSheet(
            visible = dlTarget != null,
            ep = dlTarget?.ep ?: "",
            subs = dlLoaded?.tracks,
            qualities = dlLoaded?.info?.qualities?.map { it.label }.orEmpty(),
            busy = dlBusy,
            error = dlErr,
            onClose = { dlTarget = null },
        ) { choice ->
            val d = dlTarget
            val l = dlLoaded
            if (d != null && l != null) {
                dlBusy = true
                scope.launch {
                    try {
                        app.ani.data.Downloads.enqueue(d.g, d.season, d.ep, st.prefs.mode, l, choice.subs, choice.quality)
                        toast("Download started")
                        dlTarget = null
                    } catch (e: Exception) {
                        dlErr = e.message ?: "Could not start the download"
                    } finally {
                        dlBusy = false
                    }
                }
            }
        }

        AnimatedVisibility(target != null, enter = fadeIn(motion(Motion.D2)), exit = fadeOut(motion(Motion.D2))) {
            val tg = remember(target) { target }
            if (tg != null) {
                PlayerScreen(tg.g, tg.season, tg.ep) { target = null }
            }
        }

        AnimatedVisibility(
            toastOn,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp),
            enter = fadeIn(motion(Motion.D2)),
            exit = fadeOut(motion(Motion.D2)),
        ) {
            Box(Modifier.clip(Shp.R2).background(C.S3).padding(horizontal = 16.dp, vertical = 8.dp)) { AText(toastMsg) }
        }
    }
}
