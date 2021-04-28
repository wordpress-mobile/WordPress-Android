package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_VIEWPOST_INTERCEPTED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ViewPostInReader
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

class ReaderLinkHandlerTest : BaseUnitTest() {
    @Mock lateinit var intentUtils: IntentUtils
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var readerLinkHandler: ReaderLinkHandler

    @Before
    fun setUp() {
        readerLinkHandler = ReaderLinkHandler(intentUtils, analyticsUtilsWrapper)
    }

    @Test
    fun `handles URI with host == read`() {
        val uri = buildUri(host = "read")

        val isReaderUri = readerLinkHandler.isReaderUrl(uri)

        assertThat(isReaderUri).isTrue()
    }

    @Test
    fun `handles URI with host == viewpost`() {
        val uri = buildUri(host = "viewpost")

        val isReaderUri = readerLinkHandler.isReaderUrl(uri)

        assertThat(isReaderUri).isTrue()
    }

    @Test
    fun `handles URI when intent utils can resolve it`() {
        val uri = buildUri(host = "reader")
        whenever(intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)).thenReturn(true)

        val isReaderUri = readerLinkHandler.isReaderUrl(uri)

        assertThat(isReaderUri).isTrue()
    }

    @Test
    fun `does not handle URI when intent utils cannot resolve it`() {
        val uri = buildUri(host = "reader")
        whenever(intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)).thenReturn(false)

        val isReaderUri = readerLinkHandler.isReaderUrl(uri)

        assertThat(isReaderUri).isFalse()
    }

    @Test
    fun `URI with read host opens reader`() {
        val uri = buildUri(host = "read")

        val navigateAction = readerLinkHandler.buildOpenInReaderNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }

    @Test
    fun `URI with viewpost host without query params opens reader`() {
        val uri = buildUri(host = "viewpost")

        val navigateAction = readerLinkHandler.buildOpenInReaderNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }

    @Test
    fun `URI with viewpost host with non-number query params opens reader`() {
        val uri = buildUri(
                host = "viewpost",
                queryParam1 = "blogId" to "abc",
                queryParam2 = "postId" to "cba"
        )

        val navigateAction = readerLinkHandler.buildOpenInReaderNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }

    @Test
    fun `URI with viewpost host with query params opens post in reader`() {
        val blogId: Long = 123
        val postId: Long = 321
        val uri = buildUri(
                host = "viewpost",
                queryParam1 = "blogId" to blogId.toString(),
                queryParam2 = "postId" to postId.toString()
        )

        val navigateAction = readerLinkHandler.buildOpenInReaderNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(ViewPostInReader(blogId, postId, uri))
        verify(analyticsUtilsWrapper).trackWithBlogPostDetails(READER_VIEWPOST_INTERCEPTED, blogId, postId)
    }

    @Test
    fun `opens URI in reader when host is neither read nor viewpost`() {
        val uri = buildUri(host = "openInReader")

        val navigateAction = readerLinkHandler.buildOpenInReaderNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenInReader(uri))
    }
}
