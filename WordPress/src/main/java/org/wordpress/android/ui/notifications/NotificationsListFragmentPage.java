package org.wordpress.android.ui.notifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageHandler;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.PagePostCreationSourcesDetail;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener;
import org.wordpress.android.ui.notifications.NotificationEvents.NoteLikeOrModerationStatusChanged;
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged;
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshCompleted;
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshError;
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.AppRatingDialog;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_ID_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_MODERATE_ID_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.TAB_POSITION_ALL;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.TAB_POSITION_COMMENT;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.TAB_POSITION_FOLLOW;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.TAB_POSITION_LIKE;
import static org.wordpress.android.ui.notifications.NotificationsListFragment.TAB_POSITION_UNREAD;
import static org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_ALL;
import static org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_COMMENT;
import static org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_FOLLOW;
import static org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_LIKE;
import static org.wordpress.android.ui.notifications.adapters.NotesAdapter.FILTERS.FILTER_UNREAD;
import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;
import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class NotificationsListFragmentPage extends Fragment implements
        OnScrollToTopListener,
        NotesAdapter.DataLoadedListener {
    private static final String KEY_TAB_POSITION = "tabPosition";

    private ActionableEmptyView mActionableEmptyView;
    private LinearLayoutManager mLinearLayoutManager;
    private NotesAdapter mNotesAdapter;
    private RecyclerView mRecyclerView;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private View mNewNotificationsBar;
    private boolean mIsAnimatingOutNewNotificationsBar;
    private boolean mShouldRefreshNotifications;
    private int mTabPosition;

    @Inject AccountStore mAccountStore;
    @Inject GCMMessageHandler mGCMMessageHandler;

    public static Fragment newInstance(int position) {
        NotificationsListFragmentPage fragment = new NotificationsListFragmentPage();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TAB_POSITION, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    public interface OnNoteClickListener {
        void onClickNote(String noteId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setAdapter(getNotesAdapter());

        if (savedInstanceState != null) {
            mTabPosition = savedInstanceState.getInt(KEY_TAB_POSITION, TAB_POSITION_ALL);
        }

        switch (mTabPosition) {
            case TAB_POSITION_ALL:
                mNotesAdapter.setFilter(FILTER_ALL);
                break;
            case TAB_POSITION_COMMENT:
                mNotesAdapter.setFilter(FILTER_COMMENT);
                break;
            case TAB_POSITION_FOLLOW:
                mNotesAdapter.setFilter(FILTER_FOLLOW);
                break;
            case TAB_POSITION_LIKE:
                mNotesAdapter.setFilter(FILTER_LIKE);
                break;
            case TAB_POSITION_UNREAD:
                mNotesAdapter.setFilter(FILTER_UNREAD);
                break;
            default:
                mNotesAdapter.setFilter(FILTER_ALL);
                break;
        }
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        mShouldRefreshNotifications = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications_list_fragment_page, container, false);

        if (getArguments() != null) {
            mTabPosition = getArguments().getInt(KEY_TAB_POSITION, TAB_POSITION_ALL);
        }

        mActionableEmptyView = view.findViewById(R.id.actionable_empty_view);

        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView = view.findViewById(R.id.notifications_list);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) view.findViewById(R.id.notifications_refresh),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        hideNewNotificationsBar();
                        fetchNotesFromRemote();
                    }
                });

        mNewNotificationsBar = view.findViewById(R.id.layout_new_notificatons);
        mNewNotificationsBar.setVisibility(View.GONE);
        mNewNotificationsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScrollToTop();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        mRecyclerView.setAdapter(null);
        mNotesAdapter = null;
        super.onDestroyView();
    }

    @Override
    public void onDataLoaded(int itemsCount) {
        if (!isAdded()) {
            CrashLoggingUtils.log("NotificationsListFragmentPage.onDataLoaded occurred when fragment is not attached.");
        }

        if (itemsCount > 0) {
            hideEmptyView();
        } else {
            showEmptyViewForCurrentFilter();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mShouldRefreshNotifications = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        hideNewNotificationsBar();
        EventBus.getDefault().post(new NotificationsUnseenStatus(false));

        if (mAccountStore.hasAccessToken()) {
            getNotesAdapter().reloadNotesFromDBAsync();

            if (mShouldRefreshNotifications) {
                fetchNotesFromRemote();
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_TAB_POSITION, mTabPosition);
        super.onSaveInstanceState(outState);
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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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

            // Open the latest version of this note in case it has changed, which can happen if the note was tapped
            // from the list after it was updated by another fragment (such as NotificationsDetailListFragment).
            openNoteForReply(getActivity(), noteId, false, null, mNotesAdapter.getCurrentFilter(), false);
        }
    };

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            mRecyclerView.removeOnScrollListener(this);
            clearPendingNotificationsItemsOnUI();
        }
    };

    private void clearPendingNotificationsItemsOnUI() {
        hideNewNotificationsBar();
        EventBus.getDefault().post(new NotificationsUnseenStatus(false));
        NotificationsActions.updateNotesSeenTimestamp();

        new Thread(new Runnable() {
            public void run() {
                mGCMMessageHandler.removeAllNotifications(getActivity());
            }
        }).start();
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

    private NotesAdapter getNotesAdapter() {
        if (mNotesAdapter == null) {
            mNotesAdapter = new NotesAdapter(requireActivity(), this, null);
            mNotesAdapter.setOnNoteClickListener(mOnNoteClickListener);
        }

        return mNotesAdapter;
    }

    private static Intent getOpenNoteIntent(Activity activity, String noteId) {
        Intent detailIntent = new Intent(activity, NotificationsDetailActivity.class);
        detailIntent.putExtra(NOTE_ID_EXTRA, noteId);
        return detailIntent;
    }

    public SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            return ((WPMainActivity) getActivity()).getSelectedSite();
        } else {
            return null;
        }
    }

    private void hideEmptyView() {
        if (isAdded() && mActionableEmptyView != null) {
            mActionableEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
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

    private boolean isNewNotificationsBarShowing() {
        return (mNewNotificationsBar != null && mNewNotificationsBar.getVisibility() == View.VISIBLE);
    }

    public static void openNoteForReply(Activity activity, String noteId, boolean shouldShowKeyboard, String replyText,
                                        NotesAdapter.FILTERS filter, boolean isTappedFromPushNotification) {
        if (noteId == null || activity == null || activity.isFinishing()) {
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

    private void performActionForActiveFilter() {
        if (!isAdded()) {
            return;
        }

        if (!mAccountStore.hasAccessToken()) {
            ActivityLauncher.showSignInForResult(getActivity());
            return;
        }

        if (mTabPosition == TAB_POSITION_UNREAD) {
            ActivityLauncher.addNewPostForResult(
                    getActivity(),
                    getSelectedSite(),
                    false,
                    PagePostCreationSourcesDetail.POST_FROM_NOTIFS_EMPTY_VIEW
            );
        } else if (getActivity() instanceof WPMainActivity) {
            ((WPMainActivity) getActivity()).setReaderPageActive();
        }
    }

    private void showEmptyView(@StringRes int titleResId) {
        showEmptyView(titleResId, 0, 0);
    }

    private void showEmptyView(@StringRes int titleResId, @StringRes int descriptionResId, @StringRes int buttonResId) {
        if (isAdded() && mActionableEmptyView != null) {
            mActionableEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
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

    // Show different empty view message and action button based on selected tab.
    private void showEmptyViewForCurrentFilter() {
        if (!mAccountStore.hasAccessToken()) {
            return;
        }

        switch (mTabPosition) {
            case TAB_POSITION_ALL:
                showEmptyView(
                        R.string.notifications_empty_all,
                        R.string.notifications_empty_action_all,
                        R.string.notifications_empty_view_reader);
                break;
            case TAB_POSITION_COMMENT:
                showEmptyView(
                        R.string.notifications_empty_comments,
                        R.string.notifications_empty_action_comments,
                        R.string.notifications_empty_view_reader);
                break;
            case TAB_POSITION_FOLLOW:
                showEmptyView(
                        R.string.notifications_empty_followers,
                        R.string.notifications_empty_action_followers_likes,
                        R.string.notifications_empty_view_reader);
                break;
            case TAB_POSITION_LIKE:
                showEmptyView(
                        R.string.notifications_empty_likes,
                        R.string.notifications_empty_action_followers_likes,
                        R.string.notifications_empty_view_reader);
                break;
            case TAB_POSITION_UNREAD:
                if (getSelectedSite() == null) {
                    showEmptyView(R.string.notifications_empty_unread);
                } else {
                    showEmptyView(
                            R.string.notifications_empty_unread,
                            R.string.notifications_empty_action_unread,
                            R.string.posts_empty_list_button);
                }

                break;
            default:
                showEmptyView(R.string.notifications_empty_list);
        }

        mActionableEmptyView.image.setVisibility(DisplayUtils.isLandscape(getContext()) ? View.GONE : View.VISIBLE);
    }

    private void showNewNotificationsBar() {
        if (!isAdded() || isNewNotificationsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewNotificationsBar, R.anim.notifications_bottom_bar_in);
        mNewNotificationsBar.setVisibility(View.VISIBLE);
    }

    private void showNewUnseenNotificationsUI() {
        if (!isAdded() || mRecyclerView == null || mRecyclerView.getLayoutManager() == null) {
            return;
        }

        mRecyclerView.clearOnScrollListeners();
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    mRecyclerView.addOnScrollListener(mOnScrollListener);
                }
            }
        }, 1000L);

        View first = mRecyclerView.getLayoutManager().getChildAt(0);
        // Show new notifications bar if first item is not visible on the screen.
        if (first != null && mRecyclerView.getLayoutManager().getPosition(first) > 0) {
            showNewNotificationsBar();
        }
    }

    private void updateNote(String noteId, CommentStatus status) {
        Note note = NotificationsTable.getNoteById(noteId);

        if (note != null) {
            note.setLocalStatus(status.toString());
            NotificationsTable.saveNote(note);
            EventBus.getDefault().post(new NotificationsChanged());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final NoteLikeOrModerationStatusChanged event) {
        NotificationsActions.downloadNoteAndUpdateDB(
            event.noteId,
            new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject response) {
                    EventBus.getDefault().removeStickyEvent(NoteLikeOrModerationStatusChanged.class);
                    Note note = NotificationsTable.getNoteById(event.noteId);

                    if (note != null) {
                        mNotesAdapter.replaceNote(note);
                    }
                }
            }, new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    EventBus.getDefault().removeStickyEvent(NoteLikeOrModerationStatusChanged.class);
                }
            }
        );
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationsChanged event) {
        if (!isAdded()) {
            return;
        }

        getNotesAdapter().reloadNotesFromDBAsync();

        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final NotificationsRefreshCompleted event) {
        if (!isAdded()) {
            return;
        }

        mSwipeToRefreshHelper.setRefreshing(false);
        mNotesAdapter.addAll(event.notes, true);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationsRefreshError error) {
        if (isAdded()) {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationsUnseenStatus event) {
        if (!isAdded()) {
            return;
        }

        if (event.hasUnseenNotes) {
            showNewUnseenNotificationsUI();
        } else {
            hideNewNotificationsBar();
        }
    }
}
