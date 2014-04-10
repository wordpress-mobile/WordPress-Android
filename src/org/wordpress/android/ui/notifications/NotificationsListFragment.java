package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.HashMap;

public class NotificationsListFragment extends ListFragment implements Bucket.Listener<Note> {
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;

    Bucket<Note> mBucket;

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empty_listview, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // setup the initial notes adapter, starts listening to the bucket
        mBucket = WordPress.notesBucket;

        mNotesAdapter = new NotesAdapter(WordPress.notesBucket);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
        listView.setDividerHeight(1);
        setListAdapter(mNotesAdapter);

        // Set empty text if no notifications
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.notifications_empty_list));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver();
        // start listening to bucket change events
        mBucket.addListener(this);
    }

    @Override
    public void onPause() {
        // unregister the listener and close the cursor
        mBucket.removeListener(this);
        mNotesAdapter.closeCursor();

        unregisterReceiver();
        super.onPause();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = mNotesAdapter.getNote(position);
        l.setItemChecked(position, true);
        if (note != null && !note.isPlaceholder() && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
        }
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    protected void updateLastSeenTime() {
        // set the timestamp to now
        try {
            if (mNotesAdapter != null && mNotesAdapter.getCount() > 0) {
                Note newestNote = mNotesAdapter.getNote(0);
                BucketObject meta = WordPress.metaBucket.get("meta");
                meta.setProperty("last_seen", newestNote.getTimestamp());
                meta.save();
            }
        } catch (BucketObjectMissingException e) {
            // try again later, meta is created by wordpress.com
        }
    }

    class NotesAdapter extends ResourceCursorAdapter {

        int mAvatarSz;
        Query<Note> mQuery;

        NotesAdapter(Bucket<Note> bucket) {
            super(getActivity(), R.layout.note_list_item, null, 0x0);

            // Show
            if (hasActivity() && !mBucket.hasChangeVersion()) {
                getActivity().setProgressBarIndeterminateVisibility(true);
            }

            // build a query that sorts by timestamp descending
            mQuery = bucket.query().order(Note.Schema.TIMESTAMP_INDEX,
                    Query.SortType.DESCENDING);

            mAvatarSz = DisplayUtils.dpToPx(getActivity(), 48);
            refreshNotes();
        }

        public void closeCursor() {
            Cursor cursor = getCursor();
            if (cursor != null) {
                cursor.close();
            }
        }

        public void refreshNotes() {
            Activity activity = getActivity();
            if (activity == null) return;

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    swapCursor(mQuery.execute());
                    updateLastSeenTime();
                }

            });

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
            View view = LayoutInflater.from(context).inflate(R.layout.note_list_item, null);
            NoteViewHolder holder = new NoteViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            Bucket.ObjectCursor<Note> bucketCursor = (Bucket.ObjectCursor<Note>) cursor;
            Note note = bucketCursor.getObject();

            NoteViewHolder noteViewHolder = (NoteViewHolder)view.getTag();

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
            if (noteType == null)
                return null;

            // use like icon for comment likes
            if (noteType.equals(Note.NOTE_COMMENT_LIKE_TYPE))
                noteType = Note.NOTE_LIKE_TYPE;

            Drawable icon = mNoteIcons.get(noteType);
            if (icon != null)
                return icon;

            int imageId = getResources().getIdentifier("note_icon_" + noteType, "drawable", getActivity().getPackageName());
            if (imageId == 0) {
                Log.w(WordPress.TAG, "unknown note type - " + noteType);
                return null;
            }

            icon = getResources().getDrawable(imageId);
            if (icon == null)
                return null;

            mNoteIcons.put(noteType, icon);
            return icon;
        }
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveObject(Bucket<Note> bucket, Note object) {
        if (mNotesAdapter != null) {
            mNotesAdapter.refreshNotes();
        }
    }

    @Override
    public void onDeleteObject(Bucket<Note> bucket, Note object) {
        if (mNotesAdapter != null) {
            mNotesAdapter.refreshNotes();
        }
    }

    @Override
    public void onChange(Bucket<Note> bucket, Bucket.ChangeType type, String key) {

        if (hasActivity() && type == Bucket.ChangeType.INDEX) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }
            });
        }

        if (mNotesAdapter != null) {
            mNotesAdapter.refreshNotes();
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        //noop
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WordPress.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN);
        getActivity().registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        if (!hasActivity())
            return;

        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // exception occurs if receiver already unregistered (safe to ignore)
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN)) {
                mBucket.removeListener(NotificationsListFragment.this);
                mBucket = WordPress.notesBucket;
                mBucket.addListener(NotificationsListFragment.this);
            }
        }
    };
}