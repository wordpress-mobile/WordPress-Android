package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import androidx.appcompat.widget.ListPopupWindow
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter.ReferrerMenuItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter.ReferrerMenuItem.MARK_AS_SPAM
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter.ReferrerMenuItem.OPEN_WEBSITE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReferrerPopupMenuHandler
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)
    private val mutableMarkedAsSpam = MutableLiveData<Event<Unit>>()
    val markedAsSpam: LiveData<Event<Unit>> = mutableMarkedAsSpam

    fun onMenuClick(view: View, statsGranularity: StatsGranularity, url: String, referrersUseCase: ReferrersUseCase) {
        coroutineScope.launch {
            withContext(mainDispatcher) {
                val popup = ListPopupWindow(view.context, null, R.attr.listPopupWindowStyle)
                val adapter = ReferrerMenuAdapter(view.context)
                popup.setAdapter(adapter)
                popup.width = view.context.resources.getDimensionPixelSize(R.dimen.stats_insights_menu_item_width)
                popup.anchorView = view
                popup.isModal = true
                popup.setOnItemClickListener { _, _, _, id ->
                    when (ReferrerMenuItem.values()[id.toInt()]) {
                        OPEN_WEBSITE -> {
                            referrersUseCase.openWebsite(url)
                        }
                        MARK_AS_SPAM -> {
                            coroutineScope.launch {
                                analyticsTrackerWrapper.trackGranular(
                                        Stat.STATS_REFERRERS_ITEM_MARKED_AS_SPAM,
                                        statsGranularity
                                )
                                referrersUseCase.markReferrerAsSpam(url)
                                mutableMarkedAsSpam.postValue(Event(Unit))
                            }
                        }
                    }
                    popup.dismiss()
                }
                popup.show()
            }
        }
    }
}
