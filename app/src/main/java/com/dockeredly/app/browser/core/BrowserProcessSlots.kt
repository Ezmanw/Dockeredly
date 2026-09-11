package com.dockeredly.app.browser.core

import kotlin.math.abs

/**
 * Android's WebView only supports ONE active data-directory suffix per process
 * ([android.webkit.WebView.setDataDirectorySuffix] must be called once, before any
 * WebView is created, and applies to every WebView created in that process for its
 * lifetime). There is no public WebView API - unlike GeckoView's `contextId` - for
 * partitioning storage per-instance within a single process.
 *
 * To still give Chromium web apps real, OS-enforced storage isolation from each other,
 * each is launched into one of a fixed pool of dedicated processes (see the
 * `WebAppRuntimeActivitySlotN` classes and their `android:process` manifest entries),
 * chosen by hashing the web app's stable profile id. This is a bounded approximation,
 * not perfect 1:1 isolation: two web apps that hash to the same slot share that slot's
 * WebView data directory if both are ever opened. [SLOT_COUNT] trades process/memory
 * overhead against collision odds; it is not a correctness issue for the common case of
 * one foreground web app at a time.
 */
object BrowserProcessSlots {
    const val SLOT_COUNT = 8

    fun slotForProfile(profileId: String): Int = abs(profileId.hashCode()) % SLOT_COUNT

    fun processSuffixForSlot(slot: Int): String = "webapp$slot"

    /** Extracts the slot suffix from `getProcessName()`, e.g. "com.dockeredly.app:webapp3" -> "webapp3". */
    fun suffixFromProcessName(processName: String, packageName: String): String? {
        val marker = "$packageName:"
        if (!processName.startsWith(marker)) return null
        val suffix = processName.removePrefix(marker)
        return suffix.takeIf { it.matches(Regex("webapp\\d+")) }
    }
}
