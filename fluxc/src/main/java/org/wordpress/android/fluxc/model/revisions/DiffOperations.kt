package org.wordpress.android.fluxc.model.revisions

enum class DiffOperations constructor(private val string: String) {
    COPY("copy"),
    ADD("add"),
    DELETE("del"),
    UNKNOWN("unknown");

    override fun toString(): String {
        return string
    }

    companion object {
        @JvmStatic
        fun fromString(string: String?): DiffOperations {
            return when (string) {
                "copy" -> COPY
                "add" -> ADD
                "del" -> DELETE
                else -> {
                    UNKNOWN
                }
            }
        }
    }
}
