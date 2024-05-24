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
    private val context: Context?, noteObject: FormattableContent,
    private val commentData: FormattableContent?,
    private val timestamp: Long, onNoteBlockTextClickListener: OnNoteBlockTextClickListener?,
    onGravatarClickedListener: OnGravatarClickedListener?,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper
) : UserNoteBlock(
    context, noteObject, onNoteBlockTextClickListener, onGravatarClickedListener, imageManager,
    notificationsUtilsWrapper
) {
    private var commentStatus = CommentStatus.APPROVED
    private var normalBackgroundColor = 0
    private var indentedLeftPadding = 0
    private var statusChanged = false
    private var holder: CommentUserNoteBlockHolder? = null

    override val blockType: BlockType
        get() = BlockType.USER_COMMENT
    override val layoutResourceId: Int
        get() = R.layout.note_block_comment_user

    init {
        avatarSize = context?.resources?.getDimensionPixelSize(R.dimen.avatar_sz_small) ?: 0
    }

    interface OnCommentStatusChangeListener {
        fun onCommentStatusChanged(newStatus: CommentStatus)
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    override fun configureView(view: View): View {
        holder = view.tag as CommentUserNoteBlockHolder
        setUserName()
        setUserCommentAgo()
        setUserAvatar()
        setUserComment()
        return view
    }

    private fun setUserName() {
        holder?.textName?.text = noteText.toString()
    }

    private fun setUserCommentAgo() {
        holder?.textDate?.text = DateTimeUtils.timeSpanFromTimestamp(
            timestamp,
            holder?.textDate?.context
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUserAvatar() {
        var imageUrl = ""
        if (hasImageMediaItem()) {
            noteMediaItem?.url?.let { imageUrl = WPAvatarUtils.rewriteAvatarUrl(it, avatarSize) }
            holder?.imageAvatar?.contentDescription =
                context?.getString(R.string.profile_picture, noteText.toString())
            if (!TextUtils.isEmpty(userUrl)) {
                holder?.imageAvatar?.setOnClickListener { showBlogPreview() }
                holder?.imageAvatar?.setOnTouchListener(mOnGravatarTouchListener)
            } else {
                holder?.imageAvatar?.setOnClickListener(null)
                holder?.imageAvatar?.setOnTouchListener(null)
                holder?.imageAvatar?.contentDescription = null
            }
        } else {
            holder?.imageAvatar?.setOnClickListener(null)
            holder?.imageAvatar?.setOnTouchListener(null)
            holder?.imageAvatar?.contentDescription = null
        }
        holder?.imageAvatar?.let {
            mImageManager.loadIntoCircle(it, ImageType.AVATAR_WITH_BACKGROUND, imageUrl)
        }
    }

    private fun setUserComment() {
        val spannable = getCommentTextOfNotification(holder)
        val spans = spannable.getSpans(0, spannable.length, NoteBlockClickableSpan::class.java)
        context?.let {
            for (span in spans) {
                span.enableColors(it)
            }
        }
        holder?.textComment?.text = spannable
    }

    private fun getCommentTextOfNotification(noteBlockHolder: CommentUserNoteBlockHolder?): Spannable {
        val builder = mNotificationsUtilsWrapper.getSpannableContentForRanges(
            commentData,
            noteBlockHolder?.textComment,
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
        val imageAvatar: ImageView = view.findViewById(R.id.user_avatar)
        val textName: TextView = view.findViewById(R.id.user_name)
        val textDate: TextView = view.findViewById<TextView?>(R.id.user_comment_ago).apply {
            visibility = View.VISIBLE
        }
        val textComment: TextView = view.findViewById<TextView?>(R.id.user_comment).apply {
            movementMethod = NoteBlockLinkMovementMethod()
            setOnClickListener {
                // show all comments on this post when user clicks the comment text
                onNoteBlockTextClickListener?.showReaderPostComments()
            }
        }
        val buttonMore = view.findViewById<View>(R.id.image_more).apply {
            setOnClickListener { onNoteBlockTextClickListener?.showActionPopup(this)  }
        }
    }

    fun configureResources(context: Context?) {
        normalBackgroundColor = context?.getColorFromAttribute(com.google.android.material.R.attr.colorSurface) ?: 0
        // Double margin_extra_large for increased indent in comment replies
        indentedLeftPadding = (context?.resources?.getDimensionPixelSize(R.dimen.margin_extra_large) ?: 0) * 2
    }

    val onCommentChangeListener: OnCommentStatusChangeListener =
        object : OnCommentStatusChangeListener {
            override fun onCommentStatusChanged(newStatus: CommentStatus) {
                commentStatus = newStatus
                statusChanged = true
            }
        }

    fun setCommentStatus(status: CommentStatus) {
        commentStatus = status
    }

    companion object {
        private const val EMPTY_LINE = "\n\t"
        private const val DOUBLE_EMPTY_LINE = "\n\t\n\t"
    }
}
