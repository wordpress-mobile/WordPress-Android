package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v7.widget.ListPopupWindow
import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.DOWN
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.REMOVE
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.UP
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ItemPopupMenuHandler
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val statsStore: StatsStore,
    private val statsSiteProvider: StatsSiteProvider
) {
    private val mutableTypeMoved = MutableLiveData<Event<StatsType>>()
    val typeMoved: LiveData<Event<StatsType>> = mutableTypeMoved

    fun onMenuClick(view: View, statsType: StatsType) {
        GlobalScope.launch(bgDispatcher) {
            val type = statsType as InsightType
            val insights = statsStore.getAddedInsights(statsSiteProvider.siteModel)

            val indexOfBlock = insights.indexOfFirst { it == type }
            val showUpAction = indexOfBlock > 0
            val showDownAction = indexOfBlock < insights.size - 1

            withContext(mainDispatcher) {
                val popup = ListPopupWindow(view.context)
                val adapter = InsightsMenuAdapter(view.context, showUpAction, showDownAction)
                popup.setAdapter(adapter)
                popup.width = view.context.resources.getDimensionPixelSize(R.dimen.stats_insights_menu_item_width)
                popup.anchorView = view
                popup.isModal = true
                popup.setOnItemClickListener { _, _, _, id ->
                    when (InsightsMenuItem.values()[id.toInt()]) {
                        UP -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.moveTypeUp(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(Event(type))
                            }
                        }
                        DOWN -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.moveTypeDown(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(Event(type))
                            }
                        }
                        REMOVE -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.removeType(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(Event(type))
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
