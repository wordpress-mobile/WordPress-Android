package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.ui.stats.refresh.StatsItem.LatestPostSummary
import org.wordpress.android.ui.stats.refresh.StatsItem.Today
import org.wordpress.android.ui.stats.refresh.StatsType.LATEST_POST_SUMMARY
import org.wordpress.android.ui.stats.refresh.StatsType.TODAY_STATS

class StatsListViewModel: ViewModel() {
    val data: MutableLiveData<List<StatsItem>> = MutableLiveData()
    init {
        data.value = listOf(LatestPostSummary, Today)
    }
}

enum class StatsType {
    LATEST_POST_SUMMARY, TODAY_STATS
}

sealed class StatsItem(val type: StatsType) {
    val uuid = type.ordinal
    object LatestPostSummary: StatsItem(LATEST_POST_SUMMARY)
    object Today: StatsItem(TODAY_STATS)
}

class LatestPostSummaryViewModel: ViewModel()
