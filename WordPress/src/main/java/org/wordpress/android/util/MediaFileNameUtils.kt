package org.wordpress.android.util

/**
 * Helpers used to keep the name of a media file when the app writes a processed copy of it, e.g. when optimizing,
 * rotating or compressing it. Without them the media ends up renamed on the site, since it is named after the file
 * that gets uploaded. See https://github.com/wordpress-mobile/WordPress-Android/issues/20468
 */
object MediaFileNameUtils {
    /**
     * Combines the name of the original file with the extension of the processed one, since processing can change
     * the format of the media. Falls back to the original extension when the processed file has none, which happens
     * when the original name contains characters the extension detection can't handle, such as spaces.
     *
     * @return the name to give to the processed file, or null when the original name can't be reused
     */
    @JvmStatic
    fun buildProcessedFileName(originalName: String, processedName: String): String? {
        val baseName = baseName(originalName)
        if (baseName.isEmpty()) return null

        val extension = extension(processedName) ?: extension(originalName)
        return if (extension == null) baseName else "$baseName.$extension"
    }

    /**
     * @return [fileName] with its extension replaced by [extension]
     */
    @JvmStatic
    fun replaceExtension(fileName: String, extension: String): String = "${baseName(fileName)}.$extension"

    private fun baseName(fileName: String): String {
        val separatorIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR)
        // a leading separator marks a hidden file rather than an extension
        return if (separatorIndex > 0) fileName.substring(0, separatorIndex) else fileName
    }

    private fun extension(fileName: String): String? {
        val separatorIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR)
        return if (separatorIndex <= 0 || separatorIndex == fileName.lastIndex) {
            null
        } else {
            fileName.substring(separatorIndex + 1)
        }
    }

    private const val EXTENSION_SEPARATOR = '.'
}
