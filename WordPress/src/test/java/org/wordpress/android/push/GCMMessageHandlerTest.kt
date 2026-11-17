package org.wordpress.android.push

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Covers the parsing of the notified site out of a push payload. The payload delivers blog_id as a string, and
 * these events are account-level when it is absent, so the null cases matter as much as the happy path.
 */
class GCMMessageHandlerTest {
    @Test
    fun `parsePushBlogId reads a numeric blog id`() {
        assertThat(GCMMessageHandler.parsePushBlogId("12345")).isEqualTo(12345L)
    }

    @Test
    fun `parsePushBlogId returns null when the payload has no blog id`() {
        assertThat(GCMMessageHandler.parsePushBlogId(null)).isNull()
    }

    @Test
    fun `parsePushBlogId returns null for an empty blog id`() {
        assertThat(GCMMessageHandler.parsePushBlogId("")).isNull()
    }

    @Test
    fun `parsePushBlogId returns null rather than throwing for an unparsable blog id`() {
        assertThat(GCMMessageHandler.parsePushBlogId("not-a-blog-id")).isNull()
    }
}
