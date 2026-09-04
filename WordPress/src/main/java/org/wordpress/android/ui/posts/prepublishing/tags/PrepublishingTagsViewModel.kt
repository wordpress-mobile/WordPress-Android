package org.wordpress.android.ui.posts.prepublishing.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.UpdatePostTagsUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * UI state for the pre-publish Tags screen.
 *
 * @param selectedTags the tags currently applied to the post, rendered as removable chips.
 * @param suggestions the site tags matching the current input that are not already selected.
 */
data class PrepublishingTagsUiState(
    val selectedTags: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

class PrepublishingTagsViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val updatePostTagsUseCase: UpdatePostTagsUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository

    private var allTags: List<String> = emptyList()
    private var currentFilter: String = ""
    private var initialTags: List<String> = emptyList()
    private val selectedTags: MutableList<String> = mutableListOf()

    private val _uiState = MutableLiveData<PrepublishingTagsUiState>()
    val uiState: LiveData<PrepublishingTagsUiState> = _uiState

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _dismissKeyboard = MutableLiveData<Event<Unit>>()
    val dismissKeyboard: LiveData<Event<Unit>> = _dismissKeyboard

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    fun start(editPostRepository: EditPostRepository, siteTags: List<String> = emptyList()) {
        this.editPostRepository = editPostRepository
        this.allTags = siteTags

        if (isStarted) {
            updateUiState()
            return
        }
        isStarted = true

        initialTags = parseTags(getPostTagsUseCase.getTags(editPostRepository))
        selectedTags.clear()
        selectedTags.addAll(initialTags)

        _toolbarTitleUiState.postValue(UiStringRes(R.string.prepublishing_nudges_toolbar_title_tags))
        updateUiState()
    }

    fun onSiteTagsChanged(siteTags: List<String>) {
        allTags = siteTags
        updateUiState()
    }

    fun onInputChanged(input: String) {
        currentFilter = input.trim()
        updateUiState()
    }

    fun onTagAdded(tag: String) {
        val cleaned = tag.trim()
        currentFilter = ""
        if (cleaned.isEmpty() || selectedTags.any { it.equals(cleaned, ignoreCase = true) }) {
            updateUiState()
            return
        }
        selectedTags.add(cleaned)
        persistAndRefresh()
    }

    fun onTagRemoved(tag: String) {
        val removed = selectedTags.removeAll { it.equals(tag, ignoreCase = true) }
        if (removed) persistAndRefresh()
    }

    fun onLastTagRemoved() {
        if (selectedTags.isNotEmpty()) {
            selectedTags.removeAt(selectedTags.size - 1)
            persistAndRefresh()
        }
    }

    fun onBackButtonClicked() {
        _dismissKeyboard.postValue(Event(Unit))
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    fun wereTagsChanged(): Boolean = selectedTags != initialTags

    private fun persistAndRefresh() {
        val joinedTags = selectedTags.joinToString(SEPARATOR)
        launch(bgDispatcher) {
            updatePostTagsUseCase.updateTags(joinedTags, editPostRepository)
        }
        updateUiState()
    }

    private fun updateUiState() {
        _uiState.postValue(
            PrepublishingTagsUiState(
                selectedTags = selectedTags.toList(),
                suggestions = computeSuggestions()
            )
        )
    }

    private fun computeSuggestions(): List<String> {
        val filter = currentFilter.lowercase(Locale.getDefault())
        return allTags.filter { tagName ->
            selectedTags.none { it.equals(tagName, ignoreCase = true) } &&
                    (filter.isEmpty() || tagName.lowercase(Locale.getDefault()).contains(filter))
        }
    }

    private fun parseTags(tags: String?): List<String> {
        if (tags.isNullOrEmpty()) return emptyList()
        return tags.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val SEPARATOR = ","
    }
}
