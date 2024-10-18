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
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.Companion.TIMESTAMP_THRESHOLD
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRemoteAutoSaveModel
import java.util.Date
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

    @Test
    fun `insertOrUpdatePostLikes insert a new like`() {
        val siteId = 100L
        val postId = 1000L

        val localLike = createLike(siteId, postId)

        postSqlUtils.insertOrUpdatePostLikes(siteId, postId, localLike)

        val postLikes = postSqlUtils.getPostLikesByPostId(siteId, postId)
        assertThat(postLikes).hasSize(1)
        assertThat(postLikes[0].isEqual(localLike)).isTrue
    }

    @Test
    fun `insertOrUpdatePostLikes update a changed like`() {
        val siteId = 100L
        val postId = 1000L

        val localLike = createLike(siteId, postId)
        val localLikeChanged = createLike(siteId, postId).apply {
            likerSiteUrl = "https://likerSiteUrl.wordpress.com"
        }

        postSqlUtils.insertOrUpdatePostLikes(siteId, postId, localLike)

        var postLikes = postSqlUtils.getPostLikesByPostId(siteId, postId)
        assertThat(postLikes).hasSize(1)
        assertThat(postLikes[0].isEqual(localLike)).isTrue

        postSqlUtils.insertOrUpdatePostLikes(siteId, postId, localLikeChanged)

        postLikes = postSqlUtils.getPostLikesByPostId(siteId, postId)
        assertThat(postLikes).hasSize(1)
        assertThat(postLikes[0].isEqual(localLike)).isFalse
        assertThat(postLikes[0].isEqual(localLikeChanged)).isTrue
    }

    @Test
    fun `deletePostLikesAndPurgeExpired deletes currently fetched data`() {
        val siteId = 100L
        val postId = 1000L

        val localLike = createLike(siteId, postId)

        postSqlUtils.insertOrUpdatePostLikes(siteId, postId, localLike)
        var postLikes = postSqlUtils.getPostLikesByPostId(siteId, postId)
        assertThat(postLikes).hasSize(1)

        postSqlUtils.deletePostLikesAndPurgeExpired(siteId, postId)
        postLikes = postSqlUtils.getPostLikesByPostId(siteId, postId)
        assertThat(postLikes).isEmpty()
    }

    @Test
    fun `deletePostLikesAndPurgeExpired delete data older than threshold`() {
        val siteId = 100L
        val postId = 1000L

        val sitePostList = listOf(
                Triple(101L, 1000L, Date().time),
                Triple(101L, 1001L, Date().time - TIMESTAMP_THRESHOLD / 2),
                Triple(101L, 1002L, Date().time - TIMESTAMP_THRESHOLD * 2),
                Triple(101L, 1003L, Date().time - TIMESTAMP_THRESHOLD * 2)
        )

        val expectedSizeList = listOf(1, 1, 0, 0)

        val likeList = mutableListOf<LikeModel>()

        for (sitePostTriple: Triple<Long, Long, Long> in sitePostList) {
            likeList.add(createLike(sitePostTriple.first, sitePostTriple.second, sitePostTriple.third))
        }

        for (like: LikeModel in likeList) {
            postSqlUtils.insertOrUpdatePostLikes(siteId, postId, like)
        }

        postSqlUtils.deletePostLikesAndPurgeExpired(siteId, postId)

        sitePostList.forEachIndexed { index, element ->
            assertThat(
                    postSqlUtils.getPostLikesByPostId(
                            element.first,
                            element.second
                    )
            ).hasSize(expectedSizeList[index])
        }
    }

    private fun createPost(localSiteId: Int, localId: Int, remoteId: Long) = PostModel().apply {
        setId(localId)
        setRemotePostId(remoteId)

        setLocalSiteId(localSiteId)
    }

    private fun createSite() = SiteModel().apply {
        id = 100
    }

    private fun createLike(siteId: Long, postId: Long, timeStamp: Long = Date().time) = LikeModel().apply {
        type = POST_LIKE.typeName
        remoteSiteId = siteId
        remoteItemId = postId
        likerId = 2000L
        likerName = "likerName"
        likerLogin = "likerLogin"
        likerAvatarUrl = "likerAvatarUrl"
        likerBio = "likerBio"
        likerSiteId = 3000L
        likerSiteUrl = "likerSiteUrl"
        preferredBlogId = 4000L
        preferredBlogName = "preferredBlogName"
        preferredBlogUrl = "preferredBlogUrl"
        preferredBlogBlavatarUrl = "preferredBlogBlavatarUrl"
        dateLiked = "2020-04-04 11:22:34"
        timestampFetched = timeStamp
    }
}
