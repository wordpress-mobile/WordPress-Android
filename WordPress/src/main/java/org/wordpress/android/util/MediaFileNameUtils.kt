package org.wordpress.android.util

/**
 * Helpers used to keep the name of a media file when the app writes a processed copy of it, e.g. when optimizing,
 * rotating or compressing it. Without them the media ends up renamed on the site, since it is named after the file
 * that gets uploaded. See https://github.com/wordpress-mobile/WordPress-Android/issues/20468
 */
object MediaFileNameUtils {
    /**
     * Combines the name of the original file with the extension of the processed one, since processing can change
     * the format of the media. The extension is taken verbatim, including the empty one the processing steps produce
     * when they can't detect a format: the original extension would then describe the wrong format, while an empty
     * extension is repaired from the mime type by `FluxCUtils.mediaModelFromLocalUri`, as it already is today.
     *
     * @return the name to give to the processed file, or null when the original name can't be reused
     */
    @JvmStatic
    fun buildProcessedFileName(originalName: String, processedName: String): String? {
        val baseName = baseName(originalName)
        if (baseName.isEmpty()) return null

        return baseName + extensionSuffix(processedName, fallback = extensionSuffix(originalName))
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

    /**
     * @return everything from the last extension separator of [fileName] on, e.g. ".jpg" or the bare "." left by a
     * processing step that couldn't detect the format, or [fallback] when [fileName] carries no separator at all
     */
    private fun extensionSuffix(fileName: String, fallback: String = ""): String {
        val separatorIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR)
        // a leading separator marks a hidden file rather than an extension
        return if (separatorIndex > 0) fileName.substring(separatorIndex) else fallback
    }

    private const val EXTENSION_SEPARATOR = '.'
}
