package app.ani.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ani.data.DlItem
import app.ani.data.Downloads
import app.ani.data.Show

private fun mb(b: Long): String = if (b >= 1_000_000_000L) "%.1f GB".format(b / 1e9) else "${b / 1_000_000} MB"

@Composable
fun DownloadsScreen(onOpen: (String) -> Unit) {
    val items by Downloads.items.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (items.isEmpty()) {
            Empty("No downloads yet", "Downloaded episodes appear here and play without a connection.")
            return@Column
        }
        val run = items.count { it.active }
        val done = items.count { it.done }
        val paused = items.size - run - done
        val parts = listOfNotNull(
            if (run > 0) "$run downloading" else null,
            if (paused > 0) "$paused paused" else null,
            if (done > 0) "$done finished" else null,
        )
        AText(parts.joinToString(", "), Modifier.padding(bottom = 4.dp), size = 12, color = C.Mut)
        AText(
            "${mb(Downloads.usedBytes())} used, ${mb(Downloads.freeBytes())} free",
            Modifier.padding(bottom = 8.dp), size = 12, color = C.Mut,
        )
        items.forEachIndexed { idx, d -> Row2(d, onOpen); if (idx < items.lastIndex) Hair() }
    }
}

@Composable
private fun Row2(d: DlItem, onOpen: (String) -> Unit) {
    val show = Show(key = d.gk, title = d.title, thumb = d.thumb, hue = d.gk.hashCode().mod(360).toFloat())
    val (st, pct) = d.ring()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(show, Modifier.width(56.dp).tap { onOpen(d.gk) }, small = true)
        Column(Modifier.weight(1f)) {
            AText(d.title, weight = 500, maxLines = 1)
            val size = when {
                d.failed -> "Failed. Tap the ring to retry"
                d.done -> mb(d.bytes)
                d.total > 0 -> "${mb(d.bytes)} of ${mb(d.total)}"
                else -> "Starting…"
            }
            AText("Episode ${d.ep}, $size", Modifier.padding(top = 2.dp), size = 12, color = C.Mut, maxLines = 1)
            if (!d.done) PBar(d.pct / 100f, Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        if (d.done) {
            IconBtn(AniIcons.Close, "Remove download", { Downloads.remove(d) })
        } else {
            Ring(st, pct) { if (d.active) Downloads.pause(d) else Downloads.resume(d) }
        }
    }
}
