package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.gravatar.AvatarQueryOptions;
import com.gravatar.AvatarUrl;
import com.gravatar.types.Email;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.CommentDetailFragmentBinding;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.UserSuggestionTable;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentsStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.fluxc.tools.FormattableContentMapper;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.UserSuggestion;
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.ViewPagerFragment;
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener;
import org.wordpress.android.ui.comments.CommentActions.OnNoteCommentActionListener;
import org.wordpress.android.ui.comments.unified.CommentActionPopupHandler;
import org.wordpress.android.ui.comments.unified.CommentIdentifier;
import org.wordpress.android.ui.comments.unified.CommentSource;
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity;
import org.wordpress.android.ui.engagement.BottomSheetUiState.UserProfileUiState;
import org.wordpress.android.ui.engagement.UserProfileBottomSheetFragment;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.suggestion.Suggestion;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPAvatarUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.List;

import javax.inject.Inject;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.DelicateCoroutinesApi;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 *
 * @deprecated Comments are being refactored as part of Comments Unification project. If you are adding any
 * features or modifying this class, please ping develric or klymyam
 *
 * Use [SiteCommentDetailFragment] or [NotificationCommentDetailFragment] instead before removing this class
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public abstract class CommentDetailFragment extends ViewPagerFragment implements NotificationFragment {
    protected static final String KEY_MODE = "KEY_MODE";
    protected static final String KEY_SITE_LOCAL_ID = "KEY_SITE_LOCAL_ID";
    protected static final String KEY_COMMENT_ID = "KEY_COMMENT_ID";
    protected static final String KEY_NOTE_ID = "KEY_NOTE_ID";
    protected static final int INTENT_COMMENT_EDITOR = 1010;

    @Nullable protected CommentModel mComment;
    @Nullable protected SiteModel mSite;

    @Nullable protected Note mNote;
    @Nullable private SuggestionAdapter mSuggestionAdapter;
    @Nullable private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;
    protected boolean mIsUsersBlog = false;
    protected boolean mShouldFocusReplyField;
    @Nullable private String mPreviousStatus;
    protected float mMediumOpacity;

    @Inject AccountStore mAccountStore;
    @SuppressWarnings("deprecation")
    @Inject CommentsStoreAdapter mCommentsStoreAdapter;
    @Inject SiteStore mSiteStore;
    @Inject FluxCImageLoader mImageLoader;
    @Inject ImageManager mImageManager;
    @Inject CommentsStore mCommentsStore;
    @Inject LocalCommentCacheUpdateHandler mLocalCommentCacheUpdateHandler;
    @Inject FormattableContentMapper mContentMapper;

    private boolean mIsSubmittingReply = false;
    @Nullable private NotificationsDetailListFragment mNotificationsDetailListFragment;
    @Nullable private OnPostClickListener mOnPostClickListener;
    @Nullable private OnCommentActionListener mOnCommentActionListener;
    @Nullable private OnNoteCommentActionListener mOnNoteCommentActionListener;
    @NonNull protected CommentSource mCommentSource; // this will be non-null when onCreate()

    @Nullable protected CommentDetailFragmentBinding mBinding = null;

    private CommentDetailViewModel mViewModel;

    private final OnActionClickListener mOnActionClickListener = new OnActionClickListener() {
        @Override public void onEditCommentClicked() {
            editComment();
        }

        @Override public void onUserInfoClicked() {
            UserProfileBottomSheetFragment.newInstance(getUserProfileUiState())
                                          .show(getChildFragmentManager(), UserProfileBottomSheetFragment.TAG);
        }

        @Override public void onShareClicked() {
            if (getContext() != null) {
                ActivityLauncher.openShareIntent(getContext(), mComment.getUrl(), null);
            }
        }

        @Override public void onChangeStatusClicked() {
            showModerationBottomSheet();
        }
    };

    abstract void showModerationBottomSheet();

    abstract UserProfileUiState getUserProfileUiState();

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        mViewModel = new ViewModelProvider(this).get(CommentDetailViewModel.class);
        mCommentSource = (CommentSource) requireArguments().getSerializable(KEY_MODE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSite != null && mComment != null) {
            outState.putLong(KEY_COMMENT_ID, mComment.getRemoteCommentId());
            outState.putInt(KEY_SITE_LOCAL_ID, mSite.getId());
        }

        if (mNote != null) {
            outState.putString(KEY_NOTE_ID, mNote.getId());
        }
    }

    // touching the file resulted in the MethodLength, it's suppressed until we get time to refactor this method
    @Nullable
    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        mBinding = CommentDetailFragmentBinding.inflate(inflater, container, false);
        mMediumOpacity = ResourcesCompat.getFloat(
                getResources(),
                com.google.android.material.R.dimen.material_emphasis_medium
        );

        // this is necessary in order for anchor tags in the comment text to be clickable
        mBinding.textContent.setLinksClickable(true);
        mBinding.textContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mBinding.imageMore.setOnClickListener(v ->
                CommentActionPopupHandler.show(mBinding.imageMore, mOnActionClickListener)
        );

        return mBinding.getRoot();
    }

    @NonNull
    private String getCommentSpecificFragmentTagSuffix(@NonNull CommentModel comment) {
        return CollapseFullScreenDialogFragment.TAG + "_"
               + comment.getRemoteSiteId() + "_"
               + comment.getRemoteCommentId();
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.COMMENT_DETAIL);
    }

    protected void setComment(
            @NonNull final SiteModel site,
            @Nullable final CommentModel comment
    ) {
        mSite = site;
        mComment = comment;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null);

        if (mBinding != null) {
            showComment(mBinding, mSite, mComment, mNote);
        }

        updateModerationStatus();
    }

    abstract void updateModerationStatus();

    public void enableShouldFocusReplyField() {
        mShouldFocusReplyField = true;
    }

    @Nullable
    private Note getNote() {
        return mNote;
    }

    protected SiteModel createDummyWordPressComSite(long siteId) {
        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        site.setSiteId(siteId);
        return site;
    }

    @SuppressWarnings("deprecation") // TODO: Remove when minSdkVersion >= 23
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnPostClickListener) {
            mOnPostClickListener = (OnPostClickListener) activity;
        }
        if (activity instanceof OnCommentActionListener) {
            mOnCommentActionListener = (OnCommentActionListener) activity;
        }
        if (activity instanceof OnNoteCommentActionListener) {
            mOnNoteCommentActionListener = (OnNoteCommentActionListener) activity;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mCommentsStoreAdapter.register(this);
        if (mBinding != null && mSite != null) {
            showComment(mBinding, mSite, mComment, mNote);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        mCommentsStoreAdapter.unregister(this);
        super.onStop();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0
            && mSite != null
            && event.mRemoteBlogId == mSite.getSiteId()
            && mSuggestionAdapter != null
        ) {
            List<UserSuggestion> userSuggestions = UserSuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            List<Suggestion> suggestions = Suggestion.Companion.fromUserSuggestions(userSuggestions);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mSite != null && mComment != null
            && requestCode == INTENT_COMMENT_EDITOR && resultCode == Activity.RESULT_OK) {
            reloadComment(mSite, mComment, mNote);
        }
    }

    /**
     * Reload the current comment from the local database
     */
    private void reloadComment(
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note
    ) {
        CommentModel updatedComment = mCommentsStoreAdapter.getCommentByLocalId(comment.getId());
        if (updatedComment != null) {
            setComment(site, updatedComment);
        }
        if (mNotificationsDetailListFragment != null && note != null) {
            mNotificationsDetailListFragment.refreshBlocksForEditedComment(note.getId());
        }
    }

    /**
     * open the comment for editing
     */
    @SuppressWarnings("deprecation")
    private void editComment() {
        if (!isAdded()) {
            return;
        }
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_EDITOR_OPENED,
                    mCommentSource.toAnalyticsCommentActionSource(),
                    mSite
            );
        }

        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        final CommentIdentifier commentIdentifier = getCommentIdentifier();
        if (commentIdentifier != null) {
            final Intent intent = UnifiedCommentsEditActivity.createIntent(
                    requireActivity(),
                    commentIdentifier,
                    mSite
            );
            startActivityForResult(intent, INTENT_COMMENT_EDITOR);
        } else {
            throw new IllegalArgumentException("CommentIdentifier cannot be null");
        }
    }

    abstract CommentIdentifier getCommentIdentifier();

    /*
     * display the current comment
     */
    protected void showComment(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull SiteModel site,
            @Nullable CommentModel comment,
            @Nullable Note note
    ) {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (comment == null) {
            // Hide container views when comment is null (will happen when opened from a notification).
            showCommentWhenNull(binding, note);
        } else {
            showCommentWhenNonNull(binding, site, comment);
        }
    }

    private void showCommentWhenNull(
            @NonNull CommentDetailFragmentBinding binding,
            @Nullable Note note
    ) {
        // These two views contain all the other views except the progress bar.
        binding.nestedScrollView.setVisibility(View.GONE);
        binding.layoutBottom.setVisibility(View.GONE);

        if (note != null) {
            SiteModel site = mSiteStore.getSiteBySiteId(note.getSiteId());
            if (site == null) {
                // This should not exist, we should clean that screen so a note without a site/comment
                // can be displayed
                site = createDummyWordPressComSite(note.getSiteId());
            }

            // Check if the comment is already in our store
            CommentModel comment = mCommentsStoreAdapter.getCommentBySiteAndRemoteId(site, note.getCommentId());
            if (comment != null) {
                // It exists, then show it as a "Notification"
                showCommentAsNotification(binding, site, comment, note);
            } else {
                // It's not in our store yet, request it.
                RemoteCommentPayload payload = new RemoteCommentPayload(site, note.getCommentId());
                mCommentsStoreAdapter.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
                setProgressVisible(binding, true);

                // Show a "temporary" comment built from the note data, the view will be refreshed once the
                // comment has been fetched.
                showCommentAsNotification(binding, site, null, note);
            }
        }
    }

    private void showCommentWhenNonNull(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment
    ) {
        // These two views contain all the other views except the progress bar.
        binding.nestedScrollView.setVisibility(View.VISIBLE);
        binding.layoutBottom.setVisibility(View.VISIBLE);

        binding.textName.setText(
                comment.getAuthorName() == null ? getString(R.string.anonymous) : comment.getAuthorName()
        );

        String renderingError = getString(R.string.comment_unable_to_show_error);
        binding.textContent.post(() -> CommentUtils.displayHtmlComment(
                binding.textContent,
                comment.getContent(),
                binding.textContent.getWidth(),
                binding.textContent.getLineHeight(),
                renderingError
        ));

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        String avatarUrl = "";
        if (comment.getAuthorProfileImageUrl() != null) {
            avatarUrl = WPAvatarUtils.rewriteAvatarUrl(comment.getAuthorProfileImageUrl(), avatarSz);
        } else if (comment.getAuthorEmail() != null) {
            avatarUrl = new AvatarUrl(new Email(comment.getAuthorEmail()),
                    new AvatarQueryOptions(avatarSz, null, null, null)).url().toString();
        }
        mImageManager.loadIntoCircle(binding.imageAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);

        // navigate to author's blog when avatar or name clicked
        if (comment.getAuthorUrl() != null) {
            View.OnClickListener authorListener =
                    v -> ReaderActivityLauncher.openUrl(getActivity(), comment.getAuthorUrl());
            binding.imageAvatar.setOnClickListener(authorListener);
            binding.textName.setOnClickListener(authorListener);
        }

        showPostTitle(binding, comment, site);
        binding.textSite.setText(DateTimeUtils.javaDateToTimeSpan(
                DateTimeUtils.dateFromIso8601(comment.getDatePublished()), WordPress.getContext()));

        requireActivity().invalidateOptionsMenu();
    }

    /*
     * displays the passed post title for the current comment, updates stored title if one doesn't exist
     */
    private void setPostTitle(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentModel comment,
            String postTitle
    ) {
        if (!isAdded()) {
            return;
        }
        if (TextUtils.isEmpty(postTitle)) {
            binding.textPostTitle.setText(R.string.untitled);
            return;
        }

        // if comment doesn't have a post title, set it to the passed one and save to comment table
        if (comment.getPostTitle() == null) {
            comment.setPostTitle(postTitle);
            mCommentsStoreAdapter.dispatch(CommentActionBuilder.newUpdateCommentAction(comment));
        }

        binding.textPostTitle.setText(postTitle.trim());
    }

    /*
     * ensure the post associated with this comment is available to the reader and show its
     * title above the comment
     */
    private void showPostTitle(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentModel comment,
            @NonNull SiteModel site
    ) {
        if (!isAdded()) {
            return;
        }

        boolean postExists = ReaderPostTable.postExists(site.getSiteId(), comment.getRemotePostId());

        // the post this comment is on can only be requested if this is a .com blog or a
        // jetpack-enabled self-hosted blog, and we have valid .com credentials
        boolean canRequestPost = SiteUtils.isAccessedViaWPComRest(site) && mAccountStore.hasAccessToken();

        final String title;
        final boolean hasTitle;
        if (comment.getPostTitle() != null) {
            // use comment's stored post title if available
            title = comment.getPostTitle();
            hasTitle = true;
        } else if (postExists) {
            // use title from post if available
            title = ReaderPostTable.getPostTitle(site.getSiteId(), comment.getRemotePostId());
            hasTitle = !TextUtils.isEmpty(title);
        } else {
            title = null;
            hasTitle = false;
        }
        if (hasTitle) {
            setPostTitle(binding, comment, title);
        } else if (canRequestPost) {
            binding.textPostTitle.setText(postExists ? R.string.untitled : R.string.loading);
        }

        // if this is a .com or jetpack blog, tapping the title shows the associated post
        // in the reader
        if (canRequestPost) {
            // first make sure this post is available to the reader, and once it's retrieved set
            // the title if it wasn't set above
            if (!postExists) {
                AppLog.d(T.COMMENTS, "comment detail > retrieving post");
                ReaderPostActions
                        .requestBlogPost(
                                site.getSiteId(),
                                comment.getRemotePostId(),
                                new ReaderActions.OnRequestListener<String>() {
                                    @Override
                                    public void onSuccess(String blogUrl) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        // update title if it wasn't set above
                                        if (!hasTitle) {
                                            String postTitle = ReaderPostTable.getPostTitle(
                                                    site.getSiteId(),
                                                    comment.getRemotePostId()
                                            );
                                            if (!TextUtils.isEmpty(postTitle)) {
                                                setPostTitle(binding, comment, postTitle);
                                            } else {
                                                binding.textPostTitle.setText(R.string.untitled);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(int statusCode) {
                                    }
                                });
            }

            binding.headerView.setOnClickListener(v -> {
                if (mOnPostClickListener != null) {
                    mOnPostClickListener.onPostClicked(
                            getNote(),
                            site.getSiteId(),
                            (int) comment.getRemotePostId()
                    );
                } else {
                    // right now this will happen from notifications
                    AppLog.i(T.COMMENTS, "comment detail > no post click listener");
                    ReaderActivityLauncher.showReaderPostDetail(
                            getActivity(),
                            site.getSiteId(),
                            comment.getRemotePostId()
                    );
                }
            });

            handleHeaderVisibility();
        }
    }

    abstract void handleHeaderVisibility();

    // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
    private void trackModerationEvent(final CommentStatus newStatus) {
        if (mCommentSource == null) return;
        switch (newStatus) {
            case APPROVED:
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_APPROVED);
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_APPROVED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case UNAPPROVED:
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_UNAPPROVED);
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_UNAPPROVED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case SPAM:
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_FLAGGED_AS_SPAM);
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_SPAMMED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case UNSPAM:
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_UNSPAMMED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case TRASH:
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_TRASHED);
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_TRASHED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case UNTRASH:
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_UNTRASHED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case DELETED:
                AnalyticsUtils.trackCommentActionWithSiteDetails(Stat.COMMENT_DELETED,
                        mCommentSource.toAnalyticsCommentActionSource(), mSite);
                break;
            case UNREPLIED:
            case ALL:
                break;
        }
    }

    /*
     * approve, disapprove, spam, or trash the current comment
     */
    protected void moderateComment(
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note,
            @NonNull CommentStatus newStatus
    ) {
        if (!isAdded()) {
            return;
        }
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        mPreviousStatus = comment.getStatus();

        // Restoring comment from trash or spam works by approving it, but we want to track the actual action
        // instead of generic Approve action
        CommentStatus statusToTrack;
        if (CommentStatus.fromString(mPreviousStatus) == CommentStatus.SPAM && newStatus == CommentStatus.APPROVED) {
            statusToTrack = CommentStatus.UNSPAM;
        } else if (CommentStatus.fromString(mPreviousStatus) == CommentStatus.TRASH
                   && newStatus == CommentStatus.APPROVED) {
            statusToTrack = CommentStatus.UNTRASH;
        } else {
            statusToTrack = newStatus;
        }

        trackModerationEvent(statusToTrack);

        // Fire the appropriate listener if we have one
        if (note != null && mOnNoteCommentActionListener != null) {
            mOnNoteCommentActionListener.onModerateCommentForNote(note, newStatus);
            mViewModel.dispatchModerationAction(site, comment, newStatus);
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener.onModerateComment(comment, newStatus);
            // Sad, but onModerateComment does the moderation itself (due to the undo bar), this should be refactored,
            // That's why we don't call dispatchModerationAction() here.
        }
    }

    /*
     * display the comment associated with the passed notification
     */
    private void showCommentAsNotification(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull SiteModel site,
            @Nullable CommentModel comment,
            @Nullable Note note
    ) {
        // hide standard comment views, since we'll be adding note blocks instead
        binding.commentContent.setVisibility(View.GONE);

        binding.textContent.setVisibility(View.GONE);

        if (comment != null) {
            setComment(site, comment);
        } else if (note != null) {
            setComment(site, note.buildComment());
        }

        if (note != null) {
            addDetailFragment(binding, note.getId());
        }

        requireActivity().invalidateOptionsMenu();
    }

    private void addDetailFragment(
            @NonNull CommentDetailFragmentBinding binding,
            String noteId
    ) {
        // Now we'll add a detail fragment list
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mNotificationsDetailListFragment = NotificationsDetailListFragment.newInstance(noteId);
        mNotificationsDetailListFragment.setOnEditCommentListener(mOnActionClickListener);
        fragmentTransaction.replace(binding.commentContentContainer.getId(), mNotificationsDetailListFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void setProgressVisible(
            @NonNull CommentDetailFragmentBinding binding,
            boolean visible
    ) {
        if (isAdded()) {
            binding.progressLoading.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void onCommentModerated(
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note,
            OnCommentChanged event
    ) {
        // send signal for listeners to perform any needed updates
        if (note != null) {
            EventBus.getDefault().postSticky(new NotificationEvents.NoteLikeOrModerationStatusChanged(note.getId()));
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            comment.setStatus(mPreviousStatus);
            ToastUtils.showToast(requireActivity(), R.string.error_moderate_comment);
        } else {
            reloadComment(site, comment, note);
        }
    }

    @SuppressWarnings("deprecation")
    private void onCommentCreated(
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note,
            OnCommentChanged event
    ) {
        mIsSubmittingReply = false;

        if (event.isError()) {
            if (isAdded()) {
                String strUnEscapeHTML = StringEscapeUtils.unescapeHtml4(event.error.message);
                ToastUtils.showToast(getActivity(), strUnEscapeHTML, ToastUtils.Duration.LONG);
            }
            return;
        }

        reloadComment(site, comment, note);

        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
        }

        // Self Hosted site does not return a newly created comment, so we need to fetch it manually.
        if (!site.isUsingWpComRestApi() && !event.changedCommentsLocalIds.isEmpty()) {
            CommentModel createdComment =
                    mCommentsStoreAdapter.getCommentByLocalId(event.changedCommentsLocalIds.get(0));

            if (createdComment != null) {
                mCommentsStoreAdapter.dispatch(CommentActionBuilder.newFetchCommentAction(
                        new RemoteCommentPayload(site, createdComment.getRemoteCommentId())));
            }
        }

        // approve the comment
        if (!(CommentStatus.fromString(comment.getStatus()) == CommentStatus.APPROVED)) {
            moderateComment(site, comment, note, CommentStatus.APPROVED);
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    @OptIn(markerClass = DelicateCoroutinesApi.class)
    public void onCommentChanged(OnCommentChanged event) {
        // requesting local comment cache refresh
        BuildersKt.launch(GlobalScope.INSTANCE,
                Dispatchers.getMain(),
                CoroutineStart.DEFAULT,
                (coroutineScope, continuation) -> mLocalCommentCacheUpdateHandler.requestCommentsUpdate(continuation)
        );

        if (mBinding != null) {
            setProgressVisible(mBinding, false);

            if (mSite != null && mComment != null) {
                // Moderating comment
                if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
                    onCommentModerated(mSite, mComment, mNote, event);
                    mPreviousStatus = null;
                    return;
                }

                // New comment (reply)
                if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
                    onCommentCreated(mSite, mComment, mNote, event);
                    return;
                }
            }
        }

        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            if (isAdded() && !TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
        }
    }

    @Nullable
    @Override
    public View getScrollableViewForUniqueIdProvision() {
        if (mBinding != null) {
            return mBinding.nestedScrollView;
        } else {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }

    /**
     * Listener for handling comment actions in [NotificationsDetailListFragment]
     */
    public interface OnActionClickListener {
        void onEditCommentClicked();

        void onUserInfoClicked();

        void onShareClicked();

        void onChangeStatusClicked();
    }
}
