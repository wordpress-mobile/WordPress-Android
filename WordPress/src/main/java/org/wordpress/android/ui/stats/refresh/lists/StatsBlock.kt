package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.CONTROL
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, open val data: List<BlockListItem>) {
    enum class Type {
        SUCCESS,
        ERROR,
        EMPTY,
        LOADING,
        CONTROL
    }

    data class Success(
        val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(SUCCESS, data)

    data class Error(
        val statsType: StatsType,
        override val data: List<BlockListItem> = listOf()
    ) : StatsBlock(ERROR, data)

    data class EmptyBlock(
        val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(EMPTY, data)

    data class Loading(
        val statsType: StatsType,
        override val data: List<BlockListItem>
    ) : StatsBlock(LOADING, data)

    data class Control(
        override val data: List<BlockListItem>
    ) : StatsBlock(CONTROL, data)
}
