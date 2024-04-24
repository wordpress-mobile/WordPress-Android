package org.wordpress.android.ui.engagement

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import org.wordpress.android.R
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameCharSequence
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameString
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import com.google.android.material.R as MaterialR

class LikedItemViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager
) : EngagedPeopleViewHolder(parent, R.layout.note_block_header) {
    private val snippet = itemView.findViewById<TextView>(R.id.header_snippet)
    private val avatar = itemView.findViewById<ImageView>(R.id.header_avatar)
    private val rootView = itemView.findViewById<CardView>(R.id.header_root_view)

    fun bind(likedItem: LikedItem, type: ListScenarioType) {
        val authorName = when (val author = likedItem.author) {
            is AuthorNameString -> author.nameString
            is AuthorNameCharSequence -> author.nameCharSequence
        }

        this.snippet.text = likedItem.postOrCommentText

        val avatarUrl = WPAvatarUtils.rewriteAvatarUrl(
            likedItem.authorAvatarUrl,
            rootView.context.resources.getDimensionPixelSize(R.dimen.avatar_sz_extra_small)
        )
        avatar.isGone = type == ListScenarioType.LOAD_POST_LIKES
        imageManager.loadIntoCircle(this.avatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl)

        if (!TextUtils.isEmpty(likedItem.authorPreferredSiteUrl) || likedItem.authorPreferredSiteId > 0) {
            with(this.avatar) {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = this.context.getString(R.string.profile_picture, authorName)
                setOnClickListener {
                    likedItem.onGravatarClick.invoke(
                        likedItem.authorPreferredSiteId,
                        likedItem.authorPreferredSiteUrl,
                        likedItem.blogPreviewSource
                    )
                }
                setBackgroundResource(
                    this.context.getDrawableResIdFromAttribute(MaterialR.attr.selectableItemBackgroundBorderless)
                )
            }
        } else {
            with(this.avatar) {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                contentDescription = null
                setOnClickListener(null)
                setBackgroundResource(0)
            }
        }

        rootView.setOnClickListener {
            likedItem.onHeaderClicked.invoke(
                likedItem.likedItemSiteId,
                likedItem.likedItemSiteUrl,
                likedItem.likedItemId,
                likedItem.likedItemPostId
            )
        }
    }
}
