package org.wordpress.android.ui.stats.refresh.lists.detail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.BLOCK_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsDetailViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BLOCK_DETAIL_USE_CASE) private val detailUseCase: BaseListUseCase,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : ScopedViewModel(mainDispatcher) {
    private val dateSelector = dateSelectorFactory.build(DETAIL)
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    val selectedDateChanged = dateSelector.selectedDate

    private val _showSnackbarMessage = mergeNotNull(
            detailUseCase.snackbarMessage,
            distinct = true,
            singleEvent = true
    )
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage
    val showDateSelector = dateSelector.dateSelectorData

    fun init(
        site: SiteModel,
        postId: Long,
        postType: String,
        postTitle: String,
        postUrl: String?
    ) {
        statsSiteProvider.start(site)
        statsPostProvider.init(postId, postType, postTitle, postUrl)
    }

    fun onDateChanged() {
        launch {
            detailUseCase.onDateChanged()
        }
    }

    fun refresh() {
        launch {
            detailUseCase.refreshData(true)
            _isRefreshing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        dateSelector.clear()
        detailUseCase.onCleared()
        statsPostProvider.clear()
    }

    fun onPullToRefresh() {
        _showSnackbarMessage.value = null
        statsSiteProvider.clear()
        if (networkUtilsWrapper.isNetworkAvailable()) {
            refresh()
        } else {
            _isRefreshing.value = false
            _showSnackbarMessage.value = SnackbarMessageHolder(string.no_network_title)
        }
    }
}
