package org.wordpress.android.ui.newstats.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.StatsVisitsDataValue
import uniffi.wp_api.StatsVisitsResponse

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
    fun `statsVisitorsData returns empty list when visitors field is missing`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "views"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(100u)
                )
            )
        )

        // When
        val result = response.statsVisitorsData()

        // Then
        assertThat(result).isEmpty()
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
    fun `extension functions correctly find fields regardless of order`() {
        // Given - fields in non-standard order
        val response = createResponse(
            fields = listOf("comments", "visitors", "period", "likes", "views"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.Number(10u),   // comments
                    StatsVisitsDataValue.Number(50u),   // visitors
                    StatsVisitsDataValue.String("2024-01-16"), // period
                    StatsVisitsDataValue.Number(25u),   // likes
                    StatsVisitsDataValue.Number(100u)   // views
                )
            )
        )

        // When
        val visits = response.statsVisitsData()
        val visitors = response.statsVisitorsData()
        val likes = response.statsLikesData()
        val comments = response.statsCommentsData()

        // Then
        assertThat(visits).hasSize(1)
        assertThat(visits[0].visits).isEqualTo(100uL)

        assertThat(visitors).hasSize(1)
        assertThat(visitors[0].visitors).isEqualTo(50uL)

        assertThat(likes).hasSize(1)
        assertThat(likes[0].likes).isEqualTo(25uL)

        assertThat(comments).hasSize(1)
        assertThat(comments[0].comments).isEqualTo(10uL)
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
    fun `statsPostsData returns empty list when posts field is missing`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "views"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(100u)
                )
            )
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `statsPostsData returns empty list when period field is missing`() {
        // Given
        val response = createResponse(
            fields = listOf("posts"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.Number(5u)
                )
            )
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).isEmpty()
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
    fun `statsPostsData correctly handles multiple data points`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "posts"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(5u)
                ),
                listOf(
                    StatsVisitsDataValue.String("2024-01-17"),
                    StatsVisitsDataValue.Number(3u)
                ),
                listOf(
                    StatsVisitsDataValue.String("2024-01-18"),
                    StatsVisitsDataValue.Number(7u)
                )
            )
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[0].posts).isEqualTo(5uL)
        assertThat(result[1].period).isEqualTo("2024-01-17")
        assertThat(result[1].posts).isEqualTo(3uL)
        assertThat(result[2].period).isEqualTo("2024-01-18")
        assertThat(result[2].posts).isEqualTo(7uL)
    }

    @Test
    fun `statsPostsData skips rows with invalid data types`() {
        // Given
        val response = createResponse(
            fields = listOf("period", "posts"),
            data = listOf(
                listOf(
                    StatsVisitsDataValue.String("2024-01-16"),
                    StatsVisitsDataValue.Number(5u)
                ),
                listOf(
                    StatsVisitsDataValue.String("2024-01-17"),
                    StatsVisitsDataValue.String("not a number") // Invalid: posts should be Number
                ),
                listOf(
                    StatsVisitsDataValue.String("2024-01-18"),
                    StatsVisitsDataValue.Number(7u)
                )
            )
        )

        // When
        val result = response.statsPostsData()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].period).isEqualTo("2024-01-16")
        assertThat(result[1].period).isEqualTo("2024-01-18")
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
