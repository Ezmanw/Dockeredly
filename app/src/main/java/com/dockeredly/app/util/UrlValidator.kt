package com.dockeredly.app.util

import android.util.Patterns
import java.net.URI
import java.net.URISyntaxException

sealed interface UrlValidationResult {
    data class Valid(val normalizedUrl: String) : UrlValidationResult
    data object Empty : UrlValidationResult
    data object Invalid : UrlValidationResult
    data object UnsupportedScheme : UrlValidationResult
}

/**
 * Validates and normalizes a user-entered website address. A missing scheme defaults to
 * https; only http/https are accepted since this app embeds web content, not arbitrary
 * intents, at creation time.
 */
object UrlValidator {
    fun validate(input: String): UrlValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Empty

        val candidate = if (!trimmed.contains("://")) "https://$trimmed" else trimmed

        val uri = try {
            URI(candidate)
        } catch (e: URISyntaxException) {
            return UrlValidationResult.Invalid
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return UrlValidationResult.UnsupportedScheme

        val host = uri.host
        if (host.isNullOrBlank() || !Patterns.WEB_URL.matcher(candidate).matches()) {
            return UrlValidationResult.Invalid
        }

        return UrlValidationResult.Valid(candidate)
    }

    /** A friendly display name derived from the host, used when the user leaves the name blank. */
    fun deriveNameFromUrl(url: String): String {
        val host = runCatching { URI(url).host }.getOrNull() ?: return "Web App"
        val stripped = host.removePrefix("www.")
        val base = stripped.substringBefore(".")
        return base.replaceFirstChar { it.uppercase() }
    }

    fun originOf(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "$scheme://$host$port"
    }
}
