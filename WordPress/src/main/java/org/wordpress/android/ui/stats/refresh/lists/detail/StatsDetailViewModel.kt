package org.wordpress.android.ui.stats.refresh.lists.detail

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.BLOCK_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.viewmodel.Event
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
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
) : ScopedViewModel(mainDispatcher) {
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _showSnackbarMessage = mergeNotNull(
        detailUseCase.snackbarMessage,
        distinct = true,
        singleEvent = true
    )
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    fun init(
        postId: Long,
        postType: String,
        postTitle: String,
        postUrl: String?
    ) {
        statsPostProvider.init(postId, postType, postTitle, postUrl)

        if (jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(
                JetpackOverlayConnectedFeature.STATS)) {
            showJetpackOverlay()
        }
    }

    private fun showJetpackOverlay() {
        _showJetpackOverlay.value = Event(true)
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
        statsPostProvider.clear()
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
