package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val THROTTLE_DELAY = 500L

class PrepublishingTagsViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val updatePostTagsUseCase: UpdatePostTagsUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository
    private var updateTagsJob: Job? = null

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _dismissKeyboard = MutableLiveData<Event<Unit>>()
    val dismissKeyboard: LiveData<Event<Unit>> = _dismissKeyboard

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    fun start(editPostRepository: EditPostRepository) {
        this.editPostRepository = editPostRepository

        if (isStarted) return
        isStarted = true

        setToolbarTitleUiState()
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.postValue(UiStringRes(R.string.prepublishing_nudges_toolbar_title_tags))
    }

    fun onTagsSelected(selectedTags: String) {
        updateTagsJob?.cancel()
        updateTagsJob = launch(bgDispatcher) {
            delay(THROTTLE_DELAY)
            updatePostTagsUseCase.updateTags(selectedTags, editPostRepository)
        }
    }

    fun onBackButtonClicked() {
        _dismissKeyboard.postValue(Event(Unit))
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    fun getPostTags() = getPostTagsUseCase.getTags(editPostRepository)

    override fun onCleared() {
        super.onCleared()
        updateTagsJob?.cancel()
    }
}
