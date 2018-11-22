package org.wordpress.android.ui.stats.refresh.lists

import android.support.annotation.StringRes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsListItem(val type: Type, open val insightsType: InsightsTypes?) {
    enum class Type {
        BLOCK_LIST,
        ERROR,
        EMPTY,
        LOADING
    }
}

data class BlockList(override val insightsType: InsightsTypes, val items: List<BlockListItem>) : StatsListItem(
        BLOCK_LIST,
        insightsType
)

data class Error(override val insightsType: InsightsTypes, @StringRes val failedType: Int, val errorMessage: String) :
        StatsListItem(ERROR, insightsType)

data class Empty(val isButtonVisible: Boolean = true) : StatsListItem(EMPTY, null)

data class Loading(override val insightsType: InsightsTypes) : StatsListItem(LOADING, insightsType)
