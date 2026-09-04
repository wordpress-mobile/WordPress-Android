package org.wordpress.android.ui.posts.prepublishing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.UpdatePostTagsUseCase
import org.wordpress.android.ui.posts.prepublishing.tags.PrepublishingTagsUiState
import org.wordpress.android.ui.posts.prepublishing.tags.PrepublishingTagsViewModel
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class PrepublishingTagsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingTagsViewModel

    @Mock
    lateinit var getPostTagsUseCase: GetPostTagsUseCase

    @Mock
    lateinit var updatePostTagsUseCase: UpdatePostTagsUseCase

    @Mock
    lateinit var editPostRepository: EditPostRepository

    @Before
    fun setup() {
        viewModel = PrepublishingTagsViewModel(
            getPostTagsUseCase,
            updatePostTagsUseCase,
            testDispatcher(),
            testDispatcher()
        )
    }

    @Test
    fun `when viewModel is started the post's existing tags are exposed as selected tags`() {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn("test,nature")
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository)

        assertThat(uiState?.selectedTags).containsExactly("test", "nature")
    }

    @Test
    fun `duplicate initial post tags are de-duplicated case-insensitively into a single chip`() {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn("news,News,tech")
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository)

        assertThat(uiState?.selectedTags).containsExactly("news", "tech")
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever {
            event = it
        }

        viewModel.onBackButtonClicked()

        assertThat(event).isNotNull
    }

    @Test
    fun `when a tag is added updatePostTagsUseCase's updateTags is called with the joined tags`() = test {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn(null)
        val captor = ArgumentCaptor.forClass(String::class.java)
        doNothing().whenever(updatePostTagsUseCase).updateTags(captor.capture(), any())

        viewModel.start(editPostRepository)
        viewModel.onTagAdded("test")
        viewModel.onTagAdded("nature")
        advanceUntilIdle()

        assertThat(captor.value).isEqualTo("test,nature")
    }

    @Test
    fun `a burst of added tags is persisted with a single throttled write`() = test {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn(null)

        viewModel.start(editPostRepository)
        viewModel.onTagAdded("a")
        viewModel.onTagAdded("b")
        viewModel.onTagAdded("c")
        advanceUntilIdle()

        verify(updatePostTagsUseCase, times(1)).updateTags(eq("a,b,c"), any())
    }

    @Test
    fun `pending input is committed and persisted when the screen is dismissed`() = test {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn(null)
        val captor = ArgumentCaptor.forClass(String::class.java)
        doNothing().whenever(updatePostTagsUseCase).updateTags(captor.capture(), any())
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository)
        viewModel.onInputChanged("vacation")
        viewModel.commitPendingTag()
        viewModel.onBackButtonClicked()

        assertThat(uiState?.selectedTags).containsExactly("vacation")
        assertThat(captor.value).isEqualTo("vacation")
    }

    @Test
    fun `duplicate site tag names are suggested only once`() {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn(null)
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository, listOf("news", "news", "tech"))

        assertThat(uiState?.suggestions).containsExactly("news", "tech")
    }

    @Test
    fun `when a tag is removed updatePostTagsUseCase's updateTags is called with the remaining tags`() = test {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn("test,nature")
        val captor = ArgumentCaptor.forClass(String::class.java)
        doNothing().whenever(updatePostTagsUseCase).updateTags(captor.capture(), any())

        viewModel.start(editPostRepository)
        viewModel.onTagRemoved("test")
        advanceUntilIdle()

        assertThat(captor.value).isEqualTo("nature")
    }

    @Test
    fun `when the same tag is added twice it is not duplicated`() = test {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn(null)
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository)
        viewModel.onTagAdded("test")
        viewModel.onTagAdded("TEST")
        advanceUntilIdle()

        assertThat(uiState?.selectedTags).containsExactly("test")
    }

    @Test
    fun `suggestions are filtered by the current input and exclude already selected tags`() {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn("nature")
        var uiState: PrepublishingTagsUiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository, listOf("nature", "travel", "national park"))
        viewModel.onInputChanged("nat")

        // "nature" is already selected so it is excluded; only the remaining match is suggested
        assertThat(uiState?.suggestions).containsExactly("national park")
    }

    @Test
    fun `wereTagsChanged returns true only after the selection differs from the initial tags`() {
        whenever(getPostTagsUseCase.getTags(any())).thenReturn("test")

        viewModel.start(editPostRepository)
        assertThat(viewModel.wereTagsChanged()).isFalse

        viewModel.onTagAdded("nature")
        assertThat(viewModel.wereTagsChanged()).isTrue
    }

    @Test
    fun `when viewModel is started then dismissKeyboard is called when tapping back`() {
        var event: Event<Unit>? = null
        viewModel.dismissKeyboard.observeForever {
            event = it
        }

        viewModel.start(editPostRepository)
        viewModel.onBackButtonClicked()

        assertThat(event).isNotNull
    }
}
