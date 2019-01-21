package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, val statsTypes: StatsTypes?, val items: List<BlockListItem>) {
    enum class Type {
        BLOCK_LIST,
        ERROR,
        EMPTY,
        LOADING
    }

    class Success(
        statsTypes: StatsTypes,
        items: List<BlockListItem>
    ) : StatsBlock(BLOCK_LIST, statsTypes, items)

    class Error(
        statsTypes: StatsTypes,
        items: List<BlockListItem>
    ) : StatsBlock(ERROR, statsTypes, items)

    class EmptyBlock(
        statsTypes: StatsTypes,
        items: List<BlockListItem>
    ) : StatsBlock(EMPTY, statsTypes, items)

    class Loading(
        statsTypes: StatsTypes,
        items: List<BlockListItem>
    ) : StatsBlock(LOADING, statsTypes, items)
}
