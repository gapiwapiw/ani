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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ani.data.FakeShows

data class Item(val i: Int, val sub: String, val p: Float? = null)

@Composable
fun HRow(list: List<Item>, onOpen: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(list) { item ->
            ShowCard(FakeShows[item.i], item.sub, item.p, Modifier.width(108.dp)) { onOpen(item.i) }
        }
    }
}

@Composable
fun HomeScreen(onOpen: (Int) -> Unit) {
    var recent by remember { mutableStateOf(listOf(Item(6, "2004, 74 episodes"), Item(7, "2014, 25 episodes"))) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SectionHeader("Continue watching")
        HRow(
            listOf(
                Item(0, "Episode 6, 14 min left", 0.58f),
                Item(1, "Episode 3, 21 min left", 0.22f),
                Item(3, "Next: episode 12"),
            ),
            onOpen,
        )
        SectionHeader("My list")
        HRow(
            listOf(Item(2, "2018, 13 episodes"), Item(4, "2005, 26 episodes"), Item(5, "1998, 26 episodes")),
            onOpen,
        )
        if (recent.isNotEmpty()) {
            SectionHeader("Recently viewed", "Clear") { recent = emptyList() }
            HRow(recent, onOpen)
        }
    }
}
