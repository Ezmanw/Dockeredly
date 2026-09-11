package com.dockeredly.app.browser.chromium

import android.content.Context

/**
 * A profile's clear-cache/reset-data request made while its WebView isn't live in this
 * process (e.g. from the Library or Settings screen) can't be applied immediately -
 * WebView's cookie/storage APIs only operate on the current process. Instead the request
 * is recorded here and consumed the next time a [ChromiumEngine] for that profile is
 * created, applying it with the normal WebView APIs before any page loads.
 */
internal object ChromiumProfileOps {
    private const val PREFS_NAME = "chromium_profile_ops"
    private const val KEY_CLEAR_CACHE = "pending_clear_cache"
    private const val KEY_RESET = "pending_reset"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun markClearCache(context: Context, profileId: String) {
        addToSet(context, KEY_CLEAR_CACHE, profileId)
    }

    fun markReset(context: Context, profileId: String) {
        addToSet(context, KEY_RESET, profileId)
    }

    /** Returns (shouldClearCache, shouldReset) and clears both flags for [profileId]. */
    fun consumePending(context: Context, profileId: String): Pair<Boolean, Boolean> {
        val p = prefs(context)
        val clearCacheSet = (p.getStringSet(KEY_CLEAR_CACHE, emptySet()) ?: emptySet()).toMutableSet()
        val resetSet = (p.getStringSet(KEY_RESET, emptySet()) ?: emptySet()).toMutableSet()
        val shouldClearCache = clearCacheSet.remove(profileId)
        val shouldReset = resetSet.remove(profileId)
        if (shouldClearCache || shouldReset) {
            p.edit()
                .putStringSet(KEY_CLEAR_CACHE, clearCacheSet)
                .putStringSet(KEY_RESET, resetSet)
                .apply()
        }
        return shouldClearCache to shouldReset
    }

    private fun addToSet(context: Context, key: String, value: String) {
        val p = prefs(context)
        val set = (p.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        set.add(value)
        p.edit().putStringSet(key, set).apply()
    }
}
