package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType

@ExperimentalCoroutinesApi
class LatestPostDataSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var restClient: LatestPostInsightsRestClient

    private val site = SiteModel()
    private lateinit var dataSource: LatestPostDataSource

    @Before
    fun setUp() {
        dataSource = LatestPostDataSource(restClient)
    }

    @Test
    fun `when a post comes back, then its id and featured image are returned`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(response(post(featuredImage = IMAGE_URL))))

        val result = dataSource.fetchLatestPublishedPost(site)

        assertThat(result).isEqualTo(
            LatestPostLookupResult.Success(postId = POST_ID, featuredImageUrl = IMAGE_URL)
        )
    }

    @Test
    fun `when the post has no featured image, then the url is null`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(response(post(featuredImage = null))))

        val result = dataSource.fetchLatestPublishedPost(site)

        assertThat(result).isEqualTo(
            LatestPostLookupResult.Success(postId = POST_ID, featuredImageUrl = null)
        )
    }

    @Test
    fun `when the featured image is blank, then it counts as no image`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(response(post(featuredImage = ""))))

        val result = dataSource.fetchLatestPublishedPost(site)

        assertThat(result).isEqualTo(
            LatestPostLookupResult.Success(postId = POST_ID, featuredImageUrl = null)
        )
    }

    @Test
    fun `when the site has no published posts, then the result is NoPosts`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(PostsResponse(postsFound = 0, posts = emptyList())))

        val result = dataSource.fetchLatestPublishedPost(site)

        assertThat(result).isEqualTo(LatestPostLookupResult.NoPosts)
    }

    @Test
    fun `when the fetch fails, then the error message is carried through`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(StatsError(StatsErrorType.API_ERROR, ERROR_MESSAGE)))

        val result = dataSource.fetchLatestPublishedPost(site)

        assertThat(result).isEqualTo(LatestPostLookupResult.Error(ERROR_MESSAGE))
    }

    @Test
    fun `when the lookup runs, then it bypasses the response cache`() = test {
        whenever(restClient.fetchLatestPostForInsights(any(), any()))
            .thenReturn(FetchStatsPayload(response(post(featuredImage = null))))

        dataSource.fetchLatestPublishedPost(site)

        verify(restClient).fetchLatestPostForInsights(eq(site), eq(true))
    }

    private fun response(post: PostsResponse.PostResponse) = PostsResponse(postsFound = 1, posts = listOf(post))

    private fun post(featuredImage: String?) = PostsResponse.PostResponse(
        id = POST_ID,
        title = "A title",
        date = null,
        url = "https://example.com/a-title",
        likeCount = 0,
        discussion = null,
        featuredImage = featuredImage
    )

    companion object {
        private const val POST_ID = 42L
        private const val IMAGE_URL = "https://example.com/image.jpg"
        private const val ERROR_MESSAGE = "Something went wrong"
    }
}
