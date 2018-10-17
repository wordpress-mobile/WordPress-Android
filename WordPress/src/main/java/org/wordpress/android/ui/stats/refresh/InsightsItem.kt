package org.wordpress.android.ui.stats.refresh

abstract class InsightsItem(val type: Type) {
    enum class Type {
        LIST_INSIGHTS,
        FAILED,
        EMPTY,
        // TODO Remove once all the Types are implemented
        NOT_IMPLEMENTED
    }
    val uuid = type.ordinal
}
