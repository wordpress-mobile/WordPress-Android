package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.services.ReaderCommentService;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AccountHelper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

public class ReaderCommentListActivity extends ActionBarActivity {

    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";
    private static final String KEY_HAS_UPDATED_COMMENTS = "has_updated_comments";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;
    private ReaderCommentAdapter mCommentAdapter;
    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private ReaderRecyclerView mRecyclerView;
    private SuggestionAutoCompleteText mEditComment;
    private ImageView mImgSubmitComment;
    private ViewGroup mCommentBox;

    private boolean mIsUpdatingComments;
    private boolean mHasUpdatedComments;
    private boolean mIsSubmittingComment;
    private long mReplyToCommentId;
    private long mCommentId;
    private int mRestorePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_comment_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mHasUpdatedComments = savedInstanceState.getBoolean(KEY_HAS_UPDATED_COMMENTS);
        } else {
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mCommentId = getIntent().getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0);
            // remove all but the first page of comments for this post if there's an active
            // connection - infinite scroll will take care of filling in subsequent pages
            if (NetworkUtils.isNetworkAvailable(this)) {
                ReaderCommentTable.purgeExcessCommentsForPost(mBlogId, mPostId);
            }
        }

        mRecyclerView = (ReaderRecyclerView) findViewById(R.id.recycler_view);
        int spacingHorizontal = getResources().getDimensionPixelSize(R.dimen.reader_detail_margin);
        int spacingVertical = DisplayUtils.dpToPx(this, 1);
        mRecyclerView.addItemDecoration(new ReaderRecyclerView.ReaderItemDecoration(spacingHorizontal, spacingVertical));

        mCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        mEditComment = (SuggestionAutoCompleteText) mCommentBox.findViewById(R.id.edit_comment);
        mEditComment.getAutoSaveTextHelper().setUniqueId(String.format("%s%d%d", AccountHelper
                        .getCurrentUsernameForBlog(null), mPostId, mBlogId));
        mImgSubmitComment = (ImageView) mCommentBox.findViewById(R.id.image_post_comment);

        if (!loadPost()) {
            ToastUtils.showToast(this, R.string.reader_toast_err_get_post);
            finish();
            return;
        }

        mRecyclerView.setAdapter(getCommentAdapter());

        if (savedInstanceState != null) {
            setReplyToCommentId(savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID));
        }

        refreshComments();

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(this, (int) mBlogId);
        mSuggestionAdapter = SuggestionUtils.setupSuggestions((int) mBlogId, this, mSuggestionServiceConnectionManager,
                mPost.isWP());
        if (mSuggestionAdapter != null) {
            mEditComment.setAdapter(mSuggestionAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(SuggestionEvents.SuggestionListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mBlogId) {
            List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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
            mRecyclerView.postDelayed(new Runnable() {
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
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, mHasUpdatedComments);

        super.onSaveInstanceState(outState);
    }

    private boolean loadPost() {
        mPost = ReaderPostTable.getPost(mBlogId, mPostId, true);
        if (mPost == null) {
            return false;
        }

        final TextView txtTitle = (TextView) findViewById(R.id.text_post_title);
        final WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_post_avatar);
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

        if (mCommentId > 0) {
            txtTitle.setBackgroundResource(R.drawable.selectable_background_wordpress);
            txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFinishing()) return;

                    ReaderActivityLauncher.showReaderPostDetail(ReaderCommentListActivity.this, mBlogId, mPostId);
                }
            });
        }

        return true;
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }

    private boolean hasCommentAdapter() {
        return (mCommentAdapter != null);
    }

    private ReaderCommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            mCommentAdapter = new ReaderCommentAdapter(WPActivityUtils.getThemedContext(this), getPost());

            // adapter calls this when user taps reply icon
            mCommentAdapter.setReplyListener(new ReaderCommentAdapter.RequestReplyListener() {
                @Override
                public void onRequestReply(long commentId) {
                    setReplyToCommentId(commentId);
                }
            });

            // adapter calls this when data has been loaded & displayed
            mCommentAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!isFinishing()) {
                        if (isEmpty || !mHasUpdatedComments) {
                            updateComments(isEmpty, false);
                        } else if (mRestorePosition > 0) {
                            mRecyclerView.scrollToPosition(mRestorePosition);
                        } else if (mCommentId > 0) {
                            // Scroll to the commentId once if it was passed to this activity
                            scrollToCommentId(mCommentId);
                            mCommentId = 0;
                        }
                        mRestorePosition = 0;
                        checkEmptyView();
                    }
                }
            });

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            mCommentAdapter.setDataRequestedListener(new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData() {
                    if (!mIsUpdatingComments) {
                        AppLog.i(T.READER, "reader comments > requesting next page of comments");
                        updateComments(true, true);
                    }
                }
            });
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

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdateCommentsStarted event) {
        mIsUpdatingComments = true;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdateCommentsEnded event) {
        if (isFinishing()) return;

        mIsUpdatingComments = false;
        mHasUpdatedComments = true;
        hideProgress();

        if (event.getResult().isNewOrChanged()) {
            mRestorePosition = getCurrentPosition();
            refreshComments();
        } else {
            checkEmptyView();
        }
    }

    /*
     * request comments for this post
     */
    private void updateComments(boolean showProgress, boolean requestNextPage) {
        if (mIsUpdatingComments) {
            AppLog.w(T.READER, "reader comments > already updating comments");
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.w(T.READER, "reader comments > no connection, update canceled");
            return;
        }

        if (showProgress) {
            showProgress();
        }
        ReaderCommentService.startService(this, mPost, requestNextPage);
    }

    private void checkEmptyView() {
        TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
        boolean isEmpty = hasCommentAdapter()
                && getCommentAdapter().isEmpty()
                && !mIsSubmittingComment;
        if (isEmpty && !NetworkUtils.isNetworkAvailable(this)) {
            txtEmpty.setText(R.string.no_network_message);
            txtEmpty.setVisibility(View.VISIBLE);
        } else if (isEmpty && mHasUpdatedComments) {
            txtEmpty.setText(R.string.reader_empty_comments);
            txtEmpty.setVisibility(View.VISIBLE);
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
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
            mRecyclerView.scrollToPosition(position);
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
        mIsSubmittingComment = true;

        // generate a "fake" comment id to assign to the new comment so we can add it to the db
        // and reflect it in the adapter before the API call returns
        final long fakeCommentId = ReaderCommentActions.generateFakeCommentId();

        ReaderActions.CommentActionListener actionListener = new ReaderActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded, ReaderComment newComment) {
                if (isFinishing()) {
                    return;
                }
                mIsSubmittingComment = false;
                mImgSubmitComment.setEnabled(true);
                mEditComment.setEnabled(true);
                if (succeeded) {
                    // stop highlighting the fake comment and replace it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    setReplyToCommentId(0);
                    mEditComment.getAutoSaveTextHelper().clearSavedText(mEditComment);
                } else {
                    mEditComment.setText(commentText);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(
                            ReaderCommentListActivity.this, R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
                checkEmptyView();
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
            checkEmptyView();
        }
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasCommentAdapter()) {
            return ((LinearLayoutManager)mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return 0;
        }
    }

}
