package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderBlogSectionViewBinding
import org.wordpress.android.databinding.ReaderPostDetailHeaderViewBinding
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

/**
 * topmost view in post detail
 */
class ReaderPostDetailHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ReaderPostDetailHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    fun updatePost(uiState: ReaderPostDetailsHeaderUiState) = with(binding) {
        expandableTagsView.setVisible(uiState.tagItemsVisibility)
        expandableTagsView.updateUi(uiState.tagItems)

        uiHelpers.setTextOrHide(textTitle, uiState.title)

        layoutBlogSection.updateBlogSection(uiState.blogSectionUiState)
        updateFollowButton(uiState.followButtonUiState)

        uiHelpers.setTextOrHide(textAuthor, uiState.authorName)
        textBy.setVisible(uiState.authorName != null)

        uiHelpers.updateVisibility(postDetailDotSeparator, uiState.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(postDetailTextDateline, uiState.dateLine)
    }

    private fun ReaderBlogSectionViewBinding.updateBlogSection(state: ReaderBlogSectionUiState) {
        updateBlavatar(state)

        uiHelpers.setTextOrHide(textAuthorAndBlogName, state.blogName)
        uiHelpers.setTextOrHide(textBlogUrl, state.blogUrl)

        root.apply {
            setBackgroundResource(context.getDrawableResIdFromAttribute(state.blogSectionClickData?.background ?: 0))
            state.blogSectionClickData?.onBlogSectionClicked?.let { onClick ->
                setOnClickListener { onClick.invoke(state.postId, state.blogId) }
            } ?: run {
                setOnClickListener(null)
                isClickable = false
            }
        }
    }

    private fun ReaderBlogSectionViewBinding.updateBlavatar(state: ReaderBlogSectionUiState) {
        uiHelpers.updateVisibility(imageAvatarOrBlavatar, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(imageAvatarOrBlavatar)
        } else {
            imageManager.loadIntoCircle(imageAvatarOrBlavatar, state.blavatarType, state.avatarOrBlavatarUrl)
        }
        // we don't show the p2 style of header in post details yet
        uiHelpers.updateVisibility(authorsAvatar, state.isAuthorAvatarVisible)
    }

    private fun ReaderPostDetailHeaderViewBinding.updateFollowButton(followButtonUiState: FollowButtonUiState) {
        headerFollowButton.isEnabled = followButtonUiState.isEnabled
        headerFollowButton.setVisible(followButtonUiState.isVisible)
        headerFollowButton.setIsFollowed(followButtonUiState.isFollowed)
        headerFollowButton.setOnClickListener { followButtonUiState.onFollowButtonClicked?.invoke() }
    }
}
