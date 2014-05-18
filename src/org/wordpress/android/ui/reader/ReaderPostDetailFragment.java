package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ReaderVideoUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.wordpress.android.widgets.WPListView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

public class ReaderPostDetailFragment extends Fragment
                                      implements WPListView.OnScrollDirectionListener {
    protected static enum PostChangeType { LIKED, UNLIKED, FOLLOWED, UNFOLLOWED, CONTENT }
    static interface PostChangeListener {
        public void onPostChanged(long blogId, long postId, PostChangeType changeType);
    }

    private static final String KEY_SHOW_COMMENT_BOX = "show_comment_box";
    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;

    private ViewGroup mLayoutIcons;
    private ViewGroup mLayoutLikes;
    private WPListView mListView;
    private ViewGroup mCommentFooter;
    private ProgressBar mProgressFooter;
    private WebView mWebView;

    private boolean mIsAddCommentBoxShowing;
    private long mReplyToCommentId = 0;
    private boolean mHasAlreadyUpdatedPost;
    private boolean mHasAlreadyRequestedPost;
    private boolean mIsUpdatingComments;
    private boolean mWebViewIsPaused;

    private Parcelable mListState = null;

    private final ArrayList<String> mPrevAvatarUrls = new ArrayList<String>();
    private final Handler mHandler = new Handler();

    private ReaderUtils.FullScreenListener mFullScreenListener;
    private PostChangeListener mPostChangeListener;

    public static ReaderPostDetailFragment newInstance(long blogId, long postId) {
        AppLog.d(T.READER, "reader post detail > newInstance");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putLong(ReaderConstants.ARG_POST_ID, postId);

        ReaderPostDetailFragment fragment = new ReaderPostDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /*
     * adapter containing comments for this post
     */
    private ReaderCommentAdapter mAdapter;
    private ReaderCommentAdapter getCommentAdapter() {
        if (mAdapter == null) {
            if (!hasActivity())
                AppLog.w(T.READER, "reader post detail > comment adapter created before activity");

            ReaderActions.DataLoadedListener dataLoadedListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!hasActivity())
                        return;
                    // show footer below comments when comments exist
                    mCommentFooter.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    // restore listView state (scroll position) if it was saved during rotation
                    if (mListState != null) {
                        if (!isEmpty)
                            getListView().onRestoreInstanceState(mListState);
                        mListState = null;
                    }
                }
            };

            // adapter calls this when user taps reply icon
            ReaderCommentAdapter.RequestReplyListener replyListener = new ReaderCommentAdapter.RequestReplyListener() {
                @Override
                public void onRequestReply(long commentId) {
                    if (!mIsAddCommentBoxShowing) {
                        showAddCommentBox(commentId);
                    } else {
                        hideAddCommentBox();
                    }
                }
            };

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            ReaderActions.DataRequestedListener dataRequestedListener = new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData(ReaderActions.RequestDataAction action) {
                    if (mIsUpdatingComments)
                        return;
                    AppLog.i(T.READER, "reader post detail > requesting newer comments");
                    updateComments();
                }
            };
            mAdapter = new ReaderCommentAdapter(getActivity(), mPost, replyListener, dataLoadedListener, dataRequestedListener);
        }
        return mAdapter;
    }

    private boolean isCommentAdapterEmpty() {
        return (mAdapter==null || mAdapter.isEmpty());
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = args.getLong(ReaderConstants.ARG_POST_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_post_detail, container, false);

        // locate & init listView
        mListView = (WPListView) view.findViewById(android.R.id.list);
        if (isFullScreenSupported()) {
            mListView.setOnScrollDirectionListener(this);
            ReaderUtils.addListViewHeader(mListView, DisplayUtils.getActionBarHeight(container.getContext()));
        }

        // add post detail as header to listView - must be done before setting adapter
        ViewGroup headerDetail = (ViewGroup) inflater.inflate(R.layout.reader_listitem_post_detail, mListView, false);
        mListView.addHeaderView(headerDetail, null, false);

        // add listView footer containing progress bar - footer appears whenever there are comments,
        // progress bar appears when loading new comments
        mCommentFooter = (ViewGroup) inflater.inflate(R.layout.reader_footer_progress, mListView, false);
        mCommentFooter.setVisibility(View.GONE);
        mCommentFooter.setBackgroundColor(getResources().getColor(R.color.grey_extra_light));
        mProgressFooter = (ProgressBar) mCommentFooter.findViewById(R.id.progress_footer);
        mProgressFooter.setVisibility(View.INVISIBLE);
        mListView.addFooterView(mCommentFooter);

        mLayoutIcons = (ViewGroup) view.findViewById(R.id.layout_actions);
        mLayoutLikes = (ViewGroup) view.findViewById(R.id.layout_likes);

        // setup the webView
        mWebView = (WebView) view.findViewById(R.id.webView);
        mWebView.setWebViewClient(readerWebViewClient);
        mWebView.getSettings().setUserAgentString(WordPress.getUserAgent());

        // hide these views until the post is loaded
        mListView.setVisibility(View.INVISIBLE);
        mWebView.setVisibility(View.INVISIBLE);

        // detect image taps so we can open images in the photo viewer activity
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction()==MotionEvent.ACTION_UP) {
                    HitTestResult hr = ((WebView)v).getHitTestResult();
                    if (hr != null && (hr.getType() == HitTestResult.IMAGE_TYPE || hr.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                        String imageUrl = hr.getExtra();
                        if (imageUrl == null)
                            return false;
                        // skip if image is a file: reference - this will be the video overlay, ie:
                        // file:///android_res/drawable/ic_reader_video_overlay.png
                        if (imageUrl.startsWith("file:"))
                            return false;
                        showPhotoViewer(imageUrl);
                        return true;
                    }
                }
                return false;
            }
        });

        return view;
    }

    private WPListView getListView() {
        return mListView;
    }

    @Override
    public void onScrollUp() {
        // don't change fullscreen if user is typing a comment
        if (mIsAddCommentBoxShowing) {
            return;
        }

        // otherwise always disable fullscreen when scrolling up
        if (isFullScreen()) {
            setIsFullScreen(false);
        }
    }

    @Override
    public void onScrollDown() {
        // don't change fullscreen if user is typing a comment
        if (mIsAddCommentBoxShowing) {
            return;
        }

        boolean isFullScreen = isFullScreen();
        boolean canScrollDown = mListView.canScrollDown();
        boolean canScrollUp = mListView.canScrollUp();

        if (isFullScreen && !canScrollDown) {
            // disable full screen once user hits the bottom
            setIsFullScreen(false);
        } else if (!isFullScreen && canScrollDown && canScrollUp) {
            // enable full screen when scrolling down
            setIsFullScreen(true);
        }
    }

    private boolean hasPost() {
        return (mPost != null);
    }

    void reloadPost() {
        if (hasPost() && hasActivity())
            showPost();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.reader_native_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_browse:
                if (hasPost())
                    ReaderActivityLauncher.openUrl(getActivity(), mPost.getUrl(), OpenUrlType.EXTERNAL);
                return true;
            case R.id.menu_share:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHARED_ITEM);
                sharePage();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean hasActivity() {
        return isAdded() && !isRemoving();
    }

    private boolean hostIsReaderActivity() {
        return (getActivity() instanceof ReaderActivity);
    }

    /*
     * full-screen mode hides the ActionBar and icon bar
     */
    private boolean isFullScreen() {
        return (mFullScreenListener != null && mFullScreenListener.isFullScreen());
    }

    private boolean isFullScreenSupported() {
        return (mFullScreenListener != null && mFullScreenListener.isFullScreenSupported());
    }

    private void setIsFullScreen(boolean enableFullScreen) {
        if (mFullScreenListener == null)
            return;
        // this tells ReaderActivity to enable/disable fullscreen
        if (!mFullScreenListener.onRequestFullScreen(enableFullScreen))
            return;
        if (mPost.isWP())
            animateIconBar(!enableFullScreen);
    }

    /*
     * animate in/out the layout containing the reblog/comment/like icons
     */
    private void animateIconBar(boolean isAnimatingIn) {
        if (isAnimatingIn && mLayoutIcons.getVisibility() == View.VISIBLE)
            return;
        if (!isAnimatingIn && mLayoutIcons.getVisibility() != View.VISIBLE)
            return;

        final Animation animation;
        if (isAnimatingIn) {
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                    1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        } else {
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                    0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        }

        animation.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));

        mLayoutIcons.clearAnimation();
        mLayoutIcons.startAnimation(animation);
        mLayoutIcons.setVisibility(isAnimatingIn ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);

        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasAlreadyRequestedPost);
        outState.putBoolean(KEY_SHOW_COMMENT_BOX, mIsAddCommentBoxShowing);

        if (mIsAddCommentBoxShowing) {
            outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        }

        // retain listView state if a comment has been scrolled to - this enables us to restore
        // the scroll position after comment data is reloaded
        if (getListView() != null && getListView().getFirstVisiblePosition() > 0) {
            mListState = getListView().onSaveInstanceState();
            outState.putParcelable(ReaderConstants.KEY_LIST_STATE, mListState);
        } else {
            mListState = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ReaderUtils.FullScreenListener) {
            mFullScreenListener = (ReaderUtils.FullScreenListener) activity;
        }
        if (activity instanceof PostChangeListener) {
            mPostChangeListener = (PostChangeListener) activity;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mHasAlreadyRequestedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED);
            if (savedInstanceState.getBoolean(KEY_SHOW_COMMENT_BOX)) {
                long replyToCommentId = savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID);
                showAddCommentBox(replyToCommentId);
            }
            mListState = savedInstanceState.getParcelable(ReaderConstants.KEY_LIST_STATE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!hasPost()) {
            showPost();
        }
    }

    @Override
    public void onPause() {
        // this ensures embedded videos don't continue to play when the fragment is no longer
        // active or has been detached
        if (hasEmbedsOrIframes()) {
            pauseWebView();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasEmbedsOrIframes()) {
            resumeWebView();
        }
    }

    /*
     * called by this fragment whenever the post is changed - notifies ReaderActivity of the
     * change so it can tell the list fragment to reflect the change
     */
    private void doPostChanged(PostChangeType changeType) {
        if (mPostChangeListener == null || !hasPost() || changeType == null)
            return;
        mPostChangeListener.onPostChanged(mPost.blogId, mPost.postId, changeType);
    }

    /*
     * changes the like on the passed post
     */
    private void togglePostLike(ReaderPost post, View likeButton) {
        boolean isSelected = likeButton.isSelected();
        likeButton.setSelected(!isSelected);
        AniUtils.zoomAction(likeButton);

        boolean isAskingToLike = !post.isLikedByCurrentUser;

        if (!ReaderPostActions.performLikeAction(post, isAskingToLike)) {
            likeButton.setSelected(isSelected);
            return;
        }

        // get the post again, since it has changed
        mPost = ReaderPostTable.getPost(mBlogId, mPostId);

        // fire listener so host knows about the change
        if (isAskingToLike) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LIKED_ARTICLE);
            doPostChanged(PostChangeType.LIKED);
        } else {
            doPostChanged(PostChangeType.UNLIKED);
        }

        // call returns before api completes, but local version of post will have been changed
        // so refresh to show those changes
        refreshLikes(true);
    }

    /*
     * change the follow state of the blog the passed post is in
     */
    private void togglePostFollowed(ReaderPost post, View followButton) {
        boolean isSelected = followButton.isSelected();
        followButton.setSelected(!isSelected);
        AniUtils.zoomAction(followButton);

        final boolean isAskingToFollow = !post.isFollowedByCurrentUser;
        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && hasActivity()) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(getActivity(), resId);
                }
            }
        };
        if (!ReaderBlogActions.performFollowAction(post, isAskingToFollow, actionListener)) {
            followButton.setSelected(isSelected);
            return;
        }

        // get the post again, since it has changed
        mPost = ReaderPostTable.getPost(mBlogId, mPostId);

        // fire listener so host knows about the change
        doPostChanged(isAskingToFollow ? PostChangeType.FOLLOWED : PostChangeType.UNFOLLOWED);

        // call returns before api completes, but local version of post will have been changed
        // so refresh to show those changes
        refreshFollowed();
    }

    /*
     * called when user chooses to reblog the post
     */
    private void doPostReblog(ImageView imgBtnReblog, ReaderPost post) {
        if (!hasActivity())
            return;

        if (post.isRebloggedByCurrentUser) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_already_reblogged);
            return;
        }

        imgBtnReblog.setSelected(true);
        AniUtils.zoomAction(imgBtnReblog);
        ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post);
    }

    /*
     * display the standard Android share chooser to share a link to this post
     */
    private void sharePage() {
        if (!hasActivity() || !hasPost())
            return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mPost.getUrl());
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reader_share_subject, getString(R.string.app_name)));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * get the latest version of this post - used to get latest like/comment counts
     */
    private void updatePost() {
        if (!hasPost() || !mPost.isWP())
            return;

        final int origNumLikes = mPost.numLikes;
        final int origNumReplies = mPost.numReplies;

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                switch (result) {
                    case CHANGED :
                        // post has changed, so get latest version
                        mPost = ReaderPostTable.getPost(mBlogId, mPostId);
                        doPostChanged(PostChangeType.CONTENT);
                        break;
                    case FAILED:
                        // failed to get post, so do nothing here
                        return;
                    default :
                        // unchanged
                        break;
                }

                // updating the post will get its liking users, so refresh likes here if any exist
                // to make sure the correct liking avatars are shown
                if (origNumLikes > 0 || mPost.numLikes > 0)
                    refreshLikes(false);

                // determine whether we need to make a separate request to update comments
                new Thread() {
                    @Override
                    public void run() {
                        final boolean isCommentsChanged = (mPost.numReplies != origNumReplies
                                                        || mPost.numReplies != ReaderCommentTable.getNumCommentsForPost(mPost));
                        if (isCommentsChanged) {
                            mHandler.post(new Runnable() {
                                public void run() {
                                    updateComments();
                                }
                            });
                        }
                    }
                }.start();
            }
        };
        ReaderPostActions.updatePost(mPost, resultListener);
    }

    /*
     * request comments for this post
     */
    private void updateComments() {
        if (!hasPost() || !mPost.isWP())
            return;
        if (mIsUpdatingComments)
            return;

        mIsUpdatingComments = true;

        if (!isCommentAdapterEmpty())
            showProgressFooter();

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                mIsUpdatingComments = false;
                if (!hasActivity())
                    return;
                hideProgressFooter();
                if (result == ReaderActions.UpdateResult.CHANGED) {
                    doPostChanged(PostChangeType.CONTENT);
                    refreshComments();
                }
            }
        };
        ReaderCommentActions.updateCommentsForPost(mPost, resultListener);
    }

    /*
     * show progress bar at the bottom of the screen - used when getting newer comments
     */
    private void showProgressFooter() {
        if (mProgressFooter == null || mProgressFooter.getVisibility() == View.VISIBLE )
            return;
        mProgressFooter.setVisibility(View.VISIBLE);
    }

    /*
     * hide the footer progress bar if it's showing
     */
    private void hideProgressFooter() {
        if (mProgressFooter == null || mProgressFooter.getVisibility() != View.VISIBLE )
            return;
        mProgressFooter.setVisibility(View.INVISIBLE);
    }

    /*
     * refresh adapter so latest comments appear
     */
    private void refreshComments() {
        getCommentAdapter().refreshComments();
    }

    /*
     * used by refreshLikes() to display the liking avatars - called only when there are avatars to
     * display (never called when there are no likes) - note that the passed list of avatar urls
     * has already been Photon-ized, so there's no need to do that here
     */
    private void showLikingAvatars(final ArrayList<String> avatarUrls,
                                   int maxAvatars,
                                   boolean forceReload) {
        final ViewGroup layoutLikingAvatars = (ViewGroup) mLayoutLikes.findViewById(R.id.layout_liking_avatars);

        // determine whether avatars need to be shown - goal is to avoid reloading them when
        // they're already displayed to prevent flicker
        final boolean reloadAvatars;
        if (forceReload) {
            // always reload avatars if force reload requested
            reloadAvatars = true;
        } else if (mPrevAvatarUrls.size() == avatarUrls.size()
                && mPrevAvatarUrls.containsAll(avatarUrls)) {
            // don't reload if these avatars are the same as last time
            reloadAvatars = false;
        } else {
            // avatars aren't the same as last time, but we can still skip showing
            // them if the view's child count indicates that we've already added
            // the max on a previous call to this routine
            reloadAvatars = (layoutLikingAvatars.getChildCount() < maxAvatars);
        }

        if (reloadAvatars) {
            AppLog.d(T.READER, "reader post detail > displaying liking avatars");
            layoutLikingAvatars.removeAllViews();
            LayoutInflater inflater = getActivity().getLayoutInflater();
            for (final String url: avatarUrls) {
                WPNetworkImageView imgAvatar = (WPNetworkImageView) inflater.inflate(R.layout.reader_like_avatar, layoutLikingAvatars, false);
                layoutLikingAvatars.addView(imgAvatar);
                imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);
            }

            // remember these avatars for next comparison
            mPrevAvatarUrls.clear();
            mPrevAvatarUrls.addAll(avatarUrls);
        }

        // show the liking layout if it's not already showing
        if (mLayoutLikes.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mLayoutLikes);
        }
    }
    /*
     * show latest likes for this post - pass true to force reloading avatars (used when user clicks
     * the like button, to ensure the current user's avatar appears first)
     */
    private void refreshLikes(final boolean forceReload) {
        if (!hasActivity())
            return;
        if (!hasPost() || !mPost.isWP())
            return;

        new Thread() {
            @Override
            public void run() {
                if (getView() == null) {
                    return;
                }
                final ImageView imgBtnLike = (ImageView) getView().findViewById(R.id.image_like_btn);
                final TextView txtLikeCount = (TextView) mLayoutLikes.findViewById(R.id.text_like_count);

                final int marginExtraSmall = getResources().getDimensionPixelSize(R.dimen.margin_extra_small);
                final int marginLarge = getResources().getDimensionPixelSize(R.dimen.margin_large);
                final int likeAvatarSize = getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
                final int likeAvatarSizeWithMargin = likeAvatarSize + (marginExtraSmall * 2);

                // determine how many avatars will fit the space
                final int displayWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
                final int spaceForAvatars = displayWidth - (marginLarge * 2);
                final int maxAvatars = spaceForAvatars / likeAvatarSizeWithMargin;

                // get avatar URLs of liking users up to the max, sized to fit
                ReaderUserIdList avatarIds = ReaderLikeTable.getLikesForPost(mPost);
                final ArrayList<String> avatars = ReaderUserTable.getAvatarUrls(avatarIds, maxAvatars, likeAvatarSize);

                mHandler.post(new Runnable() {
                    public void run() {
                        if (!hasActivity())
                            return;

                        imgBtnLike.setSelected(mPost.isLikedByCurrentUser);
                        imgBtnLike.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                togglePostLike(mPost, imgBtnLike);
                            }
                        });

                        // nothing more to do if no likes or liking avatars haven't been retrieved yet
                        if (avatars.size()==0 || mPost.numLikes==0) {
                            mLayoutLikes.setVisibility(View.GONE);
                            return;
                        }

                        // set the like count text
                        if (mPost.isLikedByCurrentUser) {
                            if (mPost.numLikes==1) {
                                txtLikeCount.setText(R.string.reader_likes_only_you);
                            } else {
                                txtLikeCount.setText(mPost.numLikes==2 ? getString(R.string.reader_likes_you_and_one) : getString(R.string.reader_likes_you_and_multi, mPost.numLikes-1));
                            }
                        } else {
                            txtLikeCount.setText(mPost.numLikes==1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, mPost.numLikes));
                        }

                        // clicking likes view shows activity displaying all liking users
                        mLayoutLikes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ReaderActivityLauncher.showReaderLikingUsers(getActivity(), mPost);
                            }
                        });

                        // now show the liking avatars
                        showLikingAvatars(avatars, maxAvatars, forceReload);
                    }
                });
            }
        }.start();
    }

    /*
     * show the view enabling adding a comment - triggered when user hits comment icon/count in header
     * note that this view is hidden at design time, so it will be shown the first time user taps icon.
     * pass 0 for the replyToCommentId to add a parent-level comment to the post, or pass a real
     * comment id to reply to a specific comment
     */
    private void showAddCommentBox(final long replyToCommentId) {
        if (!hasActivity())
            return;

        // skip if it's already showing
        if (mIsAddCommentBoxShowing)
            return;

        // don't show comment box if a comment is currently being submitted
        if (mIsSubmittingComment)
            return;

        final ViewGroup layoutCommentBox = (ViewGroup) getView().findViewById(R.id.layout_comment_box);
        final EditText editComment = (EditText) layoutCommentBox.findViewById(R.id.edit_comment);
        final ImageView imgBtnComment = (ImageView) getView().findViewById(R.id.image_comment_btn);

        // disable full-screen when comment box is showing
        if (isFullScreen())
            setIsFullScreen(false);

        // different hint depending on whether user is replying to a comment or commenting on the post
        editComment.setHint(replyToCommentId==0 ? R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        imgBtnComment.setSelected(true);
        AniUtils.flyIn(layoutCommentBox);

        editComment.requestFocus();
        editComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_SEND)
                    submitComment(replyToCommentId);
                return false;
            }
        });

        // submit comment when image tapped
        final ImageView imgPostComment = (ImageView) getView().findViewById(R.id.image_post_comment);
        imgPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment(replyToCommentId);
            }
        });

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editComment, InputMethodManager.SHOW_IMPLICIT);

        // if user is replying to another comment, highlight the comment being replied to and scroll
        // it to the top so the user can see which comment they're replying to - note that scrolling
        // is delayed to give time for listView to reposition due to soft keyboard appearing
        if (replyToCommentId!=0) {
            getCommentAdapter().setHighlightCommentId(replyToCommentId, false);
            getListView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollToCommentId(replyToCommentId);
                }
            }, 300);
        }

        // mReplyToCommentId must be saved here so it can be stored by onSaveInstanceState()
        mReplyToCommentId = replyToCommentId;
        mIsAddCommentBoxShowing = true;
    }

    private void hideAddCommentBox() {
        if (!hasActivity())
            return;
        if (!mIsAddCommentBoxShowing)
            return;

        final ViewGroup layoutCommentBox = (ViewGroup) getView().findViewById(R.id.layout_comment_box);
        final EditText editComment = (EditText) layoutCommentBox.findViewById(R.id.edit_comment);
        final ImageView imgBtnComment = (ImageView) getView().findViewById(R.id.image_comment_btn);

        imgBtnComment.setSelected(false);
        AniUtils.flyOut(layoutCommentBox);
        EditTextUtils.hideSoftInput(editComment);

        getCommentAdapter().setHighlightCommentId(0, false);

        mIsAddCommentBoxShowing = false;
        mReplyToCommentId = 0;
    }

    private void toggleShowAddCommentBox() {
        if (mIsAddCommentBoxShowing) {
            hideAddCommentBox();
        } else {
            showAddCommentBox(0);
        }
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private void scrollToCommentId(long commentId) {
        int position = getCommentAdapter().indexOfCommentId(commentId);
        if (position == -1)
            return;
        getListView().setSelectionFromTop(position + getListView().getHeaderViewsCount(), 0);
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private boolean mIsSubmittingComment = false;
    private void submitComment(final long replyToCommentId) {
        final EditText editComment = (EditText) getView().findViewById(R.id.edit_comment);
        final String commentText = EditTextUtils.getText(editComment);
        if (TextUtils.isEmpty(commentText))
            return;

        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_COMMENTED_ON_ARTICLE);

        // hide the comment box - this provides immediate indication that comment is being posted
        // and prevents users from submitting the same comment twice
        hideAddCommentBox();

        // generate a "fake" comment id to assign to the new comment so we can add it to the db
        // and reflect it in the adapter before the API call returns
        final long fakeCommentId = ReaderCommentActions.generateFakeCommentId();

        mIsSubmittingComment = true;
        ReaderActions.CommentActionListener actionListener = new ReaderActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded, ReaderComment newComment) {
                mIsSubmittingComment = false;
                if (!hasActivity())
                    return;
                if (succeeded) {
                    // comment posted successfully so stop highlighting the fake one and replace
                    // it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    getListView().invalidateViews();
                } else {
                    // comment failed to post - show the comment box again with the comment text intact,
                    // and remove the "fake" comment from the adapter
                    editComment.setText(commentText);
                    showAddCommentBox(replyToCommentId);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
            }
        };

        final ReaderComment newComment = ReaderCommentActions.submitPostComment(mPost,
                                                                                fakeCommentId,
                                                                                commentText,
                                                                                replyToCommentId,
                                                                                actionListener);
        if (newComment!=null) {
            doPostChanged(PostChangeType.CONTENT);
            editComment.setText(null);
            // add the "fake" comment to the adapter, highlight it, and show a progress bar
            // next to it while it's submitted
            getCommentAdapter().setHighlightCommentId(newComment.commentId, true);
            getCommentAdapter().addComment(newComment);
            // make sure it's scrolled into view
            scrollToCommentId(fakeCommentId);
        }
    }

    /*
     * refresh the follow button based on whether this is a followed blog
     */
    private void refreshFollowed() {
        if (!hasActivity())
            return;

        final TextView txtFollow = (TextView) getView().findViewById(R.id.text_follow);
        final boolean isFollowed = ReaderPostTable.isPostFollowed(mPost);

        ReaderUtils.showFollowStatus(txtFollow, isFollowed);
    }

    /*
     * creates formatted div for passed video with passed (optional) thumbnail
     */
    private static final String OVERLAY_IMG = "file:///android_asset/ic_reader_video_overlay.png";
    private String makeVideoDiv(String videoUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(videoUrl))
            return "";

        // sometimes we get src values like "//player.vimeo.com/video/70534716" - prefix these with http:
        if (videoUrl.startsWith("//"))
            videoUrl = "http:" + videoUrl;

        int overlaySz = getResources().getDimensionPixelSize(R.dimen.reader_video_overlay_size) / 2;

        if (TextUtils.isEmpty(thumbnailUrl)) {
            return String.format("<div class='wpreader-video' align='center'><a href='%s'><img style='width:%dpx; height:%dpx; display:block;' src='%s' /></a></div>", videoUrl, overlaySz, overlaySz, OVERLAY_IMG);
        } else {
            return "<div style='position:relative'>"
                    + String.format("<a href='%s'><img src='%s' style='width:100%%; height:auto;' /></a>", videoUrl, thumbnailUrl)
                    + String.format("<a href='%s'><img src='%s' style='width:%dpx; height:%dpx; position:absolute; left:0px; right:0px; top:0px; bottom:0px; margin:auto;'' /></a>", videoUrl, OVERLAY_IMG, overlaySz, overlaySz)
                    + "</div>";
        }
    }

    /*
     * called when user taps an image in the webView - shows the image full-screen
     */
    private void showPhotoViewer(final String imageUrl) {
        if (TextUtils.isEmpty(imageUrl))
            return;
        if (!hasActivity())
            return;
        // images in private posts must use https for auth token to be sent with request
        if (mPost.isPrivate) {
            ReaderActivityLauncher.showReaderPhotoViewer(getActivity(), UrlUtils.makeHttps(imageUrl));
        } else {
            ReaderActivityLauncher.showReaderPhotoViewer(getActivity(), imageUrl);
        }
    }

    private boolean hasStaticMenuDrawer() {
        return (getActivity() instanceof WPActionBarActivity)
            && (((WPActionBarActivity) getActivity()).isStaticMenuDrawer());
    }

    /*
     * size to use for images that fit the full width of the listView item
     */
    private int getFullSizeImageWidth() {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
        int marginWidth = getResources().getDimensionPixelOffset(R.dimen.reader_list_margin);
        int imageWidth = displayWidth - (marginWidth * 2);
        if (hasStaticMenuDrawer()) {
            int drawerWidth = getResources().getDimensionPixelOffset(R.dimen.menu_drawer_width);
            imageWidth -= drawerWidth;
        }
        return imageWidth;
    }

    /*
     * build html for post's content
     */
    private String getPostHtml(Context context) {
        if (mPost == null || context == null) {
            return "";
        }

        String content;
        if (mPost.hasText()) {
            // some content (such as Vimeo embeds) don't have "http:" before links, correct this here
            content = mPost.getText().replace("src=\"//", "src=\"http://");
            // insert video div before content if this is a VideoPress post (video otherwise won't appear)
            if (mPost.isVideoPress)
                content = makeVideoDiv(mPost.getFeaturedVideo(), mPost.getFeaturedImage()) + content;
        } else if (mPost.hasFeaturedImage()) {
            // some photo blogs have posts with empty content but still have a featured image, so
            // use the featured image as the content
            content = String.format("<p><img class='img.size-full' src='%s' /></p>", mPost.getFeaturedImage());
        } else {
            content = "";
        }

        int marginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);
        int marginSmall = context.getResources().getDimensionPixelSize(R.dimen.margin_small);
        int marginExtraSmall = context.getResources().getDimensionPixelSize(R.dimen.margin_extra_small);
        int fullSizeImageWidth = getFullSizeImageWidth();

        final String linkColor = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        final String greyLight = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        final String greyExtraLight = HtmlUtils.colorResToHtmlColor(context, R.color.grey_extra_light);

        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't strictly necessary, but source is invalid html5 without one
        sbHtml.append("<title>Reader Post</title>");

        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        sbHtml.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");

        // use "Open Sans" Google font
        sbHtml.append("<link rel='stylesheet' type='text/css' href='http://fonts.googleapis.com/css?family=Open+Sans' />");

        sbHtml.append("<style type='text/css'>")
              .append("  body { font-family: 'Open Sans', sans-serif; margin: 0px; padding: 0px;}")
              .append("  body, p, div { max-width: 100% !important;}")
              .append("  p, div { line-height: 1.6em; font-size: 1em; }")
              .append("  h1, h2 { line-height: 1.2em; }");

        // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
        sbHtml.append(String.format("  p { margin-top: %dpx; margin-bottom: %dpx; }", marginSmall, marginSmall))
              .append("    p:first-child { margin-top: 0px; }");

        // add border, background color, and padding to pre blocks, and add overflow scrolling
        // so user can scroll the block if it's wider than the display
        sbHtml.append("  pre { overflow-x: scroll;")
              .append("        border: 1px solid ").append(greyLight).append("; ")
              .append("        background-color: ").append(greyExtraLight).append("; ")
              .append("        padding: ").append(marginSmall).append("px; }");

        // make sure links don't overflow and are shown in the same color they are elsewhere in the app
        sbHtml.append("  a { word-wrap: break-word; text-decoration: none; color: ").append(linkColor).append("; }");

        if (hasEmbedsOrIframes()) {
            // make sure embedded videos fit the browser width and use 16:9 ratio (YouTube standard)
            int videoWidth =  DisplayUtils.pxToDp(context, fullSizeImageWidth - (marginLarge * 2));
            int videoHeight = (int)(videoWidth * 0.5625f);
            sbHtml.append("  iframe, embed { width: ").append(videoWidth).append("px !important;")
                  .append("                  height: ").append(videoHeight).append("px !important; }");
        } else {
            sbHtml.append("  iframe, embed { display: none; }");
        }

        // don't allow any image to be wider than the screen
        sbHtml.append("  img { max-width: 100% !important; height: auto;}");

        // show large wp images full-width (unnecessary in most cases since they'll already be at least
        // as wide as the display, except maybe when viewed on a large landscape tablet)
        sbHtml.append("  img.size-full, img.size-large { display: block; width: 100% !important; height: auto; }");

        // center medium-sized wp image
        sbHtml.append("  img.size-medium { display: block; margin-left: auto !important; margin-right: auto !important; }");

        // tiled image galleries look bad on mobile due to their hard-coded DIV and IMG sizes, so if
        // content contains a tiled image gallery, remove the height params and replace the width
        // params with ones that make images fit the width of the listView item, then adjust the
        // relevant CSS classes so their height/width are auto, and add top/bottom margin to images
        if (content.contains("tiled-gallery-item")) {
            String widthParam = "w=" + Integer.toString(fullSizeImageWidth);
            content = content.replaceAll("w=[0-9]+", widthParam).replaceAll("h=[0-9]+", "");
            sbHtml.append("  div.gallery-row, div.gallery-group { width: auto !important; height: auto !important; }")
                  .append("  div.tiled-gallery-item img { ")
                  .append("     width: auto !important; height: auto !important;")
                  .append("     margin-top: ").append(marginExtraSmall).append("px; ")
                  .append("     margin-bottom: ").append(marginExtraSmall).append("px; ")
                  .append(" }")
                  .append("  div.tiled-gallery-caption { clear: both; }");
        }

        sbHtml.append("</style></head><body>")
              .append(content)
              .append("</body></html>");

        return sbHtml.toString();
    }

    /*
     *  called when the post doesn't exist in local db, need to get it from server
     */
    private void requestPost() {
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_loading);
        progress.setVisibility(View.VISIBLE);
        progress.bringToFront();

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (hasActivity()) {
                    progress.setVisibility(View.GONE);
                    if (succeeded) {
                        showPost();
                    } else {
                        postFailed();
                    }
                }
            }
        };
        ReaderPostActions.requestPost(mBlogId, mPostId, actionListener);
    }

    /*
     * called when post couldn't be loaded and failed to be returned from server
     */
    private void postFailed() {
        if (hasActivity()) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_post, ToastUtils.Duration.LONG);
        }
    }

    /*
     * returns true if content contains embed or iframe tags - if so then JavaScript is enabled
     * and CSS is customized so embedded videos can play
     */
    private boolean hasEmbedsOrIframes() {
        if (mPost == null) {
            return false;
        } else if (mPost.isVideoPress || mPost.hasFeaturedVideo()) {
            return true;
        } else {
            return (mPost.getText().contains("<embed")
                 || mPost.getText().contains("<iframe")
                 || mPost.getText().contains("videos.files.wordpress.com"));
        }
    }

    private void showPost() {
        if (mIsPostTaskRunning) {
            AppLog.w(T.READER, "reader post detail > show post task already running");
        }

        new ShowPostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * AsyncTask to retrieve this post from SQLite and display it
     */
    private boolean mIsPostTaskRunning = false;
    private class ShowPostTask extends AsyncTask<Void, Void, Boolean> {
        TextView txtTitle;
        TextView txtBlogName;
        TextView txtAuthorName;
        TextView txtDate;
        TextView txtFollow;

        ImageView imgBtnReblog;
        ImageView imgBtnComment;

        WPNetworkImageView imgAvatar;
        WPNetworkImageView imgFeatured;

        String postHtml;
        String featuredImageUrl;
        boolean showFeaturedImage;

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
            final View container = getView();
            if (container == null) {
                return false;
            }

            txtTitle = (TextView) container.findViewById(R.id.text_title);
            txtBlogName = (TextView) container.findViewById(R.id.text_blog_name);
            txtDate = (TextView) container.findViewById(R.id.text_date);
            txtFollow = (TextView) container.findViewById(R.id.text_follow);
            txtAuthorName = (TextView) container.findViewById(R.id.text_author_name);

            imgAvatar = (WPNetworkImageView) container.findViewById(R.id.image_avatar);
            imgFeatured = (WPNetworkImageView) container.findViewById(R.id.image_featured);

            imgBtnReblog = (ImageView) mLayoutIcons.findViewById(R.id.image_reblog_btn);
            imgBtnComment = (ImageView) mLayoutIcons.findViewById(R.id.image_comment_btn);

            mPost = ReaderPostTable.getPost(mBlogId, mPostId);
            if (mPost == null) {
                return false;
            }
            postHtml = getPostHtml(container.getContext());

            // detect whether the post has a featured image that's not in the content - if so,
            // it will be shown between the post's title and its content (but skip mshots)
            if (mPost.hasFeaturedImage() && !PhotonUtils.isMshotsUrl(mPost.getFeaturedImage())) {
                Uri uri = Uri.parse(mPost.getFeaturedImage());
                String path = StringUtils.notNullStr(uri.getLastPathSegment());
                if (!mPost.getText().contains(path)) {
                    showFeaturedImage = true;
                    // note that only the width is used here - the imageView will adjust
                    // the height to match that of the image once loaded
                    featuredImageUrl = mPost.getFeaturedImageForDisplay(getFullSizeImageWidth(), 0);
                }
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mIsPostTaskRunning = false;

            if (!hasActivity()) {
                return;
            }

            if (!result) {
                // post couldn't be loaded which means it doesn't exist in db, so request it from
                // the server if it hasn't already been requested
                if (!mHasAlreadyRequestedPost) {
                    mHasAlreadyRequestedPost = true;
                    requestPost();
                }
                return;
            }

            // set the activity title to the post's title if this is being shown in the reader
            final String postTitle = mPost.hasTitle() ? mPost.getTitle() : getString(R.string.reader_untitled_post);
            if (hostIsReaderActivity()) {
                getActivity().setTitle(postTitle);
            }

            txtTitle.setText(postTitle);
            txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished()));

            ReaderUtils.showFollowStatus(txtFollow, mPost.isFollowedByCurrentUser);
            txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePostFollowed(mPost, txtFollow);
                }
            });

            // if we know refreshLikes() is going to show the liking layout, force it to take up
            // space right now
            if (mPost.numLikes > 0 && mLayoutLikes.getVisibility() == View.GONE)
                mLayoutLikes.setVisibility(View.INVISIBLE);

            if (mPost.hasBlogName()) {
                txtBlogName.setText(mPost.getBlogName());
                txtBlogName.setVisibility(View.VISIBLE);
            } else {
                txtBlogName.setVisibility(View.GONE);
            }

            // show author name if it exists and is different than the blog name
            if (mPost.hasAuthorName() && !mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtAuthorName.setText(mPost.getAuthorName());
                txtAuthorName.setVisibility(View.VISIBLE);
            } else {
                txtAuthorName.setVisibility(View.GONE);
            }

            if (mPost.hasPostAvatar()) {
                int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);
                imgAvatar.setVisibility(View.VISIBLE);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }

            if (showFeaturedImage) {
                imgFeatured.setVisibility(View.VISIBLE);
                imgFeatured.setImageUrl(featuredImageUrl, WPNetworkImageView.ImageType.PHOTO);
                imgFeatured.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPhotoViewer(mPost.getFeaturedImage());
                    }
                });
            } else {
                imgFeatured.setVisibility(View.GONE);
            }

            // enable reblogging wp posts
            imgBtnReblog.setVisibility(mPost.isWP() ? View.VISIBLE : View.GONE);
            imgBtnReblog.setSelected(mPost.isRebloggedByCurrentUser);
            if (mPost.isWP()) {
                imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doPostReblog(imgBtnReblog, mPost);
                    }
                });
            }

            // enable adding a comment if comments are open on this post
            if (mPost.isWP() && mPost.isCommentsOpen) {
                imgBtnComment.setVisibility(View.VISIBLE);
                imgBtnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleShowAddCommentBox();
                    }
                });
            } else {
                imgBtnComment.setVisibility(View.GONE);
            }

            // tapping title opens post in browser
            txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getActivity(), mPost.getUrl());
                }
            });

            // enable JavaScript in the webView if the post content contains embeds or iframes
            // so embedded videos will work
            mWebView.getSettings().setJavaScriptEnabled(hasEmbedsOrIframes());

            // IMPORTANT: use loadDataWithBaseURL() since loadData() may fail
            // https://code.google.com/p/android/issues/detail?id=4401
            mWebView.loadDataWithBaseURL(null, postHtml, "text/html", "UTF-8", null);

            // only show action buttons for WP posts
            if (mPost.isWP() && mLayoutIcons.getVisibility() != View.VISIBLE)
                animateIconBar(true);

            // make sure the adapter is assigned
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getCommentAdapter());
            }

            // listView is hidden in onCreateView()
            if (getListView().getVisibility() != View.VISIBLE) {
                getListView().setVisibility(View.VISIBLE);
            }

            // webView is hidden in onCreateView() and will be made visible by readerWebViewClient
            // once it finishes loading, so if it's already visible go ahead and show likes/comments
            // right away, otherwise show them after a brief delay - this gives content time to
            // load before likes/comments appear
            if (mWebView.getVisibility() == View.VISIBLE) {
                showContent();
            } else {
                showContentDelayed();
            }
        }
    }

    /*
     * webView is hidden in onCreateView() and then shown after a brief delay once post is loaded
     * to give webView content a short time to load before it appears - after it appears we can
     * then get likes & comments
     */
    private void showContentDelayed() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWebView.getVisibility() != View.VISIBLE)
                    AppLog.d(T.READER, "reader post detail > webView shown before page finished");
                showContent();
            }
        }, 1000L);
    }

    private void showContent() {
        if (!hasActivity())
            return;

        mWebView.setVisibility(View.VISIBLE);

        // show likes & comments
        refreshLikes(false);
        refreshComments();

        // request the latest info for this post if we haven't updated it already
        if (!mHasAlreadyUpdatedPost) {
            updatePost();
            mHasAlreadyUpdatedPost = true;
        }
    }

    private final WebViewClient readerWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            // show the webView now that it has loaded
            if (view.getVisibility() != View.VISIBLE)
                view.setVisibility(View.VISIBLE);
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // open clicked urls in default browser or else urls will open in this webView,
            // but only do this when webView has loaded (is visible) - have seen some posts
            // containing iframes automatically try to open urls (without being clicked)
            // before the page has loaded
            if (view.getVisibility() == View.VISIBLE) {
                openUrl(url);
                return true;
            } else {
                return false;
            }
        }
    };

    /*
     * called when user taps a link in the webView
     */
    private void openUrl(String url) {
        if (!hasActivity() || TextUtils.isEmpty(url)) {
            return;
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
    }

    private ActionBar getActionBar() {
        if (hasActivity()) {
            return getActivity().getActionBar();
        } else {
            AppLog.w(T.READER, "reader post detail > getActionBar called with no activity");
            return null;
        }
    }

    private void pauseWebView() {
        if (mWebViewIsPaused && mWebView != null) {
            mWebViewIsPaused = true;
            mWebView.onPause();
        }
    }

    private void resumeWebView() {
        if (!mWebViewIsPaused && mWebView != null) {
            mWebViewIsPaused = false;
            mWebView.onResume();
        }
    }
}
