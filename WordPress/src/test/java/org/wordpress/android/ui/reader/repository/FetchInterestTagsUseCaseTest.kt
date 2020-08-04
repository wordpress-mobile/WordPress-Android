package org.wordpress.android.ui.reader.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.test
import org.wordpress.android.ui.reader.ReaderEvents.InterestTagsFetchEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchInterestTagsUseCase
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.INTEREST_TAGS
import org.wordpress.android.ui.reader.services.update.wrapper.ReaderUpdateServiceStarterWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet

@RunWith(MockitoJUnitRunner::class)
class FetchInterestTagsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var eventBusWrapper: EventBusWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerUpdateServiceStarterWrapper: ReaderUpdateServiceStarterWrapper

    private lateinit var useCase: FetchInterestTagsUseCase

    @Before
    fun setUp() {
        useCase = FetchInterestTagsUseCase(
                contextProvider,
                eventBusWrapper,
                networkUtilsWrapper,
                readerUpdateServiceStarterWrapper
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `NetworkUnavailable returned when no network found`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // When
        val result = useCase.fetch()

        // Then
        Assertions.assertThat(result).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `SuccessWithData returned when InterestTagsFetchEnded event is posted with true`() = test {
        // Given
        val readerTag = mock<ReaderTag>()
        val event = InterestTagsFetchEnded(ReaderTagList().apply { add(readerTag) }, true)
        whenever(
                readerUpdateServiceStarterWrapper.startService(
                        contextProvider.getContext(),
                        EnumSet.of(INTEREST_TAGS)
                )
        ).then { useCase.onInterestTagsFetchEnded(event) }

        // When
        val result = useCase.fetch()

        // Then
        Assertions.assertThat(result).isEqualTo(SuccessWithData(event.interestTags))
    }

    @Test
    fun `RemoteRequestFailure returned when InterestTagsFetchEnded event is posted with false`() = test {
        // Given
        val event = InterestTagsFetchEnded(ReaderTagList(), false)
        whenever(
                readerUpdateServiceStarterWrapper.startService(
                        contextProvider.getContext(),
                        EnumSet.of(INTEREST_TAGS)
                )
        ).then { useCase.onInterestTagsFetchEnded(event) }

        // When
        val result = useCase.fetch()

        // Then
        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }
}
