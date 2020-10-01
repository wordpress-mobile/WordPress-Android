package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.reader_post_detail_header_view.view.*
import kotlinx.android.synthetic.main.reader_post_header.view.*
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * topmost view in post detail - shows blavatar, author name, blog name, and follow button
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
        val blogSectionUiState = uiState.blogSectionUiState

        updateBlavatar(blogSectionUiState)
        updateFollowButton(uiState.followButtonUiState)

        uiHelpers.setTextOrHide(text_author_and_blog_name, blogSectionUiState.blogName)
        uiHelpers.setTextOrHide(text_blog_url, blogSectionUiState.blogUrl)
        uiHelpers.updateVisibility(dot_separator, blogSectionUiState.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(text_dateline, blogSectionUiState.dateLine)

        layout_post_header.setBackgroundResource(
                layout_post_header.context.getDrawableResIdFromAttribute(
                        blogSectionUiState.blogSectionClickData?.background ?: 0
                )
        )
        blogSectionUiState.blogSectionClickData?.onBlogSectionClicked?.let {
            layout_post_header.setOnClickListener {
                blogSectionUiState.blogSectionClickData.onBlogSectionClicked.invoke(
                        blogSectionUiState.postId,
                        blogSectionUiState.blogId
                )
            }
        } ?: run {
            layout_post_header.setOnClickListener(null)
            layout_post_header.isClickable = false
        }
    }

    private fun updateBlavatar(state: ReaderBlogSectionUiState) {
        uiHelpers.updateVisibility(image_avatar_or_blavatar, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_avatar_or_blavatar)
        } else {
            imageManager.loadIntoCircle(
                    image_avatar_or_blavatar,
                    BLAVATAR_CIRCULAR, state.avatarOrBlavatarUrl
            )
        }
    }

    private fun updateFollowButton(followButtonUiState: FollowButtonUiState) {
        header_follow_button.isEnabled = followButtonUiState.isEnabled
        header_follow_button.setVisible(followButtonUiState.isVisible)
        header_follow_button.setIsFollowed(followButtonUiState.isFollowed)
        header_follow_button.setOnClickListener { followButtonUiState.onFollowButtonClicked?.invoke() }
    }
}
