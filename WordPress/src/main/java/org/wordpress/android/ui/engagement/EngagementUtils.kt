package org.wordpress.android.ui.engagement

import dagger.Reusable
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import javax.inject.Inject

@Reusable
class EngagementUtils @Inject constructor() {
    fun likesToEngagedPeople(
        likes: List<LikeModel>,
        onClick: ((userProfile: UserProfile, source: EngagementNavigationSource?) -> Unit)? = null,
        source: EngagementNavigationSource? = null
    ): List<EngageItem> {
        return likes.map { likeData ->
            Liker(
                name = likeData.likerName!!,
                login = likeData.likerLogin!!,
                userSiteId = likeData.likerSiteId,
                userSiteUrl = likeData.likerSiteUrl!!,
                userAvatarUrl = likeData.likerAvatarUrl!!,
                userBio = likeData.likerBio!!,
                userId = likeData.likerId,
                preferredBlogId = likeData.preferredBlogId,
                preferredBlogName = likeData.preferredBlogName!!,
                preferredBlogUrl = likeData.preferredBlogUrl!!,
                preferredBlogBlavatar = likeData.preferredBlogBlavatarUrl!!,
                onClick = onClick,
                source = source
            )
        }
    }

    fun likesToTrainOfFaces(likes: List<LikeModel>) = likes.map {
        AvatarItem(userAvatarUrl = it.likerAvatarUrl!!)
    }
}
