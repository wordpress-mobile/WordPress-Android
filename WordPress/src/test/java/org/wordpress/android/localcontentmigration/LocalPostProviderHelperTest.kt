package org.wordpress.android.localcontentmigration

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.resolver.DbWrapper

class LocalPostProviderHelperTest {
    private val siteProviderHelper: LocalSiteProviderHelper = mock()
    private val dbWrapper: DbWrapper = mock()
    private val mockDb: SQLiteDatabase = mock()
    private val mockCursor: Cursor = mock()
    private val postStore: PostStore = mock()
    private val mockSiteModel: SiteModel = mock()
    private val mockPostModel: PostModel = mock()
    private val mockPostModel2: PostModel = mock()

    private val mockSiteId = 471
    private val mockPostId = 42
    private val mockPostId2 = 7
    private val mockPostList = listOf(mockPostModel, mockPostModel2)
    private val mockPostIdsList = listOf(mockPostId, mockPostId2)

    // Object under test
    private val localPostProviderHelper = LocalPostProviderHelper(
            postStore,
            dbWrapper,
            siteProviderHelper,
    )

    @Before
    fun setUp() {
        whenever(postStore.getPostByLocalPostId(mockPostId)).thenReturn(mockPostModel)
        whenever(mockPostModel.id).thenReturn(mockPostId)
        whenever(mockPostModel2.id).thenReturn(mockPostId2)
        whenever(postStore.getPostsForSite(mockSiteModel)).thenReturn(mockPostList)
        whenever(siteProviderHelper.getData()).thenReturn(SitesData(sites = listOf(mockSiteModel)))
        whenever(mockSiteModel.id).thenReturn(mockSiteId)
        whenever(dbWrapper.giveMeReadableDb()).thenReturn(mockDb)
        whenever(mockDb.query(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
        )).thenReturn(mockCursor)
        whenever(mockCursor.moveToNext()).thenReturn(true, true, false)
        whenever(mockCursor.getInt(0)).thenReturn(mockPostId, mockPostId2)
    }

    @Test
    fun `when a local post id is specified`() {
        val response = localPostProviderHelper.getData(localEntityId = mockPostId) as PostData
        assertThat(response.post).isEqualTo(mockPostModel)
    }

    @Test
    fun `when a local post id is not specified`() {
        val response = localPostProviderHelper.getData() as PostsData
        assertThat(response.localIds).isEqualTo(mockPostIdsList)
    }
}
