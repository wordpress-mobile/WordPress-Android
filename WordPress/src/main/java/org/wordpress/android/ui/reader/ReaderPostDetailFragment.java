package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderCustomViewListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewPageFinishedListener;
import org.wordpress.android.ui.reader.views.ReaderWebView.ReaderWebViewUrlClickListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.ScrollDirectionListener;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPScrollView;

public class ReaderPostDetailFragment extends Fragment
        implements ScrollDirectionListener,
        ReaderCustomViewListener,
        ReaderWebViewPageFinishedListener,
        ReaderWebViewUrlClickListener {

    private static final String ARG_DISABLE_BLOCK_BLOG = "disable_block_blog";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;
    private ReaderPostRenderer mRenderer;
    private ReaderPostListType mPostListType;

    private WPScrollView mScrollView;
    private ViewGroup mLayoutIcons;
    private ViewGroup mLayoutLikes;
    private ReaderWebView mReaderWebView;
    private ReaderLikingUsersView mLikingUsersView;

    private boolean mHasAlreadyUpdatedPost;
    private boolean mHasAlreadyRequestedPost;
    private boolean mIsBlockBlogDisabled;

    private ReaderInterfaces.OnPostPopupListener mOnPopupListener;
    private ReaderInterfaces.AutoHideToolbarListener mAutoHideToolbarListener;

    private ReaderResourceVars mResourceVars;

    public static ReaderPostDetailFragment newInstance(long blogId, long postId) {
        return newInstance(blogId, postId, true, null);
    }

    public static ReaderPostDetailFragment newInstance(long blogId,
                                                       long postId,
                                                       boolean disableBlockBlog,
                                                       ReaderPostListType postListType) {
        AppLog.d(T.READER, "reader post detail > newInstance");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putLong(ReaderConstants.ARG_POST_ID, postId);
        args.putBoolean(ARG_DISABLE_BLOCK_BLOG, disableBlockBlog);
        if (postListType != null) {
            args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }

        ReaderPostDetailFragment fragment = new ReaderPostDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = args.getLong(ReaderConstants.ARG_POST_ID);
            mIsBlockBlogDisabled = args.getBoolean(ARG_DISABLE_BLOCK_BLOG);
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceVars = new ReaderResourceVars(activity);
        if (activity instanceof ReaderInterfaces.OnPostPopupListener) {
            mOnPopupListener = (ReaderInterfaces.OnPostPopupListener) activity;
        }
        if (activity instanceof ReaderInterfaces.AutoHideToolbarListener) {
            mAutoHideToolbarListener = (ReaderInterfaces.AutoHideToolbarListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_post_detail, container, false);

        mScrollView = (WPScrollView) view.findViewById(R.id.scroll_view_reader);
        mScrollView.setScrollDirectionListener(this);

        mLayoutIcons = (ViewGroup) view.findViewById(R.id.layout_actions);
        mLayoutLikes = (ViewGroup) view.findViewById(R.id.layout_likes);
        mLikingUsersView = (ReaderLikingUsersView) mLayoutLikes.findViewById(R.id.layout_liking_users_view);

        // setup the ReaderWebView
        mReaderWebView = (ReaderWebView) view.findViewById(R.id.reader_webview);
        mReaderWebView.setCustomViewListener(this);
        mReaderWebView.setUrlClickListener(this);
        mReaderWebView.setPageFinishedListener(this);

        // hide scrollView and icons until the post is loaded
        mScrollView.setVisibility(View.INVISIBLE);
        mLayoutIcons.setVisibility(View.INVISIBLE);

        // spacer that's set to the same height as the toolbar needs to be visible if fragment is
        // in an activity that supports toolbar auto-hiding (e.g. ReaderPostPagerActivity)
        View spacer = view.findViewById(R.id.spacer_autohide_toolbar);
        spacer.setVisibility(mAutoHideToolbarListener != null ? View.VISIBLE : View.GONE);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReaderWebView != null) {
            mReaderWebView.destroy();
        }
    }


    @Override
    public void onScrollUp() {
        showIconBar(true);
        showToolbar(true);
    }

    @Override
    public void onScrollDown() {
        if (mScrollView.canScrollDown() && mScrollView.canScrollUp()) {
            showIconBar(false);
            showToolbar(false);
        }
    }

    @Override
    public void onScrollCompleted() {
        if (!mScrollView.canScrollDown()) {
            showIconBar(true);
        }
    }

    private void showToolbar(boolean show) {
        if (mAutoHideToolbarListener != null) {
            mAutoHideToolbarListener.onShowHideToolbar(show);
        }
    }

    private boolean hasPost() {
        return (mPost != null);
    }

    private boolean canBlockBlog() {
        return mPost != null
                && !mIsBlockBlogDisabled
                && !mPost.isPrivate
                && !mPost.isExternal
                && (mOnPopupListener != null)
                && (getPostListType() == ReaderPostListType.TAG_FOLLOWED);
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

    private boolean isBlogPreview() {
        return (getPostListType() == ReaderPostListType.BLOG_PREVIEW);
    }

    /*
     * animate in/out the layout containing the reblog/comment/like icons
     */
    private void showIconBar(boolean show) {
        if (isAdded()) {
            ReaderAnim.animateBottomBar(mLayoutIcons, show);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);

        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_REQUESTED, mHasAlreadyRequestedPost);
        outState.putBoolean(ARG_DISABLE_BLOCK_BLOG, mIsBlockBlogDisabled);
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

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
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mHasAlreadyRequestedPost = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_REQUESTED);
            mIsBlockBlogDisabled = savedInstanceState.getBoolean(ARG_DISABLE_BLOCK_BLOG);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
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
    private void togglePostLike() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        boolean isAskingToLike = !mPost.isLikedByCurrentUser;
        ReaderIconCountView likeCount = (ReaderIconCountView) getView().findViewById(R.id.count_likes);
        likeCount.setSelected(isAskingToLike);
        ReaderAnim.animateLikeButton(likeCount.getImageView(), isAskingToLike);

        if (!ReaderPostActions.performLikeAction(mPost, isAskingToLike)) {
            likeCount.setSelected(!isAskingToLike);
            return;
        }

        // get the post again since it has changed, then refresh to show changes
        mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
        refreshLikes();
        refreshIconBarCounts(true);

        if (isAskingToLike) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LIKED_ARTICLE);
        }
    }

    /*
     * change the follow state of the blog the current post is in
     */
    private void togglePostFollowed() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        final boolean isAskingToFollow = !mPost.isFollowedByCurrentUser;
        final ReaderFollowButton followButton = (ReaderFollowButton) getView().findViewById(R.id.follow_button);
        followButton.setIsFollowedAnimated(isAskingToFollow);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
                    followButton.setIsFollowed(!isAskingToFollow);
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(getActivity(), resId);
                }
            }
        };


        // action returns before api call completes, but local post will have been changed
        if (ReaderBlogActions.followBlogForPost(mPost, isAskingToFollow, actionListener)) {
            mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
        }
    }

    /*
     * called when user chooses to reblog the post
     */
    private void reblogPost() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        if (mPost.isRebloggedByCurrentUser) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_already_reblogged);
            return;
        }

        final ImageView imgBtnReblog = (ImageView) mLayoutIcons.findViewById(R.id.image_reblog_btn);
        ReaderAnim.animateReblogButton(imgBtnReblog);
        ReaderActivityLauncher.showReaderReblogForResult(getActivity(), mPost, imgBtnReblog);
    }

    /*
     * called after the post has been reblogged
     */
    void doPostReblogged() {
        if (!isAdded()) {
            return;
        }

        // get the post again since reblog status has changed
        mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);

        final ImageView imgBtnReblog = (ImageView) mLayoutIcons.findViewById(R.id.image_reblog_btn);
        imgBtnReblog.setSelected(mPost != null && mPost.isRebloggedByCurrentUser);
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
     * get the latest version of this post
     */
    private void updatePost() {
        if (!hasPost() || !mPost.isWP()) {
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
                    refreshIconBarCounts(true);
                    refreshComments();
                }
                // refresh likes if necessary - done regardless of whether the post has changed
                // since it's possible likes weren't stored until the post was updated
                if (result != ReaderActions.UpdateResult.FAILED
                        && numLikesBefore != ReaderLikeTable.getNumLikesForPost(mPost)) {
                    refreshLikes();
                }
            }
        };
        ReaderPostActions.updatePost(mPost, resultListener);
    }

    private void refreshIconBarCounts(boolean animateChanges) {
        if (!isAdded() || !hasPost()) {
            return;
        }

        final ReaderIconCountView countLikes = (ReaderIconCountView) getView().findViewById(R.id.count_likes);
        final ReaderIconCountView countComments = (ReaderIconCountView) getView().findViewById(R.id.count_comments);

        if (mPost.isWP() && (mPost.isCommentsOpen || mPost.numReplies > 0)) {
            countComments.setCount(mPost.numReplies, animateChanges);
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

        if (mPost.isLikesEnabled) {
            countLikes.setCount(mPost.numLikes, animateChanges);
            countLikes.setVisibility(View.VISIBLE);
            countLikes.setSelected(mPost.isLikedByCurrentUser);
            countLikes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePostLike();
                }
            });
            // if we know refreshLikes() is going to show the liking layout, force it to take up
            // space right now
            if (mPost.numLikes > 0 && mLayoutLikes.getVisibility() == View.GONE) {
                mLayoutLikes.setVisibility(View.INVISIBLE);
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
        if (!isAdded() || !hasPost() || !mPost.isWP() || !mPost.isLikesEnabled) {
            return;
        }

        final TextView txtLikeCount = (TextView) mLayoutLikes.findViewById(R.id.text_like_count);
        txtLikeCount.setText(ReaderUtils.getLongLikeLabelText(getActivity(), mPost.numLikes, mPost.isLikedByCurrentUser));

        // nothing more to do if no likes
        if (mPost.numLikes == 0) {
            if (mLayoutLikes.getVisibility() != View.GONE) {
                ReaderAnim.fadeOut(mLayoutLikes, ReaderAnim.Duration.SHORT);
            }
            return;
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

        mLikingUsersView.showLikingUsers(mPost);
    }

    /*
     * update the comment count shown below the post's content
     */
    private void refreshComments() {
        if (!isAdded() || !hasPost()) {
            return;
        }

        final Button btnCommentCount = (Button) getView().findViewById(R.id.button_comment_count);
        if (mPost.numReplies == 0) {
            btnCommentCount.setVisibility(View.GONE);
        } else {
            btnCommentCount.setVisibility(View.VISIBLE);
            if (mPost.numReplies == 1) {
                btnCommentCount.setText(getString(R.string.reader_label_comment_count_single));
            } else {
                btnCommentCount.setText(getString(R.string.reader_label_comment_count_multi, mPost.numReplies));
            }
            btnCommentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(getActivity(), mPost.blogId, mPost.postId);
                }
            });
        }
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

        ReaderActivityLauncher.showReaderPhotoViewer(
                getActivity(),
                imageUrl,
                postContent,
                sourceView,
                isPrivatePost,
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
        TextView txtTitle;
        TextView txtBlogName;
        TextView txtDateAndAuthor;

        ImageView imgBtnReblog;
        ImageView imgMore;

        ReaderFollowButton followButton;
        WPNetworkImageView imgAvatar;
        ViewGroup layoutDetailHeader;

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

            mPost = ReaderPostTable.getPost(mBlogId, mPostId, false);
            if (mPost == null) {
                return false;
            }

            mReaderWebView.setIsPrivatePost(mPost.isPrivate);

            txtTitle = (TextView) container.findViewById(R.id.text_title);
            txtBlogName = (TextView) container.findViewById(R.id.text_blog_name);
            txtDateAndAuthor = (TextView) container.findViewById(R.id.text_date_and_author);

            imgAvatar = (WPNetworkImageView) container.findViewById(R.id.image_avatar);
            imgMore = (ImageView) container.findViewById(R.id.image_more);
            imgBtnReblog = (ImageView) mLayoutIcons.findViewById(R.id.image_reblog_btn);

            layoutDetailHeader = (ViewGroup) container.findViewById(R.id.layout_detail_header);
            followButton = (ReaderFollowButton) container.findViewById(R.id.follow_button);

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
                    AppLog.i(T.READER, "reader post detail > post not found, requesting it");
                    requestPost();
                }
                return;
            }

            // scrollView was hidden in onCreateView, show it now that we have the post
            mScrollView.setVisibility(View.VISIBLE);

            // render the post in the webView
            mRenderer = new ReaderPostRenderer(mReaderWebView, mPost);
            mRenderer.beginRender();

            txtTitle.setText(mPost.hasTitle() ? mPost.getTitle() : getString(R.string.reader_untitled_post));

            followButton.setIsFollowed(mPost.isFollowedByCurrentUser);
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePostFollowed();
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
                txtDateAndAuthor.setText(DateTimeUtils.javaDateToTimeSpan(
                        mPost.getDatePublished()) + " / " + mPost.getAuthorName());
            } else {
                txtDateAndAuthor.setText(DateTimeUtils.javaDateToTimeSpan(mPost.getDatePublished()));
            }

            if (mPost.hasPostAvatar()) {
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(
                        mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.AVATAR);
                imgAvatar.setVisibility(View.VISIBLE);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }

            // hide header if this fragment was shown from blog preview, otherwise tapping
            // avatar in header shows blog preview unless this post is from an external feed
            if (isBlogPreview()) {
                layoutDetailHeader.setVisibility(View.GONE);
            } else {
                imgAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), mPost);
                    }
                });
            }

            // enable reblogging wp posts
            if (mPost.canReblog()) {
                imgBtnReblog.setVisibility(View.VISIBLE);
                imgBtnReblog.setSelected(mPost.isRebloggedByCurrentUser);
                imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reblogPost();
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imgBtnReblog.setBackgroundResource(R.drawable.ripple_oval);
                }
            } else {
                imgBtnReblog.setVisibility(View.INVISIBLE);
            }

            // external blogs (feeds) don't support action icons
            if (!mPost.isExternal && (mPost.isLikesEnabled
                    || mPost.canReblog()
                    || mPost.isCommentsOpen)) {
                mLayoutIcons.setVisibility(View.VISIBLE);
            } else {
                mLayoutIcons.setVisibility(View.GONE);
            }

            // enable blocking the associated blog
            if (canBlockBlog()) {
                imgMore.setVisibility(View.VISIBLE);
                imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mOnPopupListener != null) {
                            mOnPopupListener.onShowPostPopup(view, mPost);
                        }
                    }
                });
            } else {
                imgMore.setVisibility(View.GONE);
            }

            // only show action buttons for WP posts
            mLayoutIcons.setVisibility(mPost.isWP() ? View.VISIBLE : View.GONE);
            refreshIconBarCounts(false);
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
                    refreshComments();
                    if (!mHasAlreadyUpdatedPost) {
                        mHasAlreadyUpdatedPost = true;
                        updatePost();
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
