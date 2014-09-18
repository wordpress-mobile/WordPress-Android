package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPListView;
import org.wordpress.android.widgets.WPNetworkImageView;

import javax.annotation.Nonnull;

public class ReaderCommentListActivity extends Activity {

    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";
    private static final String KEY_TOPMOST_COMMENT_ID = "topmost_comment_id";
    private static final String KEY_HAS_UPDATED_COMMENTS = "has_updated_comments";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;
    private ReaderCommentAdapter mCommentAdapter;

    private WPListView mListView;
    private EditText mEditComment;
    private ImageView mImgSubmitComment;
    private ViewGroup mCommentBox;

    private boolean mIsUpdatingComments;
    private boolean mHasUpdatedComments;
    private long mReplyToCommentId;

    private long mTopMostCommentId;
    private int mTopMostCommentTop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_comment_list);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mTopMostCommentId = savedInstanceState.getLong(KEY_TOPMOST_COMMENT_ID);
            mHasUpdatedComments = savedInstanceState.getBoolean(KEY_HAS_UPDATED_COMMENTS);
        } else {
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
        }

        mListView = (WPListView) findViewById(android.R.id.list);
        mCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        mEditComment = (EditText) mCommentBox.findViewById(R.id.edit_comment);
        mImgSubmitComment = (ImageView) mCommentBox.findViewById(R.id.image_post_comment);

        loadPost();

        // add listView header to provide initial space between the post header and list content
        int height = getResources().getDimensionPixelSize(R.dimen.margin_medium);
        ReaderUtils.addListViewHeader(mListView, height);

        mListView.setAdapter(getCommentAdapter());

        if (savedInstanceState != null) {
            setReplyToCommentId(savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID));
        }

        refreshComments();
    }

    private void setReplyToCommentId(long commentId) {
        mReplyToCommentId = commentId;
        mEditComment.setHint(mReplyToCommentId == 0 ?
                R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        // if a comment is being replied to, highlight it and scroll it to the top so the user can
        // see which comment they're replying to - note that scrolling is delayed to give time for
        // listView to reposition due to soft keyboard appearing
        if (mReplyToCommentId != 0) {
            mEditComment.requestFocus();
            EditTextUtils.showSoftInput(mEditComment);
            getCommentAdapter().setHighlightCommentId(mReplyToCommentId, false);
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollToCommentId(mReplyToCommentId);
                }
            }, 300);
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);
        outState.putLong(KEY_TOPMOST_COMMENT_ID, getTopMostCommentId());
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, mHasUpdatedComments);

        super.onSaveInstanceState(outState);
    }

    private void loadPost() {
        mPost = ReaderPostTable.getPost(mBlogId, mPostId);
        if (mPost == null) {
            ToastUtils.showToast(this, R.string.reader_toast_err_get_post);
            finish();
        }

        final View postHeader = findViewById(R.id.layout_post_header);
        final TextView txtTitle = (TextView) postHeader.findViewById(R.id.text_post_title);
        final WPNetworkImageView imgAvatar = (WPNetworkImageView) postHeader.findViewById(R.id.image_post_avatar);
        final TextView txtCommentsClosed = (TextView) findViewById(R.id.text_comments_closed);

        txtTitle.setText(mPost.getTitle());

        String url = mPost.getPostAvatarForDisplay(getResources().getDimensionPixelSize(R.dimen.avatar_sz_small));
        imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);

        if (mPost.isCommentsOpen) {
            mCommentBox.setVisibility(View.VISIBLE);
            txtCommentsClosed.setVisibility(View.GONE);

            mEditComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                        submitComment();
                    }
                    return false;
                }
            });

            mImgSubmitComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submitComment();
                }
            });
        } else {
            mCommentBox.setVisibility(View.GONE);
            mEditComment.setEnabled(false);
            txtCommentsClosed.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.reader_activity_scale_in, R.anim.reader_flyout);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean hasCommentAdapter() {
        return (mCommentAdapter != null);
    }

    private ReaderCommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            ReaderInterfaces.DataLoadedListener dataLoadedListener = new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!isFinishing()) {
                        if (isEmpty || !mHasUpdatedComments) {
                            updateComments(isEmpty);
                        } else if (mTopMostCommentId != 0) {
                            restoreTopmostComment();
                        }
                    }
                }
            };

            // adapter calls this when user taps reply icon
            ReaderCommentAdapter.RequestReplyListener replyListener = new ReaderCommentAdapter.RequestReplyListener() {
                @Override
                public void onRequestReply(long commentId) {
                    setReplyToCommentId(commentId);
                }
            };

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            ReaderActions.DataRequestedListener dataRequestedListener = new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData() {
                    if (!mIsUpdatingComments) {
                        AppLog.i(T.READER, "reader comments > requesting newer comments");
                        updateComments(true);
                    }
                }
            };
            mCommentAdapter = new ReaderCommentAdapter(this, getPost(), replyListener, dataLoadedListener, dataRequestedListener);
        }
        return mCommentAdapter;
    }

    private ReaderPost getPost() {
        return mPost;
    }

    private void showProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        progress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        progress.setVisibility(View.GONE);
    }

    /*
     * request comments for this post
     */
    private void updateComments(boolean showProgress) {
        if (mIsUpdatingComments) {
            AppLog.w(T.READER, "reader comments > already updating comments");
            return;
        }

        AppLog.d(T.READER, "reader comments > updateComments");
        mIsUpdatingComments = true;
        mHasUpdatedComments = true;

        if (showProgress) {
            showProgress();
        }

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                mIsUpdatingComments = false;
                if (!isFinishing()) {
                    hideProgress();
                    if (result == ReaderActions.UpdateResult.CHANGED) {
                        retainTopmostComment();
                        refreshComments();
                    } else {
                        mListView.setEmptyView(findViewById(R.id.text_empty));
                    }
                }
            }
        };
        ReaderCommentActions.updateCommentsForPost(getPost(), true, resultListener);
    }


    /*
     * refresh adapter so latest comments appear
     */
    private void refreshComments() {
        AppLog.d(T.READER, "reader comments > refreshComments");
        getCommentAdapter().refreshComments();
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private void scrollToCommentId(long commentId) {
        int position = getCommentAdapter().indexOfCommentId(commentId);
        if (position > -1) {
            mListView.setSelectionFromTop(position + mListView.getHeaderViewsCount(), 0);
        }
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private void submitComment() {
        final String commentText = EditTextUtils.getText(mEditComment);
        if (TextUtils.isEmpty(commentText)) {
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_COMMENTED_ON_ARTICLE);

        mImgSubmitComment.setEnabled(false);
        mEditComment.setEnabled(false);

        // generate a "fake" comment id to assign to the new comment so we can add it to the db
        // and reflect it in the adapter before the API call returns
        final long fakeCommentId = ReaderCommentActions.generateFakeCommentId();

        ReaderActions.CommentActionListener actionListener = new ReaderActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded, ReaderComment newComment) {
                if (isFinishing()) {
                    return;
                }
                mImgSubmitComment.setEnabled(true);
                mEditComment.setEnabled(true);
                if (succeeded) {
                    // stop highlighting the fake comment and replace it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    mListView.invalidateViews();
                    setReplyToCommentId(0);
                } else {
                    mEditComment.setText(commentText);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(
                            ReaderCommentListActivity.this, R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
            }
        };

        ReaderComment newComment = ReaderCommentActions.submitPostComment(
                getPost(),
                fakeCommentId,
                commentText,
                mReplyToCommentId,
                actionListener);

        if (newComment != null) {
            mEditComment.setText(null);
            // add the "fake" comment to the adapter, highlight it, and show a progress bar
            // next to it while it's submitted
            getCommentAdapter().setHighlightCommentId(newComment.commentId, true);
            getCommentAdapter().addComment(newComment);
            // make sure it's scrolled into view
            scrollToCommentId(fakeCommentId);
        }
    }

    /*
     * called before new comments are shown so the current topmost comment is remembered
     */
    private void retainTopmostComment() {
        mTopMostCommentId = getTopMostCommentId();
        View view = mListView.getChildAt(0);
        mTopMostCommentTop = (view != null ? view.getTop() : 0);
    }

    /*
     * called after new comments are shown so the previous topmost comment is scrolled to
     */
    private void restoreTopmostComment() {
        if (mTopMostCommentId != 0) {
            int position = getCommentAdapter().indexOfCommentId(mTopMostCommentId);
            if (position > -1) {
                mListView.setSelectionFromTop(position + mListView.getHeaderViewsCount(), mTopMostCommentTop);
            }
            mTopMostCommentId = 0;
            mTopMostCommentTop = 0;
        }
    }

    /*
     * returns the id of the first visible comment
     */
    private long getTopMostCommentId() {
        int position = mListView.getFirstVisiblePosition();
        int numHeaders = mListView.getHeaderViewsCount();
        if (position > numHeaders && hasCommentAdapter()) {
            return getCommentAdapter().getItemId(position - numHeaders);
        } else {
            return 0;
        }
    }

}
