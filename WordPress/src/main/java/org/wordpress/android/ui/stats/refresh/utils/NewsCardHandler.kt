package org.wordpress.android.ui.stats.refresh.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NewsCardHandler
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val statsStore: StatsStore
) {
    private val coroutineScope = CoroutineScope(mainDispatcher)
    private val mutableCardDismissed = MutableLiveData<Event<StatsType>>()
    val cardDismissed: LiveData<Event<StatsType>> = mutableCardDismissed

    private val mutableScrollTo = MutableLiveData<Event<StatsType>>()
    val scrollTo: LiveData<Event<StatsType>> = mutableScrollTo

    private val mutableHideToolbar = MutableLiveData<Event<Boolean>>()
    val hideToolbar: LiveData<Event<Boolean>> = mutableHideToolbar

    fun dismiss() = coroutineScope.launch {
        if (statsStore.isInsightsManagementNewsCardShowing()) {
            statsStore.hideInsightsManagementNewsCard()
            mutableCardDismissed.value = Event(ManagementType.NEWS_CARD)
        }
    }

    fun goToEdit() {
        mutableScrollTo.value = Event(ManagementType.CONTROL)
        mutableHideToolbar.value = Event(true)
    }
}
