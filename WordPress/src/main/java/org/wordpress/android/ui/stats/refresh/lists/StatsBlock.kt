package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, open val statsType: StatsType, open val data: List<BlockListItem>) {
    enum class Type {
        SUCCESS,
        ERROR,
        EMPTY,
        LOADING
    }

    data class Success(
        override val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(SUCCESS, statsType, data)

    data class Error(
        override val statsType: StatsType,
        override val data: List<BlockListItem> = listOf()
    ) : StatsBlock(ERROR, statsType, data)

    data class EmptyBlock(
        override val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(EMPTY, statsType, data)

    data class Loading(
        override val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(LOADING, statsType, data)
}
