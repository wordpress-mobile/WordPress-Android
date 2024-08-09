package org.wordpress.android.util.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.google.common.io.Files
import org.wordpress.android.util.AppLog
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Returns the file size in bytes from a Uri
 */
@Suppress("ReturnCount")
fun Uri.fileSize(context: Context): Long {
    val assetFileDescriptor = try {
        context.contentResolver.openAssetFileDescriptor(this, "r")
    } catch (e: FileNotFoundException) {
        AppLog.e(AppLog.T.UTILS, e)
        null
    }

    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: 0L
    if (length != 0L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return context.contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                // maybe shouldn't trust ContentResolver for size:
                // https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use 0L
                }
                cursor.moveToFirst()
                return cursor.getLong(sizeIndex)
            } ?: return 0L
    } else {
        return 0L
    }
}

/**
 * Attempts to determine the file extension from a Uri
 */
@Suppress("ReturnCount")
fun Uri.fileExtension(context: Context, defaultExtension: String = "tmp"): String {
    MimeTypeMap.getFileExtensionFromUrl(this.toString())?.let {
        return it
    }
    val mimeType = this.mimeType(context)
    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let {
        return it
    }
    return defaultExtension
}

/**
 * Copies the Uri to a temporary file and returns the file
 */
@Suppress("ReturnCount", "NestedBlockDepth")
fun Uri.copyToTempFile(context: Context): File? {
    this.fileName(context)?.let { name ->
        try {
            val extension = this.fileExtension(context)
            @Suppress("UnstableApiUsage") val file = File.createTempFile(
                Files.getNameWithoutExtension(name),
                ".$extension"
            )
            context.contentResolver.openInputStream(this).use { inputStream ->
                inputStream?.let {
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    return file
                } ?: return null
            }
        } catch (e: IOException) {
            AppLog.e(AppLog.T.UTILS, e)
            return null
        }
    } ?: return null
}

fun Uri.mimeType(context: Context): String {
    return if (this.scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.getType(this) ?: ""
    } else {
        val extension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }
}

/**
 * Returns a human-readable file size, ex: "1.5 MB"
 */
fun Uri.sizeFmt(context: Context): String {
    return android.text.format.Formatter.formatShortFileSize(
        context,
        fileSize(context)
    )
}

/**
 * Returns the file name from a Uri without any path info
 */
fun Uri.fileName(context: Context): String? {
    if (this.scheme == ContentResolver.SCHEME_CONTENT) {
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
    }
    return path?.substringAfterLast("/")
}
