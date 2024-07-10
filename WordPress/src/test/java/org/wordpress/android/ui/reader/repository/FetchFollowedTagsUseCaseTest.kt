package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsFetched
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.wrapper.ReaderUpdateServiceStarterWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchFollowedTagsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var contextProvider: ContextProvider

    @Mock
    lateinit var eventBusWrapper: EventBusWrapper

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var readerUpdateServiceStarterWrapper: ReaderUpdateServiceStarterWrapper

    private lateinit var useCase: FetchFollowedTagsUseCase

    @Before
    fun setUp() {
        useCase = FetchFollowedTagsUseCase(
            contextProvider,
            eventBusWrapper,
            networkUtilsWrapper,
            readerUpdateServiceStarterWrapper
        )
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
    fun `Success returned when FollowedTagsFetched event is posted with success`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val event = FollowedTagsFetched(true, 10)
        whenever(readerUpdateServiceStarterWrapper.startService(contextProvider.getContext(), EnumSet.of(TAGS)))
            .then { useCase.onFollowedTagsFetched(event) }

        // When
        val result = useCase.fetch()

        // Then
        Assertions.assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `RemoteRequestFailure returned when FollowedTagsFetched event is posted with failure`() = test {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val event = FollowedTagsFetched(false, 10)
        whenever(readerUpdateServiceStarterWrapper.startService(contextProvider.getContext(), EnumSet.of(TAGS)))
            .then { useCase.onFollowedTagsFetched(event) }

        // When
        val result = useCase.fetch()

        // Then
        Assertions.assertThat(result).isEqualTo(RemoteRequestFailure)
    }
}
