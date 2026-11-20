package org.wordpress.android.ui.notifications.adapters

import android.content.res.ColorStateList
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.BidiFormatter
import androidx.core.text.trimmedLength
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.NotificationsListItemBinding
import org.wordpress.android.models.Note
import org.wordpress.android.models.Notification
import org.wordpress.android.ui.comments.CommentUtils
import org.wordpress.android.ui.notifications.NotificationsListViewModel
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.DateUtils.isSameDay
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

class NoteViewHolder(
    private val binding: NotificationsListItemBinding,
    private val inlineActionEvents: MutableSharedFlow<NotificationsListViewModel.InlineActionEvent>,
    private val coroutineScope: CoroutineScope
) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper
    @Inject
    lateinit var imageManager: ImageManager

    init {
        (itemView.context.applicationContext as WordPress).component().inject(this)
    }

    fun bindTimeGroupHeader(note: Note, previousNote: Note?, position: Int) {
        // Display time group header
        timeGroupHeaderText(note, previousNote)?.let { timeGroupText ->
            with(binding.headerText) {
                visibility = View.VISIBLE
                setText(timeGroupText)
            }
        } ?: run {
            binding.headerText.visibility = View.GONE
        }

        // handle the margin top for the header
        val headerMarginTop: Int
        val context = itemView.context
        headerMarginTop = if (position == 0) {
            context.resources
                .getDimensionPixelSize(R.dimen.notifications_header_margin_top_position_0)
        } else {
            context.resources
                .getDimensionPixelSize(R.dimen.notifications_header_margin_top_position_n)
        }
        val layoutParams = binding.headerText.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = headerMarginTop
        binding.headerText.layoutParams = layoutParams
    }

    fun bindInlineActions(note: Note) = Notification.from(note).let { notification ->
        when (notification) {
            Notification.Comment -> bindLikeCommentAction(note)
            is Notification.NewPost -> bindLikePostAction(note)
            is Notification.PostLike -> bindShareAction(notification)
            is Notification.Unknown -> {
                binding.action.isVisible = false
            }
        }
    }

    private fun bindShareAction(notification: Notification.PostLike) {
        binding.action.setImageResource(R.drawable.block_share)
        val color = binding.root.context.getColorFromAttribute(R.attr.wpColorOnSurfaceMedium)
        ImageViewCompat.setImageTintList(binding.action, ColorStateList.valueOf(color))
        binding.action.isVisible = true
        binding.action.setOnClickListener {
            coroutineScope.launch {
                inlineActionEvents.emit(
                    NotificationsListViewModel.InlineActionEvent.SharePostButtonTapped(notification)
                )
            }
        }
        binding.action.contentDescription = binding.root.context.getString(R.string.share_action)
    }

    private fun bindLikePostAction(note: Note) {
        if (note.canLikePost().not()) return
        setupLikeIcon(note.hasLikedPost())
        binding.action.setOnClickListener {
            val liked = note.hasLikedPost().not()
            setupLikeIcon(liked)
            coroutineScope.launch {
                inlineActionEvents.emit(
                    NotificationsListViewModel.InlineActionEvent.LikePostButtonTapped(note, liked)
                )
            }
        }
    }

    private fun bindLikeCommentAction(note: Note) {
        if (note.canLikeComment().not()) return
        setupLikeIcon(note.hasLikedComment())
        binding.action.setOnClickListener {
            val liked = note.hasLikedComment().not()
            setupLikeIcon(liked)
            coroutineScope.launch {
                inlineActionEvents.emit(
                    NotificationsListViewModel.InlineActionEvent.LikeCommentButtonTapped(
                        note,
                        liked
                    )
                )
            }
        }
    }

    private fun setupLikeIcon(liked: Boolean) {
        binding.action.isVisible = true
        binding.action.setImageResource(if (liked) R.drawable.star_filled else R.drawable.star_empty)
        val color = if (liked) binding.root.context.getColor(R.color.inline_action_filled)
        else binding.root.context.getColorFromAttribute(R.attr.wpColorOnSurfaceMedium)
        ImageViewCompat.setImageTintList(binding.action, ColorStateList.valueOf(color))
        binding.action.contentDescription =
            binding.root.context.getString(if (liked) R.string.mnu_comment_liked else R.string.reader_label_like)
    }

    private fun timeGroupHeaderText(note: Note, previousNote: Note?): String? {
        val noteDate = Date(note.timestamp * MILLISECOND)

        // If we have a previous note, check if it's from the same calendar day
        previousNote?.let { prevNote ->
            val prevNoteDate = Date(prevNote.timestamp * MILLISECOND)
            if (isSameDay(noteDate, prevNoteDate)) {
                // Same day as previous note, don't show a header
                return null
            }
        }

        return when (note.timeGroup) {
            Note.NoteTimeGroup.GROUP_TODAY -> binding.root.context.getString(R.string.stats_timeframe_today)
            Note.NoteTimeGroup.GROUP_YESTERDAY -> binding.root.context.getString(R.string.stats_timeframe_yesterday)
            Note.NoteTimeGroup.GROUP_OLDER_TWO_DAYS,
            Note.NoteTimeGroup.GROUP_OLDER_WEEK,
            Note.NoteTimeGroup.GROUP_OLDER_MONTH -> timeGroupHeaderDate(noteDate)
        }
    }

    fun bindSubject(note: Note) {
        // Subject is stored in db as html to preserve text formatting
        var noteSubjectSpanned: Spanned = note.getFormattedSubject(notificationsUtilsWrapper)
        // Trim the '\n\n' added by HtmlCompat.fromHtml(...)
        noteSubjectSpanned = noteSubjectSpanned.subSequence(
            0,
            noteSubjectSpanned.trimmedLength()
        ) as Spanned
        val spans = noteSubjectSpanned.getSpans(
            0,
            noteSubjectSpanned.length,
            NoteBlockClickableSpan::class.java
        )
        for (span in spans) {
            span.enableColors(itemView.context)
        }
        binding.noteSubject.text = noteSubjectSpanned
    }

    fun bindSubjectNoticon(note: Note) {
        val noteSubjectNoticon = note.commentSubjectNoticon
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            val parent = binding.noteSubject.parent
            // Fix position of the subject noticon in the RtL mode
            if (parent is ViewGroup) {
                val textDirection = if (BidiFormatter.getInstance()
                        .isRtl(binding.noteSubject.text)
                ) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                parent.layoutDirection = textDirection
            }
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(itemView.context)) {
                binding.noteSubjectNoticon.scaleX = -1f
            }
            val textIndentSize = itemView.context.resources
                .getDimensionPixelSize(R.dimen.notifications_text_indent_sz)
            CommentUtils.indentTextViewFirstLine(binding.noteSubject, textIndentSize)
            binding.noteSubjectNoticon.text = noteSubjectNoticon
            binding.noteSubjectNoticon.visibility = View.VISIBLE
        } else {
            binding.noteSubjectNoticon.visibility = View.GONE
        }
    }

    fun bindContent(note: Note) {
        val noteSnippet = note.commentSubject
        if (!TextUtils.isEmpty(noteSnippet)) {
            handleMaxLines(binding.noteSubject, binding.noteDetail)
            binding.noteDetail.text = noteSnippet
            binding.noteDetail.visibility = View.VISIBLE
        } else {
            binding.noteDetail.visibility = View.GONE
        }
    }

    private fun handleMaxLines(subject: TextView, detail: TextView) {
        subject.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                subject.viewTreeObserver.removeOnPreDrawListener(this)
                if (subject.lineCount == 2) {
                    detail.maxLines = 1
                } else {
                    detail.maxLines = 2
                }
                return false
            }
        })
    }

    fun bindAvatars(note: Note) {
        if (note.shouldShowMultipleAvatars() && note.iconURLs != null && note.iconURLs!!.size > 1) {
            val avatars = note.iconURLs!!.toList()
            if (avatars.size == 2) {
                binding.noteAvatar.visibility = View.INVISIBLE
                binding.twoAvatarsView.root.visibility = View.VISIBLE
                binding.threeAvatarsView.root.visibility = View.INVISIBLE
                loadAvatar(binding.twoAvatarsView.twoAvatars1, avatars[1])
                loadAvatar(binding.twoAvatarsView.twoAvatars2, avatars[0])
            } else { // size > 3
                binding.noteAvatar.visibility = View.INVISIBLE
                binding.twoAvatarsView.root.visibility = View.INVISIBLE
                binding.threeAvatarsView.root.visibility = View.VISIBLE
                loadAvatar(binding.threeAvatarsView.threeAvatars1, avatars[2])
                loadAvatar(binding.threeAvatarsView.threeAvatars2, avatars[1])
                loadAvatar(binding.threeAvatarsView.threeAvatars3, avatars[0])
            }
        } else { // single avatar
            binding.noteAvatar.visibility = View.VISIBLE
            binding.twoAvatarsView.root.visibility = View.INVISIBLE
            binding.threeAvatarsView.root.visibility = View.INVISIBLE
            loadAvatar(binding.noteAvatar, note.iconURL)
        }
    }

    private fun loadAvatar(imageView: ImageView, avatarUrl: String) {
        val avatarSize = imageView.context.resources.getDimension(R.dimen.notifications_avatar_sz).roundToInt()
        val url = GravatarUtils.fixGravatarUrl(avatarUrl, avatarSize)
        imageManager.loadIntoCircle(imageView, ImageType.AVATAR_WITH_BACKGROUND, url)
    }

    private fun Note.shouldShowMultipleAvatars() = isFollowType || isLikeType || isCommentLikeType

    fun bindOthers(note: Note, onNoteClicked: (String) -> Unit) {
        binding.noteContentContainer.setOnClickListener { onNoteClicked(note.id) }
        binding.notificationUnread.isVisible = note.isUnread
    }

    private val Note.timeGroup
        get() = Note.getTimeGroupForTimestamp(timestamp)
}

private const val MILLISECOND = 1000

/**
 * Get formatted date string for display in notification headers
 */
private fun timeGroupHeaderDate(date: Date): String {
    val calendar = Calendar.getInstance()
    val currentYear = calendar[Calendar.YEAR]
    calendar.time = date
    val noteYear = calendar[Calendar.YEAR]

    val text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
    return when (noteYear) {
        currentYear -> text.replace(", $noteYear", "")
        else -> text
    }
}
