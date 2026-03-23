package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderPostDetailHeaderViewBinding
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.toTypeface
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.InteractionSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

/**
 * topmost view in post detail
 */
class ReaderPostDetailHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding: ReaderPostDetailHeaderViewBinding

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding = ReaderPostDetailHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun updatePost(
        uiState: ReaderPostDetailsHeaderUiState,
        readingPreferences: ReaderReadingPreferences? = null,
    ) = with(binding) {
        val themeValues = readingPreferences?.let {
            ReaderReadingPreferences.ThemeValues.from(root.context, it.theme)
        }

        expandableTagsView.setVisible(uiState.tagItemsVisibility)
        expandableTagsView.updateUi(uiState.tagItems, readingPreferences)

        updateTitle(uiState.title, readingPreferences, themeValues)

        setAuthorAndDate(uiState.authorName, uiState.dateLine)

        uiHelpers.setTextOrHide(layoutBlogSection.blogSectionTextBlogName, uiState.blogSectionUiState.blogName)

        updateFollowButton(uiState.followButtonUiState)

        updateAvatars(uiState.blogSectionUiState)
        updateBlogSectionClick(uiState.blogSectionUiState)

        updateInteractionSection(uiState.interactionSectionUiState, readingPreferences, themeValues)
    }

    private fun ReaderPostDetailHeaderViewBinding.updateTitle(
        title: UiString?,
        readingPreferences: ReaderReadingPreferences?,
        themeValues: ReaderReadingPreferences.ThemeValues?,
    ) {
        uiHelpers.setTextOrHide(textTitle, title)

        readingPreferences?.let { prefs ->
            val fontSize = resources.getDimension(R.dimen.text_sz_double_extra_large) * prefs.fontSize.multiplier
            textTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            textTitle.typeface = prefs.fontFamily.toTypeface()
            themeValues?.let { textTitle.setTextColor(it.intTextColor) }
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateBlogSectionClick(
        state: ReaderBlogSectionUiState
    ) {
        layoutBlogSection.root.apply {
            setBackgroundResource(context.getDrawableResIdFromAttribute(state.blogSectionClickData?.background ?: 0))
            state.blogSectionClickData?.onBlogSectionClicked?.let { onClick ->
                setOnClickListener { onClick.invoke() }
            } ?: run {
                setOnClickListener(null)
                isClickable = false
            }
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateAvatars(state: ReaderBlogSectionUiState) {
        val blogAvatarImage = layoutBlogSection.blogSectionImageBlogAvatar
        uiHelpers.updateVisibility(blogAvatarImage, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(blogAvatarImage)
        } else {
            imageManager.loadIntoCircle(blogAvatarImage, state.blavatarType, state.avatarOrBlavatarUrl)
        }

        val authorAvatarImage = layoutBlogSection.blogSectionImageAuthorAvatar
        val showAuthorsAvatar = state.authorAvatarUrl != null && state.isAuthorAvatarVisible
        uiHelpers.updateVisibility(authorAvatarImage, showAuthorsAvatar)
        if (!showAuthorsAvatar) {
            imageManager.cancelRequestAndClearImageView(authorAvatarImage)
        } else {
            imageManager.loadIntoCircle(authorAvatarImage, ImageType.BLAVATAR_CIRCULAR, state.authorAvatarUrl)
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateFollowButton(
        followButtonUiState: FollowButtonUiState
    ) {
        headerFollowButtonContainer.setVisible(followButtonUiState.isVisible)
        headerFollowProgress.setVisible(followButtonUiState.isFollowActionRunning)
        headerFollowButton.apply {
            setIsLoading(followButtonUiState.isFollowActionRunning)
            isEnabled = !followButtonUiState.isFollowActionRunning
            setIsFollowed(followButtonUiState.isFollowed)
            setOnClickListener { followButtonUiState.onFollowButtonClicked?.invoke() }
        }
    }

    private fun setAuthorAndDate(authorName: String?, dateLine: String) = with(binding.layoutBlogSection) {
        uiHelpers.setTextOrHide(blogSectionTextAuthor, authorName)
        uiHelpers.setTextOrHide(blogSectionTextDateline, dateLine)

        blogSectionDotSeparator.setVisible(authorName != null)
    }

    private fun updateInteractionSection(
        state: InteractionSectionUiState,
        readingPreferences: ReaderReadingPreferences?,
        themeValues: ReaderReadingPreferences.ThemeValues?,
    ) = with(binding) {
        val viewContext = root.context

        val likeCount = state.likeCount
        val commentCount = state.commentCount

        val likeLabel = ReaderUtils.getShortLikeLabelText(viewContext, likeCount)
            .takeIf { likeCount > 0 }
        val commentLabel = ReaderUtils.getShortCommentLabelText(viewContext, commentCount)
            .takeIf { commentCount > 0 }

        uiHelpers.setTextOrHide(headerLikeCount, likeLabel)
        uiHelpers.setTextOrHide(headerCommentCount, commentLabel)
        headerDotSeparator.isVisible = likeLabel != null && commentLabel != null

        headerLikeCount.setOnClickListener { state.onLikesClicked() }
        headerCommentCount.setOnClickListener { state.onCommentsClicked() }

        applyInteractionSectionTheme(readingPreferences, themeValues)
    }

    private fun applyInteractionSectionTheme(
        readingPreferences: ReaderReadingPreferences?,
        themeValues: ReaderReadingPreferences.ThemeValues?,
    ) {
        readingPreferences ?: return

        val baseFontSize = resources.getDimension(R.dimen.text_sz_medium)
        val fontSize = baseFontSize * readingPreferences.fontSize.multiplier
        val typeface = readingPreferences.fontFamily.toTypeface()
        val textColor = themeValues?.intTextColor

        listOf(binding.headerLikeCount, binding.headerCommentCount, binding.headerDotSeparator)
            .forEach { view ->
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                view.typeface = typeface
                textColor?.let { view.setTextColor(it) }
            }
    }
}
