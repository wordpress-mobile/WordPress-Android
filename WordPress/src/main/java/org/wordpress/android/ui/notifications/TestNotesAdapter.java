package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.Query;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.NoticonTextView;
import org.wordpress.android.widgets.WPNetworkImageView;

class TestNotesAdapter extends CursorAdapter {

    final private int mAvatarSz;
    private final Query mQuery;
    private int mSelectedPosition = ListView.INVALID_POSITION;
    private boolean mShouldHighlightRows;
    private int mReadBackgroundResId;
    private int mUnreadBackgroundResId;

    TestNotesAdapter(Context context, Bucket<Note> bucket) {
        super(context, null, 0x0);

        // build a query that sorts by timestamp descending
        mQuery = bucket.query().order(Note.Schema.TIMESTAMP_INDEX, Query.SortType.DESCENDING);

        mAvatarSz = (int) context.getResources().getDimension(R.dimen.avatar_sz_large);
        mReadBackgroundResId = R.drawable.list_bg_selector;
        mUnreadBackgroundResId = R.drawable.list_unread_bg_selector;
    }

    public void closeCursor() {
        Cursor cursor = getCursor();
        if (cursor != null) {
            cursor.close();
        }
    }

    public void reloadNotes() {
        changeCursor(mQuery.execute());
    }

    public Note getNote(int position) {
        getCursor().moveToPosition(position);
        return getNote();
    }

    private Note getNote() {
        return ((Bucket.ObjectCursor<Note>) getCursor()).getObject();
    }

    public void setSelectedPosition(int selectedPosition) {
        mSelectedPosition = selectedPosition;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.notifications_list_item, parent, false);
        NoteViewHolder holder = new NoteViewHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (cursor.isClosed())
            return;

        view.setActivated(mShouldHighlightRows && cursor.getPosition() == mSelectedPosition);

        Bucket.ObjectCursor<Note> bucketCursor = (Bucket.ObjectCursor<Note>) cursor;
        Note note = bucketCursor.getObject();

        NoteViewHolder noteViewHolder = (NoteViewHolder) view.getTag();

        // TODO This is probably bad for scrolling performance, optimize
        // Get the previous note to compare timestamps for header display
        Note previousNote = null;
        if (bucketCursor.moveToPrevious()) {
            previousNote = bucketCursor.getObject();
            bucketCursor.moveToNext();
        }

        if (previousNote != null && previousNote.getTimeGroup() == note.getTimeGroup()) {
            noteViewHolder.headerView.setVisibility(View.GONE);
        } else {
            if (note.getTimeGroup() == Note.NoteTimeGroup.GROUP_TODAY) {
                noteViewHolder.headerText.setText(context.getString(R.string.stats_timeframe_today).toUpperCase());
            } else if (note.getTimeGroup() == Note.NoteTimeGroup.GROUP_YESTERDAY) {
                noteViewHolder.headerText.setText(context.getString(R.string.stats_timeframe_yesterday).toUpperCase());
            } else if (note.getTimeGroup() == Note.NoteTimeGroup.GROUP_LAST_WEEK) {
                noteViewHolder.headerText.setText(context.getString(R.string.last_week).toUpperCase());
            } else {
                noteViewHolder.headerText.setText(context.getString(R.string.older).toUpperCase());
            }

            noteViewHolder.headerView.setVisibility(View.VISIBLE);
        }

        noteViewHolder.txtLabel.setText(note.getFormattedSubject());
        if (note.isCommentType()) {
            noteViewHolder.txtLabel.setMaxLines(2);
            noteViewHolder.txtDetail.setText(note.getCommentPreview());
            noteViewHolder.txtDetail.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtLabel.setMaxLines(3);
            noteViewHolder.txtDetail.setVisibility(View.GONE);
        }

        String avatarUrl = PhotonUtils.fixAvatar(note.getIconURL(), mAvatarSz);
        noteViewHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

        if (!TextUtils.isEmpty(note.getNoticonCharacter())) {
            noteViewHolder.noteIcon.setText(note.getNoticonCharacter());
            if (note.isUnread()) {
                noteViewHolder.noteIcon.setBackgroundResource(R.drawable.shape_oval_blue);
            } else {
                noteViewHolder.noteIcon.setBackgroundResource(R.drawable.shape_oval_grey);
            }
            noteViewHolder.noteIcon.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.noteIcon.setVisibility(View.GONE);
        }

        if (note.isUnread()) {
            view.setBackgroundResource(mUnreadBackgroundResId);
        } else {
            view.setBackgroundResource(mReadBackgroundResId);
        }
    }

    public void setShouldHighlightRows(boolean shouldHighlightRows) {
        mShouldHighlightRows = shouldHighlightRows;
    }

    private static class NoteViewHolder {
        private final View headerView;
        private final TextView headerText;

        private final TextView txtLabel;
        private final TextView txtDetail;
        private final WPNetworkImageView imgAvatar;
        private final NoticonTextView noteIcon;

        NoteViewHolder(View view) {
            headerView = view.findViewById(R.id.time_header);
            headerText = (TextView)view.findViewById(R.id.header_date_text);
            txtLabel = (TextView) view.findViewById(R.id.note_label);
            txtDetail = (TextView) view.findViewById(R.id.note_detail);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.note_avatar);
            noteIcon = (NoticonTextView) view.findViewById(R.id.note_icon);
        }
    }
}
