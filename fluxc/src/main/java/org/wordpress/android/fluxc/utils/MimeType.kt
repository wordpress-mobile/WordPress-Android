package org.wordpress.android.fluxc.utils

data class MimeType(val type: Type, val subtypes: List<Subtype>, val extensions: List<String> = listOf()) {
    constructor(type: Type, subtype: Subtype, extensions: List<String>) : this(
            type,
            listOf<Subtype>(subtype),
            extensions
    )

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
        DOC("doc"),
        MSDOC("ms-doc"),
        MSWORD("msword"),
        DOCX("vnd.openxmlformats-officedocument.wordprocessingml.document"),
        POWERPOINT("powerpoint"),
        MSPOWERPOINT("mspowerpoint"),
        VND_MSPOWERPOINT("vnd.ms-powerpoint"),
        X_MSPOWERPOINT("x-mspowerpoint"),
        PPTX("vnd.openxmlformats-officedocument.presentationml.presentation"),
        PPSX("vnd.openxmlformats-officedocument.presentationml.slideshow"),
        ODT("vnd.oasis.opendocument.text"),
        EXCEL("excel"),
        X_EXCEL("x-excel"),
        X_MS_EXCEL("x-msexcel"),
        VND_MS_EXCEL("vnd.ms-excel"),
        XLSX("vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        KEYNOTE("keynote"),
        ZIP("zip")
    }

    override fun toString(): String {
        return "${type.value}/${subtypes.first().value}"
    }
}
