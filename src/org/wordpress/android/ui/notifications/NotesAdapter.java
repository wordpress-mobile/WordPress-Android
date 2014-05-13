package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.Query;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.HashMap;

class NotesAdapter extends CursorAdapter {

    int mAvatarSz;
    Query mQuery;
    Context mContext;

    NotesAdapter(Context context, Bucket<Note> bucket) {
        super(context, null, 0x0);

        mContext = context;
        // build a query that sorts by timestamp descending
        mQuery = bucket.query().order(Note.Schema.TIMESTAMP_INDEX, Query.SortType.DESCENDING);

        mAvatarSz = DisplayUtils.dpToPx(context, 48);
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

    public Note getNote() {
        return ((Bucket.ObjectCursor<Note>) getCursor()).getObject();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
        NoteViewHolder holder = new NoteViewHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (cursor.isClosed())
            return;

        Bucket.ObjectCursor<Note> bucketCursor = (Bucket.ObjectCursor<Note>) cursor;
        Note note = bucketCursor.getObject();

        NoteViewHolder noteViewHolder = (NoteViewHolder) view.getTag();

        noteViewHolder.txtLabel.setText(note.getSubject());
        if (note.isCommentType()) {
            noteViewHolder.txtDetail.setText(note.getCommentPreview());
            noteViewHolder.txtDetail.setVisibility(View.VISIBLE);
        } else {
            noteViewHolder.txtDetail.setVisibility(View.GONE);
        }

        noteViewHolder.txtDate.setText(note.getTimeSpan());

        String avatarUrl = PhotonUtils.fixAvatar(note.getIconURL(), mAvatarSz);
        noteViewHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);

        noteViewHolder.imgNoteIcon.setImageDrawable(getDrawableForType(note.getType()));

        noteViewHolder.unreadIndicator.setVisibility(note.isUnread() ? View.VISIBLE : View.INVISIBLE);
        noteViewHolder.placeholderLoading.setVisibility(note.isPlaceholder() ? View.VISIBLE : View.GONE);
    }

    // HashMap of drawables for note types
    private HashMap<String, Drawable> mNoteIcons = new HashMap<String, Drawable>();

    private Drawable getDrawableForType(String noteType) {
        if (mContext == null || noteType == null)
            return null;

        // use like icon for comment likes
        if (noteType.equals(Note.NOTE_COMMENT_LIKE_TYPE))
            noteType = Note.NOTE_LIKE_TYPE;

        Drawable icon = mNoteIcons.get(noteType);
        if (icon != null)
            return icon;

        int imageId = mContext.getResources().getIdentifier("note_icon_" + noteType, "drawable", mContext.getPackageName());
        if (imageId == 0) {
            Log.w(AppLog.TAG, "unknown note type - " + noteType);
            return null;
        }

        icon = mContext.getResources().getDrawable(imageId);
        if (icon == null)
            return null;

        mNoteIcons.put(noteType, icon);
        return icon;
    }

    private static class NoteViewHolder {
        private final TextView txtLabel;
        private final TextView txtDetail;
        private final TextView unreadIndicator;
        private final TextView txtDate;
        private final ProgressBar placeholderLoading;
        private final WPNetworkImageView imgAvatar;
        private final ImageView imgNoteIcon;

        NoteViewHolder(View view) {
            txtLabel = (TextView) view.findViewById(R.id.note_label);
            txtDetail = (TextView) view.findViewById(R.id.note_detail);
            unreadIndicator = (TextView) view.findViewById(R.id.unread_indicator);
            txtDate = (TextView) view.findViewById(R.id.text_date);
            placeholderLoading = (ProgressBar) view.findViewById(R.id.placeholder_loading);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.note_avatar);
            imgNoteIcon = (ImageView) view.findViewById(R.id.note_icon);
        }
    }
}
