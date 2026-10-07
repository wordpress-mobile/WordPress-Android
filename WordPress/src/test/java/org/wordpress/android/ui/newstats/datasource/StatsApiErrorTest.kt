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

    @Test
    fun `given a plan gate body and a user who can view stats, when isStatsGatedByPlan, then true`() {
        val response =
            """{"error":"unauthorized","message":"The plan for 12345 does not allow fetching Device stats"}"""

        assertThat(isStatsGatedByPlan(response, userCanViewStats = true)).isTrue()
    }

    @Test
    fun `given a localized plan gate body, when isStatsGatedByPlan, then true is returned`() {
        val response =
            """{"error":"unauthorized","message":"Der Tarif für 12345 erlaubt das Abrufen nicht"}"""

        assertThat(isStatsGatedByPlan(response, userCanViewStats = true)).isTrue()
    }

    @Test
    fun `given a user who cannot view stats, when isStatsGatedByPlan, then false is returned`() {
        val response =
            """{"error":"unauthorized","message":"user cannot view stats"}"""

        assertThat(isStatsGatedByPlan(response, userCanViewStats = false)).isFalse()
    }

    @Test
    fun `given a different error code, when isStatsGatedByPlan, then false is returned`() {
        val response = """{"error":"invalid_blog","message":"nope"}"""

        assertThat(isStatsGatedByPlan(response, userCanViewStats = true)).isFalse()
    }

    @Test
    fun `given null body, when isStatsGatedByPlan, then false is returned`() {
        assertThat(isStatsGatedByPlan(null, userCanViewStats = true)).isFalse()
    }
}
