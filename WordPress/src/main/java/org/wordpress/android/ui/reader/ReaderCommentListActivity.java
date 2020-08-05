package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.Builder;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.services.ReaderCommentService;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_REPLY;
import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_SELECTION_END;
import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_SELECTION_START;
import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class ReaderCommentListActivity extends LocaleAwareActivity {
    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";
    private static final String KEY_HAS_UPDATED_COMMENTS = "has_updated_comments";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;
    private ReaderCommentAdapter mCommentAdapter;
    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private ReaderRecyclerView mRecyclerView;
    private SuggestionAutoCompleteText mEditComment;
    private View mSubmitReplyBtn;
    private ViewGroup mCommentBox;

    private boolean mIsUpdatingComments;
    private boolean mHasUpdatedComments;
    private boolean mIsSubmittingComment;
    private boolean mUpdateOnResume;

    private DirectOperation mDirectOperation;
    private long mReplyToCommentId;
    private long mCommentId;
    private int mRestorePosition;
    private String mInterceptedUri;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private ReaderCommentListViewModel mViewModel;

    @Override
    public void onBackPressed() {
        CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

        if (fragment != null) {
            fragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.reader_activity_comment_list);
        mViewModel = mViewModelFactory.create(ReaderCommentListViewModel.class);

        mViewModel.getScrollTo().observe(this, scrollPositionEvent -> {
            ScrollPosition content = scrollPositionEvent.getContentIfNotHandled();
            LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (content != null && layoutManager != null) {
                if (content.isSmooth()) {
                    RecyclerView.SmoothScroller smoothScrollerToTop = new LinearSmoothScroller(this) {
                        @Override protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };
                    smoothScrollerToTop.setTargetPosition(content.getPosition());
                    layoutManager.startSmoothScroll(smoothScrollerToTop);
                } else {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(content.getPosition(), 0);
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mHasUpdatedComments = savedInstanceState.getBoolean(KEY_HAS_UPDATED_COMMENTS);
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI);
        } else {
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mDirectOperation = (DirectOperation) getIntent()
                    .getSerializableExtra(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = getIntent().getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0);
            mInterceptedUri = getIntent().getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI);
        }

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) findViewById(R.id.swipe_to_refresh),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        updatePostAndComments();
                    }
                }
        );

        mRecyclerView = (ReaderRecyclerView) findViewById(R.id.recycler_view);
        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(this, 1);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        mEditComment = (SuggestionAutoCompleteText) mCommentBox.findViewById(R.id.edit_comment);
        mEditComment.getAutoSaveTextHelper().setUniqueId(String.format(Locale.US, "%d%d", mPostId, mBlogId));

        mEditComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mSubmitReplyBtn.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });
        mSubmitReplyBtn = mCommentBox.findViewById(R.id.btn_submit_reply);
        mSubmitReplyBtn.setEnabled(false);
        mSubmitReplyBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                if (view.isHapticFeedbackEnabled()) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(view.getContext(), R.string.send, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(mSubmitReplyBtn);

        if (!loadPost()) {
            ToastUtils.showToast(this, R.string.reader_toast_err_get_post);
            finish();
            return;
        }

        mRecyclerView.setAdapter(getCommentAdapter());

        if (savedInstanceState != null) {
            setReplyToCommentId(savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID), false);
        }

        // update the post and its comments upon creation
        mUpdateOnResume = (savedInstanceState == null);

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(this, mBlogId);
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mBlogId, this, mSuggestionServiceConnectionManager,
                                                              mPost.isWP(), false);
        if (mSuggestionAdapter != null) {
            mEditComment.setAdapter(mSuggestionAdapter);
        }

        AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_COMMENTS_OPENED, mPost);

        mSite = mSiteStore.getSiteBySiteId(mBlogId);

        ImageView buttonExpand = findViewById(R.id.button_expand);
        buttonExpand.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = CommentFullScreenDialogFragment.Companion
                            .newBundle(mEditComment.getText().toString(),
                                    mEditComment.getSelectionStart(),
                                    mEditComment.getSelectionEnd(),
                                    mBlogId
                            );

                    new Builder(ReaderCommentListActivity.this)
                        .setTitle(R.string.comment)
                        .setOnCollapseListener(new OnCollapseListener() {
                            @Override
                            public void onCollapse(@Nullable Bundle result) {
                                if (result != null) {
                                    mEditComment.setText(result.getString(RESULT_REPLY));
                                    mEditComment.setSelection(result.getInt(RESULT_SELECTION_START),
                                            result.getInt(RESULT_SELECTION_END));
                                    mEditComment.requestFocus();
                                }
                            }
                        })
                        .setOnConfirmListener(new OnConfirmListener() {
                            @Override
                            public void onConfirm(@Nullable Bundle result) {
                                if (result != null) {
                                    mEditComment.setText(result.getString(RESULT_REPLY));
                                    submitComment();
                                }
                            }
                        })
                        .setContent(CommentFullScreenDialogFragment.class, bundle)
                        .setAction(R.string.send)
                        .setHideActivityBar(true)
                        .build()
                        .show(getSupportFragmentManager(), CollapseFullScreenDialogFragment.TAG);
                }
            }
        );

        buttonExpand.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                if (view.isHapticFeedbackEnabled()) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(view.getContext(), R.string.description_expand, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(buttonExpand);
    }

    private final View.OnClickListener mSignInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isFinishing()) {
                return;
            }

            AnalyticsUtils.trackWithInterceptedUri(AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED, mInterceptedUri);
            ActivityLauncher.loginWithoutMagicLink(ReaderCommentListActivity.this);
        }
    };

    // to do a complete refresh we need to get updated post and new comments
    private void updatePostAndComments() {
        ReaderPostActions.updatePost(mPost, new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!isFinishing() && result.isNewOrChanged()) {
                    // get the updated post and pass it to the adapter
                    ReaderPost post = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
                    if (post != null) {
                        getCommentAdapter().setPost(post);
                        mPost = post;
                    }
                }
            }
        });

        // load the first page of comments
        updateComments(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        refreshComments();

        if (mUpdateOnResume && NetworkUtils.isNetworkAvailable(this)) {
            updatePostAndComments();
            mUpdateOnResume = false;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mBlogId && mSuggestionAdapter != null) {
            List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void setReplyToCommentId(long commentId, boolean doFocus) {
        mReplyToCommentId = commentId;
        mEditComment.setHint(mReplyToCommentId == 0
                                     ? R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        if (doFocus) {
            mEditComment.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final boolean isFocusableInTouchMode = mEditComment.isFocusableInTouchMode();

                    mEditComment.setFocusableInTouchMode(true);
                    EditTextUtils.showSoftInput(mEditComment);

                    mEditComment.setFocusableInTouchMode(isFocusableInTouchMode);

                    setupReplyToComment();
                }
            }, 200);
        } else {
            setupReplyToComment();
        }
    }

    private void setupReplyToComment() {
        // if a comment is being replied to, highlight it and scroll it to the top so the user can
        // see which comment they're replying to - note that scrolling is delayed to give time for
        // listView to reposition due to soft keyboard appearing
        if (mReplyToCommentId != 0) {
            getCommentAdapter().setHighlightCommentId(mReplyToCommentId, false);
            getCommentAdapter().notifyDataSetChanged();
            scrollToCommentId(mReplyToCommentId);

            // reset to replying to the post when user hasn't entered any text and hits
            // the back button in the editText to hide the soft keyboard
            mEditComment.setOnBackListener(new SuggestionAutoCompleteText.OnEditTextBackListener() {
                @Override
                public void onEditTextBack() {
                    if (EditTextUtils.isEmpty(mEditComment)) {
                        setReplyToCommentId(0, false);
                    }
                }
            });
        } else {
            mEditComment.setOnBackListener(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, mHasUpdatedComments);
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri);

        super.onSaveInstanceState(outState);
    }

    private void showCommentsClosedMessage(boolean show) {
        TextView txtCommentsClosed = (TextView) findViewById(R.id.text_comments_closed);
        if (txtCommentsClosed != null) {
            txtCommentsClosed.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean loadPost() {
        mPost = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
        if (mPost == null) {
            return false;
        }

        TextView txtCommentsClosed = (TextView) findViewById(R.id.text_comments_closed);
        if (!mAccountStore.hasAccessToken()) {
            mCommentBox.setVisibility(View.GONE);
            showCommentsClosedMessage(false);
        } else if (mPost.isCommentsOpen) {
            mCommentBox.setVisibility(View.VISIBLE);
            showCommentsClosedMessage(false);

            mEditComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                        submitComment();
                    }
                    return false;
                }
            });

            mSubmitReplyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submitComment();
                }
            });
        } else {
            mCommentBox.setVisibility(View.GONE);
            mEditComment.setEnabled(false);
            showCommentsClosedMessage(true);
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
                    setReplyToCommentId(commentId, true);
                }
            });

            // Enable post title click if we came here directly from notifications or deep linking
            if (mDirectOperation != null) {
                mCommentAdapter.enableHeaderClicks();
            }

            // adapter calls this when data has been loaded & displayed
            mCommentAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!isFinishing()) {
                        if (isEmpty || !mHasUpdatedComments) {
                            updateComments(isEmpty, false);
                        } else if (mCommentId > 0 || mDirectOperation != null) {
                            if (mCommentId > 0) {
                                // Scroll to the commentId once if it was passed to this activity
                                smoothScrollToCommentId(mCommentId);
                            }

                            doDirectOperation();
                        } else if (mRestorePosition > 0) {
                            mViewModel.scrollToPosition(mRestorePosition, false);
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

    private void doDirectOperation() {
        if (mDirectOperation != null) {
            switch (mDirectOperation) {
                case COMMENT_JUMP:
                    mCommentAdapter.setHighlightCommentId(mCommentId, false);

                    // clear up the direct operation vars. Only performing it once.
                    mDirectOperation = null;
                    mCommentId = 0;
                    break;
                case COMMENT_REPLY:
                    setReplyToCommentId(mCommentId, mAccountStore.hasAccessToken());

                    // clear up the direct operation vars. Only performing it once.
                    mDirectOperation = null;
                    mCommentId = 0;
                    break;
                case COMMENT_LIKE:
                    getCommentAdapter().setHighlightCommentId(mCommentId, false);
                    if (!mAccountStore.hasAccessToken()) {
                        WPSnackbar.make(mRecyclerView,
                                      R.string.reader_snackbar_err_cannot_like_post_logged_out,
                                      Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.sign_in, mSignInClickListener)
                                .show();
                    } else {
                        ReaderComment comment = ReaderCommentTable.getComment(mPost.blogId, mPost.postId, mCommentId);
                        if (comment == null) {
                            ToastUtils.showToast(ReaderCommentListActivity.this,
                                                 R.string.reader_toast_err_comment_not_found);
                        } else if (comment.isLikedByCurrentUser) {
                            ToastUtils.showToast(ReaderCommentListActivity.this,
                                                 R.string.reader_toast_err_already_liked);
                        } else {
                            long wpComUserId = mAccountStore.getAccount().getUserId();
                            if (ReaderCommentActions.performLikeAction(comment, true, wpComUserId)
                                && getCommentAdapter().refreshComment(mCommentId)) {
                                getCommentAdapter().setAnimateLikeCommentId(mCommentId);

                                AnalyticsUtils.trackWithReaderPostDetails(
                                        AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_LIKED, mPost);
                            } else {
                                ToastUtils.showToast(ReaderCommentListActivity.this,
                                                     R.string.reader_toast_err_generic);
                            }
                        }

                        // clear up the direct operation vars. Only performing it once.
                        mDirectOperation = null;
                    }
                    break;
                case POST_LIKE:
                    // nothing special to do in this case
                    break;
            }
        } else {
            mCommentId = 0;
        }
    }

    private ReaderPost getPost() {
        return mPost;
    }

    private void showProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        if (progress != null) {
            progress.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdateCommentsStarted event) {
        mIsUpdatingComments = true;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdateCommentsEnded event) {
        if (isFinishing()) {
            return;
        }

        mIsUpdatingComments = false;
        mHasUpdatedComments = true;
        hideProgress();

        if (event.getResult().isNewOrChanged()) {
            mRestorePosition = getCurrentPosition();
            refreshComments();
        } else {
            checkEmptyView();
        }

        setRefreshing(false);
    }

    /*
     * request comments for this post
     */
    private void updateComments(boolean showProgress, boolean requestNextPage) {
        if (mIsUpdatingComments) {
            AppLog.w(T.READER, "reader comments > already updating comments");
            setRefreshing(false);
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.w(T.READER, "reader comments > no connection, update canceled");
            setRefreshing(false);
            return;
        }

        if (showProgress) {
            showProgress();
        }
        ReaderCommentService.startService(this, mPost.blogId, mPost.postId, requestNextPage);
    }

    private void checkEmptyView() {
        TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
        if (txtEmpty == null) {
            return;
        }

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
        int position = getCommentAdapter().positionOfCommentId(commentId);
        if (position > -1) {
            mViewModel.scrollToPosition(position, false);
        }
    }

    /*
     * Smoothly scrolls the passed comment to the top of the listView
     */
    private void smoothScrollToCommentId(long commentId) {
        int position = getCommentAdapter().positionOfCommentId(commentId);
        if (position > -1) {
            mViewModel.scrollToPosition(position, true);
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

        AnalyticsUtils.trackWithReaderPostDetails(
                AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON, mPost);

        mSubmitReplyBtn.setEnabled(false);
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
                mEditComment.setEnabled(true);
                if (succeeded) {
                    mSubmitReplyBtn.setEnabled(false);
                    // stop highlighting the fake comment and replace it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    getCommentAdapter().refreshPost();
                    setReplyToCommentId(0, false);
                    mEditComment.getAutoSaveTextHelper().clearSavedText(mEditComment);
                } else {
                    mEditComment.setText(commentText);
                    mSubmitReplyBtn.setEnabled(true);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(
                            ReaderCommentListActivity.this, R.string.reader_toast_err_comment_failed,
                            ToastUtils.Duration.LONG);
                }
                checkEmptyView();
            }
        };

        long wpComUserId = mAccountStore.getAccount().getUserId();
        ReaderComment newComment = ReaderCommentActions.submitPostComment(
                getPost(),
                fakeCommentId,
                commentText,
                mReplyToCommentId,
                actionListener,
                wpComUserId);

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
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return 0;
        }
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if user is returning from login, make sure to update the post and its comments
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == Activity.RESULT_OK) {
            mUpdateOnResume = true;
        }
    }
}
