package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.LinearLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderPostDetailHeaderViewBinding
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.toTypeface
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderFeaturedImageUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderAction
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.image.ImageType.PHOTO
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
    private var excerptTruncationCheck: Runnable? = null

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding = ReaderPostDetailHeaderViewBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

    fun updatePost(
        uiState: ReaderPostDetailsHeaderUiState,
        readingPreferences: ReaderReadingPreferences? = null,
        onHeaderAction: ((ReaderPostDetailsHeaderAction) -> Unit)? = null,
    ) = with(binding) {
        val themeValues = readingPreferences?.let {
            ReaderReadingPreferences.ThemeValues.from(root.context, it.theme)
        }

        updateTitle(uiState.title, readingPreferences, themeValues)

        uiHelpers.setTextOrHide(
            textBlogName,
            uiState.blogSectionUiState.blogName
        )
        uiState.blogSectionUiState.blogSectionClickData
            ?.onBlogSectionClicked?.let { onClick ->
                textBlogName.setOnClickListener { onClick.invoke() }
            } ?: run {
            textBlogName.setOnClickListener(null)
            textBlogName.isClickable = false
        }

        setAuthorAndDate(
            uiState.authorName,
            uiState.blogSectionUiState.dateLine,
        )

        updateFollowButton(uiState.followButtonUiState)

        updateAvatars(uiState.blogSectionUiState)
        updateBlogSectionClick(uiState.blogSectionUiState, onHeaderAction)

        uiHelpers.setTextOrHide(textReadingTime, uiState.readingTime)
        updateFeaturedImage(uiState.featuredImageUiState, onHeaderAction)
        uiHelpers.setTextOrHide(textExcerpt, uiState.excerpt)
        excerptTruncationCheck?.let { textExcerpt.removeCallbacks(it) }
        if (uiState.excerpt != null) {
            val check = Runnable {
                val isTruncated = textExcerpt.lineCount > 0
                    && textExcerpt.layout != null
                    && textExcerpt.layout.getEllipsisCount(
                        textExcerpt.lineCount - 1
                    ) > 0
                textExcerptViewMore.setVisible(isTruncated)
            }
            excerptTruncationCheck = check
            textExcerpt.post(check)
            textExcerptViewMore.setOnClickListener {
                textExcerpt.maxLines = Int.MAX_VALUE
                textExcerptViewMore.setVisible(false)
            }
        } else {
            excerptTruncationCheck = null
            textExcerptViewMore.setVisible(false)
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateTitle(
        title: UiString?,
        readingPreferences: ReaderReadingPreferences?,
        themeValues: ReaderReadingPreferences.ThemeValues?,
    ) {
        uiHelpers.setTextOrHide(textTitle, title)

        readingPreferences?.let { prefs ->
            val fontSize = resources.getDimension(
                R.dimen.text_sz_double_extra_large
            ) * prefs.fontSize.multiplier
            textTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            textTitle.typeface = prefs.fontFamily.toTypeface()
            themeValues?.let { textTitle.setTextColor(it.intTextColor) }
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateBlogSectionClick(
        state: ReaderBlogSectionUiState,
        onHeaderAction: ((ReaderPostDetailsHeaderAction) -> Unit)?
    ) {
        layoutBlogSection.root.apply {
            setBackgroundResource(
                context.getDrawableResIdFromAttribute(
                    state.blogSectionClickData?.background ?: 0
                )
            )
            if (onHeaderAction != null) {
                setOnClickListener {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.AuthorClicked
                    )
                }
            } else {
                setOnClickListener(null)
                isClickable = false
            }
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateAvatars(
        state: ReaderBlogSectionUiState
    ) {
        val blogAvatarImage = layoutBlogSection.blogSectionImageBlogAvatar
        uiHelpers.updateVisibility(
            blogAvatarImage, state.avatarOrBlavatarUrl != null
        )
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(blogAvatarImage)
        } else {
            imageManager.loadIntoCircle(
                blogAvatarImage,
                state.blavatarType,
                state.avatarOrBlavatarUrl
            )
        }

        val authorAvatarImage = layoutBlogSection.blogSectionImageAuthorAvatar
        val showAuthorsAvatar =
            state.authorAvatarUrl != null && state.isAuthorAvatarVisible
        uiHelpers.updateVisibility(authorAvatarImage, showAuthorsAvatar)
        if (!showAuthorsAvatar) {
            imageManager.cancelRequestAndClearImageView(authorAvatarImage)
        } else {
            imageManager.loadIntoCircle(
                authorAvatarImage,
                ImageType.BLAVATAR_CIRCULAR,
                state.authorAvatarUrl
            )
        }
    }

    private fun ReaderPostDetailHeaderViewBinding.updateFollowButton(
        followButtonUiState: FollowButtonUiState
    ) {
        headerFollowButtonContainer.setVisible(followButtonUiState.isVisible)
        headerFollowProgress.setVisible(
            followButtonUiState.isFollowActionRunning
        )
        headerFollowButton.apply {
            setIsLoading(followButtonUiState.isFollowActionRunning)
            isEnabled = !followButtonUiState.isFollowActionRunning
            setIsFollowed(followButtonUiState.isFollowed)
            setOnClickListener {
                followButtonUiState.onFollowButtonClicked?.invoke()
            }
        }
    }

    private fun setAuthorAndDate(
        authorName: String?,
        dateLine: String,
    ) = with(binding.layoutBlogSection) {
        uiHelpers.setTextOrHide(blogSectionTextAuthor, authorName)
        uiHelpers.setTextOrHide(blogSectionTextDateline, dateLine)
    }

    private fun ReaderPostDetailHeaderViewBinding.updateFeaturedImage(
        state: ReaderFeaturedImageUiState?,
        onHeaderAction: ((ReaderPostDetailsHeaderAction) -> Unit)?
    ) {
        headerFeaturedImage.setVisible(state != null)
        if (state != null) {
            imageManager.load(
                headerFeaturedImage, PHOTO, state.url, FIT_CENTER
            )
            headerFeaturedImage.setOnClickListener {
                onHeaderAction?.invoke(
                    ReaderPostDetailsHeaderAction.FeaturedImageClicked(
                        state.blogId, state.url
                    )
                )
            }
        } else {
            imageManager.cancelRequestAndClearImageView(
                headerFeaturedImage
            )
        }
    }
}
