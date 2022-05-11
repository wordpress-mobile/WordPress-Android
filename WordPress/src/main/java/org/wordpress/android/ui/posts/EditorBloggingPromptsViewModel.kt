package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class EditorBloggingPromptsViewModel
@Inject constructor(
    private val bloggingPromptsStore: BloggingPromptsStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private val _onBloggingPromptLoaded = MutableLiveData<Event<String>>()
    val onBloggingPromptLoaded: LiveData<Event<String>> = _onBloggingPromptLoaded

    private var isStarted = false
    private lateinit var site: SiteModel

    private var bloggingPromptId = 0

    fun start(site: SiteModel, bloggingPromptId: Int) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.bloggingPromptId = bloggingPromptId
        this.site = site
        loadPrompt()
    }

    private fun loadPrompt() = launch {
        val prompt = bloggingPromptsStore.getPromptById(site, bloggingPromptId).first().model
        prompt?.let { _onBloggingPromptLoaded.value = Event(it.content) }
    }
}
