package org.wordpress.android.ui.stats.refresh.lists.detail

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.TOTAL_FOLLOWERS_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.TOTAL_LIKES_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.VIEWS_AND_VISITORS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

abstract class InsightsDetailViewModel (
    mainDispatcher: CoroutineDispatcher,
    private val detailUseCase: BaseListUseCase,
    private val statsSiteProvider: StatsSiteProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _showSnackbarMessage = mergeNotNull(
        detailUseCase.snackbarMessage,
        distinct = true,
        singleEvent = true
    )
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    fun init(localSiteId: Int) {
        statsSiteProvider.start(localSiteId)
    }

    fun refresh() {
        launch {
            detailUseCase.refreshData(true)
            _isRefreshing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        detailUseCase.onCleared()
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun onPullToRefresh() {
        _showSnackbarMessage.value = null
        statsSiteProvider.clear()
        if (networkUtilsWrapper.isNetworkAvailable()) {
            refresh()
        } else {
            _isRefreshing.value = false
            _showSnackbarMessage.value = SnackbarMessageHolder(UiStringRes(R.string.no_network_title))
        }
    }
}

@HiltViewModel
class ViewsVisitorsDetailViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEWS_AND_VISITORS_USE_CASE) private val detailUseCase: BaseListUseCase,
    statsSiteProvider: StatsSiteProvider,
    networkUtilsWrapper: NetworkUtilsWrapper
) : InsightsDetailViewModel(mainDispatcher, detailUseCase, statsSiteProvider, networkUtilsWrapper)

@HiltViewModel
class TotalLikesDetailViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(TOTAL_LIKES_DETAIL_USE_CASE) detailUseCase: BaseListUseCase,
    statsSiteProvider: StatsSiteProvider,
    networkUtilsWrapper: NetworkUtilsWrapper
) : InsightsDetailViewModel(mainDispatcher, detailUseCase, statsSiteProvider, networkUtilsWrapper)

@HiltViewModel
class TotalFollowersDetailViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(TOTAL_FOLLOWERS_DETAIL_USE_CASE) detailUseCase: BaseListUseCase,
    statsSiteProvider: StatsSiteProvider,
    networkUtilsWrapper: NetworkUtilsWrapper
) : InsightsDetailViewModel(mainDispatcher, detailUseCase, statsSiteProvider, networkUtilsWrapper)
