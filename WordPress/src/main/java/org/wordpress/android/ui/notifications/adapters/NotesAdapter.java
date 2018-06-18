package org.wordpress.android.ui.notifications.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.text.BidiFormatter;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.RtlUtils;
import org.wordpress.android.widgets.NoticonTextView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private final int mAvatarSz;
    private final int mColorRead;
    private final int mColorUnread;
    private final int mTextIndentSize;

    private final DataLoadedListener mDataLoadedListener;
    private final OnLoadMoreListener mOnLoadMoreListener;
    private final ArrayList<Note> mNotes = new ArrayList<>();
    private final ArrayList<Note> mFilteredNotes = new ArrayList<>();

    public enum FILTERS {
        FILTER_ALL, FILTER_LIKE, FILTER_COMMENT, FILTER_UNREAD,
        FILTER_FOLLOW
    }

    private FILTERS mCurrentFilter = FILTERS.FILTER_ALL;

    public interface DataLoadedListener {
        void onDataLoaded(int itemsCount);
    }

    public interface OnLoadMoreListener {
        void onLoadMore(long timestamp);
    }

    private NotificationsListFragment.OnNoteClickListener mOnNoteClickListener;

    public NotesAdapter(Context context, DataLoadedListener dataLoadedListener, OnLoadMoreListener onLoadMoreListener) {
        super();

        mDataLoadedListener = dataLoadedListener;
        mOnLoadMoreListener = onLoadMoreListener;

        setHasStableIds(true);

        mAvatarSz = (int) context.getResources().getDimension(R.dimen.notifications_avatar_sz);
        mColorRead = context.getResources().getColor(R.color.white);
        mColorUnread = context.getResources().getColor(R.color.grey_light);
        mTextIndentSize = context.getResources().getDimensionPixelSize(R.dimen.notifications_text_indent_sz);
    }

    public void setFilter(FILTERS newFilter) {
        mCurrentFilter = newFilter;
        myNotifyDatasetChanged();
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
    public long getItemId(int position) {
        Note note = getNoteAtPosition(position);
        if (note == null) {
            return 0;
        }

        return Long.valueOf(note.getId());
    }

    @Override
    public void onBindViewHolder(NoteViewHolder noteViewHolder, int position) {
        final Note note = getNoteAtPosition(position);
        if (note == null) {
            return;
        }
        noteViewHolder.itemView.setTag(note.getId());

        // Display group header
        Note.NoteTimeGroup timeGroup = Note.getTimeGroupForTimestamp(note.getTimestamp());

        Note.NoteTimeGroup previousTimeGroup = null;
        if (position > 0) {
            Note previousNote = getNoteAtPosition(position - 1);
            previousTimeGroup = Note.getTimeGroupForTimestamp(previousNote.getTimestamp());
        }

        if (previousTimeGroup != null && previousTimeGroup == timeGroup) {
            noteViewHolder.mHeaderView.setVisibility(View.GONE);
        } else {
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

            noteViewHolder.mHeaderView.setVisibility(View.VISIBLE);
        }

        CommentStatus commentStatus = CommentStatus.ALL;
        if (note.getCommentStatus() == CommentStatus.UNAPPROVED) {
            commentStatus = CommentStatus.UNAPPROVED;
        }

        if (!TextUtils.isEmpty(note.getLocalStatus())) {
            commentStatus = CommentStatus.fromString(note.getLocalStatus());
        }

        // Subject is stored in db as html to preserve text formatting
        CharSequence noteSubjectSpanned = note.getFormattedSubject();
        // Trim the '\n\n' added by Html.fromHtml()
        noteSubjectSpanned = noteSubjectSpanned.subSequence(0, TextUtils.getTrimmedLength(noteSubjectSpanned));
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
        noteViewHolder.mImgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

        boolean isUnread = note.isUnread();

        String noticonCharacter = note.getNoticonCharacter();
        noteViewHolder.mNoteIcon.setText(noticonCharacter);
        if (commentStatus == CommentStatus.UNAPPROVED) {
            noteViewHolder.mNoteIcon.setBackgroundResource(R.drawable.shape_oval_orange);
        } else if (isUnread) {
            noteViewHolder.mNoteIcon.setBackgroundResource(R.drawable.shape_oval_blue_white_stroke);
        } else {
            noteViewHolder.mNoteIcon.setBackgroundResource(R.drawable.shape_oval_grey_lighten_10);
        }

        if (isUnread) {
            noteViewHolder.itemView.setBackgroundColor(mColorUnread);
        } else {
            noteViewHolder.itemView.setBackgroundColor(mColorRead);
        }

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1) {
            mOnLoadMoreListener.onLoadMore(note.getTimestamp());
        }
    }

    public int getPositionForNote(String noteId) {
        return getPositionForNoteInArray(noteId, mFilteredNotes);
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

    public void setOnNoteClickListener(NotificationsListFragment.OnNoteClickListener mNoteClickListener) {
        mOnNoteClickListener = mNoteClickListener;
    }

    public void reloadNotesFromDBAsync() {
        new ReloadNotesFromDBTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        private final View mHeaderView;
        private final View mContentView;
        private final TextView mHeaderText;

        private final TextView mTxtSubject;
        private final TextView mTxtSubjectNoticon;
        private final TextView mTxtDetail;
        private final WPNetworkImageView mImgAvatar;
        private final NoticonTextView mNoteIcon;

        NoteViewHolder(View view) {
            super(view);
            mHeaderView = view.findViewById(R.id.time_header);
            mContentView = view.findViewById(R.id.note_content_container);
            mHeaderText = (TextView) view.findViewById(R.id.header_date_text);
            mTxtSubject = (TextView) view.findViewById(R.id.note_subject);
            mTxtSubjectNoticon = (TextView) view.findViewById(R.id.note_subject_noticon);
            mTxtDetail = (TextView) view.findViewById(R.id.note_detail);
            mImgAvatar = (WPNetworkImageView) view.findViewById(R.id.note_avatar);
            mNoteIcon = (NoticonTextView) view.findViewById(R.id.note_icon);

            itemView.setOnClickListener(mOnClickListener);
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
