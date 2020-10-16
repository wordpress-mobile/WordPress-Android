package org.wordpress.android.ui.suggestion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsStore
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class XPostsSuggestionSource @Inject constructor(
    private val xPostsStore: XPostsStore,
    override val site: SiteModel,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : SuggestionSource, CoroutineScope {
    override val coroutineContext: CoroutineContext = bgDispatcher + Job()

    private val _suggestions = MutableLiveData<List<Suggestion>>().apply {
        value = emptyList()
    }
    override val suggestions: LiveData<List<Suggestion>> = _suggestions

    init {
        launch {
            val suggestions = xPostsStore
                    .getXPostsFromDb(site)
                    .map { Suggestion.fromXpost(it) }
                    .sortedBy { it.value }
            _suggestions.postValue(suggestions)
        }
        refreshSuggestions()
    }

    override fun refreshSuggestions() {
        launch {
            val suggestions = xPostsStore
                    .fetchXPosts(site)
                    .xPosts
                    .map { Suggestion.fromXpost(it) }
                    .sortedBy { it.value }
            _suggestions.postValue(suggestions)
        }
    }

    override fun onSuggestionsUpdated() { /* Do nothing */ }
    override fun onDestroy() { /* Do nothing */ }
}
