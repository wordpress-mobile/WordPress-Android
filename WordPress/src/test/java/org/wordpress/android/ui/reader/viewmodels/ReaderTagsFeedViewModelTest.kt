package org.wordpress.android.ui.reader.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.viewmodels.ReaderTagsFeedViewModel.FetchState

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderTagsFeedViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var readerPostRepository: ReaderPostRepository

    private lateinit var viewModel: ReaderTagsFeedViewModel

    private val collectedUiStates: MutableList<ReaderTagsFeedViewModel.UiState> = mutableListOf()

    @Before
    fun setUp() {
        viewModel = ReaderTagsFeedViewModel(testDispatcher(), readerPostRepository)
    }

    @Test
    fun `given valid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val posts = ReaderPostList()
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }

        // When
        viewModel.fetchTag(tag)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag to FetchState.Loading,
                )
            ),
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag to FetchState.Success(posts),
                )
            ),
        )
    }

    @Test
    fun `given invalid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val error = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            throw error
        }

        // When
        viewModel.fetchTag(tag)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag to FetchState.Loading,
                )
            ),
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag to FetchState.Error(error),
                )
            ),
        )
    }

    @Test
    fun `given valid tags, when fetchAll, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTag(
            "tag1",
            "tag1",
            "tag1",
            "endpoint1",
            ReaderTagType.FOLLOWED,
        )
        val tag2 = ReaderTag(
            "tag2",
            "tag2",
            "tag2",
            "endpoint2",
            ReaderTagType.FOLLOWED,
        )
        val posts1 = ReaderPostList()
        val posts2 = ReaderPostList()
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }

        // When
        viewModel.fetchAll(listOf(tag1, tag2))
        advanceUntilIdle()

        // Then

        // tag 1
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag1] == FetchState.Loading
        }
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag1] == FetchState.Success(posts1)
        }

        // tag 2
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag2] == FetchState.Loading
        }
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag2] == FetchState.Success(posts1)
        }

        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag1 to FetchState.Success(posts1),
                    tag2 to FetchState.Success(posts2),
                )
            )
        )
    }

    @Test
    fun `given valid and invalid tags, when fetchAll, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTag(
            "tag1",
            "tag1",
            "tag1",
            "endpoint1",
            ReaderTagType.FOLLOWED,
        )
        val tag2 = ReaderTag(
            "tag2",
            "tag2",
            "tag2",
            "endpoint2",
            ReaderTagType.FOLLOWED,
        )
        val posts1 = ReaderPostList()
        val error2 = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            throw error2
        }

        // When
        viewModel.fetchAll(listOf(tag1, tag2))
        advanceUntilIdle()

        // Then

        // tag 1
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag1] == FetchState.Loading
        }
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag1] == FetchState.Success(posts1)
        }

        // tag 2
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag2] == FetchState.Loading
        }
        assertThat(collectedUiStates).anyMatch {
            it.tagStates[tag2] == FetchState.Error(error2)
        }

        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState(
                mapOf(
                    tag1 to FetchState.Success(posts1),
                    tag2 to FetchState.Error(error2),
                )
            )
        )
    }

    private fun testCollectingUiStates(block: suspend TestScope.() -> Unit) = test {
        val collectedUiStatesJob = launch {
            collectedUiStates.clear()
            viewModel.uiStateFlow.toList(collectedUiStates)
        }
        this.block()
        collectedUiStatesJob.cancel()
    }
}
