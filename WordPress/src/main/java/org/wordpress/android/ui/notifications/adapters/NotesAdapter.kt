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
import androidx.core.text.BidiFormatter
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.NoteTimeGroup
import org.wordpress.android.models.Note.TimeStampComparator
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
    private val mAvatarSz: Int
    private val mTextIndentSize: Int
    private val mDataLoadedListener: DataLoadedListener
    private val mOnLoadMoreListener: OnLoadMoreListener?
    private val mNotes = ArrayList<Note>()
    private val mFilteredNotes = ArrayList<Note>()

    @JvmField
    @Inject
    var mImageManager: ImageManager? = null

    @JvmField
    @Inject
    var mNotificationsUtilsWrapper: NotificationsUtilsWrapper? = null

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
    private var mReloadNotesFromDBTask: ReloadNotesFromDBTask? = null

    interface DataLoadedListener {
        fun onDataLoaded(itemsCount: Int)
    }

    interface OnLoadMoreListener {
        fun onLoadMore(timestamp: Long)
    }

    private var mOnNoteClickListener: OnNoteClickListener? = null
    fun setFilter(newFilter: FILTERS) {
        currentFilter = newFilter
    }

    fun addAll(notes: List<Note>, clearBeforeAdding: Boolean) {
        notes.sortedWith(TimeStampComparator())
        try {
            if (clearBeforeAdding) {
                mNotes.clear()
            }
            mNotes.addAll(notes)
        } finally {
            myNotifyDatasetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun myNotifyDatasetChanged() {
        buildFilteredNotesList(mFilteredNotes, mNotes, currentFilter)
        notifyDataSetChanged()
        mDataLoadedListener.onDataLoaded(itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notifications_list_item, parent, false)
        return NoteViewHolder(view)
    }

    private fun getNoteAtPosition(position: Int): Note? {
        return if (isValidPosition(position)) {
            mFilteredNotes[position]
        } else null
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < mFilteredNotes.size
    }

    override fun getItemCount(): Int {
        return mFilteredNotes.size
    }

    override fun onBindViewHolder(noteViewHolder: NoteViewHolder, position: Int) {
        val note = getNoteAtPosition(position) ?: return
        noteViewHolder.mContentView.tag = note.id

        // Display group header
        val timeGroup = Note.getTimeGroupForTimestamp(note.timestamp)
        var previousTimeGroup: NoteTimeGroup? = null
        if (position > 0) {
            val previousNote = getNoteAtPosition(position - 1)
            previousTimeGroup = Note.getTimeGroupForTimestamp(
                previousNote!!.timestamp
            )
        }
        if (previousTimeGroup != null && previousTimeGroup == timeGroup) {
            noteViewHolder.mHeaderText.visibility = View.GONE
        } else {
            noteViewHolder.mHeaderText.visibility = View.VISIBLE
            if (timeGroup == NoteTimeGroup.GROUP_TODAY) {
                noteViewHolder.mHeaderText.setText(R.string.stats_timeframe_today)
            } else if (timeGroup == NoteTimeGroup.GROUP_YESTERDAY) {
                noteViewHolder.mHeaderText.setText(R.string.stats_timeframe_yesterday)
            } else if (timeGroup == NoteTimeGroup.GROUP_OLDER_TWO_DAYS) {
                noteViewHolder.mHeaderText.setText(R.string.older_two_days)
            } else if (timeGroup == NoteTimeGroup.GROUP_OLDER_WEEK) {
                noteViewHolder.mHeaderText.setText(R.string.older_last_week)
            } else {
                noteViewHolder.mHeaderText.setText(R.string.older_month)
            }
        }

        // Subject is stored in db as html to preserve text formatting
        var noteSubjectSpanned: Spanned = note.getFormattedSubject(mNotificationsUtilsWrapper)
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
            span.enableColors(noteViewHolder.mContentView.context)
        }
        noteViewHolder.mTxtSubject.text = noteSubjectSpanned
        val noteSubjectNoticon = note.commentSubjectNoticon
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            val parent = noteViewHolder.mTxtSubject.parent
            // Fix position of the subject noticon in the RtL mode
            if (parent is ViewGroup) {
                val textDirection = if (BidiFormatter.getInstance()
                        .isRtl(noteViewHolder.mTxtSubject.text)
                ) ViewCompat.LAYOUT_DIRECTION_RTL else ViewCompat.LAYOUT_DIRECTION_LTR
                ViewCompat.setLayoutDirection(parent, textDirection)
            }
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteViewHolder.itemView.context)) {
                noteViewHolder.mTxtSubjectNoticon.scaleX = -1f
            }
            CommentUtils.indentTextViewFirstLine(noteViewHolder.mTxtSubject, mTextIndentSize)
            noteViewHolder.mTxtSubjectNoticon.text = noteSubjectNoticon
            noteViewHolder.mTxtSubjectNoticon.visibility = View.VISIBLE
        } else {
            noteViewHolder.mTxtSubjectNoticon.visibility = View.GONE
        }
        val noteSnippet = note.commentSubject
        if (!TextUtils.isEmpty(noteSnippet)) {
            handleMaxLines(noteViewHolder.mTxtSubject, noteViewHolder.mTxtDetail)
            noteViewHolder.mTxtDetail.text = noteSnippet
            noteViewHolder.mTxtDetail.visibility = View.VISIBLE
        } else {
            noteViewHolder.mTxtDetail.visibility = View.GONE
        }
        val avatarUrl = GravatarUtils.fixGravatarUrl(note.iconURL, mAvatarSz)
        mImageManager!!.loadIntoCircle(
            noteViewHolder.mImgAvatar,
            ImageType.AVATAR_WITH_BACKGROUND,
            avatarUrl
        )
        if (note.isUnread) {
            noteViewHolder.mUnreadNotificationView.visibility = View.VISIBLE
        } else {
            noteViewHolder.mUnreadNotificationView.visibility = View.GONE
        }

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= itemCount - 1) {
            mOnLoadMoreListener.onLoadMore(note.timestamp)
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
        val layoutParams = noteViewHolder.mHeaderText.layoutParams as MarginLayoutParams
        layoutParams.topMargin = headerMarginTop
        noteViewHolder.mHeaderText.layoutParams = layoutParams
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
        mOnNoteClickListener = mNoteClickListener
    }

    fun cancelReloadNotesTask() {
        if (mReloadNotesFromDBTask != null && mReloadNotesFromDBTask!!.status != AsyncTask.Status.FINISHED) {
            mReloadNotesFromDBTask!!.cancel(true)
            mReloadNotesFromDBTask = null
        }
    }

    fun reloadNotesFromDBAsync() {
        cancelReloadNotesTask()
        mReloadNotesFromDBTask = ReloadNotesFromDBTask()
        mReloadNotesFromDBTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @Suppress("deprecation")
    @SuppressLint("StaticFieldLeak")
    private inner class ReloadNotesFromDBTask : AsyncTask<Void?, Void?, ArrayList<Note>>() {
        override fun doInBackground(vararg voids: Void?): ArrayList<Note> {
            return NotificationsTable.getLatestNotes()
        }

        override fun onPostExecute(notes: ArrayList<Note>) {
            mNotes.clear()
            mNotes.addAll(notes)
            myNotifyDatasetChanged()
        }
    }

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mContentView: View
        val mHeaderText: TextView
        val mTxtSubject: TextView
        val mTxtSubjectNoticon: TextView
        val mTxtDetail: TextView
        val mImgAvatar: ImageView
        val mUnreadNotificationView: View

        init {
            mContentView = checkNotNull(view.findViewById(R.id.note_content_container))
            mHeaderText = checkNotNull(view.findViewById(R.id.header_text))
            mTxtSubject = checkNotNull(view.findViewById(R.id.note_subject))
            mTxtSubjectNoticon = checkNotNull(view.findViewById(R.id.note_subject_noticon))
            mTxtDetail = checkNotNull(view.findViewById(R.id.note_detail))
            mImgAvatar = checkNotNull(view.findViewById(R.id.note_avatar))
            mUnreadNotificationView = checkNotNull(view.findViewById(R.id.notification_unread))
            mContentView.setOnClickListener(mOnClickListener)
        }
    }

    private val mOnClickListener = View.OnClickListener { v ->
        if (mOnNoteClickListener != null && v.tag is String) {
            mOnNoteClickListener!!.onClickNote(v.tag as String)
        }
    }

    init {
        (context.applicationContext as WordPress).component().inject(this)
        mDataLoadedListener = dataLoadedListener
        mOnLoadMoreListener = onLoadMoreListener

        // this is on purpose - we don't show more than a hundred or so notifications at a time so no need to set
        // stable IDs. This helps prevent crashes in case a note comes with no ID (we've code checking for that
        // elsewhere, but telling the RecyclerView.Adapter the notes have stable Ids and then failing to provide them
        // will make things go south as in https://github.com/wordpress-mobile/WordPress-Android/issues/8741
        setHasStableIds(false)
        mAvatarSz = context.resources.getDimension(R.dimen.notifications_avatar_sz).toInt()
        mTextIndentSize =
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
