package org.wordpress.android.ui.notifications.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.Query;

import org.wordpress.android.R;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.NoticonTextView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends CursorRecyclerViewAdapter<NotesAdapter.NoteViewHolder> {

    private final int mAvatarSz;
    private final Bucket<Note> mNotesBucket;
    private final int mColorRead;
    private final int mColorUnread;
    private final int mTextIndentSize;
    private final List<String> mHiddenNoteIds = new ArrayList<>();
    private final List<String> mModeratingNoteIds = new ArrayList<>();

    private Query mQuery;

    private NotificationsListFragment.OnNoteClickListener mOnNoteClickListener;

    public NotesAdapter(Context context, Bucket<Note> bucket) {
        super(context, null);

        setHasStableIds(true);

        mNotesBucket = bucket;
        // build a query that sorts by timestamp descending
        mQuery = new Query();

        mAvatarSz = (int) context.getResources().getDimension(R.dimen.notifications_avatar_sz);
        mColorRead = context.getResources().getColor(R.color.white);
        mColorUnread = context.getResources().getColor(R.color.grey_light);
        mTextIndentSize = context.getResources().getDimensionPixelSize(R.dimen.notifications_text_indent_sz);
    }

    public void closeCursor() {
        Cursor cursor = getCursor();
        if (cursor != null) {
            cursor.close();
        }
    }

    private Query getQueryDefaults() {
        return mNotesBucket.query()
                .include(
                        Note.Schema.TIMESTAMP_INDEX,
                        Note.Schema.SUBJECT_INDEX,
                        Note.Schema.SNIPPET_INDEX,
                        Note.Schema.UNREAD_INDEX,
                        Note.Schema.ICON_URL_INDEX,
                        Note.Schema.NOTICON_INDEX,
                        Note.Schema.IS_UNAPPROVED_INDEX,
                        Note.Schema.COMMENT_SUBJECT_NOTICON,
                        Note.Schema.LOCAL_STATUS)
                .order(Note.Schema.TIMESTAMP_INDEX, Query.SortType.DESCENDING);
    }

    public void queryNotes() {
        mQuery = getQueryDefaults();
        changeCursor(mQuery.execute());
    }

    public void queryNotes(String columnName, Object value) {
        mQuery = getQueryDefaults();
        mQuery.where(columnName, Query.ComparisonType.EQUAL_TO, value);
        changeCursor(mQuery.execute());
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

    private String getStringForColumnName(Cursor cursor, String columnName) {
        if (columnName == null || cursor == null || cursor.getColumnIndex(columnName) == -1) {
            return "";
        }

        return StringUtils.notNullStr(cursor.getString(cursor.getColumnIndex(columnName)));
    }

    private int getIntForColumnName(Cursor cursor, String columnName) {
        if (columnName == null || cursor == null || cursor.getColumnIndex(columnName) == -1) {
            return -1;
        }

        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    private long getLongForColumnName(Cursor cursor, String columnName) {
        if (columnName == null || cursor == null || cursor.getColumnIndex(columnName) == -1) {
            return -1;
        }

        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public int getCount() {
        if (getCursor() != null) {
            return getCursor().getCount();
        }

        return 0;
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notifications_list_item, parent, false);

        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder noteViewHolder, Cursor cursor) {
        final Bucket.ObjectCursor<Note> objectCursor = (Bucket.ObjectCursor<Note>) cursor;
        noteViewHolder.itemView.setTag(objectCursor.getSimperiumKey());

        // Display group header
        Note.NoteTimeGroup timeGroup = Note.getTimeGroupForTimestamp(getLongForColumnName(objectCursor, Note.Schema.TIMESTAMP_INDEX));

        Note.NoteTimeGroup previousTimeGroup = null;
        if (objectCursor.getPosition() > 0 && objectCursor.moveToPrevious()) {
            previousTimeGroup = Note.getTimeGroupForTimestamp(getLongForColumnName(objectCursor, Note.Schema.TIMESTAMP_INDEX));
            objectCursor.moveToNext();
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

        if (mHiddenNoteIds.size() > 0 && mHiddenNoteIds.contains(objectCursor.getSimperiumKey())) {
            noteViewHolder.contentView.setVisibility(View.GONE);
            noteViewHolder.headerView.setVisibility(View.GONE);
        } else {
            noteViewHolder.contentView.setVisibility(View.VISIBLE);
        }

        CommentStatus commentStatus = CommentStatus.UNKNOWN;
        if (SqlUtils.sqlToBool(getIntForColumnName(objectCursor, Note.Schema.IS_UNAPPROVED_INDEX))) {
            commentStatus = CommentStatus.UNAPPROVED;
        }

        String localStatus = getStringForColumnName(objectCursor, Note.Schema.LOCAL_STATUS);
        if (!TextUtils.isEmpty(localStatus)) {
            commentStatus = CommentStatus.fromString(localStatus);
        }

        if (mModeratingNoteIds.size() > 0 && mModeratingNoteIds.contains(objectCursor.getSimperiumKey())) {
            noteViewHolder.progressBar.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.progressBar.setVisibility(View.GONE);
        }

        // Subject is stored in db as html to preserve text formatting
        String noteSubjectHtml = getStringForColumnName(objectCursor, Note.Schema.SUBJECT_INDEX).trim();
        CharSequence noteSubjectSpanned = Html.fromHtml(noteSubjectHtml);
        // Trim the '\n\n' added by Html.fromHtml()
        noteSubjectSpanned = noteSubjectSpanned.subSequence(0, TextUtils.getTrimmedLength(noteSubjectSpanned));
        noteViewHolder.txtSubject.setText(noteSubjectSpanned);

        String noteSubjectNoticon = getStringForColumnName(objectCursor, Note.Schema.COMMENT_SUBJECT_NOTICON);
        if (!TextUtils.isEmpty(noteSubjectNoticon)) {
            CommentUtils.indentTextViewFirstLine(noteViewHolder.txtSubject, mTextIndentSize);
            noteViewHolder.txtSubjectNoticon.setText(noteSubjectNoticon);
            noteViewHolder.txtSubjectNoticon.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtSubjectNoticon.setVisibility(View.GONE);
        }

        String noteSnippet = getStringForColumnName(objectCursor, Note.Schema.SNIPPET_INDEX);
        if (!TextUtils.isEmpty(noteSnippet)) {
            noteViewHolder.txtSubject.setMaxLines(2);
            noteViewHolder.txtDetail.setText(noteSnippet);
            noteViewHolder.txtDetail.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtSubject.setMaxLines(3);
            noteViewHolder.txtDetail.setVisibility(View.GONE);
        }

        String avatarUrl = GravatarUtils.fixGravatarUrl(getStringForColumnName(objectCursor, Note.Schema.ICON_URL_INDEX), mAvatarSz);
        noteViewHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

        boolean isUnread = SqlUtils.sqlToBool(getIntForColumnName(objectCursor, Note.Schema.UNREAD_INDEX));

        String noticonCharacter = getStringForColumnName(objectCursor, Note.Schema.NOTICON_INDEX);
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
        Bucket.ObjectCursor<Note> cursor = (Bucket.ObjectCursor<Note>) getCursor();
        if (cursor != null) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String noteKey = cursor.getSimperiumKey();
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

        public NoteViewHolder(View view) {
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
