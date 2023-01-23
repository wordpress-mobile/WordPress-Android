package org.wordpress.android.ui.jetpack.backup.download

enum class BackupDownloadErrorTypes constructor(val id: Int) {
    NetworkUnavailable(0), RemoteRequestFailure(1), GenericFailure(2);

    companion object {
        fun fromInt(value: Int): BackupDownloadErrorTypes =
            values().firstOrNull { it.id == value }
                ?: throw IllegalArgumentException("BackupDownloadErrorTypes wrong value $value")
    }
}
