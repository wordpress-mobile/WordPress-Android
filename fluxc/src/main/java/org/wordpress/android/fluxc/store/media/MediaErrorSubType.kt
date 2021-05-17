package org.wordpress.android.fluxc.store.media

import org.wordpress.android.fluxc.store.media.MediaErrorSubType.UndefinedSubType.Type.UNDEFINED
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

interface GetByString {
    fun fromString(name: String?): MediaErrorSubType?
}

sealed class MediaErrorSubType {
    data class UndefinedSubType constructor(val type: Type = UNDEFINED) : MediaErrorSubType() {
        enum class Type {
            UNDEFINED
        }

        override fun toString(): String {
            return "${UndefinedSubType::class.java.simpleName}@$type"
        }

        companion object : GetByString {
            override fun fromString(name: String?): MediaErrorSubType? {
                return if (name?.startsWith(UndefinedSubType::class.java.simpleName) == true) {
                    UndefinedSubType(UNDEFINED)
                } else {
                    null
                }
            }
        }
    }

    data class MalformedMediaArgSubType(val type: Type) : MediaErrorSubType() {
        enum class Type(val errorLogDescription: String?) {
            MEDIA_WAS_NULL("media cannot be null"),
            UNSUPPORTED_MIME_TYPE("media must define a valid MIME type"),
            NOT_VALID_LOCAL_FILE_PATH("media must define a local file path"),
            MEDIA_FILE_NOT_FOUND_LOCALLY("local file path for media does not exist"),
            DIRECTORY_PATH_SUPPLIED_FILE_NEEDED("supplied file path is a directory, a file is required"),
            NO_ERROR(null)
        }

        override fun toString(): String {
            return "${MalformedMediaArgSubType::class.java.simpleName}@$type"
        }

        companion object : GetByString {
            override fun fromString(name: String?): MediaErrorSubType? {
                return name?.takeIf { it.startsWith(MalformedMediaArgSubType::class.java.simpleName) }?.let {
                    val targetSubType: String? = it.split("@").lastOrNull()

                    targetSubType?.let { subTypeName ->
                        Type.values().firstOrNull { type ->
                            subTypeName.equals(type.name, true)
                        }?.let { matchedType ->
                            MalformedMediaArgSubType(matchedType)
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getSubTypeFromString(name: String?): MediaErrorSubType {
            if (name == null) return UndefinedSubType()

            for (errorSubType in MediaErrorSubType::class.sealedSubclasses) {
                if (errorSubType.companionObject?.isCompanion != true) {
                    throw IllegalStateException("${errorSubType.simpleName} does not implement a companion object.")
                }

                val companion = errorSubType.companionObjectInstance

                if (companion !is GetByString) {
                    throw IllegalStateException(
                            "${errorSubType.simpleName} does not implement GetByString interface."
                    )
                }

                companion.fromString(name)?.also {
                    return it
                }
            }

            return UndefinedSubType()
        }
    }
}
