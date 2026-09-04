package org.wordpress.android.support.unified.model

enum class ConversationStatus {
    ONGOING,
    CLOSED,
    SOLVED,
    UNKNOWN;

    companion object {
        fun fromStatus(status: String): ConversationStatus {
            return when (status.lowercase()) {
                "open", "new", "hold", "pending" -> ONGOING
                "closed" -> CLOSED
                "solved" -> SOLVED
                else -> UNKNOWN
            }
        }
    }
}
