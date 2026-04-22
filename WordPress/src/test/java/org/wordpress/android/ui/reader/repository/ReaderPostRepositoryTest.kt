package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.repository.usecases.ParseDiscoverCardsJsonUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.sources.ReaderPostLocalSource
import org.wordpress.android.util.PerAppLocaleManager

@ExperimentalCoroutinesApi
class ReaderPostRepositoryTest : BaseUnitTest() {
    private val perAppLocaleManager: PerAppLocaleManager = mock()
    private val localSource: ReaderPostLocalSource = mock()
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase = mock()
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var repository: ReaderPostRepository

    @Before
    fun setUp() {
        repository = ReaderPostRepository(
            perAppLocaleManager = perAppLocaleManager,
            localSource = localSource,
            getFollowedTagsUseCase = getFollowedTagsUseCase,
            parseDiscoverCardsJsonUseCase = parseDiscoverCardsJsonUseCase,
            appPrefsWrapper = appPrefsWrapper,
            ioDispatcher = testDispatcher(),
            applicationScope = testScope(),
        )
    }

    @Test
    fun `GIVEN Recommended tag and no stored page handle WHEN REQUEST_OLDER THEN reports UNCHANGED`() = test {
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList())
        whenever(appPrefsWrapper.getReaderDiscoverStreamPageHandle(ReaderTag.TAG_SLUG_RECOMMENDED))
            .thenReturn(null)
        val listener: UpdateResultListener = mock()

        repository.requestPostsWithTag(
            recommendedTag(),
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER,
            listener,
        )
        advanceUntilIdle()

        verify(listener).onUpdateResult(ReaderActions.UpdateResult.UNCHANGED)
        // Short-circuit must not touch the stored cursor or refresh counter.
        verify(appPrefsWrapper, never()).setReaderDiscoverStreamPageHandle(any(), any())
        verify(appPrefsWrapper, never()).incrementReaderCardsRefreshCounter()
        verifyNoInteractions(localSource)
    }

    @Test
    fun `GIVEN Recommended tag and empty stored page handle WHEN REQUEST_OLDER THEN reports UNCHANGED`() = test {
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList())
        whenever(appPrefsWrapper.getReaderDiscoverStreamPageHandle(ReaderTag.TAG_SLUG_RECOMMENDED))
            .thenReturn("")
        val listener: UpdateResultListener = mock()

        repository.requestPostsWithTag(
            recommendedTag(),
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER,
            listener,
        )
        advanceUntilIdle()

        verify(listener).onUpdateResult(ReaderActions.UpdateResult.UNCHANGED)
        verifyNoInteractions(localSource)
    }

    private fun recommendedTag() = ReaderTag(
        ReaderTag.TAG_SLUG_RECOMMENDED,
        ReaderTag.TAG_TITLE_RECOMMENDED,
        ReaderTag.TAG_TITLE_RECOMMENDED,
        "read/streams/discover",
        ReaderTagType.DEFAULT,
    )
}
