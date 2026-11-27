package app.ani.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.ani.data.SubTrack

@Composable
private fun SubRow(label: String, on: Boolean, onToggle: () -> Unit) {
    val box by animateColorAsState(if (on) C.Ac else Color.Transparent, motion(Motion.D2), label = "ck")
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Shp.R2).background(C.S2)
            .tap { onToggle() }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(Shp.R1).background(box).hairBorder(Shp.R1),
            contentAlignment = Alignment.Center,
        ) { if (on) AIcon(AniIcons.Check, C.OnAc, 14.dp) }
        AText(label, Modifier.weight(1f))
    }
}

/** Choices made in the sheet. quality is "best" or a label such as "720p". */
data class DlChoice(val quality: String, val subs: List<SubTrack>)

@Composable
fun DownloadSheet(
    visible: Boolean,
    ep: String,
    subs: List<SubTrack>?,
    qualities: List<String>,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onStart: (DlChoice) -> Unit,
) {
    var q by remember(ep, visible) { mutableStateOf("best") }
    var picked by remember(ep, visible, subs) { mutableStateOf(setOf<Int>()) }
    AnimatedVisibility(visible, enter = fadeIn(motion(Motion.D2)), exit = fadeOut(motion(Motion.D2))) {
        Box(Modifier.fillMaxSize().background(Color(0x8C000000)).plainClick(onClose)) {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(motion(Motion.D3)) { it / 6 },
                        exit = slideOutVertically(motion(Motion.D3)) { it / 6 },
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(C.S1)
                    .plainClick { }
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 16.dp)
                        .width(32.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(C.S3),
                )
                AText("Download episode $ep", size = 18, weight = 600)
                AText("Choose subtitle languages and quality.", Modifier.padding(top = 2.dp, bottom = 16.dp), color = C.Mut)
                when {
                    error != null -> AText(error, Modifier.padding(bottom = 16.dp), color = C.Mut)
                    subs == null -> AText("Checking what is available…", Modifier.padding(bottom = 16.dp), color = C.Mut)
                    else -> {
                        if (subs.isNotEmpty()) {
                            AText("Subtitles", Modifier.padding(bottom = 8.dp), weight = 500, color = C.Mut)
                            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                                subs.forEachIndexed { i, t ->
                                    SubRow(t.label.ifBlank { t.lang }, i in picked) {
                                        picked = if (i in picked) picked - i else picked + i
                                    }
                                }
                            }
                        }
                        AText("Quality", Modifier.padding(top = 8.dp, bottom = 8.dp), weight = 500, color = C.Mut)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (listOf("Best") + qualities).forEach { t ->
                                val v = if (t == "Best") "best" else t
                                Chip(t, on = v == q) { q = v }
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Cancel", onClose, Modifier.weight(1f), ghost = true)
                    Btn(
                        if (busy) "Starting…" else "Download",
                        { if (!busy && subs != null && error == null) onStart(DlChoice(q, picked.sorted().map { subs[it] })) },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
