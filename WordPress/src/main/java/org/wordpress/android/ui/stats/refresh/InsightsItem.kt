package org.wordpress.android.ui.stats.refresh

import android.support.annotation.StringRes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LOADING

sealed class InsightsItem(val type: Type, open val insightsType: InsightsTypes?) {
    enum class Type {
        LIST_INSIGHTS,
        FAILED,
        EMPTY,
        LOADING
    }
}

data class ListInsightItem(
    override val insightsType: InsightsTypes,
    val items: List<BlockListItem>,
    val uiState: ListUiState? = null
) : InsightsItem(
        LIST_INSIGHTS,
        insightsType
) {
    interface ListUiState
}

data class Failed(override val insightsType: InsightsTypes, @StringRes val failedType: Int, val errorMessage: String) :
        InsightsItem(FAILED, insightsType)

data class Empty(val isButtonVisible: Boolean = true) : InsightsItem(EMPTY, null)

data class Loading(override val insightsType: InsightsTypes) : InsightsItem(LOADING, insightsType)
