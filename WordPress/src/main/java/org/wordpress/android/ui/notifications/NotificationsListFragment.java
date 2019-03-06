package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionWebViewActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.MainToolbarFragment;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.AppRatingDialog;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.analytics.AnalyticsTracker.NOTIFICATIONS_SELECTED_FILTER;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION;
import static org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS;
import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;
import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class NotificationsListFragment extends Fragment implements
        WPMainActivity.OnScrollToTopListener,
        NotesAdapter.DataLoadedListener,
        MainToolbarFragment {
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_PREFILLED_REPLY_EXTRA = "prefilledReplyText";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";
    public static final String NOTE_CURRENT_LIST_FILTER_EXTRA = "currentFilter";

    private static final String KEY_LIST_SCROLL_POSITION = "scrollPosition";
    private static final String KEY_LAST_TAB_POSITION = "tabPosition";
    private static final int TAB_POSITION_ALL = 0;
    private static final int TAB_POSITION_UNREAD = 1;
    private static final int TAB_POSITION_COMMENT = 2;
    private static final int TAB_POSITION_FOLLOW = 3;
    private static final int TAB_POSITION_LIKE = 4;

    private NotesAdapter mNotesAdapter;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;
    private ActionableEmptyView mActionableEmptyView;
    private ViewGroup mConnectJetpackView;
    private TabLayout mTabLayout;
    private View mNewNotificationsBar;
    private int mLastTabPosition;

    @Nullable
    private Toolbar mToolbar = null;
    private String mToolbarTitle;

    private long mRestoredScrollNoteID;
    private boolean mIsAnimatingOutNewNotificationsBar;
    private boolean mShouldRefreshNotifications;

    @Inject AccountStore mAccountStore;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        mShouldRefreshNotifications = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_fragment_notes_list, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view_notes);

        mActionableEmptyView = view.findViewById(R.id.actionable_empty_view);
        mConnectJetpackView = view.findViewById(R.id.connect_jetpack);
        mTabLayout = view.findViewById(R.id.tab_layout);
        mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.notifications_tab_title_all)),
                TAB_POSITION_ALL);
        mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.notifications_tab_title_unread)),
                TAB_POSITION_UNREAD);
        mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.notifications_tab_title_comments)),
                TAB_POSITION_COMMENT);
        mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.notifications_tab_title_follows)),
                TAB_POSITION_FOLLOW);
        mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.notifications_tab_title_likes)),
                TAB_POSITION_LIKE);
        mTabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                Map<String, String> properties = new HashMap<>(1);

                switch (tab.getPosition()) {
                    case TAB_POSITION_ALL:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_ALL.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_ALL);
                        break;
                    case TAB_POSITION_COMMENT:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_COMMENT.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_COMMENT);
                        break;
                    case TAB_POSITION_FOLLOW:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_FOLLOW.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_FOLLOW);
                        break;
                    case TAB_POSITION_LIKE:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_LIKE.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_LIKE);
                        break;
                    case TAB_POSITION_UNREAD:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_UNREAD.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_UNREAD);
                        break;
                    default:
                        properties.put(NOTIFICATIONS_SELECTED_FILTER, FILTERS.FILTER_ALL.toString());
                        mNotesAdapter.setFilter(FILTERS.FILTER_ALL);
                        break;
                }

                AnalyticsTracker.track(Stat.NOTIFICATION_TAPPED_SEGMENTED_CONTROL, properties);
                mLastTabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(Tab tab) {
            }

            @Override
            public void onTabReselected(Tab tab) {
            }
        });

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_notifications),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        hideNewNotificationsBar();
                        fetchNotesFromRemote();
                    }
                }
            );

        // bar that appears at bottom after new notes are received and the user is on this screen
        mNewNotificationsBar = view.findViewById(R.id.layout_new_notificatons);
        mNewNotificationsBar.setVisibility(View.GONE);
        mNewNotificationsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScrollToTop();
            }
        });

        mToolbar = view.findViewById(R.id.toolbar_main);
        mToolbar.setTitle(mToolbarTitle);

        return view;
    }

    /*
     * scroll listener assigned to the recycler when the "new notifications" ribbon is shown to hide
     * it upon scrolling
     */
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            mRecyclerView.removeOnScrollListener(this); // remove the listener now
            clearPendingNotificationsItemsOnUI();
        }
    };

    private void clearPendingNotificationsItemsOnUI() {
        hideNewNotificationsBar();
        // Immediately update the unseen ribbon
        EventBus.getDefault().post(new NotificationEvents.NotificationsUnseenStatus(
                false
        ));
        // Then hit the server
        NotificationsActions.updateNotesSeenTimestamp();

        // Removes app notifications from the system bar
        new Thread(new Runnable() {
            public void run() {
                GCMMessageService.removeAllNotifications(getActivity());
            }
        }).start();
    }

    @Override
    public void onScrollToTop() {
        if (!isAdded()) {
            return;
        }
        clearPendingNotificationsItemsOnUI();
        if (mLinearLayoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
            mLinearLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRecyclerView.setAdapter(getNotesAdapter());

        if (savedInstanceState != null) {
            setRestoredFirstVisibleItemID(savedInstanceState.getLong(KEY_LIST_SCROLL_POSITION, 0));
            setSelectedTab(savedInstanceState.getInt(KEY_LAST_TAB_POSITION, 0));
        }
    }

    private void updateNote(String noteId, CommentStatus status) {
        Note note = NotificationsTable.getNoteById(noteId);
        if (note == null) {
            return;
        }
        note.setLocalStatus(status.toString());
        NotificationsTable.saveNote(note);
        EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.NOTE_DETAIL) {
            mShouldRefreshNotifications = false;
            if (resultCode == RESULT_OK) {
                String noteId = data.getStringExtra(NOTE_MODERATE_ID_EXTRA);
                String newStatus = data.getStringExtra(NOTE_MODERATE_STATUS_EXTRA);
                if (!TextUtils.isEmpty(noteId) && !TextUtils.isEmpty(newStatus)) {
                    updateNote(noteId, CommentStatus.fromString(newStatus));
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        hideNewNotificationsBar();

        // Immediately update the unseen ribbon
        EventBus.getDefault().post(new NotificationEvents.NotificationsUnseenStatus(
                false
        ));

        if (!mAccountStore.hasAccessToken()) {
            showConnectJetpackView();
            mTabLayout.setVisibility(View.GONE);
        } else {
            getNotesAdapter().reloadNotesFromDBAsync();
            if (mShouldRefreshNotifications) {
                fetchNotesFromRemote();
            }
        }

        setSelectedTab(mLastTabPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        mShouldRefreshNotifications = true;
    }

    @Override
    public void onDataLoaded(int itemsCount) {
        if (itemsCount > 0) {
            hideEmptyView();
            if (mRestoredScrollNoteID > 0) {
                restoreListScrollPosition();
            }
        } else {
            showEmptyViewForCurrentFilter();
        }
    }

    private NotesAdapter getNotesAdapter() {
        if (mNotesAdapter == null) {
            mNotesAdapter = new NotesAdapter(requireActivity(), this, null);
            mNotesAdapter.setOnNoteClickListener(mOnNoteClickListener);
        }

        return mNotesAdapter;
    }

    private final OnNoteClickListener mOnNoteClickListener = new OnNoteClickListener() {
        @Override
        public void onClickNote(String noteId) {
            if (!isAdded()) {
                return;
            }

            if (TextUtils.isEmpty(noteId)) {
                return;
            }

            AppRatingDialog.INSTANCE.incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION);

            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            openNoteForReply(getActivity(), noteId, false, null,
                    mNotesAdapter.getCurrentFilter(), false);
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
                                        String replyText,
                                        NotesAdapter.FILTERS filter,
                                        boolean isTappedFromPushNotification) {
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
        detailIntent.putExtra(NOTE_CURRENT_LIST_FILTER_EXTRA, filter);
        detailIntent.putExtra(IS_TAPPED_ON_NOTIFICATION, isTappedFromPushNotification);

        openNoteForReplyWithParams(detailIntent, activity);
    }

    private static void openNoteForReplyWithParams(Intent detailIntent, Activity activity) {
        activity.startActivityForResult(detailIntent, RequestCodes.NOTE_DETAIL);
    }

    private void showEmptyView(@StringRes int titleResId) {
        showEmptyView(titleResId, 0, 0);
    }

    private void showEmptyView(@StringRes int titleResId, @StringRes int descriptionResId, @StringRes int buttonResId) {
        if (isAdded() && mActionableEmptyView != null) {
            mActionableEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mConnectJetpackView.setVisibility(View.GONE);
            mActionableEmptyView.title.setText(titleResId);

            if (descriptionResId != 0) {
                mActionableEmptyView.subtitle.setText(descriptionResId);
                mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);
            } else {
                mActionableEmptyView.subtitle.setVisibility(View.GONE);
            }

            if (buttonResId != 0) {
                mActionableEmptyView.button.setText(buttonResId);
                mActionableEmptyView.button.setVisibility(View.VISIBLE);
            } else {
                mActionableEmptyView.button.setVisibility(View.GONE);
            }

            mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    performActionForActiveFilter();
                }
            });
        }
    }

    private void showConnectJetpackView() {
        if (isAdded() && mConnectJetpackView != null) {
            mConnectJetpackView.setVisibility(View.VISIBLE);
            mActionableEmptyView.setVisibility(View.GONE);
            mTabLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
            clearToolbarScrollFlags();

            Button setupButton = mConnectJetpackView.findViewById(R.id.jetpack_setup);
            setupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SiteModel siteModel = getSelectedSite();
                    JetpackConnectionWebViewActivity
                            .startJetpackConnectionFlow(getActivity(), NOTIFICATIONS, siteModel, false);
                }
            });
        }
    }

    private void clearToolbarScrollFlags() {
        if (mToolbar != null && mToolbar.getLayoutParams() instanceof AppBarLayout.LayoutParams) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            params.setScrollFlags(0);
        }
    }

    private void hideEmptyView() {
        if (isAdded() && mActionableEmptyView != null) {
            mActionableEmptyView.setVisibility(View.GONE);
            mConnectJetpackView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchNotesFromRemote() {
        if (!isAdded() || mNotesAdapter == null) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            return;
        }

        NotificationsUpdateServiceStarter.startService(getActivity());
    }

    // Show different empty list message and action button based on the active filter
    private void showEmptyViewForCurrentFilter() {
        if (!mAccountStore.hasAccessToken()) {
            return;
        }

        switch (mTabLayout.getSelectedTabPosition()) {
            case TAB_POSITION_ALL:
                showEmptyView(
                        R.string.notifications_empty_all,
                        R.string.notifications_empty_action_all,
                        R.string.notifications_empty_view_reader
                );
                break;
            case TAB_POSITION_COMMENT:
                showEmptyView(
                        R.string.notifications_empty_comments,
                        R.string.notifications_empty_action_comments,
                        R.string.notifications_empty_view_reader
                );
                break;
            case TAB_POSITION_FOLLOW:
                showEmptyView(
                        R.string.notifications_empty_followers,
                        R.string.notifications_empty_action_followers_likes,
                        R.string.notifications_empty_view_reader
                );
                break;
            case TAB_POSITION_LIKE:
                showEmptyView(
                        R.string.notifications_empty_likes,
                        R.string.notifications_empty_action_followers_likes,
                        R.string.notifications_empty_view_reader
                );
                break;
            case TAB_POSITION_UNREAD:
                if (getSelectedSite() == null) {
                    showEmptyView(R.string.notifications_empty_unread);
                } else {
                    showEmptyView(
                            R.string.notifications_empty_unread,
                            R.string.notifications_empty_action_unread,
                            R.string.posts_empty_list_button
                    );
                }
                break;
            default:
                showEmptyView(R.string.notifications_empty_list);
        }

        mActionableEmptyView.image.setVisibility(DisplayUtils.isLandscape(requireContext()) ? View.GONE : View.VISIBLE);
    }

    private void performActionForActiveFilter() {
        if (mTabLayout == null || !isAdded()) {
            return;
        }

        if (!mAccountStore.hasAccessToken()) {
            ActivityLauncher.showSignInForResult(getActivity());
            return;
        }

        if (mTabLayout.getSelectedTabPosition() == TAB_POSITION_UNREAD) {
            ActivityLauncher.addNewPostForResult(getActivity(), getSelectedSite(), false);
        } else {
            if (getActivity() instanceof WPMainActivity) {
                ((WPMainActivity) getActivity()).setReaderPageActive();
            }
        }
    }

    private void restoreListScrollPosition() {
        if (!isAdded() || mRestoredScrollNoteID <= 0) {
            return;
        }
        final int pos = getNotesAdapter().getPositionForNote(String.valueOf(mRestoredScrollNoteID));

        if (pos != RecyclerView.NO_POSITION && pos < mNotesAdapter.getItemCount()) {
            // Restore scroll position in list
            mLinearLayoutManager.scrollToPosition(pos);
            mRestoredScrollNoteID = 0L;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }

        // Save list view scroll position
        outState.putLong(KEY_LIST_SCROLL_POSITION, getFirstVisibleItemID());
        outState.putInt(KEY_LAST_TAB_POSITION, mLastTabPosition);

        super.onSaveInstanceState(outState);
    }

    private long getFirstVisibleItemID() {
        if (!isAdded() || mRecyclerView == null) {
            return RecyclerView.NO_POSITION;
        }

        int pos = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        return getNotesAdapter().getItemId(pos);
    }

    private void setRestoredFirstVisibleItemID(long noteID) {
        mRestoredScrollNoteID = noteID;
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

    @Override
    public void setTitle(@NonNull String title) {
        mToolbarTitle = title;
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsRefreshError error) {
        if (isAdded()) {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final NotificationEvents.NotificationsRefreshCompleted event) {
        if (!isAdded()) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(false);
        mNotesAdapter.addAll(event.notes, true);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final NotificationEvents.NoteLikeOrModerationStatusChanged event) {
        // Like/unlike done -> refresh the note and update db
        NotificationsActions.downloadNoteAndUpdateDB(
                event.noteId,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        EventBus.getDefault().removeStickyEvent(
                                NotificationEvents.NoteLikeOrModerationStatusChanged.class);
                        // now re-set the object in our list adapter with the note saved in the updated DB
                        Note note = NotificationsTable.getNoteById(event.noteId);
                        if (note != null) {
                            mNotesAdapter.replaceNote(note);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        EventBus.getDefault().removeStickyEvent(
                                NotificationEvents.NoteLikeOrModerationStatusChanged.class);
                    }
                });
    }

    public SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        if (!isAdded()) {
            return;
        }
        mRestoredScrollNoteID = getFirstVisibleItemID(); // Remember the ID of the first note visible on the screen
        getNotesAdapter().reloadNotesFromDBAsync();
        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsUnseenStatus event) {
        if (!isAdded()) {
            return;
        }
        // if a new note arrives when the notifications list is on Foreground.
        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI();
        } else {
            hideNewNotificationsBar();
        }
    }

    private void showNewUnseenNotificationsUI() {
        if (!isAdded()) {
            return;
        }

        // Make sure the RecyclerView is configured
        if (mRecyclerView == null || mRecyclerView.getLayoutManager() == null) {
            return;
        }

        mRecyclerView
                .clearOnScrollListeners(); // Just one listener. Multiple notes received here add multiple listeners.

        // Assign the scroll listener to hide the bar when the recycler is scrolled, but don't assign
        // it right away since the user may be scrolling when the bar appears (which would cause it
        // to disappear as soon as it's displayed)
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    mRecyclerView.addOnScrollListener(mOnScrollListener);
                }
            }
        }, 1000L);

        // Check if the first item is visible on the screen
        View child = mRecyclerView.getLayoutManager().getChildAt(0);
        if (child != null && mRecyclerView.getLayoutManager().getPosition(child) > 0) {
            showNewNotificationsBar();
        }
    }

    /*
     * bar that appears at the bottom when new notifications are available
     */
    private boolean isNewNotificationsBarShowing() {
        return (mNewNotificationsBar != null && mNewNotificationsBar.getVisibility() == View.VISIBLE);
    }

    private void showNewNotificationsBar() {
        if (!isAdded() || isNewNotificationsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewNotificationsBar, R.anim.notifications_bottom_bar_in);
        mNewNotificationsBar.setVisibility(View.VISIBLE);
    }

    private void hideNewNotificationsBar() {
        if (!isAdded() || !isNewNotificationsBarShowing() || mIsAnimatingOutNewNotificationsBar) {
            return;
        }

        mIsAnimatingOutNewNotificationsBar = true;

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isAdded()) {
                    mNewNotificationsBar.setVisibility(View.GONE);
                    mIsAnimatingOutNewNotificationsBar = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        AniUtils.startAnimation(mNewNotificationsBar, R.anim.notifications_bottom_bar_out, listener);
    }

    private void setSelectedTab(int position) {
        mLastTabPosition = position;

        if (mTabLayout != null) {
            TabLayout.Tab tab = mTabLayout.getTabAt(mLastTabPosition);

            if (tab != null) {
                tab.select();
            }
        }
    }
}
