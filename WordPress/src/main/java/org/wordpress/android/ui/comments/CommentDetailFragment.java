package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;
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
import org.wordpress.android.databinding.CommentActionFooterBinding;
import org.wordpress.android.databinding.CommentDetailFragmentBinding;
import org.wordpress.android.databinding.ReaderIncludeCommentBoxBinding;
import org.wordpress.android.datasets.NotificationsTable;
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
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload;
import org.wordpress.android.fluxc.store.CommentsStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.models.UserSuggestion;
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.Builder;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.ViewPagerFragment;
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener;
import org.wordpress.android.ui.comments.CommentActions.OnNoteCommentActionListener;
import org.wordpress.android.ui.comments.unified.CommentIdentifier;
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier;
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier;
import org.wordpress.android.ui.comments.unified.CommentSource;
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.suggestion.Suggestion;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPAvatarUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.extensions.ContextExtensionsKt;
import org.wordpress.android.util.extensions.ViewExtensionsKt;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

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
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public class CommentDetailFragment extends ViewPagerFragment implements NotificationFragment, OnConfirmListener,
        OnCollapseListener {
    private static final String KEY_MODE = "KEY_MODE";
    private static final String KEY_SITE_LOCAL_ID = "KEY_SITE_LOCAL_ID";
    private static final String KEY_COMMENT_ID = "KEY_COMMENT_ID";
    private static final String KEY_NOTE_ID = "KEY_NOTE_ID";
    private static final String KEY_REPLY_TEXT = "KEY_REPLY_TEXT";

    private static final int INTENT_COMMENT_EDITOR = 1010;
    private static final float NORMAL_OPACITY = 1f;

    @Nullable private CommentModel mComment;
    @Nullable private SiteModel mSite;

    @Nullable private Note mNote;
    @Nullable private SuggestionAdapter mSuggestionAdapter;
    @Nullable private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;
    @Nullable private String mRestoredReplyText;
    @Nullable private String mRestoredNoteId;
    private boolean mIsUsersBlog = false;
    private boolean mShouldFocusReplyField;
    @Nullable private String mPreviousStatus;
    private float mMediumOpacity;

    @Inject AccountStore mAccountStore;
    @SuppressWarnings("deprecation")
    @Inject CommentsStoreAdapter mCommentsStoreAdapter;
    @Inject SiteStore mSiteStore;
    @Inject FluxCImageLoader mImageLoader;
    @Inject ImageManager mImageManager;
    @Inject CommentsStore mCommentsStore;
    @Inject LocalCommentCacheUpdateHandler mLocalCommentCacheUpdateHandler;

    private boolean mIsSubmittingReply = false;
    @Nullable private NotificationsDetailListFragment mNotificationsDetailListFragment;
    @Nullable private OnPostClickListener mOnPostClickListener;
    @Nullable private OnCommentActionListener mOnCommentActionListener;
    @Nullable private OnNoteCommentActionListener mOnNoteCommentActionListener;

    @Nullable private CommentSource mCommentSource;

    /*
     * these determine which actions (moderation, replying, marking as spam) to enable
     * for this comment - all actions are enabled when opened from the comment list, only
     * changed when opened from a notification
     */
    @NonNull private EnumSet<EnabledActions> mEnabledActions = EnumSet.allOf(EnabledActions.class);

    @Nullable private CommentDetailFragmentBinding mBinding = null;
    @Nullable private ReaderIncludeCommentBoxBinding mReplyBinding = null;
    @Nullable private CommentActionFooterBinding mActionBinding = null;

    /*
     * used when called from comment list
     */
    @SuppressWarnings("deprecation")
    static CommentDetailFragment newInstance(
            @NonNull SiteModel site,
            CommentModel commentModel
    ) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_MODE, CommentSource.SITE_COMMENTS);
        args.putInt(KEY_SITE_LOCAL_ID, site.getId());
        args.putLong(KEY_COMMENT_ID, commentModel.getRemoteCommentId());
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * used when called from notification list for a comment notification
     */
    @SuppressWarnings("deprecation")
    public static CommentDetailFragment newInstance(final String noteId, final String replyText) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_MODE, CommentSource.NOTIFICATION);
        args.putString(KEY_NOTE_ID, noteId);
        args.putString(KEY_REPLY_TEXT, replyText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);

        mCommentSource = (CommentSource) requireArguments().getSerializable(KEY_MODE);

        switch (mCommentSource) {
            case SITE_COMMENTS:
                setComment(requireArguments().getLong(KEY_COMMENT_ID), requireArguments().getInt(KEY_SITE_LOCAL_ID));
                break;
            case NOTIFICATION:
                setNote(requireArguments().getString(KEY_NOTE_ID));
                setReplyText(requireArguments().getString(KEY_REPLY_TEXT));
                break;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getString(KEY_NOTE_ID) != null) {
                // The note will be set in onResume()
                // See WordPress.deferredInit()
                mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID);
            } else {
                int siteId = savedInstanceState.getInt(KEY_SITE_LOCAL_ID);
                long commentId = savedInstanceState.getLong(KEY_COMMENT_ID);
                setComment(commentId, siteId);
            }
        }

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
        mReplyBinding = mBinding.layoutCommentBox;
        mActionBinding = CommentActionFooterBinding.inflate(inflater, null, false);

        mMediumOpacity = ResourcesCompat.getFloat(
                getResources(),
                com.google.android.material.R.dimen.material_emphasis_medium
        );

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(
                mBinding.getRoot().getContext()
        );
        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation);

        mReplyBinding.layoutContainer.setBackgroundColor(elevatedColor);

        mReplyBinding.btnSubmitReply.setEnabled(false);
        mReplyBinding.btnSubmitReply.setOnLongClickListener(view1 -> {
            if (view1.isHapticFeedbackEnabled()) {
                view1.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            Toast.makeText(view1.getContext(), R.string.send, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewExtensionsKt.redirectContextClickToLongPressListener(mReplyBinding.btnSubmitReply);

        mReplyBinding.editComment.initializeWithPrefix('@');
        mReplyBinding.editComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(@NonNull CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(@NonNull CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(@NonNull Editable s) {
                mReplyBinding.btnSubmitReply.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });

        mReplyBinding.buttonExpand.setOnClickListener(
                v -> {
                    if (mSite != null && mComment != null) {
                        Bundle bundle = CommentFullScreenDialogFragment.Companion.newBundle(
                                mReplyBinding.editComment.getText().toString(),
                                mReplyBinding.editComment.getSelectionStart(),
                                mReplyBinding.editComment.getSelectionEnd(),
                                mSite.getSiteId()
                        );

                        new Builder(requireContext())
                                .setTitle(R.string.comment)
                                .setOnCollapseListener(this)
                                .setOnConfirmListener(this)
                                .setContent(CommentFullScreenDialogFragment.class, bundle)
                                .setAction(R.string.send)
                                .setHideActivityBar(true)
                                .build()
                                .show(requireActivity().getSupportFragmentManager(),
                                        getCommentSpecificFragmentTagSuffix(mComment));
                    }
                }
        );
        mReplyBinding.buttonExpand.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            Toast.makeText(v.getContext(), R.string.description_expand, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewExtensionsKt.redirectContextClickToLongPressListener(mReplyBinding.buttonExpand);
        setReplyUniqueId(mReplyBinding, mSite, mComment, mNote);

        // hide comment like button until we know it can be enabled in showCommentAsNotification()
        mActionBinding.btnLike.setVisibility(View.GONE);

        // hide moderation buttons until updateModerationButtons() is called
        mActionBinding.layoutButtons.setVisibility(View.GONE);

        // this is necessary in order for anchor tags in the comment text to be clickable
        mBinding.textContent.setLinksClickable(true);
        mBinding.textContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mReplyBinding.editComment.setHint(R.string.reader_hint_comment_on_comment);
        mReplyBinding.editComment.setOnEditorActionListener((v, actionId, event) -> {
            if (mSite != null && mComment != null
                && (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)) {
                submitReply(mReplyBinding, mSite, mComment);
            }
            return false;
        });

        if (!TextUtils.isEmpty(mRestoredReplyText)) {
            mReplyBinding.editComment.setText(mRestoredReplyText);
            mRestoredReplyText = null;
        }

        mReplyBinding.btnSubmitReply.setOnClickListener(v -> {
            if (mSite != null && mComment != null) {
                submitReply(mReplyBinding, mSite, mComment);
            }
        });

        mActionBinding.btnSpam.setOnClickListener(v -> {
            if (mSite != null && mComment != null) {
                if (CommentStatus.fromString(mComment.getStatus()) == CommentStatus.SPAM) {
                    moderateComment(mBinding, mActionBinding, mSite, mComment, mNote, CommentStatus.APPROVED);
                    announceCommentStatusChangeForAccessibility(CommentStatus.UNSPAM);
                } else {
                    moderateComment(mBinding, mActionBinding, mSite, mComment, mNote, CommentStatus.SPAM);
                    announceCommentStatusChangeForAccessibility(CommentStatus.SPAM);
                }
            }
        });

        mActionBinding.btnLike.setOnClickListener(v -> {
            if (mSite != null && mComment != null) {
                likeComment(mActionBinding, mSite, mComment, false);
            }
        });

        mActionBinding.btnMore.setOnClickListener(v -> {
            if (mSite != null && mComment != null) {
                showMoreMenu(mBinding, mActionBinding, mSite, mComment, mNote, v);
            }
        });
        // hide more button until we know it can be enabled
        mActionBinding.btnMore.setVisibility(View.GONE);

        if (mSite != null) {
            setupSuggestionServiceAndAdapter(mReplyBinding, mSite);
        }

        return mBinding.getRoot();
    }

    @NonNull
    private String getCommentSpecificFragmentTagSuffix(@NonNull CommentModel comment) {
        return CollapseFullScreenDialogFragment.TAG + "_"
               + comment.getRemoteSiteId() + "_"
               + comment.getRemoteCommentId();
    }

    @Override
    public void onConfirm(@Nullable Bundle result) {
        if (mReplyBinding != null && result != null && mSite != null && mComment != null) {
            mReplyBinding.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY));
            submitReply(mReplyBinding, mSite, mComment);
        }
    }

    @Override
    public void onCollapse(@Nullable Bundle result) {
        if (mReplyBinding != null && result != null) {
            mReplyBinding.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY));
            mReplyBinding.editComment.setSelection(result.getInt(
                            CommentFullScreenDialogFragment.RESULT_SELECTION_START),
                    result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END));
            mReplyBinding.editComment.requestFocus();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.COMMENT_DETAIL);

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(mRestoredNoteId)) {
            setNote(mRestoredNoteId);
            mRestoredNoteId = null;
        }

        CollapseFullScreenDialogFragment fragment = null;
        if (mComment != null) {
            // reattach listeners to collapsible reply dialog
            // we need to to it in onResume to make sure mComment is already initialized
            fragment = (CollapseFullScreenDialogFragment) requireActivity()
                    .getSupportFragmentManager().findFragmentByTag(getCommentSpecificFragmentTagSuffix(mComment));
        }

        if (fragment != null && fragment.isAdded()) {
            fragment.setOnCollapseListener(this);
            fragment.setOnConfirmListener(this);
        }
    }

    private void setupSuggestionServiceAndAdapter(
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull SiteModel site
    ) {
        if (!isAdded() || !SiteUtils.isAccessedViaWPComRest(site)) {
            return;
        }
        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), site.getSiteId());
        mSuggestionAdapter = SuggestionUtils.setupUserSuggestions(
                site,
                requireActivity(),
                mSuggestionServiceConnectionManager
        );
        replyBinding.editComment.setAdapter(mSuggestionAdapter);
    }

    private void setReplyUniqueId(
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @Nullable SiteModel site,
            @Nullable CommentModel comment,
            @Nullable Note note
    ) {
        if (isAdded()) {
            String sId = null;
            if (site != null && comment != null) {
                sId = String.format(Locale.US, "%d-%d", site.getSiteId(), comment.getRemoteCommentId());
            } else if (note != null) {
                sId = String.format(Locale.US, "%d-%d", note.getSiteId(), note.getCommentId());
            }
            if (sId != null) {
                replyBinding.editComment.getAutoSaveTextHelper().setUniqueId(sId);
                replyBinding.editComment.getAutoSaveTextHelper().loadString(replyBinding.editComment);
            }
        }
    }

    private void setComment(final long commentRemoteId, final int siteLocalId) {
        final SiteModel site = mSiteStore.getSiteByLocalId(siteLocalId);
        if (site != null) {
            setComment(site, mCommentsStoreAdapter.getCommentBySiteAndRemoteId(site, commentRemoteId));
        }
    }

    private void setComment(
            @NonNull final SiteModel site,
            @Nullable final CommentModel comment
    ) {
        mSite = site;
        mComment = comment;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null);

        if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
            showComment(mBinding, mReplyBinding, mActionBinding, mSite, mComment, mNote);
        }

        // Reset the reply unique id since mComment just changed.
        if (mReplyBinding != null) setReplyUniqueId(mReplyBinding, mSite, mComment, mNote);
    }

    private void disableShouldFocusReplyField() {
        mShouldFocusReplyField = false;
    }

    public void enableShouldFocusReplyField() {
        mShouldFocusReplyField = true;
    }

    @Nullable
    @Override
    public Note getNote() {
        return mNote;
    }

    private SiteModel createDummyWordPressComSite(long siteId) {
        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        site.setSiteId(siteId);
        return site;
    }

    public void setNote(@NonNull Note note) {
        mNote = note;
        mSite = mSiteStore.getSiteBySiteId(note.getSiteId());
        if (mSite == null) {
            // This should not exist, we should clean that screen so a note without a site/comment can be displayed
            mSite = createDummyWordPressComSite(mNote.getSiteId());
        }
        if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
            showComment(mBinding, mReplyBinding, mActionBinding, mSite, mComment, mNote);
        }
    }

    @Override
    public void setNote(String noteId) {
        if (noteId == null) {
            showErrorToastAndFinish();
            return;
        }

        Note note = NotificationsTable.getNoteById(noteId);
        if (note == null) {
            showErrorToastAndFinish();
        } else {
            setNote(note);
        }
    }

    private void setReplyText(String replyText) {
        if (replyText == null) {
            return;
        }
        mRestoredReplyText = replyText;
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        if (getActivity() != null) {
            ToastUtils.showToast(getActivity(), R.string.error_notification_open);
            getActivity().finish();
        }
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
        if (mBinding != null && mReplyBinding != null && mActionBinding != null && mSite != null) {
            showComment(mBinding, mReplyBinding, mActionBinding, mSite, mComment, mNote);
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
    private void editComment(@NonNull SiteModel site) {
        if (!isAdded()) {
            return;
        }
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_EDITOR_OPENED,
                    mCommentSource.toAnalyticsCommentActionSource(),
                    site
            );
        }

        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        final CommentIdentifier commentIdentifier = mapCommentIdentifier();
        if (commentIdentifier != null) {
            final Intent intent = UnifiedCommentsEditActivity.createIntent(
                    requireActivity(),
                    commentIdentifier,
                    site
            );
            startActivityForResult(intent, INTENT_COMMENT_EDITOR);
        } else {
            throw new IllegalArgumentException("CommentIdentifier cannot be null");
        }
    }

    @Nullable
    private CommentIdentifier mapCommentIdentifier() {
        if (mCommentSource == null) return null;
        switch (mCommentSource) {
            case SITE_COMMENTS:
                if (mComment != null) {
                    return new SiteCommentIdentifier(mComment.getId(), mComment.getRemoteCommentId());
                } else {
                    return null;
                }
            case NOTIFICATION:
                if (mNote != null) {
                    return new NotificationCommentIdentifier(mNote.getId(), mNote.getCommentId());
                } else {
                    return null;
                }
            default:
                return null;
        }
    }

    /*
     * display the current comment
     */
    private void showComment(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @Nullable CommentModel comment,
            @Nullable Note note
    ) {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (comment == null) {
            // Hide container views when comment is null (will happen when opened from a notification).
            showCommentWhenNull(binding, replyBinding, actionBinding, note);
        } else {
            showCommentWhenNonNull(binding, replyBinding, actionBinding, site, comment, note);
        }
    }

    private void showCommentWhenNull(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull CommentActionFooterBinding actionBinding,
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
                showCommentAsNotification(binding, replyBinding, actionBinding, site, comment, note);
            } else {
                // It's not in our store yet, request it.
                RemoteCommentPayload payload = new RemoteCommentPayload(site, note.getCommentId());
                mCommentsStoreAdapter.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
                setProgressVisible(binding, true);

                // Show a "temporary" comment built from the note data, the view will be refreshed once the
                // comment has been fetched.
                showCommentAsNotification(binding, replyBinding, actionBinding, site, null, note);
            }
        }
    }

    private void showCommentWhenNonNull(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note
    ) {
        // These two views contain all the other views except the progress bar.
        binding.nestedScrollView.setVisibility(View.VISIBLE);
        binding.layoutBottom.setVisibility(View.VISIBLE);

        // Add action buttons footer
        if (note == null && actionBinding.layoutButtons.getParent() == null) {
            binding.commentContentContainer.addView(actionBinding.layoutButtons);
        }

        binding.textName.setText(
                comment.getAuthorName() == null ? getString(R.string.anonymous) : comment.getAuthorName()
        );
        binding.textDate.setText(
                DateTimeUtils.javaDateToTimeSpan(
                        DateTimeUtils.dateFromIso8601(comment.getDatePublished()), WordPress.getContext()
                )
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
                    new AvatarQueryOptions.Builder().setPreferredSize(avatarSz).build()).url(null).toString();
        }
        mImageManager.loadIntoCircle(binding.imageAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);

        updateStatusViews(binding, actionBinding, site, comment, note);

        // navigate to author's blog when avatar or name clicked
        if (comment.getAuthorUrl() != null) {
            View.OnClickListener authorListener =
                    v -> ReaderActivityLauncher.openUrl(getActivity(), comment.getAuthorUrl());
            binding.imageAvatar.setOnClickListener(authorListener);
            binding.textName.setOnClickListener(authorListener);
            binding.textName.setTextColor(ContextExtensionsKt.getColorFromAttribute(
                    binding.textName.getContext(),
                    com.google.android.material.R.attr.colorPrimary)
            );
        } else {
            binding.textName.setTextColor(ContextExtensionsKt.getColorFromAttribute(
                    binding.textName.getContext(),
                    com.google.android.material.R.attr.colorOnSurface)
            );
        }

        showPostTitle(binding, comment, site);

        // make sure reply box is showing
        if (replyBinding.layoutContainer.getVisibility() != View.VISIBLE && canReply()) {
            AniUtils.animateBottomBar(replyBinding.layoutContainer, true);
            if (mShouldFocusReplyField) {
                replyBinding.editComment.performClick();
                disableShouldFocusReplyField();
            }
        }

        requireActivity().invalidateOptionsMenu();
    }

    /*
     * displays the passed post title for the current comment, updates stored title if one doesn't exist
     */
    private void setPostTitle(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentModel comment,
            String postTitle,
            boolean isHyperlink
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

        // display "on [Post Title]..."
        if (isHyperlink) {
            String html = getString(R.string.on)
                          + " <font color=" + HtmlUtils.colorResToHtmlColor(getActivity(),
                    ContextExtensionsKt.getColorResIdFromAttribute(
                            requireActivity(),
                            com.google.android.material.R.attr.colorPrimary
                    ))
                          + ">"
                          + postTitle.trim()
                          + "</font>";
            binding.textPostTitle.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY));
        } else {
            String text = getString(R.string.on) + " " + postTitle.trim();
            binding.textPostTitle.setText(text);
        }
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
            setPostTitle(binding, comment, title, canRequestPost);
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
                                                setPostTitle(binding, comment, postTitle, true);
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

            binding.textPostTitle.setOnClickListener(v -> {
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
        }
    }

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
    private void moderateComment(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
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
            dispatchModerationAction(site, comment, newStatus);
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener.onModerateComment(comment, newStatus);
            // Sad, but onModerateComment does the moderation itself (due to the undo bar), this should be refactored,
            // That's why we don't call dispatchModerationAction() here.
        }

        updateStatusViews(binding, actionBinding, site, comment, note);
    }

    private void dispatchModerationAction(
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            CommentStatus newStatus
    ) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            mCommentsStoreAdapter.dispatch(
                    CommentActionBuilder.newDeleteCommentAction(new RemoteCommentPayload(site, comment))
            );
        } else {
            // Actual moderation (push the modified comment).
            comment.setStatus(newStatus.toString());
            mCommentsStoreAdapter.dispatch(
                    CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(site, comment))
            );
        }
    }

    /*
     * post comment box text as a reply to the current comment
     */
    @SuppressWarnings("deprecation")
    private void submitReply(
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment
    ) {
        if (!isAdded() || mIsSubmittingReply) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        final String replyText = EditTextUtils.getText(replyBinding.editComment);
        if (TextUtils.isEmpty(replyText)) {
            return;
        }

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        replyBinding.editComment.setEnabled(false);
        EditTextUtils.hideSoftInput(replyBinding.editComment);
        replyBinding.btnSubmitReply.setVisibility(View.GONE);
        replyBinding.progressSubmitComment.setVisibility(View.VISIBLE);

        mIsSubmittingReply = true;

        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentReplyWithDetails(
                    false,
                    site,
                    comment,
                    mCommentSource.toAnalyticsCommentActionSource()
            );
        }

        // Pseudo comment reply
        CommentModel reply = new CommentModel();
        reply.setContent(replyText);

        mCommentsStoreAdapter.dispatch(
                CommentActionBuilder.newCreateNewCommentAction(new RemoteCreateCommentPayload(site, comment, reply))
        );
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note
    ) {
        if (!isAdded()) {
            return;
        }

        final int statusTextResId; // string resource id for status text
        final int statusColor; // color for status text

        CommentStatus commentStatus = CommentStatus.fromString(comment.getStatus());
        switch (commentStatus) {
            case APPROVED:
                statusTextResId = R.string.comment_status_approved;
                statusColor = ContextExtensionsKt.getColorFromAttribute(
                        requireActivity(),
                        R.attr.wpColorWarningDark
                );
                break;
            case UNAPPROVED:
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = ContextExtensionsKt.getColorFromAttribute(
                        requireActivity(),
                        R.attr.wpColorWarningDark
                );
                break;
            case SPAM:
                statusTextResId = R.string.comment_status_spam;
                statusColor = ContextExtensionsKt.getColorFromAttribute(
                        requireActivity(),
                        com.google.android.material.R.attr.colorError
                );
                break;
            case DELETED:
            case ALL:
            case UNREPLIED:
            case UNSPAM:
            case UNTRASH:
            case TRASH:
            default:
                statusTextResId = R.string.comment_status_trash;
                statusColor = ContextExtensionsKt.getColorFromAttribute(
                        requireActivity(),
                        com.google.android.material.R.attr.colorError
                );
                break;
        }

        if (canLike(site)) {
            actionBinding.btnLike.setVisibility(View.VISIBLE);
            toggleLikeButton(actionBinding, comment.getILike());
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been CommentStatus.APPROVED
        if (mIsUsersBlog && commentStatus != CommentStatus.APPROVED) {
            binding.textStatus.setText(getString(statusTextResId).toUpperCase(Locale.getDefault()));
            binding.textStatus.setTextColor(statusColor);
            if (binding.textStatus.getVisibility() != View.VISIBLE) {
                binding.textStatus.clearAnimation();
                AniUtils.fadeIn(binding.textStatus, AniUtils.Duration.LONG);
            }
        } else {
            binding.textStatus.setVisibility(View.GONE);
        }

        if (canModerate()) {
            setModerateButtonForStatus(actionBinding, commentStatus);
            actionBinding.btnModerate.setOnClickListener(
                    v -> performModerateAction(binding, actionBinding, site, comment, note)
            );
            actionBinding.btnModerate.setVisibility(View.VISIBLE);
        } else {
            actionBinding.btnModerate.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            actionBinding.btnSpam.setVisibility(View.VISIBLE);
            if (commentStatus == CommentStatus.SPAM) {
                actionBinding.btnSpamText.setText(R.string.mnu_comment_unspam);
            } else {
                actionBinding.btnSpamText.setText(R.string.mnu_comment_spam);
            }
        } else {
            actionBinding.btnSpam.setVisibility(View.GONE);
        }

        if (canTrash()) {
            if (commentStatus == CommentStatus.TRASH) {
                ColorUtils.setImageResourceWithTint(
                        actionBinding.btnModerateIcon,
                        R.drawable.ic_undo_white_24dp,
                        ContextExtensionsKt.getColorResIdFromAttribute(
                                actionBinding.btnModerateText.getContext(),
                                com.google.android.material.R.attr.colorOnSurface
                        )
                );
                actionBinding.btnModerateText.setText(R.string.mnu_comment_untrash);
            }
        }

        if (canShowMore()) {
            actionBinding.btnMore.setVisibility(View.VISIBLE);
        } else {
            actionBinding.btnMore.setVisibility(View.GONE);
        }

        actionBinding.layoutButtons.setVisibility(View.VISIBLE);
    }

    private void performModerateAction(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note
    ) {
        if (!isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        CommentStatus newStatus = CommentStatus.APPROVED;
        CommentStatus currentStatus = CommentStatus.fromString(comment.getStatus());
        if (currentStatus == CommentStatus.APPROVED) {
            newStatus = CommentStatus.UNAPPROVED;
        }
        announceCommentStatusChangeForAccessibility(
                currentStatus == CommentStatus.TRASH ? CommentStatus.UNTRASH : newStatus);

        setModerateButtonForStatus(actionBinding, newStatus);
        AniUtils.startAnimation(actionBinding.btnModerateIcon, R.anim.notifications_button_scale);
        moderateComment(binding, actionBinding, site, comment, note, newStatus);
    }

    private void setModerateButtonForStatus(
            @NonNull CommentActionFooterBinding actionBinding,
            CommentStatus status
    ) {
        int color;

        if (status == CommentStatus.APPROVED) {
            color = ContextExtensionsKt.getColorResIdFromAttribute(
                    actionBinding.btnModerateText.getContext(),
                    com.google.android.material.R.attr.colorSecondary
            );
            actionBinding.btnModerateText.setText(R.string.comment_status_approved);
            actionBinding.btnModerateText.setAlpha(NORMAL_OPACITY);
            actionBinding.btnModerateIcon.setAlpha(NORMAL_OPACITY);
        } else {
            color = ContextExtensionsKt.getColorResIdFromAttribute(
                    actionBinding.btnModerateText.getContext(),
                    com.google.android.material.R.attr.colorOnSurface
            );
            actionBinding.btnModerateText.setText(R.string.mnu_comment_approve);
            actionBinding.btnModerateText.setAlpha(mMediumOpacity);
            actionBinding.btnModerateIcon.setAlpha(mMediumOpacity);
        }

        ColorUtils.setImageResourceWithTint(
                actionBinding.btnModerateIcon,
                R.drawable.ic_checkmark_white_24dp, color
        );
        actionBinding.btnModerateText.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        return mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
               || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE);
    }

    private boolean canMarkAsSpam() {
        return mEnabledActions.contains(EnabledActions.ACTION_SPAM);
    }

    private boolean canReply() {
        return mEnabledActions.contains(EnabledActions.ACTION_REPLY);
    }

    private boolean canTrash() {
        return canModerate();
    }

    private boolean canEdit(@NonNull SiteModel site) {
        return site.getHasCapabilityEditOthersPosts() || site.isSelfHostedAdmin();
    }

    private boolean canLike(@NonNull SiteModel site) {
        return mEnabledActions.contains(EnabledActions.ACTION_LIKE_COMMENT)
               && SiteUtils.isAccessedViaWPComRest(site);
    }

    /*
     * The more button contains controls which only moderates can use
     */
    private boolean canShowMore() {
        return canModerate();
    }

    /*
     * display the comment associated with the passed notification
     */
    private void showCommentAsNotification(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @Nullable CommentModel comment,
            @Nullable Note note
    ) {
        // hide standard comment views, since we'll be adding note blocks instead
        binding.commentContent.setVisibility(View.GONE);

        binding.textContent.setVisibility(View.GONE);

        /*
         * determine which actions to enable for this comment - if the comment is from this user's
         * blog then all actions will be enabled, but they won't be if it's a reply to a comment
         * this user made on someone else's blog
         */
        if (note != null) {
            mEnabledActions = note.getEnabledCommentActions();
        }

        // Set 'Reply to (Name)' in comment reply EditText if it's a reasonable size
        if (note != null
            && !TextUtils.isEmpty(note.getCommentAuthorName())
            && note.getCommentAuthorName().length() < 28) {
            replyBinding.editComment.setHint(
                    String.format(getString(R.string.comment_reply_to_user), note.getCommentAuthorName())
            );
        }

        if (comment != null) {
            setComment(site, comment);
        } else if (note != null) {
            setComment(site, note.buildComment());
        }

        if (note != null) {
            addDetailFragment(binding, actionBinding, note.getId());
        }

        requireActivity().invalidateOptionsMenu();
    }

    private void addDetailFragment(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
            String noteId
    ) {
        // Now we'll add a detail fragment list
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mNotificationsDetailListFragment = NotificationsDetailListFragment.newInstance(noteId);
        mNotificationsDetailListFragment.setFooterView(actionBinding.layoutButtons);
        fragmentTransaction.replace(binding.commentContentContainer.getId(), mNotificationsDetailListFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void likeComment(
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @SuppressWarnings("SameParameterValue") boolean forceLike
    ) {
        if (!isAdded()) {
            return;
        }
        if (forceLike && actionBinding.btnLike.isActivated()) {
            return;
        }

        toggleLikeButton(actionBinding, !actionBinding.btnLike.isActivated());

        ReaderAnim.animateLikeButton(actionBinding.btnLikeIcon, actionBinding.btnLike.isActivated());

        // Bump analytics
        // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
        if (mCommentSource == CommentSource.NOTIFICATION) {
            AnalyticsTracker.track(
                    actionBinding.btnLike.isActivated() ? Stat.NOTIFICATION_LIKED : Stat.NOTIFICATION_UNLIKED
            );
        }
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                    actionBinding.btnLike.isActivated() ? Stat.COMMENT_LIKED : Stat.COMMENT_UNLIKED,
                    mCommentSource.toAnalyticsCommentActionSource(),
                    site
            );
        }

        if (mNotificationsDetailListFragment != null) {
            // Optimistically set comment to approved when liking an unapproved comment
            // WP.com will set a comment to approved if it is liked while unapproved
            if (actionBinding.btnLike.isActivated()
                && CommentStatus.fromString(comment.getStatus()) == CommentStatus.UNAPPROVED) {
                comment.setStatus(CommentStatus.APPROVED.toString());
                mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.APPROVED);
                setModerateButtonForStatus(actionBinding, CommentStatus.APPROVED);
            }
        }
        mCommentsStoreAdapter.dispatch(CommentActionBuilder.newLikeCommentAction(
                new RemoteLikeCommentPayload(site, comment, actionBinding.btnLike.isActivated()))
        );
        if (mNote != null) {
            EventBus.getDefault().postSticky(new NotificationEvents
                    .OnNoteCommentLikeChanged(mNote, actionBinding.btnLike.isActivated()));
        }

        actionBinding.btnLike.announceForAccessibility(
                getText(actionBinding.btnLike.isActivated() ? R.string.comment_liked_talkback
                        : R.string.comment_unliked_talkback)
        );
    }

    private void toggleLikeButton(
            @NonNull CommentActionFooterBinding actionBinding,
            boolean isLiked
    ) {
        int color;
        int drawable;

        if (isLiked) {
            color = ContextExtensionsKt.getColorResIdFromAttribute(
                    actionBinding.btnLikeIcon.getContext(),
                    com.google.android.material.R.attr.colorSecondary
            );
            drawable = R.drawable.ic_star_white_24dp;
            actionBinding.btnLikeText.setText(getResources().getString(R.string.mnu_comment_liked));
            actionBinding.btnLike.setActivated(true);
            actionBinding.btnLikeText.setAlpha(NORMAL_OPACITY);
            actionBinding.btnLikeIcon.setAlpha(NORMAL_OPACITY);
        } else {
            color = ContextExtensionsKt.getColorResIdFromAttribute(
                    actionBinding.btnLikeIcon.getContext(),
                    com.google.android.material.R.attr.colorOnSurface
            );
            drawable = R.drawable.ic_star_outline_white_24dp;
            actionBinding.btnLikeText.setText(getResources().getString(R.string.reader_label_like));
            actionBinding.btnLike.setActivated(false);
            actionBinding.btnLikeText.setAlpha(mMediumOpacity);
            actionBinding.btnLikeIcon.setAlpha(mMediumOpacity);
        }

        ColorUtils.setImageResourceWithTint(actionBinding.btnLikeIcon, drawable, color);
        actionBinding.btnLikeText.setTextColor(ContextCompat.getColor(requireContext(), color));
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
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
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
            updateStatusViews(binding, actionBinding, site, comment, note);
            ToastUtils.showToast(requireActivity(), R.string.error_moderate_comment);
        } else {
            reloadComment(site, comment, note);
        }
    }

    @SuppressWarnings("deprecation")
    private void onCommentCreated(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull ReaderIncludeCommentBoxBinding replyBinding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note,
            OnCommentChanged event
    ) {
        mIsSubmittingReply = false;
        replyBinding.editComment.setEnabled(true);
        replyBinding.btnSubmitReply.setVisibility(View.VISIBLE);
        replyBinding.progressSubmitComment.setVisibility(View.GONE);
        updateStatusViews(binding, actionBinding, site, comment, note);

        if (event.isError()) {
            if (isAdded()) {
                String strUnEscapeHTML = StringEscapeUtils.unescapeHtml4(event.error.message);
                ToastUtils.showToast(getActivity(), strUnEscapeHTML, ToastUtils.Duration.LONG);
                // refocus editor on failure and show soft keyboard
                EditTextUtils.showSoftInput(replyBinding.editComment);
            }
            return;
        }

        reloadComment(site, comment, note);

        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
            replyBinding.editComment.setText(null);
            replyBinding.editComment.getAutoSaveTextHelper().clearSavedText(replyBinding.editComment);
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
            moderateComment(binding, actionBinding, site, comment, note, CommentStatus.APPROVED);
        }
    }

    private void onCommentLiked(
            @NonNull CommentActionFooterBinding actionBinding,
            @Nullable Note note,
            OnCommentChanged event
    ) {
        // send signal for listeners to perform any needed updates
        if (note != null) {
            EventBus.getDefault().postSticky(new NotificationEvents.NoteLikeOrModerationStatusChanged(note.getId()));
        }

        if (event.isError()) {
            // Revert button state in case of an error
            toggleLikeButton(actionBinding, !actionBinding.btnLike.isActivated());
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

        if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
            setProgressVisible(mBinding, false);

            if (mSite != null && mComment != null) {
                // Moderating comment
                if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
                    onCommentModerated(mBinding, mActionBinding, mSite, mComment, mNote, event);
                    mPreviousStatus = null;
                    return;
                }

                // New comment (reply)
                if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
                    onCommentCreated(mBinding, mReplyBinding, mActionBinding, mSite, mComment, mNote, event);
                    return;
                }
            }

            // Like/Unlike
            if (event.causeOfChange == CommentAction.LIKE_COMMENT) {
                onCommentLiked(mActionBinding, mNote, event);
                return;
            }
        }

        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            if (isAdded() && !TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
        }
    }

    private void announceCommentStatusChangeForAccessibility(CommentStatus newStatus) {
        int resId = -1;
        switch (newStatus) {
            case APPROVED:
                resId = R.string.comment_approved_talkback;
                break;
            case UNAPPROVED:
                resId = R.string.comment_unapproved_talkback;
                break;
            case SPAM:
                resId = R.string.comment_spam_talkback;
                break;
            case TRASH:
                resId = R.string.comment_trash_talkback;
                break;
            case DELETED:
                resId = R.string.comment_delete_talkback;
                break;
            case UNSPAM:
                resId = R.string.comment_unspam_talkback;
                break;
            case UNTRASH:
                resId = R.string.comment_untrash_talkback;
                break;
            case UNREPLIED:
            case ALL:
                // ignore
                break;
            default:
                AppLog.w(T.COMMENTS,
                        "AnnounceCommentStatusChangeForAccessibility - Missing switch branch for comment status: "
                        + newStatus);
        }
        if (resId != -1 && getView() != null) {
            getView().announceForAccessibility(getText(resId));
        }
    }

    // Handle More Menu
    private void showMoreMenu(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note,
            View view
    ) {
        androidx.appcompat.widget.PopupMenu morePopupMenu =
                new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        morePopupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                editComment(site);
                return true;
            }
            if (item.getItemId() == R.id.action_trash) {
                trashComment(binding, actionBinding, site, comment, note);
                return true;
            }
            if (item.getItemId() == R.id.action_copy_link_address) {
                copyCommentLinkAddress(binding, comment);
                return true;
            }
            return false;
        });

        morePopupMenu.inflate(R.menu.menu_comment_more);

        MenuItem trashMenuItem = morePopupMenu.getMenu().findItem(R.id.action_trash);
        MenuItem copyLinkAddress = morePopupMenu.getMenu().findItem(R.id.action_copy_link_address);
        if (canTrash()) {
            CommentStatus commentStatus = CommentStatus.fromString(comment.getStatus());
            if (commentStatus == CommentStatus.TRASH) {
                copyLinkAddress.setVisible(false);
                trashMenuItem.setTitle(R.string.mnu_comment_delete_permanently);
            } else {
                trashMenuItem.setTitle(R.string.mnu_comment_trash);
                copyLinkAddress.setVisible(commentStatus != CommentStatus.SPAM);
            }
        } else {
            trashMenuItem.setVisible(false);
            copyLinkAddress.setVisible(false);
        }

        MenuItem editMenuItem = morePopupMenu.getMenu().findItem(R.id.action_edit);
        editMenuItem.setVisible(false);
        if (canEdit(site)) {
            editMenuItem.setVisible(true);
        }
        morePopupMenu.show();
    }

    private void trashComment(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentActionFooterBinding actionBinding,
            @NonNull SiteModel site,
            @NonNull CommentModel comment,
            @Nullable Note note
    ) {
        if (!isAdded()) {
            return;
        }

        CommentStatus status = CommentStatus.fromString(comment.getStatus());
        // If the comment status is trash or spam, next deletion is a permanent deletion.
        if (status == CommentStatus.TRASH || status == CommentStatus.SPAM) {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.delete));
            dialogBuilder.setMessage(getResources().getText(R.string.dlg_sure_to_delete_comment));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    (dialog, whichButton) -> {
                        moderateComment(binding, actionBinding, site, comment, note, CommentStatus.DELETED);
                        announceCommentStatusChangeForAccessibility(CommentStatus.DELETED);
                    });
            dialogBuilder.setNegativeButton(
                    getResources().getText(R.string.no),
                    null);
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            moderateComment(binding, actionBinding, site, comment, note, CommentStatus.TRASH);
            announceCommentStatusChangeForAccessibility(CommentStatus.TRASH);
        }
    }

    private void copyCommentLinkAddress(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentModel comment
    ) {
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("CommentLinkAddress", comment.getUrl()));
            showSnackBar(binding, comment, getString(R.string.comment_q_action_copied_url));
        } catch (Exception e) {
            AppLog.e(T.UTILS, e);
            showSnackBar(binding, comment, getString(R.string.error_copy_to_clipboard));
        }
    }

    private void showSnackBar(
            @NonNull CommentDetailFragmentBinding binding,
            @NonNull CommentModel comment,
            String message
    ) {
        Snackbar snackBar = WPSnackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                                      .setAction(getString(R.string.share_action),
                                              v -> {
                                                  try {
                                                      Intent intent = new Intent(Intent.ACTION_SEND);
                                                      intent.setType("text/plain");
                                                      intent.putExtra(Intent.EXTRA_TEXT, comment.getUrl());
                                                      startActivity(Intent.createChooser(intent,
                                                              getString(R.string.comment_share_link_via)));
                                                  } catch (ActivityNotFoundException exception) {
                                                      ToastUtils.showToast(binding.getRoot().getContext(),
                                                              R.string.comment_toast_err_share_intent);
                                                  }
                                              })
                                      .setAnchorView(binding.layoutBottom);
        snackBar.show();
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
        mReplyBinding = null;
        mActionBinding = null;
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }
}
