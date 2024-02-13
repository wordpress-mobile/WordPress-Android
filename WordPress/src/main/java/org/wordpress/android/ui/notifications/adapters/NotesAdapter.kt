@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.notifications.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
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
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.NoteTimeGroup
import org.wordpress.android.models.Note.TimeStampComparator
import org.wordpress.android.models.NoteType
import org.wordpress.android.models.type
import org.wordpress.android.ui.comments.CommentUtils
import org.wordpress.android.ui.notifications.NotificationsListFragmentPage.OnNoteClickListener
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.NoteViewHolder
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class NotesAdapter(
    context: Context, dataLoadedListener: DataLoadedListener,
    onLoadMoreListener: OnLoadMoreListener?
) : RecyclerView.Adapter<NoteViewHolder>() {
    private val avatarSize: Int
    private val textIndentSize: Int
    private val dataLoadedListener: DataLoadedListener
    private val onLoadMoreListener: OnLoadMoreListener?
    private val notes = ArrayList<Note>()
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
    private var reloadNotesFromDBTask: ReloadNotesFromDBTask? = null

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

    fun addAll(notes: List<Note>, clearBeforeAdding: Boolean) {
        notes.sortedWith(TimeStampComparator())
        try {
            if (clearBeforeAdding) {
                this.notes.clear()
            }
            this.notes.addAll(notes)
        } finally {
            myNotifyDatasetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun myNotifyDatasetChanged() {
        buildFilteredNotesList(filteredNotes, notes, currentFilter)
        notifyDataSetChanged()
        dataLoadedListener.onDataLoaded(itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notifications_list_item, parent, false)
        return NoteViewHolder(view)
    }

    private fun getNoteAtPosition(position: Int): Note? {
        return if (isValidPosition(position)) {
            filteredNotes[position]
        } else null
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < filteredNotes.size
    }

    override fun getItemCount(): Int {
        return filteredNotes.size
    }

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
        val note = getNoteAtPosition(position) ?: return
        val previousNote = getNoteAtPosition(position - 1)
        noteViewHolder.contentView.tag = note.id

        // Display time group header
        timeGroupHeaderText(note, previousNote)?.let { timeGroupText ->
            with(noteViewHolder.headerText) {
                visibility = View.VISIBLE
                setText(timeGroupText)
            }
        } ?: run {
            noteViewHolder.headerText.visibility = View.GONE
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
            span.enableColors(noteViewHolder.contentView.context)
        }
        noteViewHolder.textSubject.text = noteSubjectSpanned
        val noteSubjectNoticon = note.commentSubjectNoticon
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            val parent = noteViewHolder.textSubject.parent
            // Fix position of the subject noticon in the RtL mode
            if (parent is ViewGroup) {
                val textDirection = if (BidiFormatter.getInstance()
                        .isRtl(noteViewHolder.textSubject.text)
                ) ViewCompat.LAYOUT_DIRECTION_RTL else ViewCompat.LAYOUT_DIRECTION_LTR
                ViewCompat.setLayoutDirection(parent, textDirection)
            }
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteViewHolder.itemView.context)) {
                noteViewHolder.textSubjectNoticon.scaleX = -1f
            }
            CommentUtils.indentTextViewFirstLine(noteViewHolder.textSubject, textIndentSize)
            noteViewHolder.textSubjectNoticon.text = noteSubjectNoticon
            noteViewHolder.textSubjectNoticon.visibility = View.VISIBLE
        } else {
            noteViewHolder.textSubjectNoticon.visibility = View.GONE
        }
        val noteSnippet = note.commentSubject
        if (!TextUtils.isEmpty(noteSnippet)) {
            handleMaxLines(noteViewHolder.textSubject, noteViewHolder.textDetail)
            noteViewHolder.textDetail.text = noteSnippet
            noteViewHolder.textDetail.visibility = View.VISIBLE
        } else {
            noteViewHolder.textDetail.visibility = View.GONE
        }
        noteViewHolder.loadAvatars(note)
        noteViewHolder.bindInlineActionIconsForNote(note)
        noteViewHolder.unreadNotificationView.isVisible = note.isUnread

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
        val layoutParams = noteViewHolder.headerText.layoutParams as MarginLayoutParams
        layoutParams.topMargin = headerMarginTop
        noteViewHolder.headerText.layoutParams = layoutParams
    }

    private fun NoteViewHolder.loadAvatars(note: Note) {
        if (note.shouldShowMultipleAvatars() && note.iconURLs != null && note.iconURLs!!.size > 1) {
            val avatars = note.iconURLs!!.toList()
            if (avatars.size == 2) {
                imageAvatar.visibility = View.INVISIBLE
                twoAvatarsView.visibility = View.VISIBLE
                threeAvatarsView.visibility = View.INVISIBLE
                loadAvatar(twoAvatars1, avatars[1])
                loadAvatar(twoAvatars2, avatars[0])
            } else { // size > 3
                imageAvatar.visibility = View.INVISIBLE
                twoAvatarsView.visibility = View.INVISIBLE
                threeAvatarsView.visibility = View.VISIBLE
                loadAvatar(threeAvatars1, avatars[2])
                loadAvatar(threeAvatars2, avatars[1])
                loadAvatar(threeAvatars3, avatars[0])
            }
        } else { // single avatar
            imageAvatar.visibility = View.VISIBLE
            twoAvatarsView.visibility = View.INVISIBLE
            threeAvatarsView.visibility = View.INVISIBLE
            loadAvatar(imageAvatar, note.iconURL)
        }
    }

    private fun loadAvatar(imageView: ImageView, avatarUrl: String) {
        val url = GravatarUtils.fixGravatarUrl(avatarUrl, avatarSize)
        imageManager.loadIntoCircle(imageView, ImageType.AVATAR_WITH_BACKGROUND, url)
    }

    private fun Note.shouldShowMultipleAvatars() = isFollowType || isLikeType || isCommentLikeType

    @Suppress("ForbiddenComment")
    private fun NoteViewHolder.bindInlineActionIconsForNote(note: Note) {
        when (note.type) {
            NoteType.Comment -> {
                actionIcon.setImageResource(R.drawable.star_empty)
                actionIcon.isVisible = true
                actionIcon.setOnClickListener {
                    // TODO: handle tap on comment's inline action icon (the star)
                }
            }
            NoteType.NewPost,
            NoteType.Reblog,
            NoteType.Like -> {
                // TODO: Use the icon from the Figma design
                actionIcon.setImageResource(R.drawable.gb_ic_share)
                actionIcon.isVisible = true
                actionIcon.setOnClickListener {
                    // TODO: handle tap on comment's inline action icon (the share icon)
                }
            }
            else -> {
                actionIcon.isVisible = false
            }
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

    fun setOnNoteClickListener(mNoteClickListener: OnNoteClickListener?) {
        onNoteClickListener = mNoteClickListener
    }

    fun cancelReloadNotesTask() {
        if (reloadNotesFromDBTask != null && reloadNotesFromDBTask!!.status != AsyncTask.Status.FINISHED) {
            reloadNotesFromDBTask!!.cancel(true)
            reloadNotesFromDBTask = null
        }
    }

    fun reloadNotesFromDBAsync() {
        cancelReloadNotesTask()
        reloadNotesFromDBTask = ReloadNotesFromDBTask()
        reloadNotesFromDBTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ReloadNotesFromDBTask : AsyncTask<Void?, Void?, ArrayList<Note>>() {
        override fun doInBackground(vararg voids: Void?): ArrayList<Note> {
            return NotificationsTable.getLatestNotes()
        }

        override fun onPostExecute(notes: ArrayList<Note>) {
            this@NotesAdapter.notes.clear()
            this@NotesAdapter.notes.addAll(notes)
            myNotifyDatasetChanged()
        }
    }

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentView: View
        val headerText: TextView
        val textSubject: TextView
        val textSubjectNoticon: TextView
        val textDetail: TextView
        val imageAvatar: ImageView
        val twoAvatarsView: View
        val twoAvatars1: ImageView
        val twoAvatars2: ImageView
        val threeAvatarsView: View
        val threeAvatars1: ImageView
        val threeAvatars2: ImageView
        val threeAvatars3: ImageView
        val unreadNotificationView: View
        val actionIcon: ImageView

        init {
            contentView = checkNotNull(view.findViewById(R.id.note_content_container))
            headerText = checkNotNull(view.findViewById(R.id.header_text))
            textSubject = checkNotNull(view.findViewById(R.id.note_subject))
            textSubjectNoticon = checkNotNull(view.findViewById(R.id.note_subject_noticon))
            textDetail = checkNotNull(view.findViewById(R.id.note_detail))
            imageAvatar = checkNotNull(view.findViewById(R.id.note_avatar))
            twoAvatars1 = checkNotNull(view.findViewById(R.id.two_avatars_1))
            twoAvatars2 = checkNotNull(view.findViewById(R.id.two_avatars_2))
            threeAvatars1 = checkNotNull(view.findViewById(R.id.three_avatars_1))
            threeAvatars2 = checkNotNull(view.findViewById(R.id.three_avatars_2))
            threeAvatars3 = checkNotNull(view.findViewById(R.id.three_avatars_3))
            twoAvatarsView = checkNotNull(view.findViewById(R.id.two_avatars_view))
            threeAvatarsView = checkNotNull(view.findViewById(R.id.three_avatars_view))
            unreadNotificationView = checkNotNull(view.findViewById(R.id.notification_unread))
            actionIcon = checkNotNull(view.findViewById(R.id.action))
            contentView.setOnClickListener(onClickListener)
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
        fun buildFilteredNotesList(
            filteredNotes: ArrayList<Note>,
            notes: ArrayList<Note>,
            filter: FILTERS
        ) {
            filteredNotes.clear()
            if (notes.isEmpty() || filter == FILTERS.FILTER_ALL) {
                filteredNotes.addAll(notes)
                return
            }
            for (currentNote in notes) {
                when (filter) {
                    FILTERS.FILTER_COMMENT -> if (currentNote.isCommentType) {
                        filteredNotes.add(currentNote)
                    }
                    FILTERS.FILTER_FOLLOW -> if (currentNote.isFollowType) {
                        filteredNotes.add(currentNote)
                    }
                    FILTERS.FILTER_UNREAD -> if (currentNote.isUnread) {
                        filteredNotes.add(currentNote)
                    }
                    FILTERS.FILTER_LIKE -> if (currentNote.isLikeType) {
                        filteredNotes.add(currentNote)
                    }
                    else -> Unit
                }
            }
        }
    }
}