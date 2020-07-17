package org.wordpress.android.fluxc.utils

data class MimeType(val type: Type, val subtype: Subtype, val extensions: List<String> = listOf()) {
    enum class Type(val value: String) {
        AUDIO("audio"),
        VIDEO("video"),
        IMAGE("image"),
        APPLICATION("application")
    }

    enum class Subtype(val value: String) {
        MPEG("mpeg"),
        MP4("mp4"),
        OGG("ogg"),
        X_WAV("x-wav"),
        QUICKTIME("quicktime"),
        X_MS_WMV("x-ms-wmv"),
        AVI("avi"),
        MP2P("mp2p"),
        THREE_GPP("3gpp"),
        THREE_GPP_2("3gpp2"),
        JPEG("jpeg"),
        PNG("png"),
        GIF("gif"),
        PDF("pdf"),
        MSWORD("msword"),
        DOCX("vnd.openxmlformats-officedocument.wordprocessingml.document"),
        MSPOWERPOINT("mspowerpoint"),
        PPTX("vnd.openxmlformats-officedocument.presentationml.presentation"),
        ODT("vnd.oasis.opendocument.text"),
        EXCEL("vnd.ms-excel"),
        XLSX("vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        KEYNOTE("keynote"),
        ZIP("zip")
    }

    override fun toString(): String {
        return "${type.value}/${subtype.value}"
    }
}
