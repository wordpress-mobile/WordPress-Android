package org.wordpress.android.ui.stats.refresh.sections

import android.support.annotation.StringRes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.LOADING

sealed class StatsListItem(val type: Type, open val insightsType: InsightsTypes?) {
    enum class Type {
        BLOCK_LIST,
        FAILED,
        EMPTY,
        LOADING
    }
}

data class ListInsightItem(override val insightsType: InsightsTypes, val items: List<BlockListItem>) : StatsListItem(
        BLOCK_LIST,
        insightsType
)

data class Failed(override val insightsType: InsightsTypes, @StringRes val failedType: Int, val errorMessage: String) :
        StatsListItem(FAILED, insightsType)

data class Empty(val isButtonVisible: Boolean = true) : StatsListItem(EMPTY, null)

data class Loading(override val insightsType: InsightsTypes) : StatsListItem(LOADING, insightsType)
