package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, open val statsTypes: StatsTypes?) {
    enum class Type {
        BLOCK_LIST,
        ERROR,
        EMPTY,
        LOADING
    }
}

data class BlockList(
    override val statsTypes: StatsTypes,
    val items: List<BlockListItem>
) : StatsBlock(
        BLOCK_LIST,
        statsTypes
)

data class Error(override val statsTypes: StatsTypes, val errorMessage: String) :
        StatsBlock(ERROR, statsTypes)

data class Empty(val isButtonVisible: Boolean = true) : StatsBlock(EMPTY, null)

data class Loading(override val statsTypes: InsightsTypes) : StatsBlock(LOADING, statsTypes)
