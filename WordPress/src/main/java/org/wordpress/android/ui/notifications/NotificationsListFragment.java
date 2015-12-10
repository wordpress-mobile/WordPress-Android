package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
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

import de.greenrobot.event.EventBus;

public class NotificationsListFragment extends Fragment
        implements Bucket.Listener<Note>,
                   WPMainActivity.OnScrollToTopListener, RadioGroup.OnCheckedChangeListener {
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";

    private static final String KEY_LIST_SCROLL_POSITION = "scrollPosition";

    private NotesAdapter mNotesAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;
    private ViewGroup mEmptyView;
    private View mFilterView;
    private RadioGroup mFilterRadioGroup;
    private View mFilterDivider;

    private int mRestoredScrollPosition;

    private Bucket<Note> mBucket;

    public static NotificationsListFragment newInstance() {
        return new NotificationsListFragment();
    }

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        void onClickNote(String noteId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_fragment_notes_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_notes);

        mFilterRadioGroup = (RadioGroup)view.findViewById(R.id.notifications_radio_group);
        mFilterRadioGroup.setOnCheckedChangeListener(this);
        mFilterDivider = view.findViewById(R.id.notifications_filter_divider);
        mEmptyView = (ViewGroup) view.findViewById(R.id.empty_view);
        mFilterView = view.findViewById(R.id.notifications_filter);

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

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

        configureBucketAndAdapter();
        refreshNotes();

        // start listening to bucket change events
        if (mBucket != null) {
            mBucket.addListener(this);
        }

        // Removes app notifications from the system bar
        new Thread(new Runnable() {
            public void run() {
                GCMMessageService.removeAllNotifications(getActivity());
            }
        }).start();

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

    // Sets up the notes bucket and list adapter
    private void configureBucketAndAdapter() {
        mBucket = SimperiumUtils.getNotesBucket();
        if (mBucket != null) {
            if (mNotesAdapter == null) {
                mNotesAdapter = new NotesAdapter(getActivity(), mBucket);
                mNotesAdapter.setOnNoteClickListener(mOnNoteClickListener);
            }

            if (mRecyclerView.getAdapter() == null) {
                mRecyclerView.setAdapter(mNotesAdapter);
            }
        } else {
            if (!AccountHelper.isSignedInWordPressDotCom()) {
                // let user know that notifications require a wp.com account and enable sign-in
                showEmptyView(R.string.notifications_account_required, 0, R.string.sign_in);
                mFilterRadioGroup.setVisibility(View.GONE);
            } else {
                // failed for some other reason
                showEmptyView(R.string.error_refresh_notifications);
            }
        }
    }

    private final OnNoteClickListener mOnNoteClickListener = new OnNoteClickListener() {
        @Override
        public void onClickNote(String noteId) {
            if (!isAdded()) {
                return;
            }

            if (TextUtils.isEmpty(noteId)) return;

            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            openNote(getActivity(), noteId, false, true);
        }
    };

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

        if (activity.isFinishing()) {
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

    private void showEmptyView(@StringRes int titleResId) {
        showEmptyView(titleResId, 0, 0);
    }

    private void showEmptyView(@StringRes int titleResId, @StringRes int descriptionResId, @StringRes int buttonResId) {
        if (isAdded() && mEmptyView != null) {
            mEmptyView.setVisibility(View.VISIBLE);
            mFilterDivider.setVisibility(View.GONE);
            setFilterViewScrollable(false);
            ((TextView) mEmptyView.findViewById(R.id.text_empty)).setText(titleResId);

            TextView descriptionTextView = (TextView) mEmptyView.findViewById(R.id.text_empty_description);
            if (descriptionResId > 0) {
                descriptionTextView.setText(descriptionResId);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }

            TextView btnAction = (TextView)mEmptyView.findViewById(R.id.button_empty_action);
            if (buttonResId > 0) {
                btnAction.setText(buttonResId);
                btnAction.setVisibility(View.VISIBLE);
            } else {
                btnAction.setVisibility(View.GONE);
            }

            btnAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performActionForActiveFilter();
                }
            });
        }
    }

    private void setFilterViewScrollable(boolean isScrollable) {
        if (mFilterView != null && mFilterView.getLayoutParams() instanceof AppBarLayout.LayoutParams) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mFilterView.getLayoutParams();
            if (isScrollable) {
                params.setScrollFlags(
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL|
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                );
            } else {
                params.setScrollFlags(0);
            }
        }
    }

    private void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            setFilterViewScrollable(true);
            mEmptyView.setVisibility(View.GONE);
            mFilterDivider.setVisibility(View.VISIBLE);
        }
    }

    private void refreshNotes() {
        if (!isAdded() || mNotesAdapter == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Filter the list according to the RadioGroup selection
                int checkedId = mFilterRadioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.notifications_filter_all) {
                    mNotesAdapter.queryNotes();
                } else if (checkedId == R.id.notifications_filter_unread) {
                    mNotesAdapter.queryNotes(Note.Schema.UNREAD_INDEX, 1);
                } else if (checkedId == R.id.notifications_filter_comments) {
                    mNotesAdapter.queryNotes(Note.Schema.TYPE_INDEX, Note.NOTE_COMMENT_TYPE);
                } else if (checkedId == R.id.notifications_filter_follows) {
                    mNotesAdapter.queryNotes(Note.Schema.TYPE_INDEX, Note.NOTE_FOLLOW_TYPE);
                } else if (checkedId == R.id.notifications_filter_likes) {
                    mNotesAdapter.queryNotes(Note.Schema.TYPE_INDEX, Note.NOTE_LIKE_TYPE);
                } else {
                    mNotesAdapter.queryNotes();
                }

                restoreListScrollPosition();
                if (mNotesAdapter.getCount() > 0) {
                    hideEmptyView();
                } else {
                    showEmptyViewForCurrentFilter();
                }
            }
        });
    }

    // Show different empty list message and action button based on the active filter
    private void showEmptyViewForCurrentFilter() {
        if (!AccountHelper.isSignedInWordPressDotCom()) return;

        int i = mFilterRadioGroup.getCheckedRadioButtonId();
        if (i == R.id.notifications_filter_all) {
            showEmptyView(
                    R.string.notifications_empty_all,
                    R.string.notifications_empty_action_all,
                    R.string.notifications_empty_view_reader
            );
        } else if (i == R.id.notifications_filter_unread) {// User might not have a blog, if so just show the title
            if (WordPress.getCurrentBlog() == null) {
                showEmptyView(R.string.notifications_empty_unread);
            } else {
                showEmptyView(
                        R.string.notifications_empty_unread,
                        R.string.notifications_empty_action_unread,
                        R.string.new_post
                );
            }
        } else if (i == R.id.notifications_filter_comments) {
            showEmptyView(
                    R.string.notifications_empty_comments,
                    R.string.notifications_empty_action_comments,
                    R.string.notifications_empty_view_reader
            );
        } else if (i == R.id.notifications_filter_follows) {
            showEmptyView(
                    R.string.notifications_empty_followers,
                    R.string.notifications_empty_action_followers_likes,
                    R.string.notifications_empty_view_reader
            );
        } else if (i == R.id.notifications_filter_likes) {
            showEmptyView(
                    R.string.notifications_empty_likes,
                    R.string.notifications_empty_action_followers_likes,
                    R.string.notifications_empty_view_reader
            );
        } else {
            showEmptyView(R.string.notifications_empty_list);
        }
    }

    private void performActionForActiveFilter() {
        if (mFilterRadioGroup == null || !isAdded()) return;

        if (!AccountHelper.isSignedInWordPressDotCom()) {
            ActivityLauncher.showSignInForResult(getActivity());
            return;
        }

        int i = mFilterRadioGroup.getCheckedRadioButtonId();
        if (i == R.id.notifications_filter_unread) {// Create a new post
            ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getCurrentBlog(), false);
        } else {// Switch to Reader tab
            if (getActivity() instanceof WPMainActivity) {
                ((WPMainActivity) getActivity()).setReaderTabActive();
            }
        }
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
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

    // Notification filter methods
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        refreshNotes();
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
