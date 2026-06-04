package com.nuvio.app.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal actual object SharePlatform {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun shareFile(filePath: String, title: String, mimeType: String) {
        val context = appContext ?: return
        val uri = resolveShareUri(context, filePath) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    private fun resolveShareUri(context: Context, filePath: String): Uri? {
        val uri = Uri.parse(filePath)
        return when {
            uri.scheme == "content" -> uri
            uri.scheme == "file" -> resolveFileShareUri(context, uri.path ?: return null)
            else -> resolveFileShareUri(context, filePath)
        }
    }

    private fun resolveFileShareUri(context: Context, path: String): Uri? {
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }
}
