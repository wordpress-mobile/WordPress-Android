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
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.DOWN
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.REMOVE
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.UP
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter
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
    private val statsStore: StatsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)
    private val mutableTypeMoved = MutableLiveData<Event<StatsType>>()
    val typeMoved: LiveData<Event<StatsType>> = mutableTypeMoved

    fun onMenuClick(view: View, statsType: StatsType) {
        coroutineScope.launch {
            withContext(mainDispatcher) {
                val popup = ListPopupWindow(view.context, null, R.attr.listPopupWindowStyle)
                val adapter = ReferrerMenuAdapter(view.context)
                popup.setAdapter(adapter)
                popup.width = view.context.resources.getDimensionPixelSize(R.dimen.stats_insights_menu_item_width)
                popup.anchorView = view
                popup.isModal = true
                popup.setOnItemClickListener { _, _, _, id ->
                    when (InsightsMenuItem.values()[id.toInt()]) {
                        UP -> {
                        }
                        DOWN -> {
                        }
                        REMOVE -> {
                        }
                    }
                    popup.dismiss()
                }
                popup.show()
            }
        }
    }
}
