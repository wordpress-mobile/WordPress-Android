package org.wordpress.android.fluxc.model.stats

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient.CountryViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse.ViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse.ViewsResponse.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient.VideoPlaysResponse
import org.wordpress.android.fluxc.store.stats.time.POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.stats.time.POST_ID
import org.wordpress.android.fluxc.store.stats.time.POST_TITLE
import org.wordpress.android.fluxc.store.stats.time.POST_URL
import org.wordpress.android.fluxc.store.stats.time.POST_VIEWS

@RunWith(MockitoJUnitRunner::class)
class TimeStatsMapperTest {
    @Mock lateinit var gson: Gson
    private lateinit var timeStatsMapper: TimeStatsMapper

    @Before
    fun setUp() {
        timeStatsMapper = TimeStatsMapper(gson)
    }

    @Test
    fun `parses portfolio page type`() {
        val portfolioResponse = PostViewsResponse(
                POST_ID,
                POST_TITLE,
                "jetpack-portfolio",
                POST_URL,
                POST_VIEWS
        )
        val response = POST_AND_PAGE_VIEWS_RESPONSE.copy(
                days = mapOf(
                        "2019-01-01" to ViewsResponse(
                                listOf(portfolioResponse), 10
                        )
                )
        )

        val mappedResult = timeStatsMapper.map(response, LimitMode.All)

        assertThat(mappedResult.views).hasSize(1)
        assertThat(mappedResult.views.first().type).isEqualTo(ViewsType.OTHER)
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

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.groups).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherClicks).isZero()
        assertThat(result.totalClicks).isZero()
    }

    @Test
    fun `parses empty country views`() {
        val response = CountryViewsResponse(emptyMap(), emptyMap())

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.countries).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherViews).isZero()
        assertThat(result.totalViews).isZero()
    }

    @Test
    fun `parses empty authors`() {
        val response = AuthorsResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.authors).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherViews).isZero()
    }

    @Test
    fun `parses empty search terms`() {
        val response = SearchTermsResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.searchTerms).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherSearchTerms).isZero()
        assertThat(result.totalSearchTerms).isZero()
    }

    @Test
    fun `parses empty videos`() {
        val response = VideoPlaysResponse(null, emptyMap())

        val result = timeStatsMapper.map(response, LimitMode.Top(5))

        assertThat(result.plays).isEmpty()
        assertThat(result.hasMore).isFalse()
        assertThat(result.otherPlays).isZero()
        assertThat(result.totalPlays).isZero()
    }
}
