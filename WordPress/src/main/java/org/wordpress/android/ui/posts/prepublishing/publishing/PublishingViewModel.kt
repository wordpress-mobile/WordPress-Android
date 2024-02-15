package org.wordpress.android.ui.posts.prepublishing.publishing

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import javax.inject.Inject
import javax.inject.Named

class PublishingViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val uploadUiStateUseCase: PostModelUploadUiStateUseCase,
    uploadActionUseCase: UploadActionUseCase,
    uploadStore: UploadStore,
) : ScopedViewModel(bgDispatcher) {

    private val uploadStatusTracker = PostModelUploadStatusTracker(
        uploadStore = uploadStore,
        uploadActionUseCase = uploadActionUseCase
    )

    val uiState: MutableLiveData<PostModelUploadUiStateUseCase.PostUploadUiState> = MutableLiveData()

    // not sure about this implementation
    fun getUploadUiState(
        post: PostModel,
        site: SiteModel
    ) {
        // this is a synchronous callback and should be called from a background thread
        /// should be autotried to get the current state? Not sure
        launch {
            uiState.postValue(
                uploadUiStateUseCase.createUploadUiState(
                    post = post,
                    site = site,
                    uploadStatusTracker = uploadStatusTracker
                )
            )
        }
    }
}

