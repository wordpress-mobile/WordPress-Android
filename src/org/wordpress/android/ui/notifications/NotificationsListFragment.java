package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.HashMap;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

public class NotificationsListFragment extends ListFragment {
    private static final int LOAD_MORE_WITHIN_X_ROWS = 5;
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    private View mProgressFooterView;
    private boolean mAllNotesLoaded;
    private PullToRefreshHelper mPullToRefreshHelper;

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empty_listview, container, false);
        return v;
    }

    public void animateRefresh(boolean refresh) {
        mPullToRefreshHelper.setRefreshing(refresh);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // setup the initial notes adapter, starst listening to the bucket
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

        initPullToRefreshHelper();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean isRefreshing = mPullToRefreshHelper.isRefreshing();
        super.onConfigurationChanged(newConfig);
        // Pull to refresh layout is destroyed onDetachedFromWindow,
        // so we have to re-init the layout, via the helper here
        initPullToRefreshHelper();
        mPullToRefreshHelper.setRefreshing(isRefreshing);
    }

    private void initPullToRefreshHelper() {
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout),
                new PullToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        if (getActivity() instanceof NotificationsActivity) {
                            //((NotificationsActivity) getActivity()).refreshNotes();
                        }
                    }
                }, TextView.class);
    }

    @Override
    public void onDestroy() {
        // unregister the listener and close the cursor
        mNotesAdapter.stopListening();
        super.onDestroy();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = mNotesAdapter.getNote(position);
        l.setItemChecked(position, true);
        if (note != null && !note.isPlaceholder() && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        super.setListAdapter(adapter);
    }

    public void setNotesAdapter(NotesAdapter adapter) {
        mNotesAdapter = adapter;
        this.setListAdapter(adapter);
    }

    public NotesAdapter getNotesAdapter() {
        return mNotesAdapter;
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    protected void updateLastSeenTime(){
        // set the timestamp to now
        try {
            if (mNotesAdapter == null) return;
            Note newestNote = mNotesAdapter.getNote(0);
            BucketObject meta = WordPress.metaBucket.get("meta");
            meta.setProperty("last_seen", newestNote.getTimestamp());
            meta.save();
        } catch (BucketObjectMissingException e) {
            // try again later, meta is created by wordpress.com
        }
    }

    boolean hasAdapter() {
        return (mNotesAdapter != null);
    }

    class NotesAdapter extends ResourceCursorAdapter implements Bucket.Listener<Note> {

        int mAvatarSz;
        Query<Note> mQuery;
        Bucket<Note> mBucket;

        NotesAdapter(Bucket<Note> bucket) {
            super(getActivity(), R.layout.note_list_item, null, 0x0);

            mBucket = bucket;

            // start listening to bucket change events
            mBucket.addListener(this);

            // build a query that sorts by timestamp descending
            mQuery = bucket.query().order(Note.Schema.TIMESTAMP_INDEX,
                    Query.SortType.DESCENDING);

            mAvatarSz = DisplayUtils.dpToPx(getActivity(), 48);
            refreshNotes();
        }

        public void stopListening() {
            mBucket.removeListener(this);
            Cursor cursor = getCursor();
            if (cursor != null) {
                cursor.close();
            }
        }

        @Override
        public void onSaveObject(Bucket<Note> bucket, Note object) {
            refreshNotes();
        }

        @Override
        public void onDeleteObject(Bucket<Note> bucket, Note object) {
            refreshNotes();
        }

        @Override
        public void onChange(Bucket<Note> bucket, Bucket.ChangeType type, String key) {
            refreshNotes();
        }

        public void refreshNotes() {
            Activity activity = getActivity();
            if (activity == null) return;

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run(){
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
        public void bindView(View view, Context context, Cursor cursor) {

            Bucket.ObjectCursor<Note> bucketCursor = (Bucket.ObjectCursor<Note>) cursor;
            final Note note = bucketCursor.getObject();

            final TextView txtLabel = (TextView) view.findViewById(R.id.note_label);
            final TextView txtDetail = (TextView) view.findViewById(R.id.note_detail);
            final TextView unreadIndicator = (TextView) view.findViewById(R.id.unread_indicator);
            final TextView txtDate = (TextView) view.findViewById(R.id.text_date);
            final ProgressBar placeholderLoading = (ProgressBar) view.findViewById(R.id.placeholder_loading);
            final NetworkImageView imgAvatar = (NetworkImageView) view.findViewById(R.id.note_avatar);
            final ImageView imgNoteIcon = (ImageView) view.findViewById(R.id.note_icon);

            txtLabel.setText(note.getSubject());
            if (note.isCommentType()) {
                txtDetail.setText(note.getCommentPreview());
                txtDetail.setVisibility(View.VISIBLE);
            } else {
                txtDetail.setVisibility(View.GONE);
            }

            txtDate.setText(note.getTimeSpan());

            // gravatars default to having s=256 which is considerably larger than we need here, so
            // change the s= param to the actual size used here
            String avatarUrl = note.getIconURL();
            if (avatarUrl!=null && avatarUrl.contains("s=256"))
                avatarUrl = avatarUrl.replace("s=256", "s=" + mAvatarSz);
            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
            imgAvatar.setImageUrl(avatarUrl, WordPress.imageLoader);

            imgNoteIcon.setImageDrawable(getDrawableForType(note.getType()));

            unreadIndicator.setVisibility(note.isUnread() ? View.VISIBLE : View.INVISIBLE);
            placeholderLoading.setVisibility(note.isPlaceholder() ? View.VISIBLE : View.GONE);

        }


        // HashMap of drawables for note types
        private HashMap<String, Drawable> mNoteIcons = new HashMap<String, Drawable>();
        private Drawable getDrawableForType(String noteType) {
            if (noteType==null)
                return null;

            // use like icon for comment likes
            if (noteType.equals(Note.NOTE_COMMENT_LIKE_TYPE))
                noteType = Note.NOTE_LIKE_TYPE;

            Drawable icon = mNoteIcons.get(noteType);
            if (icon != null)
                return icon;

            int imageId = getResources().getIdentifier("note_icon_" + noteType, "drawable", getActivity().getPackageName());
            if (imageId==0) {
                Log.w(WordPress.TAG, "unknown note type - " + noteType);
                return null;
            }

            icon = getResources().getDrawable(imageId);
            if (icon==null)
                return null;

            mNoteIcons.put(noteType, icon);
            return icon;
        }

        @Override
        public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
            //noop
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}