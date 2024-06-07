package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import kotlin.test.Test

@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class PostListFeaturedImageTrackerTest : BaseUnitTest() {
    private val dispatcher: Dispatcher = mock()
    private val mediaStore: MediaStore = mock()

    private lateinit var tracker: PostListFeaturedImageTracker

    private val site = SiteModel().apply { id = 123 }

    @Before
    fun setup() {
        tracker = PostListFeaturedImageTracker(dispatcher, mediaStore)
    }

    @Test
    fun `given id exists in map, when getFeaturedImageUrl invoked, then return url`() {
        val imageId = 123L
        val imageUrl = "https://example.com/image.jpg"
        tracker.featuredImageMap[imageId] = imageUrl

        val result = tracker.getFeaturedImageUrl(site, imageId)

        assertEquals(imageUrl, result)
    }

    @Test
    fun `given id is 0, when getFeaturedImageUrl invoked, then return null`() {
        val result = tracker.getFeaturedImageUrl(site, 0L)

        assertNull(result)
    }

    @Test
    fun `given id not in map and exists in store, when invoked, then return url from media store`() {
        val imageId = 456L
        val imageUrl = "https://example.com/image.jpg"
        val mediaModel = MediaModel(site.id, imageId).apply {
            url = imageUrl
        }

        whenever(mediaStore.getSiteMediaWithId(site, imageId)).thenReturn(mediaModel)

        val result = tracker.getFeaturedImageUrl(site, imageId)

        assertEquals(imageUrl, result)
        assertEquals(imageUrl, tracker.featuredImageMap[imageId])
    }

    @Test
    fun `given id not in map or store, when invoked, then return null and dispatch fetch request`() {
        val imageId = 123L

        whenever(mediaStore.getSiteMediaWithId(site, imageId)).thenReturn(null)

        val result = tracker.getFeaturedImageUrl(site, imageId)

        assertNull(result)
        verify(dispatcher).dispatch(any())
        assert(tracker.ongoingRequests.contains(imageId))
    }

    @Test
    fun `given request ongoing for id, when invoked, should return null`() {
        val imageId = 123L

        tracker.ongoingRequests.add(imageId)

        val result = tracker.getFeaturedImageUrl(site, imageId)

        assertNull(result)
        verify(mediaStore, never()).getSiteMediaWithId(site, imageId)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given id in map and ongoingRequests, when invalidate, then remove id from map and ongoingRequests`() {
        val imageId1 = 123L
        val imageId2 = 456L

        tracker.featuredImageMap[imageId1] = "https://example.com/image1.jpg"
        tracker.featuredImageMap[imageId2] = "https://example.com/image2.jpg"
        tracker.ongoingRequests.add(imageId1)
        tracker.ongoingRequests.add(imageId2)

        tracker.invalidateFeaturedMedia(listOf(imageId1, imageId2))

        assertNull(tracker.featuredImageMap[imageId1])
        assertNull(tracker.featuredImageMap[imageId2])
        assert(!tracker.ongoingRequests.contains(imageId1))
        assert(!tracker.ongoingRequests.contains(imageId2))
    }
}
