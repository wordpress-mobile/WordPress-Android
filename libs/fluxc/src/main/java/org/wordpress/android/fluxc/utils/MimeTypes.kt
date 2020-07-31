package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.utils.MimeType.Subtype
import org.wordpress.android.fluxc.utils.MimeType.Type.APPLICATION
import org.wordpress.android.fluxc.utils.MimeType.Type.AUDIO
import org.wordpress.android.fluxc.utils.MimeType.Type.IMAGE
import org.wordpress.android.fluxc.utils.MimeType.Type.VIDEO

class MimeTypes {
    /*
     * The WordPress supported audio types based on https://wordpress.com/support/accepted-filetypes/ are:
     * .mp3, .m4a, .ogg, .wav
     * This translates (based on https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/MediaFile.java) to:
     * .mp3 - "audio/mpeg"
     * .m4a - "audio/mp4"
     * .ogg - "audio/ogg", "application/ogg"
     * .wav - "audio/x-wav"
     */
    private val audioTypes = listOf(
            MimeType(AUDIO, Subtype.MPEG, listOf("mp3")),
            MimeType(AUDIO, Subtype.MP4, listOf("m4a")),
            MimeType(AUDIO, Subtype.OGG, listOf("ogg")),
            MimeType(APPLICATION, Subtype.OGG, listOf("ogg")),
            MimeType(AUDIO, Subtype.X_WAV, listOf("wav"))
    )

    /*
     * The WordPress supported video types based on https://wordpress.com/support/accepted-filetypes/ are:
     * .mp4, .m4v (MPEG-4), .mov (QuickTime), .wmv (Windows Media Video), .avi, .mpg, .ogv (Ogg), .3gp (3GPP), .3g2 (3GPP2)
     * This translates (based on https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/MediaFile.java) to:
     * .mp4, .m4v (MPEG-4) - "video/mp4"
     * .mov - missing - using "video/quicktime"
     * .wmv - "video/x-ms-wmv"
     * .avi - "video/avi"
     * .mpg - "video/mpeg", "video/mp2p"
     * .ogv (Ogg) - missing - using "video/ogg"
     * .3gp (3GPP) - "video/3gpp"
     * .3g2 (3GPP2) - "video/3gpp2"
     */
    private val videoTypes = listOf(
            MimeType(VIDEO, Subtype.MP4, listOf("mp4", "m4v")),
            MimeType(VIDEO, Subtype.QUICKTIME, listOf("mov")),
            MimeType(VIDEO, Subtype.X_MS_WMV, listOf("wmv")),
            MimeType(VIDEO, Subtype.AVI, listOf("avi")),
            MimeType(VIDEO, Subtype.MPEG, listOf("mpg")),
            MimeType(VIDEO, Subtype.MP2P, listOf("mpg")),
            MimeType(VIDEO, Subtype.OGG, listOf("ogv")),
            MimeType(VIDEO, Subtype.THREE_GPP, listOf("3gp")),
            MimeType(VIDEO, Subtype.THREE_GPP_2, listOf("3g2"))
    )

    /*
     * The WordPress supported image types based on https://wordpress.com/support/accepted-filetypes/ are:
     * .jpg, .jpeg, .png, .gif
     * This translates (based on https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/MediaFile.java) to:
     * .jpg, .jpeg - "image/jpeg"
     * .png - "image/png"
     * .gif - "image/gif"
     */
    private val imageTypes = listOf(
            MimeType(IMAGE, Subtype.JPEG, listOf("jpg", "jpeg")),
            MimeType(IMAGE, Subtype.PNG, listOf("png")),
            MimeType(IMAGE, Subtype.GIF, listOf("gif"))
    )

