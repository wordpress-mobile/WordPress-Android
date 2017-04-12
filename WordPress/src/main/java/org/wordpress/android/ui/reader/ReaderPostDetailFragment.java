package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
import org.wordpress.android.ui.reader.ReaderInterfaces.AutoHideToolbarListener;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderSimplePostList;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderPostDetailHeaderView;
import org.wordpress.android.ui.reader.views.ReaderSimplePostContainerView;
import org.wordpress.android.ui.reader.views.ReaderSimplePostView;
import org.wordpress.android.ui.reader.views.ReaderTagStrip;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.WPScrollView;
import org.wordpress.android.widgets.WPScrollView.ScrollDirectionListener;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.passcodelock.AppLockManager;

import java.util.EnumSet;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ReaderPostDetailFragment extends Fragment
        implements WPMainActivity.OnActivityBackPressedListener,
                   ScrollDirectionListener,
                   ReaderCustomViewListener,
                   ReaderWebViewPageFinishedListener,
                   ReaderWebViewUrlClickListener {
    private long mPostId;
    private long mBlogId;
    private DirectOperation mDirectOperation;
    private int mCommentId;
    private boolean mIsFeed;
    private String mInterceptedUri;
    private ReaderPost mPost;
    private ReaderPostRenderer mRenderer;
    private ReaderPostListType mPostListType;

    private final ReaderPostHistory mPostHistory = new ReaderPostHistory();

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private WPScrollView mScrollView;
    private ViewGroup mLayoutFooter;
    private ReaderWebView mReaderWebView;
    private ReaderLikingUsersView mLikingUsersView;
    private View mLikingUsersDivider;
    private View mLikingUsersLabel;
    private WPTextView mSignInButton;

    private ReaderSimplePostContainerView mGlobalRelatedPostsView;
    private ReaderSimplePostContainerView mLocalRelatedPostsView;

    private boolean mPostSlugsResolutionUnderway;
    private boolean mHasAlreadyUpdatedPost;
    private boolean mHasAlreadyRequestedPost;
    private boolean mIsWebViewPaused;
    private boolean mIsRelatedPost;

    private boolean mHasTrackedGlobalRelatedPosts;
    private boolean mHasTrackedLocalRelatedPosts;

    private int mToolbarHeight;
    private String mErrorMessage;

    private boolean mIsToolbarShowing = true;
    private AutoHideToolbarListener mAutoHideToolbarListener;

    // min scroll distance before toggling toolbar
    private static final float MIN_SCROLL_DISTANCE_Y = 10;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    public static ReaderPostDetailFragment newInstance(long blogId, long postId) {
        return newInstance(false, blogId, postId, null, 0, false, null, null, false);
    }

    public static ReaderPostDetailFragment newInstance(boolean isFeed,
                                                       long blogId,
                                                       long postId,
                                                       DirectOperation directOperation,
                                                       int commentId,
                                                       boolean isRelatedPost,
                                                       String interceptedUri,
                                                       ReaderPostListType postListType,
                                                       boolean postSlugsResolutionUnderway) {
        AppLog.d(T.READER, "reader post detail > newInstance");

        Bundle args = new Bundle();
        args.putBoolean(ReaderConstants.ARG_IS_FEED, isFeed);
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putLong(ReaderConstants.ARG_POST_ID, postId);
        args.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost);
        args.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, directOperation);
        args.putInt(ReaderConstants.ARG_COMMENT_ID, commentId);
        args.putString(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri);
        if (postListType != null) {
            args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }
        args.putBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY, postSlugsResolutionUnderway);

        ReaderPostDetailFragment fragment = new ReaderPostDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        if (savedInstanceState != null) {
            mPostHistory.restoreInstance(savedInstanceState);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mIsFeed = args.getBoolean(ReaderConstants.ARG_IS_FEED);
            mBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = args.getLong(ReaderConstants.ARG_POST_ID);
            mDirectOperation = (DirectOperation) args.getSerializable(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = args.getInt(ReaderConstants.ARG_COMMENT_ID);
            mIsRelatedPost = args.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            mInterceptedUri = args.getString(ReaderConstants.ARG_INTERCEPTED_URI);
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            mPostSlugsResolutionUnderway = args.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof AutoHideToolbarListener) {
            mAutoHideToolbarListener = (AutoHideToolbarListener) activity;
        }
        mToolbarHeight = activity.getResources().getDimensionPixelSize(R.dimen.toolbar_height);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_post_detail, container, false);

        CustomSwipeRefreshLayout swipeRefreshLayout = (CustomSwipeRefreshLayout) view.findViewById(R.id.swipe_to_refresh);

        //this fragment hides/shows toolbar with scrolling, which messes up ptr animation position
        //so we have to set it manually
        int swipeToRefreshOffset = getResources().getDimensionPixelSize(R.dimen.toolbar_content_offset);
        swipeRefreshLayout.setProgressViewOffset(false, 0, swipeToRefreshOffset);

        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(), swipeRefreshLayout, new SwipeToRefreshHelper.RefreshListener() {
            @Override
            public void onRefreshStarted() {
                if (!isAdded()) {
                    return;
                }

                updatePost();
            }
        });

        mScrollView = (WPScrollView) view.findViewById(R.id.scroll_view_reader);
        mScrollView.setScrollDirectionListener(this);

        mLayoutFooter = (ViewGroup) view.findViewById(R.id.layout_post_detail_footer);
        mLikingUsersView = (ReaderLikingUsersView) view.findViewById(R.id.layout_liking_users_view);
        mLikingUsersDivider = view.findViewById(R.id.layout_liking_users_divider);
        mLikingUsersLabel = view.findViewById(R.id.text_liking_users_label);

        // setup the ReaderWebView
        mReaderWebView = (ReaderWebView) view.findViewById(R.id.reader_webview);
        mReaderWebView.setCustomViewListener(this);
        mReaderWebView.setUrlClickListener(this);
        mReaderWebView.setPageFinishedListener(this);

        // hide footer and scrollView until the post is loaded
        mLayoutFooter.setVisibility(View.INVISIBLE);
        mScrollView.setVisibility(View.INVISIBLE);

        View relatedPostsContainer = view.findViewById(R.id.container_related_posts);
        mGlobalRelatedPostsView = (ReaderSimplePostContainerView) relatedPostsContainer.findViewById(R.id.related_posts_view_global);
        mLocalRelatedPostsView = (ReaderSimplePostContainerView) relatedPostsContainer.findViewById(R.id.related_posts_view_local);

        mSignInButton = (WPTextView) view.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);

        final ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress_loading);
        if (mPostSlugsResolutionUnderway) {
            progress.setVisibility(View.VISIBLE);
        }

        showPost();

        return view;
    }

    private final View.OnClickListener mSignInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EventBus.getDefault().post(new ReaderEvents.DoSignIn());
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReaderWebView != null) {
            mReaderWebView.destroy();
        }
    }

    private boolean hasPost() {
        return (mPost != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.reader_detail, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // browse & share require the post to have a URL (some feed-based posts don't have one)
        boolean postHasUrl = hasPost() && mPost.hasUrl();
        MenuItem mnuBrowse = menu.findItem(R.id.menu_browse);
        if (mnuBrowse != null) {
            mnuBrowse.setVisible(postHasUrl || (mInterceptedUri != null));
        }
        MenuItem mnuShare = menu.findItem(R.id.menu_share);
        if (mnuShare != null) {
            mnuShare.setVisible(postHasUrl);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_browse) {
            if (hasPost()) {
                ReaderActivityLauncher.openUrl(getActivity(), mPost.getUrl(), OpenUrlType.EXTERNAL);
            } else if (mInterceptedUri != null) {
                AnalyticsUtils.trackWithInterceptedUri(AnalyticsTracker.Stat.DEEP_LINKED_FALLBACK, mInterceptedUri);
                ReaderActivityLauncher.openUrl(getActivity(), mInterceptedUri, OpenUrlType.EXTERNAL);
                getActivity().finish();
            }
            return true;
        } else if (i == R.id.menu_share) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SHARED_ITEM);
            sharePage();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ReaderConstants.ARG_IS_FEED, mIsFeed);
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);
        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, mDirectOperation);
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, mCommentId);

        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, mIsRelatedPost);
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri);
        outState.putBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY, mPostSlugsResolutionUnderway);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasAlreadyRequestedPost);

        outState.putBoolean(ReaderConstants.KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS, mHasTrackedGlobalRelatedPosts);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS, mHasTrackedLocalRelatedPosts);

        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

        mPostHistory.saveInstance(outState);

        if (!TextUtils.isEmpty(mErrorMessage)) {
            outState.putString(ReaderConstants.KEY_ERROR_MESSAGE, mErrorMessage);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        restoreState(savedInstanceState);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mIsFeed = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_FEED);
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mDirectOperation = (DirectOperation) savedInstanceState
                    .getSerializable(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = savedInstanceState.getInt(ReaderConstants.ARG_COMMENT_ID);
            mIsRelatedPost = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI);
            mPostSlugsResolutionUnderway = savedInstanceState.getBoolean(ReaderConstants.KEY_POST_SLUGS_RESOLUTION_UNDERWAY);
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mHasAlreadyRequestedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED);
            mHasTrackedGlobalRelatedPosts = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_GLOBAL_RELATED_POSTS);
            mHasTrackedLocalRelatedPosts = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_TRACKED_LOCAL_RELATED_POSTS);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.KEY_ERROR_MESSAGE)) {
                mErrorMessage = savedInstanceState.getString(ReaderConstants.KEY_ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    @Override
    public boolean onActivityBackPressed() {
        return goBackInPostHistory();
    }

    /*
     * changes the like on the passed post
     */
    private void togglePostLike() {
        if (hasPost()) {
            setPostLike(!mPost.isLikedByCurrentUser);
        }
    }

    /*
     * changes the like on the passed post
     */
    private void setPostLike(boolean isAskingToLike) {
        if (!isAdded() || !hasPost() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isAskingToLike != ReaderPostTable.isPostLikedByCurrentUser(mPost)) {
            ReaderIconCountView likeCount = (ReaderIconCountView) getView().findViewById(R.id.count_likes);
            likeCount.setSelected(isAskingToLike);
            ReaderAnim.animateLikeButton(likeCount.getImageView(), isAskingToLike);

            boolean success = ReaderPostActions.performLikeAction(mPost, isAskingToLike,
                    mAccountStore.getAccount().getUserId());
            if (!success) {
                likeCount.setSelected(!isAskingToLike);
                return;
            }

            // get the post again since it has changed, then refresh to show changes
            mPost = ReaderPostTable.getBlogPost(mPost.blogId, mPost.postId, false);
            refreshLikes();
            refreshIconCounts();
        }

        if (isAskingToLike) {
            AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_LIKED, mPost);
        } else {
            AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_UNLIKED, mPost);
        }
    }

    /**
     * display the standard Android share chooser to share this post
     */
    private void sharePage() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        String url = (mPost.hasShortUrl() ? mPost.getShortUrl() : mPost.getUrl());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, mPost.getTitle());
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * replace the current post with the passed one
     */
    private void replacePost(long blogId, long postId, boolean clearCommentOperation) {
        mIsFeed = false;
        mBlogId = blogId;
        mPostId = postId;

        if (clearCommentOperation) {
            mDirectOperation = null;
            mCommentId = 0;
        }

        mHasAlreadyRequestedPost = false;
        mHasAlreadyUpdatedPost = false;
        mHasTrackedGlobalRelatedPosts = false;
        mHasTrackedLocalRelatedPosts = false;

        // hide views that would show info for the previous post - these will be re-displayed
        // with the correct info once the new post loads
        mGlobalRelatedPostsView.setVisibility(View.GONE);
        mLocalRelatedPostsView.setVisibility(View.GONE);

        mLikingUsersView.setVisibility(View.GONE);
        mLikingUsersDivider.setVisibility(View.GONE);
        mLikingUsersLabel.setVisibility(View.GONE);

        // clear the webView - otherwise it will remain scrolled to where the user scrolled to
        mReaderWebView.clearContent();

        // make sure the toolbar and footer are showing
        showToolbar(true);
        showFooter(true);

        // now show the passed post
        showPost();
    }

    /*
     * request posts related to the current one - only available for wp.com
     */
    private void requestRelatedPosts() {
        if (hasPost() && mPost.isWP()) {
            ReaderPostActions.requestRelatedPosts(mPost);
        }
    }

    /*
     * related posts were retrieved
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.RelatedPostsUpdated event) {
        if (!isAdded() || !hasPost()) return;

        // make sure this event is for the current post
        if (event.getSourcePostId() == mPost.postId && event.getSourceSiteId() == mPost.blogId) {
            if (event.hasLocalRelatedPosts()) {
                showRelatedPosts(event.getLocalRelatedPosts(), false);
            }
            if (event.hasGlobalRelatedPosts()) {
                showRelatedPosts(event.getGlobalRelatedPosts(), true);
            }
        }
    }

    /*
     * show the passed list of related posts - can be either global (related posts from
     * across wp.com) or local (related posts from the same site as the current post)
     */
    private void showRelatedPosts(@NonNull ReaderSimplePostList relatedPosts, final boolean isGlobal) {
        // tapping a related post should open the related post detail
        ReaderSimplePostView.OnSimplePostClickListener listener = new ReaderSimplePostView.OnSimplePostClickListener() {
            @Override
            public void onSimplePostClick(View v, long siteId, long postId) {
                showRelatedPostDetail(siteId, postId, isGlobal);
            }
        };

        // different container views for global/local related posts
        ReaderSimplePostContainerView relatedPostsView = isGlobal ? mGlobalRelatedPostsView : mLocalRelatedPostsView;
        relatedPostsView.showPosts(relatedPosts, mPost.getBlogName(), isGlobal, listener);

        // fade in this related posts view
        if (relatedPostsView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(relatedPostsView, AniUtils.Duration.MEDIUM);
        }

        trackRelatedPostsIfShowing();
    }

    /*
     * track that related posts have loaded and are scrolled into view if we haven't
     * already tracked it
     */
    private void trackRelatedPostsIfShowing() {
        if (!mHasTrackedGlobalRelatedPosts && isVisibleAndScrolledIntoView(mGlobalRelatedPostsView)) {
            mHasTrackedGlobalRelatedPosts = true;
            AppLog.d(T.READER, "reader post detail > global related posts rendered");
            mGlobalRelatedPostsView.trackRailcarRender();
        }

        if (!mHasTrackedLocalRelatedPosts && isVisibleAndScrolledIntoView(mLocalRelatedPostsView)) {
            mHasTrackedLocalRelatedPosts = true;
            AppLog.d(T.READER, "reader post detail > local related posts rendered");
            mLocalRelatedPostsView.trackRailcarRender();
        }
    }

    /*
     * returns True if the passed view is visible and has been scrolled into view - assumes
     * that the view is a child of mScrollView
     */
    private boolean isVisibleAndScrolledIntoView(View view) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            Rect scrollBounds = new Rect();
            mScrollView.getHitRect(scrollBounds);
            return view.getLocalVisibleRect(scrollBounds);
        }
        return false;
    }

    /*
     * user clicked a single related post - if we're already viewing a related post, add it to the
     * history stack so the user can back-button through the history - otherwise start a new detail
     * activity for this related post
     */
    private void showRelatedPostDetail(long blogId, long postId, boolean isGlobal) {
        AnalyticsTracker.Stat stat = isGlobal
                ? AnalyticsTracker.Stat.READER_GLOBAL_RELATED_POST_CLICKED
                : AnalyticsTracker.Stat.READER_LOCAL_RELATED_POST_CLICKED;
        AnalyticsUtils.trackWithReaderPostDetails(stat, blogId, postId);

        if (mIsRelatedPost) {
            mPostHistory.push(new ReaderBlogIdPostId(mPost.blogId, mPost.postId));
            replacePost(blogId, postId, true);
        } else {
            ReaderActivityLauncher.showReaderPostDetail(
                    getActivity(),
                    false,
                    blogId,
                    postId,
                    null,
                    0,
                    true,
                    null);
        }
    }

    /*
     * if the fragment is maintaining a backstack of posts, navigate to the previous one
     */
    protected boolean goBackInPostHistory() {
        if (!mPostHistory.isEmpty()) {
            ReaderBlogIdPostId ids = mPostHistory.pop();
            replacePost(ids.getBlogId(), ids.getPostId(), true);
            return true;
        } else {
            return false;
        }
    }

    /*
     * get the latest version of this post
     */
    private void updatePost() {
        if (!hasPost() || !mPost.isWP()) {
            setRefreshing(false);
            return;
        }

        final int numLikesBefore = ReaderLikeTable.getNumLikesForPost(mPost);

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!isAdded()) {
                    return;
                }
                // if the post has changed, reload it from the db and update the like/comment counts
                if (result.isNewOrChanged()) {
                    mPost = ReaderPostTable.getBlogPost(mPost.blogId, mPost.postId, false);
                    refreshIconCounts();
                }
                // refresh likes if necessary - done regardless of whether the post has changed
                // since it's possible likes weren't stored until the post was updated
                if (result != ReaderActions.UpdateResult.FAILED
                        && numLikesBefore != ReaderLikeTable.getNumLikesForPost(mPost)) {
                    refreshLikes();
                }

                setRefreshing(false);

                if (mDirectOperation != null && mDirectOperation == DirectOperation.POST_LIKE) {
                    doLikePost();
                }
            }
        };
        ReaderPostActions.updatePost(mPost, resultListener);
    }

    private void refreshIconCounts() {
        if (!isAdded() || !hasPost() || !canShowFooter()) {
            return;
        }

        final ReaderIconCountView countLikes = (ReaderIconCountView) getView().findViewById(R.id.count_likes);
        final ReaderIconCountView countComments = (ReaderIconCountView) getView().findViewById(R.id.count_comments);

        if (canShowCommentCount()) {
            countComments.setCount(mPost.numReplies);
            countComments.setVisibility(View.VISIBLE);
            countComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(getActivity(), mPost.blogId, mPost.postId);
                }
            });
        } else {
            countComments.setVisibility(View.INVISIBLE);
            countComments.setOnClickListener(null);
        }

        if (canShowLikeCount()) {
            countLikes.setCount(mPost.numLikes);
            countLikes.setVisibility(View.VISIBLE);
            countLikes.setSelected(mPost.isLikedByCurrentUser);
            if (!mAccountStore.hasAccessToken()) {
                countLikes.setEnabled(false);
            } else if (mPost.canLikePost()) {
                countLikes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        togglePostLike();
                    }
                });
            }
            // if we know refreshLikes() is going to show the liking users, force liking user
            // views to take up space right now
            if (mPost.numLikes > 0 && mLikingUsersView.getVisibility() == View.GONE) {
                mLikingUsersView.setVisibility(View.INVISIBLE);
                mLikingUsersDivider.setVisibility(View.INVISIBLE);
                mLikingUsersLabel.setVisibility(View.INVISIBLE);
            }
        } else {
            countLikes.setVisibility(View.INVISIBLE);
            countLikes.setOnClickListener(null);
        }
    }

    private void doLikePost() {
        if (!isAdded()) {
            return;
        }

        if (!mAccountStore.hasAccessToken()) {
            Snackbar.make(getView(), R.string.reader_snackbar_err_cannot_like_post_logged_out, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.sign_in, mSignInClickListener).show();
            return;
        }

        if (!mPost.canLikePost()) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_cannot_like_post);
            return;
        }

        setPostLike(true);
    }

    /*
     * show latest likes for this post
     */
    private void refreshLikes() {
        if (!isAdded() || !hasPost() || !mPost.canLikePost()) {
            return;
        }

        // nothing more to do if no likes
        if (mPost.numLikes == 0) {
            mLikingUsersView.setVisibility(View.GONE);
            mLikingUsersDivider.setVisibility(View.GONE);
            mLikingUsersLabel.setVisibility(View.GONE);
            return;
        }

        // clicking likes view shows activity displaying all liking users
        mLikingUsersView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReaderActivityLauncher.showReaderLikingUsers(getActivity(), mPost.blogId, mPost.postId);
            }
        });

        mLikingUsersDivider.setVisibility(View.VISIBLE);
        mLikingUsersLabel.setVisibility(View.VISIBLE);
        mLikingUsersView.setVisibility(View.VISIBLE);
        mLikingUsersView.showLikingUsers(mPost);
    }

    private boolean showPhotoViewer(String imageUrl, View sourceView, int startX, int startY) {
        if (!isAdded() || TextUtils.isEmpty(imageUrl)) {
            return false;
        }

        // make sure this is a valid web image (could be file: or data:)
        if (!imageUrl.startsWith("http")) {
            return false;
        }

        String postContent = (mRenderer != null ? mRenderer.getRenderedHtml() : null);
        boolean isPrivatePost = (mPost != null && mPost.isPrivate);
        EnumSet<PhotoViewerOption> options = EnumSet.noneOf(PhotoViewerOption.class);
        if (isPrivatePost) {
            options.add(ReaderActivityLauncher.PhotoViewerOption.IS_PRIVATE_IMAGE);
        }

        ReaderActivityLauncher.showReaderPhotoViewer(
                getActivity(),
                imageUrl,
                postContent,
                sourceView,
                options,
                startX,
                startY);

        return true;
    }

    /*
     *  called when the post doesn't exist in local db, need to get it from server
     */
    private void requestPost() {
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_loading);
        progress.setVisibility(View.VISIBLE);
        progress.bringToFront();

        ReaderActions.OnRequestListener listener = new ReaderActions.OnRequestListener() {
            @Override
            public void onSuccess() {
                mHasAlreadyRequestedPost = true;
                if (isAdded()) {
                    progress.setVisibility(View.GONE);
                    showPost();
                    EventBus.getDefault().post(new ReaderEvents.SinglePostDownloaded());
                }
            }

            @Override
            public void onFailure(int statusCode) {
                mHasAlreadyRequestedPost = true;
                if (isAdded()) {
                    progress.setVisibility(View.GONE);
                    onRequestFailure(statusCode);
                }
            }
        };

        if (mIsFeed) {
            ReaderPostActions.requestFeedPost(mBlogId, mPostId, listener);
        } else {
            ReaderPostActions.requestBlogPost(mBlogId, mPostId, listener);
        }
    }

    /*
     * post slugs resolution to IDs has completed
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.PostSlugsRequestCompleted event) {
        mPostSlugsResolutionUnderway = false;

        if (!isAdded()) return;

        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_loading);
        progress.setVisibility(View.GONE);

        if (event.getStatusCode() == 200) {
            replacePost(event.getBlogId(), event.getPostId(), false);
        } else {
            onRequestFailure(event.getStatusCode());
        }
    }

    private void onRequestFailure(int statusCode) {
        int errMsgResId;
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            errMsgResId = R.string.no_network_message;
        } else {
            switch (statusCode) {
                case 401:
                case 403:
                    final boolean offerSignIn = WPUrlUtils.isWordPressCom(mInterceptedUri)
                            && !mAccountStore.hasAccessToken();

                    if (!offerSignIn) {
                        errMsgResId = (mInterceptedUri == null)
                                ? R.string.reader_err_get_post_not_authorized
                                : R.string.reader_err_get_post_not_authorized_fallback;
                        mSignInButton.setVisibility(View.GONE);
                    } else {
                        errMsgResId = (mInterceptedUri == null)
                                ? R.string.reader_err_get_post_not_authorized_signin
                                : R.string.reader_err_get_post_not_authorized_signin_fallback;
                        mSignInButton.setVisibility(View.VISIBLE);
                        AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_WPCOM_SIGN_IN_NEEDED,
                                mPost);
                    }
                    AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_USER_UNAUTHORIZED, mPost);
                    break;
                case 404:
                    errMsgResId = R.string.reader_err_get_post_not_found;
                    break;
                default:
                    errMsgResId = R.string.reader_err_get_post_generic;
                    break;
            }
        }
        showError(getString(errMsgResId));
    }

    /*
     * shows an error message in the middle of the screen - used when requesting post fails
     */
    private void showError(String errorMessage) {
        if (!isAdded()) return;

        TextView txtError = (TextView) getView().findViewById(R.id.text_error);
        txtError.setText(errorMessage);
        if (errorMessage == null) {
            txtError.setVisibility(View.GONE);
        } else if (txtError.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(txtError, AniUtils.Duration.MEDIUM);
        }
        mErrorMessage = errorMessage;
    }

    private void showPost() {
        if (mPostSlugsResolutionUnderway) {
            AppLog.w(T.READER, "reader post detail > post request already running");
            return;
        }

        if (mIsPostTaskRunning) {
            AppLog.w(T.READER, "reader post detail > show post task already running");
            return;
        }

        new ShowPostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * AsyncTask to retrieve this post from SQLite and display it
     */
    private boolean mIsPostTaskRunning = false;

    private class ShowPostTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            mIsPostTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsPostTaskRunning = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mPost = mIsFeed ? ReaderPostTable.getFeedPost(mBlogId, mPostId, false)
                    : ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
            if (mPost == null) {
                return false;
            }

            // "discover" Editor Pick posts should open the original (source) post
            if (mPost.isDiscoverPost()) {
                ReaderPostDiscoverData discoverData = mPost.getDiscoverData();
                if (discoverData != null
                        && discoverData.getDiscoverType() == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
                        && discoverData.getBlogId() != 0
                        && discoverData.getPostId() != 0) {
                    mIsFeed = false;
                    mBlogId = discoverData.getBlogId();
                    mPostId = discoverData.getPostId();
                    mPost = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
                    if (mPost == null) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mIsPostTaskRunning = false;

            if (!isAdded()) return;

            // make sure options menu reflects whether we now have a post
            getActivity().invalidateOptionsMenu();

            if (!result) {
                // post couldn't be loaded which means it doesn't exist in db, so request it from
                // the server if it hasn't already been requested
                if (!mHasAlreadyRequestedPost) {
                    AppLog.i(T.READER, "reader post detail > post not found, requesting it");
                    requestPost();
                } else if (!TextUtils.isEmpty(mErrorMessage)) {
                    // post has already been requested and failed, so restore previous error message
                    showError(mErrorMessage);
                }
                return;
            }

            if (mDirectOperation != null) {
                switch (mDirectOperation) {
                    case COMMENT_JUMP:
                    case COMMENT_REPLY:
                    case COMMENT_LIKE:
                        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
                            // passcode screen was launched already (when ReaderPostPagerActivity got resumed) so reset
                            // the timeout to let the passcode screen come up for the ReaderCommentListActivity.
                            // See https://github.com/wordpress-mobile/WordPress-Android/issues/4887
                            AppLockManager.getInstance().getAppLock().forcePasswordLock();
                        }
                        ReaderActivityLauncher.showReaderComments(getActivity(), mPost.blogId, mPost.postId,
                                mDirectOperation, mCommentId, mInterceptedUri);
                        getActivity().finish();
                        getActivity().overridePendingTransition(0, 0);
                        return;
                    case POST_LIKE:
                        // Liking needs to be handled "later" after the post has been updated from the server so,
                        // nothing special to do here
                        break;
                }
            }

            AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_RENDERED, mPost);

            mReaderWebView.setIsPrivatePost(mPost.isPrivate);
            mReaderWebView.setBlogSchemeIsHttps(UrlUtils.isHttps(mPost.getBlogUrl()));

            TextView txtTitle = (TextView) getView().findViewById(R.id.text_title);
            TextView txtDateline = (TextView) getView().findViewById(R.id.text_dateline);

            ReaderTagStrip tagStrip = (ReaderTagStrip) getView().findViewById(R.id.tag_strip);
            ReaderPostDetailHeaderView headerView = (ReaderPostDetailHeaderView) getView().findViewById(R.id.header_view);
            if (!canShowFooter()) {
                mLayoutFooter.setVisibility(View.GONE);
            }

            // add padding to the scrollView to make room for the top and bottom toolbars - this also
            // ensures the scrollbar matches the content so it doesn't disappear behind the toolbars
            int topPadding = (mAutoHideToolbarListener != null ? mToolbarHeight : 0);
            int bottomPadding = (canShowFooter() ? mLayoutFooter.getHeight() : 0);
            mScrollView.setPadding(0, topPadding, 0, bottomPadding);

            // scrollView was hidden in onCreateView, show it now that we have the post
            mScrollView.setVisibility(View.VISIBLE);

            // render the post in the webView
            mRenderer = new ReaderPostRenderer(mReaderWebView, mPost);
            mRenderer.beginRender();

            txtTitle.setText(mPost.hasTitle() ? mPost.getTitle() : getString(R.string.reader_untitled_post));

            String timestamp = DateTimeUtils.javaDateToTimeSpan(mPost.getDisplayDate(), WordPress.getContext());
            txtDateline.setText(timestamp);

            headerView.setPost(mPost, mAccountStore.hasAccessToken());
            tagStrip.setPost(mPost);

            if (canShowFooter() && mLayoutFooter.getVisibility() != View.VISIBLE) {
                AniUtils.fadeIn(mLayoutFooter, AniUtils.Duration.LONG);
            }

            refreshIconCounts();
        }
    }

    /*
     * called by the web view when the content finishes loading - likes aren't displayed
     * until this is triggered, to avoid having them appear before the webView content
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        if (!isAdded()) {
            return;
        }

        if (url != null && url.equals("about:blank")) {
            // brief delay before showing comments/likes to give page time to render
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded()) {
                        return;
                    }
                    refreshLikes();
                    if (!mHasAlreadyUpdatedPost) {
                        mHasAlreadyUpdatedPost = true;
                        updatePost();
                    }
                    requestRelatedPosts();
                }
            }, 300);
        } else {
            AppLog.w(T.READER, "reader post detail > page finished - " + url);
        }
    }

    /*
     * return the container view that should host the full screen video
     */
    @Override
    public ViewGroup onRequestCustomView() {
        if (isAdded()) {
            return (ViewGroup) getView().findViewById(R.id.layout_custom_view_container);
        } else {
            return null;
        }
    }

    /*
     * return the container view that should be hidden when full screen video is shown
     */
    @Override
    public ViewGroup onRequestContentView() {
        if (isAdded()) {
            return (ViewGroup) getView().findViewById(R.id.layout_post_detail_container);
        } else {
            return null;
        }
    }

    @Override
    public void onCustomViewShown() {
        // full screen video has just been shown so hide the ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public void onCustomViewHidden() {
        // user returned from full screen video so re-display the ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }

    boolean isCustomViewShowing() {
        return mReaderWebView != null && mReaderWebView.isCustomViewShowing();
    }

    void hideCustomView() {
        if (mReaderWebView != null) {
            mReaderWebView.hideCustomView();
        }
    }

    @Override
    public boolean onUrlClick(String url) {
        // if this is a "wordpress://blogpreview?" link, show blog preview for the blog - this is
        // used for Discover posts that highlight a blog
        if (ReaderUtils.isBlogPreviewUrl(url)) {
            long siteId = ReaderUtils.getBlogIdFromBlogPreviewUrl(url);
            if (siteId != 0) {
                ReaderActivityLauncher.showReaderBlogPreview(getActivity(), siteId);
            }
            return true;
        }

        OpenUrlType openUrlType = shouldOpenExternal(url) ? OpenUrlType.EXTERNAL : OpenUrlType.INTERNAL;
        ReaderActivityLauncher.openUrl(getActivity(), url, openUrlType);
        return true;
    }

    /*
     * returns True if the passed URL should be opened in the external browser app
     */
    private boolean shouldOpenExternal(String url) {
        // open YouTube videos in external app so they launch the YouTube player
        if (ReaderVideoUtils.isYouTubeVideoLink(url)) {
            return true;
        }

        // if the mime type starts with "application" open it externally - this will either
        // open it in the associated app or the default browser (which will enable the user
        // to download it)
        String mimeType = UrlUtils.getUrlMimeType(url);
        if (mimeType != null && mimeType.startsWith("application")) {
            return true;
        }

        // open all other urls using an AuthenticatedWebViewActivity
        return false;
    }

    @Override
    public boolean onImageUrlClick(String imageUrl, View view, int x, int y) {
        return showPhotoViewer(imageUrl, view, x, y);
    }

    private ActionBar getActionBar() {
        if (isAdded() && getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            AppLog.w(T.READER, "reader post detail > getActionBar returned null");
            return null;
        }
    }

    void pauseWebView() {
        if (mReaderWebView == null) {
            AppLog.w(T.READER, "reader post detail > attempt to pause null webView");
        } else if (!mIsWebViewPaused) {
            AppLog.d(T.READER, "reader post detail > pausing webView");
            mReaderWebView.hideCustomView();
            mReaderWebView.onPause();
            mIsWebViewPaused = true;
        }
    }

    void resumeWebViewIfPaused() {
        if (mReaderWebView == null) {
            AppLog.w(T.READER, "reader post detail > attempt to resume null webView");
        } else if (mIsWebViewPaused) {
            AppLog.d(T.READER, "reader post detail > resuming paused webView");
            mReaderWebView.onResume();
            mIsWebViewPaused = false;
        }
    }

    @Override
    public void onScrollUp(float distanceY) {
        if (!mIsToolbarShowing
                && -distanceY >= MIN_SCROLL_DISTANCE_Y) {
            showToolbar(true);
            showFooter(true);
        }
    }

    @Override
    public void onScrollDown(float distanceY) {
        if (mIsToolbarShowing
                && distanceY >= MIN_SCROLL_DISTANCE_Y
                && mScrollView.canScrollDown()
                && mScrollView.canScrollUp()
                && mScrollView.getScrollY() > mToolbarHeight) {
            showToolbar(false);
            showFooter(false);
        }
    }

    @Override
    public void onScrollCompleted() {
        if (!mIsToolbarShowing
                && (!mScrollView.canScrollDown() || !mScrollView.canScrollUp())) {
            showToolbar(true);
            showFooter(true);
        }

        trackRelatedPostsIfShowing();
    }

    private void showToolbar(boolean show) {
        mIsToolbarShowing = show;
        if (mAutoHideToolbarListener != null) {
            mAutoHideToolbarListener.onShowHideToolbar(show);
        }
    }

    private void showFooter(boolean show) {
        if (isAdded() && canShowFooter()) {
            AniUtils.animateBottomBar(mLayoutFooter, show);
        }
    }

    /*
     * can we show the footer bar which contains the like & comment counts?
     */
    private boolean canShowFooter() {
        return canShowLikeCount() || canShowCommentCount();
    }

    private boolean canShowCommentCount() {
        if (mPost == null) {
            return false;
        }
        if (!mAccountStore.hasAccessToken()) {
            return mPost.numReplies > 0;
        }
        return mPost.isWP()
                && !mPost.isJetpack
                && !mPost.isDiscoverPost()
                && (mPost.isCommentsOpen || mPost.numReplies > 0);
    }

    private boolean canShowLikeCount() {
        if (mPost == null) {
            return false;
        }
        if (!mAccountStore.hasAccessToken()) {
            return mPost.numLikes > 0;
        }
        return mPost.canLikePost() || mPost.numLikes > 0;
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }
}
