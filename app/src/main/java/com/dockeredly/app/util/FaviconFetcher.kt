package com.dockeredly.app.util

import android.content.Context
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Best-effort retrieval of a website's own icon, preferring (in order) a web app manifest
 * icon, an HTML `<link rel="icon"|"apple-touch-icon">`, then `/favicon.ico`. Runs on a
 * background dispatcher and never throws - any failure just means the caller falls back
 * to a user-picked image or a Material icon, per the app's icon preference order.
 */
object FaviconFetcher {
    data class Result(val localPath: String, val sourceUrl: String)

    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 6000
    private const val MAX_HTML_BYTES = 200_000

    suspend fun fetch(context: Context, pageUrl: String, webAppId: String): Result? =
        withContext(Dispatchers.IO) {
            val origin = UrlValidator.originOf(pageUrl) ?: return@withContext null
            val html = fetchText(pageUrl, MAX_HTML_BYTES)

            val manifestIconUrl = html?.let { findManifestIconUrl(it, origin) }
            val linkIconUrl = html?.let { findLinkIconUrl(it, origin) }

            val candidates = listOfNotNull(manifestIconUrl, linkIconUrl, "$origin/favicon.ico")
            for (candidate in candidates) {
                val bytes = fetchBytes(candidate) ?: continue
                val localPath = IconProcessor.processBytes(context, bytes, webAppId) ?: continue
                return@withContext Result(localPath, candidate)
            }
            null
        }

    private fun resolve(origin: String, ref: String): String? = runCatching {
        URI(origin).resolve(ref).toString()
    }.getOrNull()

    private fun findLinkIconUrl(html: String, origin: String): String? {
        val linkRegex = Regex("""<link\s+[^>]*rel=["']([^"']*icon[^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val hrefRegex = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = linkRegex.findAll(html).firstOrNull() ?: return null
        val href = hrefRegex.find(match.value)?.groupValues?.get(1) ?: return null
        return resolve(origin, href)
    }

    private fun findManifestIconUrl(html: String, origin: String): String? {
        val manifestRegex = Regex(
            """<link\s+[^>]*rel=["']manifest["'][^>]*href=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        val manifestPath = manifestRegex.find(html)?.groupValues?.get(1) ?: return null
        val manifestUrl = resolve(origin, manifestPath) ?: return null
        val manifestText = fetchText(manifestUrl, MAX_HTML_BYTES) ?: return null
        return runCatching {
            val icons = JSONObject(manifestText).optJSONArray("icons") ?: return null
            var bestUrl: String? = null
            var bestSize = -1
            for (i in 0 until icons.length()) {
                val icon = icons.optJSONObject(i) ?: continue
                val src = icon.optString("src").takeIf { it.isNotBlank() } ?: continue
                val size = icon.optString("sizes")
                    .split(" ", "x").firstOrNull { it.toIntOrNull() != null }
                    ?.toIntOrNull() ?: 0
                if (size >= bestSize) {
                    bestSize = size
                    bestUrl = src
                }
            }
            bestUrl?.let { resolve(manifestUrl, it) }
        }.getOrNull()
    }

    private fun fetchText(url: String, maxBytes: Int): String? = runCatching {
        val connection = openConnection(url)
        try {
            connection.inputStream.use { readUpTo(it, maxBytes) }.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun readUpTo(stream: java.io.InputStream, maxBytes: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val read = stream.read(chunk, 0, minOf(chunk.size, maxBytes - total))
            if (read == -1) break
            buffer.write(chunk, 0, read)
            total += read
        }
        return buffer.toByteArray()
    }

    private fun fetchBytes(url: String): ByteArray? = runCatching {
        val connection = openConnection(url)
        try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Dockeredly icon fetcher)")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("HTTP ${connection.responseCode}")
        }
        return connection
    }
}
