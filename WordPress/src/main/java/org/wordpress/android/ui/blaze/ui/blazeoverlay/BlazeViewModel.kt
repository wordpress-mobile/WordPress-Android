package org.wordpress.android.ui.blaze.ui.blazeoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUiState
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
    private val siteSelectedSiteRepository: SelectedSiteRepository
) : ViewModel() {
    private lateinit var blazeFlowSource: BlazeFlowSource

    private val _uiState = MutableLiveData<BlazeUiState>()
    val uiState: LiveData<BlazeUiState> = _uiState

    private val _promoteUiState = MutableLiveData<BlazeUiState.PromoteScreen>()
    val promoteUiState: LiveData<BlazeUiState.PromoteScreen> = _promoteUiState

    private val featuredImageTracker =
        PostListFeaturedImageTracker(dispatcher = dispatcher, mediaStore = mediaStore)

    fun start(source: BlazeFlowSource, postId: PostUIModel?) {
        blazeFlowSource = source
        initialize(postId)
    }

    fun initialize(postModel: PostUIModel?) {
        blazeFeatureUtils.trackOverlayDisplayed(blazeFlowSource)
        postModel?.let {
            val updatedPostModel = updatePostModel(it)
            _uiState.value = BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
            _promoteUiState.value = BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
        } ?: run {
            _uiState.value = BlazeUiState.PromoteScreen.Site
            _promoteUiState.value = BlazeUiState.PromoteScreen.Site
        }
    }

    private fun updatePostModel(postModel: PostUIModel): PostUIModel {
        val featuredImage =
            featuredImageTracker.getFeaturedImageUrl(siteSelectedSiteRepository.getSelectedSite()!!, postModel.imageUrl)
        val updatedUrl = UrlUtils.removeScheme(postModel.url)
        return postModel.copy(url = updatedUrl, featuredImageUrl = featuredImage)
    }

    // to do: tracking logic and logic for done state - this might not be where we want to track
    private fun showNextScreen(currentBlazeUiState: BlazeUiState) {
        when (currentBlazeUiState) {
            is BlazeUiState.PromoteScreen.Site -> _uiState.value = BlazeUiState.WebViewScreen
            is BlazeUiState.PromoteScreen.PromotePost -> _uiState.value = BlazeUiState.WebViewScreen
            is BlazeUiState.PromoteScreen.Page -> _uiState.value = BlazeUiState.WebViewScreen
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
