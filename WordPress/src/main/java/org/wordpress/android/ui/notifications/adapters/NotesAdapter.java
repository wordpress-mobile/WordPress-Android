package org.wordpress.android.ui.notifications.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.GravatarUtils;
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
    private final List<String> mHiddenNoteIds = new ArrayList<>();
    private final List<String> mModeratingNoteIds = new ArrayList<>();
    private boolean mIsAddingNotes;

    private final DataLoadedListener mDataLoadedListener;
    private final ArrayList<Note> mNotes = new ArrayList<>();

    public interface DataLoadedListener {
        void onDataLoaded(boolean isEmpty);
    }

    private NotificationsListFragment.OnNoteClickListener mOnNoteClickListener;

    public NotesAdapter(Context context, DataLoadedListener dataLoadedListener) {
        super();

        mDataLoadedListener = dataLoadedListener;

        setHasStableIds(true);

        mAvatarSz = (int) context.getResources().getDimension(R.dimen.notifications_avatar_sz);
        mColorRead = context.getResources().getColor(R.color.white);
        mColorUnread = context.getResources().getColor(R.color.grey_light);
        mTextIndentSize = context.getResources().getDimensionPixelSize(R.dimen.notifications_text_indent_sz);
    }

    public void addHiddenNoteId(String noteId) {
        mHiddenNoteIds.add(noteId);
        notifyDataSetChanged();
    }

    public void removeHiddenNoteId(String noteId) {
        mHiddenNoteIds.remove(noteId);
        notifyDataSetChanged();
    }

    public void addModeratingNoteId(String noteId) {
        mModeratingNoteIds.add(noteId);
        notifyDataSetChanged();
    }

    public void removeModeratingNoteId(String noteId) {
        mModeratingNoteIds.remove(noteId);
        notifyDataSetChanged();
    }

    public void addAll(List<Note> notes, boolean clearBeforeAdding) {
        if (notes.size() > 0) {
            Collections.sort(notes, new Note.TimeStampComparator());
            mIsAddingNotes = true;
            try {
                if (clearBeforeAdding) {
                    mNotes.clear();
                }
                mNotes.addAll(notes);
            } finally {
                notifyDataSetChanged();
                mIsAddingNotes = false;
            }
        }

        if (mDataLoadedListener != null) {
            mDataLoadedListener.onDataLoaded(getItemCount() == 0);
        }
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notifications_list_item, parent, false);

        return new NoteViewHolder(view);
    }

    private Note getNoteAtPosition(int position) {
        if (isValidPosition(position))
            return mNotes.get(position);
        return null;
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mNotes.size());
    }

    @Override
    public int getItemCount() {
        return mNotes.size();
    }

    @Override
    public void onBindViewHolder(NoteViewHolder noteViewHolder, int position) {
        final Note note = getNoteAtPosition(position);
        noteViewHolder.itemView.setTag(note.getId());

        // Display group header
        Note.NoteTimeGroup timeGroup = Note.getTimeGroupForTimestamp(note.getTimestamp());

        Note.NoteTimeGroup previousTimeGroup = null;
        if (position > 0) {
            Note previousNote = getNoteAtPosition(position - 1);
            previousTimeGroup = Note.getTimeGroupForTimestamp(previousNote.getTimestamp());
        }

        if (previousTimeGroup != null && previousTimeGroup == timeGroup) {
            noteViewHolder.headerView.setVisibility(View.GONE);
        } else {
            if (timeGroup == Note.NoteTimeGroup.GROUP_TODAY) {
                noteViewHolder.headerText.setText(R.string.stats_timeframe_today);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_YESTERDAY) {
                noteViewHolder.headerText.setText(R.string.stats_timeframe_yesterday);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_OLDER_TWO_DAYS) {
                noteViewHolder.headerText.setText(R.string.older_two_days);
            } else if (timeGroup == Note.NoteTimeGroup.GROUP_OLDER_WEEK) {
                noteViewHolder.headerText.setText(R.string.older_last_week);
            } else {
                noteViewHolder.headerText.setText(R.string.older_month);
            }

            noteViewHolder.headerView.setVisibility(View.VISIBLE);
        }

        if (mHiddenNoteIds.size() > 0 && mHiddenNoteIds.contains(note.getId())) {
            noteViewHolder.contentView.setVisibility(View.GONE);
            noteViewHolder.headerView.setVisibility(View.GONE);
        } else {
            noteViewHolder.contentView.setVisibility(View.VISIBLE);
        }

        CommentStatus commentStatus = CommentStatus.UNKNOWN;
        if (note.getCommentStatus() == CommentStatus.UNAPPROVED) {
            commentStatus = CommentStatus.UNAPPROVED;
        }

        if (!TextUtils.isEmpty(note.getLocalStatus())) {
            commentStatus = CommentStatus.fromString(note.getLocalStatus());
        }

        if (mModeratingNoteIds.size() > 0 && mModeratingNoteIds.contains(note.getId())) {
            noteViewHolder.progressBar.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.progressBar.setVisibility(View.GONE);
        }

        // Subject is stored in db as html to preserve text formatting
        CharSequence noteSubjectSpanned = note.getFormattedSubject();
        // Trim the '\n\n' added by Html.fromHtml()
        noteSubjectSpanned = noteSubjectSpanned.subSequence(0, TextUtils.getTrimmedLength(noteSubjectSpanned));
        noteViewHolder.txtSubject.setText(noteSubjectSpanned);

        String noteSubjectNoticon = note.getCommentSubjectNoticon();
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            CommentUtils.indentTextViewFirstLine(noteViewHolder.txtSubject, mTextIndentSize);
            noteViewHolder.txtSubjectNoticon.setText(noteSubjectNoticon);
            noteViewHolder.txtSubjectNoticon.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtSubjectNoticon.setVisibility(View.GONE);
        }

        String noteSnippet = note.getCommentSubject();
        if (!TextUtils.isEmpty(noteSnippet)) {
            noteViewHolder.txtSubject.setMaxLines(2);
            noteViewHolder.txtDetail.setText(noteSnippet);
            noteViewHolder.txtDetail.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtSubject.setMaxLines(3);
            noteViewHolder.txtDetail.setVisibility(View.GONE);
        }

        String avatarUrl = GravatarUtils.fixGravatarUrl(note.getIconURL(), mAvatarSz);
        noteViewHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

        boolean isUnread = note.isUnread();

        String noticonCharacter = note.getNoticonCharacter();
        noteViewHolder.noteIcon.setText(noticonCharacter);
        if (commentStatus == CommentStatus.UNAPPROVED) {
            noteViewHolder.noteIcon.setBackgroundResource(R.drawable.shape_oval_orange);
        } else if (isUnread) {
            noteViewHolder.noteIcon.setBackgroundResource(R.drawable.shape_oval_blue_white_stroke);
        } else {
            noteViewHolder.noteIcon.setBackgroundResource(R.drawable.shape_oval_grey);
        }

        if (isUnread) {
            noteViewHolder.itemView.setBackgroundColor(mColorUnread);
        } else {
            noteViewHolder.itemView.setBackgroundColor(mColorRead);
        }
    }

    public int getPositionForNote(String noteId) {
        for (int i = 0; i < mNotes.size(); i++) {
            String noteKey = mNotes.get(i).getId();
            if (noteKey != null && noteKey.equals(noteId)) {
                return i;
            }
        }

        return RecyclerView.NO_POSITION;
    }

    public void setOnNoteClickListener(NotificationsListFragment.OnNoteClickListener mNoteClickListener) {
        mOnNoteClickListener = mNoteClickListener;
    }

    public void reloadNotes() {
        new ReloadNotesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class ReloadNotesTask extends AsyncTask<Void, Void, ArrayList<Note>> {

        @Override
        protected ArrayList<Note> doInBackground(Void... voids) {
            return NotificationsTable.getLatestNotes();
        }

        @Override
        protected void onPostExecute(ArrayList<Note> notes) {
            mNotes.clear();
            mNotes.addAll(notes);
            notifyDataSetChanged();

            mDataLoadedListener.onDataLoaded(getItemCount() == 0);
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final View headerView;
        private final View contentView;
        private final TextView headerText;

        private final TextView txtSubject;
        private final TextView txtSubjectNoticon;
        private final TextView txtDetail;
        private final WPNetworkImageView imgAvatar;
        private final NoticonTextView noteIcon;
        private final View progressBar;

        NoteViewHolder(View view) {
            super(view);
            headerView = view.findViewById(R.id.time_header);
            contentView = view.findViewById(R.id.note_content_container);
            headerText = (TextView)view.findViewById(R.id.header_date_text);
            txtSubject = (TextView) view.findViewById(R.id.note_subject);
            txtSubjectNoticon = (TextView) view.findViewById(R.id.note_subject_noticon);
            txtDetail = (TextView) view.findViewById(R.id.note_detail);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.note_avatar);
            noteIcon = (NoticonTextView) view.findViewById(R.id.note_icon);
            progressBar = view.findViewById(R.id.moderate_progress);

            itemView.setOnClickListener(mOnClickListener);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnNoteClickListener != null && v.getTag() instanceof String) {
                mOnNoteClickListener.onClickNote((String)v.getTag());
            }
        }
    };
}
