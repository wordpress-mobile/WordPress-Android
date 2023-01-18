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
    private val _onBloggingPromptLoaded = MutableLiveData<Event<EditorLoadedPrompt>>()
    val onBloggingPromptLoaded: LiveData<Event<EditorLoadedPrompt>> = _onBloggingPromptLoaded

    private var isStarted = false

    fun start(site: SiteModel, bloggingPromptId: Int) {
        if (bloggingPromptId < 0) {
            return
        }
        if (isStarted) {
            return
        }
        isStarted = true
        loadPrompt(site, bloggingPromptId)
    }

    private fun loadPrompt(site: SiteModel, promptId: Int) = launch {
        val prompt = bloggingPromptsStore.getPromptById(site, promptId).first().model
        prompt?.let {
            _onBloggingPromptLoaded.postValue(
                Event(
                    EditorLoadedPrompt(
                        promptId,
                        it.content,
                        createPromptTags(promptId)
                    )
                )
            )
        }
    }

    private fun createPromptTags(promptId: Int): List<String> = listOf(
        BLOGGING_PROMPT_TAG,
        BLOGGING_PROMPT_SPECIFIC_TAG_TEMPLATE.format(promptId)
    )

    data class EditorLoadedPrompt(val promptId: Int, val content: String, val tags: List<String>)
}

internal const val BLOGGING_PROMPT_TAG = "dailyprompt"
internal const val BLOGGING_PROMPT_SPECIFIC_TAG_TEMPLATE = "dailyprompt-%s"
