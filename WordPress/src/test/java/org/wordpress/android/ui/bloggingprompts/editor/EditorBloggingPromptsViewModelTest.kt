package org.wordpress.android.ui.bloggingprompts.editor

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.test
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel

@InternalCoroutinesApi
class EditorBloggingPromptsViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingPromptsStore: BloggingPromptsStore
    @Mock lateinit var siteModel: SiteModel

    private lateinit var viewModel: EditorBloggingPromptsViewModel
    private var loadedPromptContent: String? = null

    @Before
    fun setUp() {
        viewModel = EditorBloggingPromptsViewModel(
                bloggingPromptsStore,
                TEST_DISPATCHER
        )


        viewModel.onBloggingPromptLoaded.observeForever {
            it.applyIfNotHandled {
                loadedPromptContent = this
            }
        }
    }

    @Test
    fun `starting VM fetches a prompt and posts it to onBloggingPromptLoaded`() = test {
        viewModel.start(siteModel, 123)

        verify(bloggingPromptsStore, times(1)).getPromptById(any(), any())
    }
}
