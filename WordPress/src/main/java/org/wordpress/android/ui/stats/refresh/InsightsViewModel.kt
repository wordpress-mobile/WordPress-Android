package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.modules.UI_CONTEXT
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.NOT_IMPLEMENTED
import org.wordpress.android.ui.stats.refresh.StatsUiState.ListStatus.DONE
import org.wordpress.android.ui.stats.refresh.StatsUiState.ListStatus.FETCHING
import javax.inject.Inject
import javax.inject.Named

class InsightsViewModel
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(UI_CONTEXT) private val uiContext: CoroutineDispatcher
) : ViewModel() {
    private val mutableData: MutableLiveData<StatsUiState> = MutableLiveData()
    val data: LiveData<StatsUiState> = mutableData
    private val loaders: Map<InsightsTypes, (suspend () -> InsightsItem)> = mapOf()
    fun start() {
        if (mutableData.value == null) {
            mutableData.value = StatsUiState(status = FETCHING)
        }
        launch(uiContext) {
            val deferred = statsStore.getInsights().map {
                async {
                    loaders[it]?.invoke() ?: NotImplemented(it.name)
                }
            }
            mutableData.value = StatsUiState(deferred.map { it.await() }, DONE)
        }
    }
}

data class NotImplemented(val text: String) : InsightsItem(NOT_IMPLEMENTED)

data class StatsUiState(val data: List<InsightsItem> = listOf(), val status: ListStatus) {
    enum class ListStatus {
        DONE,
        ERROR,
        FETCHING
    }
}
