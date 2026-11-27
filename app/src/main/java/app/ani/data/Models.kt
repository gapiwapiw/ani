package app.ani.data

data class SearchResult(val id: String, val name: String, val thumb: String)

data class SeasonRef(
    val id: String,
    val name: String,
    val thumb: String,
    val kind: String,
    val num: Int,
    val label: String,
)

data class ShowGroup(
    val key: String,
    val name: String,
    val thumb: String,
    val seasons: List<SeasonRef>,
)

data class SearchOut(val results: List<SearchResult>, val groups: List<ShowGroup>)

data class EpisodeRef(val number: String, val epId: String)

data class SubTrack(val lang: String, val label: String, val url: String)

data class Quality(val label: String, val height: Int, val url: String)

data class SourceInfo(
    val m3u8: String,
    val referer: String,
    val headers: Map<String, String>,
    val subtitles: List<SubTrack>,
    val audio: String,
    val qualities: List<Quality>,
)

data class ExtraSub(val lang: String, val label: String, val url: String, val count: Int)

data class ShowMeta(
    val name: String,
    val year: String,
    val rating: String,
    val desc: String,
    val genres: List<String>,
    val bg: String,
    /** season number -> (episode number -> title). */
    val epTitles: Map<Int, Map<String, String>> = emptyMap(),
)

data class ResolverStatus(
    val source: String = "bundled",
    val version: Int = 0,
    val updatedAt: Long = 0L,
    val error: String? = null,
)