    /*
     * The WordPress supported image types based on https://wordpress.com/support/accepted-filetypes/ are:
     * .pdf (Portable Document Format; Adobe Acrobat), .doc, .docx (Microsoft Word Document), .ppt, .pptx, .pps, .ppsx (Microsoft PowerPoint Presentation), .odt (OpenDocument Text Document), .xls, .xlsx (Microsoft Excel Document), .key (Apple Keynote Presentation), .zip (Archive File Format)
     * This translates (based on https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/MediaFile.java) to:
     * .pdf - "application/pdf"
     * .doc - "application/msword", "application/doc", "application/ms-doc"
     * .docx - "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
     * .ppt - "application/mspowerpoint", "application/powerpoint", "application/x-mspowerpoint"
     * .pptx - "application/vnd.openxmlformats-officedocument.presentationml.presentation"
     * .pps - missing - "application/vnd.ms-powerpoint"
     * .ppsx - missing - "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
     * .odt - missing - "application/vnd.oasis.opendocument.text"
     * .xls - "application/excel", "application/x-excel", "application/x-msexcel", "application/vnd.ms-excel"
     * .xlsx - "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
     * .key - missing - "application/keynote"
     * .zip (Archive File Format)
     */
    private val documentTypes = listOf(
            MimeType(APPLICATION, Subtype.PDF, listOf("pdf")),
            MimeType(APPLICATION, listOf(Subtype.MSWORD, Subtype.DOC, Subtype.MSDOC), listOf("doc")),
            MimeType(APPLICATION, Subtype.DOCX, listOf("docx")),
            MimeType(
                    APPLICATION,
                    listOf(Subtype.POWERPOINT, Subtype.MSPOWERPOINT, Subtype.X_MSPOWERPOINT),
                    listOf("ppt")
            ),
            MimeType(APPLICATION, Subtype.VND_MSPOWERPOINT, listOf("pps")),
            MimeType(APPLICATION, Subtype.PPTX, listOf("pptx")),
            MimeType(APPLICATION, Subtype.PPSX, listOf("ppsx")),
            MimeType(APPLICATION, Subtype.ODT, listOf("odt")),
            MimeType(
                    APPLICATION,
                    listOf(Subtype.EXCEL, Subtype.X_EXCEL, Subtype.VND_MS_EXCEL, Subtype.X_MS_EXCEL),
                    listOf("xls")
            ),
            MimeType(APPLICATION, Subtype.XLSX, listOf("xlsx")),
            MimeType(APPLICATION, Subtype.KEYNOTE, listOf("key")),
            MimeType(APPLICATION, Subtype.ZIP, listOf("zip"))
    )

    fun isAudioType(type: String?): Boolean {
        return isExpectedMimeType(audioTypes, type)
    }

    fun isVideoType(type: String?): Boolean {
        return isExpectedMimeType(videoTypes, type)
    }

    fun isImageType(type: String?): Boolean {
        return isExpectedMimeType(imageTypes, type)
    }

    fun isApplicationType(type: String?): Boolean {
        return isExpectedMimeType(documentTypes, type)
    }

    fun isSupportedAudioType(type: String?): Boolean {
        return isSupportedMimeType(audioTypes, type)
    }

    fun isSupportedVideoType(type: String?): Boolean {
        return isSupportedMimeType(videoTypes, type)
    }

    fun isSupportedImageType(type: String?): Boolean {
        return isSupportedMimeType(imageTypes, type)
    }

    fun isSupportedApplicationType(type: String?): Boolean {
        return isSupportedMimeType(documentTypes, type)
    }

    fun getAllTypes(): Array<String> {
        return (audioTypes.toStrings() + videoTypes.toStrings() + imageTypes.toStrings() + documentTypes.toStrings())
                .toSet()
                .toTypedArray()
    }

    private fun List<MimeType>.toStrings(): List<String> {
        return this.map { mimeType -> mimeType.subtypes.map { print(mimeType.type, it) } }.flatten()
    }

    private fun isExpectedMimeType(
        expected: List<MimeType>,
        type: String?
    ): Boolean {
        if (type == null) return false
        val split = type.split("/")
        return split.size == 2 && expected.any { it.type.value == split[0] }
    }

    private fun isSupportedMimeType(
        expected: List<MimeType>,
        type: String?
    ): Boolean {
        if (type == null) return false
        return expected.any { mimeType -> mimeType.subtypes.any { print(mimeType.type, it) == type } }
    }

    fun getMimeTypeForExtension(extension: String): String? {
        return (imageTypes + videoTypes + audioTypes + documentTypes).find { it.extensions.contains(extension) }
                ?.toString()
    }

    private fun print(type: MimeType.Type, subtype: Subtype) = "${type.value}/${subtype.value}"
}
