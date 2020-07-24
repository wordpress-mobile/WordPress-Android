package org.wordpress.android.ui.stories.media

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.ProgressDialogUiState.HiddenProgressDialog
import org.wordpress.android.ui.posts.ProgressDialogUiState.VisibleProgressDialog
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.stories.media.StoryEditorMediaViewModel.AddMediaToStoryPostUiState.AddingMediaToStoryIdle
import org.wordpress.android.ui.stories.media.StoryEditorMediaViewModel.AddMediaToStoryPostUiState.AddingMultipleMediaToStory
import org.wordpress.android.ui.stories.media.StoryEditorMediaViewModel.AddMediaToStoryPostUiState.AddingSingleMediaToStory
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class StoryEditorMediaViewModel @Inject constructor(
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase,
    private val addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    // region Fields
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private lateinit var site: SiteModel
    private lateinit var editorMediaListener: EditorMediaListener

    private val _uiState: MutableLiveData<AddMediaToStoryPostUiState> = MutableLiveData()
    val uiState: LiveData<AddMediaToStoryPostUiState> = _uiState

    private val _snackBarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val snackBarMessage = _snackBarMessage as LiveData<Event<SnackbarMessageHolder>>

    fun start(site: SiteModel, editorMediaListener: EditorMediaListener) {
        this.site = site
        this.editorMediaListener = editorMediaListener
        _uiState.value = AddingMediaToStoryIdle
    }

    // region Adding new media to a post
    fun advertiseImageOptimisationAndAddMedia(uriList: List<Uri>) {
        if (mediaUtilsWrapper.shouldAdvertiseImageOptimization()) {
            editorMediaListener.advertiseImageOptimization {
                addNewMediaItemsToEditorAsync(
                        uriList,
                        false
                )
            }
        } else {
            addNewMediaItemsToEditorAsync(uriList, false)
        }
    }

    fun addNewMediaItemsToEditorAsync(uriList: List<Uri>, freshlyTaken: Boolean) {
        launch {
            _uiState.value = if (uriList.size > 1) {
                AddingMultipleMediaToStory
            } else {
                AddingSingleMediaToStory
            }
            val allMediaSucceed = addLocalMediaToPostUseCase.addNewMediaToEditorAsync(
                    uriList,
                    site,
                    freshlyTaken,
                    editorMediaListener,
                    false // don't start upload for StoryComposer, that'll be all started
                                            // when finished composing
            )
            if (!allMediaSucceed) {
                _snackBarMessage.value = Event(SnackbarMessageHolder(R.string.gallery_error))
            }
            _uiState.value = AddingMediaToStoryIdle
        }
    }

    fun onPhotoPickerMediaChosen(uriList: List<Uri>) {
        val onlyVideos = uriList.all { mediaUtilsWrapper.isVideo(it.toString()) }
        if (onlyVideos) {
            addNewMediaItemsToEditorAsync(uriList, false)
        } else {
            advertiseImageOptimisationAndAddMedia(uriList)
        }
    }
    // endregion

    fun addExistingMediaToEditorAsync(source: AddExistingMediaSource, mediaIdList: List<Long>) {
        launch {
            addExistingMediaToPostUseCase.addMediaExistingInRemoteToEditorAsync(
                    site,
                    source,
                    mediaIdList,
                    editorMediaListener
            )
        }
    }

    fun cancelAddMediaToEditorActions() {
        job.cancel()
    }

    sealed class AddMediaToStoryPostUiState(
        val editorOverlayVisibility: Boolean,
        val progressDialogUiState: ProgressDialogUiState
    ) {
        /**
         * Adding multiple media items at once can take several seconds on slower devices, so we show a blocking
         * progress dialog in this situation - otherwise the user could accidentally back out of the process
         * before all items were added
         */
        object AddingMultipleMediaToStory : AddMediaToStoryPostUiState(
                editorOverlayVisibility = true,
                progressDialogUiState = VisibleProgressDialog(
                        messageString = UiStringRes(R.string.add_media_progress),
                        cancelable = false,
                        indeterminate = true
                )
        )

        object AddingSingleMediaToStory : AddMediaToStoryPostUiState(true, HiddenProgressDialog)

        object AddingMediaToStoryIdle : AddMediaToStoryPostUiState(false, HiddenProgressDialog)
    }
}
