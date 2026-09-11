package com.dockeredly.app.runtime

/**
 * Trivial per-slot subclasses of [WebAppRuntimeActivity], each declared in the manifest
 * with a distinct `android:process`. See [com.dockeredly.app.browser.core.BrowserProcessSlots]
 * for why Chromium web apps need dedicated processes to get isolated persistent storage.
 */
class WebAppRuntimeActivitySlot0 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot1 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot2 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot3 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot4 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot5 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot6 : WebAppRuntimeActivity()
class WebAppRuntimeActivitySlot7 : WebAppRuntimeActivity()
