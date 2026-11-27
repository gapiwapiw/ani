package app.ani.data

import app.ani.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class Cue(val s: Long, val e: Long, val text: String)

/** Minimal WebVTT / SRT reader. Subtitles are drawn by the app (Compose), not by Media3. */
object Subs {
    private val TIME = Regex(
        "(?:(\\d+):)?(\\d{1,2}):(\\d{2})[.,](\\d{1,3})\\s*-->\\s*(?:(\\d+):)?(\\d{1,2}):(\\d{2})[.,](\\d{1,3})",
    )
    private val TAGS = Regex("<[^>]*>|\\{\\\\[^}]*\\}")

    private fun ms(h: String, m: String, s: String, f: String): Long =
        (h.ifEmpty { "0" }.toLong() * 3600 + m.toLong() * 60 + s.toLong()) * 1000 + f.padEnd(3, '0').take(3).toLong()

    fun parse(raw: String): List<Cue> {
        val text = raw.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        val out = ArrayList<Cue>()
        for (block in text.split("\n\n")) {
            val lines = block.trim().split("\n")
            val ti = lines.indexOfFirst { TIME.containsMatchIn(it) }
            if (ti < 0) continue
            val m = TIME.find(lines[ti]) ?: continue
            val g = m.groupValues
            val body = lines.drop(ti + 1).joinToString("\n") { TAGS.replace(it, "") }
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
                .trim()
            if (body.isNotEmpty()) out.add(Cue(ms(g[1], g[2], g[3], g[4]), ms(g[5], g[6], g[7], g[8]), body))
        }
        return out.sortedBy { it.s }
    }

    /** Download and parse one subtitle file. Returns null when it cannot be read as text. */
    suspend fun load(url: String, referer: String): List<Cue>? = withContext(Dispatchers.IO) {
        try {
            val b = Request.Builder().url(url)
            if (referer.isNotBlank()) b.header("Referer", referer)
            Graph.client.newCall(b.build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val bytes = r.body.bytes()
                if (bytes.size > 3 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) return@withContext null
                parse(String(bytes, Charsets.UTF_8))
            }
        } catch (e: Exception) {
            null
        }
    }
}
