package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderSearchTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult;
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionAdapter;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.ui.reader.services.ReaderPostService.UpdateAction;
import org.wordpress.android.ui.reader.services.ReaderSearchService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.greenrobot.event.EventBus;

public class ReaderPostListFragment extends Fragment
        implements ReaderInterfaces.OnPostSelectedListener,
                   ReaderInterfaces.OnTagSelectedListener,
                   ReaderInterfaces.OnPostPopupListener,
                   WPMainActivity.OnActivityBackPressedListener,
                   WPMainActivity.OnScrollToTopListener {

    private ReaderPostAdapter mPostAdapter;
    private ReaderSearchSuggestionAdapter mSearchSuggestionAdapter;

    private FilteredRecyclerView mRecyclerView;
    private boolean mFirstLoad = true;
    private final ReaderTagList mTags = new ReaderTagList();

    private View mNewPostsBar;
    private View mEmptyView;
    private View mEmptyViewBoxImages;
    private ProgressBar mProgress;

    private SearchView mSearchView;
    private MenuItem mSettingsMenuItem;
    private MenuItem mSearchMenuItem;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private long mCurrentFeedId;
    private String mCurrentSearchQuery;
    private ReaderPostListType mPostListType;

    private int mRestorePosition;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mHasUpdatedPosts;
    private boolean mIsAnimatingOutNewPostsBar;

    private static boolean mHasPurgedReaderDb;
    private static Date mLastAutoUpdateDt;

    private final HistoryStack mTagPreviewHistory = new HistoryStack("tag_preview_history");

    private static class HistoryStack extends Stack<String> {
        private final String keyName;
        HistoryStack(@SuppressWarnings("SameParameterValue") String keyName) {
            this.keyName = keyName;
        }
        void restoreInstance(Bundle bundle) {
            clear();
            if (bundle.containsKey(keyName)) {
                ArrayList<String> history = bundle.getStringArrayList(keyName);
                if (history != null) {
                    this.addAll(history);
                }
            }
        }
        void saveInstance(Bundle bundle) {
            if (!isEmpty()) {
                ArrayList<String> history = new ArrayList<>();
                history.addAll(this);
                bundle.putStringArrayList(keyName, history);
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
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(savedInstanceState);
            }
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
            mHasUpdatedPosts = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mFirstLoad = savedInstanceState.getBoolean(ReaderConstants.KEY_FIRST_LOAD);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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

            // if the user was searching, make sure the filter toolbar is showing
            // so the user can see the search keyword they entered
            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                mRecyclerView.showToolbar();
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
            setCurrentTag(ReaderUtils.getDefaultTag());
        } else {
            // otherwise, refresh posts to make sure any changes are reflected and auto-update
            // posts in the current tag if it's time
            refreshPosts();
            updateCurrentTagIfTime();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

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
        EventBus.getDefault().unregister(this);
    }

    /*
     * ensures the adapter is created and posts are updated if they haven't already been
     */
    private void checkPostAdapter()  {
        if (isAdded() && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostAdapter());

            if (!mHasUpdatedPosts && NetworkUtils.isNetworkAvailable(getActivity())) {
                mHasUpdatedPosts = true;
                if (getPostListType().isTagType()) {
                    updateCurrentTagIfTime();
                } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag);
        }
        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mTagPreviewHistory.saveInstance(outState);
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId);
        outState.putLong(ReaderConstants.ARG_FEED_ID, mCurrentFeedId);
        outState.putString(ReaderConstants.ARG_SEARCH_QUERY, mCurrentSearchQuery);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasUpdatedPosts);
        outState.putBoolean(ReaderConstants.KEY_FIRST_LOAD, mFirstLoad);
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

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
        mRecyclerView = (FilteredRecyclerView) rootView.findViewById(R.id.reader_recycler_view);

        Context context = container.getContext();

        // view that appears when current tag/blog has no posts - box images in this view are
        // displayed and animated for tags only
        mEmptyView = rootView.findViewById(R.id.empty_custom_view);
        mEmptyViewBoxImages = mEmptyView.findViewById(R.id.layout_box_images);

        mRecyclerView.setLogT(AppLog.T.READER);
        mRecyclerView.setCustomEmptyView(mEmptyView);
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

                if (mFirstLoad){
                    /* let onResume() take care of this logic, as the FilteredRecyclerView.FilterListener onLoadData method
                    * is called on two moments: once for first time load, and then each time the swipe to refresh gesture
                    * triggers a refresh
                    */
                    mRecyclerView.setRefreshing(false);
                    mFirstLoad = false;
                } else {
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                        case TAG_PREVIEW:
                            updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
                            break;
                        case BLOG_PREVIEW:
                            updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
                            break;
                    }
                    // make sure swipe-to-refresh progress shows since this is a manual refresh
                    mRecyclerView.setRefreshing(true);
                }
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                onTagChanged((ReaderTag)criteria);
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
            public void onShowCustomEmptyView (EmptyViewMessageType emptyViewMsgType) {
                setEmptyTitleAndDescription(
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
        mRecyclerView.setToolbarBackgroundColor(ContextCompat.getColor(context, R.color.blue_medium));
        mRecyclerView.setToolbarSpinnerTextColor(ContextCompat.getColor(context, R.color.white));
        mRecyclerView.setToolbarSpinnerDrawable(R.drawable.arrow);
        mRecyclerView.setToolbarLeftAndRightPadding(
                getResources().getDimensionPixelSize(R.dimen.margin_medium) + spacingHorizontal,
                getResources().getDimensionPixelSize(R.dimen.margin_extra_large) + spacingHorizontal);

        // add a menu to the filtered recycler's toolbar
        if (!ReaderUtils.isLoggedOutReader()
                && (getPostListType() == ReaderPostListType.TAG_FOLLOWED || getPostListType() == ReaderPostListType.SEARCH_RESULTS)) {
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
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

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

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                resetPostAdapter(ReaderPostListType.SEARCH_RESULTS);
                showSearchMessage();
                mSettingsMenuItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                hideSearchMessage();
                resetSearchSuggestionAdapter();
                mSettingsMenuItem.setVisible(true);
                mCurrentSearchQuery = null;

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
        ReaderSearchService.startService(getActivity(), mCurrentSearchQuery, offset);
    }

    private void submitSearchQuery(@NonNull String query) {
        if (!isAdded()) return;

        mSearchView.clearFocus(); // this will hide suggestions and the virtual keyboard
        hideSearchMessage();

        // remember this query for future suggestions
        String trimQuery = query != null ? query.trim() : "";
        ReaderSearchTable.addOrUpdateQueryString(trimQuery);

        // remove cached results for this search - search results are ephemeral so each search
        // should be treated as a "fresh" one
        ReaderTag searchTag = ReaderSearchService.getTagForSearchQuery(trimQuery);
        ReaderPostTable.deletePostsWithTag(searchTag);

        mPostAdapter.setCurrentTag(searchTag);
        mCurrentSearchQuery = trimQuery;
        updatePostsInCurrentSearch(0);

        // track that the user performed a search
        if (!trimQuery.equals("")) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("query", trimQuery);
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED, properties);
        }
    }

    /*
     * reuse "empty" view to let user know what they're querying
     */
    private void showSearchMessage() {
        if (!isAdded()) return;

        // clear posts so only the empty view is visible
        getPostAdapter().clear();

        setEmptyTitleAndDescription(false);
        showEmptyView();
    }

    private void hideSearchMessage() {
        hideEmptyView();
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
        if (!isAdded()) return;

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(true, updateAction);
        setEmptyTitleAndDescription(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.SearchPostsEnded event) {
        if (!isAdded()) return;

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(false, updateAction);

        // load the results if the search succeeded and it's the current search - note that success
        // means the search didn't fail, not necessarily that is has results - which is fine because
        // if there aren't results then refreshing will show the empty message
        if (event.didSucceed()
                && getPostListType() == ReaderPostListType.SEARCH_RESULTS
                && event.getQuery().equals(mCurrentSearchQuery)) {
            refreshPosts();
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
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
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
        // Only pass the blogID if available. Do not track feedID
        AnalyticsUtils.trackWithBlogDetails(
                AnalyticsTracker.Stat.READER_BLOG_BLOCKED,
                mCurrentBlogId != 0 ? mCurrentBlogId : null
        );

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
        Snackbar.make(getView(), getString(R.string.reader_toast_blog_blocked), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, undoListener)
                .show();
    }

    /*
     * box/pages animation that appears when loading an empty list
     */
    private boolean shouldShowBoxAndPagesAnimation() {
        return getPostListType().isTagType();
    }

    private void startBoxAndPagesAnimation() {
        if (!isAdded()) return;

        ImageView page1 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page1);
        ImageView page2 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page2);
        ImageView page3 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page3);

        page1.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page1));
        page2.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page2));
        page3.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page3));
    }

    private void setEmptyTitleAndDescription(boolean requestFailed) {
        if (!isAdded()) return;

        String title;
        String description = null;

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            title = getString(R.string.reader_empty_posts_no_connection);
        } else if (requestFailed) {
            title = getString(R.string.reader_empty_posts_request_failed);
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
                    } else if (getCurrentTag().isPostsILike()) {
                        title = getString(R.string.reader_empty_posts_liked);
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
                    if (isSearchViewEmpty() || TextUtils.isEmpty(mCurrentSearchQuery)) {
                        title = getString(R.string.reader_label_post_search_explainer);
                    } else if (isUpdating()) {
                        title = getString(R.string.reader_label_post_search_running);
                    } else {
                        title = getString(R.string.reader_empty_posts_in_search_title);
                        String formattedQuery = "<em>" + mCurrentSearchQuery + "</em>";
                        description = String.format(getString(R.string.reader_empty_posts_in_search_description), formattedQuery);
                    }
                    break;

                default:
                    title = getString(R.string.reader_empty_posts_in_tag);
                    break;
            }
        }

        setEmptyTitleAndDescription(title, description);
    }

    private void setEmptyTitleAndDescription(@NonNull String title, String description) {
        if (!isAdded()) return;

        TextView titleView = (TextView) mEmptyView.findViewById(R.id.title_empty);
        titleView.setText(title);

        TextView descriptionView = (TextView) mEmptyView.findViewById(R.id.description_empty);
        if (description == null) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            if (description.contains("<") && description.contains(">")) {
                descriptionView.setText(Html.fromHtml(description));
            } else {
                descriptionView.setText(description);
            }
            descriptionView.setVisibility(View.VISIBLE);
        }

        mEmptyViewBoxImages.setVisibility(shouldShowBoxAndPagesAnimation() ? View.VISIBLE : View.GONE);
    }

    private void showEmptyView() {
        if (isAdded()) {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyView() {
        if (isAdded()) {
            mEmptyView.setVisibility(View.GONE);
        }
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
            mRecyclerView.setRefreshing(false);
            if (isEmpty) {
                setEmptyTitleAndDescription(false);
                showEmptyView();
                if (shouldShowBoxAndPagesAnimation()) {
                    startBoxAndPagesAnimation();
                }
            } else {
                hideEmptyView();
                if (mRestorePosition > 0) {
                    AppLog.d(T.READER, "reader post list > restoring position");
                    mRecyclerView.scrollRecycleViewToPosition(mRestorePosition);
                }
            }
            mRestorePosition = 0;
        }
    };

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener = new ReaderActions.DataRequestedListener() {
        @Override
        public void onRequestData() {
            // skip if update is already in progress
            if (isUpdating()) {
                return;
            }

            // request older posts unless we already have the max # to show
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    if (ReaderPostTable.getNumPostsWithTag(mCurrentTag) < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
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
                    ReaderTag searchTag = ReaderSearchService.getTagForSearchQuery(mCurrentSearchQuery);
                    int offset = ReaderPostTable.getNumPostsWithTag(searchTag);
                    if (offset < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentSearch(offset);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;
            }
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating post adapter");
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mPostAdapter = new ReaderPostAdapter(context, getPostListType());
            mPostAdapter.setOnPostSelectedListener(this);
            mPostAdapter.setOnTagSelectedListener(this);
            mPostAdapter.setOnPostPopupListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            if (getActivity() instanceof ReaderBlogInfoView.OnBlogInfoLoadedListener) {
                mPostAdapter.setOnBlogInfoLoadedListener((ReaderBlogInfoView.OnBlogInfoLoadedListener) getActivity());
            }
            if (getPostListType().isTagType()) {
                mPostAdapter.setCurrentTag(getCurrentTag());
            } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                mPostAdapter.setCurrentBlogAndFeed(mCurrentBlogId, mCurrentFeedId);
            } else if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                ReaderTag searchTag = ReaderSearchService.getTagForSearchQuery(mCurrentSearchQuery);
                mPostAdapter.setCurrentTag(searchTag);
            }
        }
        return mPostAdapter;
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

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                AppPrefs.setReaderTag(tag);
                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagSlug());
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
        } else if (goBackInTagHistory()) {
            return true;
        } else {
            return false;
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
            ReaderPostService.startServiceForFeed(getActivity(), mCurrentFeedId, updateAction);
        } else {
            ReaderPostService.startServiceForBlog(getActivity(), mCurrentBlogId, updateAction);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (!isAdded()) return;

        setIsUpdating(true, event.getAction());
        setEmptyTitleAndDescription(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (!isAdded()) return;

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
            setEmptyTitleAndDescription(requestFailed);
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
        if (!isAdded()) return;

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        if (tag == null) {
            AppLog.w(T.READER, "null tag passed to updatePostsWithTag");
            return;
        }
        AppLog.d(T.READER, "reader post list > updating tag " + tag.getTagNameForLog() + ", updateAction=" + updateAction.name());
        ReaderPostService.startServiceForTag(getActivity(), tag, updateAction);
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
     * and are unlikely to show new posts due to the way they're sorted by score
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
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (isAdded()) {
                    mNewPostsBar.setVisibility(View.GONE);
                    mIsAnimatingOutNewPostsBar = false;
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
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
        if (!isAdded() || post == null) return;

        // "discover" posts that highlight another post should open the original (source) post when tapped
        if (post.isDiscoverPost()) {
            ReaderPostDiscoverData discoverData = post.getDiscoverData();
            if (discoverData != null && discoverData.getDiscoverType() == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK) {
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
                ReaderActivityLauncher.showReaderPostDetail(getActivity(), post.blogId, post.postId);
                break;
        }
    }

    /*
     * called from adapter when user taps a tag on a post to display tag preview
     */
    @Override
    public void onTagSelected(String tagName) {
        if (!isAdded()) return;

        ReaderTag tag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED);
        if (getPostListType().equals(ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            setCurrentTag(tag);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(getActivity(), tag);
        }
    }

    /*
     * called when user selects a tag from the tag toolbar
     */
    private void onTagChanged(ReaderTag tag) {
        if (!isAdded() || isCurrentTag(tag)) return;

        trackTagLoaded(tag);
        AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
        setCurrentTag(tag);
    }

    private void trackTagLoaded(ReaderTag tag) {
        AnalyticsTracker.Stat stat = null;

        if (tag.isDiscover()) {
            stat = AnalyticsTracker.Stat.READER_DISCOVER_VIEWED;
        } else if (tag.isTagTopic()) {
            stat = AnalyticsTracker.Stat.READER_TAG_LOADED;
        } else if (tag.isListTopic()) {
            stat = AnalyticsTracker.Stat.READER_LIST_LOADED;
        }

        if (stat == null) return;

        Map<String, String> properties = new HashMap<>();
        properties.put("tag", tag.getTagSlug());

        AnalyticsTracker.track(stat, properties);
    }

    /*
     * called when user taps "..." icon next to a post
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {
        if (view == null || post == null || !isAdded()) return;

        Context context = view.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(view);
        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setModal(true);

        List<Integer> menuItems = new ArrayList<>();
        boolean isFollowed = ReaderPostTable.isPostFollowed(post);
        if (isFollowed) {
            menuItems.add(ReaderMenuAdapter.ITEM_UNFOLLOW);
        } else {
            menuItems.add(ReaderMenuAdapter.ITEM_FOLLOW);
        }
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            menuItems.add(ReaderMenuAdapter.ITEM_BLOCK);
        }
        listPopup.setAdapter(new ReaderMenuAdapter(context, menuItems));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdded()) return;

                listPopup.dismiss();
                switch((int) id) {
                    case ReaderMenuAdapter.ITEM_FOLLOW:
                    case ReaderMenuAdapter.ITEM_UNFOLLOW:
                        toggleFollowStatusForPost(post);
                        break;
                    case ReaderMenuAdapter.ITEM_BLOCK:
                        blockBlogForPost(post);
                        break;
                }
            }
        });
        listPopup.show();
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
        ReaderUpdateService.startService(getActivity(), EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS));
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getCurrentPosition() > 0) {
            mRecyclerView.smoothScrollToPosition(0);
            mRecyclerView.showToolbar();
        }
    }

    public static void resetLastUpdateDate() {
        mLastAutoUpdateDt = null;
    }

    private class LoadTagsTask extends AsyncTask<Void, Void, ReaderTagList> {

        private final FilteredRecyclerView.FilterCriteriaAsyncLoaderListener mFilterCriteriaLoaderListener;

        public LoadTagsTask(FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener){
            mFilterCriteriaLoaderListener = listener;
        }

        @Override
        protected ReaderTagList doInBackground(Void... voids) {
            ReaderTagList tagList = ReaderTagTable.getDefaultTags();
            tagList.addAll(ReaderTagTable.getCustomListTags());
            tagList.addAll(ReaderTagTable.getFollowedTags());
            return tagList;
        }

        @Override
        protected void onPostExecute(ReaderTagList tagList) {
            if (tagList != null && !tagList.isSameList(mTags)) {
                mTags.clear();
                mTags.addAll(tagList);
                if (mFilterCriteriaLoaderListener != null)
                    //noinspection unchecked
                    mFilterCriteriaLoaderListener.onFilterCriteriasLoaded((List)mTags);
            }
        }
    }

}

