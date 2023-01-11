package org.wordpress.android.ui.posts.editor

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Editor.GUTENBERG
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class PostEditorAnalyticsSessionTest {
    @Test
    fun `verify editor_session events have blog_id field`() {
        // Arrange
        val analyticsTracker = mock<AnalyticsTrackerWrapper>()
        val postEditorAnalyticsSession = createPostEditorAnalyticsSessionTracker(analyticsTracker)

        // trigger all the editor_session events
        postEditorAnalyticsSession.start(null, true, null)
        postEditorAnalyticsSession.end()
        postEditorAnalyticsSession.switchEditor(GUTENBERG)
        postEditorAnalyticsSession.applyTemplate("Just a template name")

        // verify that all invocations have the blog_id entry
        verify(analyticsTracker, times(4))
            .track(anyOrNull(), argThat(MapHasEntry("blog_id" to blogId)))
    }

    internal class MapHasEntry(val entry: Pair<*, *>) : ArgumentMatcher<Map<String, Any?>> {
        override fun matches(map: Map<String, Any?>): Boolean {
            return map.containsKey(entry.first) && map.get(entry.first) == entry.second
        }

        override fun toString(): String {
            // to print when verification fails
            return "[Map containing a (\"${entry.first}\", ${entry.second}L) pair]"
        }
    }

    private companion object Fixtures {
        private const val blogId = 1234L

        fun createPostEditorAnalyticsSessionTracker(
            analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
        ): PostEditorAnalyticsSession {
            val site = mock<SiteModel>()
            whenever(site.origin).thenReturn(SiteModel.ORIGIN_WPCOM_REST)
            whenever(site.siteId).thenReturn(blogId)
            site.getSiteId()
            return PostEditorAnalyticsSession.getNewPostEditorAnalyticsSession(
                GUTENBERG,
                mock(),
                site,
                false,
                analyticsTrackerWrapper
            )
        }
    }
}
