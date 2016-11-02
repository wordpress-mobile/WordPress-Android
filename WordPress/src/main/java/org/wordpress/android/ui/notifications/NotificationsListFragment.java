package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NotificationsListFragment extends Fragment
        implements WPMainActivity.OnScrollToTopListener, RadioGroup.OnCheckedChangeListener, NotesAdapter.DataLoadedListener {
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_PREFILLED_REPLY_EXTRA = "prefilledReplyText";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";

    private static final String KEY_LIST_SCROLL_POSITION = "scrollPosition";

    private NotesAdapter mNotesAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;
    private ViewGroup mEmptyView;
    private View mFilterView;
    private RadioGroup mFilterRadioGroup;
    private View mFilterDivider;

    private int mRestoredScrollPosition;

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

        mFilterRadioGroup = (RadioGroup) view.findViewById(R.id.notifications_radio_group);
        mFilterRadioGroup.setOnCheckedChangeListener(this);
        mFilterDivider = view.findViewById(R.id.notifications_filter_divider);
        mEmptyView = (ViewGroup) view.findViewById(R.id.empty_view);
        mFilterView = view.findViewById(R.id.notifications_filter);

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_notifications);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchNotesFromRemote();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRecyclerView.setAdapter(getNotesAdapter());

        if (savedInstanceState != null) {
            setRestoredListPosition(savedInstanceState.getInt(KEY_LIST_SCROLL_POSITION, RecyclerView.NO_POSITION));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mNotesAdapter.reloadNotesFromDBAsync();

        // Removes app notifications from the system bar
        new Thread(new Runnable() {
            public void run() {
                GCMMessageService.removeAllNotifications(getActivity());
            }
        }).start();
    }

    @Override
    public void onDataLoaded(int itemsCount) {
        if (itemsCount > 0) {
            hideEmptyView();
        } else {
            showEmptyViewForCurrentFilter();
        }
    }

    private NotesAdapter getNotesAdapter() {
        if (mNotesAdapter == null) {
            mNotesAdapter = new NotesAdapter(getActivity(), this, null);
            mNotesAdapter.setOnNoteClickListener(mOnNoteClickListener);
        }

        return mNotesAdapter;
    }

    // TODO: Maybe reintroduce infinite scrolling later.
    /*
    private final NotesAdapter.OnLoadMoreListener mOnLoadMoreListener = new NotesAdapter.OnLoadMoreListener() {
        @Override
        public void onLoadMore(long noteTimestamp) {
            Map<String, String> params = new HashMap<>();
            AppLog.d(AppLog.T.NOTIFS, String.format("Requesting more notes before %s", noteTimestamp));
            params.put("before", String.valueOf(noteTimestamp));
            NotesResponseHandler notesHandler = new NotesResponseHandler() {
                @Override
                public void onNotes(List<Note> notes) {
                    // API returns 'on or before' timestamp, so remove first item
                    if (notes.size() >= 1) {
                        notes.remove(0);
                    }
                    //mNotesAdapter.setAllNotesLoaded(notes.size() == 0);
                    mNotesAdapter.addAll(notes, false);
                }
            };
            WordPress.getRestClientUtilsV1_1().getNotifications(params, notesHandler, notesHandler);

        }
    };
*/
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
            openNoteForReply(getActivity(), noteId, false, null);
        }
    };

    private static Intent getOpenNoteIntent(Activity activity,
                                            String noteId) {
        Intent detailIntent = new Intent(activity, NotificationsDetailActivity.class);
        detailIntent.putExtra(NOTE_ID_EXTRA, noteId);
        return detailIntent;
    }

    /**
     * Open a note fragment based on the type of note
     */
    public static void openNoteForReply(Activity activity,
                                        String noteId,
                                        boolean shouldShowKeyboard,
                                        String replyText) {
        if (noteId == null || activity == null) {
            return;
        }

        if (activity.isFinishing()) {
            return;
        }

        Intent detailIntent = getOpenNoteIntent(activity, noteId);
        detailIntent.putExtra(NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard);
        if (!TextUtils.isEmpty(replyText)) {
            detailIntent.putExtra(NOTE_PREFILLED_REPLY_EXTRA, replyText);
        }
        activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL);
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
            mRecyclerView.setVisibility(View.GONE);
            setFilterViewScrollable(false);
            ((TextView) mEmptyView.findViewById(R.id.text_empty)).setText(titleResId);

            TextView descriptionTextView = (TextView) mEmptyView.findViewById(R.id.text_empty_description);
            if (descriptionResId > 0) {
                descriptionTextView.setText(descriptionResId);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }

            TextView btnAction = (TextView) mEmptyView.findViewById(R.id.button_empty_action);
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
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL |
                                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                );
            } else {
                params.setScrollFlags(0);
            }
        }
    }

    // TODO Needs implementation
    private void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            setFilterViewScrollable(true);
            mEmptyView.setVisibility(View.GONE);
            mFilterDivider.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchNotesFromRemote() {
        if (!isAdded() || mNotesAdapter == null) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            mSwipeRefreshLayout.setRefreshing(true);
            return;
        }

        NotificationsUpdateService.startService(getActivity());
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
                && mRestoredScrollPosition < mNotesAdapter.getItemCount()) {
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Filter the list according to the RadioGroup selection
                int checkedId = mFilterRadioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.notifications_filter_all) {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_ALL);
                } else if (checkedId == R.id.notifications_filter_unread) {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_UNREAD);
                } else if (checkedId == R.id.notifications_filter_comments) {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_COMMENT);
                } else if (checkedId == R.id.notifications_filter_follows) {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_FOLLOW);
                } else if (checkedId == R.id.notifications_filter_likes) {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_LIKE);
                } else {
                    mNotesAdapter.setFilter(NotesAdapter.FILTERS.FILTER_ALL);
                }

                restoreListScrollPosition();
            }
        });
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getScrollPosition() > 0) {
            mLinearLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);
            NotificationsActions.updateSeenNotes();
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
    public void onEventMainThread(NotificationEvents.NotificationsRefreshError error) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_notifications));
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
    @SuppressWarnings("unused")
    public void onEventMainThread(final NotificationEvents.NotificationsRefreshCompleted event) {
        if (!isAdded()) {
            return;
        }
        mSwipeRefreshLayout.setRefreshing(false);
        NotificationsActions.updateSeenNotes(); // Refresh launched by the user. Update the last seen time.
        mNotesAdapter.addAll(event.notes, true);
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
