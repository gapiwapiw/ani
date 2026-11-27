package app.ani.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Watch progress for one season entry. Same shape as the web app: w maps episode -> (position, duration). */
data class Hist(
    val sid: String,
    val gk: String,
    val ep: String,
    val pos: Double,
    val dur: Double,
    val t: Long,
    val w: Map<String, Pair<Double, Double>>,
)

data class Prefs(
    val mode: String = "sub",
    val subSize: Int = 20,
    val speed: Float = 1f,
    val quality: String = "best",
    val subLang: String = "English",
    val dlLangs: List<String> = listOf("ara"),
)

/** All user data in one JSON file in filesDir (state.json). Observable through Compose state. */
class Store(private val file: File, private val scope: CoroutineScope) {
    var groups by mutableStateOf<Map<String, ShowGroup>>(emptyMap()); private set
    var hist by mutableStateOf<Map<String, Hist>>(emptyMap()); private set
    var list by mutableStateOf<List<String>>(emptyList()); private set
    var later by mutableStateOf<List<String>>(emptyList()); private set
    var recentQ by mutableStateOf<List<String>>(emptyList()); private set
    var recentA by mutableStateOf<List<String>>(emptyList()); private set
    var prefs by mutableStateOf(Prefs()); private set

    private val lock = Mutex()

    init { load() }

    // ---- queries ----
    fun group(key: String): ShowGroup? = groups[key]
    fun latestFor(g: ShowGroup): Hist? =
        g.seasons.mapNotNull { hist[it.id] }.maxByOrNull { it.t }

    // ---- changes ----
    fun remember(g: ShowGroup) {
        groups = groups + (g.key to g)
        save()
    }

    fun toggleList(key: String) { list = toggle(list, key); save() }
    fun toggleLater(key: String) { later = toggle(later, key); save() }
    private fun toggle(l: List<String>, k: String) = if (k in l) l - k else listOf(k) + l

    fun addQuery(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        recentQ = (listOf(t) + recentQ.filter { !it.equals(t, true) }).take(8)
        save()
    }

    fun clearQueries() { recentQ = emptyList(); save() }

    fun touchRecent(g: ShowGroup) {
        groups = groups + (g.key to g)
        recentA = (listOf(g.key) + recentA.filter { it != g.key }).take(20)
        save()
    }

    fun clearRecent() { recentA = emptyList(); save() }

    fun progress(g: ShowGroup, sid: String, ep: String, pos: Double, dur: Double) {
        if (dur <= 0.0 || dur.isNaN() || pos.isNaN()) return
        val old = hist[sid]
        val w = (old?.w ?: emptyMap()) + (ep to (pos to dur))
        hist = hist + (sid to Hist(sid, g.key, ep, pos, dur, System.currentTimeMillis(), w))
        if (!groups.containsKey(g.key)) groups = groups + (g.key to g)
        save()
    }

    fun setPrefs(p: Prefs) { prefs = p; save() }

    // ---- persistence ----
    private fun load() {
        try {
            if (!file.exists()) return
            val o = JSONObject(file.readText())
            groups = o.optJSONObject("groups")?.let { g ->
                g.keys().asSequence().associateWith { groupFrom(g.getJSONObject(it)) }
            } ?: emptyMap()
            hist = o.optJSONObject("hist")?.let { h ->
                h.keys().asSequence().associateWith { histFrom(it, h.getJSONObject(it)) }
            } ?: emptyMap()
            list = strings(o.optJSONArray("list"))
            later = strings(o.optJSONArray("later"))
            recentQ = strings(o.optJSONArray("rq"))
            recentA = strings(o.optJSONArray("ra"))
            o.optJSONObject("prefs")?.let { p ->
                val d = Prefs()
                prefs = Prefs(
                    mode = p.optString("mode", d.mode),
                    subSize = p.optInt("subSize", d.subSize),
                    speed = p.optDouble("speed", d.speed.toDouble()).toFloat(),
                    quality = p.optString("quality", d.quality),
                    subLang = p.optString("subLang", d.subLang),
                    dlLangs = strings(p.optJSONArray("dlLangs")).ifEmpty { d.dlLangs },
                )
            }
        } catch (e: Exception) {
            // A corrupt file must not stop the app. Start empty; the next save overwrites it.
        }
    }

    private fun save() {
        val text = toJson().toString()
        scope.launch(Dispatchers.IO) {
            lock.withLock {
                try {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(text)
                    if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
                } catch (e: Exception) { /* disk full etc: keep running with in-memory state */ }
            }
        }
    }

    private fun toJson(): JSONObject {
        val o = JSONObject()
        val g = JSONObject()
        for ((k, v) in groups) g.put(k, groupTo(v))
        o.put("groups", g)
        val h = JSONObject()
        for ((k, v) in hist) h.put(k, histTo(v))
        o.put("hist", h)
        o.put("list", JSONArray(list))
        o.put("later", JSONArray(later))
        o.put("rq", JSONArray(recentQ))
        o.put("ra", JSONArray(recentA))
        o.put(
            "prefs",
            JSONObject().put("mode", prefs.mode).put("subSize", prefs.subSize).put("speed", prefs.speed.toDouble())
                .put("quality", prefs.quality).put("subLang", prefs.subLang).put("dlLangs", JSONArray(prefs.dlLangs)),
        )
        return o
    }

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    private fun groupTo(g: ShowGroup) = JSONObject().put("key", g.key).put("name", g.name).put("thumb", g.thumb)
        .put(
            "seasons",
            JSONArray(
                g.seasons.map {
                    JSONObject().put("id", it.id).put("name", it.name).put("thumb", it.thumb)
                        .put("kind", it.kind).put("num", it.num).put("label", it.label)
                },
            ),
        )

    private fun groupFrom(o: JSONObject): ShowGroup {
        val a = o.getJSONArray("seasons")
        return ShowGroup(
            o.getString("key"), o.getString("name"), o.optString("thumb", ""),
            (0 until a.length()).map {
                val s = a.getJSONObject(it)
                SeasonRef(
                    s.getString("id"), s.getString("name"), s.optString("thumb", ""),
                    s.getString("kind"), s.optInt("num", 0), s.getString("label"),
                )
            },
        )
    }

    private fun histTo(h: Hist): JSONObject {
        val w = JSONObject()
        for ((k, v) in h.w) w.put(k, JSONArray(listOf(v.first, v.second)))
        return JSONObject().put("gk", h.gk).put("sid", h.sid).put("ep", h.ep).put("pos", h.pos)
            .put("dur", h.dur).put("t", h.t).put("w", w)
    }

    private fun histFrom(sid: String, o: JSONObject): Hist {
        val wj = o.optJSONObject("w")
        val w = HashMap<String, Pair<Double, Double>>()
        if (wj != null) for (k in wj.keys()) {
            val a = wj.getJSONArray(k)
            w[k] = a.getDouble(0) to a.getDouble(1)
        }
        return Hist(sid, o.optString("gk"), o.optString("ep"), o.optDouble("pos"), o.optDouble("dur"), o.optLong("t"), w)
    }
}
