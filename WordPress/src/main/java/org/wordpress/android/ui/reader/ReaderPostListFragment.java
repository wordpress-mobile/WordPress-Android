package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.ui.reader.services.ReaderPostService.UpdateAction;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.ui.reader.views.ReaderTagToolbar;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.ArrayList;
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
                   ReaderTagToolbar.OnTagChangedListener,
                   WPMainActivity.OnScrollToTopListener {

    private ReaderPostAdapter mPostAdapter;
    private ReaderRecyclerView mRecyclerView;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private View mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private long mCurrentFeedId;
    private ReaderPostListType mPostListType;

    private int mRestorePosition;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mIsAnimatingOutNewPostsBar;

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
            tag = ReaderTag.getDefaultTag();
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
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(savedInstanceState);
            }
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
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

        if (mWasPaused) {
            AppLog.d(T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;

            // default to refreshing posts in case the user returned from an activity that
            // changed one (or more) of them
            boolean shouldRefreshPosts = true;

            if (getPostListType().equals(ReaderPostListType.TAG_FOLLOWED)) {
                // check if the user added a tag in ReaderSubsActivity
                Object event = EventBus.getDefault().getStickyEvent(ReaderEvents.TagAdded.class);
                if (event != null) {
                    String tagName = ((ReaderEvents.TagAdded) event).getTagName();
                    EventBus.getDefault().removeStickyEvent(event);
                    ReaderTag newTag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
                    setCurrentTag(newTag, true);
                    shouldRefreshPosts = false;
                // make sure the current tag is still valid
                } else if (!ReaderTagTable.tagExists(getCurrentTag())) {
                    AppLog.d(T.READER, "reader post list > current tag no longer valid");
                    setCurrentTag(ReaderTag.getDefaultTag(), true);
                    shouldRefreshPosts = false;
                // auto-update the current tag if it's time
                } else if (!isUpdating() && ReaderTagTable.shouldAutoUpdateTag(getCurrentTag())) {
                    AppLog.i(T.READER, "reader post list > auto-updating current tag after resume");
                    updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
                    shouldRefreshPosts = false;
                }
            }

            if (shouldRefreshPosts) {
                refreshPosts();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        purgeDatabaseIfNeeded();
        performInitialUpdateIfNeeded();
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            updateFollowedTagsAndBlogsIfNeeded();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedTagsChanged event) {
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            // update the current tag if the list fragment is empty - this will happen if
            // the tag table was previously empty (ie: first run)
            if (isPostAdapterEmpty()) {
                updateCurrentTag();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedBlogsChanged event) {
        // refresh posts if user is viewing "Blogs I Follow"
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                && hasCurrentTag()
                && getCurrentTag().isBlogsIFollow()) {
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
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

        super.onSaveInstanceState(outState);
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasPostAdapter()) {
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return -1;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards, container, false);
        mRecyclerView = (ReaderRecyclerView) rootView.findViewById(R.id.recycler_view);

        Context context = container.getContext();

        // add the item decoration (divivers) to the recycler, skipping the first item if the first
        // item is the tag toolbar (shown when viewing posts in followed tags) - this is to avoid
        // having the tag toolbar take up more vertical space than necessary
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        boolean skipFirstItem = (getPostListType() == ReaderPostListType.TAG_FOLLOWED);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical, skipFirstItem));

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollRecycleViewToPosition(0);
                refreshPosts();
            }
        });

        // view that appears when current tag/blog has no posts - box images in this view are
        // displayed and animated for tags only
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mEmptyView.findViewById(R.id.layout_box_images).setVisibility(shouldShowBoxAndPagesAnimation() ? View.VISIBLE : View.GONE);

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                (CustomSwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            showSwipeToRefreshProgress(false);
                            return;
                        }
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
                        showSwipeToRefreshProgress(true);
                    }
                }
        );

        return rootView;
    }

    private void scrollRecycleViewToPosition(int position) {
        if (!isAdded() || mRecyclerView == null) return;

        mRecyclerView.scrollToPosition(position);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        boolean adapterAlreadyExists = hasPostAdapter();
        mRecyclerView.setAdapter(getPostAdapter());

        // if adapter didn't already exist, populate it now then update the tag/blog - this
        // check is important since without it the adapter would be reset and posts would
        // be updated every time the user moves between fragments
        if (!adapterAlreadyExists) {
            boolean isRecreated = (savedInstanceState != null);
            if (getPostListType().isTagType()) {
                getPostAdapter().setCurrentTag(mCurrentTag);
                if (!isRecreated && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                    updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
                }
            } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                getPostAdapter().setCurrentBlogAndFeed(mCurrentBlogId, mCurrentFeedId);
                if (!isRecreated) {
                    updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
                }
            }
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
        if (post == null || !hasPostAdapter()) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
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
        final ReaderBlogActions.BlockedBlogResult blockResult =
                ReaderBlogActions.blockBlogFromReader(post.blogId, actionListener);
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOCKED_BLOG);

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
     * box/pages animation that appears when loading an empty list (only appears for tags)
     */
    private boolean shouldShowBoxAndPagesAnimation() {
        return getPostListType().isTagType();
    }
    private void startBoxAndPagesAnimation() {
        if (!isAdded()) {
            return;
        }

        ImageView page1 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page1);
        ImageView page2 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page2);
        ImageView page3 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page3);

        page1.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page1));
        page2.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page2));
        page3.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page3));
    }

    private void setEmptyTitleAndDescription(boolean requestFailed) {
        if (!isAdded()) {
            return;
        }

        int titleResId;
        int descriptionResId = 0;

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            titleResId = R.string.reader_empty_posts_no_connection;
        } else if (requestFailed) {
            titleResId = R.string.reader_empty_posts_request_failed;
        } else if (isUpdating()) {
            titleResId = R.string.reader_empty_posts_in_tag_updating;
        } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            titleResId = R.string.reader_empty_posts_in_blog;
        } else if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && hasCurrentTag()) {
            if (getCurrentTag().isBlogsIFollow()) {
                titleResId = R.string.reader_empty_followed_blogs_title;
                descriptionResId = R.string.reader_empty_followed_blogs_description;
            } else if (getCurrentTag().isPostsILike()) {
                titleResId = R.string.reader_empty_posts_liked;
            } else {
                titleResId = R.string.reader_empty_posts_in_tag;
            }
        } else {
            titleResId = R.string.reader_empty_posts_in_tag;
        }

        TextView titleView = (TextView) mEmptyView.findViewById(R.id.title_empty);
        titleView.setText(getString(titleResId));

        TextView descriptionView = (TextView) mEmptyView.findViewById(R.id.description_empty);
        if (descriptionResId == 0) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            descriptionView.setText(getString(descriptionResId));
            descriptionView.setVisibility(View.VISIBLE);
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
            if (isEmpty) {
                setEmptyTitleAndDescription(false);
                mEmptyView.setVisibility(View.VISIBLE);
                if (shouldShowBoxAndPagesAnimation()) {
                    startBoxAndPagesAnimation();
                }
            } else {
                mEmptyView.setVisibility(View.GONE);
                if (mRestorePosition > 0) {
                    AppLog.d(T.READER, "reader post list > restoring position");
                    scrollRecycleViewToPosition(mRestorePosition);
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
            mPostAdapter.setOnTagChangedListener(this);
            mPostAdapter.setOnPostPopupListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            if (getActivity() instanceof ReaderBlogInfoView.OnBlogInfoLoadedListener) {
                mPostAdapter.setOnBlogInfoLoadedListener((ReaderBlogInfoView.OnBlogInfoLoadedListener) getActivity());
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
        return (mCurrentTag != null ? mCurrentTag.getTagName() : "");
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private void setCurrentTag(final ReaderTag tag, boolean allowAutoUpdate) {
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
                mTagPreviewHistory.push(tag.getTagName());
                break;
        }

        getPostAdapter().setCurrentTag(tag);
        hideNewPostsBar();
        showLoadingProgress(false);

        // update posts in this tag if it's time to do so
        if (allowAutoUpdate && ReaderTagTable.shouldAutoUpdateTag(tag)) {
            updatePostsWithTag(tag, UpdateAction.REQUEST_NEWER);
        }
    }

    /*
    * when previewing posts with a specific tag, a history of previewed tags is retained so
    * the user can navigate back through them - this is faster and requires less memory
    * than creating a new fragment for each previewed tag
    */
    boolean goBackInTagHistory() {
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

        ReaderTag newTag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
        setCurrentTag(newTag, false);

        return true;
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

        // determine whether to show the "new posts" bar - when this is shown, the newly
        // downloaded posts aren't displayed until the user taps the bar - only appears
        // when there are new posts in a followed tag and the user has scrolled the list
        // beyond the first post
        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW
                && event.getAction() == UpdateAction.REQUEST_NEWER
                && getPostListType() == ReaderPostListType.TAG_FOLLOWED
                && !isPostAdapterEmpty()
                && !isFirstPostVisible()) {
            showNewPostsBar();
        } else if (event.getResult().isNewOrChanged()) {
            refreshPosts();
        } else {
            boolean requestFailed = (event.getResult() == ReaderActions.UpdateResult.FAILED);
            setEmptyTitleAndDescription(requestFailed);
        }
    }

    /*
     * returns true if the first post is still visible in the RecyclerView - will return
     * false if the first post is scrolled out of view, or if the list is empty
     */
    private boolean isFirstPostVisible() {
        if (!isAdded()
                || mRecyclerView == null
                || mRecyclerView.getLayoutManager() == null) {
            return false;
        }

        View child = mRecyclerView.getLayoutManager().getChildAt(0);
        return (child != null && mRecyclerView.getLayoutManager().getPosition(child) == 0);
    }

    /*
     * get latest posts for this tag from the server
     */
    private void updatePostsWithTag(ReaderTag tag, UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        ReaderPostService.startServiceForTag(getActivity(), tag, updateAction);
    }

    private void updateCurrentTag() {
        updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
    }

    private boolean isUpdating() {
        return mIsUpdating;
    }

    private void showSwipeToRefreshProgress(boolean showProgress) {
        if (mSwipeToRefreshHelper != null && mSwipeToRefreshHelper.isRefreshing() != showProgress) {
            mSwipeToRefreshHelper.setRefreshing(showProgress);
        }
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
            showSwipeToRefreshProgress(true);
        } else if (!isUpdating) {
            // hide swipe-to-refresh progress if update is complete
            showSwipeToRefreshProgress(false);
        }
        mIsUpdating = isUpdating;
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private boolean isNewPostsBarShowing() {
        return (mNewPostsBar != null && mNewPostsBar.getVisibility() == View.VISIBLE);
    }

    private void showNewPostsBar() {
        if (!isAdded() || isNewPostsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);
    }

    private void hideNewPostsBar() {
        if (!isAdded() || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return;
        }

        mIsAnimatingOutNewPostsBar = true;

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

        ReaderPostListType type = getPostListType();
        Map<String, Object> analyticsProperties = new HashMap<>();

        switch (type) {
            case TAG_FOLLOWED:
            case TAG_PREVIEW:
                String key = (type == ReaderPostListType.TAG_PREVIEW ?
                        AnalyticsTracker.READER_DETAIL_TYPE_TAG_PREVIEW :
                        AnalyticsTracker.READER_DETAIL_TYPE_NORMAL);
                analyticsProperties.put(AnalyticsTracker.READER_DETAIL_TYPE_KEY, key);
                ReaderActivityLauncher.showReaderPostPagerForTag(
                        getActivity(),
                        getCurrentTag(),
                        getPostListType(),
                        post.blogId,
                        post.postId);
                break;
            case BLOG_PREVIEW:
                analyticsProperties.put(AnalyticsTracker.READER_DETAIL_TYPE_KEY,
                        AnalyticsTracker.READER_DETAIL_TYPE_BLOG_PREVIEW);
                ReaderActivityLauncher.showReaderPostPagerForBlog(
                        getActivity(),
                        post.blogId,
                        post.postId);
                break;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE, analyticsProperties);
    }

    /*
     * called from adapter when user taps a tag on a post to display tag preview
     */
    @Override
    public void onTagSelected(String tagName) {
        if (!isAdded()) return;

        ReaderTag tag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
        if (getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            setCurrentTag(tag, true);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(getActivity(), tag);
        }
    }

    /*
     * called from adapter when user selects a tag from the tag toolbar
     */
    @Override
    public void onTagChanged(ReaderTag tag) {
        if (!isAdded() || isCurrentTag(tag)) return;

        Map<String, String> properties = new HashMap<>();
        properties.put("tag", tag.getTagName());
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_TAG, properties);
        if (tag.isFreshlyPressed()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
        }
        AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
        setCurrentTag(tag, true);
    }

    /*
     * called when user taps "..." icon next to a post
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {
        if (view == null || post == null || !isAdded()) {
            return;
        }

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
     * purge reader db if it hasn't been done yet, but only if there's an active connection
     * since we don't want to purge posts that the user would expect to see when offline
     */
    private void purgeDatabaseIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPurgedDatabase.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.d(T.READER, "reader post list > purging database");
            ReaderDatabase.purgeAsync();
            EventBus.getDefault().postSticky(new ReaderEvents.HasPurgedDatabase());
        }
    }

    /*
     * initial update performed the first time the user opens the reader
     */
    private void performInitialUpdateIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPerformedInitialUpdate.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            // update current user to ensure we have their user_id as well as their latest info
            // in case they changed their avatar, name, etc. since last time
            AppLog.d(T.READER, "reader post list > updating current user");
            EventBus.getDefault().postSticky(new ReaderEvents.HasPerformedInitialUpdate());
        }
    }

    /*
     * start background service to get the latest followed tags and blogs if it's time to do so
     */
    private void updateFollowedTagsAndBlogsIfNeeded() {
        ReaderEvents.UpdatedFollowedTagsAndBlogs lastUpdateEvent =
                EventBus.getDefault().getStickyEvent(ReaderEvents.UpdatedFollowedTagsAndBlogs.class);
        if (lastUpdateEvent != null && lastUpdateEvent.minutesSinceLastUpdate() < 120) {
            return;
        }

        AppLog.d(T.READER, "reader post list > updating tags and blogs");
        EventBus.getDefault().postSticky(new ReaderEvents.UpdatedFollowedTagsAndBlogs());

        ReaderUpdateService.startService(getActivity(),
                EnumSet.of(ReaderUpdateService.UpdateTask.TAGS,
                           ReaderUpdateService.UpdateTask.FOLLOWED_BLOGS));
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getCurrentPosition() > 0) {
            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
        }
    }
}
