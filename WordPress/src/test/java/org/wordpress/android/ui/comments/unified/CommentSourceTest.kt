package org.wordpress.android.ui.comments.unified

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource

class CommentSourceTest {
    @Test
    fun `Should return the correct analytics comment action source for NOTIFICATION`() {
        val expected = "notifications"
        val actual = AnalyticsCommentActionSource.NOTIFICATIONS.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return the correct analytics comment action source for SITE_COMMENTS`() {
        val expected = "site_comments"
        val actual = AnalyticsCommentActionSource.SITE_COMMENTS.toString()
        assertEquals(expected, actual)
    }
}
