package com.dockeredly.app.domain.model

/**
 * Where a web app's icon came from, in the preference order the creation flow tries:
 * a manifest/favicon fetched from the website, an image the user picked, or a Material
 * fallback icon when neither is available. [localPath] is always the on-disk, already
 * processed square icon file actually rendered in the UI once one has been resolved.
 */
sealed interface IconSource {
    data class WebsiteIcon(val sourceUrl: String, val localPath: String) : IconSource
    data class UserImage(val localPath: String) : IconSource
    data object Fallback : IconSource
}
