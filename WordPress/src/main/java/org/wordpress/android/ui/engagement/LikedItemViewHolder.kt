package org.wordpress.android.ui.engagement

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameCharSequence
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameString
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class LikedItemViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager
) : EngagedPeopleViewHolder(parent, R.layout.note_block_header) {
    private val name = itemView.findViewById<TextView>(R.id.header_user)
    private val snippet = itemView.findViewById<TextView>(R.id.header_snippet)
    private val avatar = itemView.findViewById<ImageView>(R.id.header_avatar)
    private val rootView = itemView.findViewById<View>(R.id.header_root_view)

    fun bind(likedItem: LikedItem) {
        val authorName = when (val author = likedItem.author) {
            is AuthorNameString -> author.nameString
            is AuthorNameCharSequence -> author.nameCharSequence
        }

        this.name.text = authorName
        this.snippet.text = likedItem.postOrCommentText

        val avatarUrl = GravatarUtils.fixGravatarUrl(
            likedItem.authorAvatarUrl,
            rootView.context.resources.getDimensionPixelSize(R.dimen.avatar_sz_small)
        )

        imageManager.loadIntoCircle(this.avatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl)

        if (!TextUtils.isEmpty(likedItem.authorPreferredSiteUrl) || likedItem.authorPreferredSiteId > 0) {
            with(this.avatar) {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = this.context.getString(string.profile_picture, authorName)
                setOnClickListener {
                    likedItem.onGravatarClick.invoke(
                        likedItem.authorPreferredSiteId,
                        likedItem.authorPreferredSiteUrl,
                        likedItem.blogPreviewSource
                    )
                }
                setBackgroundResource(
                    this.context.getDrawableResIdFromAttribute(R.attr.selectableItemBackgroundBorderless)
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
