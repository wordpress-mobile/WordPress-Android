package org.wordpress.android.ui.engagement.utils

import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.LikeModel.LikeType
import org.wordpress.android.fluxc.model.LikeModel.LikeType.COMMENT_LIKE
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.ui.engagement.EngageItem
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_NOTIFICATION_LIST
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_READER_LIST
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType.GENERIC
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure.EmptyStateData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.PagingInfo
import org.wordpress.android.ui.engagement.ListScenario
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_1
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_2
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_3
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_4
import org.wordpress.android.ui.engagement.utils.GetLikesTestConfig.TEST_CONFIG_5
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

fun getDefaultLikers(numLikers: Int, likedItemType: LikeType, siteId: Long, itemId: Long): List<LikeModel> {
    val likersList = mutableListOf<LikeModel>()

    for (i in 0 until numLikers) {
        likersList.add(LikeModel().apply {
            type = likedItemType.typeName
            remoteSiteId = siteId
            remoteItemId = itemId
            likerName = "liker name $i"
            likerLogin = "liker${i}login"
            likerSiteId = 10L + i * 100L
            likerSiteUrl = "https://liker$i-site.org"
            likerAvatarUrl = "https://liker$i-site.org/avatar.jpg"
            likerBio = "This is the bio for liker$i"
            likerId = 100L + i * 1000L
            preferredBlogId = 10L + i * 100L
            preferredBlogName = "Liker $i preferred blog name"
            preferredBlogUrl = "https://liker$i-preferred-blog.wordpress.com"
            preferredBlogBlavatarUrl = "https://liker$i-preferred-blog.wordpress.com/avatar.jpg"
        })
    }

    return likersList
}

enum class GetLikesTestConfig {
    TEST_CONFIG_1,
    TEST_CONFIG_2,
    TEST_CONFIG_3,
    TEST_CONFIG_4,
    TEST_CONFIG_5
}

fun getGetLikesState(testConfig: GetLikesTestConfig): GetLikesState {
    val pageInfo = PagingInfo(
            20,
            1
    )
    return when (testConfig) {
        // Like Data available for post and fetched from API; no more data available
        TEST_CONFIG_1 -> {
            LikesData(
                    getDefaultLikers(10, POST_LIKE, 10, 100),
                    10,
                    false,
                    pageInfo
            )
        }
        // Like Data available for post and fetched from API, more data available
        TEST_CONFIG_2 -> {
            LikesData(
                    getDefaultLikers(10, COMMENT_LIKE, 10, 100),
                    10,
                    true,
                    pageInfo
            )
        }
        // Like Data available for comment and fetched from API, no more data available
        TEST_CONFIG_3 -> {
            LikesData(
                    getDefaultLikers(10, COMMENT_LIKE, 10, 100),
                    10,
                    false,
                    pageInfo
            )
        }
        // Like Data available for comment and fetched from API, more data available
        TEST_CONFIG_4 -> {
            LikesData(
                    getDefaultLikers(10, COMMENT_LIKE, 10, 100),
                    10,
                    true,
                    pageInfo
            )
        }
        // Failure getting like data from API, and no cached data available
        TEST_CONFIG_5 -> {
            val cachedData = listOf<LikeModel>()
            Failure(
                    GENERIC,
                    UiStringText("There was an error"),
                    listOf(),
                    EmptyStateData(
                            cachedData.isEmpty(),
                            UiStringRes(10)
                    ),
                    10,
                    false,
                    pageInfo
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun List<LikeModel>.isEqualTo(engageItemList: List<EngageItem>): Boolean {
    val sameSize = this.size == engageItemList.size
    val likersList = engageItemList as? List<Liker>

    val allEquals = this.all { likeModel ->
        val liker = likersList!!.firstOrNull {
            it.userId == likeModel.likerId
        }

        liker != null && likeModel.isEqualTo(liker)
    }

    return sameSize && allEquals
}

fun LikeModel.isEqualTo(liker: Liker): Boolean {
    return liker.name == this.likerName &&
            liker.login == this.likerLogin &&
            liker.userSiteId == this.likerSiteId &&
            liker.userSiteUrl == this.likerSiteUrl &&
            liker.userAvatarUrl == this.likerAvatarUrl &&
            liker.userBio == this.likerBio &&
            liker.userId == this.likerId &&
            liker.preferredBlogId == this.preferredBlogId &&
            liker.preferredBlogName == this.preferredBlogName &&
            liker.preferredBlogUrl == this.preferredBlogUrl &&
            liker.preferredBlogBlavatar == this.preferredBlogBlavatarUrl
}

fun ListScenario.generatesEquivalentLikedItem(likedItem: LikedItem): Boolean {
    val headerData = this.headerData
    return likedItem.author == headerData.authorName &&
            likedItem.postOrCommentText == headerData.snippetText &&
            likedItem.authorAvatarUrl == headerData.authorAvatarUrl &&
            likedItem.likedItemId == this.postOrCommentId &&
            likedItem.likedItemSiteId == this.siteId &&
            likedItem.likedItemSiteUrl == this.commentSiteUrl &&
            likedItem.likedItemPostId == this.commentPostId &&
            likedItem.authorUserId == headerData.authorUserId &&
            likedItem.authorPreferredSiteId == headerData.authorPreferredSiteId &&
            likedItem.authorPreferredSiteUrl == headerData.authorPreferredSiteUrl &&
            likedItem.blogPreviewSource == when (this.source) {
                LIKE_NOTIFICATION_LIST -> ReaderTracker.SOURCE_NOTIFICATION
                LIKE_READER_LIST -> ReaderTracker.SOURCE_READER_LIKE_LIST
            }
}
