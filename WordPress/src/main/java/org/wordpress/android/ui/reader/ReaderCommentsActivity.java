package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
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
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPListView;
import org.wordpress.android.widgets.WPNetworkImageView;

import javax.annotation.Nonnull;

public class ReaderCommentsActivity extends Activity {

    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;
    private ReaderCommentAdapter mCommentAdapter;

    private WPListView mListView;
    private ViewGroup mLayoutCommentBox;

    private boolean mIsUpdatingComments;
    private long mReplyToCommentId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reader_activity_comments);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
        } else {
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
        }

        loadPost();

        mListView = (WPListView) findViewById(android.R.id.list);

        // add listView header to provide initial space between the post header and list content
        int height = getResources().getDimensionPixelSize(R.dimen.margin_extra_small);
        ReaderUtils.addListViewHeader(mListView, height);

        mListView.setAdapter(getCommentAdapter());

        mLayoutCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        mLayoutCommentBox.setVisibility(View.VISIBLE);

        final ImageView imgPostComment = (ImageView) mLayoutCommentBox.findViewById(R.id.image_post_comment);
        imgPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment(mReplyToCommentId);
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_REPLY_TO_COMMENT_ID)) {
            showAddCommentBox(savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID));
        }

        refreshComments();
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

        String color = HtmlUtils.colorResToHtmlColor(this, R.color.grey_medium);
        String title = "<font color=" + color + ">" + getString(R.string.reader_label_comments_on) + "</font>"
                     + " " + mPost.getTitle();
        txtTitle.setText(Html.fromHtml(title));

        String url = mPost.getPostAvatarForDisplay(getResources().getDimensionPixelSize(R.dimen.avatar_sz_small));
        imgAvatar.setImageUrl(url, WPNetworkImageView.ImageType.AVATAR);
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

    private boolean isCommentAdapterEmpty() {
        return (mCommentAdapter == null || mCommentAdapter.isEmpty());
    }

    private ReaderCommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            ReaderInterfaces.DataLoadedListener dataLoadedListener = new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!isFinishing()) {
                        if (isEmpty) {
                            updateComments();
                        }
                    }
                }
            };

            // adapter calls this when user taps reply icon
            ReaderCommentAdapter.RequestReplyListener replyListener = new ReaderCommentAdapter.RequestReplyListener() {
                @Override
                public void onRequestReply(long commentId) {
                    showAddCommentBox(commentId);
                }
            };

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            ReaderActions.DataRequestedListener dataRequestedListener = new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData() {
                    if (!mIsUpdatingComments) {
                        AppLog.i(T.READER, "reader comments > requesting newer comments");
                        updateComments();
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

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);
        if (mReplyToCommentId != 0) {
            outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        }

        super.onSaveInstanceState(outState);
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
    private void updateComments() {
        if (mIsUpdatingComments) {
            AppLog.w(T.READER, "reader comments > already updating comments");
            return;
        }

        AppLog.d(T.READER, "reader comments > updateComments");
        mIsUpdatingComments = true;

        if (isCommentAdapterEmpty()) {
            showProgress();
        }

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                mIsUpdatingComments = false;
                if (!isFinishing()) {
                    hideProgress();
                    if (result == ReaderActions.UpdateResult.CHANGED) {
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
     * show the view enabling adding a comment - triggered when user hits comment icon/count in header
     * note that this view is hidden at design time, so it will be shown the first time user taps icon.
     * pass 0 for the replyToCommentId to add a parent-level comment to the post, or pass a real
     * comment id to reply to a specific comment
     */
    private void showAddCommentBox(final long replyToCommentId) {
        if (isFinishing() || mIsSubmittingComment) {
            return;
        }

        // different hint depending on whether user is replying to a comment or commenting on the post
        final EditText editComment = (EditText) mLayoutCommentBox.findViewById(R.id.edit_comment);
        editComment.setHint(replyToCommentId == 0 ? R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        if (mLayoutCommentBox.getVisibility() != View.VISIBLE) {
            AniUtils.flyIn(mLayoutCommentBox);
        }

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

        EditTextUtils.showSoftInput(editComment);

        // if user is replying to another comment, highlight the comment being replied to and scroll
        // it to the top so the user can see which comment they're replying to - note that scrolling
        // is delayed to give time for listView to reposition due to soft keyboard appearing
        if (replyToCommentId != 0) {
            getCommentAdapter().setHighlightCommentId(replyToCommentId, false);
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollToCommentId(replyToCommentId);
                }
            }, 300);
        }

        mReplyToCommentId = replyToCommentId;
    }

    void hideAddCommentBox() {
        if (isFinishing() || mLayoutCommentBox.getVisibility() != View.VISIBLE) {
            return;
        }

        final EditText editComment = (EditText) mLayoutCommentBox.findViewById(R.id.edit_comment);
        AniUtils.flyOut(mLayoutCommentBox);
        EditTextUtils.hideSoftInput(editComment);

        getCommentAdapter().setHighlightCommentId(0, false);

        mReplyToCommentId = 0;
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
    private boolean mIsSubmittingComment = false;

    private void submitComment(final long replyToCommentId) {
        final EditText editComment = (EditText) findViewById(R.id.edit_comment);
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
                if (isFinishing()) {
                    return;
                }
                if (succeeded) {
                    // comment posted successfully so stop highlighting the fake one and replace
                    // it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    mListView.invalidateViews();
                    showAddCommentBox(0);
                } else {
                    // comment failed to post - show the comment box again with the comment text intact,
                    // and remove the "fake" comment from the adapter
                    editComment.setText(commentText);
                    showAddCommentBox(replyToCommentId);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(
                            ReaderCommentsActivity.this, R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
            }
        };

        final ReaderComment newComment = ReaderCommentActions.submitPostComment(
                getPost(),
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

}
