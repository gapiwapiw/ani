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
import app.ani.data.FakeShows
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

@Composable
fun SearchScreen(q: String, onQ: (String) -> Unit, onOpen: (Int) -> Unit) {
    var recent by remember { mutableStateOf(listOf("frieren", "mushishi", "slime")) }
    val results = FakeShows.withIndex().filter { it.value.title.contains(q, ignoreCase = true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        if (q.isEmpty()) {
            if (recent.isNotEmpty()) {
                SectionHeader("Recent searches", "Clear") { recent = emptyList() }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { r -> Chip(r) { onQ(r) } }
                }
            }
            SectionHeader("All titles")
        } else {
            Spacer(Modifier.height(16.dp))
        }
        if (results.isEmpty()) {
            Empty("No results for “$q”", "Check the spelling, or try the original Japanese title.", Modifier.padding(horizontal = 16.dp))
        } else {
            results.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { (i, s) ->
                        ShowCard(s, "${s.year}, ${s.eps} episodes", null, Modifier.weight(1f)) { onOpen(i) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
