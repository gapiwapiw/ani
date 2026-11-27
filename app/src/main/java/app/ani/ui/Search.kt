package app.ani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ani.Graph
import app.ani.data.SearchOut
import app.ani.data.ShowGroup
import app.ani.data.toShow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border

@Composable
fun SearchField(q: String, onQ: (String) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) C.Ac2 else C.Ln, motion(Motion.D2), label = "qborder")
    Box(
        modifier.height(44.dp).clip(Shp.R2).background(C.S1).border(1.dp, border, Shp.R2),
    ) {
        AIcon(AniIcons.Search, C.Mut, 20.dp, Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
        BasicTextField(
            value = q,
            onValueChange = onQ,
            singleLine = true,
            textStyle = TextStyle(color = C.Tx, fontSize = 16.sp),
            cursorBrush = SolidColor(C.Ac2),
            modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 14.dp)
                .wrapContentHeight(Alignment.CenterVertically)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (q.isEmpty()) AText("Search anime", size = 16, color = C.Mut)
                    inner()
                }
            },
        )
    }
}

private sealed interface SState {
    data object Idle : SState
    data object Loading : SState
    data class Done(val out: SearchOut) : SState
    data class Err(val msg: String) : SState
}

@Composable
fun SearchScreen(q: String, mode: String, onQ: (String) -> Unit, onOpen: (ShowGroup) -> Unit) {
    val st = Graph.store
    var state by remember(q, mode) { mutableStateOf<SState>(SState.Idle) }
    LaunchedEffect(q, mode) {
        if (q.isBlank()) return@LaunchedEffect
        state = SState.Loading
        delay(450)
        state = try {
            val r = Graph.repo.search(q.trim(), mode)
            if (r.groups.isNotEmpty()) st.addQuery(q)
            SState.Done(r)
        } catch (e: Exception) {
            SState.Err(e.message ?: "Something went wrong")
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        if (q.isBlank()) {
            if (st.recentQ.isNotEmpty()) {
                SectionHeader("Recent searches", "Clear") { st.clearQueries() }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) { st.recentQ.forEach { r -> Chip(r) { onQ(r) } } }
            } else {
                Empty("Find something to watch", "Type a title above.", Modifier.padding(horizontal = 16.dp))
            }
            return@Column
        }
        Spacer(Modifier.height(16.dp))
        when (val s = state) {
            SState.Idle, SState.Loading ->
                AText("Searching…", Modifier.padding(horizontal = 16.dp), color = C.Mut)
            is SState.Err ->
                Empty("Couldn’t search", s.msg, Modifier.padding(horizontal = 16.dp))
            is SState.Done ->
                if (s.out.groups.isEmpty()) {
                    Empty("No results for “$q”", "Check the spelling, or try the original Japanese title.", Modifier.padding(horizontal = 16.dp))
                } else {
                    s.out.groups.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { g ->
                                ShowCard(g.toShow(), g.sub(), null, Modifier.weight(1f)) { onOpen(g) }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
        }
    }
}
