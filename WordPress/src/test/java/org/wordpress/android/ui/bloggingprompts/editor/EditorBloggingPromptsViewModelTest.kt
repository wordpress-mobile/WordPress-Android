package org.wordpress.android.ui.bloggingprompts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsPostTagProvider
import org.wordpress.android.ui.posts.BloggingPromptsEditorBlockMapper
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel.EditorLoadedPrompt
import java.util.Date

@ExperimentalCoroutinesApi
class EditorBloggingPromptsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var siteModel: SiteModel

    private lateinit var viewModel: EditorBloggingPromptsViewModel
    private var loadedPrompt: EditorLoadedPrompt? = null

    private val bloggingPrompt = BloggingPromptsResult(
        model = BloggingPromptModel(
            id = 123,
            text = "title",
            title = "",
            content = "content",
            date = Date(),
            isAnswered = false,
            attribution = "",
            respondentsCount = 5,
            respondentsAvatarUrls = listOf()
        )
    )
    private val bloggingPromptsStore: BloggingPromptsStore = mock {
        onBlocking { getPromptById(any(), any()) } doReturn flowOf(bloggingPrompt)
    }
    private val bloggingPromptsBlock = "blogging_prompts_block"
    private val bloggingPromptsEditorBlockMapper: BloggingPromptsEditorBlockMapper = mock {
        on { it.map(any()) } doReturn bloggingPromptsBlock
    }

    @Before
    fun setUp() {
        viewModel = EditorBloggingPromptsViewModel(
            bloggingPromptsStore,
            bloggingPromptsEditorBlockMapper,
            testDispatcher()
        )

        viewModel.onBloggingPromptLoaded.observeForever {
            it.applyIfNotHandled {
                loadedPrompt = this
            }
        }
    }

    @Test
    fun `starting VM fetches a prompt and posts it to onBloggingPromptLoaded`() = test {
        viewModel.start(siteModel, 123)

        assertThat(loadedPrompt?.promptId).isEqualTo(bloggingPrompt.model?.id)

        verify(bloggingPromptsStore, times(1)).getPromptById(any(), any())
    }

    @Test
    fun `should NOT execute start method if prompt ID is less than 0`() = test {
        viewModel.start(siteModel, -1)
        verify(bloggingPromptsStore, times(0)).getPromptById(any(), any())
    }

    @Test
    fun `should load blogging prompt mapped block`() {
        viewModel.start(siteModel, 123)
        assertThat(loadedPrompt?.content).isEqualTo(bloggingPromptsBlock)
    }

    @Test
    fun `should add prompt id tag`() {
        viewModel.start(siteModel, 123)
        assertThat(loadedPrompt?.tags).containsOnly(
            BloggingPromptsPostTagProvider.BLOGGING_PROMPT_TAG,
            BloggingPromptsPostTagProvider.promptIdTag(123)
        )
    }
}
