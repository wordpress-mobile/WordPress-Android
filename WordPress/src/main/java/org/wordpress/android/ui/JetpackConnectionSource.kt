package org.wordpress.android.ui

enum class JetpackConnectionSource(private val value: String) {
    NOTIFICATIONS("notifications"),
    STATS("stats"),
    XMLRPC_DISABLED("xmlrpc_disabled");

    override fun toString() = value

    companion object {
        @JvmStatic
        fun fromString(value: String) = when {
            NOTIFICATIONS.value == value -> NOTIFICATIONS
            STATS.value == value -> STATS
            XMLRPC_DISABLED.value == value -> XMLRPC_DISABLED
            else -> null
        }
    }
}
