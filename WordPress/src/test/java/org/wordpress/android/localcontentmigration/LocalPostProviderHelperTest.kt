package org.wordpress.android.localcontentmigration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData

class LocalPostProviderHelperTest {
    private val siteProviderHelper: LocalSiteProviderHelper = mock()
    private val postStore: PostStore = mock()
    private val mockSiteModel: SiteModel = mock()
    private val mockPostModel: PostModel = mock()
    private val mockPostModel2: PostModel = mock()

    private val mockPostId = 42
    private val mockPostId2 = 7
    private val mockPostList = listOf(mockPostModel, mockPostModel2)
    private val mockPostIdsList = listOf(mockPostId, mockPostId2)

    // Object under test
    private val localPostProviderHelper = LocalPostProviderHelper(postStore, siteProviderHelper)

    @Before
    fun setUp() {
        whenever(postStore.getPostByLocalPostId(mockPostId)).thenReturn(mockPostModel)
        whenever(mockPostModel.id).thenReturn(mockPostId)
        whenever(mockPostModel2.id).thenReturn(mockPostId2)
        whenever(postStore.getPostsForSite(mockSiteModel)).thenReturn(mockPostList)
        whenever(siteProviderHelper.getData()).thenReturn(SitesData(sites = listOf(mockSiteModel)))
    }

    @Test
    fun `when a local post id is specified`() {
        val response = localPostProviderHelper.getData(localEntityId = mockPostId) as PostData
        assertThat(response.post).isEqualTo(mockPostModel)
    }

    @Test
    fun `when a local post id is not specified but a local site id is specified`() {
        val response = localPostProviderHelper.getData() as PostsData
        assertThat(response.localIds).isEqualTo(mockPostIdsList)
    }
}
