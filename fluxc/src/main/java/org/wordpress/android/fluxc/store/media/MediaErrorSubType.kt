package org.wordpress.android.fluxc.store.media

import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MediaErrorSubtypeCategory.MALFORMED_MEDIA_ARG_SUBTYPE
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MediaErrorSubtypeCategory.UNDEFINED_SUBTYPE

sealed class MediaErrorSubType(val category: MediaErrorSubtypeCategory, val subTypeName: String) {
    // This enum lists the categories of MediaErrorType that are mapped into one or multiple
    // MediaErrorSubType in the companion object categories field.
    // UNDEFINED_SUBTYPE category collects all MediaErrorType(s) that are not mapped yet.
    // To map another MediaErrorType, add an item to the enum and define a MediaErrorSubType element
    enum class MediaErrorSubtypeCategory {
        UNDEFINED_SUBTYPE,
        MALFORMED_MEDIA_ARG_SUBTYPE
    }

    fun serialize(): String {
        return "${category.name}:$subTypeName"
    }

    companion object {
        private val categories = MediaErrorSubtypeCategory.values().flatMap { value ->
            when (value) {
                UNDEFINED_SUBTYPE -> listOf(UndefinedSubType)
                MALFORMED_MEDIA_ARG_SUBTYPE -> Type.values().map { MalformedMediaArgSubType(it) }
            }
        }

        @JvmStatic
        @Suppress("ReturnCount")
        fun deserialize(name: String?): MediaErrorSubType {
            if (name == null) return UndefinedSubType

            categories.forEach { subType ->
                if (subType.serialize() == name) {
                    return subType
                }
            }

            return UndefinedSubType
        }
    }

    object UndefinedSubType : MediaErrorSubType(UNDEFINED_SUBTYPE, "")

    data class MalformedMediaArgSubType(
        val type: Type
    ) : MediaErrorSubType(MALFORMED_MEDIA_ARG_SUBTYPE, type.name) {
        enum class Type(val errorLogDescription: String?) {
            MEDIA_WAS_NULL("media cannot be null"),
            UNSUPPORTED_MIME_TYPE("media must define a valid MIME type"),
            NOT_VALID_LOCAL_FILE_PATH("media must define a local file path"),
            MEDIA_FILE_NOT_FOUND_LOCALLY("local file path for media does not exist"),
            DIRECTORY_PATH_SUPPLIED_FILE_NEEDED("supplied file path is a directory, a file is required"),
            NO_ERROR(null)
        }
    }
}
