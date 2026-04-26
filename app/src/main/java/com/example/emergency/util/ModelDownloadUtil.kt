package com.example.emergency.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

object ModelDownloadUtil {
    fun downloadModel(context: Context, url: String, destFile: File): Long {
        // Ensure parent directory exists
        destFile.parentFile?.mkdirs()
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Downloading model weights")
            setDescription("Downloading LLM weights for Mark")
            // Use the correct API for app-specific external storage
            setDestinationInExternalFilesDir(context, null, destFile.name)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
