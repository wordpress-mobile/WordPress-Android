package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v7.widget.PopupMenu
import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.Event
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
    private val mutableTypeMoved = MutableLiveData<TypeChangeEvent>()
    val typeMoved: LiveData<TypeChangeEvent> = mutableTypeMoved
    fun onMenuClick(view: View, statsType: StatsTypes) {
        GlobalScope.launch(bgDispatcher) {
            val type = statsType as InsightsTypes
            val insights = statsStore.getInsights(statsSiteProvider.siteModel)

            val indexOfBlock = insights.indexOfFirst { it == type }
            val showUpAction = indexOfBlock > 0
            val showDownAction = indexOfBlock < insights.size - 1
            withContext(mainDispatcher) {
                val popup = PopupMenu(view.context, view)
                val popupMenu = popup.menu
                popup.inflate(R.menu.menu_stats_item)
                popupMenu.findItem(R.id.action_move_up).isVisible = showUpAction
                popupMenu.findItem(R.id.action_move_down).isVisible = showDownAction
                popup.show()
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_move_up -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.moveTypeUp(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(TypeChangeEvent(type))
                            }
                            true
                        }
                        R.id.action_move_down -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.moveTypeDown(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(TypeChangeEvent(type))
                            }
                            true
                        }
                        R.id.action_remove -> {
                            GlobalScope.launch(bgDispatcher) {
                                statsStore.removeType(statsSiteProvider.siteModel, type)
                                mutableTypeMoved.postValue(TypeChangeEvent(type))
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    data class TypeChangeEvent(val types: StatsTypes) : Event()
}
