package app.ani.data

import android.content.Context
import app.ani.BuildConfig
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs resolver.js inside QuickJS. The app knows nothing about the site: it only injects
 * http.get and a few byte helpers, then calls search / episodes / sources / info / findSubs.
 * Load order: remote RESOLVER_URL (if set), cached copy in filesDir, bundled asset.
 */
class Resolver(private val ctx: Context, baseClient: OkHttpClient) {
    private val client = baseClient.newBuilder()
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Headers for poster requests, declared by resolver.js (imageHeaders). */
    @Volatile var imageHeaders: Map<String, String> = emptyMap()
        private set

    private val cacheFile get() = File(ctx.filesDir, "resolver.js")
    private val metaFile get() = File(ctx.filesDir, "resolver.meta")

    @Volatile private var script: String? = null
    private val _status = MutableStateFlow(ResolverStatus())
    val status: StateFlow<ResolverStatus> = _status.asStateFlow()

    /** Pick the best available script. Safe to call more than once. */
    suspend fun init() {
        if (script != null) return
        if (BuildConfig.RESOLVER_URL.isNotBlank()) refresh()
        if (script != null) { loadImageHeaders(); return }
        val cached = runCatching { cacheFile.readText() }.getOrNull()
        if (!cached.isNullOrBlank()) {
            val v = versionOf(cached)
            if (v != null) {
                script = cached
                _status.value = ResolverStatus("cached", v, cacheFile.lastModified(), null)
                loadImageHeaders()
                return
            }
        }
        val bundled = ctx.assets.open("resolver.js").bufferedReader().use { it.readText() }
        script = bundled
        _status.value = ResolverStatus("bundled", versionOf(bundled) ?: 0, 0L, null)
        loadImageHeaders()
    }

    private suspend fun loadImageHeaders() {
        try {
            val o = JSONObject(call("(typeof imageHeaders === 'undefined' ? {} : imageHeaders)", skipInit = true))
            imageHeaders = o.keys().asSequence().associateWith { o.getString(it) }
        } catch (e: Exception) {
            imageHeaders = emptyMap()
        }
    }

