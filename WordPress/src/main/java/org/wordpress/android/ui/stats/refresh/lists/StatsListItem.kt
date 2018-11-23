package org.wordpress.android.ui.stats.refresh.lists

import android.support.annotation.StringRes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsListItem(val type: Type, open val statsTypes: StatsTypes?) {
    enum class Type {
        BLOCK_LIST,
        ERROR,
        EMPTY,
        LOADING
    }
}

data class BlockList(override val statsTypes: StatsTypes, val items: List<BlockListItem>) : StatsListItem(
        BLOCK_LIST,
        statsTypes
)

data class Error(override val statsTypes: StatsTypes, @StringRes val errorType: Int, val errorMessage: String) :
        StatsListItem(ERROR, statsTypes)

data class Empty(val isButtonVisible: Boolean = true) : StatsListItem(EMPTY, null)

data class Loading(override val statsTypes: InsightsTypes) : StatsListItem(LOADING, statsTypes)
