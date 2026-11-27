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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ani.data.FakeShows

data class Dl(val show: Int, val ep: Int, val state: DlState, val pct: Int, val size: String)

@Composable
fun DownloadsScreen() {
    val items = remember {
        mutableStateListOf(
            Dl(0, 2, DlState.Done, 100, "312 MB"),
            Dl(1, 7, DlState.Run, 62, "188 of 304 MB"),
            Dl(4, 8, DlState.Paused, 30, "92 of 305 MB"),
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
        AText("1 downloading, 1 paused, 1 finished", Modifier.padding(bottom = 8.dp), size = 12, color = C.Mut)
        items.forEachIndexed { idx, d ->
            val s = FakeShows[d.show]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Poster(s, Modifier.width(56.dp), small = true)
                Column(Modifier.weight(1f)) {
                    AText(s.title, weight = 500, maxLines = 1)
                    AText("Episode ${d.ep}, ${d.size}", Modifier.padding(top = 2.dp), size = 12, color = C.Mut)
                    if (d.state != DlState.Done) PBar(d.pct / 100f, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                Ring(d.state, d.pct) {
                    val n = when (d.state) {
                        DlState.Run -> DlState.Paused
                        DlState.Paused -> DlState.Run
                        else -> d.state
                    }
                    items[idx] = d.copy(state = n)
                }
            }
            if (idx < items.lastIndex) Hair()
        }
    }
}
