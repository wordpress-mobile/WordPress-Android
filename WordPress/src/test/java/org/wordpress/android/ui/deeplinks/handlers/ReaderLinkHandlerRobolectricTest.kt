package org.wordpress.android.ui.deeplinks.handlers

import androidx.core.net.toUri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

/**
 * Robolectric tests for ReaderLinkHandler that require real URI parsing.
 * These tests verify the applink deep link handling for read host with path segments.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReaderLinkHandlerRobolectricTest {
    @Mock
    lateinit var intentUtils: IntentUtils

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var readerLinkHandler: ReaderLinkHandler

    private val feedId: Long = 138734090
    private val postId: Long = 5879194632
    private val blogId: Long = 111

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        readerLinkHandler = ReaderLinkHandler(intentUtils, analyticsUtilsWrapper)
    }

    @Test
    fun `applink URI with read host and feeds posts path opens post in reader`() {
        val uri = UriWrapper("wordpress://read/feeds/$feedId/posts/$postId".toUri())

        val navigateAction = readerLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isInstanceOf(OpenInReader::class.java)
        val openInReader = navigateAction as OpenInReader
        assertThat(openInReader.uri.toString())
            .isEqualTo("https://wordpress.com/read/feeds/$feedId/posts/$postId")
    }

    @Test
    fun `applink URI with read host and blogs posts path opens post in reader`() {
        val uri = UriWrapper("wordpress://read/blogs/$blogId/posts/$postId".toUri())

        val navigateAction = readerLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isInstanceOf(OpenInReader::class.java)
        val openInReader = navigateAction as OpenInReader
        assertThat(openInReader.uri.toString())
            .isEqualTo("https://wordpress.com/read/blogs/$blogId/posts/$postId")
    }

    @Test
    fun `applink URI with read host and incomplete path opens reader`() {
        val uri = UriWrapper("wordpress://read/feeds/$feedId".toUri())

        val navigateAction = readerLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }

    @Test
    fun `applink URI with read host and missing posts segment opens reader`() {
        val uri = UriWrapper("wordpress://read/feeds/$feedId/other/$postId".toUri())

        val navigateAction = readerLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }

    @Test
    fun `applink URI with read host only opens reader`() {
        val uri = UriWrapper("wordpress://read".toUri())

        val navigateAction = readerLinkHandler.buildNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(OpenReader)
    }
}
