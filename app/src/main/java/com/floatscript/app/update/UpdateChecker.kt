package com.floatscript.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads the latest release from the public GitHub Releases API.
 *
 * Only a plain GET is made — no identifiers, no analytics. Returns
 * `Result.success(null)` when the release has no APK asset to download.
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
            Result.success(parse(body))
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun parse(json: String): UpdateInfo? {
        val root = JSONObject(json)
        val tag = root.optString("tag_name").ifBlank { return null }

        var apkUrl: String? = null
        var size = 0L
        val assets = root.optJSONArray("assets")
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
            notes = root.optString("body"),
            apkUrl = apkUrl,
            sizeBytes = size
        )
    }

    companion object {
        const val DEFAULT_ENDPOINT =
            "https://api.github.com/repos/merry8989-uk/floatscript/releases/latest"
    }
}
