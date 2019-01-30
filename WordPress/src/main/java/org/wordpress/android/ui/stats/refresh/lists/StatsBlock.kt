package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, open val statsTypes: StatsTypes, open val data: List<BlockListItem>) {
    enum class Type {
        SUCCESS,
        ERROR,
        EMPTY,
        LOADING
    }

    data class Success(
        override val statsTypes: StatsTypes,
        override val data: List<BlockListItem>
    ) : StatsBlock(SUCCESS, statsTypes, data)

    data class Error(
        override val statsTypes: StatsTypes,
        override val data: List<BlockListItem> = listOf()
    ) : StatsBlock(ERROR, statsTypes, data)

    data class EmptyBlock(
        override val statsTypes: StatsTypes,
        override val data: List<BlockListItem>
    ) : StatsBlock(EMPTY, statsTypes, data)

    data class Loading(
        override val statsTypes: StatsTypes,
        override val data: List<BlockListItem>
    ) : StatsBlock(LOADING, statsTypes, data)
}
