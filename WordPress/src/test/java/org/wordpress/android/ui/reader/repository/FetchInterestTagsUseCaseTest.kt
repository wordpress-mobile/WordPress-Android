package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchInterestTagsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchInterestTagsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var tagsAndSitesUpdater: ReaderTagsAndSitesUpdater

    private lateinit var useCase: FetchInterestTagsUseCase

    @Before
    fun setUp() {
        useCase = FetchInterestTagsUseCase(networkUtilsWrapper, tagsAndSitesUpdater)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `NetworkUnavailable returned when no network found`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        assertThat(useCase.fetch()).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `SuccessWithData returned when the interests are fetched`() = test {
        val interests = ReaderTagList().apply { add(mock<ReaderTag>()) }
        whenever(tagsAndSitesUpdater.fetchInterestTags()).thenReturn(interests)

        assertThat(useCase.fetch()).isEqualTo(SuccessWithData(interests))
    }

    @Test
    fun `RemoteRequestFailure returned when the interests fetch fails`() = test {
        whenever(tagsAndSitesUpdater.fetchInterestTags()).thenReturn(null)

        assertThat(useCase.fetch()).isEqualTo(RemoteRequestFailure)
    }
}
