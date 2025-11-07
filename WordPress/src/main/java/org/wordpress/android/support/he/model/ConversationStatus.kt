package org.wordpress.android.support.he.model

enum class ConversationStatus {
    WAITING_FOR_SUPPORT,
    WAITING_FOR_USER,
    CLOSED,
    SOLVED,
    UNKNOWN;

    companion object {
        fun fromStatus(status: String): ConversationStatus {
            return when (status.lowercase()) {
                "open", "new", "hold" -> WAITING_FOR_SUPPORT
                "closed" -> CLOSED
                "pending" -> WAITING_FOR_USER
                "solved" -> SOLVED
                else -> UNKNOWN
            }
        }
    }
}
