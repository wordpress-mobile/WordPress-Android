package org.wordpress.android.ui.posts.prepublishing.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
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
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository

    private var allTags: List<String> = emptyList()
    private var currentFilter: String = ""
    private var initialTags: List<String> = emptyList()
    private val selectedTags: MutableList<String> = mutableListOf()
    private var persistJob: Job? = null
    private var pendingWrite = false

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

    /**
     * Adds any text still sitting in the input field (never confirmed with a comma, IME action or
     * suggestion tap) as a tag. Called right before the screen is dismissed so typed-but-uncommitted
     * text is not silently dropped.
     */
    fun commitPendingTag() {
        onTagAdded(currentFilter)
    }

    fun onBackButtonClicked() {
        flushPendingTags()
        _dismissKeyboard.postValue(Event(Unit))
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    // Compare as sets so reordering (e.g. removing then re-adding the same tag) is not reported as a
    // change; only a differing tag set counts.
    fun wereTagsChanged(): Boolean = selectedTags.toSet() != initialTags.toSet()

    private fun persistAndRefresh() {
        updateUiState()
        // Throttle persistence so a burst of edits (e.g. pasting "a,b,c") collapses into a single
        // write. Cancelling the pending job guarantees only the latest snapshot is persisted and
        // that no two writes race on the shared post model. The throttle runs on the main
        // dispatcher so pendingWrite is only ever touched on a single thread.
        pendingWrite = true
        persistJob?.cancel()
        persistJob = launch(mainDispatcher) {
            delay(PERSIST_THROTTLE_DELAY_MS)
            writePendingTags()
        }
    }

    /**
     * Persists the latest tags immediately, cancelling any queued throttled write. Called when the
     * screen is dismissed (back button or [onCleared]) so the latest snapshot is never lost while a
     * write is still queued.
     */
    private fun flushPendingTags() {
        persistJob?.cancel()
        writePendingTags()
    }

    /**
     * Writes the current tags if a write is still pending. The write is dispatched on
     * [EditPostRepository]'s own scope, which outlives this ViewModel, so it completes even when
     * this call originates from [onCleared].
     */
    private fun writePendingTags() {
        if (!pendingWrite) return
        pendingWrite = false
        updatePostTagsUseCase.updateTags(selectedTags.joinToString(SEPARATOR), editPostRepository)
    }

    override fun onCleared() {
        // Covers dismiss paths that never reach the back handler (e.g. dragging the sheet away):
        // promote any typed-but-uncommitted text before the final synchronous flush so it is saved.
        commitPendingTag()
        flushPendingTags()
        super.onCleared()
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
        }.distinctBy { it.lowercase(Locale.getDefault()) }
    }

    private fun parseTags(tags: String?): List<String> {
        if (tags.isNullOrEmpty()) return emptyList()
        return tags.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        private const val SEPARATOR = ","
        private const val PERSIST_THROTTLE_DELAY_MS = 500L
    }
}
