package app.ani.data

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import app.ani.Graph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** Everything the player needs for one episode. */
data class Loaded(val info: SourceInfo, val tracks: List<SubTrack>)

@UnstableApi
object Playback {
    /** Network only, with the resolver's headers. Used to prepare downloads. */
    fun upstreamFactory(headers: Map<String, String>): DataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(headers)

    /** Playback data source: downloaded data first (read-only cache), network with the resolver's headers otherwise. */
    fun dataSourceFactory(headers: Map<String, String>): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(headers)
        return CacheDataSource.Factory()
            .setCache(Graph.cache)
            .setUpstreamDataSourceFactory(http)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Resolve the stream and look for extra subtitle languages (best effort, 5 s). */
    suspend fun load(g: ShowGroup, season: SeasonRef, ep: String, mode: String): Loaded = coroutineScope {
        Downloads.localLoaded(season.id, ep, mode)?.let { return@coroutineScope it }
        val extra = async {
            if (season.kind != "season" || season.num <= 0) emptyList()
            else withTimeoutOrNull(5_000) {
                runCatching { Graph.resolver.findSubs(g.name, season.num, ep, 0, 0) }.getOrDefault(emptyList())
            } ?: emptyList()
        }
        val info = Graph.repo.sources(season.id, ep, mode)
        val tracks = info.subtitles + extra.await().map { SubTrack(it.lang, it.label, it.url) }
        Loaded(info, tracks)
    }
}
