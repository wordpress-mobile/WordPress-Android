package org.wordpress.android.ui.stats.refresh

abstract class InsightsItem(val type: Type) {
    enum class Type {
        // TODO Remove once all the Types are implemented
        NOT_IMPLEMENTED
    }
    val uuid = type.ordinal
}
