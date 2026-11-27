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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.ContentScale
import app.ani.Graph
import app.ani.data.EpisodeRef
import app.ani.data.SeasonRef
import app.ani.data.ShowGroup
import app.ani.data.ShowMeta
import app.ani.data.toShow
import coil3.compose.AsyncImage

@Composable
fun ShowScreen(
    g: ShowGroup,
    mode: String,
    onMode: (String) -> Unit,
    onBack: () -> Unit,
    onWatch: (SeasonRef, String) -> Unit,
    onDownload: (SeasonRef, String) -> Unit,
    toast: (String) -> Unit,
) {
    val st = Graph.store
    val scroll = rememberScrollState()
    val thr = with(LocalDensity.current) { 180.dp.toPx() }
    val solid = scroll.value > thr
    val meta by produceState<ShowMeta?>(null, g.key) { value = Graph.repo.info(g.name) }
    val s = g.toShow(meta)
    val liked = g.key in st.list
    val later = g.key in st.later
    var expanded by remember(g.key) { mutableStateOf(false) }
    val latest = st.latestFor(g)
    var si by remember(g.key) {
        mutableIntStateOf(g.seasons.indexOfFirst { it.id == latest?.sid }.coerceAtLeast(0))
    }
    val season = g.seasons[si.coerceIn(0, g.seasons.lastIndex)]
    val eps by produceState<Result<List<EpisodeRef>>?>(null, season.id, mode) {
        value = null
        value = runCatching { Graph.repo.episodes(season.id, mode) }
    }
    var limit by remember(season.id, mode) { mutableIntStateOf(20) }
    val tbBg by animateColorAsState(if (solid) C.Bg else Color.Transparent, motion(Motion.D2), label = "tb")
    val titleAlpha by animateFloatAsState(if (solid) 1f else 0f, motion(Motion.D2), label = "tbt")

    val list = eps?.getOrNull().orEmpty()
    val resumeSeason = g.seasons.firstOrNull { it.id == latest?.sid } ?: season
    val resumeEp = latest?.ep ?: list.firstOrNull()?.number
    val watchLabel = if (latest != null) "Continue episode ${latest.ep}" else "Watch" + (resumeEp?.let { " episode $it" } ?: "")

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // Hero: the only decorative gradient is this fade into the background.
            Box(Modifier.fillMaxWidth().height(300.dp).background(s.bg())) {
                AText(
                    s.title.take(1), Modifier.align(Alignment.Center),
                    size = 96, weight = 500, color = Color.hsl(s.hue, 0.14f, 0.30f), lh = 1f,
                )
                if (s.thumb.isNotBlank()) {
                    AsyncImage(
                        model = s.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
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
                        if (s.year.isNotBlank()) AText(s.year, color = C.Mut)
                        if (list.isNotEmpty()) AText("${list.size} episodes", color = C.Mut)
                        if (s.rating.isNotBlank()) Box(Modifier.hairBorder(Shp.R1).padding(horizontal = 6.dp)) { AText(s.rating, size = 12) }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Btn(
                        watchLabel,
                        { if (resumeEp != null) onWatch(resumeSeason, resumeEp) },
                        Modifier.weight(1f), icon = AniIcons.PlayFilled,
                    )
                    IconBtn(
                        AniIcons.Plus, "Watch later",
                        { st.remember(g); st.toggleLater(g.key); toast(if (!later) "Saved to watch later" else "Removed from watch later") },
                        tint = if (later) C.Ac2 else C.Tx, sq = true,
                    )
                    IconBtn(
                        AniIcons.Download, "Download",
                        { if (resumeEp != null) onDownload(resumeSeason, resumeEp) },
                        sq = true,
                    )
                }
                if (s.desc.isNotBlank()) {
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
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText("Episodes", size = 18, weight = 600)
                    Seg(listOf("Sub", "Dub"), if (mode == "dub") 1 else 0, { onMode(if (it == 1) "dub" else "sub") })
                }
                if (g.seasons.size > 1) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        g.seasons.forEachIndexed { i, sr ->
                            val on = i == si
                            Column(Modifier.width(IntrinsicSize.Max).tap { si = i }) {
                                AText(sr.label, Modifier.padding(vertical = 8.dp), weight = 500, color = if (on) C.Tx else C.Mut, maxLines = 1)
                                Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) C.Tx else Color.Transparent))
                            }
                        }
                    }
                    Hair(Modifier.padding(bottom = 8.dp))
                }
                val res = eps
                when {
                    res == null -> AText("Loading episodes…", Modifier.padding(vertical = 16.dp), color = C.Mut)
                    res.isFailure -> Empty("Couldn’t load episodes", res.exceptionOrNull()?.message ?: "Try again later.")
                    list.isEmpty() -> Empty("No episodes", "Nothing is available for ${if (mode == "dub") "dub" else "sub"} yet.")
                }
                val h = st.hist[season.id]
                val titles = if (season.kind == "season") meta?.epTitles?.get(season.num) else null
                list.take(limit).forEach { e ->
                    val n = e.number
                    val w = h?.w?.get(n)
                    val frac = if (w != null && w.second > 0) (w.first / w.second).toFloat() else 0f
                    val watched = frac > 0.92f
                    val cur = h?.ep == n && !watched
                    val title = titles?.get(n)?.takeIf { it.isNotBlank() } ?: "Episode $n"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.weight(0.44f).aspectRatio(16f / 9f)
                                .alpha(if (watched) 0.5f else 1f)
                                .clip(RoundedCornerShape(8.dp)).background(s.bg())
                                .tap { onWatch(season, n) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AText(n, size = 18, weight = 500, color = Color.hsl(s.hue, 0.14f, 0.42f))
                            if (frac > 0.01f && !watched) {
                                Box(
                                    Modifier.align(Alignment.BottomStart).fillMaxWidth(frac.coerceIn(0f, 1f))
                                        .height(2.dp).background(C.Ac2),
                                )
                            }
                        }
                        Column(Modifier.weight(0.56f).tap { onWatch(season, n) }) {
                            AText("Episode $n", size = 12, color = C.Mut)
                            AText(
                                title, Modifier.padding(top = 2.dp), weight = 500, maxLines = 2, lh = 1.3f,
                                color = when {
                                    cur -> C.Ac2
                                    watched -> C.Tx.copy(alpha = 0.5f)
                                    else -> C.Tx
                                },
                            )
                        }
                        val dl = app.ani.data.Downloads.find(season.id, n, mode)
                        val (rs, rp) = dl?.ring() ?: (DlState.Idle to 0)
                        val dls by app.ani.data.Downloads.items.collectAsState()
                        Ring(rs, rp) {
                            when {
                                dl == null -> onDownload(season, n)
                                dl.done -> {}
                                dl.active -> app.ani.data.Downloads.pause(dl)
                                else -> app.ani.data.Downloads.resume(dl)
                            }
                        }
                        if (dls.isEmpty()) Unit
                    }
                }
                if (list.size > limit) {
                    Btn("Show more", { limit += 30 }, Modifier.fillMaxWidth().padding(top = 8.dp), ghost = true)
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
                { st.remember(g); st.toggleList(g.key); toast(if (!liked) "Added to my list" else "Removed from my list") },
                tint = if (liked) C.Ac2 else C.Tx, bg = chip,
            )
        }
    }
}
