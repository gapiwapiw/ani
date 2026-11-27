package app.ani.data

/** Thin cache over the resolver so screens do not refetch the same lists. */
class Repo(val resolver: Resolver) {
    private val eps = HashMap<String, List<EpisodeRef>>()
    private val metas = HashMap<String, ShowMeta?>()

    suspend fun search(q: String, mode: String): SearchOut = resolver.search(q, mode)

    suspend fun episodes(id: String, mode: String): List<EpisodeRef> {
        val k = "$id|$mode"
        eps[k]?.let { return it }
        val r = resolver.episodes(id, mode)
        eps[k] = r
        return r
    }

    suspend fun info(title: String): ShowMeta? {
        if (metas.containsKey(title)) return metas[title]
        val m = try { resolver.info(title) } catch (e: Exception) { null }
        metas[title] = m
        return m
    }

    suspend fun sources(id: String, ep: String, mode: String): SourceInfo = resolver.sources(id, ep, mode)
}
