package com.dockeredly.app.util

import android.net.Uri

/**
 * Decides whether a navigation target should stay inside the web app's embedded engine or
 * be handed off to another Android app. Ordinary http/https navigation always stays
 * in-app; schemes another app must own (mail, telephony, market, arbitrary intents, or a
 * scheme the engine itself can't render) are handed off.
 */
object ExternalLinkClassifier {
    private val inAppSchemes = setOf("http", "https", "data", "blob", "about")
    private val externalSchemes = setOf(
        "mailto", "tel", "sms", "smsto", "mms", "mmsto", "geo", "market", "intent", "whatsapp", "tg",
    )

    fun shouldOpenExternally(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        if (scheme in inAppSchemes) return false
        if (scheme in externalSchemes) return true
        // Any other custom scheme (a deep link into a third-party app) can't be
        // rendered by the embedded engine, so it must be handed off.
        return true
    }
}
