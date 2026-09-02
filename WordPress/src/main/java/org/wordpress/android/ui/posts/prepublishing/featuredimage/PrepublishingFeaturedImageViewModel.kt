package org.wordpress.android.ui.posts.prepublishing.featuredimage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.FeaturedImageHelper
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageData
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState
import org.wordpress.android.ui.posts.FeaturedImageHelper.TrackableEvent
import org.wordpress.android.ui.posts.UpdateFeaturedImageUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingFeaturedImageViewModel @Inject constructor(
    private val featuredImageHelper: FeaturedImageHelper,
    private val updateFeaturedImageUseCase: UpdateFeaturedImageUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var siteModel: SiteModel

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _launchFeaturedImagePicker = MutableLiveData<Event<Int>>()
    val launchFeaturedImagePicker: LiveData<Event<Int>> = _launchFeaturedImagePicker

    private val _syncFeaturedImageToEditor = MutableLiveData<Event<Unit>>()
    val syncFeaturedImageToEditor: LiveData<Event<Unit>> = _syncFeaturedImageToEditor

    fun start(editPostRepository: EditPostRepository, siteModel: SiteModel) {
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        if (isStarted) return
        isStarted = true
        refreshFeaturedImageState()
    }

    fun onSetOrChangeClicked() {
        val postId = editPostRepository.getPost()?.id ?: return
        featuredImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_SET_CLICKED, postId)
        _launchFeaturedImagePicker.postValue(Event(postId))
    }

    fun onRemoveClicked() {
        val post = editPostRepository.getPost() ?: return
        featuredImageHelper.cancelFeaturedImageUpload(siteModel, post, false)
        featuredImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_REMOVE_CLICKED, post.id)
        updateFeaturedImageUseCase.updateFeaturedImage(NO_FEATURED_IMAGE_ID, editPostRepository) {
            refreshFeaturedImageState()
            // The model is the source of truth, but the editor caches the featured image separately,
            // so tell the host to push the now-cleared id to the editor.
            _syncFeaturedImageToEditor.postValue(Event(Unit))
        }
    }

    fun onRetryClicked() {
        val post = editPostRepository.getPost() ?: return
        featuredImageHelper.retryFeaturedImageUpload(siteModel, post)
        refreshFeaturedImageState()
    }

    fun onBackButtonClick() {
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    fun refreshFeaturedImageState() {
        val post = editPostRepository.getPost() ?: return
        val featuredImageData = featuredImageHelper.createCurrentFeaturedImageState(siteModel, post)
        val hasImage = featuredImageData.uiState != FeaturedImageState.IMAGE_EMPTY
        _uiState.postValue(
            UiState(
                featuredImageData = featuredImageData,
                setButtonText = if (hasImage) {
                    UiStringRes(R.string.post_settings_choose_featured_image)
                } else {
                    UiStringRes(R.string.post_settings_set_featured_image)
                },
                isRemoveButtonVisible = hasImage
            )
        )
    }

    data class UiState(
        val featuredImageData: FeaturedImageData,
        val setButtonText: UiStringRes,
        val isRemoveButtonVisible: Boolean
    )

    companion object {
        private const val NO_FEATURED_IMAGE_ID = 0L
    }
}
