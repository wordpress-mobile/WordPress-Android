package org.wordpress.android.ui.bloggingprompts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

    private val bloggingPrompt = listOf(
        BloggingPromptsResult(
            model = BloggingPromptModel(
                id = 123,
                text = "title",
                date = Date(),
                isAnswered = false,
                attribution = "",
                respondentsCount = 5,
                respondentsAvatarUrls = listOf(),
                answeredLink = "https://wordpress.com/tag/dailyprompt-123",
            )
        ),
        BloggingPromptsResult(
            model = BloggingPromptModel(
                id = 321,
                text = "title",
                date = Date(),
                isAnswered = false,
                attribution = "",
                respondentsCount = 10,
                respondentsAvatarUrls = listOf(),
                answeredLink = "https://wordpress.com/tag/dailyprompt-321",
                bloganuaryId = "bloganuaryTag"
            )
        )
    )
    private val bloggingPromptsStore: BloggingPromptsStore = mock {
        onBlocking { getPromptById(any(), any()) } doAnswer { mock ->
            flowOf(bloggingPrompt.first { it.model?.id == mock.arguments[1] })
        }
    }
    private val bloggingPromptsBlock = "blogging_prompts_block"
    private val bloggingPromptsEditorBlockMapper: BloggingPromptsEditorBlockMapper = mock {
        on { it.map(any()) } doReturn bloggingPromptsBlock
    }
    private val bloggingPromptsPostTagProvider: BloggingPromptsPostTagProvider = mock()

    @Before
    fun setUp() {
        viewModel = EditorBloggingPromptsViewModel(
            bloggingPromptsStore,
            bloggingPromptsEditorBlockMapper,
            bloggingPromptsPostTagProvider,
            testDispatcher(),
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

        assertThat(loadedPrompt?.promptId).isEqualTo(123)

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
        whenever(bloggingPromptsPostTagProvider.promptIdTag(any())).thenReturn("promptIdTag")
        viewModel.start(siteModel, 123)
        assertThat(loadedPrompt?.tags).containsOnly(
            BloggingPromptsPostTagProvider.BLOGGING_PROMPT_TAG,
            "promptIdTag"
        )
    }

    @Test
    fun `should add bloganuary tags`() {
        whenever(bloggingPromptsPostTagProvider.promptIdTag(any())).thenReturn("promptIdTag")
        viewModel.start(siteModel, 321)
        assertThat(loadedPrompt?.tags).containsOnly(
            BloggingPromptsPostTagProvider.BLOGGING_PROMPT_TAG,
            "promptIdTag",
            BloggingPromptsPostTagProvider.BLOGANUARY_TAG,
            "bloganuaryTag"
        )
    }
}
