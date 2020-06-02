package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository

@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderInterestsViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(TEST_DISPATCHER, readerTagRepository)
    }

    @Test
    fun `interests ui correctly loaded if non empty interests received from repo on start`() = test {
        // Given
        val mockInterests = getMockInterests()
        whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

        // When
        initViewModel()

        // Then
        assertThat(requireNotNull(viewModel.uiState.value).interests).isEqualTo(mockInterests)
        assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[0])
                .isInstanceOf(InterestUiState::class.java)
        assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[0].title)
                .isEqualTo(mockInterests[0].tagTitle)
    }

    @Test
    fun `interests ui not updated if no tags received from repo on start`() = test {
        // Given
        whenever(readerTagRepository.getInterests()).thenReturn(ReaderTagList())

        // When
        initViewModel()

        // Then
        assertThat(viewModel.uiState.value).isNull()
    }

    private fun initViewModel() = viewModel.start()

    private fun getMockInterests() =
        ReaderTagList().apply {
            for (c in 'A'..'Z')
                (add(
                    ReaderTag(
                        c.toString(), c.toString(), c.toString(),
                        "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                        ReaderTagType.DEFAULT
                    )
                ))
        }
}
