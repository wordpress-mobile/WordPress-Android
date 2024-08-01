package org.wordpress.android.util.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.common.io.Files
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun Uri.fileSize(context: Context): Long {
    val assetFileDescriptor = try {
        context.contentResolver.openAssetFileDescriptor(this, "r")
    } catch (e: FileNotFoundException) {
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
                // maybe shouldn't trust ContentResolver for size: https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use 0L
                }
                cursor.moveToFirst()
                return try {
                    cursor.getLong(sizeIndex)
                } catch (_: Throwable) {
                    0L
                }
            } ?: 0L
    } else {
        return 0L
    }
}

fun Uri.copyToTempFile(mimeType: String, context: Context): File? {
    this.fileName(context)?.let { name ->
        try {
            var extension = mimeType.substringAfterLast("/")
            if (extension.isEmpty() || extension == "*") {
                extension = "tmp"
            }
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
            return null
        }
    } ?: return null
}

fun Uri.mimeType(context: Context): String {
    return context.contentResolver.getType(this) ?: ""
}

fun Uri.sizeFmt(context: Context): String {
    return android.text.format.Formatter.formatShortFileSize(
        context,
        fileSize(context)
    )
}

fun Uri.fileName(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(nameIndex)
    }
}
