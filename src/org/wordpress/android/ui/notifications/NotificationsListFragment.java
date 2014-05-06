package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.SimperiumUtils;

public class NotificationsListFragment extends ListFragment implements Bucket.Listener<Note> {
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    private boolean mShouldLoadFirstNote;

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
        mBucket = SimperiumUtils.getNotesBucket();

        mNotesAdapter = new NotesAdapter(getActivity(), mBucket);

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
        refreshNotes();

        registerReceiver();
        // start listening to bucket change events
        mBucket.addListener(this);

        // Show progress indicator if we haven't indexed the notes yet
        if (hasActivity() && !mBucket.hasChangeVersion()) {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void onPause() {
        // unregister the listener and close the cursor
        mBucket.removeListener(this);

        unregisterReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mNotesAdapter.closeCursor();

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

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    protected void updateLastSeenTime() {
        // set the timestamp to now
        try {
            if (mNotesAdapter != null && mNotesAdapter.getCount() > 0) {
                Note newestNote = mNotesAdapter.getNote(0);
                BucketObject meta = SimperiumUtils.getMetaBucket().get("meta");
                meta.setProperty("last_seen", newestNote.getTimestamp());
                meta.save();
            }
        } catch (BucketObjectMissingException e) {
            // try again later, meta is created by wordpress.com
        }
    }

    public void refreshNotes() {
        if (!hasActivity() || mNotesAdapter == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNotesAdapter.reloadNotes();
                updateLastSeenTime();

                // Show first note if we're on a landscape tablet
                if (mShouldLoadFirstNote && mNotesAdapter.getCount() > 0) {
                    mShouldLoadFirstNote = false;
                    Note note = mNotesAdapter.getNote(0);
                    if (note != null && mNoteClickListener != null) {
                        mNoteClickListener.onClickNote(note);
                        getListView().setItemChecked(0, true);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Simperium bucket listener methods
     */
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

        if (hasActivity() && type == Bucket.ChangeType.INDEX) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }
            });
        }

        refreshNotes();
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        //noop
    }

    public void setShouldLoadFirstNote(boolean shouldLoad) {
        mShouldLoadFirstNote = shouldLoad;
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }


    /**
     * Broadcast listener for simperium sign in
     */
    private void registerReceiver() {
        if (!hasActivity())
            return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN);
        filter.addAction(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED);
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
            if (intent == null || intent.getAction() == null || !hasActivity())
                return;
            if (intent.getAction().equals(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_SIGNED_IN)) {
                // Get the new bucket instance and start listening again
                mBucket.removeListener(NotificationsListFragment.this);
                mBucket = SimperiumUtils.getNotesBucket();
                mBucket.addListener(NotificationsListFragment.this);
            } else if (intent.getAction().equals(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED)) {
                getActivity().setProgressBarIndeterminateVisibility(false);

                // Simperium user is not authorized, log in again
                Intent notAuthorizedIntent = new Intent();
                notAuthorizedIntent.setAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS);
                getActivity().sendBroadcast(notAuthorizedIntent);
            }
        }
    };
}
