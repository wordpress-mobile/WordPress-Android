package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import org.wordpress.android.ui.rs.data.RsSiteRestClient
import uniffi.wp_api.PostEndpointType

@ExperimentalCoroutinesApi
class StatsPostThumbnailsDataSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var rsSiteRestClient: RsSiteRestClient

    private val site = SiteModel()
    private lateinit var dataSource: StatsPostThumbnailsDataSource

    @Before
    fun setUp() {
        dataSource = StatsPostThumbnailsDataSource(rsSiteRestClient, testDispatcher())
    }

    @Test
    fun `when items are posts and pages, then each endpoint is asked for its own ids`() = test {
        whenever(rsSiteRestClient.fetchFeaturedMediaIds(any(), any(), any())).thenReturn(emptyMap())

        dataSource.fetchThumbnails(
            site,
            listOf(
                StatsThumbnailRequest(POST_ID, "post"),
                StatsThumbnailRequest(PAGE_ID, "page"),
                StatsThumbnailRequest(OTHER_POST_ID, "post")
            )
        )

        verify(rsSiteRestClient).fetchFeaturedMediaIds(
            eq(site), eq(PostEndpointType.Posts), eq(listOf(POST_ID, OTHER_POST_ID))
        )
        verify(rsSiteRestClient).fetchFeaturedMediaIds(
            eq(site), eq(PostEndpointType.Pages), eq(listOf(PAGE_ID))
        )
    }

    @Test
    fun `when an item is the homepage or an attachment, then it never reaches the network`() = test {
        val result = dataSource.fetchThumbnails(
            site,
            listOf(
                StatsThumbnailRequest(POST_ID, "homepage"),
                StatsThumbnailRequest(PAGE_ID, "attachment"),
                StatsThumbnailRequest(OTHER_POST_ID, null)
            )
        )

        assertThat(result).isEmpty()
        verify(rsSiteRestClient, never()).fetchFeaturedMediaIds(any(), any(), any())
        verify(rsSiteRestClient, never()).fetchFeaturedImageUrls(any(), any(), any(), any())
    }

    @Test
    fun `when media resolves, then thumbnails come back keyed by post id`() = test {
        whenever(
            rsSiteRestClient.fetchFeaturedMediaIds(eq(site), eq(PostEndpointType.Posts), any())
        ).thenReturn(mapOf(POST_ID to MEDIA_ID))
        whenever(rsSiteRestClient.fetchFeaturedImageUrls(eq(site), eq(listOf(MEDIA_ID)), any(), any()))
            .thenReturn(mapOf(MEDIA_ID to FeaturedImageUrls(thumbnail = THUMBNAIL_URL, hero = HERO_URL)))

        val result = dataSource.fetchThumbnails(site, listOf(StatsThumbnailRequest(POST_ID, "post")))

        assertThat(result).isEqualTo(mapOf(POST_ID to THUMBNAIL_URL))
    }

    @Test
    fun `when two posts share a featured image, then it is only requested once`() = test {
        whenever(
            rsSiteRestClient.fetchFeaturedMediaIds(eq(site), eq(PostEndpointType.Posts), any())
        ).thenReturn(mapOf(POST_ID to MEDIA_ID, OTHER_POST_ID to MEDIA_ID))
        whenever(rsSiteRestClient.fetchFeaturedImageUrls(eq(site), eq(listOf(MEDIA_ID)), any(), any()))
            .thenReturn(mapOf(MEDIA_ID to FeaturedImageUrls(thumbnail = THUMBNAIL_URL, hero = HERO_URL)))

        val result = dataSource.fetchThumbnails(
            site,
            listOf(StatsThumbnailRequest(POST_ID, "post"), StatsThumbnailRequest(OTHER_POST_ID, "post"))
        )

        assertThat(result).isEqualTo(mapOf(POST_ID to THUMBNAIL_URL, OTHER_POST_ID to THUMBNAIL_URL))
    }

    @Test
    fun `when no post has a featured image, then no media call is made`() = test {
        whenever(rsSiteRestClient.fetchFeaturedMediaIds(any(), any(), any())).thenReturn(emptyMap())

        val result = dataSource.fetchThumbnails(site, listOf(StatsThumbnailRequest(POST_ID, "post")))

        assertThat(result).isEmpty()
        verify(rsSiteRestClient, never()).fetchFeaturedImageUrls(any(), any(), any(), any())
    }

    @Test
    fun `when the media lookup drops an id, then that post simply has no thumbnail`() = test {
        whenever(
            rsSiteRestClient.fetchFeaturedMediaIds(eq(site), eq(PostEndpointType.Posts), any())
        ).thenReturn(mapOf(POST_ID to MEDIA_ID, OTHER_POST_ID to OTHER_MEDIA_ID))
        whenever(rsSiteRestClient.fetchFeaturedImageUrls(any(), any(), any(), any()))
            .thenReturn(mapOf(MEDIA_ID to FeaturedImageUrls(thumbnail = THUMBNAIL_URL, hero = HERO_URL)))

        val result = dataSource.fetchThumbnails(
            site,
            listOf(StatsThumbnailRequest(POST_ID, "post"), StatsThumbnailRequest(OTHER_POST_ID, "post"))
        )

        assertThat(result).isEqualTo(mapOf(POST_ID to THUMBNAIL_URL))
    }

    @Test
    fun `when the item type is capitalised, then it still maps to an endpoint`() = test {
        whenever(rsSiteRestClient.fetchFeaturedMediaIds(any(), any(), any())).thenReturn(emptyMap())

        dataSource.fetchThumbnails(site, listOf(StatsThumbnailRequest(PAGE_ID, "Page")))

        verify(rsSiteRestClient).fetchFeaturedMediaIds(
            eq(site), eq(PostEndpointType.Pages), eq(listOf(PAGE_ID))
        )
    }

    @Test
    fun `when there are no items, then nothing is fetched`() = test {
        val result = dataSource.fetchThumbnails(site, emptyList())

        assertThat(result).isEmpty()
        verify(rsSiteRestClient, never()).fetchFeaturedMediaIds(any(), any(), any())
    }

    companion object {
        private const val POST_ID = 10L
        private const val OTHER_POST_ID = 11L
        private const val PAGE_ID = 20L
        private const val MEDIA_ID = 100L
        private const val OTHER_MEDIA_ID = 101L
        private const val THUMBNAIL_URL = "https://example.com/image.jpg?w=120&h=120"
        private const val HERO_URL = "https://example.com/image.jpg?w=1080&h=0"
    }
}
