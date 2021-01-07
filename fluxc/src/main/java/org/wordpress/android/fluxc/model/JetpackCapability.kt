package org.wordpress.android.fluxc.model

enum class JetpackCapability {
    BACKUP,
    BACKUP_DAILY,
    BACKUP_REALTIME,
    SCAN,
    ANTISPAM,
    RESTORE,
    ALTERNATE_RESTORE,
    UNKNOWN;

    companion object {
        fun fromString(item: String): JetpackCapability {
            return when (item) {
                "backup" -> BACKUP
                "backup-daily" -> BACKUP_DAILY
                "backup-realtime" -> BACKUP_REALTIME
                "scan" -> SCAN
                "antispam" -> ANTISPAM
                "restore" -> RESTORE
                "alternate-restore" -> ALTERNATE_RESTORE
                else -> UNKNOWN
            }
        }
    }
}
