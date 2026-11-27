package app.ani.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ani.Graph
import app.ani.data.ShowGroup
import app.ani.data.toShow

data class Item(val g: ShowGroup, val sub: String, val p: Float? = null)

/** "2 seasons, 1 more" style label for a franchise card. */
fun ShowGroup.sub(): String {
    val n = seasons.count { it.kind == "season" }
    val o = seasons.size - n
    val a = when (n) { 0 -> ""; 1 -> "1 season"; else -> "$n seasons" }
    val b = if (o > 0) (if (n == 0) "$o entries" else "+$o more") else ""
    return listOf(a, b).filter { it.isNotEmpty() }.joinToString(", ")
}

@Composable
fun HRow(list: List<Item>, onOpen: (ShowGroup) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(list, key = { it.g.key }) { item ->
            ShowCard(item.g.toShow(), item.sub, item.p, Modifier.width(108.dp)) { onOpen(item.g) }
        }
    }
}

@Composable
fun HomeScreen(onOpen: (ShowGroup) -> Unit) {
    val st = Graph.store
    val cont = st.hist.values.sortedByDescending { it.t }
        .mapNotNull { h -> st.group(h.gk)?.let { g -> h to g } }
        .distinctBy { it.second.key }.take(12)
        .map { (h, g) ->
            val frac = if (h.dur > 0) (h.pos / h.dur).toFloat() else 0f
            if (frac > 0.92f) Item(g, "Watched episode ${h.ep}")
            else Item(g, "Episode ${h.ep}, ${minutesLeft(h.dur - h.pos)} min left", frac)
        }
    val mine = st.list.mapNotNull { st.group(it) }.map { Item(it, it.sub()) }
    val later = st.later.mapNotNull { st.group(it) }.map { Item(it, it.sub()) }
    val recent = st.recentA.mapNotNull { st.group(it) }.map { Item(it, it.sub()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        if (cont.isEmpty() && mine.isEmpty() && later.isEmpty() && recent.isEmpty()) {
            Empty("Nothing here yet", "Search for a title to get started.", Modifier.padding(horizontal = 16.dp))
        }
        if (cont.isNotEmpty()) {
            SectionHeader("Continue watching")
            HRow(cont, onOpen)
        }
        if (mine.isNotEmpty()) {
            SectionHeader("My list")
            HRow(mine, onOpen)
        }
        if (later.isNotEmpty()) {
            SectionHeader("Watch later")
            HRow(later, onOpen)
        }
        if (recent.isNotEmpty()) {
            SectionHeader("Recently viewed", "Clear") { st.clearRecent() }
            HRow(recent, onOpen)
        }
    }
}

private fun minutesLeft(sec: Double): Int = maxOf(1, Math.round(sec / 60.0).toInt())
