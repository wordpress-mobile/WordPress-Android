package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderActionBarTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

import java.util.HashMap;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

/**
 * Fragment hosted by ReaderActivity which shows a list of posts in a specific tag
 */
public class ReaderPostListFragment extends Fragment
                                    implements AbsListView.OnScrollListener,
                                               ActionBar.OnNavigationListener {

    static interface OnPostSelectedListener {
        public void onPostSelected(long blogId, long postId);
    }

    private ReaderPostAdapter mPostAdapter;
    private OnPostSelectedListener mPostSelectedListener;
    private ReaderFullScreenUtils.FullScreenListener mFullScreenListener;

    private PullToRefreshHelper mPullToRefreshHelper;
    private ListView mListView;
    private TextView mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private String mCurrentTag;
    private boolean mIsUpdating = false;
    private boolean mIsFlinging = false;

    static final String KEY_TAG_NAME = "tag_name";
    private static final String KEY_LIST_STATE = "list_state";
    private Parcelable mListState = null;

    protected static enum RefreshType {AUTOMATIC, MANUAL}

    static ReaderPostListFragment newInstance(final String tagName) {
        AppLog.d(T.READER, "reader post list > newInstance");

        Bundle args = new Bundle();
        if (!TextUtils.isEmpty(tagName))
            args.putString(KEY_TAG_NAME, tagName);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        // note that setCurrentTag() should NOT be called here since it's automatically
        // called from the actionbar navigation handler
        if (args != null && args.containsKey(KEY_TAG_NAME))
            mCurrentTag = args.getString(KEY_TAG_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_post_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);

        // bar that appears at top when new posts are downloaded
        mNewPostsBar = (TextView) view.findViewById(R.id.text_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadPosts(true);
                hideNewPostsBar();
            }
        });

        // add a header to the listView and margin to new posts bar that's the same height as
        // the ActionBar when fullscreen mode is supported
        if (isFullScreenSupported()) {
            Context context = container.getContext();
            ReaderFullScreenUtils.addListViewHeader(context, mListView);
            final int actionbarHeight = DisplayUtils.getActionBarHeight(context);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mNewPostsBar.getLayoutParams();
            params.topMargin = actionbarHeight;
        }

        // textView that appears when current tag has no posts
        mEmptyView = view.findViewById(R.id.empty_view);

        // set the listView's scroll listeners so we can detect up/down scrolling
        mListView.setOnScrollListener(this);

        // tapping a post opens the detail view
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // take header into account
                position -= mListView.getHeaderViewsCount();
                ReaderPost post = (ReaderPost) getPostAdapter().getItem(position);
                if (post != null && mPostSelectedListener != null)
                    mPostSelectedListener.onPostSelected(post.blogId, post.postId);
            }
        });

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) view.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) view.findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.MANUAL);
                    }
                });
        mListView.setAdapter(getPostAdapter());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            AppLog.d(T.READER, "reader post list > restoring instance state");
            mCurrentTag = savedInstanceState.getString(KEY_TAG_NAME);
            mListState = savedInstanceState.getParcelable(KEY_LIST_STATE);
        }

        setHasOptionsMenu(true);
        checkActionBar();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ReaderFullScreenUtils.FullScreenListener)
            mFullScreenListener = (ReaderFullScreenUtils.FullScreenListener) activity;

        if (activity instanceof OnPostSelectedListener)
            mPostSelectedListener = (OnPostSelectedListener) activity;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (hasCurrentTag())
            outState.putString(KEY_TAG_NAME, mCurrentTag);

        // retain list state so we can return to this position
        // http://stackoverflow.com/a/5694441/1673548
        if (mListView != null && mListView.getFirstVisiblePosition() > 0)
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.reader_native, menu);
        checkActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags :
                ReaderActivityLauncher.showReaderTagsForResult(getActivity(), null);
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * show/hide progress bar which appears at the bottom of the activity when loading more posts
     */
    private void showLoadingProgress() {
        if (hasActivity() && mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingProgress() {
        if (hasActivity() && mProgress != null) {
            mProgress.setVisibility(View.GONE);
        }
    }

    /*
     * ensures that the ActionBar is correctly set to list navigation mode using the tag adapter
     */
    private void checkActionBar() {
        // skip out if we're in list navigation mode, since that means the actionBar is
        // already correctly configured
        final ActionBar actionBar = getActionBar();
        if (actionBar == null || actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
            return;

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(getActionBarAdapter(), this);

        selectTagInActionBar(getCurrentTag());
    }


    private void startBoxAndPagesAnimation() {
        if (!hasActivity())
            return;

        Animation animPage1 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page1);
        ImageView page1 = (ImageView) getView().findViewById(R.id.empty_tags_box_page1);
        page1.startAnimation(animPage1);

        Animation animPage2 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page2);
        ImageView page2 = (ImageView) getView().findViewById(R.id.empty_tags_box_page2);
        page2.startAnimation(animPage2);

        Animation animPage3 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page3);
        ImageView page3 = (ImageView) getView().findViewById(R.id.empty_tags_box_page3);
        page3.startAnimation(animPage3);
    }

    private void setEmptyTitleAndDescriptionForCurrentTag() {
        if (!isPostAdapterEmpty())
            return;
        if (!hasActivity())
            return;

        int title, description = -1;
        if (isUpdating()) {
            title = R.string.reader_empty_posts_in_tag_updating;
        } else {
            int tagIndex = getActionBarAdapter().getIndexOfTagName(mCurrentTag);

            final String tagId;
            if (tagIndex > -1) {
                ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(tagIndex);
                tagId = tag.getStringIdFromEndpoint();
            } else {
                tagId = "";
            }
            if (tagId.equals(ReaderTag.TAG_ID_FOLLOWING)) {
                title = R.string.reader_empty_followed_blogs_title;
                description = R.string.reader_empty_followed_blogs_description;
            } else {
                if (tagId.equals(ReaderTag.TAG_ID_LIKED)) {
                    title = R.string.reader_empty_posts_liked;
                } else {
                    title = R.string.reader_empty_posts_in_tag;
                }
            }
        }
        TextView titleView = (TextView) getView().findViewById(R.id.title_empty);
        TextView descriptionView = (TextView) getView().findViewById(R.id.description_empty);
        titleView.setText(getString(title));
        if (description == -1) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            descriptionView.setText(getString(description));
            descriptionView.setVisibility(View.VISIBLE);
        }
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!hasActivity())
                return;
            if (isEmpty) {
                startBoxAndPagesAnimation();
                setEmptyTitleAndDescriptionForCurrentTag();
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                // restore listView state - this returns to the previously scrolled-to item
                if (mListState != null && mListView != null) {
                    mListView.onRestoreInstanceState(mListState);
                    mListState = null;
                }
            }
        }
    };

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener = new ReaderActions.DataRequestedListener() {
        @Override
        public void onRequestData(ReaderActions.RequestDataAction action) {
            // skip if update is already in progress
            if (isUpdating())
                return;
            // skip if we already have the max # of posts
            if (ReaderPostTable.getNumPostsWithTag(mCurrentTag) >= Constants.READER_MAX_POSTS_TO_DISPLAY)
                return;
            // request older posts
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
            updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_OLDER, RefreshType.MANUAL);
        }
    };

    /*
     * called by post adapter when user requests to reblog a post
     */
    private final ReaderActions.RequestReblogListener mReblogListener = new ReaderActions.RequestReblogListener() {
        @Override
        public void onRequestReblog(ReaderPost post) {
            if (hasActivity())
                ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post);
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter==null)
            mPostAdapter = new ReaderPostAdapter(getActivity(),
                                                 mReblogListener,
                                                 mDataLoadedListener,
                                                 mDataRequestedListener);
        return mPostAdapter;
    }

    private boolean hasPostAdapter () {
        return mPostAdapter!=null;
    }
    private boolean isPostAdapterEmpty() {
        return (mPostAdapter==null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTag(final String tagName) {
        if (!hasCurrentTag() || TextUtils.isEmpty(tagName))
            return false;
        return (mCurrentTag.equalsIgnoreCase(tagName));
    }

    private String getCurrentTag() {
        return StringUtils.notNullStr(mCurrentTag);
    }

    private boolean hasCurrentTag() {
        return !TextUtils.isEmpty(mCurrentTag);
    }

    private void setCurrentTag(final String tagName) {
        if (TextUtils.isEmpty(tagName))
            return;

        // skip if this is already the current tag and the post adapter is already showing it - this
        // will happen when the list fragment is restored and the current tag is re-selected in the
        // actionBar dropdown
        if (isCurrentTag(tagName)
                && hasPostAdapter()
                && tagName.equals(getPostAdapter().getCurrentTag()))
            return;

        mCurrentTag = tagName;
        UserPrefs.setReaderTag(tagName);

        getPostAdapter().setCurrentTag(tagName);
        hideNewPostsBar();

        // update posts in this tag if it's time to do so
        if (ReaderTagTable.shouldAutoUpdateTag(tagName))
            updatePostsWithTag(tagName, ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
        getPostAdapter().refresh();
    }

    /*
     * tell the adapter to reload a single post - called when user returns from detail, where the
     * post may have been changed (either by the user, or because it updated)
     */
    void reloadPost(ReaderPost post) {
        if (post == null)
            return;
        getPostAdapter().reloadPost(post);
    }

    /*
     * reload current tag
     */
    private void reloadPosts(boolean animateRows) {
        getPostAdapter().reload(animateRows);
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    void updateFollowStatusOnPostsForBlog(long blogId, boolean followStatus) {
        if (hasPostAdapter())
            getPostAdapter().updateFollowStatusOnPostsForBlog(blogId, followStatus);
    }

    /*
     * get latest posts for this tag from the server
     */
    private void updatePostsWithCurrentTag(ReaderActions.RequestDataAction updateAction, RefreshType refreshType) {
        if (hasCurrentTag())
            updatePostsWithTag(mCurrentTag, updateAction, refreshType);
    }

    private void updatePostsWithTag(final String tagName, final ReaderActions.RequestDataAction updateAction,
                                    RefreshType refreshType) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }

        setIsUpdating(true, updateAction);
        setEmptyTitleAndDescriptionForCurrentTag();

        // if this is "Posts I Like" or "Blogs I Follow" and it's a manual refresh (user tapped refresh icon),
        // refresh the posts so posts that were unliked/unfollowed no longer appear
        if (refreshType == RefreshType.MANUAL && isCurrentTag(tagName)) {
            if (tagName.equals(ReaderTag.TAG_NAME_LIKED) || tagName.equals(ReaderTag.TAG_NAME_FOLLOWING))
                refreshPosts();
        }

        ReaderPostActions.updatePostsWithTag(tagName, updateAction, new ReaderActions.UpdateResultAndCountListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                if (!hasActivity()) {
                    AppLog.w(T.READER, "reader post list > volley response when fragment has no activity");
                    return;
                }

                setIsUpdating(false, updateAction);

                if (result == ReaderActions.UpdateResult.CHANGED && numNewPosts > 0 && isCurrentTag(tagName)) {
                    // if we loaded new posts and posts are already displayed, show the "new posts"
                    // bar rather than immediately refreshing the list
                    if (!isPostAdapterEmpty() && updateAction == ReaderActions.RequestDataAction.LOAD_NEWER) {
                        showNewPostsBar(numNewPosts);
                    } else {
                        refreshPosts();
                    }
                } else {
                    // update empty view title and description if the the post list is empty
                    setEmptyTitleAndDescriptionForCurrentTag();
                }
            }
        });
    }

    public boolean isUpdating() {
        return mIsUpdating;
    }

    public void setIsUpdating(boolean isUpdating, ReaderActions.RequestDataAction updateAction) {
        if (!hasActivity() || mIsUpdating == isUpdating) {
            return;
        }
        switch (updateAction) {
            case LOAD_OLDER:
                // if these are older posts, show/hide message bar at bottom
                if (isUpdating) {
                    showLoadingProgress();
                } else {
                    hideLoadingProgress();
                }
                break;
            default:
                mPullToRefreshHelper.setRefreshing(isUpdating);
                break;
        }
        mIsUpdating = isUpdating;
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private void showNewPostsBar(int numNewPosts) {
        if (mNewPostsBar==null || mNewPostsBar.getVisibility()==View.VISIBLE)
            return;
        if (numNewPosts==1) {
            mNewPostsBar.setText(R.string.reader_label_new_posts_one);
        } else {
            mNewPostsBar.setText(getString(R.string.reader_label_new_posts_multi, numNewPosts));
        }
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);
        mPullToRefreshHelper.hideTipTemporarily(true);
    }

    private void hideNewPostsBar() {
        if (mNewPostsBar==null || mNewPostsBar.getVisibility()!=View.VISIBLE)
            return;
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mNewPostsBar.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
        mPullToRefreshHelper.showTip(true);
    }

    /*
     * make sure current tag still exists, reset to default if it doesn't
     */
    private void checkCurrentTag() {
        if (hasCurrentTag() && !ReaderTagTable.tagExists(getCurrentTag()))
            mCurrentTag = ReaderTag.TAG_NAME_DEFAULT;
    }

    /*
     * refresh the list of tags shown in the ActionBar
     */
    void refreshTags() {
        if (!hasActivity())
            return;
        checkCurrentTag();
        getActionBarAdapter().refreshTags();
    }

    /*
     * called from ReaderActivity after user adds/removes tags
     */
    void doTagsChanged(final String newCurrentTag) {
        checkCurrentTag();
        getActionBarAdapter().reloadTags();
        if (!TextUtils.isEmpty(newCurrentTag))
            setCurrentTag(newCurrentTag);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        boolean isFlingingNow = (scrollState == SCROLL_STATE_FLING);
        if (isFlingingNow != mIsFlinging) {
            mIsFlinging = isFlingingNow;
            if (hasPostAdapter())
                getPostAdapter().setIsFlinging(mIsFlinging);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // nop
    }

    private boolean isFullScreenSupported() {
        return (mFullScreenListener != null && mFullScreenListener.isFullScreenSupported());
    }

    /*
     * ActionBar tag dropdown adapter
     */
    private ReaderActionBarTagAdapter mActionBarAdapter;
    private ReaderActionBarTagAdapter getActionBarAdapter() {
        if (mActionBarAdapter == null) {
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!hasActivity())
                        return;
                    AppLog.d(T.READER, "reader post list > ActionBar adapter loaded");
                    selectTagInActionBar(getCurrentTag());
                }
            };

            boolean isStaticMenuDrawer;
            if (getActivity() instanceof WPActionBarActivity) {
                isStaticMenuDrawer = ((WPActionBarActivity)getActivity()).isStaticMenuDrawer();
            } else {
                isStaticMenuDrawer = false;
            }
            mActionBarAdapter = new ReaderActionBarTagAdapter(getActivity(), isStaticMenuDrawer, dataListener);
        }

        return mActionBarAdapter;
    }

    private ActionBar getActionBar() {
        if (getActivity() instanceof Activity) {
            return getActivity().getActionBar();
        } else {
            AppLog.w(T.READER, "reader post list > null ActionBar");
            return null;
        }
    }

    /*
     * make sure the passed tag is the one selected in the actionbar
     */
    private void selectTagInActionBar(final String tagName) {
        if (TextUtils.isEmpty(tagName))
            return;

        ActionBar actionBar = getActionBar();
        if (actionBar == null)
            return;

        int position = getActionBarAdapter().getIndexOfTagName(tagName);
        if (position == -1 || position == actionBar.getSelectedNavigationIndex())
            return;

        if (actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
            AppLog.w(T.READER, "reader post list > unexpected ActionBar navigation mode");
            return;
        }

        actionBar.setSelectedNavigationItem(position);
    }

    /*
     * called when user selects a tag from the ActionBar dropdown
     */
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        final ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(itemPosition);
        if (tag == null)
            return false;

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("tag", tag.getTagName());
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_TAG, properties);

        if (tag.getTagName().equals(ReaderTag.TAG_NAME_FRESHLY_PRESSED)) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
        }

        setCurrentTag(tag.getTagName());
        AppLog.d(T.READER, "reader post list > tag chosen from actionbar: " + tag.getTagName());

        return true;
    }
}
