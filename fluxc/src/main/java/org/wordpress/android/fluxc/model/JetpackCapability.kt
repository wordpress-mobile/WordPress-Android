package org.wordpress.android.fluxc.model

enum class JetpackCapability(private val stringValue: String) {
    BACKUP("backup"),
    BACKUP_DAILY("backup-daily"),
    BACKUP_REALTIME("backup-realtime"),
    SCAN("scan"),
    ANTISPAM("antispam"),
    RESTORE("restore"),
    ALTERNATE_RESTORE("alternate-restore"),
    UNKNOWN("unknown");

    override fun toString(): String {
        return stringValue
    }

    companion object {
        fun fromString(string: String): JetpackCapability {
            for (item in values()) {
                if (item.stringValue == string) {
                    return item
                }
            }
            return UNKNOWN
        }
    }
}
