package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.LatestPostDataSource
import org.wordpress.android.ui.newstats.datasource.LatestPostLookupResult
import org.wordpress.android.ui.newstats.datasource.PostViewsData

@ExperimentalCoroutinesApi
class StatsLatestPostUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var latestPostDataSource:
        LatestPostDataSource

    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var useCase: StatsLatestPostUseCase

    private val site = SiteModel().apply {
        siteId = TEST_SITE_ID
    }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        useCase = StatsLatestPostUseCase(
            statsRepository,
            latestPostDataSource,
            accountStore
        )
    }

    @Test
    fun `when no access token, then errors without fetching`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)

            val result = useCase(site)

            assertThat(result).isInstanceOf(
                LatestPostResult.Error::class.java
            )
            verify(latestPostDataSource, never())
                .fetchLatestPublishedPost(any())
        }

    @Test
    fun `when site has no posts, then no stats are fetched`() =
        test {
            whenever(
                latestPostDataSource
                    .fetchLatestPublishedPost(site)
            ).thenReturn(LatestPostLookupResult.NoPosts)

            val result = useCase(site)

            assertThat(result).isEqualTo(
                LatestPostResult.NoPosts
            )
            verify(statsRepository, never())
                .fetchPostViews(any(), any())
        }

    @Test
    fun `when the lookup fails, then no stats are fetched`() =
        test {
            whenever(
                latestPostDataSource
                    .fetchLatestPublishedPost(site)
            ).thenReturn(
                LatestPostLookupResult.Error("nope")
            )

            val result = useCase(site)

            assertThat(result).isInstanceOf(
                LatestPostResult.Error::class.java
            )
            verify(statsRepository, never())
                .fetchPostViews(any(), any())
        }

    @Test
    fun `when the stats fetch fails, then the result is an error`() =
        test {
            givenLookupSucceeds()
            givenStatsReturn(PostViewsResult.Error("boom"))

            val result = useCase(site)

            assertThat(result).isInstanceOf(
                LatestPostResult.Error::class.java
            )
        }

    @Test
    fun `when both calls succeed, then stats and image are returned`() =
        test {
            givenLookupSucceeds()
            val data = createPostViewsData()
            givenStatsReturn(PostViewsResult.Success(data))

            val result = useCase(site)

            assertThat(result).isEqualTo(
                LatestPostResult.Success(
                    data = data,
                    featuredImageUrl = TEST_IMAGE_URL
                )
            )
        }

    @Test
    fun `when the post has no featured image, then the url is null`() =
        test {
            givenLookupSucceeds(imageUrl = null)
            givenStatsReturn(
                PostViewsResult.Success(
                    createPostViewsData()
                )
            )

            val result = useCase(site)
                as LatestPostResult.Success

            assertThat(result.featuredImageUrl).isNull()
        }

    private suspend fun givenLookupSucceeds(
        imageUrl: String? = TEST_IMAGE_URL
    ) {
        whenever(
            latestPostDataSource
                .fetchLatestPublishedPost(site)
        ).thenReturn(
            LatestPostLookupResult.Success(
                postId = TEST_POST_ID,
                featuredImageUrl = imageUrl
            )
        )
    }

    private suspend fun givenStatsReturn(
        result: PostViewsResult
    ) {
        whenever(
            statsRepository.fetchPostViews(
                TEST_SITE_ID,
                TEST_POST_ID
            )
        ).thenReturn(result)
    }

    private fun createPostViewsData() = PostViewsData(
        postId = TEST_POST_ID,
        totalViews = 10L,
        dailyViews = emptyList(),
        weeks = emptyList(),
        years = emptyList(),
        averages = emptyList(),
        post = null
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_POST_ID = 42L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val TEST_IMAGE_URL =
            "https://example.com/image.jpg"
    }
}
