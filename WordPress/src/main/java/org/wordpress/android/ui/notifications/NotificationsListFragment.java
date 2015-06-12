package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

public class NotificationsListFragment extends Fragment
        implements Bucket.Listener<Note>,
                   WPMainActivity.OnScrollToTopListener {
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";

    private static final String KEY_LIST_SCROLL_POSITION = "scrollPosition";

    private NotesAdapter mNotesAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;
    private ViewGroup mEmptyView;

    private int mRestoredScrollPosition;

    private Bucket<Note> mBucket;

    public static NotificationsListFragment newInstance() {
        return new NotificationsListFragment();
    }

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(String noteId);
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_fragment_notes_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_notes);
        mEmptyView = (ViewGroup) view.findViewById(R.id.empty_view);

        RecyclerView.ItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(true);
        mRecyclerView.setItemAnimator(animator);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        // setup the initial notes adapter, starts listening to the bucket
        mBucket = SimperiumUtils.getNotesBucket();
        if (mBucket != null) {
            if (mNotesAdapter == null) {
                mNotesAdapter = new NotesAdapter(getActivity(), mBucket);
                mNotesAdapter.setOnNoteClickListener(new OnNoteClickListener() {
                    @Override
                    public void onClickNote(String noteId) {
                        if (TextUtils.isEmpty(noteId)) return;

                        // open the latest version of this note just in case it has changed - this can
                        // happen if the note was tapped from the list fragment after it was updated
                        // by another fragment (such as NotificationCommentLikeFragment)
                        openNote(getActivity(), noteId, false, true);
                    }
                });
            }

            mRecyclerView.setAdapter(mNotesAdapter);
        } else {
            if (!AccountHelper.isSignedInWordPressDotCom()) {
                // let user know that notifications require a wp.com account and enable sign-in
                showEmptyView(R.string.notifications_account_required, true);
            } else {
                // failed for some other reason
                showEmptyView(R.string.error_refresh_notifications, false);
            }
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setRestoredListPosition(savedInstanceState.getInt(KEY_LIST_SCROLL_POSITION, RecyclerView.NO_POSITION));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();

        // start listening to bucket change events
        if (mBucket != null) {
            mBucket.addListener(this);
        }

        // Remove notification if it is showing when we resume this activity.
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(GCMIntentService.NOTIFICATION_SERVICE);
        notificationManager.cancel(GCMIntentService.PUSH_NOTIFICATION_ID);

        if (SimperiumUtils.isUserAuthorized()) {
            SimperiumUtils.startBuckets();
            AppLog.i(AppLog.T.NOTIFS, "Starting Simperium buckets");
        }
    }

    @Override
    public void onPause() {
        // unregister the listener
        if (mBucket != null) {
            mBucket.removeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Close Simperium cursor
        if (mNotesAdapter != null) {
            mNotesAdapter.closeCursor();
        }

        super.onDestroy();
    }

    /**
     * Open a note fragment based on the type of note
     */
    public static void openNote(Activity activity,
                                String noteId,
                                boolean shouldShowKeyboard,
                                boolean shouldSlideIn) {
        if (noteId == null || activity == null) {
            return;
        }

        Intent detailIntent = new Intent(activity, NotificationsDetailActivity.class);
        detailIntent.putExtra(NOTE_ID_EXTRA, noteId);
        detailIntent.putExtra(NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard);
        if (shouldSlideIn) {
            ActivityLauncher.slideInFromRightForResult(activity, detailIntent, RequestCodes.NOTE_DETAIL);
        } else {
            activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL);
        }
    }

    private void setNoteIsHidden(String noteId, boolean isHidden) {
        if (mNotesAdapter == null) return;

        if (isHidden) {
            mNotesAdapter.addHiddenNoteId(noteId);
        } else {
            // Scroll the row into view if it isn't visible so the animation can be seen
            int notePosition = mNotesAdapter.getPositionForNote(noteId);
            if (notePosition != RecyclerView.NO_POSITION &&
                    mLinearLayoutManager.findFirstCompletelyVisibleItemPosition() > notePosition) {
                mLinearLayoutManager.scrollToPosition(notePosition);
            }

            mNotesAdapter.removeHiddenNoteId(noteId);
        }
    }

    private void setNoteIsModerating(String noteId, boolean isModerating) {
        if (mNotesAdapter == null) return;

        if (isModerating) {
            mNotesAdapter.addModeratingNoteId(noteId);
        } else {
            mNotesAdapter.removeModeratingNoteId(noteId);
        }
    }

    public void updateLastSeenTime() {
        // set the timestamp to now
        try {
            if (mNotesAdapter != null && mNotesAdapter.getCount() > 0 && SimperiumUtils.getMetaBucket() != null) {
                Note newestNote = mNotesAdapter.getNote(0);
                BucketObject meta = SimperiumUtils.getMetaBucket().get("meta");
                if (meta != null && newestNote != null) {
                    meta.setProperty("last_seen", newestNote.getTimestamp());
                    meta.save();
                }
            }
        } catch (BucketObjectMissingException e) {
            // try again later, meta is created by wordpress.com
        }
    }

    private void showEmptyView(@StringRes int stringResId, boolean showSignIn) {
        if (isAdded() && mEmptyView != null) {
            ((TextView) mEmptyView.findViewById(R.id.text_empty)).setText(stringResId);
            mEmptyView.setVisibility(View.VISIBLE);
            Button btnSignIn = (Button) mEmptyView.findViewById(R.id.button_sign_in);
            btnSignIn.setVisibility(showSignIn ? View.VISIBLE : View.GONE);
            if (showSignIn) {
                btnSignIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityLauncher.showSignInForResult(getActivity());
                    }
                });
            }
        }
    }

    private void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    void refreshNotes() {
        if (!isAdded() || mNotesAdapter == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNotesAdapter.reloadNotes();
                restoreListScrollPosition();
                if (mNotesAdapter.getCount() > 0) {
                    hideEmptyView();
                } else {
                    showEmptyView(R.string.notifications_empty_list, false);
                }
            }
        });
    }

    private void restoreListScrollPosition() {
        if (isAdded() && mRecyclerView != null && mRestoredScrollPosition != RecyclerView.NO_POSITION
                && mRestoredScrollPosition < mNotesAdapter.getCount()) {
            // Restore scroll position in list
            mLinearLayoutManager.scrollToPosition(mRestoredScrollPosition);
            mRestoredScrollPosition = RecyclerView.NO_POSITION;
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }

        // Save list view scroll position
        outState.putInt(KEY_LIST_SCROLL_POSITION, getScrollPosition());

        super.onSaveInstanceState(outState);
    }

    private int getScrollPosition() {
        if (!isAdded() || mRecyclerView == null) {
            return RecyclerView.NO_POSITION;
        }

        return mLinearLayoutManager.findFirstVisibleItemPosition();
    }

    private void setRestoredListPosition(int listPosition) {
        mRestoredScrollPosition = listPosition;
    }

    /**
     * Simperium bucket listener methods
     */
    @Override
    public void onSaveObject(Bucket<Note> bucket, final Note object) {
        refreshNotes();
    }

    @Override
    public void onDeleteObject(Bucket<Note> bucket, final Note object) {
        refreshNotes();
    }

    @Override
    public void onNetworkChange(Bucket<Note> bucket, final Bucket.ChangeType type, final String key) {
        // Reset the note's local status when a remote change is received
        if (type == Bucket.ChangeType.MODIFY) {
            try {
                Note note = bucket.get(key);
                if (note.isCommentType()) {
                    note.setLocalStatus(null);
                    note.save();
                }
            } catch (BucketObjectMissingException e) {
                AppLog.e(AppLog.T.NOTIFS, "Could not create note after receiving change.");
            }
        }

        refreshNotes();
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        //noop
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getScrollPosition() > 0) {
            mLinearLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NoteModerationStatusChanged event) {
        setNoteIsModerating(event.mNoteId, event.mIsModerating);

        EventBus.getDefault().removeStickyEvent(NotificationEvents.NoteModerationStatusChanged.class);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NoteVisibilityChanged event) {
        setNoteIsHidden(event.mNoteId, event.mIsHidden);

        EventBus.getDefault().removeStickyEvent(NotificationEvents.NoteVisibilityChanged.class);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NoteModerationFailed event) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.error_moderate_comment, Duration.LONG);
        }

        EventBus.getDefault().removeStickyEvent(NotificationEvents.NoteModerationFailed.class);
    }
}
