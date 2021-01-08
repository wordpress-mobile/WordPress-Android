package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.reader_post_detail_header_view.view.*
import kotlinx.android.synthetic.main.reader_blog_section_view.view.*
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * topmost view in post detail
 */
class ReaderPostDetailHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
        View.inflate(context, layout.reader_post_detail_header_view, this)
    }

    fun updatePost(uiState: ReaderPostDetailsHeaderUiState) {
        expandable_tags_view.setVisible(uiState.tagItemsVisibility)
        expandable_tags_view.updateUi(uiState.tagItems)

        uiHelpers.setTextOrHide(text_title, uiState.title)

        updateBlogSection(uiState.blogSectionUiState)
        updateFollowButton(uiState.followButtonUiState)

        uiHelpers.setTextOrHide(text_author, uiState.authorName)
        text_by.setVisible(uiState.authorName != null)

        uiHelpers.updateVisibility(post_detail_dot_separator, uiState.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(post_detail_text_dateline, uiState.dateLine)
    }

    private fun updateBlogSection(
        blogSectionUiState: ReaderBlogSectionUiState
    ) {
        updateBlavatar(blogSectionUiState)

        uiHelpers.setTextOrHide(text_author_and_blog_name, blogSectionUiState.blogName)
        uiHelpers.setTextOrHide(text_blog_url, blogSectionUiState.blogUrl)

        layout_blog_section.setBackgroundResource(
                layout_blog_section.context.getDrawableResIdFromAttribute(
                        blogSectionUiState.blogSectionClickData?.background ?: 0
                )
        )
        blogSectionUiState.blogSectionClickData?.onBlogSectionClicked?.let {
            layout_blog_section.setOnClickListener {
                blogSectionUiState.blogSectionClickData.onBlogSectionClicked.invoke(
                        blogSectionUiState.postId,
                        blogSectionUiState.blogId
                )
            }
        } ?: run {
            layout_blog_section.setOnClickListener(null)
            layout_blog_section.isClickable = false
        }
    }

    private fun updateBlavatar(state: ReaderBlogSectionUiState) {
        uiHelpers.updateVisibility(image_avatar_or_blavatar, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_avatar_or_blavatar)
        } else {
            imageManager.loadIntoCircle(
                    image_avatar_or_blavatar,
                    state.blavatarType, state.avatarOrBlavatarUrl
            )
        }
        // we don't show the p2 style of header in post details yet
        uiHelpers.updateVisibility(authors_avatar, state.isAuthorAvatarVisible)
    }

    private fun updateFollowButton(followButtonUiState: FollowButtonUiState) {
        header_follow_button.isEnabled = followButtonUiState.isEnabled
        header_follow_button.setVisible(followButtonUiState.isVisible)
        header_follow_button.setIsFollowed(followButtonUiState.isFollowed)
        header_follow_button.setOnClickListener { followButtonUiState.onFollowButtonClicked?.invoke() }
    }
}
