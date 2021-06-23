package org.wordpress.android.ui

enum class JetpackConnectionSource(private val value: String) {
    NOTIFICATIONS("notifications"), STATS("stats");

    override fun toString(): String {
        return value
    }

    companion object {
        @JvmStatic fun fromString(value: String): JetpackConnectionSource? {
            return when {
                NOTIFICATIONS.value == value -> {
                    NOTIFICATIONS
                }
                STATS.value == value -> {
                    STATS
                }
                else -> {
                    null
                }
            }
        }
    }
}
