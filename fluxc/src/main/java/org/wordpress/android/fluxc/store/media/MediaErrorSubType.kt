package org.wordpress.android.fluxc.store.media

import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.DIRECTORY_PATH_SUPPLIED_FILE_NEEDED
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.MEDIA_FILE_NOT_FOUND_LOCALLY
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.MEDIA_WAS_NULL
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.NOT_VALID_LOCAL_FILE_PATH
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.NO_ERROR
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE

sealed class MediaErrorSubType(val categoryName: String, val subTypeName: String) {
    // This enum describes the categories of MediaErrorType that are mapped into one or multiple
    // MediaErrorSubType. UNDEFINED category collects all MediaErrorType(s) that are not mapped yet.
    // To map another MediaErrorType, add an item to the enum and define a MediaErrorSubType element
    enum class MediaErrorSubtypeCategory(val errors: List<MediaErrorSubType>) {
        UNDEFINED_SUBTYPE(listOf(
                UndefinedSubType
        )),
        MALFORMED_MEDIA_ARG_SUBTYPE(listOf(
                MalformedMediaArgSubType(MEDIA_WAS_NULL),
                MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE),
                MalformedMediaArgSubType(NOT_VALID_LOCAL_FILE_PATH),
                MalformedMediaArgSubType(MEDIA_FILE_NOT_FOUND_LOCALLY),
                MalformedMediaArgSubType(DIRECTORY_PATH_SUPPLIED_FILE_NEEDED),
                MalformedMediaArgSubType(NO_ERROR)
        ));
    }

    fun serialize(): String {
        return "$categoryName:$subTypeName"
    }

    companion object {
        @JvmStatic
        fun deserialize(name: String?): MediaErrorSubType {
            if (name == null) return UndefinedSubType

            MediaErrorSubtypeCategory.values().forEach { category ->
                category.errors.forEach { subType ->
                    if (subType.serialize() == name) {
                        return subType
                    }
                }
            }

            return UndefinedSubType
        }
    }

    object UndefinedSubType : MediaErrorSubType(UndefinedSubType::class.java.simpleName, "")

    data class MalformedMediaArgSubType(
        val type: Type
    ) : MediaErrorSubType(MalformedMediaArgSubType::class.java.simpleName, type.name) {
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
