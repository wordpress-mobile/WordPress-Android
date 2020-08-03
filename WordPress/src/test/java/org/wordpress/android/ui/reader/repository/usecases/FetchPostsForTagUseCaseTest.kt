package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.test
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.services.post.wrapper.ReaderPostServiceStarterWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider

@RunWith(MockitoJUnitRunner::class)
class FetchPostsForTagUseCaseTest {
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerPostServiceStarterWrapper: ReaderPostServiceStarterWrapper

    private lateinit var useCase: FetchPostsForTagUseCase

    @Before
    fun setUp() {
        useCase = FetchPostsForTagUseCase(
                networkUtilsWrapper,
                contextProvider,
                readerPostServiceStarterWrapper
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

    }

    @Test
    fun `NetworkUnavailable returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val readerTag = mock<ReaderTag>()

        // When
        val result = useCase.fetch(readerTag)

        // Then
        Assertions.assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `Started returned when FetchPostsForTagUseCase event is posted`() = test {
        // Given
        val readerTag = mock<ReaderTag>()

        // When
        val result = useCase.fetch(readerTag)

        // Then
        Assertions.assertThat(result).isEqualTo(Started)
    }
}
