package org.wordpress.android.ui.reader;

import android.animation.Animator;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.ReaderWebView.ReaderCustomViewListener;
import org.wordpress.android.ui.reader.ReaderWebView.ReaderWebViewUrlClickListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPListView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

public class ReaderPostDetailFragment extends Fragment
        implements WPListView.OnScrollDirectionListener,
                   ReaderCustomViewListener,
                   ReaderWebViewUrlClickListener {

    private static final String KEY_SHOW_COMMENT_BOX = "show_comment_box";
    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;

    private ReaderPostListType mPostListType;

    private ViewGroup mLayoutIcons;
    private ViewGroup mLayoutLikes;
    private ViewGroup mHeaderDetail;
    private WPListView mListView;
    private ViewGroup mCommentFooter;
    private ProgressBar mProgressFooter;
    private ReaderWebView mReaderWebView;
    private View mNotificationCommentView;

    private boolean mIsAddCommentBoxShowing;
    private long mReplyToCommentId = 0;

    private boolean mHasAlreadyUpdatedPost;
    private boolean mHasAlreadyRequestedPost;
    private boolean mIsUpdatingComments;

    private long mTopMostCommentId;
    private int mTopMostCommentTop;

    private Parcelable mListState;
    private final Handler mHandler = new Handler();

    private ReaderUtils.FullScreenListener mFullScreenListener;

    // Used for loading a comment notification
    private Note mNote;

    public static ReaderPostDetailFragment newInstance(long blogId, long postId) {
        return newInstance(blogId, postId, null);
    }

    public static ReaderPostDetailFragment newInstance(long blogId,
                                                       long postId,
                                                       ReaderPostListType postListType) {
        AppLog.d(T.READER, "reader post detail > newInstance");
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE);

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putLong(ReaderConstants.ARG_POST_ID, postId);
        if (postListType != null) {
            args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }

        ReaderPostDetailFragment fragment = new ReaderPostDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /*
     * adapter containing comments for this post
     */
    private ReaderCommentAdapter mCommentAdapter;

    private ReaderCommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            ReaderActions.DataLoadedListener dataLoadedListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (isAdded()) {
                        // show footer below comments when comments exist
                        mCommentFooter.setVisibility(isEmpty && mNote == null ? View.GONE : View.VISIBLE);
                        // restore listView state (scroll position) if it was saved during rotation
                        if (mListState != null) {
                            if (!isEmpty) {
                                getListView().onRestoreInstanceState(mListState);
                            }
                            mListState = null;
                        }
                        if (mTopMostCommentId != 0) {
                            restoreTopmostComment();
                        }

                        if (mNote != null && !isEmpty && mListView.getVisibility() == View.INVISIBLE) {
                            scrollToCommentId(mNote.getCommentId());

                            mListView.setAlpha(0.0f);
                            mListView.setVisibility(View.VISIBLE);
                            mListView.animate().alpha(1.0f).setStartDelay(300).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mHeaderDetail.setAlpha(0.0f);
                                    mHeaderDetail.setVisibility(View.VISIBLE);
                                    mHeaderDetail.animate().alpha(1.0f).setStartDelay(300);

                                    if (mNotificationCommentView != null) {
                                        mNotificationCommentView.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            });
                        }

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
                public void onRequestData() {
                    if (!mIsUpdatingComments) {
                        AppLog.i(T.READER, "reader post detail > requesting newer comments");
                        updateComments(true);
                    }
                }
            };
            mCommentAdapter = new ReaderCommentAdapter(getActivity(), mPost, replyListener, dataLoadedListener, dataRequestedListener);
        }
        return mCommentAdapter;
    }

    /*
     * called before new comments are shown so the current topmost comment is remembered
     */
    private void retainTopmostComment() {
        int position = getListView().getFirstVisiblePosition();
        int numHeaders = getListView().getHeaderViewsCount();
        if (position > numHeaders) {
            mTopMostCommentId = getCommentAdapter().getItemId(position - numHeaders);
            View v = getListView().getChildAt(0);
            mTopMostCommentTop = (v != null ? v.getTop() : 0);
        } else {
            mTopMostCommentId = 0;
            mTopMostCommentTop = 0;
        }
    }

    /*
     * called after new comments are shown so the previous topmost comment is scrolled to
     */
    private void restoreTopmostComment() {
        if (mTopMostCommentId != 0) {
            int position = getCommentAdapter().indexOfCommentId(mTopMostCommentId);
            if (position > -1) {
                getListView().setSelectionFromTop(position + getListView().getHeaderViewsCount(), mTopMostCommentTop);
            }
            mTopMostCommentId = 0;
            mTopMostCommentTop = 0;
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = args.getLong(ReaderConstants.ARG_POST_ID);
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
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
        mHeaderDetail = (ViewGroup) inflater.inflate(R.layout.reader_listitem_post_detail, mListView, false);
        mListView.addHeaderView(mHeaderDetail, null, false);

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

        // setup the ReaderWebView
        mReaderWebView = (ReaderWebView) view.findViewById(R.id.reader_webview);
        mReaderWebView.setCustomViewListener(this);
        mReaderWebView.setUrlClickListener(this);

        // hide these views until the post is loaded
        mListView.setVisibility(View.INVISIBLE);
        mReaderWebView.setVisibility(View.INVISIBLE);
        mLayoutIcons.setVisibility(View.INVISIBLE);

        // if loaded with a note, we'll load the note instantly in a special ReaderComment view
        if (mNote != null) {
            mHeaderDetail.setVisibility(View.INVISIBLE);
            ReaderComment comment = ReaderComment.fromNote(mNote);
            mNotificationCommentView = getCommentAdapter().getViewForComment(comment);

            if (mNotificationCommentView != null) {
                RelativeLayout viewContainer = (RelativeLayout) view.findViewById(R.id.layout_post_detail_container);
                viewContainer.addView(mNotificationCommentView);
            }
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReaderWebView != null) {
            mReaderWebView.destroy();
        }
    }

    private WPListView getListView() {
        return mListView;
    }

    @Override
    public void onScrollUp() {
        // return from full screen when scrolling up unless user is typing a comment
        if (isFullScreen() && !mIsAddCommentBoxShowing) {
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
                if (hasPost()) {
                    ReaderActivityLauncher.openUrl(getActivity(), mPost.getUrl(), OpenUrlType.EXTERNAL);
                }
                return true;
            case R.id.menu_share:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHARED_ITEM);
                sharePage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        // this tells the host activity to enable/disable fullscreen
        if (mFullScreenListener != null && mFullScreenListener.onRequestFullScreen(enableFullScreen)) {
            if (mPost.isWP()) {
                animateIconBar(!enableFullScreen);
            }
        }
    }

    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    private boolean isBlogPreview() {
        return (getPostListType() == ReaderPostListType.BLOG_PREVIEW);
    }

    /*
     * animate in/out the layout containing the reblog/comment/like icons
     */
    private void animateIconBar(boolean isAnimatingIn) {
        if (isAnimatingIn && mLayoutIcons.getVisibility() == View.VISIBLE) {
            return;
        }
        if (!isAnimatingIn && mLayoutIcons.getVisibility() != View.VISIBLE) {
            return;
        }

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

    public void setNote(Note note) {
        this.mNote = note;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);

        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasAlreadyRequestedPost);
        outState.putBoolean(KEY_SHOW_COMMENT_BOX, mIsAddCommentBoxShowing);
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

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

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ReaderUtils.FullScreenListener) {
            mFullScreenListener = (ReaderUtils.FullScreenListener) activity;
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

        restoreState(savedInstanceState);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mHasAlreadyRequestedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED);
            if (savedInstanceState.getBoolean(KEY_SHOW_COMMENT_BOX)) {
                long replyToCommentId = savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID);
                showAddCommentBox(replyToCommentId);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
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
        super.onPause();

        // this ensures embedded videos don't continue to play when the fragment is no longer
        // active or has been detached
        pauseWebView();
    }

    /*
     * changes the like on the passed post
     */
    private void togglePostLike(ReaderPost post, View likeButton) {
        boolean isSelected = likeButton.isSelected();
        likeButton.setSelected(!isSelected);

        boolean isAskingToLike = !post.isLikedByCurrentUser;
        ReaderAnim.animateLikeButton(likeButton, isAskingToLike);

        if (!ReaderPostActions.performLikeAction(post, isAskingToLike)) {
            likeButton.setSelected(isSelected);
            return;
        }

        // get the post again since it has changed, then refresh to show changes
        mPost = ReaderPostTable.getPost(mBlogId, mPostId);
        refreshLikes();

        if (isAskingToLike) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LIKED_ARTICLE);
        }
    }

    /*
     * change the follow state of the blog the passed post is in
     */
    private void togglePostFollowed(ReaderPost post, View followButton) {
        boolean isSelected = followButton.isSelected();
        followButton.setSelected(!isSelected);
        ReaderAnim.animateFollowButton(followButton);

        final boolean isAskingToFollow = !post.isFollowedByCurrentUser;
        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
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

        // call returns before api completes, but local version of post will have been changed
        // so refresh to show those changes
        refreshFollowed();
    }

    /*
     * called when user chooses to reblog the post
     */
    private void doPostReblog(ImageView imgBtnReblog, ReaderPost post) {
        if (!isAdded()) {
            return;
        }

        if (post.isRebloggedByCurrentUser) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_already_reblogged);
            return;
        }

        imgBtnReblog.setSelected(true);
        ReaderAnim.animateReblogButton(imgBtnReblog);
        ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post, imgBtnReblog);
    }

    /*
     * display the standard Android share chooser to share a link to this post
     */
    private void sharePage() {
        if (!isAdded() || !hasPost())
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
     * get the latest version of this post so we can show the latest likes/comments
     */
    private void updatePost() {
        if (!hasPost() || !mPost.isWP()) {
            return;
        }

        final int numLikesBefore = mPost.numLikes;

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result != ReaderActions.UpdateResult.FAILED) {
                    mPost = ReaderPostTable.getPost(mBlogId, mPostId);
                    if (numLikesBefore != mPost.numLikes) {
                        refreshLikes();
                    }
                    updateComments(false);
                }
            }
        };
        ReaderPostActions.updatePost(mPost, resultListener);
    }

    /*
     * request comments for this post
     */
    private void updateComments(boolean requestNewer) {
        if (!hasPost() || !mPost.isWP()) {
            return;
        }
        if (mIsUpdatingComments) {
            AppLog.w(T.READER, "reader post detail > already updating comments");
            return;
        }

        AppLog.d(T.READER, "reader post detail > updateComments");
        mIsUpdatingComments = true;

        // show progress if we're requesting newer comments
        if (requestNewer) {
            showProgressFooter();
        }

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                mIsUpdatingComments = false;
                if (!isAdded()) {
                    return;
                }
                hideProgressFooter();
                if (result == ReaderActions.UpdateResult.CHANGED) {
                    retainTopmostComment();
                    refreshComments();
                }
            }
        };
        ReaderCommentActions.updateCommentsForPost(mPost, requestNewer, resultListener);
    }

    /*
     * show progress bar at the bottom of the screen - used when getting newer comments
     */
    private void showProgressFooter() {
        if (mProgressFooter != null && mProgressFooter.getVisibility() != View.VISIBLE) {
            mProgressFooter.setVisibility(View.VISIBLE);
        }
    }

    /*
     * hide the footer progress bar if it's showing
     */
    private void hideProgressFooter() {
        if (mProgressFooter != null && mProgressFooter.getVisibility() == View.VISIBLE) {
            mProgressFooter.setVisibility(View.INVISIBLE);
        }
    }

    /*
     * refresh adapter so latest comments appear
     */
    private void refreshComments() {
        AppLog.d(T.READER, "reader post detail > refreshComments");
        getCommentAdapter().refreshComments();
    }

    /*
     * show latest likes for this post
     */
    private void refreshLikes() {
        AppLog.d(T.READER, "reader post detail > refreshLikes");
        if (!isAdded() || !hasPost() || !mPost.isWP() || !mPost.isLikesEnabled) {
            return;
        }

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
                        if (!isAdded()) {
                            return;
                        }

                        imgBtnLike.setSelected(mPost.isLikedByCurrentUser);
                        imgBtnLike.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                togglePostLike(mPost, imgBtnLike);
                            }
                        });

                        // nothing more to do if no likes or liking avatars haven't been retrieved yet
                        if (avatars.size() == 0 || mPost.numLikes == 0) {
                            if (mLayoutLikes.getVisibility() != View.GONE) {
                                ReaderAnim.fadeOut(mLayoutLikes, ReaderAnim.Duration.SHORT);
                            }
                            return;
                        }

                        // set the like count text
                        if (mPost.isLikedByCurrentUser) {
                            if (mPost.numLikes == 1) {
                                txtLikeCount.setText(R.string.reader_likes_only_you);
                            } else {
                                txtLikeCount.setText(mPost.numLikes == 2 ? getString(R.string.reader_likes_you_and_one) : getString(R.string.reader_likes_you_and_multi, mPost.numLikes - 1));
                            }
                        } else {
                            txtLikeCount.setText(mPost.numLikes == 1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, mPost.numLikes));
                        }

                        // clicking likes view shows activity displaying all liking users
                        mLayoutLikes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ReaderActivityLauncher.showReaderLikingUsers(getActivity(), mPost);
                            }
                        });

                        if (mLayoutLikes.getVisibility() != View.VISIBLE) {
                            ReaderAnim.fadeIn(mLayoutLikes, ReaderAnim.Duration.SHORT);
                        }

                        showLikingAvatars(avatars);
                    }
                });
            }
        }.start();
    }

    /*
     * used by refreshLikes() to display the liking avatars - called only when there are avatars to
     * display (never called when there are no likes) - note that the passed list of avatar urls
     * has already been Photon-ized, so there's no need to do that here
     */
    private void showLikingAvatars(final ArrayList<String> avatarUrls) {
        ViewGroup layoutLikingAvatars = (ViewGroup) mLayoutLikes.findViewById(R.id.layout_liking_avatars);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // remove excess existing views
        int numExistingViews = layoutLikingAvatars.getChildCount();
        if (numExistingViews > avatarUrls.size()) {
            int numToRemove = numExistingViews - avatarUrls.size();
            layoutLikingAvatars.removeViews(numExistingViews - numToRemove, numToRemove);
        }

        int index = 0;
        for (String url : avatarUrls) {
            WPNetworkImageView imgAvatar;
            // reuse existing view when possible, otherwise inflate a new one
            if (index < numExistingViews) {
                imgAvatar = (WPNetworkImageView) layoutLikingAvatars.getChildAt(index);
            } else {
                imgAvatar = (WPNetworkImageView) inflater.inflate(R.layout.reader_like_avatar, layoutLikingAvatars, false);
                layoutLikingAvatars.addView(imgAvatar);
            }
            imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);
            index++;
        }
    }

    /*
     * show the view enabling adding a comment - triggered when user hits comment icon/count in header
     * note that this view is hidden at design time, so it will be shown the first time user taps icon.
     * pass 0 for the replyToCommentId to add a parent-level comment to the post, or pass a real
     * comment id to reply to a specific comment
     */
    private void showAddCommentBox(final long replyToCommentId) {
        if (!isAdded())
            return;

        // skip if it's already showing or if a comment is currently being submitted
        if (mIsAddCommentBoxShowing || mIsSubmittingComment) {
            return;
        }

        final ViewGroup layoutCommentBox = (ViewGroup) getView().findViewById(R.id.layout_comment_box);
        final EditText editComment = (EditText) layoutCommentBox.findViewById(R.id.edit_comment);
        final ImageView imgBtnComment = (ImageView) getView().findViewById(R.id.image_comment_btn);

        // disable full-screen when comment box is showing
        if (isFullScreen()) {
            setIsFullScreen(false);
        }

        // different hint depending on whether user is replying to a comment or commenting on the post
        editComment.setHint(replyToCommentId == 0 ? R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        imgBtnComment.setSelected(true);
        AniUtils.flyIn(layoutCommentBox);

        editComment.requestFocus();
        editComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                    submitComment(replyToCommentId);
                }
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

        EditTextUtils.showSoftInput(editComment);

        // if user is replying to another comment, highlight the comment being replied to and scroll
        // it to the top so the user can see which comment they're replying to - note that scrolling
        // is delayed to give time for listView to reposition due to soft keyboard appearing
        if (replyToCommentId != 0) {
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

    boolean isAddCommentBoxShowing() {
        return mIsAddCommentBoxShowing;
    }

    void hideAddCommentBox() {
        if (!isAdded() || !mIsAddCommentBoxShowing) {
            return;
        }

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
        if (position > -1) {
            getListView().setSelectionFromTop(position + getListView().getHeaderViewsCount(), 0);
        }
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private boolean mIsSubmittingComment = false;

    private void submitComment(final long replyToCommentId) {
        final EditText editComment = (EditText) getView().findViewById(R.id.edit_comment);
        final String commentText = EditTextUtils.getText(editComment);
        if (TextUtils.isEmpty(commentText)) {
            return;
        }

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
                if (!isAdded()) {
                    return;
                }
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
        if (newComment != null) {
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
        if (!isAdded()) {
            return;
        }

        final TextView txtFollow = (TextView) getView().findViewById(R.id.text_follow);
        final boolean isFollowed = ReaderPostTable.isPostFollowed(mPost);

        ReaderUtils.showFollowStatus(txtFollow, isFollowed);
    }

    /*
     * creates formatted div for passed video with passed (optional) thumbnail
     */
    private static final String OVERLAY_IMG = "file:///android_asset/ic_reader_video_overlay.png";

    private String makeVideoDiv(String videoUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(videoUrl)) {
            return "";
        }

        // sometimes we get src values like "//player.vimeo.com/video/70534716" - prefix these with http:
        if (videoUrl.startsWith("//")) {
            videoUrl = "http:" + videoUrl;
        }

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

    private boolean showPhotoViewer(String imageUrl, View source, int startX, int startY) {
        if (!isAdded() || TextUtils.isEmpty(imageUrl)) {
            return false;
        }

        // make sure this is a valid web image (could be file: or data:)
        if (!imageUrl.startsWith("http")) {
            return false;
        }

        // images in private posts must use https for auth token to be sent with request
        if (hasPost() && mPost.isPrivate) {
            imageUrl = UrlUtils.makeHttps(imageUrl);
        }

        ReaderActivityLauncher.showReaderPhotoViewer(getActivity(), imageUrl, source, startX, startY);
        return true;
    }

    private boolean hasStaticMenuDrawer() {
        return (getActivity() instanceof WPActionBarActivity)
                && (((WPActionBarActivity) getActivity()).isStaticMenuDrawer());
    }

    /*
     * size to use for images that fit the full width of the listView item
     */
    private int getFullSizeImageWidth(Context context) {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
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
            if (mPost.isVideoPress) {
                content = makeVideoDiv(mPost.getFeaturedVideo(), mPost.getFeaturedImage()) + content;
            }
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
        int fullSizeImageWidth = getFullSizeImageWidth(context);

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
              .append("  body, p, div { max-width: 100% !important; word-wrap: break-word; }")
              .append("  p, div { line-height: 1.6em; font-size: 1em; }")
              .append("  h1, h2 { line-height: 1.2em; }");

        // make sure long strings don't force the user to scroll horizontally
        sbHtml.append("  body, p, div, a { word-wrap: break-word; }");

        // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
        sbHtml.append(String.format("  p { margin-top: %dpx; margin-bottom: %dpx; }", marginSmall, marginSmall))
              .append("    p:first-child { margin-top: 0px; }");

        // add border, background color, and padding to pre blocks, and add overflow scrolling
        // so user can scroll the block if it's wider than the display
        sbHtml.append("  pre { overflow-x: scroll;")
              .append("        border: 1px solid ").append(greyLight).append("; ")
              .append("        background-color: ").append(greyExtraLight).append("; ")
              .append("        padding: ").append(marginSmall).append("px; }");

        // add a left border to blockquotes
        sbHtml.append("  blockquote { margin-left: ").append(marginSmall).append("px; ")
              .append("               padding-left: ").append(marginSmall).append("px; ")
              .append("               border-left: 3px solid ").append(greyLight).append("; }");

        // show links in the same color they are elsewhere in the app
        sbHtml.append("  a { text-decoration: none; color: ").append(linkColor).append("; }");

        // if javascript is allowed, make sure embedded videos fit the browser width and
        // use 16:9 ratio (YouTube standard) - if not allowed, hide iframes/embeds
        if (canEnableJavaScript()) {
            int videoWidth = DisplayUtils.pxToDp(context, fullSizeImageWidth - (marginLarge * 2));
            int videoHeight = (int) (videoWidth * 0.5625f);
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
                  .append("  }")
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
                if (isAdded()) {
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
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_post, ToastUtils.Duration.LONG);
        }
    }

    /*
     * javascript should only be enabled for wp blogs (not external feeds)
     */
    private boolean canEnableJavaScript() {
        return (mPost != null && mPost.isWP());
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
        TextView txtDateAndAuthor;
        TextView txtFollow;

        ImageView imgBtnReblog;
        ImageView imgBtnLike;
        ImageView imgBtnComment;

        WPNetworkImageView imgAvatar;
        WPNetworkImageView imgFeatured;

        ViewGroup layoutDetailHeader;

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

            mPost = ReaderPostTable.getPost(mBlogId, mPostId);
            if (mPost == null) {
                return false;
            }

            txtTitle = (TextView) container.findViewById(R.id.text_title);
            txtBlogName = (TextView) container.findViewById(R.id.text_blog_name);
            txtFollow = (TextView) container.findViewById(R.id.text_follow);
            txtDateAndAuthor = (TextView) container.findViewById(R.id.text_date_and_author);

            imgAvatar = (WPNetworkImageView) container.findViewById(R.id.image_avatar);
            imgFeatured = (WPNetworkImageView) container.findViewById(R.id.image_featured);

            imgBtnReblog = (ImageView) mLayoutIcons.findViewById(R.id.image_reblog_btn);
            imgBtnLike = (ImageView) getView().findViewById(R.id.image_like_btn);
            imgBtnComment = (ImageView) mLayoutIcons.findViewById(R.id.image_comment_btn);

            layoutDetailHeader = (ViewGroup) container.findViewById(R.id.layout_detail_header);

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
                    featuredImageUrl = mPost.getFeaturedImageForDisplay(getFullSizeImageWidth(container.getContext()), 0);
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mIsPostTaskRunning = false;

            if (!isAdded()) {
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

            txtTitle.setText(mPost.hasTitle() ? mPost.getTitle() : getString(R.string.reader_untitled_post));

            ReaderUtils.showFollowStatus(txtFollow, mPost.isFollowedByCurrentUser);
            txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePostFollowed(mPost, txtFollow);
                }
            });

            if (mPost.hasBlogName()) {
                txtBlogName.setText(mPost.getBlogName());
                txtBlogName.setVisibility(View.VISIBLE);
            } else if (mPost.hasBlogUrl()) {
                txtBlogName.setText(UrlUtils.getDomainFromUrl(mPost.getBlogUrl()));
                txtBlogName.setVisibility(View.VISIBLE);
            } else {
                txtBlogName.setVisibility(View.GONE);
            }

            // show date and author name if author name exists and is different than the blog name,
            // otherwise just show the date
            if (mPost.hasAuthorName() && !mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtDateAndAuthor.setText(DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished()) + " / " + mPost.getAuthorName());
            } else {
                txtDateAndAuthor.setText(DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished()));
            }

            if (mPost.hasPostAvatar()) {
                int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);
                imgAvatar.setVisibility(View.VISIBLE);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }

            // hide blog name, avatar & follow button if this fragment was shown from blog preview
            if (isBlogPreview()) {
                layoutDetailHeader.setVisibility(View.GONE);
            }

            if (showFeaturedImage) {
                imgFeatured.setVisibility(View.VISIBLE);
                imgFeatured.setImageUrl(featuredImageUrl, WPNetworkImageView.ImageType.PHOTO);
                imgFeatured.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int startX = (DisplayUtils.getDisplayPixelWidth(getActivity()) / 2);
                        int startY = (DisplayUtils.getDisplayPixelWidth(getActivity()) / 2);
                        showPhotoViewer(mPost.getFeaturedImage(), view, startX, startY);
                    }
                });
            } else {
                imgFeatured.setVisibility(View.GONE);
            }

            // enable reblogging wp posts
            if (mPost.canReblog()) {
                imgBtnReblog.setVisibility(View.VISIBLE);
                imgBtnReblog.setSelected(mPost.isRebloggedByCurrentUser);
                imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doPostReblog(imgBtnReblog, mPost);
                    }
                });
            } else {
                imgBtnReblog.setVisibility(View.GONE);
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

            if (mPost.isLikesEnabled) {
                imgBtnLike.setVisibility(View.VISIBLE);
                // if we know refreshLikes() is going to show the liking layout, force it to take up
                // space right now
                if (mPost.numLikes > 0 && mLayoutLikes.getVisibility() == View.GONE) {
                    mLayoutLikes.setVisibility(View.INVISIBLE);
                }
            } else {
                imgBtnLike.setVisibility(View.GONE);
            }

            // external blogs (feeds) don't support action icons
            if (!mPost.isExternal && (mPost.isLikesEnabled
                                   || mPost.canReblog()
                                   || mPost.isCommentsOpen)) {
                mLayoutIcons.setVisibility(View.VISIBLE);
            } else {
                mLayoutIcons.setVisibility(View.GONE);
            }

            // enable JavaScript in the webView if it's safe to do so
            mReaderWebView.getSettings().setJavaScriptEnabled(canEnableJavaScript());

            // IMPORTANT: use loadDataWithBaseURL() since loadData() may fail
            // https://code.google.com/p/android/issues/detail?id=4401
            mReaderWebView.loadDataWithBaseURL(null, postHtml, "text/html", "UTF-8", null);

            // only show action buttons for WP posts
            mLayoutIcons.setVisibility(mPost.isWP() ? View.VISIBLE : View.GONE);

            // make sure the adapter is assigned
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getCommentAdapter());
            }

            // listView is hidden in onCreateView()
            if (mNote == null && getListView().getVisibility() != View.VISIBLE) {
                getListView().setVisibility(View.VISIBLE);
            }

            // If we are loading from a notification, we need to set the post in the adapter
            if (mNote != null) {
                getCommentAdapter().setPost(mPost);
            }

            // webView is hidden in onCreateView() and will be made visible by readerWebViewClient
            // once it finishes loading, so if it's already visible go ahead and show likes/comments
            // right away, otherwise show them after a brief delay - this gives content time to
            // load before likes/comments appear
            if (mReaderWebView.getVisibility() == View.VISIBLE || mNote != null) {
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
                showContent();
            }
        }, 1000L);
    }

    private void showContent() {
        if (!isAdded()) {
            return;
        }

        // we don't want to show the post until we've loaded the comments if loading from a notification
        if (mNote != null) {
            mReaderWebView.setVisibility(View.VISIBLE);
        }

        // show likes & comments
        refreshLikes();
        refreshComments();

        // request the latest info for this post if we haven't updated it already
        if (!mHasAlreadyUpdatedPost) {
            updatePost();
            mHasAlreadyUpdatedPost = true;
        }
    }

    /*
     * return the container view that should host the fullscreen video
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
     * return the container view that should be hidden when fullscreen video is shown
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
        // fullscreen video has just been shown so hide the ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public void onCustomViewHidden() {
        // user returned from fullscreen video so re-display the ActionBar
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
        if (isAdded()) {
            return getActivity().getActionBar();
        } else {
            AppLog.w(T.READER, "reader post detail > getActionBar called with no activity");
            return null;
        }
    }

    void pauseWebView() {
        if (mReaderWebView != null) {
            mReaderWebView.hideCustomView();
            mReaderWebView.onPause();
        } else {
            AppLog.i(T.READER, "reader post detail > attempt to pause webView when null");
        }
    }

}
