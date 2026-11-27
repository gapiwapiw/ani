package app.ani.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import app.ani.AniDownloadService
import app.ani.Graph
import app.ani.ui.DlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One row in the downloads list. Metadata travels in the DownloadRequest data (JSON). */
data class DlItem(
    val id: String,
    val gk: String,
    val sid: String,
    val ep: String,
    val mode: String,
    val title: String,
    val label: String,
    val thumb: String,
    val state: Int,
    val stopReason: Int,
    val pct: Float,
    val bytes: Long,
    val total: Long,
    val meta: JSONObject,
) {
    val done get() = state == Download.STATE_COMPLETED
    val active get() = state == Download.STATE_DOWNLOADING || state == Download.STATE_QUEUED || state == Download.STATE_RESTARTING
    val failed get() = state == Download.STATE_FAILED
    fun ring(): Pair<DlState, Int> = when {
        done -> DlState.Done to 100
        active -> DlState.Run to pct.toInt()
        else -> DlState.Paused to pct.toInt()
    }
}

object Downloads {
    private val _items = MutableStateFlow<List<DlItem>>(emptyList())
    val items: StateFlow<List<DlItem>> = _items.asStateFlow()
    private var started = false

    fun id(sid: String, ep: String, mode: String) = "$sid|$ep|$mode"
    fun find(sid: String, ep: String, mode: String): DlItem? = _items.value.firstOrNull { it.id == id(sid, ep, mode) }

    fun start() {
        if (started) return
        started = true
        Graph.downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                Graph.scope.launch { refresh() }
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                Graph.scope.launch { refresh() }
            }
        })
        Graph.scope.launch {
            while (true) {
                refresh()
                delay(if (_items.value.any { it.active }) 1000L else 5000L)
            }
        }
    }

    suspend fun refresh() {
        val list = withContext(Dispatchers.IO) {
            val dm = Graph.downloadManager
            val byId = LinkedHashMap<String, Download>()
            try {
                dm.downloadIndex.getDownloads().use { c -> while (c.moveToNext()) c.download.let { byId[it.request.id] = it } }
            } catch (e: IOException) { /* index unreadable: show live downloads only */ }
            for (d in dm.currentDownloads) byId[d.request.id] = d
            byId.values.mapNotNull { toItem(it) }.sortedByDescending { it.meta.optLong("t") }
        }
        _items.value = list
    }

    private fun toItem(d: Download): DlItem? = try {
        val m = JSONObject(String(d.request.data, Charsets.UTF_8))
        DlItem(
            d.request.id, m.optString("g"), m.optString("sid"), m.optString("ep"), m.optString("mode", "sub"),
            m.optString("title"), m.optString("label"), m.optString("thumb"),
            d.state, d.stopReason, d.percentDownloaded.coerceAtLeast(0f), d.bytesDownloaded, d.contentLength, m,
        )
    } catch (e: Exception) { null }

    fun usedBytes(): Long = _items.value.sumOf { it.bytes }
    fun freeBytes(): Long = Graph.app.filesDir.usableSpace

    // ---- actions ----
    fun pause(item: DlItem) =
        DownloadService.sendSetStopReason(Graph.app, AniDownloadService::class.java, item.id, 1, false)

    fun resume(item: DlItem) {
        if (item.failed) {
            // A failed download is restarted by adding its request again.
            val req = DownloadRequest.Builder(item.id, android.net.Uri.parse(item.meta.optString("m3u8")))
                .setMimeType("application/x-mpegURL").setData(item.meta.toString().toByteArray()).build()
            DownloadService.sendAddDownload(Graph.app, AniDownloadService::class.java, req, false)
        } else {
            DownloadService.sendSetStopReason(Graph.app, AniDownloadService::class.java, item.id, Download.STOP_REASON_NONE, false)
        }
    }

    fun remove(item: DlItem) {
        DownloadService.sendRemoveDownload(Graph.app, AniDownloadService::class.java, item.id, false)
        runCatching { File(Graph.app.filesDir, "dl/" + safe(item.id)).deleteRecursively() }
    }

    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9._-]"), "_")

    /** Queue an episode. quality is "best" or a label like "720p". Subtitles are saved next to it. */
    suspend fun enqueue(g: ShowGroup, season: SeasonRef, ep: String, mode: String, l: Loaded, tracks: List<SubTrack>, quality: String) {
        val ctx: Context = Graph.app
        val id = id(season.id, ep, mode)
        Graph.store.updateDlHeaders(l.info.headers)
        val maxH = quality.removeSuffix("p").toIntOrNull()
        val helper = DownloadHelper.forMediaItem(
            ctx, MediaItem.fromUri(l.info.m3u8), DefaultRenderersFactory(ctx),
            Playback.upstreamFactory(l.info.headers),
        )
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { cont ->
                helper.prepare(object : DownloadHelper.Callback {
                    override fun onPrepared(helper: DownloadHelper) {
                        try {
                            if (maxH != null) {
                                val params = TrackSelectionParameters.Builder(ctx).setMaxVideoSize(Int.MAX_VALUE, maxH).build()
                                for (p in 0 until helper.periodCount) {
                                    helper.clearTrackSelections(p)
                                    helper.addTrackSelection(p, params)
                                }
                            }
                            cont.resume(Unit)
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }

                    override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                        cont.resumeWithException(e)
                    }
                })
                cont.invokeOnCancellation { helper.release() }
            }
        }
        // Subtitles first (small), then hand the stream to the download manager.
        val dir = File(ctx.filesDir, "dl/" + safe(id)).also { it.mkdirs() }
        val subs = JSONArray()
        withContext(Dispatchers.IO) {
            tracks.forEachIndexed { i, t ->
                try {
                    val r = okhttp3.Request.Builder().url(t.url).header("Referer", l.info.referer).build()
                    Graph.client.newCall(r).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val f = File(dir, "sub$i.txt")
                            f.writeBytes(resp.body.bytes())
                            subs.put(JSONObject().put("label", t.label.ifBlank { t.lang }).put("lang", t.lang).put("file", f.absolutePath))
                        }
                    }
                } catch (e: Exception) { /* a missing subtitle must not block the video */ }
            }
        }
        val meta = JSONObject()
            .put("g", g.key).put("sid", season.id).put("ep", ep).put("mode", mode)
            .put("title", g.name).put("label", season.label).put("thumb", g.thumb)
            .put("m3u8", l.info.m3u8).put("referer", l.info.referer)
            .put("headers", JSONObject(l.info.headers)).put("subs", subs).put("t", System.currentTimeMillis())
        val request = helper.getDownloadRequest(id, meta.toString().toByteArray())
        helper.release()
        DownloadService.sendAddDownload(ctx, AniDownloadService::class.java, request, false)
        refresh()
    }

    /** What the player uses for an episode that finished downloading, or null. */
    fun localLoaded(sid: String, ep: String, mode: String): Loaded? {
        val d = find(sid, ep, mode)?.takeIf { it.done } ?: return null
        val m = d.meta
        val hs = m.optJSONObject("headers")
        val headers = hs?.keys()?.asSequence()?.associateWith { hs.getString(it) } ?: emptyMap()
        val sa = m.optJSONArray("subs") ?: JSONArray()
        val tracks = (0 until sa.length()).map {
            val s = sa.getJSONObject(it)
            SubTrack(s.optString("lang"), s.optString("label"), "file://" + s.optString("file"))
        }.filter { File(it.url.removePrefix("file://")).exists() }
        return Loaded(SourceInfo(m.optString("m3u8"), m.optString("referer"), headers, tracks, "", emptyList()), tracks)
    }
}
