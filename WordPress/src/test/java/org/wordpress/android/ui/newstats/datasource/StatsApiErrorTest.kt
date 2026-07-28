package org.wordpress.android.ui.newstats.datasource

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsApiErrorTest {
    @Test
    fun `given invalid_blog body, when parseStatsApiErrorCode, then error code is returned`() {
        val response =
            """{"error":"invalid_blog","message":"File download stats are not available for Jetpack sites"}"""

        assertThat(parseStatsApiErrorCode(response)).isEqualTo("invalid_blog")
    }

    @Test
    fun `given spaced json, when parseStatsApiErrorCode, then error code is returned`() {
        val response = """{ "error" : "invalid_blog" , "message": "nope" }"""

        assertThat(parseStatsApiErrorCode(response)).isEqualTo("invalid_blog")
    }

    @Test
    fun `given body without error field, when parseStatsApiErrorCode, then null is returned`() {
        val response = """{"message":"Something went wrong"}"""

        assertThat(parseStatsApiErrorCode(response)).isNull()
    }

    @Test
    fun `given null or blank body, when parseStatsApiErrorCode, then null is returned`() {
        assertThat(parseStatsApiErrorCode(null)).isNull()
        assertThat(parseStatsApiErrorCode("")).isNull()
        assertThat(parseStatsApiErrorCode("   ")).isNull()
    }

    @Test
    fun `given invalid_blog body, when isStatsUnavailableForSite, then true is returned`() {
        val response =
            """{"error":"invalid_blog","message":"File download stats are not available for Jetpack sites"}"""

        assertThat(isStatsUnavailableForSite(response)).isTrue()
    }

    @Test
    fun `given a different error code, when isStatsUnavailableForSite, then false is returned`() {
        val response = """{"error":"unauthorized","message":"nope"}"""

        assertThat(isStatsUnavailableForSite(response)).isFalse()
    }

    @Test
    fun `given null body, when isStatsUnavailableForSite, then false is returned`() {
        assertThat(isStatsUnavailableForSite(null)).isFalse()
    }
}
