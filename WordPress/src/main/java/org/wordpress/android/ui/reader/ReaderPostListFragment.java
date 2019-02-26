package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderSearchTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction;
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.ReaderStore;
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.models.news.NewsItem;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.main.BottomNavController;
import org.wordpress.android.ui.main.MainToolbarFragment;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.news.NewsViewHolder.NewsCardListener;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.quickstart.QuickStartEvent;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult;
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter.SiteSearchAdapterListener;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction;
import org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.WPDialogSnackbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST;
import static org.wordpress.android.fluxc.generated.AccountActionBuilder.newUpdateSubscriptionNotificationPostAction;

public class ReaderPostListFragment extends Fragment
        implements ReaderInterfaces.OnPostSelectedListener,
        ReaderInterfaces.OnPostPopupListener,
        ReaderInterfaces.OnFollowListener,
        WPMainActivity.OnActivityBackPressedListener,
        WPMainActivity.OnScrollToTopListener,
        MainToolbarFragment {
    private static final int TAB_POSTS = 0;
    private static final int TAB_SITES = 1;
    private static final int NO_POSITION = -1;

    private ReaderPostAdapter mPostAdapter;
    private ReaderSiteSearchAdapter mSiteSearchAdapter;
    private ReaderSearchSuggestionAdapter mSearchSuggestionAdapter;

    private FilteredRecyclerView mRecyclerView;
    private boolean mFirstLoad = true;

    private View mNewPostsBar;
    private ActionableEmptyView mActionableEmptyView;
    private ProgressBar mProgress;
    private TabLayout mSearchTabs;

    private SearchView mSearchView;
    private MenuItem mSettingsMenuItem;
    private MenuItem mSearchMenuItem;

    private BottomNavController mBottomNavController;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private long mCurrentFeedId;
    private String mCurrentSearchQuery;
    private ReaderPostListType mPostListType;
    private ReaderSiteModel mLastTappedSiteSearchResult;

    private int mRestorePosition;
    private int mSiteSearchRestorePosition;
    private int mPostSearchAdapterPos;
    private int mSiteSearchAdapterPos;
    private int mSearchTabsPos = NO_POSITION;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mHasUpdatedPosts;
    private boolean mIsAnimatingOutNewPostsBar;

    private static boolean mHasPurgedReaderDb;
    private static Date mLastAutoUpdateDt;

    private final HistoryStack mTagPreviewHistory = new HistoryStack("tag_preview_history");

    private AlertDialog mBookmarksSavedLocallyDialog;
    private QuickStartEvent mQuickStartEvent;

    private ReaderPostListViewModel mViewModel;

    private Observer<NewsItem> mNewsItemObserver = new Observer<NewsItem>() {
        @Override public void onChanged(@Nullable NewsItem newsItem) {
            getPostAdapter().updateNewsCardItem(newsItem);
        }
    };

    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject AccountStore mAccountStore;
    @Inject ReaderStore mReaderStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;
    @Inject QuickStartStore mQuickStartStore;

    private enum ActionableEmptyViewButtonType {
        DISCOVER,
        FOLLOWED
    }

    private static class HistoryStack extends Stack<String> {
        private final String mKeyName;

        HistoryStack(@SuppressWarnings("SameParameterValue") String keyName) {
            mKeyName = keyName;
        }

        void restoreInstance(Bundle bundle) {
            clear();
            if (bundle.containsKey(mKeyName)) {
                ArrayList<String> history = bundle.getStringArrayList(mKeyName);
                if (history != null) {
                    this.addAll(history);
                }
            }
        }

        void saveInstance(Bundle bundle) {
            if (!isEmpty()) {
                ArrayList<String> history = new ArrayList<>();
                history.addAll(this);
                bundle.putStringArrayList(mKeyName, history);
            }
        }
    }

    public static ReaderPostListFragment newInstance() {
        ReaderTag tag = AppPrefs.getReaderTag();
        if (tag == null) {
            tag = ReaderUtils.getDefaultTag();
        }
        return newInstanceForTag(tag, ReaderPostListType.TAG_FOLLOWED);
    }

    /*
     * show posts with a specific tag (either TAG_FOLLOWED or TAG_PREVIEW)
     */
    static ReaderPostListFragment newInstanceForTag(ReaderTag tag, ReaderPostListType listType) {
        AppLog.d(T.READER, "reader post list > newInstance (tag)");

        Bundle args = new Bundle();
        args.putSerializable(ReaderConstants.ARG_TAG, tag);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, listType);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);
        fragment.trackTagLoaded(tag);

        return fragment;
    }

    /*
     * show posts in a specific blog
     */
    public static ReaderPostListFragment newInstanceForBlog(long blogId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static ReaderPostListFragment newInstanceForFeed(long feedId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_FEED_ID, feedId);
        args.putLong(ReaderConstants.ARG_BLOG_ID, feedId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public @Nullable SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }
        return null;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            if (args.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) args.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }

            mCurrentBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mCurrentFeedId = args.getLong(ReaderConstants.ARG_FEED_ID);
            mCurrentSearchQuery = args.getString(ReaderConstants.ARG_SEARCH_QUERY);

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(getCurrentTagName());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            AppLog.d(T.READER, "reader post list > restoring instance state");
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_BLOG_ID)) {
                mCurrentBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_FEED_ID)) {
                mCurrentFeedId = savedInstanceState.getLong(ReaderConstants.ARG_FEED_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_SEARCH_QUERY)) {
                mCurrentSearchQuery = savedInstanceState.getString(ReaderConstants.ARG_SEARCH_QUERY);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                        (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(savedInstanceState);
            }
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mSiteSearchRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
            mHasUpdatedPosts = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mFirstLoad = savedInstanceState.getBoolean(ReaderConstants.KEY_FIRST_LOAD);
            mSearchTabsPos = savedInstanceState.getInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, NO_POSITION);
            mQuickStartEvent = savedInstanceState.getParcelable(QuickStartEvent.KEY);
        }
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // we need to pass activity, since this fragment extends Android Native fragment (we can pass `this` as soon as
        // this fragment extends Support fragment.
        mViewModel = ViewModelProviders.of((FragmentActivity) getActivity(), mViewModelFactory)
                                       .get(ReaderPostListViewModel.class);
        mViewModel.start(mCurrentTag);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBookmarksSavedLocallyDialog != null) {
            mBookmarksSavedLocallyDialog.dismiss();
        }
        mWasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPostAdapter();

        if (mWasPaused) {
            AppLog.d(T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;

            if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
                resumeFollowedTag();
            } else {
                refreshPosts();
            }

            // if the user tapped a site to show site preview, it's possible they also changed the follow
            // status so tell the search adapter to check whether it has the correct follow status
            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS && mLastTappedSiteSearchResult != null) {
                getSiteSearchAdapter().checkFollowStatusForSite(mLastTappedSiteSearchResult);
                mLastTappedSiteSearchResult = null;
            }

            ReaderTag discoverTag = ReaderUtils.getTagFromEndpoint(ReaderTag.DISCOVER_PATH);
            ReaderTag readerTag = AppPrefs.getReaderTag();

            if (discoverTag != null && discoverTag.equals(readerTag)) {
                setCurrentTag(readerTag);
                updateCurrentTag();
            } else if (discoverTag == null) {
                AppLog.w(T.READER, "Discover tag not found; ReaderTagTable returned null");
            }
        }
    }

    /*
     * called when fragment is resumed and we're looking at posts in a followed tag
     */
    private void resumeFollowedTag() {
        Object event = EventBus.getDefault().getStickyEvent(ReaderEvents.TagAdded.class);
        if (event != null) {
            // user just added a tag so switch to it.
            String tagName = ((ReaderEvents.TagAdded) event).getTagName();
            EventBus.getDefault().removeStickyEvent(event);
            ReaderTag newTag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED);
            setCurrentTag(newTag);
        } else if (!ReaderTagTable.tagExists(getCurrentTag())) {
            // current tag no longer exists, revert to default
            AppLog.d(T.READER, "reader post list > current tag no longer valid");
            ReaderTag tag = ReaderUtils.getDefaultTag();
            // it's possible the default tag won't exist if the user just changed the app's
            // language, in which case default to the first tag in the table
            if (!ReaderTagTable.tagExists(tag)) {
                tag = ReaderTagTable.getFirstTag();
            }
            setCurrentTag(tag);
        } else {
            // otherwise, refresh posts to make sure any changes are reflected and auto-update
            // posts in the current tag if it's time
            refreshPosts();
            updateCurrentTagIfTime();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        super.onAttach(context);

        // detect the bottom nav controller when this fragment is hosted in the main activity - this is used to
        // hide the bottom nav when the user searches from the reader
        if (context instanceof BottomNavController) {
            mBottomNavController = (BottomNavController) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBottomNavController = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
        EventBus.getDefault().registerSticky(this);

        reloadTags();

        // purge database and update followed tags/blog if necessary - note that we don't purge unless
        // there's a connection to avoid removing posts the user would expect to see offline
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && NetworkUtils.isNetworkAvailable(getActivity())) {
            purgeDatabaseIfNeeded();
            updateFollowedTagsAndBlogsIfNeeded();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
    }

    /*
     * ensures the adapter is created and posts are updated if they haven't already been
     */
    private void checkPostAdapter() {
        if (isAdded() && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostAdapter());

            if (!mHasUpdatedPosts && NetworkUtils.isNetworkAvailable(getActivity())) {
                mHasUpdatedPosts = true;
                if (getPostListType().isTagType()) {
                    updateCurrentTagIfTime();
                } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    updatePostsInCurrentBlogOrFeed(ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER);
                }
            }
        }
    }

    /*
     * reset the post adapter to initial state and create it again using the passed list type
     */
    private void resetPostAdapter(ReaderPostListType postListType) {
        mPostListType = postListType;
        mPostAdapter = null;
        mRecyclerView.setAdapter(null);
        mRecyclerView.setAdapter(getPostAdapter());
        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported());
    }

    @Override
    public void setTitle(@NonNull String title) {
        // Do nothing - no title for this toolbar
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedTagsChanged event) {
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            // reload the tag filter since tags have changed
            reloadTags();

            // update the current tag if the list fragment is empty - this will happen if
            // the tag table was previously empty (ie: first run)
            if (isPostAdapterEmpty()) {
                updateCurrentTag();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedBlogsChanged event) {
        // refresh posts if user is viewing "Followed Sites"
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED
            && hasCurrentTag()
            && getCurrentTag().isFollowedSites()) {
            refreshPosts();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(final QuickStartEvent event) {
        if (!isAdded() || getView() == null) {
            return;
        }

        mQuickStartEvent = event;
        EventBus.getDefault().removeStickyEvent(event);

        if (mQuickStartEvent.getTask() == QuickStartTask.FOLLOW_SITE
            && isAdded() && getActivity() instanceof WPMainActivity) {
            Spannable title = QuickStartUtils.stylizeQuickStartPrompt(getActivity(),
                    R.string.quick_start_dialog_follow_sites_message_short_search,
                    R.drawable.ic_search_white_24dp);

            WPDialogSnackbar snackbar = WPDialogSnackbar.make(requireActivity().findViewById(R.id.coordinator),
                    title, AccessibilityUtils.getSnackbarDuration(requireContext(),
                            getResources().getInteger(R.integer.quick_start_snackbar_duration_ms)));

            ((WPMainActivity) getActivity()).showQuickStartSnackBar(snackbar);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag);
        }

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mTagPreviewHistory.saveInstance(outState);
        } else if (getPostListType() == ReaderPostListType.SEARCH_RESULTS
                   && mSearchView != null
                   && mSearchView.getQuery() != null) {
            String query = mSearchView.getQuery().toString();
            outState.putString(ReaderConstants.ARG_SEARCH_QUERY, query);
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId);
        outState.putLong(ReaderConstants.ARG_FEED_ID, mCurrentFeedId);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasUpdatedPosts);
        outState.putBoolean(ReaderConstants.KEY_FIRST_LOAD, mFirstLoad);
        outState.putBoolean(ReaderConstants.KEY_IS_REFRESHING, mRecyclerView.isRefreshing());
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        outState.putParcelable(QuickStartEvent.KEY, mQuickStartEvent);

        if (isSearchTabsShowing()) {
            int tabPosition = getSearchTabsPosition();
            outState.putInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, tabPosition);
            int siteSearchPosition = tabPosition == TAB_SITES ? getCurrentPosition() : mSiteSearchAdapterPos;
            outState.putInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION, siteSearchPosition);
        }

        super.onSaveInstanceState(outState);
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasPostAdapter()) {
            return mRecyclerView.getCurrentPosition();
        } else {
            return -1;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards, container, false);
        mRecyclerView = rootView.findViewById(R.id.reader_recycler_view);

        Context context = container.getContext();

        mActionableEmptyView = rootView.findViewById(R.id.empty_custom_view);

        mRecyclerView.setLogT(AppLog.T.READER);
        mRecyclerView.setCustomEmptyView(mActionableEmptyView);
        mRecyclerView.setFilterListener(new FilteredRecyclerView.FilterListener() {
            @Override
            public List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh) {
                return null;
            }

            @Override
            public void onLoadFilterCriteriaOptionsAsync(
                    FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener, boolean refresh) {
                loadTags(listener);
            }

            @Override
            public void onLoadData() {
                if (!isAdded()) {
                    return;
                }
                if (!NetworkUtils.checkConnection(getActivity())) {
                    mRecyclerView.setRefreshing(false);
                    return;
                }

                if (mFirstLoad) {
                    // let onResume() take care of this logic, as the FilteredRecyclerView.FilterListener onLoadData
                    // method is called on two moments: once for first time load, and then each time the swipe to
                    // refresh gesture triggers a refresh.
                    mRecyclerView.setRefreshing(false);
                    mFirstLoad = false;
                } else {
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                            // fall through to TAG_PREVIEW
                        case TAG_PREVIEW:
                            updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
                            break;
                        case BLOG_PREVIEW:
                            updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
                            break;
                        case SEARCH_RESULTS:
                            // no-op
                            break;
                    }
                    // make sure swipe-to-refresh progress shows since this is a manual refresh
                    mRecyclerView.setRefreshing(true);
                }

                if (getCurrentTag() != null && getCurrentTag().isBookmarked()) {
                    ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();
                    refreshPosts();
                }
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                onTagChanged((ReaderTag) criteria);
            }

            @Override
            public FilterCriteria onRecallSelection() {
                if (hasCurrentTag()) {
                    return getCurrentTag();
                } else {
                    AppLog.w(T.READER, "reader post list > no current tag in onRecallSelection");
                    return ReaderUtils.getDefaultTag();
                }
            }

            @Override
            public String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType) {
                return null;
            }

            @Override
            public void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType) {
                setEmptyTitleDescriptionAndButton(
                        EmptyViewMessageType.NETWORK_ERROR.equals(emptyViewMsgType)
                        || EmptyViewMessageType.PERMISSION_ERROR.equals(emptyViewMsgType)
                        || EmptyViewMessageType.GENERIC_ERROR.equals(emptyViewMsgType));
            }
        });

        // add the item decoration (dividers) to the recycler, skipping the first item if the first
        // item is the tag toolbar (shown when viewing posts in followed tags) - this is to avoid
        // having the tag toolbar take up more vertical space than necessary
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical, false));

        // the following will change the look and feel of the toolbar to match the current design
        mRecyclerView.setToolbarBackgroundColor(ContextCompat.getColor(context, R.color.color_primary));
        mRecyclerView.setToolbarSpinnerTextColor(ContextCompat.getColor(context, R.color.white));
        mRecyclerView.setToolbarSpinnerDrawable(R.drawable.ic_dropdown_blue_light_24dp);
        mRecyclerView.setToolbarLeftAndRightPadding(
                getResources().getDimensionPixelSize(R.dimen.margin_medium),
                getResources().getDimensionPixelSize(R.dimen.margin_extra_large));

        // add a menu to the filtered recycler's toolbar
        if (mAccountStore.hasAccessToken() && (getPostListType() == ReaderPostListType.TAG_FOLLOWED
                                               || getPostListType() == ReaderPostListType.SEARCH_RESULTS)) {
            setupRecyclerToolbar();
        }

        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported());

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.scrollRecycleViewToPosition(0);
                refreshPosts();
            }
        });

        // progress bar that appears when loading more posts
        mProgress = rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        if (savedInstanceState != null && savedInstanceState.getBoolean(ReaderConstants.KEY_IS_REFRESHING)) {
            mIsUpdating = true;
            mRecyclerView.setRefreshing(true);
        }

        return rootView;
    }

    /*
     * adds a menu to the recycler's toolbar containing settings & search items - only called
     * for followed tags
     */
    private void setupRecyclerToolbar() {
        Menu menu = mRecyclerView.addToolbarMenu(R.menu.reader_list);
        mSettingsMenuItem = menu.findItem(R.id.menu_reader_settings);
        mSearchMenuItem = menu.findItem(R.id.menu_reader_search);

        mSettingsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ReaderActivityLauncher.showReaderSubs(getActivity());
                return true;
            }
        });

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setQueryHint(getString(R.string.reader_hint_post_search));
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setIconified(true);

        // force the search view to take up as much horizontal space as possible (without this
        // it looks truncated on landscape)
        int maxWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
        mSearchView.setMaxWidth(maxWidth);

        // this is hacky, but we want to change the SearchView's autocomplete to show suggestions
        // after a single character is typed, and there's no less hacky way to do this...
        View view = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        if (view instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) view).setThreshold(1);
        }

        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED);
                }
                resetPostAdapter(ReaderPostListType.SEARCH_RESULTS);
                showSearchMessage();
                mSettingsMenuItem.setVisible(false);

                // hide the bottom navigation when search is active
                if (mBottomNavController != null) {
                    mBottomNavController.onRequestHideBottomNavigation();
                }

                if (getSelectedSite() != null) {
                    QuickStartUtils.completeTaskAndRemindNextOne(mQuickStartStore, QuickStartTask.FOLLOW_SITE,
                            mDispatcher, getSelectedSite(), mQuickStartEvent, getContext());
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                hideSearchMessage();
                hideSearchTabs();
                resetSearchSuggestionAdapter();
                mSettingsMenuItem.setVisible(true);
                mCurrentSearchQuery = null;

                if (mBottomNavController != null) {
                    mBottomNavController.onRequestShowBottomNavigation();
                }

                // return to the followed tag that was showing prior to searching
                resetPostAdapter(ReaderPostListType.TAG_FOLLOWED);

                return true;
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    submitSearchQuery(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (TextUtils.isEmpty(newText)) {
                        showSearchMessage();
                        hideSearchTabs();
                    } else {
                        populateSearchSuggestionAdapter(newText);
                    }
                    return true;
                    }
                }
        );
    }

    /*
     * start the search service to search for posts matching the current query - the passed
     * offset is used during infinite scroll, pass zero for initial search
     */
    private void updatePostsInCurrentSearch(int offset) {
        ReaderSearchServiceStarter.startService(getActivity(), mCurrentSearchQuery, offset);
    }

    /*
     * start a search for reader sites matching the current search query
     */
    private void updateSitesInCurrentSearch(int offset) {
        if (getSearchTabsPosition() == TAB_SITES) {
            if (offset == 0) {
                mRecyclerView.setRefreshing(true);
            } else {
                showLoadingProgress(true);
            }
        }
        ReaderSearchSitesPayload payload = new ReaderSearchSitesPayload(
                mCurrentSearchQuery,
                ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
                offset,
                false);
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload));
    }

    private void submitSearchQuery(@NonNull String query) {
        if (!isAdded()) {
            return;
        }

        mSearchView.clearFocus(); // this will hide suggestions and the virtual keyboard
        hideSearchMessage();

        // remember this query for future suggestions
        String trimQuery = query.trim();
        ReaderSearchTable.addOrUpdateQueryString(trimQuery);

        // remove cached results for this search - search results are ephemeral so each search
        // should be treated as a "fresh" one
        ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(trimQuery);
        ReaderPostTable.deletePostsWithTag(searchTag);

        mPostAdapter.setCurrentTag(searchTag);
        mCurrentSearchQuery = trimQuery;
        updatePostsInCurrentSearch(0);
        updateSitesInCurrentSearch(0);

        // track that the user performed a search
        if (!trimQuery.equals("")) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("query", trimQuery);
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, properties);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReaderSitesSearched(OnReaderSitesSearched event) {
        if (!isAdded()) {
            return;
        }

        if (!isUpdating()) {
            mRecyclerView.setRefreshing(false);
        }
        showLoadingProgress(false);

        ReaderSiteSearchAdapter adapter = getSiteSearchAdapter();
        if (event.isError()) {
            adapter.clear();
        } else if (StringUtils.equals(event.searchTerm, mCurrentSearchQuery)) {
            adapter.setCanLoadMore(event.canLoadMore);
            if (event.offset == 0) {
                adapter.setSiteList(event.sites);
            } else {
                adapter.addSiteList(event.sites);
            }
            if (mSiteSearchRestorePosition > 0) {
                mRecyclerView.scrollRecycleViewToPosition(mSiteSearchRestorePosition);
            }
        }

        if (getSearchTabsPosition() == TAB_SITES && adapter.isEmpty()) {
            setEmptyTitleDescriptionAndButton(event.isError());
            showEmptyView();
        }

        mSiteSearchRestorePosition = 0;
    }

    /*
     * reuse "empty" view to let user know what they're querying
     */
    private void showSearchMessage() {
        if (!isAdded()) {
            return;
        }

        // clear posts and sites so only the empty view is visible
        getPostAdapter().clear();
        getSiteSearchAdapter().clear();

        setEmptyTitleDescriptionAndButton(false);
        showEmptyView();
    }

    private void hideSearchMessage() {
        hideEmptyView();
    }

    /*
     * create the TabLayout that separates search results between POSTS and SITES and places it below
     * the FilteredRecyclerView's toolbar
     */
    private void createSearchTabs() {
        if (mSearchTabs == null) {
            ViewGroup rootView = getView().findViewById(android.R.id.content);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            mSearchTabs = (TabLayout) inflater.inflate(R.layout.reader_search_tabs, rootView);
            mSearchTabs.setVisibility(View.GONE);
            mRecyclerView.getAppBarLayout().addView(mSearchTabs);
        }
    }

    private boolean isSearchTabsShowing() {
        return mSearchTabs != null && mSearchTabs.getVisibility() == View.VISIBLE;
    }

    private void showSearchTabs() {
        if (!isAdded()) {
            return;
        }
        if (mSearchTabs == null) {
            createSearchTabs();
        }
        if (mSearchTabs.getVisibility() != View.VISIBLE) {
            mSearchTabs.setVisibility(View.VISIBLE);

            mPostSearchAdapterPos = 0;
            mSiteSearchAdapterPos = 0;

            mSearchTabs.addOnTabSelectedListener(new OnTabSelectedListener() {
                @Override public void onTabSelected(Tab tab) {
                    if (tab.getPosition() == TAB_POSTS) {
                        mRecyclerView.setAdapter(getPostAdapter());
                        if (mPostSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mPostSearchAdapterPos);
                        }
                        if (getPostAdapter().isEmpty()) {
                            setEmptyTitleDescriptionAndButton(false);
                            showEmptyView();
                        } else {
                            hideEmptyView();
                        }
                    } else if (tab.getPosition() == TAB_SITES) {
                        mRecyclerView.setAdapter(getSiteSearchAdapter());
                        if (mSiteSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mSiteSearchAdapterPos);
                        }
                        if (getSiteSearchAdapter().isEmpty()) {
                            setEmptyTitleDescriptionAndButton(false);
                            showEmptyView();
                        } else {
                            hideEmptyView();
                        }
                    }
                }

                @Override public void onTabUnselected(Tab tab) {
                    if (tab.getPosition() == TAB_POSTS) {
                        mPostSearchAdapterPos = mRecyclerView.getCurrentPosition();
                    } else if (tab.getPosition() == TAB_SITES) {
                        mSiteSearchAdapterPos = mRecyclerView.getCurrentPosition();
                    }
                }

                @Override public void onTabReselected(Tab tab) {
                    mRecyclerView.smoothScrollToPosition(0);
                }
            });

            if (mSearchTabsPos != NO_POSITION && mSearchTabsPos != mSearchTabs.getSelectedTabPosition()) {
                Tab tab = mSearchTabs.getTabAt(mSearchTabsPos);
                if (tab != null) {
                    tab.select();
                }
                mSearchTabsPos = NO_POSITION;
            }
        }
    }

    private void hideSearchTabs() {
        if (isAdded() && mSearchTabs != null && mSearchTabs.getVisibility() == View.VISIBLE) {
            mSearchTabs.setVisibility(View.GONE);
            mSearchTabs.clearOnTabSelectedListeners();
            if (mSearchTabs.getSelectedTabPosition() != TAB_POSTS) {
                mSearchTabs.getTabAt(TAB_POSTS).select();
            }
            mRecyclerView.setAdapter(getPostAdapter());
            mLastTappedSiteSearchResult = null;
            showLoadingProgress(false);
        }
    }

    private int getSearchTabsPosition() {
        return isSearchTabsShowing() ? mSearchTabs.getSelectedTabPosition() : -1;
    }

    /*
     * create and assign the suggestion adapter for the search view
     */
    private void createSearchSuggestionAdapter() {
        mSearchSuggestionAdapter = new ReaderSearchSuggestionAdapter(getActivity());
        mSearchView.setSuggestionsAdapter(mSearchSuggestionAdapter);

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String query = mSearchSuggestionAdapter.getSuggestion(position);
                if (!TextUtils.isEmpty(query)) {
                    mSearchView.setQuery(query, true);
                }
                return true;
            }
        });
    }

    private void populateSearchSuggestionAdapter(String query) {
        if (mSearchSuggestionAdapter == null) {
            createSearchSuggestionAdapter();
        }
        mSearchSuggestionAdapter.setFilter(query);
    }

    private void resetSearchSuggestionAdapter() {
        mSearchView.setSuggestionsAdapter(null);
        mSearchSuggestionAdapter = null;
    }

    /*
     * is the search input showing?
     */
    private boolean isSearchViewExpanded() {
        return mSearchView != null && !mSearchView.isIconified();
    }

    private boolean isSearchViewEmpty() {
        return mSearchView != null && mSearchView.getQuery().length() == 0;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.SearchPostsStarted event) {
        if (!isAdded()) {
            return;
        }

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(true, updateAction);
        setEmptyTitleDescriptionAndButton(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.SearchPostsEnded event) {
        if (!isAdded()) {
            return;
        }

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(false, updateAction);

        // load the results if the search succeeded and it's the current search - note that success
        // means the search didn't fail, not necessarily that is has results - which is fine because
        // if there aren't results then refreshing will show the empty message
        if (event.didSucceed()
            && getPostListType() == ReaderPostListType.SEARCH_RESULTS
            && event.getQuery().equals(mCurrentSearchQuery)) {
            refreshPosts();
            showSearchTabs();
        } else {
            hideSearchTabs();
        }
    }

    /*
     * called when user taps follow item in popup menu for a post
     */
    private void toggleFollowStatusForPost(final ReaderPost post) {
        if (post == null
            || !hasPostAdapter()
            || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        final boolean isAskingToFollow = !ReaderPostTable.isPostFollowed(post);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isAdded() && !succeeded) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog
                            : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(getActivity(), resId);
                    getPostAdapter().setFollowStatusForBlog(post.blogId, !isAskingToFollow);
                }
            }
        };

        if (ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)) {
            getPostAdapter().setFollowStatusForBlog(post.blogId, isAskingToFollow);
        }
    }

    /*
     * blocks the blog associated with the passed post and removes all posts in that blog
     * from the adapter
     */
    private void blockBlogForPost(final ReaderPost post) {
        if (post == null
            || !isAdded()
            || !hasPostAdapter()
            || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_block_blog, ToastUtils.Duration.LONG);
                }
            }
        };

        // perform call to block this blog - returns list of posts deleted by blocking so
        // they can be restored if the user undoes the block
        final BlockedBlogResult blockResult = ReaderBlogActions.blockBlogFromReader(post.blogId, actionListener);
        AnalyticsUtils.trackWithSiteId(AnalyticsTracker.Stat.READER_BLOG_BLOCKED, post.blogId);

        // remove posts in this blog from the adapter
        getPostAdapter().removePostsInBlog(post.blogId);

        // show the undo snackbar enabling the user to undo the block
        View.OnClickListener undoListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderBlogActions.undoBlockBlogFromReader(blockResult);
                refreshPosts();
            }
        };
        Snackbar.make(getSnackbarParent(), getString(R.string.reader_toast_blog_blocked),
                AccessibilityUtils.getSnackbarDuration(getActivity()))
                .setAction(R.string.undo, undoListener)
                .show();
    }

    /*
     * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
     * parent to be the main activity's CoordinatorLayout
     */
    private View getSnackbarParent() {
        View coordinator = getActivity().findViewById(R.id.coordinator);
        if (coordinator != null) {
            return coordinator;
        }
        return getView();
    }

    private void setEmptyTitleDescriptionAndButton(boolean requestFailed) {
        if (!isAdded()) {
            return;
        }

        int heightToolbar = getActivity().getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        int heightTabs = getActivity().getResources().getDimensionPixelSize(R.dimen.tab_height);
        mActionableEmptyView.updateLayoutForSearch(false, heightToolbar);
        boolean isSearching = false;
        String title;
        String description = null;
        ActionableEmptyViewButtonType button = null;

        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && getCurrentTag().isBookmarked()) {
            setEmptyTitleAndDescriptionForBookmarksList();
            return;
        } else if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            title = getString(R.string.reader_empty_posts_no_connection);
        } else if (requestFailed) {
            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                title = getString(R.string.reader_empty_search_request_failed);
            } else {
                title = getString(R.string.reader_empty_posts_request_failed);
            }
        } else if (isUpdating() && getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            title = getString(R.string.reader_empty_posts_in_tag_updating);
        } else {
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                    if (getCurrentTag().isFollowedSites()) {
                        if (ReaderBlogTable.hasFollowedBlogs()) {
                            title = getString(R.string.reader_empty_followed_blogs_no_recent_posts_title);
                            description = getString(R.string.reader_empty_followed_blogs_no_recent_posts_description);
                        } else {
                            title = getString(R.string.reader_empty_followed_blogs_title);
                            description = getString(R.string.reader_empty_followed_blogs_description);
                        }

                        button = ActionableEmptyViewButtonType.DISCOVER;
                    } else if (getCurrentTag().isPostsILike()) {
                        title = getString(R.string.reader_empty_posts_liked_title);
                        description = getString(R.string.reader_empty_posts_liked_description);
                        button = ActionableEmptyViewButtonType.FOLLOWED;
                    } else if (getCurrentTag().isListTopic()) {
                        title = getString(R.string.reader_empty_posts_in_custom_list);
                    } else {
                        title = getString(R.string.reader_empty_posts_in_tag);
                    }
                    break;
                case BLOG_PREVIEW:
                    title = getString(R.string.reader_empty_posts_in_blog);
                    break;
                case SEARCH_RESULTS:
                    isSearching = true;

                    if (isSearchViewEmpty() || TextUtils.isEmpty(mCurrentSearchQuery)) {
                        title = getString(R.string.reader_label_post_search_explainer);
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar);
                    } else if (isUpdating()) {
                        title = "";
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar);
                    } else {
                        title = getString(R.string.reader_empty_search_title);
                        String formattedQuery = "<em>" + mCurrentSearchQuery + "</em>";
                        description = String.format(getString(R.string.reader_empty_search_description),
                                formattedQuery);
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar + heightTabs);
                    }
                    break;
                case TAG_PREVIEW:
                    // fall through to the default case
                default:
                    title = getString(R.string.reader_empty_posts_in_tag);
                    break;
            }
        }

        setEmptyTitleDescriptionAndButton(title, description, button, isSearching);
    }

    /*
     * Currently, only local bookmarks are supported.  Show an empty view if the local database has no data.
     */
    private void setEmptyTitleAndDescriptionForBookmarksList() {
        // replace %s placeholder with bookmark outline icon
        String description = getString(R.string.reader_empty_saved_posts_description);
        SpannableStringBuilder ssb = new SpannableStringBuilder(description);
        int imagePlaceholderPosition = description.indexOf("%s");
        addBookmarkImageSpan(ssb, imagePlaceholderPosition);

        mActionableEmptyView.image.setVisibility(View.VISIBLE);
        mActionableEmptyView.title.setText(R.string.reader_empty_saved_posts_title);
        mActionableEmptyView.subtitle.setText(ssb);
        mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);
        mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_followed);
        mActionableEmptyView.button.setVisibility(View.VISIBLE);
        mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                setCurrentTagFromEmptyViewButton(ActionableEmptyViewButtonType.FOLLOWED);
            }
        });
    }

    private void addBookmarkImageSpan(SpannableStringBuilder ssb, int imagePlaceholderPosition) {
        Drawable d = ContextCompat.getDrawable(getActivity(), R.drawable.ic_bookmark_grey_dark_18dp);
        d.setBounds(0, 0, (int) (d.getIntrinsicWidth() * 1.2), (int) (d.getIntrinsicHeight() * 1.2));
        ssb.setSpan(new ImageSpan(d), imagePlaceholderPosition, imagePlaceholderPosition + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setEmptyTitleDescriptionAndButton(@NonNull String title, String description,
                                                   final ActionableEmptyViewButtonType button, boolean isSearching) {
        if (!isAdded()) {
            return;
        }

        mActionableEmptyView.image.setVisibility(!isUpdating() && !isSearching ? View.VISIBLE : View.GONE);
        mActionableEmptyView.title.setText(title);

        if (description == null) {
            mActionableEmptyView.subtitle.setVisibility(View.GONE);
        } else {
            mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);

            if (description.contains("<") && description.contains(">")) {
                mActionableEmptyView.subtitle.setText(Html.fromHtml(description));
            } else {
                mActionableEmptyView.subtitle.setText(description);
            }
        }

        if (button == null) {
            mActionableEmptyView.button.setVisibility(View.GONE);
        } else {
            mActionableEmptyView.button.setVisibility(View.VISIBLE);

            switch (button) {
                case DISCOVER:
                    mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_discover);
                    break;
                case FOLLOWED:
                    mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_followed);
                    break;
            }

            mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    setCurrentTagFromEmptyViewButton(button);
                }
            });
        }
    }

    private void showEmptyView() {
        if (isAdded()) {
            mActionableEmptyView.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyView() {
        if (isAdded()) {
            mActionableEmptyView.setVisibility(View.GONE);
        }
    }

    private boolean isEmptyViewShowing() {
        return isAdded() && mActionableEmptyView.getVisibility() == View.VISIBLE;
    }

    private void setCurrentTagFromEmptyViewButton(ActionableEmptyViewButtonType button) {
        ReaderTag tag;

        switch (button) {
            case DISCOVER:
                tag = ReaderUtils.getTagFromEndpoint(ReaderTag.DISCOVER_PATH);
                break;
            case FOLLOWED:
                tag = ReaderUtils.getTagFromEndpoint(ReaderTag.FOLLOWING_PATH);
                break;
            default:
                tag = ReaderUtils.getDefaultTag();
        }

        mRecyclerView.refreshFilterCriteriaOptions();

        if (!ReaderTagTable.tagExists(tag)) {
            tag = ReaderTagTable.getFirstTag();
        }

        setCurrentTag(tag);
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderInterfaces.DataLoadedListener mDataLoadedListener = new ReaderInterfaces.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!isAdded()) {
                return;
            }
            if (isEmpty) {
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS
                    || getSearchTabsPosition() == TAB_SITES && getSiteSearchAdapter().isEmpty()
                    || getSearchTabsPosition() == TAB_POSTS && getPostAdapter().isEmpty()) {
                    setEmptyTitleDescriptionAndButton(false);
                    showEmptyView();
                }
            } else {
                hideEmptyView();
                if (mRestorePosition > 0) {
                    AppLog.d(T.READER, "reader post list > restoring position");
                    mRecyclerView.scrollRecycleViewToPosition(mRestorePosition);
                }
                if (getPostListType() == ReaderPostListType.SEARCH_RESULTS && !isSearchTabsShowing()) {
                    showSearchTabs();
                }
            }
            mRestorePosition = 0;
        }
    };

    private final ReaderInterfaces.OnPostBookmarkedListener mOnPostBookmarkedListener =
            new ReaderInterfaces.OnPostBookmarkedListener() {
                @Override public void onBookmarkedStateChanged(boolean isBookmarked, long blogId, long postId,
                                                               boolean isCachingActionRequired) {
                    if (!isAdded()) {
                        return;
                    }

                    String tag = Long.toString(blogId) + Long.toString(postId);

                    if (NetworkUtils.isNetworkAvailable(getActivity())
                        && isCachingActionRequired && isBookmarked
                        && getFragmentManager().findFragmentByTag(tag) == null) {
                        getFragmentManager().beginTransaction()
                                            .add(ReaderPostWebViewCachingFragment.newInstance(blogId, postId), tag)
                                            .commit();
                    }

                    if (isBookmarked && !isBookmarksList()) {
                        if (AppPrefs.shouldShowBookmarksSavedLocallyDialog()) {
                            AppPrefs.setBookmarksSavedLocallyDialogShown();
                            showBookmarksSavedLocallyDialog();
                        } else {
                            // show snackbar when not in saved posts list
                            showBookmarkSnackbar();
                        }
                    }
                }
            };

    private void showBookmarksSavedLocallyDialog() {
        mBookmarksSavedLocallyDialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.reader_save_posts_locally_dialog_title))
                .setMessage(getString(R.string.reader_save_posts_locally_dialog_message))
                .setPositiveButton(R.string.dialog_button_ok, new OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        showBookmarkSnackbar();
                    }
                })
                .setCancelable(false)
                .create();
        mBookmarksSavedLocallyDialog.show();
    }

    private boolean isBookmarksList() {
        return getPostListType() == ReaderPostListType.TAG_FOLLOWED
               && (mCurrentTag != null && mCurrentTag.isBookmarked());
    }

    private void showBookmarkSnackbar() {
        if (!isAdded()) {
            return;
        }

        Snackbar.make(getView(), R.string.reader_bookmark_snack_title,
                AccessibilityUtils.getSnackbarDuration(getActivity())).setAction(R.string.reader_bookmark_snack_btn,
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        AnalyticsTracker
                                .track(AnalyticsTracker.Stat.READER_SAVED_LIST_VIEWED_FROM_POST_LIST_NOTICE);
                        ActivityLauncher.viewSavedPostsListInReader(getActivity());
                        if (getActivity() instanceof WPMainActivity) {
                            getActivity().overridePendingTransition(0, 0);
                        }
                    }
                })
                .show();
    }

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener =
            new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData() {
                    // skip if update is already in progress
                    if (isUpdating()) {
                        return;
                    }

                    // request older posts unless we already have the max # to show
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                            // fall through to TAG_PREVIEW
                        case TAG_PREVIEW:
                            if (ReaderPostTable.getNumPostsWithTag(mCurrentTag)
                                < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                // request older posts
                                updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_OLDER);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;

                        case BLOG_PREVIEW:
                            int numPosts;
                            if (mCurrentFeedId != 0) {
                                numPosts = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                            } else {
                                numPosts = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                            }
                            if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_OLDER);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;

                        case SEARCH_RESULTS:
                            ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(mCurrentSearchQuery);
                            int offset = ReaderPostTable.getNumPostsWithTag(searchTag);
                            if (offset < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                updatePostsInCurrentSearch(offset);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;
                    }
                }
            };

    private final NewsCardListener mNewsCardListener = new NewsCardListener() {
        @Override public void onItemShown(@NotNull NewsItem item) {
            mViewModel.onNewsCardShown(item, getCurrentTag());
        }

        @Override public void onItemClicked(@NotNull NewsItem item) {
            mViewModel.onNewsCardExtendedInfoRequested(item);
            Activity activity = getActivity();
            if (activity != null) {
                WPWebViewActivity.openURL(activity, item.getActionUrl());
            }
        }

        @Override public void onDismissClicked(NewsItem item) {
            mViewModel.onNewsCardDismissed(item);
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating post adapter");
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mPostAdapter = new ReaderPostAdapter(context, getPostListType(), mImageManager);
            mPostAdapter.setOnFollowListener(this);
            mPostAdapter.setOnPostSelectedListener(this);
            mPostAdapter.setOnPostPopupListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            mPostAdapter.setOnPostBookmarkedListener(mOnPostBookmarkedListener);
            mPostAdapter.setOnNewsCardListener(mNewsCardListener);
            if (getActivity() instanceof ReaderSiteHeaderView.OnBlogInfoLoadedListener) {
                mPostAdapter.setOnBlogInfoLoadedListener((ReaderSiteHeaderView.OnBlogInfoLoadedListener) getActivity());
            }
            mViewModel.getNewsDataSource().removeObserver(mNewsItemObserver);
            if (getPostListType().isTagType()) {
                mPostAdapter.setCurrentTag(getCurrentTag());
                mViewModel.getNewsDataSource().observe((FragmentActivity) getActivity(), mNewsItemObserver);
            } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                mPostAdapter.setCurrentBlogAndFeed(mCurrentBlogId, mCurrentFeedId);
            } else if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(mCurrentSearchQuery);
                mPostAdapter.setCurrentTag(searchTag);
            }
        }
        return mPostAdapter;
    }

    private ReaderSiteSearchAdapter getSiteSearchAdapter() {
        if (mSiteSearchAdapter == null) {
            mSiteSearchAdapter = new ReaderSiteSearchAdapter(new SiteSearchAdapterListener() {
                @Override
                public void onSiteClicked(@NonNull ReaderSiteModel site) {
                    mLastTappedSiteSearchResult = site;
                    ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                            getActivity(), site.getSiteId(), site.getFeedId());
                }

                @Override
                public void onLoadMore(int offset) {
                    showLoadingProgress(true);
                    updateSitesInCurrentSearch(offset);
                }
            });
        }
        return mSiteSearchAdapter;
    }

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    private boolean isPostAdapterEmpty() {
        return (mPostAdapter == null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTag(final ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    private boolean isCurrentTagName(String tagName) {
        return (tagName != null && tagName.equalsIgnoreCase(getCurrentTagName()));
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private String getCurrentTagName() {
        return (mCurrentTag != null ? mCurrentTag.getTagSlug() : "");
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private void setCurrentTag(final ReaderTag tag) {
        if (tag == null) {
            return;
        }

        // skip if this is already the current tag and the post adapter is already showing it
        if (isCurrentTag(tag)
            && hasPostAdapter()
            && getPostAdapter().isCurrentTag(tag)) {
            return;
        }

        mCurrentTag = tag;
        mViewModel.onTagChanged(tag);

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                AppPrefs.setReaderTag(tag);
                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagSlug());
                break;
            case BLOG_PREVIEW:
                // noop
                break;
            case SEARCH_RESULTS:
                // noop
                break;
        }

        getPostAdapter().setCurrentTag(tag);
        hideNewPostsBar();
        showLoadingProgress(false);

        updateCurrentTagIfTime();
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    @Override
    public boolean onActivityBackPressed() {
        if (isSearchViewExpanded()) {
            mSearchMenuItem.collapseActionView();
            return true;
        } else {
            return goBackInTagHistory();
        }
    }

    /*
     * when previewing posts with a specific tag, a history of previewed tags is retained so
     * the user can navigate back through them - this is faster and requires less memory
     * than creating a new fragment for each previewed tag
     */
    private boolean goBackInTagHistory() {
        if (mTagPreviewHistory.empty()) {
            return false;
        }

        String tagName = mTagPreviewHistory.pop();
        if (isCurrentTagName(tagName)) {
            if (mTagPreviewHistory.empty()) {
                return false;
            }
            tagName = mTagPreviewHistory.pop();
        }

        ReaderTag newTag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED);
        setCurrentTag(newTag);

        return true;
    }

    /*
     * load tags on which the main data will be filtered
     */
    private void loadTags(FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener) {
        new LoadTagsTask(listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
        hideNewPostsBar();
        if (hasPostAdapter()) {
            getPostAdapter().refresh();
        }
    }

    /*
     * same as above but clears posts before refreshing
     */
    private void reloadPosts() {
        hideNewPostsBar();
        if (hasPostAdapter()) {
            getPostAdapter().reload();
        }
    }

    /*
     * reload the list of tags for the dropdown filter
     */
    private void reloadTags() {
        if (isAdded() && mRecyclerView != null) {
            mRecyclerView.refreshFilterCriteriaOptions();
        }
    }

    /*
     * get posts for the current blog from the server
     */
    private void updatePostsInCurrentBlogOrFeed(final UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled blog update");
            return;
        }
        if (mCurrentFeedId != 0) {
            ReaderPostServiceStarter.startServiceForFeed(getActivity(), mCurrentFeedId, updateAction);
        } else {
            ReaderPostServiceStarter.startServiceForBlog(getActivity(), mCurrentBlogId, updateAction);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (!isAdded()) {
            return;
        }

        setIsUpdating(true, event.getAction());
        setEmptyTitleDescriptionAndButton(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (!isAdded()) {
            return;
        }

        setIsUpdating(false, event.getAction());
        if (event.getReaderTag() != null && !isCurrentTag(event.getReaderTag())) {
            return;
        }

        // don't show new posts if user is searching - posts will automatically
        // appear when search is exited
        if (isSearchViewExpanded()
            || getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
            return;
        }

        // determine whether to show the "new posts" bar - when this is shown, the newly
        // downloaded posts aren't displayed until the user taps the bar - only appears
        // when there are new posts in a followed tag and the user has scrolled the list
        // beyond the first post
        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW
            && event.getAction() == UpdateAction.REQUEST_NEWER
            && getPostListType() == ReaderPostListType.TAG_FOLLOWED
            && !isPostAdapterEmpty()
            && (!isAdded() || !mRecyclerView.isFirstItemVisible())) {
            showNewPostsBar();
        } else if (event.getResult().isNewOrChanged()) {
            refreshPosts();
        } else {
            boolean requestFailed = (event.getResult() == ReaderActions.UpdateResult.FAILED);
            setEmptyTitleDescriptionAndButton(requestFailed);
            // if we requested posts in order to fill a gap but the request failed or didn't
            // return any posts, reload the adapter so the gap marker is reset (hiding its
            // progress bar)
            if (event.getAction() == UpdateAction.REQUEST_OLDER_THAN_GAP) {
                reloadPosts();
            }
        }
    }

    /*
     * get latest posts for this tag from the server
     */
    private void updatePostsWithTag(ReaderTag tag, UpdateAction updateAction) {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        if (tag == null) {
            AppLog.w(T.READER, "null tag passed to updatePostsWithTag");
            return;
        }
        AppLog.d(T.READER,
                "reader post list > updating tag " + tag.getTagNameForLog() + ", updateAction=" + updateAction.name());
        ReaderPostServiceStarter.startServiceForTag(getActivity(), tag, updateAction);
    }

    private void updateCurrentTag() {
        updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
    }

    /*
     * update the current tag if it's time to do so - note that the check is done in the
     * background since it can be expensive and this is called when the fragment is
     * resumed, which on slower devices can result in a janky experience
     */
    private void updateCurrentTagIfTime() {
        if (!isAdded() || !hasCurrentTag()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                if (ReaderTagTable.shouldAutoUpdateTag(getCurrentTag()) && isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCurrentTag();
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isUpdating() {
        return mIsUpdating;
    }

    /*
     * show/hide progress bar which appears at the bottom of the activity when loading more posts
     */
    private void showLoadingProgress(boolean showProgress) {
        if (isAdded() && mProgress != null) {
            if (showProgress) {
                mProgress.bringToFront();
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    private void setIsUpdating(boolean isUpdating, UpdateAction updateAction) {
        if (!isAdded() || mIsUpdating == isUpdating) {
            return;
        }

        if (updateAction == UpdateAction.REQUEST_OLDER) {
            // show/hide progress bar at bottom if these are older posts
            showLoadingProgress(isUpdating);
        } else if (isUpdating && isPostAdapterEmpty()) {
            // show swipe-to-refresh if update started and no posts are showing
            mRecyclerView.setRefreshing(true);
        } else if (!isUpdating) {
            // hide swipe-to-refresh progress if update is complete
            mRecyclerView.setRefreshing(false);
        }
        mIsUpdating = isUpdating;

        // if swipe-to-refresh isn't active, keep it disabled during an update - this prevents
        // doing a refresh while another update is already in progress
        if (mRecyclerView != null && !mRecyclerView.isRefreshing()) {
            mRecyclerView.setSwipeToRefreshEnabled(!isUpdating && isSwipeToRefreshSupported());
        }
    }

    /*
     * swipe-to-refresh isn't supported for search results since they're really brief snapshots
     * and are unlikely to show new posts due to the way they're sorted
     */
    private boolean isSwipeToRefreshSupported() {
        return getPostListType() != ReaderPostListType.SEARCH_RESULTS;
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private boolean isNewPostsBarShowing() {
        return (mNewPostsBar != null && mNewPostsBar.getVisibility() == View.VISIBLE);
    }

    /*
     * scroll listener assigned to the recycler when the "new posts" bar is shown to hide
     * it upon scrolling
     */
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            hideNewPostsBar();
        }
    };

    private void showNewPostsBar() {
        if (!isAdded() || isNewPostsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);

        // assign the scroll listener to hide the bar when the recycler is scrolled, but don't assign
        // it right away since the user may be scrolling when the bar appears (which would cause it
        // to disappear as soon as it's displayed)
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && isNewPostsBarShowing()) {
                    mRecyclerView.addOnScrollListener(mOnScrollListener);
                }
            }
        }, 1000L);

        // remove the gap marker if it's showing, since it's no longer valid
        getPostAdapter().removeGapMarker();
    }

    private void hideNewPostsBar() {
        if (!isAdded() || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return;
        }

        mIsAnimatingOutNewPostsBar = true;

        // remove the onScrollListener assigned in showNewPostsBar()
        mRecyclerView.removeOnScrollListener(mOnScrollListener);

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isAdded()) {
                    mNewPostsBar.setVisibility(View.GONE);
                    mIsAnimatingOutNewPostsBar = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    /*
     * called from adapter when user taps a post
     */
    @Override
    public void onPostSelected(ReaderPost post) {
        if (!isAdded() || post == null) {
            return;
        }

        AppRatingDialog.INSTANCE.incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST);

        if (post.isBookmarked) {
            if (isBookmarksList()) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST);
            } else {
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST);
            }
        }

        // "discover" posts that highlight another post should open the original (source) post when tapped
        if (post.isDiscoverPost()) {
            ReaderPostDiscoverData discoverData = post.getDiscoverData();
            if (discoverData != null
                && discoverData.getDiscoverType() == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK) {
                if (discoverData.getBlogId() != 0 && discoverData.getPostId() != 0) {
                    ReaderActivityLauncher.showReaderPostDetail(
                            getActivity(),
                            discoverData.getBlogId(),
                            discoverData.getPostId());
                    return;
                } else if (discoverData.hasPermalink()) {
                    // if we don't have a blogId/postId, we sadly resort to showing the post
                    // in a WebView activity - this will happen for non-JP self-hosted
                    ReaderActivityLauncher.openUrl(getActivity(), discoverData.getPermaLink());
                    return;
                }
            }
        }

        // if this is a cross-post, we want to show the original post
        if (post.isXpost()) {
            ReaderActivityLauncher.showReaderPostDetail(getActivity(), post.xpostBlogId, post.xpostPostId);
            return;
        }

        ReaderPostListType type = getPostListType();

        switch (type) {
            case TAG_FOLLOWED:
                // fall through to the TAG_PREVIEW
            case TAG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForTag(
                        getActivity(),
                        getCurrentTag(),
                        getPostListType(),
                        post.blogId,
                        post.postId);
                break;
            case BLOG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForBlog(
                        getActivity(),
                        post.blogId,
                        post.postId);
                break;
            case SEARCH_RESULTS:
                AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED, post);
                ReaderActivityLauncher.showReaderPostDetail(getActivity(), post.blogId, post.postId);
                break;
        }
    }

    /*
     * called when user selects a tag from the tag toolbar
     */
    private void onTagChanged(ReaderTag tag) {
        if (!isAdded() || isCurrentTag(tag)) {
            return;
        }
        // clear 'post removed from saved posts' undo items
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();
        }

        trackTagLoaded(tag);
        AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
        setCurrentTag(tag);
    }

    private void trackTagLoaded(ReaderTag tag) {
        if (tag == null) {
            return;
        }

        AnalyticsTracker.Stat stat;
        if (tag.isDiscover()) {
            stat = AnalyticsTracker.Stat.READER_DISCOVER_VIEWED;
        } else if (tag.isTagTopic()) {
            stat = AnalyticsTracker.Stat.READER_TAG_LOADED;
        } else if (tag.isListTopic()) {
            stat = AnalyticsTracker.Stat.READER_LIST_LOADED;
        } else if (tag.isBookmarked()) {
            stat = AnalyticsTracker.Stat.READER_SAVED_LIST_VIEWED_FROM_FILTER;
        } else {
            return;
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("tag", tag.getTagSlug());

        AnalyticsTracker.track(stat, properties);
    }

    /*
     * called when user taps "..." icon next to a post
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {
        if (view == null || post == null || !isAdded()) {
            return;
        }

        List<Integer> menuItems = new ArrayList<>();

        if (ReaderPostTable.isPostFollowed(post)) {
            menuItems.add(ReaderMenuAdapter.ITEM_UNFOLLOW);

            // When blogId and feedId are not equal, post is not a feed so show notifications option.
            if (post.blogId != post.feedId) {
                if (ReaderBlogTable.isNotificationsEnabled(post.blogId)) {
                    menuItems.add(ReaderMenuAdapter.ITEM_NOTIFICATIONS_OFF);
                } else {
                    menuItems.add(ReaderMenuAdapter.ITEM_NOTIFICATIONS_ON);
                }
            }
        } else {
            menuItems.add(ReaderMenuAdapter.ITEM_FOLLOW);
        }

        menuItems.add(ReaderMenuAdapter.ITEM_SHARE);

        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            menuItems.add(ReaderMenuAdapter.ITEM_BLOCK);
        }

        Context context = view.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setAdapter(new ReaderMenuAdapter(context, menuItems));
        listPopup.setDropDownGravity(Gravity.END);
        listPopup.setAnchorView(view);
        listPopup.setModal(true);
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdded()) {
                    return;
                }

                listPopup.dismiss();
                switch ((int) id) {
                    case ReaderMenuAdapter.ITEM_FOLLOW:
                        onFollowTapped(getView(), post.getBlogName(), post.blogId);
                        toggleFollowStatusForPost(post);
                        break;
                    case ReaderMenuAdapter.ITEM_UNFOLLOW:
                        onFollowingTapped();
                        toggleFollowStatusForPost(post);
                        break;
                    case ReaderMenuAdapter.ITEM_BLOCK:
                        blockBlogForPost(post);
                        break;
                    case ReaderMenuAdapter.ITEM_NOTIFICATIONS_OFF:
                        AnalyticsUtils.trackWithSiteId(Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF, post.blogId);
                        ReaderBlogTable.setNotificationsEnabledByBlogId(post.blogId, false);
                        updateSubscription(SubscriptionAction.DELETE, post.blogId);
                        break;
                    case ReaderMenuAdapter.ITEM_NOTIFICATIONS_ON:
                        AnalyticsUtils.trackWithSiteId(Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON, post.blogId);
                        ReaderBlogTable.setNotificationsEnabledByBlogId(post.blogId, true);
                        updateSubscription(SubscriptionAction.NEW, post.blogId);
                        break;
                    case ReaderMenuAdapter.ITEM_SHARE:
                        AnalyticsUtils.trackWithSiteId(Stat.SHARED_ITEM_READER, post.blogId);
                        sharePost(post);
                        break;
                }
            }
        });
        listPopup.show();
    }

    @Override
    public void onFollowTapped(View view, String blogName, final long blogId) {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());

        String blog = TextUtils.isEmpty(blogName)
                ? getString(R.string.reader_followed_blog_notifications_this)
                : blogName;

        Snackbar.make(getSnackbarParent(), Html.fromHtml(getString(R.string.reader_followed_blog_notifications,
                "<b>", blog, "</b>")), AccessibilityUtils.getSnackbarDuration(getActivity()))
                .setAction(getString(R.string.reader_followed_blog_notifications_action),
                        new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                AnalyticsUtils.trackWithSiteId(Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED, blogId);
                                AddOrDeleteSubscriptionPayload payload = new AddOrDeleteSubscriptionPayload(
                                        String.valueOf(blogId), SubscriptionAction.NEW);
                                mDispatcher.dispatch(newUpdateSubscriptionNotificationPostAction(payload));
                                ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, true);
                            }
                        })
                .setActionTextColor(getResources().getColor(R.color.color_accent))
                .show();
    }

    @Override
    public void onFollowingTapped() {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscriptionUpdated(OnSubscriptionUpdated event) {
        if (event.isError()) {
            AppLog.e(T.API, ReaderPostListFragment.class.getSimpleName() + ".onSubscriptionUpdated: "
                            + event.error.type + " - " + event.error.message);
        } else {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());
        }
    }

    private void sharePost(ReaderPost post) {
        String url = (post.hasShortUrl() ? post.getShortUrl() : post.getUrl());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_share_intent);
        }
    }

    private void updateSubscription(SubscriptionAction action, long blogId) {
        AddOrDeleteSubscriptionPayload payload = new AddOrDeleteSubscriptionPayload(String.valueOf(blogId), action);
        mDispatcher.dispatch(newUpdateSubscriptionNotificationPostAction(payload));
    }

    /*
     * purge reader db if it hasn't been done yet
     */
    private void purgeDatabaseIfNeeded() {
        if (!mHasPurgedReaderDb) {
            AppLog.d(T.READER, "reader post list > purging database");
            mHasPurgedReaderDb = true;
            ReaderDatabase.purgeAsync();
        }
    }

    /*
     * start background service to get the latest followed tags and blogs if it's time to do so
     */
    private void updateFollowedTagsAndBlogsIfNeeded() {
        if (mLastAutoUpdateDt != null) {
            int minutesSinceLastUpdate = DateTimeUtils.minutesBetween(mLastAutoUpdateDt, new Date());
            if (minutesSinceLastUpdate < 120) {
                return;
            }
        }

        AppLog.d(T.READER, "reader post list > updating tags and blogs");
        mLastAutoUpdateDt = new Date();
        ReaderUpdateServiceStarter.startService(getActivity(), EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS));
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getCurrentPosition() > 0) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    // reset the timestamp that determines when followed tags/blogs are updated so they're
    // updated when the fragment is recreated (necessary after signin/disconnect)
    public static void resetLastUpdateDate() {
        mLastAutoUpdateDt = null;
    }

    private class LoadTagsTask extends AsyncTask<Void, Void, ReaderTagList> {
        private final FilteredRecyclerView.FilterCriteriaAsyncLoaderListener mFilterCriteriaLoaderListener;

        LoadTagsTask(FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener) {
            mFilterCriteriaLoaderListener = listener;
        }

        @Override
        protected ReaderTagList doInBackground(Void... voids) {
            ReaderTagList tagList = ReaderTagTable.getDefaultTags();
            tagList.addAll(ReaderTagTable.getCustomListTags());
            tagList.addAll(ReaderTagTable.getFollowedTags());
            tagList.addAll(ReaderTagTable.getBookmarkTags());
            return tagList;
        }

        @Override
        protected void onPostExecute(ReaderTagList tagList) {
            if (mFilterCriteriaLoaderListener != null) {
                //noinspection unchecked
                mFilterCriteriaLoaderListener.onFilterCriteriasLoaded((List) tagList);
            }
        }
    }
}
