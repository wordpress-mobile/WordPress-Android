package org.wordpress.android.ui.newstats.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.StatsVisitsDataValue
import uniffi.wp_api.StatsVisitsResponse

/**
 * Tests for StatsVisitsResponse extension functions.
 * These tests verify that our extension functions correctly wrap the wordpress-rs helper functions.
 */
class StatsVisitsResponseExtensionsTest {
    @Test
    fun `statsVisitsData returns visits data points with correct values`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "views"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(100u)
                ),
                listOf(
                    StatsVisitsDataValue.String("2024-01-17"),
                    StatsVisitsDataValue.Number(200u)
                )
            )
        )

        // When
        val result = response.statsVisitsData()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].visits).isEqualTo(100uL)
        assertThat(result[1].period).isEqualTo("2024-01-17")
        assertThat(result[1].visits).isEqualTo(200uL)
    }

    @Test
    fun `statsVisitsData returns empty list when data is empty`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "views"),
            data = emptyList()
        )

        // When
        val result = response.statsVisitsData()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `statsVisitorsData returns visitors data points with correct values`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "visitors"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(50u)
                )
            )
        )

        // When
        val result = response.statsVisitorsData()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].visitors).isEqualTo(50uL)
    }

    @Test
    fun `statsLikesData returns likes data points with correct values`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "likes"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(25u)
                )
            )
        )

        // When
        val result = response.statsLikesData()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].likes).isEqualTo(25uL)
    }

    @Test
    fun `statsCommentsData returns comments data points with correct values`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "comments"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(10u)
                )
            )
        )

        // When
        val result = response.statsCommentsData()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].comments).isEqualTo(10uL)
    }

    @Test
    fun `statsPostsData returns posts data points with correct values`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "posts"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(5u)
                )
            )
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].posts).isEqualTo(5uL)
    }

    @Test
    fun `statsPostsData returns empty list when data is empty`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "posts"),
            data = emptyList()
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `extension functions correctly find fields regardless of order`() {
        // Given - fields in non-standard order
        val response = createResponse(
            fields = listOf("comments", "visitors", "period", "likes", "views", "posts"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.Number(10u),   // comments
                    StatsVisitsDataValue.Number(50u),   // visitors
                    StatsVisitsDataValue.String("2024-01-16"), // period
                    StatsVisitsDataValue.Number(25u),   // likes
                    StatsVisitsDataValue.Number(100u),  // views
                    StatsVisitsDataValue.Number(5u)     // posts
                )
            )
        )

        // When
        val visits = response.statsVisitsData()
        val visitors = response.statsVisitorsData()
        val likes = response.statsLikesData()
        val comments = response.statsCommentsData()
        val posts = response.statsPostsData()

        // Then
        assertThat(visits).hasSize(1)
        assertThat(visits[0].visits).isEqualTo(100uL)

        assertThat(visitors).hasSize(1)
        assertThat(visitors[0].visitors).isEqualTo(50uL)

        assertThat(likes).hasSize(1)
        assertThat(likes[0].likes).isEqualTo(25uL)

        assertThat(comments).hasSize(1)
        assertThat(comments[0].comments).isEqualTo(10uL)

        assertThat(posts).hasSize(1)
        assertThat(posts[0].posts).isEqualTo(5uL)
    }

    private fun createResponse(
        fields: List<String>,
        data: List<List<StatsVisitsDataValue>>
    ): StatsVisitsResponse {
        return StatsVisitsResponse(
            date = "2024-01-16",
            unit = "day",
            fields = fields,
            data = data
        )
    }
}
