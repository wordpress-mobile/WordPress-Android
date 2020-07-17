package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.BLOG_PREVIEW
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class ReaderPostUiStateBuilderTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var builder: ReaderPostUiStateBuilder

    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var urlUtilsWrapper: UrlUtilsWrapper
    @Mock lateinit var gravatarUtilsWrapper: GravatarUtilsWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var readerImageScannerProvider: ReaderImageScannerProvider
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder

    @Before
    fun setUp() {
        builder = ReaderPostUiStateBuilder(
                accountStore,
                urlUtilsWrapper,
                gravatarUtilsWrapper,
                dateTimeUtilsWrapper,
                readerImageScannerProvider,
                readerUtilsWrapper,
                readerPostMoreButtonUiStateBuilder
        )
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(anyOrNull())).thenReturn("")
    }

    @Test
    fun `clicks on blog header are disabled on blog preview`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, BLOG_PREVIEW)
        // Assert
        assertThat(uiState.postHeaderClickData).isNull()
    }

    private fun mapPostToUiState(post: ReaderPost, postListType: ReaderPostListType = TAG_FOLLOWED): ReaderPostUiState {
        return builder.mapPostToUiState(
                post = post,
                photonWidth = 0,
                photonHeight = 0,
                postListType = postListType,
                isBookmarkList = false,
                onButtonClicked = mock(),
                onItemClicked = mock(),
                onItemRendered = mock(),
                onDiscoverSectionClicked = mock(),
                onMoreButtonClicked = mock(),
                onVideoOverlayClicked = mock(),
                onPostHeaderViewClicked = mock()
        )
    }

    private fun createPost(): ReaderPost {
        return ReaderPost()
    }
}
