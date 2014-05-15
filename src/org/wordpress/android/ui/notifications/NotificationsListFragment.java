package org.wordpress.android.ui.notifications;

import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.util.SimperiumUtils;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class NotificationsListFragment extends ListFragment implements Bucket.Listener<Note> {
    private PullToRefreshHelper mFauxPullToRefreshHelper;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initPullToRefreshHelper();
        mFauxPullToRefreshHelper.registerReceiver(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();

        registerReceiver();
        // start listening to bucket change events
        mBucket.addListener(this);
    }

    @Override
    public void onPause() {
        // unregister the listener and close the cursor
        mBucket.removeListener(this);

        unregisterReceiver();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mNotesAdapter.closeCursor();

        mFauxPullToRefreshHelper.unregisterReceiver(getActivity());
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean isRefreshing = mFauxPullToRefreshHelper.isRefreshing();
        super.onConfigurationChanged(newConfig);
        // Pull to refresh layout is destroyed onDetachedFromWindow,
        // so we have to re-init the layout, via the helper here
        initPullToRefreshHelper();
        mFauxPullToRefreshHelper.setRefreshing(isRefreshing);
    }

    private void initPullToRefreshHelper() {
        mFauxPullToRefreshHelper = new PullToRefreshHelper(
                getActivity(),
                (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout),
                new PullToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        // Show a fake refresh animation for a few seconds
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (hasActivity()) {
                                    mFauxPullToRefreshHelper.setRefreshing(false);
                                }
                            }
                        }, 2000);
                    }
                }, LinearLayout.class
        );
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = mNotesAdapter.getNote(position);
        l.setItemChecked(position, true);
        if (note != null && mNoteClickListener != null) {
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
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        if (!hasActivity())
            return;

        try {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
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
            }
        }
    };
}