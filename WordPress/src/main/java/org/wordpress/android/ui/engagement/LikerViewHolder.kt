package org.wordpress.android.ui.engagement

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.ResourceProvider

class LikerViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val resourceProvider: ResourceProvider
) : EngagedPeopleViewHolder(parent, R.layout.liker_user) {
    private val likerName = itemView.findViewById<TextView>(R.id.user_name)
    private val likerLogin = itemView.findViewById<TextView>(R.id.user_login)
    private val likerAvatar = itemView.findViewById<ImageView>(R.id.user_avatar)
    private val likerRootView = itemView.findViewById<View>(R.id.liker_root_view)

    fun bind(liker: Liker) {
        this.likerName.text = liker.name
        this.likerLogin.text = if (liker.login.isNotBlank()) {
            resourceProvider.getString(R.string.at_username, liker.login)
        } else {
            liker.login
        }

        val likerAvatarUrl = GravatarUtils.fixGravatarUrl(
            liker.userAvatarUrl,
            likerRootView.context.resources.getDimensionPixelSize(R.dimen.avatar_sz_medium)
        )

        imageManager.loadIntoCircle(this.likerAvatar, ImageType.AVATAR_WITH_BACKGROUND, likerAvatarUrl)

        if (liker.onClick != null) {
            likerRootView.isEnabled = true
            likerRootView.setOnClickListener {
                liker.onClick.invoke(
                    UserProfile(
                        userAvatarUrl = liker.userAvatarUrl,
                        blavatarUrl = liker.preferredBlogBlavatar,
                        userName = liker.name,
                        userLogin = liker.login,
                        userBio = liker.userBio,
                        siteTitle = liker.preferredBlogName,
                        siteUrl = liker.preferredBlogUrl,
                        siteId = liker.preferredBlogId
                    ),
                    liker.source
                )
            }
        } else {
            likerRootView.isEnabled = true
            likerRootView.setOnClickListener(null)
        }
    }
}
