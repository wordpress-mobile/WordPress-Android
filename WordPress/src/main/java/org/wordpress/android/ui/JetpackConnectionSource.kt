package org.wordpress.android.ui

enum class JetpackConnectionSource(private val value: String) {
    NOTIFICATIONS("notifications"), STATS("stats");

    override fun toString() = value

    companion object {
        @JvmStatic
        fun fromString(value: String) = when {
            NOTIFICATIONS.value == value -> NOTIFICATIONS
            STATS.value == value -> STATS
            else -> null
        }
    }
}
