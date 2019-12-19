package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRemoteAutoSaveModel
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class PostSqlUtilsTest {
    private val postSqlUtils = PostSqlUtils()

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `post autoSave fields get updated`() {
        val remotePostId = 200L
        val revisionId = 999L
        val modifiedDate = "test date"
        val previewUrl = "test url"

        val site = createSite()

        var post = PostModel()
        post.setLocalSiteId(site.id)
        post.setRemotePostId(remotePostId)

        post = postSqlUtils.insertPostForResult(post)

        assertNull(post.autoSaveModified)
        assertNull(post.autoSavePreviewUrl)
        assertEquals(0, post.autoSaveRevisionId)

        postSqlUtils.updatePostsAutoSave(
                site,
                PostRemoteAutoSaveModel(revisionId, remotePostId, modifiedDate, previewUrl)
        )

        val postsForSite = postSqlUtils.getPostsForSite(site, false)

        assertEquals(1, postsForSite.size)
        assertEquals(revisionId, postsForSite.first().autoSaveRevisionId)
        assertEquals(remotePostId, postsForSite.first().remotePostId)
        assertEquals(modifiedDate, postsForSite.first().autoSaveModified)
        assertEquals(modifiedDate, postsForSite.first().remoteAutoSaveModified)
        assertEquals(previewUrl, postsForSite.first().autoSavePreviewUrl)
    }

    @Test
    fun `insertOrUpdatePost deletes posts with duplicate REMOTE_POST_ID`() {
        // Given
        val site = createSite()

        val localPost = createPost(localSiteId = site.id, localId = 900, remoteId = 8571)
        postSqlUtils.insertPostForResult(localPost)

        val postFromFetch = createPost(localSiteId = site.id, localId = 100_00, remoteId = localPost.remotePostId)
        postSqlUtils.insertPostForResult(postFromFetch)

        // When
        val updatedRowsCount = postSqlUtils.insertOrUpdatePost(localPost, true)

        // Then
        // 2 row changes. First is the deleted row, second is the overwrite
        assertThat(updatedRowsCount).isEqualTo(2)

        // The postFromFetch should not exist anymore
        assertThat(postSqlUtils.getPostsByLocalOrRemotePostIds(listOf(LocalId(postFromFetch.id)), site.id)).isEmpty()

        // The localPost should still exist
        assertThat(postSqlUtils.getPostsByLocalOrRemotePostIds(listOf(LocalId(localPost.id)), site.id)).hasSize(1)

        // There is only one post with the remote id
        val postsWithSameRemotePostId =
                postSqlUtils.getPostsByLocalOrRemotePostIds(listOf(RemoteId(localPost.remotePostId)), site.id)
        assertThat(postsWithSameRemotePostId).hasSize(1)
    }

    private fun createPost(localSiteId: Int, localId: Int, remoteId: Long) = PostModel().apply {
        setId(localId)
        setRemotePostId(remoteId)

        setLocalSiteId(localSiteId)
    }

    private fun createSite() = SiteModel().apply {
        id = 100
    }
}
