package com.dockeredly.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.dockeredly.app.browser.core.DownloadRequest

/**
 * Hands a web app's download request to Android's system DownloadManager, the standard,
 * user-visible way for an embedded browser engine to save a file without requesting
 * broad filesystem permissions.
 */
object DownloadHandler {
    fun enqueue(context: Context, request: DownloadRequest) {
        val uri = runCatching { Uri.parse(request.url) }.getOrNull() ?: return
        val fileName = URLUtil.guessFileName(request.url, request.contentDisposition, request.mimeType)

        val downloadRequest = DownloadManager.Request(uri).apply {
            setMimeType(request.mimeType ?: MimeTypeMap.getFileExtensionFromUrl(request.url)?.let {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
            })
            addRequestHeader("User-Agent", request.userAgent)
            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(request.url))
            setDescription(fileName)
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
        }

        runCatching {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(downloadRequest)
        }
    }
}
