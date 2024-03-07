package org.wordpress.android.ui.posts

import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.PostImmutableModel
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostFreshnessCheckerImplTest {
    private val fixedCurrentTimeMillis = 100_000L // Example current time in milliseconds
    private val timeProvider = object : TimeProvider {
        override fun currentTimeMillis() = fixedCurrentTimeMillis
    }

    private val postFreshnessChecker = PostFreshnessCheckerImpl(timeProvider)

    @Test
    fun `should refresh post when post is older than cache validity`() {
        // Post timestamp is set to simulate being older than the cache validity
        val postTimestamp = fixedCurrentTimeMillis - PostFreshnessCheckerImpl.CACHE_VALIDITY_MILLIS - 1
        val post = mock<PostImmutableModel> {
            on { dbTimestamp }.thenReturn(postTimestamp)
        }

        // Adjust the system time or post creation time as needed to reflect the scenario being tested
        assertTrue(postFreshnessChecker.shouldRefreshPost(post))
    }

    @Test
    fun `should not refresh post when post is within cache validity`() {
        // Post timestamp is set to simulate being within the cache validity period
        val postTimestamp = fixedCurrentTimeMillis - PostFreshnessCheckerImpl.CACHE_VALIDITY_MILLIS + 1
        val post = mock<PostImmutableModel> {
            on { dbTimestamp }.thenReturn(postTimestamp)
        }

        assertFalse(postFreshnessChecker.shouldRefreshPost(post))
    }
}
