package org.wordpress.android.localcontentmigration

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData

class LocalPostProviderHelperTest {
    private val siteStore: SiteStore = mock()
    private val postStore: PostStore = mock()
    private val mockSiteModel: SiteModel = mock()
    private val mockPostModel: PostModel = mock()
    private val mockPostModel2: PostModel = mock()

    private val mockPostId = 42
    private val mockPostId2 = 7
    private val mockSiteId = 42
    private val mockPostList = listOf(mockPostModel, mockPostModel2)
    private val mockPostIdsList = listOf(mockPostId, mockPostId2)

    // Object under test
    private val localPostProviderHelper = LocalPostProviderHelper(siteStore, postStore)

    @Before
    fun setUp() {
        whenever(postStore.getPostByLocalPostId(mockPostId)).thenReturn(mockPostModel)
        whenever(mockPostModel.id).thenReturn(mockPostId)
        whenever(mockPostModel2.id).thenReturn(mockPostId2)
        whenever(siteStore.getSiteByLocalId(mockSiteId)).thenReturn(mockSiteModel)
        whenever(postStore.getPostsForSite(mockSiteModel)).thenReturn(mockPostList)
    }

    @Test
    fun `when a local post id is specified`() {
        val response = localPostProviderHelper.getData(localEntityId = mockPostId) as PostData
        assertThat(response.post).isEqualTo(mockPostModel)
    }

    @Test
    fun `when a local post id is not specified but a local site id is specified`() {
        val response = localPostProviderHelper.getData(localSiteId = mockSiteId) as PostsData
        assertThat(response.localIds).isEqualTo(mockPostIdsList)
    }
}
