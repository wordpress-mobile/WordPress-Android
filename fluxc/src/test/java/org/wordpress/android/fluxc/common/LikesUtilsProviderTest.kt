package org.wordpress.android.fluxc.common

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.LikeType
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikesUtilsProvider
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.util.Date

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class LikesUtilsProviderTest {
    private val likesUtilsProvider = LikesUtilsProvider()
    private val postSqlUtils = PostSqlUtils()

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `getPageOffsetParams returns empty params map when no data`() {
        val siteId = 100L
        val itemId = 1000L
        val params = likesUtilsProvider.getPageOffsetParams(POST_LIKE, siteId, itemId)

        assertThat(params).isEmpty()
    }

    @Test
    fun `getPageOffsetParams returns oldest date liked and user id for pagination API`() {
        val siteId = 100L
        val itemId = 1000L

        val likedTimestampList = listOf(
                Pair(2000L, "2020-04-04 21:20:00"),
                Pair(2001L, "2020-04-04 21:21:00"),
                Pair(2002L, "2020-04-04 21:22:00")
        )

        likedTimestampList.forEach {
            postSqlUtils.insertOrUpdatePostLikes(
                    siteId,
                    itemId,
                    createLike(POST_LIKE, siteId, itemId, it.first, it.second)
            )
        }
        val params = likesUtilsProvider.getPageOffsetParams(POST_LIKE, siteId, itemId)

        assertThat(params).hasSize(2)
        assertThat(params!!["before"]).isEqualTo(likedTimestampList.first().second)
        assertThat(params!!["exclude[]"]).isEqualTo(likedTimestampList.first().first.toString())
    }

    @Test
    fun `getPageOffsetParams returns oldest date liked and list of user ids for pagination API when dates overlap`() {
        val siteId = 100L
        val itemId = 1000L

        val likedTimestampList = listOf(
                Pair(2000L, "2020-04-04 21:20:00"),
                Pair(2001L, "2020-04-04 21:20:00"),
                Pair(2002L, "2020-04-04 21:20:00"),
                Pair(2003L, "2020-04-04 21:22:00"),
                Pair(2004L, "2020-04-04 21:23:00")
        )

        likedTimestampList.forEach {
            postSqlUtils.insertOrUpdatePostLikes(
                    siteId,
                    itemId,
                    createLike(POST_LIKE, siteId, itemId, it.first, it.second)
            )
        }
        val params = likesUtilsProvider.getPageOffsetParams(POST_LIKE, siteId, itemId)

        assertThat(params).hasSize(2)
        assertThat(params!!["before"]).isEqualTo(likedTimestampList.first().second)
        val excludedUserIds = likedTimestampList.map {
            it.first
        }.toList().take(3)

        assertThat(params!!["exclude[]"]).isEqualTo(excludedUserIds.joinToString(separator = "&exclude[]="))
    }

    private fun createLike(likeType: LikeType, siteId: Long, postId: Long, userId: Long, dateLikedString: String) =
            LikeModel().apply {
                type = likeType.typeName
                remoteSiteId = siteId
                remoteItemId = postId
                likerId = userId
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
                dateLiked = dateLikedString
                timestampFetched = Date().time
            }
}
