package org.wordpress.android.ui.notifications.blocks

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock.OnGravatarClickedListener
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.getMediaUrlOrEmpty
import org.wordpress.android.util.getRangeIdOrZero
import org.wordpress.android.util.getRangeSiteIdOrZero
import org.wordpress.android.util.getRangeUrlOrEmpty
import org.wordpress.android.util.getTextOrEmpty
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

// Note header, displayed at top of detail view
@Suppress("LongParameterList")
class HeaderNoteBlock(
    context: Context?,
    private val mHeadersList: List<FormattableContent>?,
    private val mImageType: ImageType,
    onNoteBlockTextClickListener: OnNoteBlockTextClickListener?,
    private val mGravatarClickedListener: OnGravatarClickedListener?,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper
) : NoteBlock(
    FormattableContent(),
    imageManager,
    notificationsUtilsWrapper,
    onNoteBlockTextClickListener
) {
    private var replyToComment = false
    private var mAvatarSize = 0
    override val blockType: BlockType
        get() = BlockType.USER_HEADER
    override val layoutResourceId: Int
        get() = R.layout.note_block_header
    private val avatarUrl: String
        get() = WPAvatarUtils.rewriteAvatarUrl(getHeader(0).getMediaUrlOrEmpty(0), mAvatarSize)
    private val userUrl: String
        get() = getHeader(0).getRangeUrlOrEmpty(0)
    private val snippet: String
        get() = getHeader(1).getTextOrEmpty()

    private val mOnClickListener = View.OnClickListener { onNoteBlockTextClickListener?.showDetailForNoteIds() }

    init {
        mAvatarSize = context?.resources?.getDimensionPixelSize(R.dimen.avatar_sz_small) ?: 0
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    override fun configureView(view: View): View {
        val noteBlockHolder = view.tag as NoteHeaderBlockHolder
        val spannable: Spannable = mNotificationsUtilsWrapper.getSpannableContentForRanges(mHeadersList?.getOrNull(0))
        val spans = spannable.getSpans(0, spannable.length, NoteBlockClickableSpan::class.java)
        for (span in spans) {
            span.enableColors(view.context)
        }
        if (mImageType == ImageType.AVATAR_WITH_BACKGROUND) {
            mImageManager.loadIntoCircle(noteBlockHolder.mAvatarImageView, mImageType, avatarUrl)
        } else {
            mImageManager.load(noteBlockHolder.mAvatarImageView, mImageType, avatarUrl)
        }
        val siteId = getHeader(0).getRangeSiteIdOrZero(0)
        val userId = getHeader(0).getRangeIdOrZero(0)
        if (!TextUtils.isEmpty(userUrl) && siteId > 0 && userId > 0) {
            noteBlockHolder.mAvatarImageView.setOnClickListener {
                mGravatarClickedListener?.onGravatarClicked(siteId, userId, userUrl)
            }
            noteBlockHolder.mAvatarImageView.contentDescription =
                view.context.getString(R.string.profile_picture, spannable)
            noteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener)
            if (siteId == userId) {
                noteBlockHolder.mAvatarImageView.importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            } else {
                noteBlockHolder.mAvatarImageView.importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        } else {
            noteBlockHolder.mAvatarImageView.importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
            noteBlockHolder.mAvatarImageView.contentDescription = null
            noteBlockHolder.mAvatarImageView.setOnClickListener(null)
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null)
        }
        noteBlockHolder.mAvatarImageView.isVisible = replyToComment
        noteBlockHolder.mSnippetTextView.text = snippet
        return view
    }

    override fun getViewHolder(view: View): Any = NoteHeaderBlockHolder(view)

    fun getHeader(headerIndex: Int): FormattableContent? = mHeadersList?.getOrNull(headerIndex)

    fun setReplyToComment(isReplyToComment: Boolean) {
        replyToComment = isReplyToComment
    }

    private inner class NoteHeaderBlockHolder internal constructor(view: View) {
        val mSnippetTextView: TextView = view.findViewById(R.id.header_snippet)
        val mAvatarImageView: ImageView = view.findViewById(R.id.header_avatar)

        init {
            view.findViewById<View>(R.id.header_root_view).setOnClickListener(mOnClickListener)
        }
    }

    @Suppress("MagicNumber")
    private val mOnGravatarTouchListener = OnTouchListener { v, event ->
        val animationDuration = 150
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.9f).scaleY(0.9f)
                    .alpha(0.5f)
                    .setDuration(animationDuration.toLong())
                    .setInterpolator(DecelerateInterpolator())
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1.0f).scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(animationDuration.toLong())
                    .setInterpolator(DecelerateInterpolator())
                if (event.actionMasked == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    v.performClick()
                }
            }
        }
        true
    }
}
