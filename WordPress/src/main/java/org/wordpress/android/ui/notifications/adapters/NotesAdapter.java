package org.wordpress.android.ui.notifications.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.text.BidiFormatter;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.NoticonUtils;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.notifications.NotificationsListFragmentPage.OnNoteClickListener;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.RtlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.BadgedImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private final int mAvatarSz;
    private final int mColorUnread;
    private final int mTextIndentSize;

    private final DataLoadedListener mDataLoadedListener;
    private final OnLoadMoreListener mOnLoadMoreListener;
    private final ArrayList<Note> mNotes = new ArrayList<>();
    private final ArrayList<Note> mFilteredNotes = new ArrayList<>();
    @Inject protected ImageManager mImageManager;
    @Inject protected NotificationsUtilsWrapper mNotificationsUtilsWrapper;
    @Inject protected NoticonUtils mNoticonUtils;

    public enum FILTERS {
        FILTER_ALL,
        FILTER_COMMENT,
        FILTER_FOLLOW,
        FILTER_LIKE,
        FILTER_UNREAD;

        public String toString() {
            switch (this) {
                case FILTER_ALL:
                    return "all";
                case FILTER_COMMENT:
                    return "comment";
                case FILTER_FOLLOW:
                    return "follow";
                case FILTER_LIKE:
                    return "like";
                case FILTER_UNREAD:
                    return "unread";
                default:
                    return "all";
            }
        }
    }

    private FILTERS mCurrentFilter = FILTERS.FILTER_ALL;
    private ReloadNotesFromDBTask mReloadNotesFromDBTask;

    public interface DataLoadedListener {
        void onDataLoaded(int itemsCount);
    }

    public interface OnLoadMoreListener {
        void onLoadMore(long timestamp);
    }

    private OnNoteClickListener mOnNoteClickListener;

    public NotesAdapter(Context context, DataLoadedListener dataLoadedListener, OnLoadMoreListener onLoadMoreListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mDataLoadedListener = dataLoadedListener;
        mOnLoadMoreListener = onLoadMoreListener;

        // this is on purpose - we don't show more than a hundred or so notifications at a time so no need to set
        // stable IDs. This helps prevent crashes in case a note comes with no ID (we've code checking for that
        // elsewhere, but telling the RecyclerView.Adapter the notes have stable Ids and then failing to provide them
        // will make things go south as in https://github.com/wordpress-mobile/WordPress-Android/issues/8741
        setHasStableIds(false);

        mAvatarSz = (int) context.getResources().getDimension(R.dimen.notifications_avatar_sz);
        mColorUnread = ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface),
                        context.getResources().getInteger(R.integer.selected_list_item_opacity));
        mTextIndentSize = context.getResources().getDimensionPixelSize(R.dimen.notifications_text_indent_sz);
    }

    public void setFilter(FILTERS newFilter) {
        mCurrentFilter = newFilter;
    }

    public FILTERS getCurrentFilter() {
        return mCurrentFilter;
    }

    public void addAll(List<Note> notes, boolean clearBeforeAdding) {
        Collections.sort(notes, new Note.TimeStampComparator());
        try {
            if (clearBeforeAdding) {
                mNotes.clear();
            }
            mNotes.addAll(notes);
        } finally {
            myNotifyDatasetChanged();
        }
    }

    private void myNotifyDatasetChanged() {
        buildFilteredNotesList(mFilteredNotes, mNotes, mCurrentFilter);
        notifyDataSetChanged();
        if (mDataLoadedListener != null) {
            mDataLoadedListener.onDataLoaded(getItemCount());
        }
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notifications_list_item, parent, false);

        return new NoteViewHolder(view);
    }

    // Instead of building the filtered notes list dynamically, create it once and re-use it.
    // Otherwise it's re-created so many times during layout.
    public static void buildFilteredNotesList(ArrayList<Note> filteredNotes, ArrayList<Note> notes, FILTERS filter) {
        filteredNotes.clear();
        if (notes.isEmpty() || filter == FILTERS.FILTER_ALL) {
            filteredNotes.addAll(notes);
            return;
        }
        for (Note currentNote : notes) {
            switch (filter) {
                case FILTER_COMMENT:
                    if (currentNote.isCommentType()) {
                        filteredNotes.add(currentNote);
                    }
                    break;
                case FILTER_FOLLOW:
                    if (currentNote.isFollowType()) {
                        filteredNotes.add(currentNote);
                    }
                    break;
                case FILTER_UNREAD:
                    if (currentNote.isUnread()) {
                        filteredNotes.add(currentNote);
                    }
                    break;
                case FILTER_LIKE:
                    if (currentNote.isLikeType()) {
                        filteredNotes.add(currentNote);
                    }
                    break;
            }
        }
    }

    private Note getNoteAtPosition(int position) {
        if (isValidPosition(position)) {
            return mFilteredNotes.get(position);
        }

        return null;
    }

    public void replaceNote(Note newNote) {
        if (newNote != null) {
            int position = getPositionForNoteUnfiltered(newNote.getId());
            if (position != RecyclerView.NO_POSITION && position < mNotes.size()) {
                mNotes.set(position, newNote);
            }
        }
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mFilteredNotes.size());
    }

    @Override
    public int getItemCount() {
        return mFilteredNotes.size();
    }

    @Override
    public void onBindViewHolder(NoteViewHolder noteViewHolder, int position) {
        final Note note = getNoteAtPosition(position);
        if (note == null) {
            return;
        }
        noteViewHolder.mContentView.setTag(note.getId());

        // Display group header
        Note.NoteTimeGroup timeGroup = Note.getTimeGroupForTimestamp(note.getTimestamp());

        Note.NoteTimeGroup previousTimeGroup = null;
        if (position > 0) {
            Note previousNote = getNoteAtPosition(position - 1);
            previousTimeGroup = Note.getTimeGroupForTimestamp(previousNote.getTimestamp());
        }

        if (previousTimeGroup != null && previousTimeGroup == timeGroup) {
            noteViewHolder.mHeaderText.setVisibility(View.GONE);
            noteViewHolder.mHeaderDivider.setVisibility(View.GONE);
        } else {
            noteViewHolder.mHeaderText.setVisibility(View.VISIBLE);
            noteViewHolder.mHeaderDivider.setVisibility(View.VISIBLE);

            if (timeGroup == Note.NoteTimeGroup.GROUP_TODAY) {
                noteViewHolder.mHeaderText.setText(R.string.stats_timeframe_today);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_YESTERDAY) {
                noteViewHolder.mHeaderText.setText(R.string.stats_timeframe_yesterday);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_OLDER_TWO_DAYS) {
                noteViewHolder.mHeaderText.setText(R.string.older_two_days);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_OLDER_WEEK) {
                noteViewHolder.mHeaderText.setText(R.string.older_last_week);
            } else {
                noteViewHolder.mHeaderText.setText(R.string.older_month);
            }
        }

        CommentStatus commentStatus = CommentStatus.ALL;
        if (note.getCommentStatus() == CommentStatus.UNAPPROVED) {
            commentStatus = CommentStatus.UNAPPROVED;
        }

        if (!TextUtils.isEmpty(note.getLocalStatus())) {
            commentStatus = CommentStatus.fromString(note.getLocalStatus());
        }

        // Subject is stored in db as html to preserve text formatting
        Spanned noteSubjectSpanned = note.getFormattedSubject(mNotificationsUtilsWrapper);
        // Trim the '\n\n' added by Html.fromHtml()
        noteSubjectSpanned =
                (Spanned) noteSubjectSpanned.subSequence(0, TextUtils.getTrimmedLength(noteSubjectSpanned));

        NoteBlockClickableSpan[] spans =
                noteSubjectSpanned.getSpans(0, noteSubjectSpanned.length(), NoteBlockClickableSpan.class);
        for (NoteBlockClickableSpan span : spans) {
            span.enableColors(noteViewHolder.mContentView.getContext());
        }

        noteViewHolder.mTxtSubject.setText(noteSubjectSpanned);

        String noteSubjectNoticon = note.getCommentSubjectNoticon();
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            ViewParent parent = noteViewHolder.mTxtSubject.getParent();
            // Fix position of the subject noticon in the RtL mode
            if (parent instanceof ViewGroup) {
                int textDirection = BidiFormatter.getInstance().isRtl(noteViewHolder.mTxtSubject.getText())
                        ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR;
                ViewCompat.setLayoutDirection((ViewGroup) parent, textDirection);
            }
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteViewHolder.itemView.getContext())) {
                noteViewHolder.mTxtSubjectNoticon.setScaleX(-1);
            }
            CommentUtils.indentTextViewFirstLine(noteViewHolder.mTxtSubject, mTextIndentSize);
            noteViewHolder.mTxtSubjectNoticon.setText(noteSubjectNoticon);
            noteViewHolder.mTxtSubjectNoticon.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.mTxtSubjectNoticon.setVisibility(View.GONE);
        }

        String noteSnippet = note.getCommentSubject();
        if (!TextUtils.isEmpty(noteSnippet)) {
            noteViewHolder.mTxtSubject.setMaxLines(2);
            noteViewHolder.mTxtDetail.setText(noteSnippet);
            noteViewHolder.mTxtDetail.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.mTxtSubject.setMaxLines(3);
            noteViewHolder.mTxtDetail.setVisibility(View.GONE);
        }

        String avatarUrl = GravatarUtils.fixGravatarUrl(note.getIconURL(), mAvatarSz);
        mImageManager.loadIntoCircle(noteViewHolder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);

        boolean isUnread = note.isUnread();

        int gridicon = mNoticonUtils.noticonToGridicon(note.getNoticonCharacter());
        noteViewHolder.mImgAvatar.setBadgeIcon(gridicon);
        if (commentStatus == CommentStatus.UNAPPROVED) {
            noteViewHolder.mImgAvatar.setBadgeBackground(R.drawable.bg_oval_warning_dark);
        } else if (isUnread) {
            noteViewHolder.mImgAvatar.setBadgeBackground(R.drawable.bg_oval_primary);
        } else {
            noteViewHolder.mImgAvatar.setBadgeBackground(R.drawable.bg_oval_neutral_20);
        }

        if (isUnread) {
            noteViewHolder.mContentView.setBackgroundColor(mColorUnread);
        } else {
            noteViewHolder.mContentView.setBackgroundColor(0);
        }

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1) {
            mOnLoadMoreListener.onLoadMore(note.getTimestamp());
        }
    }

    private int getPositionForNoteUnfiltered(String noteId) {
        return getPositionForNoteInArray(noteId, mNotes);
    }

    private int getPositionForNoteInArray(String noteId, ArrayList<Note> notes) {
        if (notes != null && noteId != null) {
            for (int i = 0; i < notes.size(); i++) {
                String noteKey = notes.get(i).getId();
                if (noteKey != null && noteKey.equals(noteId)) {
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public void setOnNoteClickListener(OnNoteClickListener mNoteClickListener) {
        mOnNoteClickListener = mNoteClickListener;
    }

    public void cancelReloadNotesTask() {
        if (mReloadNotesFromDBTask != null && mReloadNotesFromDBTask.getStatus() != Status.FINISHED) {
            mReloadNotesFromDBTask.cancel(true);
            mReloadNotesFromDBTask = null;
        }
    }

    public void reloadNotesFromDBAsync() {
        cancelReloadNotesTask();
        mReloadNotesFromDBTask = new ReloadNotesFromDBTask();
        mReloadNotesFromDBTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class ReloadNotesFromDBTask extends AsyncTask<Void, Void, ArrayList<Note>> {
        @Override
        protected ArrayList<Note> doInBackground(Void... voids) {
            return NotificationsTable.getLatestNotes();
        }

        @Override
        protected void onPostExecute(ArrayList<Note> notes) {
            mNotes.clear();
            mNotes.addAll(notes);
            myNotifyDatasetChanged();
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final View mContentView;
        private final TextView mHeaderText;
        private final View mHeaderDivider;
        private final TextView mTxtSubject;
        private final TextView mTxtSubjectNoticon;
        private final TextView mTxtDetail;
        private final BadgedImageView mImgAvatar;

        NoteViewHolder(View view) {
            super(view);
            mContentView = view.findViewById(R.id.note_content_container);
            mHeaderText = view.findViewById(R.id.header_text);
            mHeaderDivider = view.findViewById(R.id.header_divider);
            mTxtSubject = view.findViewById(R.id.note_subject);
            mTxtSubjectNoticon = view.findViewById(R.id.note_subject_noticon);
            mTxtDetail = view.findViewById(R.id.note_detail);
            mImgAvatar = view.findViewById(R.id.note_avatar);

            mContentView.setOnClickListener(mOnClickListener);
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnNoteClickListener != null && v.getTag() instanceof String) {
                mOnNoteClickListener.onClickNote((String) v.getTag());
            }
        }
    };
}
