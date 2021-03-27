package org.wordpress.android.ui.engagement

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class LikerViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val context: Context
) : EngagedPeopleViewHolder(parent, R.layout.liker_user) {
    private val likerName = itemView.findViewById<TextView>(R.id.user_name)
    private val likerLogin = itemView.findViewById<TextView>(R.id.user_login)
    private val likerAvatar = itemView.findViewById<ImageView>(R.id.user_avatar)
    private val likerRootView = itemView.findViewById<View>(R.id.liker_root_view)

    fun bind(liker: Liker) {
        this.likerName.text = liker.name
        this.likerLogin.text = if (liker.login.isNotBlank()) "@${liker.login}" else liker.login

        val likerAvatarUrl = GravatarUtils.fixGravatarUrl(
                liker.userAvatarUrl,
                context.resources.getDimensionPixelSize(R.dimen.avatar_sz_medium)
        )

        imageManager.loadIntoCircle(this.likerAvatar, ImageType.AVATAR_WITH_BACKGROUND, likerAvatarUrl)

        if (liker.userSiteUrl.isNotEmpty()) {
            likerRootView.isEnabled = true
            likerRootView.setOnClickListener {
                liker.onClick(liker.userSiteId, liker.userSiteUrl)
            }
        } else {
            likerRootView.isEnabled = true
            likerRootView.setOnClickListener(null)
        }
    }
}
