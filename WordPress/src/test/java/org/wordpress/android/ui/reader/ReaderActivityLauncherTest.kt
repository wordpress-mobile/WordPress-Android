package org.wordpress.android.ui.reader

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReaderActivityLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val feedId = 12345L
    private val tagSlug = "dogs"
    private val source = "deeplink"

    @Test
    fun `buildReaderFeedIntent creates intent with correct feed id`() {
        val intent = ReaderActivityLauncher.buildReaderFeedIntent(context, feedId, source)

        assertThat(intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0L)).isEqualTo(feedId)
    }

    @Test
    fun `buildReaderFeedIntent creates intent with is feed flag set to true`() {
        val intent = ReaderActivityLauncher.buildReaderFeedIntent(context, feedId, source)

        assertThat(intent.getBooleanExtra(ReaderConstants.ARG_IS_FEED, false)).isTrue()
    }

    @Test
    fun `buildReaderFeedIntent creates intent with correct source`() {
        val intent = ReaderActivityLauncher.buildReaderFeedIntent(context, feedId, source)

        assertThat(intent.getStringExtra(ReaderConstants.ARG_SOURCE)).isEqualTo(source)
    }

    @Test
    fun `buildReaderFeedIntent creates intent with blog preview list type`() {
        val intent = ReaderActivityLauncher.buildReaderFeedIntent(context, feedId, source)

        @Suppress("DEPRECATION")
        val listType = intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE) as ReaderPostListType
        assertThat(listType).isEqualTo(ReaderPostListType.BLOG_PREVIEW)
    }

    @Test
    fun `buildReaderFeedIntent creates intent for ReaderPostListActivity`() {
        val intent = ReaderActivityLauncher.buildReaderFeedIntent(context, feedId, source)

        assertThat(intent.component?.className).isEqualTo(ReaderPostListActivity::class.java.name)
    }

    @Test
    fun `buildReaderTagIntent creates intent with correct tag slug`() {
        val intent = ReaderActivityLauncher.buildReaderTagIntent(context, tagSlug, source)

        @Suppress("DEPRECATION")
        val tag = intent.getSerializableExtra(ReaderConstants.ARG_TAG) as ReaderTag
        assertThat(tag.tagSlug).isEqualTo(tagSlug)
    }

    @Test
    fun `buildReaderTagIntent creates intent with correct source`() {
        val intent = ReaderActivityLauncher.buildReaderTagIntent(context, tagSlug, source)

        assertThat(intent.getStringExtra(ReaderConstants.ARG_SOURCE)).isEqualTo(source)
    }

    @Test
    fun `buildReaderTagIntent creates intent with tag preview list type`() {
        val intent = ReaderActivityLauncher.buildReaderTagIntent(context, tagSlug, source)

        @Suppress("DEPRECATION")
        val listType = intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE) as ReaderPostListType
        assertThat(listType).isEqualTo(ReaderPostListType.TAG_PREVIEW)
    }

    @Test
    fun `buildReaderTagIntent creates intent for ReaderPostListActivity`() {
        val intent = ReaderActivityLauncher.buildReaderTagIntent(context, tagSlug, source)

        assertThat(intent.component?.className).isEqualTo(ReaderPostListActivity::class.java.name)
    }

    @Test
    fun `createReaderSearchIntent creates intent for ReaderSearchActivity`() {
        val intent = ReaderActivityLauncher.createReaderSearchIntent(context)

        assertThat(intent.component?.className).isEqualTo(ReaderSearchActivity::class.java.name)
    }
}
