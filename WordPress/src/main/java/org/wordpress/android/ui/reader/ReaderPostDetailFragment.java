package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
import org.wordpress.android.ui.reader.ReaderInterfaces.AutoHideToolbarListener;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderRelatedPost;
import org.wordpress.android.ui.reader.models.ReaderRelatedPostList;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPScrollView;
import org.wordpress.android.widgets.WPScrollView.ScrollDirectionListener;

import java.util.EnumSet;

import de.greenrobot.event.EventBus;

public class ReaderPostDetailFragment extends Fragment
        implements WPMainActivity.OnActivityBackPressedListener,
                   ScrollDirectionListener,
                   ReaderCustomViewListener,
                   ReaderWebViewPageFinishedListener,
                   ReaderWebViewUrlClickListener {

    private long mPostId;
    private long mBlogId;
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

    private boolean mHasAlreadyUpdatedPost;
    private boolean mHasAlreadyRequestedPost;
    private boolean mIsLoggedOutReader;
    private boolean mIsWebViewPaused;
    private boolean mIsRelatedPost;

    private int mToolbarHeight;
    private String mErrorMessage;

    private boolean mIsToolbarShowing = true;
    private AutoHideToolbarListener mAutoHideToolbarListener;

    // min scroll distance before toggling toolbar
    private static final float MIN_SCROLL_DISTANCE_Y = 10;

    public static ReaderPostDetailFragment newInstance(long blogId, long postId) {
        return newInstance(blogId, postId, false, null);
    }

    public static ReaderPostDetailFragment newInstance(long blogId,
                                                       long postId,
                                                       boolean isRelatedPost,
                                                       ReaderPostListType postListType) {
        AppLog.d(T.READER, "reader post detail > newInstance");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putLong(ReaderConstants.ARG_POST_ID, postId);
        args.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost);
        if (postListType != null) {
            args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }

        ReaderPostDetailFragment fragment = new ReaderPostDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsLoggedOutReader = ReaderUtils.isLoggedOutReader();
        if (savedInstanceState != null) {
            mPostHistory.restoreInstance(savedInstanceState);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = args.getLong(ReaderConstants.ARG_POST_ID);
            mIsRelatedPost = args.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
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

        // setup the ReaderWebView
        mReaderWebView = (ReaderWebView) view.findViewById(R.id.reader_webview);
        mReaderWebView.setCustomViewListener(this);
        mReaderWebView.setUrlClickListener(this);
        mReaderWebView.setPageFinishedListener(this);

        // hide footer and scrollView until the post is loaded
        mLayoutFooter.setVisibility(View.INVISIBLE);
        mScrollView.setVisibility(View.INVISIBLE);

        return view;
    }

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
            mnuBrowse.setVisible(postHasUrl);
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
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);

        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, mIsRelatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasAlreadyRequestedPost);

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
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mIsRelatedPost = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mHasAlreadyRequestedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED);
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
        if (!hasPost()) {
            showPost();
        }
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
        if (!isAdded() || !hasPost() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        boolean isAskingToLike = !mPost.isLikedByCurrentUser;
        ReaderIconCountView likeCount = (ReaderIconCountView) getView().findViewById(R.id.count_likes);
        likeCount.setSelected(isAskingToLike);
        ReaderAnim.animateLikeButton(likeCount.getImageView(), isAskingToLike);

        boolean success = ReaderPostActions.performLikeAction(mPost, isAskingToLike);
        if (!success) {
            likeCount.setSelected(!isAskingToLike);
            return;
        }

        // get the post again since it has changed, then refresh to show changes
        mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
        refreshLikes();
        refreshIconCounts();

        if (isAskingToLike) {
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.READER_ARTICLE_LIKED, mBlogId);
        } else {
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.READER_ARTICLE_UNLIKED, mBlogId);
        }
    }

    /*
     * user tapped follow button to follow/unfollow the blog this post is from
     */
    private void togglePostFollowed() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        final boolean isAskingToFollow = !ReaderPostTable.isPostFollowed(mPost);
        final ReaderFollowButton followButton = (ReaderFollowButton) getView().findViewById(R.id.follow_button);

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!isAdded()) {
                    return;
                }
                followButton.setEnabled(true);
                if (!succeeded) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(getActivity(), resId);
                    followButton.setIsFollowedAnimated(!isAskingToFollow);
                }
            }
        };

        followButton.setEnabled(false);

        if (ReaderBlogActions.followBlogForPost(mPost, isAskingToFollow, listener)) {
            followButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }

    /*
     * display the standard Android share chooser to share this post
     */
    private static final int MAX_SHARE_TITLE_LEN = 100;

    private void sharePage() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        final String url = (mPost.hasShortUrl() ? mPost.getShortUrl() : mPost.getUrl());
        final String shareText;

        if (mPost.hasTitle()) {
            final String title;
            // we don't know where the user will choose to share, so enforce a max title length
            // in order to fit a tweet with some extra room for the URL and user edits
            if (mPost.getTitle().length() > MAX_SHARE_TITLE_LEN) {
                title = mPost.getTitle().substring(0, MAX_SHARE_TITLE_LEN).trim() + "…";
            } else {
                title = mPost.getTitle().trim();
            }
            shareText = title + " - " + url;
        } else {
            shareText = url;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reader_share_subject, getString(R.string.app_name)));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * replace the current post with the passed one
     */
    private void replacePost(long blogId, long postId) {
        mBlogId = blogId;
        mPostId = postId;
        mHasAlreadyRequestedPost = false;
        mHasAlreadyUpdatedPost = false;

        // hide views that would show info for the previous post - these will be re-displayed
        // with the correct info once the new post loads
        getView().findViewById(R.id.container_related_posts).setVisibility(View.GONE);
        getView().findViewById(R.id.text_related_posts_label).setVisibility(View.GONE);
        mLikingUsersView.setVisibility(View.GONE);
        mLikingUsersDivider.setVisibility(View.GONE);

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
     * related posts were retrieved, so show them
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.RelatedPostsUpdated event) {
        if (!isAdded() || !hasPost()) return;

        // make sure this is for the current post
        if (event.getSourcePost().postId == mPostId && event.getSourcePost().blogId == mBlogId) {
            showRelatedPosts(event.getRelatedPosts());
        }
    }

    private void showRelatedPosts(@NonNull ReaderRelatedPostList relatedPosts) {
        // locate the related posts container and remove any existing related post views
        ViewGroup container = (ViewGroup) getView().findViewById(R.id.container_related_posts);
        container.removeAllViews();

        // add a separate view for each related post
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        int imageSize = DisplayUtils.dpToPx(getActivity(), getResources().getDimensionPixelSize(R.dimen.reader_related_post_image_size));
        for (final ReaderRelatedPost relatedPost : relatedPosts) {
            View postView = inflater.inflate(R.layout.reader_related_post, container, false);
            TextView txtTitle = (TextView) postView.findViewById(R.id.text_related_post_title);
            TextView txtSubtitle = (TextView) postView.findViewById(R.id.text_related_post_subtitle);
            WPNetworkImageView imgFeatured = (WPNetworkImageView) postView.findViewById(R.id.image_related_post);

            txtTitle.setText(relatedPost.getTitle());
            txtSubtitle.setText(relatedPost.getSubtitle());

            imgFeatured.setVisibility(relatedPost.hasFeaturedImage() ? View.VISIBLE : View.GONE);
            if (relatedPost.hasFeaturedImage()) {
                String imageUrl = PhotonUtils.getPhotonImageUrl(relatedPost.getFeaturedImage(), imageSize, imageSize);
                imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
                imgFeatured.setVisibility(View.VISIBLE);
            }

            // tapping this view should open the related post detail
            postView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we're already viewing a related post, add it to the history stack
                    // so the user can back-button through the history - otherwise start a
                    // new detail activity for this related post
                    if (mIsRelatedPost) {
                        mPostHistory.push(new ReaderBlogIdPostId(mPost.blogId, mPost.postId));
                        replacePost(relatedPost.getBlogId(), relatedPost.getPostId());
                    } else {
                        ReaderActivityLauncher.showReaderPostDetail(getActivity(), relatedPost.getBlogId(), relatedPost.getPostId(), true);
                    }
                }
            });

            container.addView(postView);
        }

        View label = getView().findViewById(R.id.text_related_posts_label);
        if (label.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(label, AniUtils.Duration.MEDIUM);
        }
        if (container.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(container, AniUtils.Duration.MEDIUM);
        }
    }

    /*
     * if the fragment is maintaining a backstack of posts, navigate to the previous one
     */
    protected boolean goBackInPostHistory() {
        if (!mPostHistory.isEmpty()) {
            ReaderBlogIdPostId ids = mPostHistory.pop();
            replacePost(ids.getBlogId(), ids.getPostId());
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
                    mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
                    refreshIconCounts();
                }
                // refresh likes if necessary - done regardless of whether the post has changed
                // since it's possible likes weren't stored until the post was updated
                if (result != ReaderActions.UpdateResult.FAILED
                        && numLikesBefore != ReaderLikeTable.getNumLikesForPost(mPost)) {
                    refreshLikes();
                }

                setRefreshing(false);
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
            if (mIsLoggedOutReader) {
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
            }
        } else {
            countLikes.setVisibility(View.INVISIBLE);
            countLikes.setOnClickListener(null);
        }
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
                if (isAdded()) {
                    progress.setVisibility(View.GONE);
                    showPost();
                }
            }

            @Override
            public void onFailure(int statusCode) {
                if (isAdded()) {
                    progress.setVisibility(View.GONE);
                    int errMsgResId;
                    if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                        errMsgResId = R.string.no_network_message;
                    } else {
                        switch (statusCode) {
                            case 401:
                            case 403:
                                errMsgResId = R.string.reader_err_get_post_not_authorized;
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
            }
        };
        ReaderPostActions.requestPost(mBlogId, mPostId, listener);
    }

    /*
     * shows an error message in the middle of the screen - used when requesting post fails
     */
    private void showError(String errorMessage) {
        if (!isAdded()) return;

        TextView txtError = (TextView) getView().findViewById(R.id.text_error);
        txtError.setText(errorMessage);
        if (txtError.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(txtError, AniUtils.Duration.MEDIUM);
        }
        mErrorMessage = errorMessage;
    }

    private void showPost() {
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
            mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
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
                    mBlogId = discoverData.getBlogId();
                    mPostId = discoverData.getPostId();
                    mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
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
                    mHasAlreadyRequestedPost = true;
                    AppLog.i(T.READER, "reader post detail > post not found, requesting it");
                    requestPost();
                } else if (!TextUtils.isEmpty(mErrorMessage)) {
                    // post has already been requested and failed, so restore previous error message
                    showError(mErrorMessage);
                }
                return;
            }

            mReaderWebView.setIsPrivatePost(mPost.isPrivate);
            mReaderWebView.setBlogSchemeIsHttps(UrlUtils.isHttps(mPost.getBlogUrl()));

            TextView txtTitle = (TextView) getView().findViewById(R.id.text_title);
            TextView txtBlogName = (TextView) getView().findViewById(R.id.text_blog_name);
            TextView txtAuthor = (TextView) getView().findViewById(R.id.text_author);
            TextView txtDateLine = (TextView) getView().findViewById(R.id.text_dateline);
            TextView txtTag = (TextView) getView().findViewById(R.id.text_tag);

            ViewGroup layoutHeader = (ViewGroup) getView().findViewById(R.id.layout_post_detail_header);
            ReaderFollowButton followButton = (ReaderFollowButton) layoutHeader.findViewById(R.id.follow_button);
            WPNetworkImageView imgAvatar = (WPNetworkImageView) getView().findViewById(R.id.image_avatar);

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

            followButton.setVisibility(mIsLoggedOutReader ? View.GONE : View.VISIBLE);
            if (!mIsLoggedOutReader) {
                followButton.setIsFollowed(mPost.isFollowedByCurrentUser);
                followButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePostFollowed();
                    }
                });
            }

            // clicking the header shows blog preview
            if (getPostListType() != ReaderPostListType.BLOG_PREVIEW) {
                layoutHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), mPost);
                    }
                });
            }

            if (mPost.hasBlogName()) {
                txtBlogName.setText(mPost.getBlogName());
                txtBlogName.setVisibility(View.VISIBLE);
            } else if (mPost.hasBlogUrl()) {
                txtBlogName.setText(UrlUtils.getHost(mPost.getBlogUrl()));
                txtBlogName.setVisibility(View.VISIBLE);
            } else {
                txtBlogName.setVisibility(View.GONE);
            }

            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
            if (mPost.hasBlogUrl()) {
                String imageUrl = GravatarUtils.blavatarFromUrl(mPost.getBlogUrl(), avatarSz);
                imgAvatar.setImageUrl(imageUrl, WPNetworkImageView.ImageType.BLAVATAR);
            } else {
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);
            }

            if (mPost.hasAuthorName()) {
                txtAuthor.setText(mPost.getAuthorName());
                txtAuthor.setVisibility(View.VISIBLE);
            } else {
                txtAuthor.setVisibility(View.GONE);
            }

            String dateLine;
            if (mPost.hasBlogUrl()) {
                dateLine = UrlUtils.getHost(mPost.getBlogUrl()) + " \u2022 " + DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished());
            } else {
                dateLine = DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished());
            }
            txtDateLine.setText(dateLine);

            final String tagToDisplay = mPost.getTagForDisplay(null);
            if (!TextUtils.isEmpty(tagToDisplay)) {
                txtTag.setText(ReaderUtils.makeHashTag(tagToDisplay));
                txtTag.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderTag tag = ReaderUtils.getTagFromTagName(tagToDisplay, ReaderTagType.FOLLOWED);
                        ReaderActivityLauncher.showReaderTagPreview(v.getContext(), tag);
                    }
                });
            }

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
                        requestRelatedPosts();
                    }
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
            long blogId = ReaderUtils.getBlogIdFromBlogPreviewUrl(url);
            if (blogId != 0) {
                ReaderActivityLauncher.showReaderBlogPreview(getActivity(), blogId);
            }
            return true;
        }

        // open YouTube videos in external app so they launch the YouTube player, open all other
        // urls using an AuthenticatedWebViewActivity
        final OpenUrlType openUrlType;
        if (ReaderVideoUtils.isYouTubeVideoLink(url)) {
            openUrlType = OpenUrlType.EXTERNAL;
        } else {
            openUrlType = OpenUrlType.INTERNAL;
        }
        ReaderActivityLauncher.openUrl(getActivity(), url, openUrlType);
        return true;
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
        if (mIsLoggedOutReader) {
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
        if (mIsLoggedOutReader) {
            return mPost.numLikes > 0;
        }
        return mPost.canLikePost() || mPost.numLikes > 0;
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

}
