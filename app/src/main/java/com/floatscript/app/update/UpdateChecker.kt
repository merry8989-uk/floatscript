package com.floatscript.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads the releases from the public GitHub Releases API.
 *
 * Deliberately uses the *list* endpoint and picks the highest version that has an
 * APK asset, rather than trusting `/releases/latest`. GitHub defines "latest" as
 * the most recently *published* release, so a stray or re-published older tag
 * would otherwise make the app think it is up to date.
 *
 * Only a plain GET is made — no identifiers, no analytics.
 */
class UpdateChecker(
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val userAgent: String = "FloatScript"
) {

    suspend fun latest(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Result.success(pickHighest(body))
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    /** Highest-versioned release that ships an APK, or null when there is none. */
    internal fun pickHighest(json: String): UpdateInfo? {
        val releases: List<JSONObject> = when (val parsed = runCatching { JSONArray(json) }.getOrNull()) {
            null -> {
                // Fall back to a single-release payload.
                val single = runCatching { JSONObject(json) }.getOrNull() ?: return null
                listOf(single)
            }
            else -> (0 until parsed.length()).mapNotNull { parsed.optJSONObject(it) }
        }

        return releases
            .mapNotNull { toUpdateInfo(it) }
            .maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
    }

    private fun compareVersions(a: String, b: String): Int {
        val pa = AppVersion.parse(a)
        val pb = AppVersion.parse(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun toUpdateInfo(release: JSONObject): UpdateInfo? {
        if (release.optBoolean("draft", false)) return null
        val tag = release.optString("tag_name").ifBlank { return null }

        var apkUrl: String? = null
        var size = 0L
        val assets = release.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    size = asset.optLong("size", 0L)
                    break
                }
            }
        }
        if (apkUrl.isNullOrBlank()) return null

        return UpdateInfo(
            version = AppVersion.normalize(tag),
            tag = tag,
            notes = release.optString("body"),
            apkUrl = apkUrl,
            sizeBytes = size
        )
    }

    companion object {
        const val DEFAULT_ENDPOINT =
            "https://api.github.com/repos/merry8989-uk/floatscript/releases?per_page=30"
    }
}
