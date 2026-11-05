package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchFollowedTagsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchFollowedTagsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var tagsAndSitesUpdater: ReaderTagsAndSitesUpdater

    private lateinit var useCase: FetchFollowedTagsUseCase

    @Before
    fun setUp() {
        useCase = FetchFollowedTagsUseCase(networkUtilsWrapper, tagsAndSitesUpdater)
    }

    @Test
    fun `NetworkUnavailable returned when no network found`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        assertThat(useCase.fetch()).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `Success returned when the tags update succeeds`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(tagsAndSitesUpdater.updateTags()).thenReturn(true)

        assertThat(useCase.fetch()).isEqualTo(Success)
    }

    @Test
    fun `RemoteRequestFailure returned when the tags update fails`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(tagsAndSitesUpdater.updateTags()).thenReturn(false)

        assertThat(useCase.fetch()).isEqualTo(RemoteRequestFailure)
    }
}
