package org.wordpress.android.ui.jetpack.restore

enum class RestoreErrorTypes(val id: Int) {
    NetworkUnavailable(0), RemoteRequestFailure(1), GenericFailure(2), OtherRequestRunning(3);

    companion object {
        fun fromInt(value: Int): RestoreErrorTypes =
            values().firstOrNull { it.id == value }
                ?: throw IllegalArgumentException("RestoreErrorTypes wrong value $value")
    }
}
