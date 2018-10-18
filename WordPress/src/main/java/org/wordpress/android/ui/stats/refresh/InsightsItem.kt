package org.wordpress.android.ui.stats.refresh

import android.support.annotation.StringRes
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.NOT_IMPLEMENTED

sealed class InsightsItem(val type: Type) {
    enum class Type {
        LIST_INSIGHTS,
        FAILED,
        EMPTY,
        // TODO Remove once all the Types are implemented
        NOT_IMPLEMENTED
    }
    val uuid = type.ordinal
}

data class ListInsightItem(val items: List<BlockListItem>) : InsightsItem(LIST_INSIGHTS)

data class NotImplemented(val text: String) : InsightsItem(NOT_IMPLEMENTED)

data class Failed(@StringRes val failedType: Int, val errorMessage: String) : InsightsItem(FAILED)

data class Empty(val isButtonVisible: Boolean = true) : InsightsItem(EMPTY)
