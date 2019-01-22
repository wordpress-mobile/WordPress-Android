package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

sealed class StatsBlock(val type: Type, open val statsTypes: StatsTypes, open val items: List<BlockListItem>) {
    enum class Type {
        SUCCESS,
        ERROR,
        EMPTY,
        LOADING
    }

    data class Success(
        override val statsTypes: StatsTypes,
        override val items: List<BlockListItem>
    ) : StatsBlock(SUCCESS, statsTypes, items)

    data class Error(
        override val statsTypes: StatsTypes,
        override val items: List<BlockListItem>
    ) : StatsBlock(ERROR, statsTypes, items)

    data class EmptyBlock(
        override val statsTypes: StatsTypes,
        override val items: List<BlockListItem>
    ) : StatsBlock(EMPTY, statsTypes, items)

    data class Loading(
        override val statsTypes: StatsTypes,
        override val items: List<BlockListItem>
    ) : StatsBlock(LOADING, statsTypes, items)

    fun update(newBlock: StatsBlock): StatsBlock {
        return when (newBlock.type) {
            LOADING, EMPTY, SUCCESS -> newBlock
            ERROR -> {
                if (this.type == SUCCESS) {
                    Error(newBlock.statsTypes, this.items)
                } else {
                    newBlock
                }
            }
        }
    }
}
