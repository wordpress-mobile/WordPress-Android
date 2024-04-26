package org.wordpress.android.ui.blaze.blazeoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUIModel
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.PageUIModel
import org.wordpress.android.ui.blaze.PostUIModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.PostListFeaturedImageTracker
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class BlazeViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
    dispatcher: Dispatcher,
    mediaStore: MediaStore,
    private val selectedSiteRepository: SelectedSiteRepository
) : ViewModel() {
    private lateinit var blazeFlowSource: BlazeFlowSource

    private val _uiState = MutableLiveData<BlazeUiState>()
    val uiState: LiveData<BlazeUiState> = _uiState

    private val _promoteUiState = MutableLiveData<BlazeUiState.PromoteScreen>()
    val promoteUiState: LiveData<BlazeUiState.PromoteScreen> = _promoteUiState

    private val _onSelectedSiteMissing = MutableLiveData<Unit>()
    val onSelectedSiteMissing = _onSelectedSiteMissing as LiveData<Unit>
    private val featuredImageTracker =
        PostListFeaturedImageTracker(dispatcher = dispatcher, mediaStore = mediaStore)

    fun start(source: BlazeFlowSource, blazeUIModel: BlazeUIModel?, shouldShowOverlay: Boolean) {
        blazeFlowSource = source
        blazeUIModel?.let { initializePromoteContentUIState(it) } ?: run {
            initializePromoteSiteUIState(shouldShowOverlay)
        }
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _onSelectedSiteMissing.value = Unit
            return
        }
    }

    private fun initializePromoteContentUIState(blazeUIModel: BlazeUIModel) {
        when (blazeUIModel) {
            is PostUIModel -> initializePromotePostUIState(blazeUIModel)
            is PageUIModel -> initializePromotePageUIState(blazeUIModel)
        }
    }

    private fun initializePromotePostUIState(postModel: PostUIModel) {
        val updatedPostModel = postModel.copy(
            url = UrlUtils.removeScheme(postModel.url),
            featuredImageUrl = selectedSiteRepository.getSelectedSite()?.let {
                featuredImageTracker.getFeaturedImageUrl(
                    it,
                    postModel.featuredImageId
                )
            }
        )

        _promoteUiState.value = BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
        _uiState.value = if (shouldShowOverlayAndTrack()) {
            BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
        } else {
            BlazeUiState.WebViewScreen
        }
    }

    private fun initializePromotePageUIState(pageModel: PageUIModel) {
        val updatedPageModel = pageModel.copy(
            url = UrlUtils.removeScheme(pageModel.url),
            featuredImageUrl = selectedSiteRepository.getSelectedSite()?.let {
                featuredImageTracker.getFeaturedImageUrl(
                    it,
                    pageModel.featuredImageId
                )
            }
        )

        _promoteUiState.value = BlazeUiState.PromoteScreen.PromotePage(updatedPageModel)
        _uiState.value = if (shouldShowOverlayAndTrack()) {
            BlazeUiState.PromoteScreen.PromotePage(updatedPageModel)
        } else {
            BlazeUiState.WebViewScreen
        }
    }

    private fun initializePromoteSiteUIState(shouldShowOverlay: Boolean) {
        _promoteUiState.value = BlazeUiState.PromoteScreen.Site
        _uiState.value = if (shouldShowOverlayAndTrack() || shouldShowOverlay) {
            BlazeUiState.PromoteScreen.Site
        } else {
            BlazeUiState.WebViewScreen
        }
    }

    private fun shouldShowOverlayAndTrack(): Boolean {
        val shouldShowOverlay = !blazeFeatureUtils.shouldHideBlazeOverlay()
        if (shouldShowOverlay) {
            blazeFeatureUtils.trackOverlayDisplayed(blazeFlowSource)
            blazeFeatureUtils.setShouldHideBlazeOverlay()
        }
        return shouldShowOverlay
    }

    // to do: tracking logic and logic for done state - this might not be where we want to track
    private fun showNextScreen(currentBlazeUiState: BlazeUiState) {
        when (currentBlazeUiState) {
            is BlazeUiState.PromoteScreen.Site -> _uiState.value = BlazeUiState.WebViewScreen
            is BlazeUiState.PromoteScreen.PromotePost -> _uiState.value = BlazeUiState.WebViewScreen
            is BlazeUiState.PromoteScreen.PromotePage -> _uiState.value = BlazeUiState.WebViewScreen
            is BlazeUiState.WebViewScreen -> _uiState.value = BlazeUiState.Done
            else -> {}
        }
    }

    fun onPromoteWithBlazeClicked() {
        blazeFeatureUtils.trackPromoteWithBlazeClicked(blazeFlowSource)
        uiState.value?.let { showNextScreen(it) }
    }

    fun dismissOverlay() {
        _uiState.postValue(BlazeUiState.Done)
        blazeFeatureUtils.trackOverlayDismissed(blazeFlowSource)
    }

    fun getSource() = blazeFlowSource
}
