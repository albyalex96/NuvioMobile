package com.nuvio.app.features.downloads

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.app.core.logging.InAppLogger
import java.io.File
import java.io.FileInputStream

@Composable
actual fun rememberDownloadFileSaver(): (DownloadItem) -> Unit {
    val context = LocalContext.current
    var pendingItem by remember { mutableStateOf<DownloadItem?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        val item = pendingItem ?: return@rememberLauncherForActivityResult
        pendingItem = null
        if (uri != null) {
            copyToDestination(context, item.localFileUri ?: "", uri)
        }
    }

    return { item ->
        pendingItem = item
        val fileName = deriveSaveFileName(item)
        launcher.launch(fileName)
    }
}

private fun deriveSaveFileName(item: DownloadItem): String {
    val baseName = item.fileName.substringBeforeLast('.')
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .takeIf { it.isNotBlank() } ?: "video"
    return "$baseName.mp4"
}

private fun copyToDestination(context: Context, sourceUri: String, destUri: Uri) {
    try {
        val sourceScheme = Uri.parse(sourceUri).scheme
        if (sourceScheme == "content") {
            context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            val file = File(Uri.parse(sourceUri).path ?: return)
            val fis = FileInputStream(file)
            fis.use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }
    } catch (e: Exception) {
        InAppLogger.error("DownloadFileSaver", "save failed: ${e.message}")
    }
}
