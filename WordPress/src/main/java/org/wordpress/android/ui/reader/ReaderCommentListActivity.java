package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.ReaderActivityCommentListBinding;
import org.wordpress.android.databinding.ReaderIncludeCommentBoxBinding;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.UserSuggestionTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.UserSuggestion;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.Builder;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity;
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderCommentMenuActionAdapter.ReaderCommentMenuActionType;
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource;
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel;
import org.wordpress.android.ui.suggestion.Suggestion;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource;
import org.wordpress.android.util.extensions.CompatExtensionsKt;
import org.wordpress.android.util.extensions.ViewExtensionsKt;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_REPLY;
import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_SELECTION_END;
import static org.wordpress.android.ui.CommentFullScreenDialogFragment.RESULT_SELECTION_START;
import static org.wordpress.android.ui.reader.FollowConversationUiStateKt.FOLLOW_CONVERSATION_UI_STATE_FLAGS_KEY;
import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

@AndroidEntryPoint
public class ReaderCommentListActivity extends LocaleAwareActivity implements OnConfirmListener,
        OnCollapseListener {
    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";
    private static final String KEY_HAS_UPDATED_COMMENTS = "has_updated_comments";

    private static final String NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG";

    private long mPostId;
    private long mBlogId;
    @Nullable private ReaderPost mPost;
    @Nullable private ReaderCommentAdapter mCommentAdapter;
    @Nullable private SuggestionAdapter mSuggestionAdapter;
    @Nullable private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    @Nullable private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private boolean mIsUpdatingComments;
    private boolean mHasUpdatedComments;
    private boolean mIsSubmittingComment;
    private boolean mUpdateOnResume;

    @Nullable private DirectOperation mDirectOperation;
    private long mReplyToCommentId;
    private long mCommentId;
    private int mRestorePosition;
    @Nullable private String mInterceptedUri;
    @Nullable private String mSource;

    @Inject AccountStore mAccountStore;
    @Inject UiHelpers mUiHelpers;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject ReaderTracker mReaderTracker;
    @Inject SiteStore mSiteStore;

    @Nullable private ReaderCommentListViewModel mViewModel;
    @Nullable private ConversationNotificationsViewModel mConversationViewModel;

    @Nullable private ReaderActivityCommentListBinding mBinding = null;
    @Nullable private ReaderIncludeCommentBoxBinding mBoxBinding = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ReaderActivityCommentListBinding.inflate(getLayoutInflater());
        mBoxBinding = mBinding.layoutCommentBox;
        setContentView(mBinding.getRoot());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                        getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

                if (fragment != null) {
                    fragment.collapse();
                } else {
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        if (mBinding != null) {
            setSupportActionBar(mBinding.toolbarMain);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        if (mBinding != null) {
            initViewModels(mBinding, savedInstanceState);
        }

        if (mBinding != null && mBoxBinding != null && mConversationViewModel != null) {
            mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                    mBinding.swipeToRefresh,
                    () -> {
                        mConversationViewModel.onRefresh();
                        updatePostAndComments(mBinding, mBoxBinding);
                    }
            );
        }

        if (mBoxBinding != null) {
            mBoxBinding.editComment.initializeWithPrefix('@');
            mBoxBinding.editComment.getAutoSaveTextHelper().setUniqueId(
                    String.format(Locale.US, "%d%d", mPostId, mBlogId)
            );

            mBoxBinding.editComment.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(@NonNull CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(@NonNull CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(@NonNull Editable s) {
                    mBoxBinding.btnSubmitReply.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
                }
            });
            mBoxBinding.btnSubmitReply.setEnabled(false);
            mBoxBinding.btnSubmitReply.setOnLongClickListener(view -> {
                if (view.isHapticFeedbackEnabled()) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(view.getContext(), R.string.send, Toast.LENGTH_SHORT).show();
                return true;
            });
            ViewExtensionsKt.redirectContextClickToLongPressListener(mBoxBinding.btnSubmitReply);
        }

        if (mBinding != null && mBoxBinding != null
            && !loadPost(mBinding, mBoxBinding)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_get_post);
            finish();
            return;
        }

        if (mBinding != null && mBoxBinding != null) {
            int spacingHorizontal = 0;
            int spacingVertical = DisplayUtils.dpToPx(this, 1);
            mBinding.recyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));
            mBinding.recyclerView.setAdapter(getCommentAdapter(mBinding, mBoxBinding));
        }

        if (mBinding != null && mBoxBinding != null
            && savedInstanceState != null) {
            setReplyToCommentId(
                    mBinding,
                    mBoxBinding,
                    savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID),
                    false
            );
        }

        // update the post and its comments upon creation
        mUpdateOnResume = (savedInstanceState == null);

        if (mSource != null) {
            mReaderTracker.trackPost(AnalyticsTracker.Stat.READER_ARTICLE_COMMENTS_OPENED, mPost, mSource);
        }

        if (mBoxBinding != null && mPost != null) {
            mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(this, mBlogId);
            mSuggestionAdapter = SuggestionUtils.setupUserSuggestions(
                    mBlogId,
                    this,
                    mSuggestionServiceConnectionManager,
                    mPost.isWP()
            );
            mBoxBinding.editComment.setAdapter(mSuggestionAdapter);

            mBoxBinding.buttonExpand.setOnClickListener(
                    v -> {
                        Bundle bundle = CommentFullScreenDialogFragment.Companion
                                .newBundle(
                                        mBoxBinding.editComment.getText().toString(),
                                        mBoxBinding.editComment.getSelectionStart(),
                                        mBoxBinding.editComment.getSelectionEnd(),
                                        mBlogId
                                );

                        new Builder(ReaderCommentListActivity.this)
                                .setTitle(R.string.comment)
                                .setOnCollapseListener(this)
                                .setOnConfirmListener(this)
                                .setContent(CommentFullScreenDialogFragment.class, bundle)
                                .setAction(R.string.send)
                                .setHideActivityBar(true)
                                .build()
                                .show(getSupportFragmentManager(), CollapseFullScreenDialogFragment.TAG);
                    }
            );

            mBoxBinding.buttonExpand.setOnLongClickListener(view -> {
                if (view.isHapticFeedbackEnabled()) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(view.getContext(), R.string.description_expand, Toast.LENGTH_SHORT).show();
                return true;
            });
            ViewExtensionsKt.redirectContextClickToLongPressListener(mBoxBinding.buttonExpand);
        }

        // reattach listeners to collapsible reply dialog
        CollapseFullScreenDialogFragment fragment =
                (CollapseFullScreenDialogFragment) getSupportFragmentManager().findFragmentByTag(
                        CollapseFullScreenDialogFragment.TAG);

        if (fragment != null && fragment.isAdded()) {
            fragment.setOnCollapseListener(this);
            fragment.setOnConfirmListener(this);
        }
    }

    private void initViewModels(
            @NonNull ReaderActivityCommentListBinding binding,
            @Nullable Bundle savedInstanceState
    ) {
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(ReaderCommentListViewModel.class);
        mViewModel.getScrollTo().observe(this, scrollPositionEvent -> {
            ScrollPosition content = scrollPositionEvent.getContentIfNotHandled();
            LayoutManager layoutManager = binding.recyclerView.getLayoutManager();
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
                binding.appbarMain.post(binding.appbarMain::requestLayout);
            }
        });

        mConversationViewModel = new ViewModelProvider(this, mViewModelFactory).get(
                ConversationNotificationsViewModel.class
        );
        mConversationViewModel.getSnackbarEvents().observe(this, snackbarMessageHolderEvent -> {
            FragmentManager fm = getSupportFragmentManager();
            CommentNotificationsBottomSheetFragment bottomSheet =
                    (CommentNotificationsBottomSheetFragment) fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG);

            if (bottomSheet != null) return;

            snackbarMessageHolderEvent.applyIfNotHandled(holder -> {
                WPSnackbar.make(binding.coordinatorLayout,
                                  mUiHelpers.getTextOfUiString(ReaderCommentListActivity.this, holder.getMessage()),
                                  Snackbar.LENGTH_LONG)
                          .setAction(
                                  holder.getButtonTitle() != null
                                          ? mUiHelpers.getTextOfUiString(
                                          ReaderCommentListActivity.this,
                                          holder.getButtonTitle())
                                          : null,
                                  v -> holder.getButtonAction().invoke())
                          .show();
                return Unit.INSTANCE;
            });
        });

        mConversationViewModel.getShowBottomSheetEvent().observe(this, event ->
                event.applyIfNotHandled(isShowingData -> {
                    FragmentManager fm = getSupportFragmentManager();
                    CommentNotificationsBottomSheetFragment bottomSheet =
                            (CommentNotificationsBottomSheetFragment) fm.findFragmentByTag(
                                    NOTIFICATIONS_BOTTOM_SHEET_TAG
                            );
                    if (isShowingData.getShow() && bottomSheet == null) {
                        bottomSheet = CommentNotificationsBottomSheetFragment.newInstance(
                                isShowingData.isReceivingNotifications(),
                                false
                        );
                        bottomSheet.show(fm, NOTIFICATIONS_BOTTOM_SHEET_TAG);
                    } else if (!isShowingData.getShow() && bottomSheet != null) {
                        bottomSheet.dismiss();
                    }
                    return Unit.INSTANCE;
                })
        );

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mHasUpdatedComments = savedInstanceState.getBoolean(KEY_HAS_UPDATED_COMMENTS);
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI);
            mSource = savedInstanceState.getString(ReaderConstants.ARG_SOURCE);
        } else {
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mDirectOperation = (DirectOperation) getIntent()
                    .getSerializableExtra(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = getIntent().getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0);
            mInterceptedUri = getIntent().getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI);
            mSource = getIntent().getStringExtra(ReaderConstants.ARG_SOURCE);
        }

        mConversationViewModel.start(mBlogId, mPostId, ThreadedCommentsActionSource.READER_THREADED_COMMENTS);
    }

    @Override
    public void onCollapse(@Nullable Bundle result) {
        if (mBoxBinding != null && result != null) {
            mBoxBinding.editComment.setText(result.getString(RESULT_REPLY));
            mBoxBinding.editComment.setSelection(
                    result.getInt(RESULT_SELECTION_START),
                    result.getInt(RESULT_SELECTION_END)
            );
            mBoxBinding.editComment.requestFocus();
        }
    }

    @Override
    public void onConfirm(@Nullable Bundle result) {
        if (mBinding != null && mBoxBinding != null && result != null) {
            mBoxBinding.editComment.setText(result.getString(RESULT_REPLY));
            submitComment(mBinding, mBoxBinding);
        }
    }

    private final View.OnClickListener mSignInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(@NonNull View v) {
            if (isFinishing()) {
                return;
            }

            if (mInterceptedUri != null) {
                mReaderTracker.trackUri(AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED, mInterceptedUri);
            }
            ActivityLauncher.loginWithoutMagicLink(ReaderCommentListActivity.this);
        }
    };

    // to do a complete refresh we need to get updated post and new comments
    private void updatePostAndComments(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        if (mPost != null) {
            ReaderPostActions.updatePost(mPost, result -> {
                if (!isFinishing() && result.isNewOrChanged()) {
                    // get the updated post and pass it to the adapter
                    ReaderPost post = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
                    if (post != null) {
                        getCommentAdapter(binding, boxBinding).setPost(post);
                        mPost = post;
                    }
                }
            });

            // load the first page of comments
            updateComments(binding, mPost, true, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (mBinding != null && mBoxBinding != null) {
            refreshComments(mBinding, mBoxBinding);

            if (mUpdateOnResume && NetworkUtils.isNetworkAvailable(this)) {
                updatePostAndComments(mBinding, mBoxBinding);
                mUpdateOnResume = false;
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mBlogId && mSuggestionAdapter != null) {
            List<UserSuggestion> userSuggestions = UserSuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            List<Suggestion> suggestions = Suggestion.Companion.fromUserSuggestions(userSuggestions);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.threaded_comments_menu, menu);

        if (mConversationViewModel != null) {
            mConversationViewModel.getUpdateFollowUiState().observe(this, uiState -> {
                MenuItem bellItem = menu.findItem(R.id.manage_notifications_item);
                MenuItem followItem = menu.findItem(R.id.follow_item);

                if (bellItem != null && followItem != null) {
                    ShimmerFrameLayout shimmerView =
                            followItem.getActionView().findViewById(R.id.shimmer_view_container);
                    TextView followText =
                            followItem.getActionView().findViewById(R.id.follow_button);

                    followItem.getActionView().setOnClickListener(
                            uiState.getOnFollowTapped() != null
                                    ? v -> uiState.getOnFollowTapped().invoke()
                                    : null
                    );

                    bellItem.setOnMenuItemClickListener(item -> {
                        uiState.getOnManageNotificationsTapped().invoke();
                        return true;
                    });

                    followItem.getActionView().setEnabled(uiState.getFlags().isMenuEnabled());
                    followText.setEnabled(uiState.getFlags().isMenuEnabled());
                    bellItem.setEnabled(uiState.getFlags().isMenuEnabled());

                    if (uiState.getFlags().getShowMenuShimmer()) {
                        if (!shimmerView.isShimmerVisible()) {
                            shimmerView.showShimmer(true);
                        } else if (!shimmerView.isShimmerStarted()) {
                            shimmerView.startShimmer();
                        }
                    } else {
                        shimmerView.hideShimmer();
                    }

                    followItem.setVisible(uiState.getFlags().isFollowMenuVisible());
                    bellItem.setVisible(uiState.getFlags().isBellMenuVisible());

                    setResult(RESULT_OK, new Intent().putExtra(
                            FOLLOW_CONVERSATION_UI_STATE_FLAGS_KEY,
                            uiState.getFlags()
                    ));
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void performCommentAction(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding,
            @NonNull ReaderComment comment,
            @NonNull ReaderCommentMenuActionType action
    ) {
        switch (action) {
            case EDIT:
                SiteModel postSite = mSiteStore.getSiteBySiteId(comment.blogId);
                if (postSite != null) {
                    openCommentEditor(comment, postSite);
                }
                break;
            case UNAPPROVE:
                moderateComment(
                        binding,
                        boxBinding,
                        comment,
                        CommentStatus.UNAPPROVED,
                        R.string.comment_unapproved,
                        Stat.COMMENT_UNAPPROVED
                );
                break;
            case SPAM:
                moderateComment(
                        binding,
                        boxBinding,
                        comment,
                        CommentStatus.SPAM,
                        R.string.comment_spammed,
                        Stat.COMMENT_SPAMMED
                );
                break;
            case TRASH:
                moderateComment(
                        binding,
                        boxBinding,
                        comment,
                        CommentStatus.TRASH,
                        R.string.comment_trashed,
                        Stat.COMMENT_TRASHED
                );
                break;
            case SHARE:
                shareComment(comment.getShortUrl());
                break;
            case APPROVE:
            case DIVIDER_NO_ACTION:
                break;
        }
    }

    private void openCommentEditor(
            @NonNull ReaderComment comment,
            @NonNull SiteModel postSite
    ) {
        final Intent intent = UnifiedCommentsEditActivity.createIntent(
                this,
                new ReaderCommentIdentifier(comment.blogId, comment.postId, comment.commentId),
                postSite
        );
        startActivity(intent);
    }

    private void moderateComment(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding,
            ReaderComment comment,
            CommentStatus newStatus,
            int undoMessage,
            Stat tracker
    ) {
        getCommentAdapter(binding, boxBinding).removeComment(comment.commentId);
        checkEmptyView(binding, boxBinding);

        Snackbar snackbar = WPSnackbar.make(
                binding.coordinatorLayout, undoMessage, Snackbar.LENGTH_LONG
        ).setAction(R.string.undo, view -> getCommentAdapter(binding, boxBinding).refreshComments());

        snackbar.addCallback(new BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(@Nullable Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);

                if (event == DISMISS_EVENT_ACTION) {
                    AnalyticsUtils.trackCommentActionWithReaderPostDetails(
                            Stat.COMMENT_MODERATION_UNDO,
                            AnalyticsCommentActionSource.READER, mPost
                    );
                    return;
                }

                AnalyticsUtils.trackCommentActionWithReaderPostDetails(
                        tracker,
                        AnalyticsCommentActionSource.READER,
                        mPost
                );
                ReaderCommentActions.moderateComment(comment, newStatus);
            }
        });

        snackbar.show();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.CommentModerated event) {
        if (mBinding == null || mBoxBinding == null || isFinishing()) {
            return;
        }

        if (!event.isSuccess()) {
            ToastUtils.showToast(ReaderCommentListActivity.this, R.string.comment_moderation_error);
            getCommentAdapter(mBinding, mBoxBinding).refreshComments();
        } else {
            // we do try to remove the comment in case you did PTR and it appeared in the list again
            getCommentAdapter(mBinding, mBoxBinding).removeComment(event.getCommentId());
        }
        checkEmptyView(mBinding, mBoxBinding);
    }


    private void shareComment(String commentUrl) {
        mReaderTracker.trackPost(
                Stat.READER_ARTICLE_COMMENT_SHARED,
                mPost
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, commentUrl);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_link)));
    }

    @SuppressWarnings("deprecation")
    private void setReplyToCommentId(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding,
            long commentId,
            boolean doFocus
    ) {
        if (mReplyToCommentId == commentId) {
            mReplyToCommentId = 0;
        } else {
            mReplyToCommentId = commentId;
        }
        boxBinding.editComment.setHint(mReplyToCommentId == 0
                ? R.string.reader_hint_comment_on_post
                : R.string.reader_hint_comment_on_comment
        );

        if (doFocus) {
            boxBinding.editComment.postDelayed(() -> {
                final boolean isFocusableInTouchMode = boxBinding.editComment.isFocusableInTouchMode();

                boxBinding.editComment.setFocusableInTouchMode(true);
                EditTextUtils.showSoftInput(boxBinding.editComment);

                boxBinding.editComment.setFocusableInTouchMode(isFocusableInTouchMode);

                setupReplyToComment(binding, boxBinding);
            }, 200);
        } else {
            setupReplyToComment(binding, boxBinding);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setupReplyToComment(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        // if a comment is being replied to, highlight it and scroll it to the top so the user can
        // see which comment they're replying to - note that scrolling is delayed to give time for
        // listView to reposition due to soft keyboard appearing
        getCommentAdapter(binding, boxBinding).setHighlightCommentId(mReplyToCommentId, false);
        getCommentAdapter(binding, boxBinding).setReplyTargetComment(mReplyToCommentId);
        getCommentAdapter(binding, boxBinding).notifyDataSetChanged();
        if (mReplyToCommentId != 0) {
            scrollToCommentId(binding, boxBinding, mReplyToCommentId);

            // reset to replying to the post when user hasn't entered any text and hits
            // the back button in the editText to hide the soft keyboard
            boxBinding.editComment.setOnBackListener(() -> {
                if (EditTextUtils.isEmpty(boxBinding.editComment)) {
                    setReplyToCommentId(binding, boxBinding, 0, false);
                }
            });
        } else {
            boxBinding.editComment.setOnBackListener(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId);
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId);
        if (mBinding != null) {
            outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition(mBinding));
        } else {
            outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, 0);
        }
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, mHasUpdatedComments);
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri);
        outState.putString(ReaderConstants.ARG_SOURCE, mSource);

        super.onSaveInstanceState(outState);
    }

    private void showCommentsClosedMessage(
            @NonNull ReaderActivityCommentListBinding binding,
            boolean show
    ) {
        binding.textCommentsClosed.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean loadPost(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        mPost = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);
        if (mBoxBinding == null || mPost == null) {
            return false;
        }

        if (!mAccountStore.hasAccessToken()) {
            mBoxBinding.layoutContainer.setVisibility(View.GONE);
            showCommentsClosedMessage(binding, false);
        } else if (mPost.isCommentsOpen) {
            mBoxBinding.layoutContainer.setVisibility(View.VISIBLE);
            showCommentsClosedMessage(binding, false);

            mBoxBinding.editComment.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                    submitComment(binding, boxBinding);
                }
                return false;
            });

            mBoxBinding.btnSubmitReply.setOnClickListener(v -> submitComment(binding, boxBinding));
        } else {
            mBoxBinding.layoutContainer.setVisibility(View.GONE);
            mBoxBinding.editComment.setEnabled(false);
            showCommentsClosedMessage(binding, true);
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

    private ReaderCommentAdapter getCommentAdapter(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        if (mCommentAdapter == null && mPost != null) {
            mCommentAdapter = new ReaderCommentAdapter(WPActivityUtils.getThemedContext(this), mPost);

            // adapter calls this when user taps reply icon
            mCommentAdapter.setReplyListener(
                    commentId -> setReplyToCommentId(binding, boxBinding, commentId, true)
            );
            // adapter calls this when user taps share icon
            mCommentAdapter.setCommentMenuActionListener(
                    (comment, action) -> performCommentAction(binding, boxBinding, comment, action)
            );

            // Enable post title click if we came here directly from notifications or deep linking
            if (mDirectOperation != null) {
                mCommentAdapter.enableHeaderClicks();
            }

            // adapter calls this when data has been loaded & displayed
            mCommentAdapter.setDataLoadedListener(isEmpty -> {
                if (!isFinishing()) {
                    if (isEmpty || !mHasUpdatedComments) {
                        updateComments(binding, mPost, isEmpty, false);
                    } else if (mCommentId > 0 || mDirectOperation != null) {
                        if (mCommentId > 0) {
                            // Scroll to the commentId once if it was passed to this activity
                            smoothScrollToCommentId(binding, boxBinding, mCommentId);
                        }

                        doDirectOperation(binding, boxBinding);
                    } else if (mRestorePosition > 0) {
                        if (mViewModel != null) {
                            mViewModel.scrollToPosition(mRestorePosition, false);
                        }
                    }
                    mRestorePosition = 0;
                    checkEmptyView(binding, boxBinding);
                }
            });

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            mCommentAdapter.setDataRequestedListener(() -> {
                if (!mIsUpdatingComments) {
                    AppLog.i(T.READER, "reader comments > requesting next page of comments");
                    updateComments(binding, mPost, true, true);
                }
            });
        }
        return mCommentAdapter;
    }

    private void doDirectOperation(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        if (mDirectOperation != null) {
            switch (mDirectOperation) {
                case COMMENT_JUMP:
                    if (mCommentAdapter != null) {
                        mCommentAdapter.setHighlightCommentId(mCommentId, false);

                        // clear up the direct operation vars. Only performing it once.
                        mDirectOperation = null;
                        mCommentId = 0;
                    }
                    break;
                case COMMENT_REPLY:
                    setReplyToCommentId(binding, boxBinding, mCommentId, mAccountStore.hasAccessToken());

                    // clear up the direct operation vars. Only performing it once.
                    mDirectOperation = null;
                    mCommentId = 0;
                    break;
                case COMMENT_LIKE:
                    getCommentAdapter(binding, boxBinding).setHighlightCommentId(mCommentId, false);
                    if (!mAccountStore.hasAccessToken()) {
                        WPSnackbar.make(binding.coordinatorLayout,
                                          R.string.reader_snackbar_err_cannot_like_post_logged_out,
                                          Snackbar.LENGTH_INDEFINITE)
                                  .setAction(R.string.sign_in, mSignInClickListener)
                                  .show();
                    } else if (mPost != null) {
                        ReaderComment comment = ReaderCommentTable.getComment(mPost.blogId, mPost.postId, mCommentId);
                        if (comment == null) {
                            ToastUtils.showToast(
                                    ReaderCommentListActivity.this,
                                    R.string.reader_toast_err_comment_not_found
                            );
                        } else if (comment.isLikedByCurrentUser) {
                            ToastUtils.showToast(
                                    ReaderCommentListActivity.this,
                                    R.string.reader_toast_err_already_liked
                            );
                        } else {
                            long wpComUserId = mAccountStore.getAccount().getUserId();
                            if (ReaderCommentActions.performLikeAction(comment, true, wpComUserId)
                                && getCommentAdapter(binding, boxBinding).refreshComment(mCommentId)) {
                                getCommentAdapter(binding, boxBinding).setAnimateLikeCommentId(mCommentId);

                                mReaderTracker.trackPost(
                                        AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_LIKED,
                                        mPost
                                );
                                mReaderTracker.trackPost(
                                        AnalyticsTracker.Stat.COMMENT_LIKED,
                                        mPost,
                                        AnalyticsCommentActionSource.READER.toString()
                                );
                            } else {
                                ToastUtils.showToast(
                                        ReaderCommentListActivity.this,
                                        R.string.reader_toast_err_generic
                                );
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

    private void showProgress(@NonNull ReaderActivityCommentListBinding binding) {
        binding.progressLoading.setVisibility(View.VISIBLE);
    }

    private void hideProgress(@NonNull ReaderActivityCommentListBinding binding) {
        binding.progressLoading.setVisibility(View.GONE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdateCommentsStarted event) {
        mIsUpdatingComments = true;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdateCommentsEnded event) {
        if (mBinding == null || mBoxBinding == null || isFinishing()) {
            return;
        }

        mIsUpdatingComments = false;
        mHasUpdatedComments = true;
        hideProgress(mBinding);

        if (event.getResult().isNewOrChanged()) {
            mRestorePosition = getCurrentPosition(mBinding);
            refreshComments(mBinding, mBoxBinding);
        } else {
            checkEmptyView(mBinding, mBoxBinding);
        }

        setRefreshing(false);
    }

    /*
     * request comments for this post
     */
    private void updateComments(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderPost post,
            boolean showProgress,
            boolean requestNextPage
    ) {
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
            showProgress(binding);
        }
        ReaderCommentService.startService(this, post.blogId, post.postId, requestNextPage);
    }

    private void checkEmptyView(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        boolean isEmpty = hasCommentAdapter()
                          && getCommentAdapter(binding, boxBinding).isEmpty()
                          && !mIsSubmittingComment;
        if (isEmpty && !NetworkUtils.isNetworkAvailable(this)) {
            binding.textEmpty.setText(R.string.no_network_message);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else if (isEmpty && mHasUpdatedComments) {
            binding.textEmpty.setText(R.string.reader_empty_comments);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
        }
    }

    /*
     * refresh adapter so latest comments appear
     */
    private void refreshComments(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        AppLog.d(T.READER, "reader comments > refreshComments");
        getCommentAdapter(binding, boxBinding).refreshComments();
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private void scrollToCommentId(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding,
            long commentId
    ) {
        int position = getCommentAdapter(binding, boxBinding).positionOfCommentId(commentId);
        if (mViewModel != null && position > -1) {
            mViewModel.scrollToPosition(position, false);
        }
    }

    /*
     * Smoothly scrolls the passed comment to the top of the listView
     */
    private void smoothScrollToCommentId(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding,
            long commentId
    ) {
        int position = getCommentAdapter(binding, boxBinding).positionOfCommentId(commentId);
        if (mViewModel != null && position > -1) {
            mViewModel.scrollToPosition(position, true);
        }
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private void submitComment(
            @NonNull ReaderActivityCommentListBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding boxBinding
    ) {
        if (mBoxBinding == null) {
            return;
        }

        final String commentText = EditTextUtils.getText(mBoxBinding.editComment);
        if (TextUtils.isEmpty(commentText)) {
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        if (mReplyToCommentId != 0) {
            mReaderTracker.trackPost(AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_REPLIED_TO, mPost);
        } else {
            mReaderTracker.trackPost(AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON, mPost);
        }

        mBoxBinding.btnSubmitReply.setEnabled(false);
        mBoxBinding.editComment.setEnabled(false);
        mIsSubmittingComment = true;

        // generate a "fake" comment id to assign to the new comment so we can add it to the db
        // and reflect it in the adapter before the API call returns
        final long fakeCommentId = ReaderCommentActions.generateFakeCommentId();

        ReaderActions.CommentActionListener actionListener = (succeeded, newComment) -> {
            if (isFinishing()) {
                return;
            }
            mIsSubmittingComment = false;
            mBoxBinding.editComment.setEnabled(true);
            if (succeeded) {
                mBoxBinding.btnSubmitReply.setEnabled(false);
                // stop highlighting the fake comment and replace it with the real one
                getCommentAdapter(binding, boxBinding).setHighlightCommentId(0, false);
                getCommentAdapter(binding, boxBinding).setReplyTargetComment(0);
                getCommentAdapter(binding, boxBinding).replaceComment(fakeCommentId, newComment);
                getCommentAdapter(binding, boxBinding).refreshPost();
                setReplyToCommentId(binding, boxBinding, 0, false);
                mBoxBinding.editComment.getAutoSaveTextHelper().clearSavedText(mBoxBinding.editComment);
            } else {
                mBoxBinding.editComment.setText(commentText);
                mBoxBinding.btnSubmitReply.setEnabled(true);
                getCommentAdapter(binding, boxBinding).removeComment(fakeCommentId);
                ToastUtils.showToast(
                        ReaderCommentListActivity.this, R.string.reader_toast_err_comment_failed,
                        ToastUtils.Duration.LONG);
            }
            checkEmptyView(binding, boxBinding);
        };

        long wpComUserId = mAccountStore.getAccount().getUserId();
        ReaderComment newComment = ReaderCommentActions.submitPostComment(
                mPost,
                fakeCommentId,
                commentText,
                mReplyToCommentId,
                actionListener,
                wpComUserId);

        if (newComment != null) {
            mBoxBinding.editComment.setText(null);
            // add the "fake" comment to the adapter, highlight it, and show a progress bar
            // next to it while it's submitted
            getCommentAdapter(binding, boxBinding).setHighlightCommentId(newComment.commentId, true);
            getCommentAdapter(binding, boxBinding).setReplyTargetComment(0);
            getCommentAdapter(binding, boxBinding).addComment(newComment);
            // make sure it's scrolled into view
            scrollToCommentId(binding, boxBinding, fakeCommentId);
            checkEmptyView(binding, boxBinding);
        }
    }

    private int getCurrentPosition(@NonNull ReaderActivityCommentListBinding binding) {
        if (hasCommentAdapter()) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) binding.recyclerView.getLayoutManager();
            if (layoutManager != null) {
                return layoutManager.findFirstVisibleItemPosition();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void setRefreshing(boolean refreshing) {
        if (mSwipeToRefreshHelper != null) {
            mSwipeToRefreshHelper.setRefreshing(refreshing);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if user is returning from login, make sure to update the post and its comments
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == Activity.RESULT_OK) {
            mUpdateOnResume = true;
        }
    }
}