    /** Fetch RESOLVER_URL, validate, cache. Returns an error message or null on success. */
    suspend fun refresh(): String? {
        val url = BuildConfig.RESOLVER_URL
        if (url.isBlank()) return "No update URL is set in this build"
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
                val text = client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@withContext "HTTP ${r.code}"
                    r.body.string()
                }
                val v = versionOf(text) ?: return@withContext "Downloaded file is not a valid resolver"
                cacheFile.writeText(text)
                script = text
                _status.value = ResolverStatus("remote", v, System.currentTimeMillis(), null)
                null
            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                _status.value = _status.value.copy(error = msg)
                msg
            }
        }
    }

    private suspend fun versionOf(text: String): Int? = try {
        val js = QuickJs.create(Dispatchers.Default)
        try {
            js.evaluate<Any?>(PRELUDE + "\n" + text)
            js.evaluate<Int>("version")
        } finally {
            js.close()
        }
    } catch (e: Exception) {
        null
    }

    private fun httpGet(url: String, headersJson: String): String {
        val b = Request.Builder().url(url)
        val h = JSONObject(headersJson)
        for (k in h.keys()) b.header(k, h.getString(k))
        client.newCall(b.build()).execute().use { r ->
            val o = JSONObject()
            o.put("status", r.code)
            o.put("body", r.body.string())
            o.put("url", r.request.url.toString())
            return o.toString()
        }
    }

    /** Evaluate `expr` (a JS expression, may await) and return its JSON string. */
    private suspend fun call(expr: String, skipInit: Boolean = false): String {
        if (!skipInit) init()
        val src = script ?: error("Resolver not loaded")
        val js = QuickJs.create(Dispatchers.Default)
        try {
            js.asyncFunction("__http_get") { args ->
                val url = args[0].toString()
                val hdr = args[1].toString()
                withContext(Dispatchers.IO) { httpGet(url, hdr) }
            }
            js.evaluate<Any?>(PRELUDE + "\n" + src)
            return js.evaluate<String>("JSON.stringify(await $expr)")
        } finally {
            js.close()
        }
    }

    private fun q(s: String) = JSONObject.quote(s)

    suspend fun search(query: String, mode: String): SearchOut {
        val o = JSONObject(call("search(${q(query)}, ${q(mode)})"))
        val results = o.getJSONArray("results").objs().map {
            SearchResult(it.getString("id"), it.getString("name"), it.optString("thumb", ""))
        }
        val groups = o.getJSONArray("groups").objs().map { g ->
            ShowGroup(
                g.getString("key"), g.getString("name"), g.optString("thumb", ""),
                g.getJSONArray("seasons").objs().map { s ->
                    SeasonRef(
                        s.getString("id"), s.getString("name"), s.optString("thumb", ""),
                        s.getString("kind"), s.optInt("num", 0), s.getString("label"),
                    )
                },
            )
        }
        return SearchOut(results, groups)
    }

    suspend fun episodes(showId: String, mode: String): List<EpisodeRef> =
        JSONArray(call("episodes(${q(showId)}, ${q(mode)})")).objs()
            .map { EpisodeRef(it.getString("number"), it.getString("epId")) }

    suspend fun sources(showId: String, ep: String, mode: String): SourceInfo {
        val o = JSONObject(call("sources(${q(showId)}, ${q(ep)}, ${q(mode)})"))
        val hs = o.getJSONObject("headers")
        val headers = hs.keys().asSequence().associateWith { hs.getString(it) }
        return SourceInfo(
            m3u8 = o.getString("m3u8"),
            referer = o.getString("referer"),
            headers = headers,
            subtitles = o.optJSONArray("subtitles")?.objs()?.map {
                SubTrack(it.optString("lang"), it.optString("label"), it.getString("url"))
            } ?: emptyList(),
            audio = o.optString("audio", ""),
            qualities = o.optJSONArray("qualities")?.objs()?.map {
                Quality(it.getString("label"), it.optInt("height", 0), it.getString("url"))
            } ?: emptyList(),
        )
    }

    suspend fun info(title: String): ShowMeta? {
        val o = JSONObject(call("info(${q(title)})"))
        if (!o.has("name")) return null
        return ShowMeta(
            o.optString("name"), o.optString("year"), o.optString("rating"),
            o.optString("desc"),
            o.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            o.optString("bg"),
            epTitles = parseEpField(o.optJSONObject("eps"), "t"),
            epThumbs = parseEpField(o.optJSONObject("eps"), "th"),
        )
    }

    suspend fun findSubs(title: String, season: Int, ep: String, count: Int, offset: Int): List<ExtraSub> {
        val arg = JSONObject()
            .put("title", title).put("season", season).put("ep", ep)
            .put("count", count).put("offset", offset).toString()
        val o = JSONObject(call("findSubs($arg)"))
        return o.optJSONArray("subs")?.objs()?.map {
            ExtraSub(it.getString("lang"), it.getString("label"), it.getString("url"), it.optInt("count", 1))
        } ?: emptyList()
    }

    /** Raw subtitle text -> WebVTT (conversion rules live in resolver.js). */
    suspend fun toVtt(text: String): String =
        JSONObject("{\"v\":" + call("toVtt(${q(text)})") + "}").getString("v")

    private fun parseEpField(eps: JSONObject?, field: String): Map<Int, Map<String, String>> {
        if (eps == null) return emptyMap()
        val out = HashMap<Int, Map<String, String>>()
        for (s in eps.keys()) {
            val m = eps.getJSONObject(s)
            val inner = HashMap<String, String>()
            for (e in m.keys()) inner[e] = m.getJSONObject(e).optString(field, "")
            out[s.toIntOrNull() ?: continue] = inner
        }
        return out
    }

    private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    companion object {
        /** Host helpers injected before resolver.js. Contains no site-specific knowledge. */
        private const val PRELUDE = """
var http = { get: async function (url, headers) {
  return JSON.parse(await __http_get(String(url), JSON.stringify(headers || {})));
} };
var util = (function () {
  var T = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  var M = {}; for (var i = 0; i < 64; i++) M[T.charAt(i)] = i; M['-'] = 62; M['_'] = 63;
  function b64decode(s) {
    s = String(s).replace(/[^A-Za-z0-9+\/_-]/g, '');
    var out = [], b = 0, n = 0;
    for (var i = 0; i < s.length; i++) { b = (b << 6) | M[s.charAt(i)]; n += 6; if (n >= 8) { n -= 8; out.push((b >> n) & 255); b &= (1 << n) - 1; } }
    return out;
  }
  function utf8(a) {
    var s = '', i = 0;
    while (i < a.length) {
      var c = a[i++];
      if (c < 128) s += String.fromCharCode(c);
      else if (c < 224) s += String.fromCharCode(((c & 31) << 6) | (a[i++] & 63));
      else if (c < 240) s += String.fromCharCode(((c & 15) << 12) | ((a[i++] & 63) << 6) | (a[i++] & 63));
      else { var cp = ((c & 7) << 18) | ((a[i++] & 63) << 12) | ((a[i++] & 63) << 6) | (a[i++] & 63); cp -= 65536; s += String.fromCharCode(55296 + (cp >> 10), 56320 + (cp & 1023)); }
    }
    return s;
  }
  function xor(a, k) { var o = new Array(a.length); for (var i = 0; i < a.length; i++) o[i] = a[i] ^ k[i % k.length]; return o; }
  return { b64decode: b64decode, utf8: utf8, xor: xor };
})();
"""
    }
}
