package org.wordpress.android.ui.engagement

import dagger.Reusable
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import javax.inject.Inject

@Reusable
class EngagementUtils @Inject constructor() {
    fun likesToEngagedPeople(
        likes: List<LikeModel>,
        onClick: ((userProfile: UserProfile) -> Unit)? = null
    ): List<EngageItem> {
        return likes.map { likeData ->
            Liker(
                    name = likeData.likerName!!,
                    login = likeData.likerLogin!!,
                    userSiteId = likeData.likerSiteId,
                    userSiteUrl = likeData.likerSiteUrl!!,
                    userAvatarUrl = likeData.likerAvatarUrl!!,
                    userBio = likeData.likerBio!!,
                    remoteId = likeData.remoteLikeId,
                    preferredBlogId = likeData.preferredBlogId,
                    preferredBlogName = likeData.preferredBlogName!!,
                    preferredBlogUrl = likeData.preferredBlogUrl!!,
                    preferredBlogBlavatar = likeData.preferredBlogBlavatarUrl!!,
                    onClick = onClick
            )
        }
    }
}
