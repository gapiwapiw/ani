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
import app.ani.data.FakeShows
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
private fun BottomNav(tab: Int, onTab: (Int) -> Unit) {
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
                    if (i == 2) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-4).dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(RoundedCornerShape(8.dp)).background(C.S3).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) { AText("1", size = 12, lh = 1.3f) }
                    }
                }
                AText(label, size = 12, color = col)
            }
        }
    }
}

@Composable
fun AniApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    var dub by rememberSaveable { mutableIntStateOf(0) }
    var openShow by rememberSaveable { mutableIntStateOf(-1) }
    var lastShow by rememberSaveable { mutableIntStateOf(0) }
    var player by rememberSaveable { mutableStateOf(false) }
    var sheetEp by rememberSaveable { mutableIntStateOf(-1) }
    var subSize by rememberSaveable { mutableIntStateOf(20) }

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

    // Back closes the sheet, then the player, then the show page, then exits.
    BackHandler(enabled = sheetEp >= 0 || player || openShow >= 0) {
        when {
            sheetEp >= 0 -> sheetEp = -1
            player -> player = false
            else -> openShow = -1
        }
    }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Header(tab, q, { q = it }, dub) { dub = it }
            Box(Modifier.weight(1f)) {
                Crossfade(tab, animationSpec = motion(Motion.D3), label = "tab") { t ->
                    val open = { i: Int -> lastShow = i; openShow = i }
                    when (t) {
                        0 -> HomeScreen(open)
                        1 -> SearchScreen(q, { q = it }, open)
                        2 -> DownloadsScreen()
                        else -> SettingsScreen(subSize) { subSize = it; toast("Subtitle size $it") }
                    }
                }
            }
            BottomNav(tab) { tab = it }
        }

        AnimatedVisibility(
            openShow >= 0,
            enter = slideInHorizontally(motion(Motion.D3)) { it },
            exit = slideOutHorizontally(motion(Motion.D3)) { it },
        ) {
            ShowScreen(
                s = FakeShows[lastShow],
                onBack = { openShow = -1 },
                onWatch = { player = true },
                onDownload = { sheetEp = it },
                toast = toast,
            )
        }

        DownloadSheet(sheetEp >= 0, if (sheetEp >= 0) sheetEp else 6, { sheetEp = -1 }) {
            sheetEp = -1
            toast("Download started")
        }

        AnimatedVisibility(player, enter = fadeIn(motion(Motion.D2)), exit = fadeOut(motion(Motion.D2))) {
            PlayerScreen(FakeShows[lastShow], subSize) { player = false }
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
