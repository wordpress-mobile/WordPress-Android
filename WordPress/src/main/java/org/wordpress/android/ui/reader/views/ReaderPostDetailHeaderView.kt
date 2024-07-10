package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderPostDetailHeaderViewBinding
import org.wordpress.android.databinding.ReaderPostDetailHeaderViewNewBinding
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.toTypeface
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.InteractionSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.ReaderImprovementsFeatureConfig
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
    private val binding: ReaderPostDetailHeaderBinding

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var readerImprovementsFeatureConfig: ReaderImprovementsFeatureConfig

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding = if (readerImprovementsFeatureConfig.isEnabled()) {
            val viewBinding = ReaderPostDetailHeaderViewNewBinding.inflate(LayoutInflater.from(context), this, true)
            ReaderPostDetailHeaderBinding.ImprovementsEnabled(
                viewBinding,
                uiHelpers
            )
        } else {
            val viewBinding = ReaderPostDetailHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)
            ReaderPostDetailHeaderBinding.ImprovementsDisabled(
                viewBinding,
                uiHelpers
            )
        }
    }

    fun updatePost(
        uiState: ReaderPostDetailsHeaderUiState,
        readingPreferences: ReaderReadingPreferences? = null,
    ) = with(binding) {
        expandableTagsView.setVisible(uiState.tagItemsVisibility)
        expandableTagsView.updateUi(uiState.tagItems, readingPreferences)

        updateTitle(uiState.title, readingPreferences)

        setAuthorAndDate(uiState.authorName, uiState.dateLine)

        uiHelpers.setTextOrHide(blogNameText, uiState.blogSectionUiState.blogName)
        blogUrlText?.let { uiHelpers.setTextOrHide(it, uiState.blogSectionUiState.blogUrl) }

        followButton.update(uiState.followButtonUiState)

        updateAvatars(uiState.blogSectionUiState)
        updateBlogSectionClick(uiState.blogSectionUiState)

        updateInteractionSection(uiState.interactionSectionUiState, readingPreferences)
    }

    private fun ReaderPostDetailHeaderBinding.updateTitle(
        title: UiString?,
        readingPreferences: ReaderReadingPreferences?
    ) {
        uiHelpers.setTextOrHide(titleText, title)

        readingPreferences?.let { prefs ->
            // Using the base font from the Improved header Theme for now
            val fontSize = resources.getDimension(R.dimen.text_sz_double_extra_large) * prefs.fontSize.multiplier
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            titleText.typeface = prefs.fontFamily.toTypeface()
        }
    }

    private fun ReaderPostDetailHeaderBinding.updateBlogSectionClick(
        state: ReaderBlogSectionUiState
    ) {
        blogSectionRoot.apply {
            setBackgroundResource(context.getDrawableResIdFromAttribute(state.blogSectionClickData?.background ?: 0))
            state.blogSectionClickData?.onBlogSectionClicked?.let { onClick ->
                setOnClickListener { onClick.invoke() }
            } ?: run {
                setOnClickListener(null)
                isClickable = false
            }
        }
    }

    private fun ReaderPostDetailHeaderBinding.updateAvatars(state: ReaderBlogSectionUiState) {
        uiHelpers.updateVisibility(blogAvatarImage, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(blogAvatarImage)
        } else {
            imageManager.loadIntoCircle(blogAvatarImage, state.blavatarType, state.avatarOrBlavatarUrl)
        }

        val showAuthorsAvatar = state.authorAvatarUrl != null && state.isAuthorAvatarVisible
        uiHelpers.updateVisibility(authorAvatarImage, showAuthorsAvatar)
        if (!showAuthorsAvatar) {
            imageManager.cancelRequestAndClearImageView(authorAvatarImage)
        } else {
            imageManager.loadIntoCircle(authorAvatarImage, ImageType.BLAVATAR_CIRCULAR, state.authorAvatarUrl!!)
        }
    }

    private fun ReaderFollowButton.update(followButtonUiState: FollowButtonUiState) {
        isEnabled = followButtonUiState.isEnabled
        setVisible(followButtonUiState.isVisible)
        setIsFollowed(followButtonUiState.isFollowed)
        setOnClickListener { followButtonUiState.onFollowButtonClicked?.invoke() }
    }

    private interface ReaderPostDetailHeaderBinding {
        val titleText: MaterialTextView
        val blogNameText: MaterialTextView
        val blogUrlText: MaterialTextView?
        val expandableTagsView: ReaderExpandableTagsView
        val blogAvatarImage: ImageView
        val authorAvatarImage: ImageView
        val followButton: ReaderFollowButton
        val blogSectionRoot: ViewGroup

        fun setAuthorAndDate(authorName: String?, dateLine: String)

        fun updateInteractionSection(state: InteractionSectionUiState, readingPreferences: ReaderReadingPreferences?)

        class ImprovementsDisabled(
            private val binding: ReaderPostDetailHeaderViewBinding,
            private val uiHelpers: UiHelpers,
        ) : ReaderPostDetailHeaderBinding {
            override val titleText: MaterialTextView
                get() = binding.textTitle
            override val blogNameText: MaterialTextView
                get() = binding.layoutBlogSection.textAuthorAndBlogName
            override val blogUrlText: MaterialTextView
                get() = binding.layoutBlogSection.textBlogUrl
            override val expandableTagsView: ReaderExpandableTagsView
                get() = binding.expandableTagsView
            override val blogAvatarImage: ImageView
                get() = binding.layoutBlogSection.imageAvatarOrBlavatar
            override val authorAvatarImage: ImageView
                get() = binding.layoutBlogSection.authorsAvatar
            override val followButton: ReaderFollowButton
                get() = binding.headerFollowButton
            override val blogSectionRoot: ViewGroup
                get() = binding.layoutBlogSection.root

            override fun setAuthorAndDate(authorName: String?, dateLine: String) = with(binding) {
                uiHelpers.setTextOrHide(textAuthor, authorName)
                uiHelpers.setTextOrHide(postDetailTextDateline, dateLine)

                textBy.setVisible(authorName != null)
                postDetailDotSeparator.setVisible(authorName != null)
            }

            override fun updateInteractionSection(
                state: InteractionSectionUiState,
                readingPreferences: ReaderReadingPreferences?
            ) {
                // do nothing
            }
        }

        class ImprovementsEnabled(
            private val binding: ReaderPostDetailHeaderViewNewBinding,
            private val uiHelpers: UiHelpers,
        ) : ReaderPostDetailHeaderBinding {
            override val titleText: MaterialTextView
                get() = binding.textTitle
            override val blogNameText: MaterialTextView
                get() = binding.layoutBlogSection.blogSectionTextBlogName
            override val blogUrlText: MaterialTextView?
                get() = null
            override val expandableTagsView: ReaderExpandableTagsView
                get() = binding.expandableTagsView
            override val blogAvatarImage: ImageView
                get() = binding.layoutBlogSection.blogSectionImageBlogAvatar
            override val authorAvatarImage: ImageView
                get() = binding.layoutBlogSection.blogSectionImageAuthorAvatar
            override val followButton: ReaderFollowButton
                get() = binding.headerFollowButton
            override val blogSectionRoot: ViewGroup
                get() = binding.layoutBlogSection.root

            override fun setAuthorAndDate(authorName: String?, dateLine: String) = with(binding.layoutBlogSection) {
                uiHelpers.setTextOrHide(blogSectionTextAuthor, authorName)
                uiHelpers.setTextOrHide(blogSectionTextDateline, dateLine)

                blogSectionDotSeparator.setVisible(authorName != null)
            }

            override fun updateInteractionSection(
                state: InteractionSectionUiState,
                readingPreferences: ReaderReadingPreferences?
            ) {
                with(binding) {
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

                    readingPreferences?.let { prefs ->
                        // Ideally we should get from the view theme directly, but let's hardcode it for now
                        val baseFontSize = viewContext.resources.getDimension(R.dimen.text_sz_medium)
                        val fontSize = baseFontSize * prefs.fontSize.multiplier
                        headerLikeCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                        headerCommentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                        headerDotSeparator.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                        headerLikeCount.typeface = prefs.fontFamily.toTypeface()
                        headerCommentCount.typeface = prefs.fontFamily.toTypeface()
                        headerDotSeparator.typeface = prefs.fontFamily.toTypeface()
                    }
                }
            }
        }
    }
}
