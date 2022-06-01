package org.wordpress.android.ui.stats.refresh.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ActionCardHandler
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val statsStore: StatsStore,
    private val statsSiteProvider: StatsSiteProvider
) {
    private val coroutineScope = CoroutineScope(mainDispatcher)

    private val mutableActionCard = MutableLiveData<Event<StatsType>>()
    val actionCard: LiveData<Event<StatsType>> = mutableActionCard

    fun display(type: InsightType) = coroutineScope.launch {
        // It is set to be shown only once per site
        if (!statsStore.isActionTypeShown(statsSiteProvider.siteModel, type)) {
            statsStore.addActionType(statsSiteProvider.siteModel, type)
        }
        mutableActionCard.value = Event(type)
    }

    fun dismiss(type: InsightType) = coroutineScope.launch {
        statsStore.removeActionType(statsSiteProvider.siteModel, type)
        mutableActionCard.value = Event(type)
    }
}
