package org.wordpress.android.ui.notifications.blocks

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

// A user block with slightly different formatting for display in a comment detail
@Suppress("LongParameterList")
class CommentUserNoteBlock(
    private val mContext: Context?, noteObject: FormattableContent,
    private val mCommentData: FormattableContent?,
    private val timestamp: Long, onNoteBlockTextClickListener: OnNoteBlockTextClickListener?,
    onGravatarClickedListener: OnGravatarClickedListener?,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper
) : UserNoteBlock(
    mContext, noteObject, onNoteBlockTextClickListener, onGravatarClickedListener, imageManager,
    notificationsUtilsWrapper
) {
    private var mCommentStatus = CommentStatus.APPROVED
    private var mNormalBackgroundColor = 0
    private var mIndentedLeftPadding = 0
    private var mStatusChanged = false
    private var mNoteBlockHolder: CommentUserNoteBlockHolder? = null

    override val blockType: BlockType
        get() = BlockType.USER_COMMENT
    override val layoutResourceId: Int
        get() = R.layout.note_block_comment_user

    init {
        avatarSize = mContext?.resources?.getDimensionPixelSize(R.dimen.avatar_sz_small) ?: 0
    }

    interface OnCommentStatusChangeListener {
        fun onCommentStatusChanged(newStatus: CommentStatus)
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    override fun configureView(view: View): View {
        mNoteBlockHolder = view.tag as CommentUserNoteBlockHolder
        setUserName()
        setUserCommentAgo()
        setUserAvatar()
        setUserComment()
        return view
    }

    private fun setUserName() {
        mNoteBlockHolder?.mNameTextView?.text = noteText.toString()
    }

    private fun setUserCommentAgo() {
        mNoteBlockHolder?.mAgoTextView?.text = DateTimeUtils.timeSpanFromTimestamp(
            timestamp,
            mNoteBlockHolder?.mAgoTextView?.context
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUserAvatar() {
        var imageUrl = ""
        if (hasImageMediaItem()) {
            noteMediaItem?.url?.let { imageUrl = WPAvatarUtils.rewriteAvatarUrl(it, avatarSize) }
            mNoteBlockHolder?.mAvatarImageView?.contentDescription =
                mContext?.getString(R.string.profile_picture, noteText.toString())
            if (!TextUtils.isEmpty(userUrl)) {
                mNoteBlockHolder?.mAvatarImageView?.setOnClickListener { showBlogPreview() }
                mNoteBlockHolder?.mAvatarImageView?.setOnTouchListener(mOnGravatarTouchListener)
            } else {
                mNoteBlockHolder?.mAvatarImageView?.setOnClickListener(null)
                mNoteBlockHolder?.mAvatarImageView?.setOnTouchListener(null)
                mNoteBlockHolder?.mAvatarImageView?.contentDescription = null
            }
        } else {
            mNoteBlockHolder?.mAvatarImageView?.setOnClickListener(null)
            mNoteBlockHolder?.mAvatarImageView?.setOnTouchListener(null)
            mNoteBlockHolder?.mAvatarImageView?.contentDescription = null
        }
        mNoteBlockHolder?.mAvatarImageView?.let {
            mImageManager.loadIntoCircle(it, ImageType.AVATAR_WITH_BACKGROUND, imageUrl)
        }
    }

    private fun setUserComment() {
        val spannable = getCommentTextOfNotification(mNoteBlockHolder)
        val spans = spannable.getSpans(0, spannable.length, NoteBlockClickableSpan::class.java)
        mContext?.let {
            for (span in spans) {
                span.enableColors(it)
            }
        }
        mNoteBlockHolder?.mCommentTextView?.text = spannable
    }

    private fun getCommentTextOfNotification(noteBlockHolder: CommentUserNoteBlockHolder?): Spannable {
        val builder = mNotificationsUtilsWrapper.getSpannableContentForRanges(
            mCommentData,
            noteBlockHolder?.mCommentTextView,
            onNoteBlockTextClickListener,
            false
        )
        return removeNewLineInList(builder)
    }

    private fun removeNewLineInList(builder: SpannableStringBuilder): Spannable {
        var content = builder.toString()
        while (content.contains(DOUBLE_EMPTY_LINE)) {
            val doubleSpaceIndex = content.indexOf(DOUBLE_EMPTY_LINE)
            builder.replace(
                doubleSpaceIndex,
                doubleSpaceIndex + DOUBLE_EMPTY_LINE.length,
                EMPTY_LINE
            )
            content = builder.toString()
        }
        return builder
    }

    override fun getViewHolder(view: View): Any = CommentUserNoteBlockHolder(view)

    private inner class CommentUserNoteBlockHolder constructor(view: View) {
        val mAvatarImageView: ImageView = view.findViewById(R.id.user_avatar)
        val mNameTextView: TextView = view.findViewById(R.id.user_name)
        val mAgoTextView: TextView = view.findViewById<TextView?>(R.id.user_comment_ago).apply {
            visibility = View.VISIBLE
        }
        val mCommentTextView: TextView = view.findViewById<TextView?>(R.id.user_comment).apply {
            movementMethod = NoteBlockLinkMovementMethod()
            setOnClickListener {
                // show all comments on this post when user clicks the comment text
                onNoteBlockTextClickListener?.showReaderPostComments()
            }
        }
    }

    fun configureResources(context: Context?) {
        mNormalBackgroundColor = context?.getColorFromAttribute(com.google.android.material.R.attr.colorSurface) ?: 0
        // Double margin_extra_large for increased indent in comment replies
        mIndentedLeftPadding = (context?.resources?.getDimensionPixelSize(R.dimen.margin_extra_large) ?: 0) * 2
    }

    val onCommentChangeListener: OnCommentStatusChangeListener =
        object : OnCommentStatusChangeListener {
            override fun onCommentStatusChanged(newStatus: CommentStatus) {
                mCommentStatus = newStatus
                mStatusChanged = true
            }
        }

    fun setCommentStatus(status: CommentStatus) {
        mCommentStatus = status
    }

    companion object {
        private const val EMPTY_LINE = "\n\t"
        private const val DOUBLE_EMPTY_LINE = "\n\t\n\t"
    }
}
