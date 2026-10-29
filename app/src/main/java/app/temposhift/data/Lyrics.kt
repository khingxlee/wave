package app.temposhift.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

const val LYR_IDLE = 0
const val LYR_LOADING = 1
const val LYR_SYNCED = 2
const val LYR_PLAIN = 3
const val LYR_NONE = 4
const val LYR_ERROR = 5

data class LyricLine(val timeMs: Long, val text: String)

/** Lyrics as received: timed text in LRC format ([syncedRaw]) and/or plain text. */
class LyricsData(val syncedRaw: String, val plain: String) {
    val synced: List<LyricLine> by lazy { parseLrc(syncedRaw) }
}

private val TIMESTAMP = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

fun parseLrc(text: String): List<LyricLine> {
    val out = ArrayList<LyricLine>()
    for (raw in text.lines()) {
        val stamps = TIMESTAMP.findAll(raw).toList()
        if (stamps.isEmpty()) continue
        val body = raw.substring(stamps.last().range.last + 1).trim()
        for (m in stamps) {
            val minutes = m.groupValues[1].toLong()
            val seconds = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val millis = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
            out.add(LyricLine(minutes * 60000L + seconds * 1000L + millis, body))
        }
    }
    out.sortBy { it.timeMs }
    return out
}

/** Looks lyrics up on LRCLIB (free, no account needed). */
object Lyrics {
    private const val BASE = "https://lrclib.net/api/"

    /**
     * Returns the lyrics found (possibly empty when the song has none) or null when the service could not be reached.
     * Pass an empty [artist] when the artist is unknown.
     */
    fun fetch(title: String, artist: String, album: String, durationSec: Int): LyricsData? {
        return try {
            if (artist.isNotBlank()) {
                val q = StringBuilder("track_name=").append(enc(title)).append("&artist_name=").append(enc(artist))
                if (album.isNotBlank()) q.append("&album_name=").append(enc(album))
                if (durationSec > 0) q.append("&duration=").append(durationSec)
                val body = get(BASE + "get?" + q)
                if (body != null) {
                    val d = fromJson(JSONObject(body))
                    if (!isEmpty(d)) return d
                }
            }
            val query = if (artist.isNotBlank()) "$artist $title" else title
            val arr = JSONArray(get(BASE + "search?q=" + enc(query)) ?: "[]")
            var best: LyricsData? = null
            for (i in 0 until arr.length()) {
                val d = fromJson(arr.getJSONObject(i))
                if (isEmpty(d)) continue
                if (d.syncedRaw.isNotBlank()) return d
                if (best == null) best = d
            }
            best ?: LyricsData("", "")
        } catch (_: Exception) {
            null
        }
    }

    private fun isEmpty(d: LyricsData) = d.syncedRaw.isBlank() && d.plain.isBlank()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun str(o: JSONObject, key: String): String = if (o.isNull(key)) "" else o.optString(key, "")

    private fun fromJson(o: JSONObject) = LyricsData(str(o, "syncedLyrics"), str(o, "plainLyrics"))

    /** Tries up to three times with short pauses, because the first request on a phone often fails and the next one works. */
    private fun get(url: String): String? {
        var last: java.io.IOException? = null
        for (attempt in 0 until 3) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 6000
                c.readTimeout = 9000
                c.setRequestProperty("User-Agent", "Wave/6.0 (Android)")
                c.setRequestProperty("Accept", "application/json")
                val code = c.responseCode
                if (code == 404) return null
                if (code !in 200..299) throw java.io.IOException("HTTP $code")
                return c.inputStream.bufferedReader().use { it.readText() }
            } catch (e: java.io.IOException) {
                last = e
            } finally {
                c.disconnect()
            }
            try { Thread.sleep(350L * (attempt + 1)) } catch (_: InterruptedException) { }
        }
        throw last ?: java.io.IOException("network")
    }
}
