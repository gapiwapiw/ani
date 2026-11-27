package app.ani.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.ani.data.FakeEpisodes
import app.ani.data.Show

@Composable
fun ShowScreen(
    s: Show,
    onBack: () -> Unit,
    onWatch: () -> Unit,
    onDownload: (Int) -> Unit,
    toast: (String) -> Unit,
) {
    val scroll = rememberScrollState()
    val thr = with(LocalDensity.current) { 180.dp.toPx() }
    val solid = scroll.value > thr
    var liked by remember(s) { mutableStateOf(false) }
    var expanded by remember(s) { mutableStateOf(false) }
    var season by remember(s) { mutableIntStateOf(0) }
    var dub by remember(s) { mutableIntStateOf(0) }
    val st = remember(s) {
        mutableStateMapOf(2 to (DlState.Done to 100), 7 to (DlState.Run to 62))
    }
    val tbBg by animateColorAsState(if (solid) C.Bg else Color.Transparent, motion(Motion.D2), label = "tb")
    val titleAlpha by animateFloatAsState(if (solid) 1f else 0f, motion(Motion.D2), label = "tbt")

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // Hero: the only decorative gradient is this fade into the background.
            Box(Modifier.fillMaxWidth().height(300.dp).background(s.bg())) {
                if (s.none) {
                    AIcon(AniIcons.Film, C.Mut, 48.dp, Modifier.align(Alignment.Center))
                } else {
                    AText(
                        s.title.take(1), Modifier.align(Alignment.Center),
                        size = 96, weight = 500, color = Color.hsl(s.hue, 0.14f, 0.30f), lh = 1f,
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to C.Bg),
                    ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp)) {
                    AText(s.title, size = 28, weight = 600, lh = 1.15f)
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AText("${s.year}", color = C.Mut)
                        AText("${s.eps} episodes", color = C.Mut)
                        Box(Modifier.hairBorder(Shp.R1).padding(horizontal = 6.dp)) { AText(s.rating, size = 12) }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Btn("Continue episode 6", onWatch, Modifier.weight(1f), icon = AniIcons.PlayFilled)
                    IconBtn(AniIcons.Plus, "Watch later", { toast("Saved to watch later") }, sq = true)
                    IconBtn(AniIcons.Download, "Download", { onDownload(6) }, sq = true)
                }
                AText(
                    s.desc, maxLines = if (expanded) Int.MAX_VALUE else 3,
                    color = C.Tx.copy(alpha = 0.78f), lh = 1.55f,
                )
                if (s.desc.length > 140) {
                    AText(
                        if (expanded) "Less" else "More",
                        Modifier.padding(top = 4.dp).tap { expanded = !expanded },
                        size = 12, color = C.Ac2,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText("Episodes", size = 18, weight = 600)
                    Seg(listOf("Sub", "Dub"), dub, { dub = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("Season 1", "Season 2", "Movies").forEachIndexed { i, name ->
                        val on = i == season
                        Column(Modifier.width(IntrinsicSize.Max).tap { season = i }) {
                            AText(name, Modifier.padding(vertical = 8.dp), weight = 500, color = if (on) C.Tx else C.Mut)
                            Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) C.Tx else Color.Transparent))
                        }
                    }
                }
                Hair(Modifier.padding(bottom = 8.dp))
                FakeEpisodes.forEachIndexed { k, title ->
                    val n = k + 1
                    val watched = n < 6
                    val cur = st[n] ?: (DlState.Idle to 0)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.weight(0.44f).aspectRatio(16f / 9f)
                                .alpha(if (watched) 0.5f else 1f)
                                .clip(RoundedCornerShape(8.dp)).background(s.bg())
                                .tap(onClick = onWatch),
                            contentAlignment = Alignment.Center,
                        ) {
                            AText("$n", size = 18, weight = 500, color = Color.hsl(s.hue, 0.14f, 0.42f))
                            if (n == 6) {
                                Box(
                                    Modifier.align(Alignment.BottomStart).fillMaxWidth(0.58f)
                                        .height(2.dp).background(C.Ac2),
                                )
                            }
                        }
                        Column(Modifier.weight(0.56f).tap(onClick = onWatch)) {
                            AText("Episode $n, 24 min", size = 12, color = C.Mut)
                            AText(
                                title, Modifier.padding(top = 2.dp), weight = 500, maxLines = 2, lh = 1.3f,
                                color = when {
                                    n == 6 -> C.Ac2
                                    watched -> C.Tx.copy(alpha = 0.5f)
                                    else -> C.Tx
                                },
                            )
                        }
                        Ring(cur.first, cur.second) {
                            when (cur.first) {
                                DlState.Run -> st[n] = DlState.Paused to cur.second
                                DlState.Paused -> st[n] = DlState.Run to cur.second
                                DlState.Idle -> onDownload(n)
                                DlState.Done -> {}
                            }
                        }
                    }
                }
            }
            Box(Modifier.height(32.dp))
        }
        // Toolbar: transparent over the hero, solid once scrolled.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().background(tbBg)
                .drawBehind {
                    if (solid) drawRect(C.Ln, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
                }
                .statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val chip = if (solid) Color.Transparent else Color(0x99151413)
            IconBtn(AniIcons.Back, "Back", onBack, bg = chip)
            AText(s.title, Modifier.weight(1f).alpha(titleAlpha), size = 16, weight = 500, maxLines = 1)
            IconBtn(
                if (liked) AniIcons.HeartFilled else AniIcons.Heart, "Add to my list",
                { liked = !liked; toast(if (liked) "Added to my list" else "Removed from my list") },
                tint = if (liked) C.Ac2 else C.Tx, bg = chip,
            )
        }
    }
}
