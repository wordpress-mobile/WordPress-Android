package org.wordpress.android.fluxc.model.stats

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient.CountryViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient.VideoPlaysResponse

@RunWith(MockitoJUnitRunner::class)
class TimeStatsMapperTest {
    @Mock lateinit var gson: Gson
    private lateinit var timeStatsMapper: TimeStatsMapper

    @Before
    fun setUp() {
        timeStatsMapper = TimeStatsMapper(gson)
    }

    @Test
    fun `parses empty referrers`() {
        val response = ReferrersResponse("DAYS", emptyMap())

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.groups).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherViews).isZero()
        assertThat(result.totalViews).isZero()
    }

    @Test
    fun `parses empty posts and views`() {
        val response = PostAndPageViewsResponse(null, emptyMap(), "DAYS")

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.views).isEmpty()
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `parses empty clicks`() {
        val response = ClicksResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, 5)

        assertThat(result.groups).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherClicks).isZero()
        assertThat(result.totalClicks).isZero()
    }

    @Test
    fun `parses empty country views`() {
        val response = CountryViewsResponse(emptyMap(), emptyMap())

        val result = timeStatsMapper.map(response, 5)

        assertThat(result.countries).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherViews).isZero()
        assertThat(result.totalViews).isZero()
    }

    @Test
    fun `parses empty authors`() {
        val response = AuthorsResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, 5)

        assertThat(result.authors).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherViews).isZero()
    }

    @Test
    fun `parses empty search terms`() {
        val response = SearchTermsResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, 5)

        assertThat(result.searchTerms).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherSearchTerms).isZero()
        assertThat(result.totalSearchTerms).isZero()
    }

    @Test
    fun `parses empty videos`() {
        val response = VideoPlaysResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, 5)

        assertThat(result.plays).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherPlays).isZero()
        assertThat(result.totalPlays).isZero()
    }
}
