package app.ani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
private fun SelectRow(label: String, options: List<String>) {
    var i by remember { mutableIntStateOf(0) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AText(label)
        // Tap cycles through the options. A proper picker comes with the real settings.
        Box(
            Modifier.clip(Shp.R1).background(C.S2).hairBorder(Shp.R1)
                .tap { i = (i + 1) % options.size }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) { AText(options[i]) }
    }
}

@Composable
fun SettingsScreen(subSize: Int, onSubSize: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth()) { AText("Playback", Modifier.padding(top = 8.dp, bottom = 12.dp), weight = 500, color = C.Mut) }
        Column(Modifier.fillMaxWidth().clip(Shp.R2).background(C.S1).padding(horizontal = 16.dp)) {
            SelectRow("Subtitle language", listOf("English", "Arabic"))
            Hair()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AText("Subtitle size")
                AniSlider(
                    value = (subSize - 16) / 14f,
                    onChange = { onSubSize((16 + it * 14).roundToInt()) },
                    modifier = Modifier.width(140.dp),
                    track = C.S3,
                )
            }
            Hair()
            SelectRow("Default speed", listOf("1×", "1.25×"))
        }
        Row(Modifier.fillMaxWidth()) { AText("About", Modifier.padding(top = 24.dp, bottom = 12.dp), weight = 500, color = C.Mut) }
        Column(Modifier.fillMaxWidth().clip(Shp.R2).background(C.S1).padding(16.dp)) {
            AText("ani", weight = 500)
            AText(
                "2.0\nDownloads, cached posters, history and settings live in the storage folder, so updating the app never touches them.",
                size = 12, color = C.Mut, lh = 1.6f,
            )
        }
    }
}
