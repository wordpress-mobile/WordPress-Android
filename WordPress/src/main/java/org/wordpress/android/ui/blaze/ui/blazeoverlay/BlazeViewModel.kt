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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BlazeViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
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
            val featuredImage =
                featuredImageTracker.getFeaturedImageUrl(siteSelectedSiteRepository.getSelectedSite()!!, it.imageUrl)
            val updatedPostModel = postModel.copy(featuredImageUrl = featuredImage)
            _uiState.value = BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
            _promoteUiState.value = BlazeUiState.PromoteScreen.PromotePost(updatedPostModel)
        } ?: run {
            _uiState.value = BlazeUiState.PromoteScreen.Site
            _promoteUiState.value = BlazeUiState.PromoteScreen.Site
        }
    }

    // to do: tracking logic and logic for done state
    fun showNextScreen(currentBlazeUiState: BlazeUiState) {
        when (currentBlazeUiState) {
            is BlazeUiState.PromoteScreen.Site -> _uiState.value = BlazeUiState.PostSelectionScreen
            is BlazeUiState.PromoteScreen.PromotePost -> _uiState.value = BlazeUiState.AppearanceScreen
            is BlazeUiState.PostSelectionScreen -> _uiState.value = BlazeUiState.AppearanceScreen
            is BlazeUiState.AppearanceScreen -> _uiState.value = BlazeUiState.AudienceScreen
            is BlazeUiState.AudienceScreen -> _uiState.value = BlazeUiState.PaymentGateway
            is BlazeUiState.PaymentGateway -> _uiState.value = BlazeUiState.Done
            else -> {}
        }
    }

    fun onPromoteWithBlazeClicked() {
        blazeFeatureUtils.trackPromoteWithBlazeClicked()
    }
}
