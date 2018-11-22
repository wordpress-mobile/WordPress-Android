package org.wordpress.android.ui.stats.refresh.sections

import android.support.annotation.StringRes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.LOADING

sealed class StatsItem(val type: Type, open val insightsType: InsightsTypes?) {
    enum class Type {
        LIST_INSIGHTS,
        FAILED,
        EMPTY,
        LOADING
    }
}

data class ListInsightItem(override val insightsType: InsightsTypes, val items: List<BlockListItem>) : StatsItem(
        LIST_INSIGHTS,
        insightsType
)

data class Failed(override val insightsType: InsightsTypes, @StringRes val failedType: Int, val errorMessage: String) :
        StatsItem(FAILED, insightsType)

data class Empty(val isButtonVisible: Boolean = true) : StatsItem(EMPTY, null)

data class Loading(override val insightsType: InsightsTypes) : StatsItem(LOADING, insightsType)
