package org.wordpress.android.ui.notifications.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.BidiFormatter
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.NotificationsListItemBinding
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.NoteTimeGroup
import org.wordpress.android.models.Notification
import org.wordpress.android.models.Notification.Comment
import org.wordpress.android.models.Notification.PostNotification
import org.wordpress.android.models.Notification.Unknown
import org.wordpress.android.ui.comments.CommentUtils
import org.wordpress.android.ui.notifications.NotificationsListFragmentPage.OnNoteClickListener
import org.wordpress.android.ui.notifications.NotificationsListViewModel.InlineActionEvent
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.NoteViewHolder
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class NotesAdapter(
    context: Context, dataLoadedListener: DataLoadedListener,
    onLoadMoreListener: OnLoadMoreListener?,
    private val inlineActionEvents: MutableSharedFlow<InlineActionEvent>,
) : RecyclerView.Adapter<NoteViewHolder>() {
    private val avatarSize: Int
    private val textIndentSize: Int
    private val dataLoadedListener: DataLoadedListener
    private val onLoadMoreListener: OnLoadMoreListener?
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    val filteredNotes = ArrayList<Note>()

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    enum class FILTERS {
        FILTER_ALL,
        FILTER_COMMENT,
        FILTER_FOLLOW,
        FILTER_LIKE,
        FILTER_UNREAD;

        override fun toString(): String {
            return when (this) {
                FILTER_ALL -> "all"
                FILTER_COMMENT -> "comment"
                FILTER_FOLLOW -> "follow"
                FILTER_LIKE -> "like"
                FILTER_UNREAD -> "unread"
            }
        }
    }

    var currentFilter = FILTERS.FILTER_ALL
        private set
    private var reloadLocalNotesJob: Job? = null

    interface DataLoadedListener {
        fun onDataLoaded(itemsCount: Int)
    }

    interface OnLoadMoreListener {
        fun onLoadMore(timestamp: Long)
    }

    private var onNoteClickListener: OnNoteClickListener? = null
    fun setFilter(newFilter: FILTERS) {
        currentFilter = newFilter
    }

    /**
     * Add notes to the adapter and notify the change
     */
    fun addAll(notes: List<Note>) = coroutineScope.launch {
        val newNotes = buildFilteredNotesList(notes, currentFilter)
        val diff = AsyncListDiffer(this@NotesAdapter, object: ItemCallback<Note>(){
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.json.toString() == newItem.json.toString()
        })

        filteredNotes.clear()
        filteredNotes.addAll(newNotes)
        withContext(Dispatchers.Main) {
            diff.submitList(newNotes)
            dataLoadedListener.onDataLoaded(itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder =
        NoteViewHolder(NotificationsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = filteredNotes.size

    private val Note.timeGroup
        get() = Note.getTimeGroupForTimestamp(timestamp)

    @StringRes
    private fun timeGroupHeaderText(note: Note, previousNote: Note?) =
        previousNote?.timeGroup.let { previousTimeGroup ->
            val timeGroup = note.timeGroup
            if (previousTimeGroup?.let { it == timeGroup } == true) {
                // If the previous time group exists and is the same, we don't need a new one
                null
            } else {
                // Otherwise, we create a new one
                when (timeGroup) {
                    NoteTimeGroup.GROUP_TODAY -> R.string.stats_timeframe_today
                    NoteTimeGroup.GROUP_YESTERDAY -> R.string.stats_timeframe_yesterday
                    NoteTimeGroup.GROUP_OLDER_TWO_DAYS -> R.string.older_two_days
                    NoteTimeGroup.GROUP_OLDER_WEEK -> R.string.older_last_week
                    NoteTimeGroup.GROUP_OLDER_MONTH -> R.string.older_month
                }
            }
        }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun onBindViewHolder(noteViewHolder: NoteViewHolder, position: Int) {
        val note = filteredNotes.getOrNull(position) ?: return
        val previousNote = filteredNotes.getOrNull(position - 1)
        noteViewHolder.binding.noteContentContainer.tag = note.id

        // Display time group header
        timeGroupHeaderText(note, previousNote)?.let { timeGroupText ->
            with(noteViewHolder.binding.headerText) {
                visibility = View.VISIBLE
                setText(timeGroupText)
            }
        } ?: run {
            noteViewHolder.binding.headerText.visibility = View.GONE
        }

        // Subject is stored in db as html to preserve text formatting
        var noteSubjectSpanned: Spanned = note.getFormattedSubject(notificationsUtilsWrapper)
        // Trim the '\n\n' added by HtmlCompat.fromHtml(...)
        noteSubjectSpanned = noteSubjectSpanned.subSequence(
            0,
            TextUtils.getTrimmedLength(noteSubjectSpanned)
        ) as Spanned
        val spans = noteSubjectSpanned.getSpans(
            0,
            noteSubjectSpanned.length,
            NoteBlockClickableSpan::class.java
        )
        for (span in spans) {
            span.enableColors(noteViewHolder.itemView.context)
        }
        noteViewHolder.binding.noteSubject.text = noteSubjectSpanned
        val noteSubjectNoticon = note.commentSubjectNoticon
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            val parent = noteViewHolder.binding.noteSubject.parent
            // Fix position of the subject noticon in the RtL mode
            if (parent is ViewGroup) {
                val textDirection = if (BidiFormatter.getInstance()
                        .isRtl(noteViewHolder.binding.noteSubject.text)
                ) ViewCompat.LAYOUT_DIRECTION_RTL else ViewCompat.LAYOUT_DIRECTION_LTR
                ViewCompat.setLayoutDirection(parent, textDirection)
            }
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteViewHolder.itemView.context)) {
                noteViewHolder.binding.noteSubjectNoticon.scaleX = -1f
            }
            CommentUtils.indentTextViewFirstLine(noteViewHolder.binding.noteSubject, textIndentSize)
            noteViewHolder.binding.noteSubjectNoticon.text = noteSubjectNoticon
            noteViewHolder.binding.noteSubjectNoticon.visibility = View.VISIBLE
        } else {
            noteViewHolder.binding.noteSubjectNoticon.visibility = View.GONE
        }
        val noteSnippet = note.commentSubject
        if (!TextUtils.isEmpty(noteSnippet)) {
            handleMaxLines(noteViewHolder.binding.noteSubject, noteViewHolder.binding.noteDetail)
            noteViewHolder.binding.noteDetail.text = noteSnippet
            noteViewHolder.binding.noteDetail.visibility = View.VISIBLE
        } else {
            noteViewHolder.binding.noteDetail.visibility = View.GONE
        }
        noteViewHolder.loadAvatars(note)
        noteViewHolder.bindInlineActionIconsForNote(note)
        noteViewHolder.binding.notificationUnread.isVisible = note.isUnread

        // request to load more comments when we near the end
        if (onLoadMoreListener != null && position >= itemCount - 1) {
            onLoadMoreListener.onLoadMore(note.timestamp)
        }
        val headerMarginTop: Int
        val context = noteViewHolder.itemView.context
        headerMarginTop = if (position == 0) {
            context.resources
                .getDimensionPixelSize(R.dimen.notifications_header_margin_top_position_0)
        } else {
            context.resources
                .getDimensionPixelSize(R.dimen.notifications_header_margin_top_position_n)
        }
        val layoutParams = noteViewHolder.binding.headerText.layoutParams as MarginLayoutParams
        layoutParams.topMargin = headerMarginTop
        noteViewHolder.binding.headerText.layoutParams = layoutParams
    }

    private fun NoteViewHolder.loadAvatars(note: Note) {
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
        val url = GravatarUtils.fixGravatarUrl(avatarUrl, avatarSize)
        imageManager.loadIntoCircle(imageView, ImageType.AVATAR_WITH_BACKGROUND, url)
    }

    private fun Note.shouldShowMultipleAvatars() = isFollowType || isLikeType || isCommentLikeType

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

    fun setOnNoteClickListener(mNoteClickListener: OnNoteClickListener?) {
        onNoteClickListener = mNoteClickListener
    }

    fun cancelReloadLocalNotes() {
        reloadLocalNotesJob?.cancel()
    }

    /**
     * Reload the notes from local database and update the adapter
     */
    fun reloadLocalNotes() {
        cancelReloadLocalNotes()
        reloadLocalNotesJob = coroutineScope.launch {
            val notes = NotificationsTable.getLatestNotes()
            addAll(notes)
        }
    }

    /**
     * Update the note in the adapter and notify the change
     */
    fun updateNote(note: Note) {
        val notePosition = filteredNotes.indexOfFirst { it.id == note.id }
        if (notePosition != -1) {
            filteredNotes[notePosition] = note
            notifyItemChanged(notePosition)
        }
    }

    inner class NoteViewHolder(val binding: NotificationsListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.noteContentContainer.setOnClickListener(onClickListener)
        }

        fun bindInlineActionIconsForNote(note: Note) = Notification.from(note).let { notification ->
            when (notification) {
                Comment -> bindLikeCommentAction(note)
                is PostNotification.NewPost -> bindLikePostAction(note)
                is PostNotification -> bindShareAction(notification)
                is Unknown -> {
                    binding.action.isVisible = false
                }
            }
        }

        private fun bindShareAction(notification: PostNotification) {
            binding.action.setImageResource(R.drawable.block_share)
            val color = binding.root.context.getColorFromAttribute(R.attr.wpColorOnSurfaceMedium)
            ImageViewCompat.setImageTintList(binding.action, ColorStateList.valueOf(color))
            binding.action.isVisible = true
            binding.action.setOnClickListener {
                coroutineScope.launch {
                    inlineActionEvents.emit(
                        InlineActionEvent.SharePostButtonTapped(notification)
                    )
                }
            }
        }

        private fun bindLikePostAction(note: Note) {
            if (note.canLikePost().not()) return
            setupLikeIcon(note.hasLikedPost())
            binding.action.setOnClickListener {
                val liked = note.hasLikedPost().not()
                setupLikeIcon(liked)
                coroutineScope.launch {
                    inlineActionEvents.emit(
                        InlineActionEvent.LikePostButtonTapped(note, liked)
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
                        InlineActionEvent.LikeCommentButtonTapped(note, liked)
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
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        if (onNoteClickListener != null && view.tag is String) {
            onNoteClickListener!!.onClickNote(view.tag as String)
        }
    }

    init {
        (context.applicationContext as WordPress).component().inject(this)
        this.dataLoadedListener = dataLoadedListener
        this.onLoadMoreListener = onLoadMoreListener

        // this is on purpose - we don't show more than a hundred or so notifications at a time so no need to set
        // stable IDs. This helps prevent crashes in case a note comes with no ID (we've code checking for that
        // elsewhere, but telling the RecyclerView.Adapter the notes have stable Ids and then failing to provide them
        // will make things go south as in https://github.com/wordpress-mobile/WordPress-Android/issues/8741
        setHasStableIds(false)
        avatarSize = context.resources.getDimension(R.dimen.notifications_avatar_sz).toInt()
        textIndentSize =
            context.resources.getDimensionPixelSize(R.dimen.notifications_text_indent_sz)
    }

    companion object {
        // Instead of building the filtered notes list dynamically, create it once and re-use it.
        // Otherwise it's re-created so many times during layout.
        @JvmStatic
        fun buildFilteredNotesList(
            notes: List<Note>,
            filter: FILTERS
        ): ArrayList<Note> = notes.filter { note ->
            when (filter) {
                FILTERS.FILTER_ALL -> true
                FILTERS.FILTER_COMMENT -> note.isCommentType
                FILTERS.FILTER_FOLLOW -> note.isFollowType
                FILTERS.FILTER_UNREAD -> note.isUnread
                FILTERS.FILTER_LIKE -> note.isLikeType
            }
        }.sortedByDescending { it.timestamp }.let { result -> ArrayList(result) }
    }
}
