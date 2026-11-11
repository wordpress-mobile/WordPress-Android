package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class ReaderTagRepositoryTest : BaseUnitTest() {
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var tagsAndSitesUpdater: ReaderTagsAndSitesUpdater
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var getFollowedTagsUseCase: GetFollowedTagsUseCase
    @Mock lateinit var shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase
    @Mock lateinit var readerTagTableWrapper: ReaderTagTableWrapper

    private lateinit var repository: ReaderTagRepository

    @Before
    fun setUp() {
        whenever(readerUtilsWrapper.getTagFromTagName(any(), any())).thenReturn(mock())
        repository = ReaderTagRepository(
            testDispatcher(),
            testDispatcher(),
            readerUtilsWrapper,
            tagsAndSitesUpdater,
            networkUtilsWrapper,
            accountStore,
            getFollowedTagsUseCase,
            shouldAutoUpdateTagUseCase,
            readerTagTableWrapper
        )
    }

    @Test
    fun `getInterests returns NetworkUnavailable when there's no connection`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        assertThat(repository.getInterests()).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `getInterests returns the fetched interests`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val interests = ReaderTagList().apply { add(mock<ReaderTag>()) }
        whenever(tagsAndSitesUpdater.fetchInterestTags()).thenReturn(interests)

        assertThat(repository.getInterests()).isEqualTo(SuccessWithData(interests))
    }

    @Test
    fun `getInterests returns RemoteRequestFailure when the fetch fails`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(tagsAndSitesUpdater.fetchInterestTags()).thenReturn(null)

        assertThat(repository.getInterests()).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `getUserTags returns the tags after a successful update`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val tags = ReaderTagList().apply { add(mock<ReaderTag>()) }
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList(), tags)
        whenever(shouldAutoUpdateTagUseCase.get(any())).thenReturn(true)
        whenever(tagsAndSitesUpdater.updateTags()).thenReturn(true)

        assertThat(repository.getUserTags()).isEqualTo(SuccessWithData(tags))
    }

    @Test
    fun `getUserTags returns RemoteRequestFailure when the update fails`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList())
        whenever(shouldAutoUpdateTagUseCase.get(any())).thenReturn(true)
        whenever(tagsAndSitesUpdater.updateTags()).thenReturn(false)

        assertThat(repository.getUserTags()).isEqualTo(RemoteRequestFailure)
    }

    @Test
    fun `saveInterests returns NetworkUnavailable when logged in without a connection`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        assertThat(repository.saveInterests(listOf(mock()))).isEqualTo(NetworkUnavailable)
    }

    @Test
    fun `saveInterests returns Success when the tags are followed`() = test {
        val tags = listOf<ReaderTag>(mock())
        whenever(tagsAndSitesUpdater.followTags(tags)).thenReturn(true)

        assertThat(repository.saveInterests(tags)).isEqualTo(Success)
    }
}
