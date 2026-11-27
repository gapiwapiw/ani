package app.ani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.ani.BuildConfig
import app.ani.Graph
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
private fun Group(title: String, top: Int = 8, content: @Composable () -> Unit) {
    AText(title, Modifier.padding(top = top.dp, bottom = 12.dp), weight = 500, color = C.Mut)
    Column(Modifier.fillMaxWidth().clip(Shp.R2).background(C.S1).padding(horizontal = 16.dp)) { content() }
}

@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        AText(label, Modifier.padding(bottom = 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o -> Chip(o, on = o == selected) { onPick(o) } }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AText(label)
        AText(value, color = C.Mut)
    }
}

@Composable
fun SettingsScreen(toast: (String) -> Unit) {
    val st = Graph.store
    val p = st.prefs
    val rs by Graph.resolver.status.collectAsState()
    val scope = rememberCoroutineScope()
    val speeds = listOf("0.75×", "1×", "1.25×", "1.5×", "2×")
    val speedVals = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Group("Playback") {
            ChoiceRow("Subtitle language", listOf("Off", "English", "Arabic", "French", "Spanish", "Portuguese", "German"), p.subLang) {
                st.updatePrefs(p.copy(subLang = it))
            }
            Hair()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AText("Subtitle size")
                AniSlider(
                    value = (p.subSize - 16) / 14f,
                    onChange = { st.updatePrefs(p.copy(subSize = (16 + it * 14).roundToInt())) },
                    modifier = Modifier.width(140.dp),
                    track = C.S3,
                )
            }
            Hair()
            ChoiceRow("Default speed", speeds, speeds[speedVals.indexOf(p.speed).coerceAtLeast(1)]) {
                st.updatePrefs(p.copy(speed = speedVals[speeds.indexOf(it)]))
            }
            Hair()
            ChoiceRow("Download quality", listOf("Best", "1080p", "720p", "480p"), when (p.quality) { "best" -> "Best"; else -> p.quality }) {
                st.updatePrefs(p.copy(quality = if (it == "Best") "best" else it))
            }
        }
        Group("Source", top = 24) {
            InfoRow("Resolver", rs.source)
            Hair()
            InfoRow("Version", rs.version.toString())
            Hair()
            InfoRow(
                "Last update",
                if (rs.updatedAt > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(rs.updatedAt)) else "Never",
            )
            if (rs.error != null) {
                Hair()
                AText(rs.error ?: "", Modifier.padding(vertical = 12.dp), size = 12, color = C.Mut)
            }
            Hair()
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Btn(
                    "Refresh",
                    { scope.launch { toast(Graph.resolver.refresh()?.let { "Refresh failed: $it" } ?: "Resolver updated") } },
                    Modifier.fillMaxWidth(), ghost = true,
                )
            }
        }
        Group("About", top = 24) {
            Column(Modifier.padding(vertical = 16.dp)) {
                AText("ani", weight = 500)
                AText(
                    "${BuildConfig.VERSION_NAME}\nHistory, my list and settings are stored on this device. Downloads stay until you remove them.",
                    Modifier.padding(top = 2.dp), size = 12, color = C.Mut, lh = 1.6f,
                )
            }
        }
    }
}
