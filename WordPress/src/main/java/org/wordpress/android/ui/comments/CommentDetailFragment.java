package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.Builder;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener;
import org.wordpress.android.ui.comments.CommentActions.OnNoteCommentActionListener;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private static final String KEY_MODE = "KEY_MODE";
    private static final String KEY_SITE_LOCAL_ID = "KEY_SITE_LOCAL_ID";
    private static final String KEY_COMMENT_ID = "KEY_COMMENT_ID";
    private static final String KEY_NOTE_ID = "KEY_NOTE_ID";
    private static final String KEY_REPLY_TEXT = "KEY_REPLY_TEXT";

    private static final int INTENT_COMMENT_EDITOR = 1010;
    private static final int FROM_BLOG_COMMENT = 1;
    private static final int FROM_NOTE = 2;

    private CommentModel mComment;
    private SiteModel mSite;

    private Note mNote;
    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;
    private TextView mTxtStatus;
    private TextView mTxtContent;
    private View mSubmitReplyBtn;
    private SuggestionAutoCompleteText mEditReply;
    private ViewGroup mLayoutReply;
    private ViewGroup mLayoutButtons;
    private ViewGroup mCommentContentLayout;
    private View mBtnLikeComment;
    private ImageView mBtnLikeIcon;
    private TextView mBtnLikeTextView;
    private View mBtnModerateComment;
    private ImageView mBtnModerateIcon;
    private TextView mBtnModerateTextView;
    private View mBtnSpamComment;
    private TextView mBtnSpamCommentText;
    private View mBtnMoreComment;
    private View mSnackbarAnchor;
    private String mRestoredReplyText;
    private String mRestoredNoteId;
    private boolean mIsUsersBlog = false;
    private boolean mShouldFocusReplyField;
    private String mPreviousStatus;
    private float mNormalOpacity = 1f;
    private float mMediumOpacity;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject CommentStore mCommentStore;
    @Inject SiteStore mSiteStore;
    @Inject FluxCImageLoader mImageLoader;
    @Inject ImageManager mImageManager;

    private boolean mIsSubmittingReply = false;
    private NotificationsDetailListFragment mNotificationsDetailListFragment;
    private OnPostClickListener mOnPostClickListener;
    private OnCommentActionListener mOnCommentActionListener;
    private OnNoteCommentActionListener mOnNoteCommentActionListener;

    /*
     * these determine which actions (moderation, replying, marking as spam) to enable
     * for this comment - all actions are enabled when opened from the comment list, only
     * changed when opened from a notification
     */
    private EnumSet<EnabledActions> mEnabledActions = EnumSet.allOf(EnabledActions.class);

    /*
     * used when called from comment list
     */
    static CommentDetailFragment newInstance(SiteModel site, CommentModel commentModel) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MODE, FROM_BLOG_COMMENT);
        args.putInt(KEY_SITE_LOCAL_ID, site.getId());
        args.putLong(KEY_COMMENT_ID, commentModel.getRemoteCommentId());
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * used when called from notification list for a comment notification
     */
    public static CommentDetailFragment newInstance(final String noteId, final String replyText) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MODE, FROM_NOTE);
        args.putString(KEY_NOTE_ID, noteId);
        args.putString(KEY_REPLY_TEXT, replyText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        switch (getArguments().getInt(KEY_MODE)) {
            case FROM_BLOG_COMMENT:
                setComment(getArguments().getLong(KEY_COMMENT_ID), getArguments().getInt(KEY_SITE_LOCAL_ID));
                break;
            case FROM_NOTE:
                setNote(getArguments().getString(KEY_NOTE_ID));
                setReplyText(getArguments().getString(KEY_REPLY_TEXT));
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
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mComment != null) {
            outState.putLong(KEY_COMMENT_ID, mComment.getRemoteCommentId());
            outState.putInt(KEY_SITE_LOCAL_ID, mSite.getId());
        }

        if (mNote != null) {
            outState.putString(KEY_NOTE_ID, mNote.getId());
        }
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }

    // touching the file resulted in the MethodLength, it's suppressed until we get time to refactor this method
    @SuppressWarnings("checkstyle:MethodLength")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);

        mMediumOpacity = ResourcesCompat.getFloat(getResources(), R.dimen.material_emphasis_medium);

        mTxtStatus = view.findViewById(R.id.text_status);
        mTxtContent = view.findViewById(R.id.text_content);

        //noinspection InflateParams
        mLayoutButtons = (ViewGroup) inflater.inflate(R.layout.comment_action_footer, null, false);
        mBtnLikeComment = mLayoutButtons.findViewById(R.id.btn_like);
        mBtnLikeIcon = mLayoutButtons.findViewById(R.id.btn_like_icon);
        mBtnLikeTextView = mLayoutButtons.findViewById(R.id.btn_like_text);
        mBtnModerateComment = mLayoutButtons.findViewById(R.id.btn_moderate);
        mBtnModerateIcon = mLayoutButtons.findViewById(R.id.btn_moderate_icon);
        mBtnModerateTextView = mLayoutButtons.findViewById(R.id.btn_moderate_text);
        mBtnSpamComment = mLayoutButtons.findViewById(R.id.btn_spam);
        mBtnSpamCommentText = mLayoutButtons.findViewById(R.id.btn_spam_text);
        mBtnMoreComment = mLayoutButtons.findViewById(R.id.btn_more);
        mSnackbarAnchor = view.findViewById(R.id.layout_bottom);

        // As we are using CommentDetailFragment in a ViewPager, and we also use nested fragments within
        // CommentDetailFragment itself:
        // It is important to have a live reference to the Comment Container layout at the moment this layout is
        // inflated (onCreateView), so we can make sure we set its ID correctly once we have an actual Comment object
        // to populate it with. Otherwise, we could be searching and finding the container for _another fragment/page
        // in the viewpager_, which would cause strange results (changing the views for a different fragment than we
        // intended to).
        mCommentContentLayout = view.findViewById(R.id.comment_content_container);

        mLayoutReply = view.findViewById(R.id.layout_comment_box);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(view.getContext());
        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation);

        mLayoutReply.setBackgroundColor(elevatedColor);

        mSubmitReplyBtn = mLayoutReply.findViewById(R.id.btn_submit_reply);
        mSubmitReplyBtn.setEnabled(false);
        mSubmitReplyBtn.setOnLongClickListener(view1 -> {
            if (view1.isHapticFeedbackEnabled()) {
                view1.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            Toast.makeText(view1.getContext(), R.string.send, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(mSubmitReplyBtn);

        mEditReply = mLayoutReply.findViewById(R.id.edit_comment);
        mEditReply.addTextChangedListener(new TextWatcher() {
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

        ImageView buttonExpand = mLayoutReply.findViewById(R.id.button_expand);
        buttonExpand.setOnClickListener(
                v -> {
                    Bundle bundle = CommentFullScreenDialogFragment.Companion.newBundle(
                            mEditReply.getText().toString(),
                            mEditReply.getSelectionStart(),
                            mEditReply.getSelectionEnd(),
                            mSite.getSiteId()
                    );

                    new Builder(requireContext())
                            .setTitle(R.string.comment)
                            .setOnCollapseListener(result -> {
                                if (result != null) {
                                    mEditReply.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY));
                                    mEditReply.setSelection(result.getInt(
                                            CommentFullScreenDialogFragment.RESULT_SELECTION_START),
                                            result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END));
                                    mEditReply.requestFocus();
                                }
                            })
                            .setOnConfirmListener(result -> {
                                if (result != null) {
                                    mEditReply.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY));
                                    submitReply();
                                }
                            })
                            .setContent(CommentFullScreenDialogFragment.class, bundle)
                            .setAction(R.string.send)
                            .setHideActivityBar(true)
                            .build()
                            .show(requireActivity().getSupportFragmentManager(),
                                    CollapseFullScreenDialogFragment.TAG);
                }
        );
        buttonExpand.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            Toast.makeText(v.getContext(), R.string.description_expand, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(buttonExpand);
        setReplyUniqueId();

        // hide comment like button until we know it can be enabled in showCommentAsNotification()
        mBtnLikeComment.setVisibility(View.GONE);

        // hide moderation buttons until updateModerationButtons() is called
        mLayoutButtons.setVisibility(View.GONE);

        // this is necessary in order for anchor tags in the comment text to be clickable
        mTxtContent.setLinksClickable(true);
        mTxtContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mEditReply.setHint(R.string.reader_hint_comment_on_comment);
        mEditReply.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                submitReply();
            }
            return false;
        });

        if (!TextUtils.isEmpty(mRestoredReplyText)) {
            mEditReply.setText(mRestoredReplyText);
            mRestoredReplyText = null;
        }

        mSubmitReplyBtn.setOnClickListener(v -> submitReply());

        mBtnSpamComment.setOnClickListener(v -> {
            if (mComment == null) {
                return;
            }

            if (CommentStatus.fromString(mComment.getStatus()) == CommentStatus.SPAM) {
                moderateComment(CommentStatus.APPROVED);
                announceCommentStatusChangeForAccessibility(CommentStatus.UNSPAM);
            } else {
                moderateComment(CommentStatus.SPAM);
                announceCommentStatusChangeForAccessibility(CommentStatus.SPAM);
            }
        });

        mBtnLikeComment.setOnClickListener(v -> likeComment(false));

        mBtnMoreComment.setOnClickListener(v -> showMoreMenu(v));
        // hide more button until we know it can be enabled
        mBtnMoreComment.setVisibility(View.GONE);

        setupSuggestionServiceAndAdapter();

        return view;
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
    }

    private void setupSuggestionServiceAndAdapter() {
        if (!isAdded() || mSite == null || !SiteUtils.isAccessedViaWPComRest(mSite)) {
            return;
        }
        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), mSite.getSiteId());
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mSite, getActivity(),
                mSuggestionServiceConnectionManager, false);
        if (mSuggestionAdapter != null) {
            mEditReply.setAdapter(mSuggestionAdapter);
        }
    }

    private void setReplyUniqueId() {
        if (mEditReply != null && isAdded()) {
            String sId = null;
            if (mSite != null && mComment != null) {
                sId = String.format(Locale.US, "%d-%d", mSite.getSiteId(), mComment.getRemoteCommentId());
            } else if (mNote != null) {
                sId = String.format(Locale.US, "%d-%d", mNote.getSiteId(), mNote.getCommentId());
            }
            if (sId != null) {
                mEditReply.getAutoSaveTextHelper().setUniqueId(sId);
                mEditReply.getAutoSaveTextHelper().loadString(mEditReply);
            }
        }
    }

    private void setComment(final long commentRemoteId, final int siteLocalId) {
        final SiteModel site = mSiteStore.getSiteByLocalId(siteLocalId);
        setComment(mCommentStore.getCommentBySiteAndRemoteId(site, commentRemoteId), site);
    }

    private void setComment(@Nullable final CommentModel comment, @Nullable final SiteModel site) {
        mComment = comment;
        mSite = site;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null && site != null);

        if (isAdded()) {
            showComment();
        }

        // Reset the reply unique id since mComment just changed.
        setReplyUniqueId();
    }

    private void disableShouldFocusReplyField() {
        mShouldFocusReplyField = false;
    }

    public void enableShouldFocusReplyField() {
        mShouldFocusReplyField = true;
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    private SiteModel createDummyWordPressComSite(long siteId) {
        SiteModel site = new SiteModel();
        site.setIsWPCom(true);
        site.setSiteId(siteId);
        return site;
    }

    public void setNote(Note note) {
        mNote = note;
        mSite = mSiteStore.getSiteBySiteId(note.getSiteId());
        if (mSite == null) {
            // This should not exist, we should clean that screen so a note without a site/comment can be displayed
            mSite = createDummyWordPressComSite(mNote.getSiteId());
        }
        if (isAdded() && mNote != null) {
            showComment();
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
            return;
        }
        setNote(note);
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
    public void onAttach(@NotNull Activity activity) {
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
        mDispatcher.register(this);
        showComment();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && mSite != null
            && event.mRemoteBlogId == mSite.getSiteId() && mSuggestionAdapter != null) {
            List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_COMMENT_EDITOR && resultCode == Activity.RESULT_OK) {
            reloadComment();
        }
    }

    /**
     * Reload the current comment from the local database
     */
    private void reloadComment() {
        if (mComment == null) {
            return;
        }
        CommentModel updatedComment = mCommentStore.getCommentByLocalId(mComment.getId());
        if (updatedComment != null) {
            setComment(updatedComment, mSite);
        }
    }

    /**
     * open the comment for editing
     */
    private void editComment() {
        if (!isAdded() || mComment == null) {
            return;
        }
        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        Intent intent = new Intent(getActivity(), EditCommentActivity.class);
        intent.putExtra(WordPress.SITE, mSite);
        intent.putExtra(EditCommentActivity.KEY_COMMENT, mComment);
        if (mNote != null && mComment == null) {
            intent.putExtra(EditCommentActivity.KEY_NOTE_ID, mNote.getId());
        }
        startActivityForResult(intent, INTENT_COMMENT_EDITOR);
    }

    /*
     * display the current comment
     */
    private void showComment() {
        if (!isAdded() || getView() == null) {
            return;
        }

        // these two views contain all the other views except the progress bar
        final ScrollView scrollView = getView().findViewById(R.id.scroll_view);
        final View layoutBottom = getView().findViewById(R.id.layout_bottom);

        // hide container views when comment is null (will happen when opened from a notification)
        if (mComment == null) {
            scrollView.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.GONE);

            if (mNote != null) {
                SiteModel site = mSiteStore.getSiteBySiteId(mNote.getSiteId());
                if (site == null) {
                    // This should not exist, we should clean that screen so a note without a site/comment
                    // can be displayed
                    site = createDummyWordPressComSite(mNote.getSiteId());
                }

                // Check if the comment is already in our store
                CommentModel comment = mCommentStore.getCommentBySiteAndRemoteId(site, mNote.getCommentId());
                if (comment != null) {
                    // It exists, then show it as a "Notification"
                    showCommentAsNotification(mNote, site, comment);
                } else {
                    // It's not in our store yet, request it.
                    RemoteCommentPayload payload = new RemoteCommentPayload(site, mNote.getCommentId());
                    mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
                    setProgressVisible(true);

                    // Show a "temporary" comment built from the note data, the view will be refreshed once the
                    // comment has been fetched.
                    showCommentAsNotification(mNote, site, null);
                }
            }
            return;
        }

        scrollView.setVisibility(View.VISIBLE);
        layoutBottom.setVisibility(View.VISIBLE);

        // Add action buttons footer
        if (mNote == null && mLayoutButtons.getParent() == null) {
            mCommentContentLayout.addView(mLayoutButtons);
        }

        final ImageView imgAvatar = getView().findViewById(R.id.image_avatar);
        final TextView txtName = getView().findViewById(R.id.text_name);
        final TextView txtDate = getView().findViewById(R.id.text_date);

        txtName.setText(mComment.getAuthorName() == null ? getString(R.string.anonymous) : mComment.getAuthorName());
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(mComment.getDatePublished()),
                WordPress.getContext()));

        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(mTxtContent, mComment.getContent(), maxImageSz);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        String avatarUrl = "";
        if (mComment.getAuthorProfileImageUrl() != null) {
            avatarUrl = GravatarUtils.fixGravatarUrl(mComment.getAuthorProfileImageUrl(), avatarSz);
        } else if (mComment.getAuthorEmail() != null) {
            avatarUrl = GravatarUtils.gravatarFromEmail(mComment.getAuthorEmail(), avatarSz);
        }
        mImageManager.loadIntoCircle(imgAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);

        updateStatusViews();

        // navigate to author's blog when avatar or name clicked
        if (mComment.getAuthorUrl() != null) {
            View.OnClickListener authorListener =
                    v -> ReaderActivityLauncher.openUrl(getActivity(), mComment.getAuthorUrl());
            imgAvatar.setOnClickListener(authorListener);
            txtName.setOnClickListener(authorListener);
            txtName.setTextColor(ContextExtensionsKt.getColorFromAttribute(txtName.getContext(), R.attr.colorPrimary));
        } else {
            txtName.setTextColor(
                    ContextExtensionsKt.getColorFromAttribute(txtName.getContext(), R.attr.colorOnSurface));
        }

        showPostTitle(mSite, mComment.getRemotePostId());

        // make sure reply box is showing
        if (mLayoutReply.getVisibility() != View.VISIBLE && canReply()) {
            AniUtils.animateBottomBar(mLayoutReply, true);
            if (mEditReply != null && mShouldFocusReplyField) {
                mEditReply.performClick();
                disableShouldFocusReplyField();
            }
        }

        getActivity().invalidateOptionsMenu();
    }

    /*
     * displays the passed post title for the current comment, updates stored title if one doesn't exist
     */
    private void setPostTitle(TextView txtTitle, String postTitle, boolean isHyperlink) {
        if (txtTitle == null || !isAdded()) {
            return;
        }
        if (TextUtils.isEmpty(postTitle)) {
            txtTitle.setText(R.string.untitled);
            return;
        }

        // if comment doesn't have a post title, set it to the passed one and save to comment table
        if (mComment != null && mComment.getPostTitle() == null) {
            mComment.setPostTitle(postTitle);
            mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(mComment));
        }

        // display "on [Post Title]..."
        if (isHyperlink) {
            String html = getString(R.string.on)
                          + " <font color=" + HtmlUtils.colorResToHtmlColor(getActivity(),
                    ContextExtensionsKt.getColorResIdFromAttribute(getActivity(), R.attr.colorPrimary))
                          + ">"
                          + postTitle.trim()
                          + "</font>";
            txtTitle.setText(Html.fromHtml(html));
        } else {
            String text = getString(R.string.on) + " " + postTitle.trim();
            txtTitle.setText(text);
        }
    }

    /*
     * ensure the post associated with this comment is available to the reader and show its
     * title above the comment
     */
    private void showPostTitle(final SiteModel site, final long postId) {
        if (!isAdded()) {
            return;
        }

        final TextView txtPostTitle = getView().findViewById(R.id.text_post_title);
        boolean postExists = ReaderPostTable.postExists(site.getSiteId(), postId);

        // the post this comment is on can only be requested if this is a .com blog or a
        // jetpack-enabled self-hosted blog, and we have valid .com credentials
        boolean canRequestPost = SiteUtils.isAccessedViaWPComRest(site) && mAccountStore.hasAccessToken();

        final String title;
        final boolean hasTitle;
        if (mComment.getPostTitle() != null) {
            // use comment's stored post title if available
            title = mComment.getPostTitle();
            hasTitle = true;
        } else if (postExists) {
            // use title from post if available
            title = ReaderPostTable.getPostTitle(site.getSiteId(), postId);
            hasTitle = !TextUtils.isEmpty(title);
        } else {
            title = null;
            hasTitle = false;
        }
        if (hasTitle) {
            setPostTitle(txtPostTitle, title, canRequestPost);
        } else if (canRequestPost) {
            txtPostTitle.setText(postExists ? R.string.untitled : R.string.loading);
        }

        // if this is a .com or jetpack blog, tapping the title shows the associated post
        // in the reader
        if (canRequestPost) {
            // first make sure this post is available to the reader, and once it's retrieved set
            // the title if it wasn't set above
            if (!postExists) {
                AppLog.d(T.COMMENTS, "comment detail > retrieving post");
                ReaderPostActions.requestBlogPost(site.getSiteId(), postId, new ReaderActions.OnRequestListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }

                        // update title if it wasn't set above
                        if (!hasTitle) {
                            String postTitle = ReaderPostTable.getPostTitle(site.getSiteId(), postId);
                            if (!TextUtils.isEmpty(postTitle)) {
                                setPostTitle(txtPostTitle, postTitle, true);
                            } else {
                                txtPostTitle.setText(R.string.untitled);
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode) {
                    }
                });
            }

            txtPostTitle.setOnClickListener(v -> {
                if (mOnPostClickListener != null) {
                    mOnPostClickListener.onPostClicked(getNote(), site.getSiteId(),
                            (int) mComment.getRemotePostId());
                } else {
                    // right now this will happen from notifications
                    AppLog.i(T.COMMENTS, "comment detail > no post click listener");
                    ReaderActivityLauncher.showReaderPostDetail(getActivity(), site.getSiteId(),
                            mComment.getRemotePostId());
                }
            });
        }
    }

    private void trackModerationFromNotification(final CommentStatus newStatus) {
        switch (newStatus) {
            case APPROVED:
                AnalyticsTracker.track(Stat.NOTIFICATION_APPROVED);
                break;
            case UNAPPROVED:
                AnalyticsTracker.track(Stat.NOTIFICATION_UNAPPROVED);
                break;
            case SPAM:
                AnalyticsTracker.track(Stat.NOTIFICATION_FLAGGED_AS_SPAM);
                break;
            case TRASH:
                AnalyticsTracker.track(Stat.NOTIFICATION_TRASHED);
                break;
        }
    }

    /*
     * approve, disapprove, spam, or trash the current comment
     */
    private void moderateComment(CommentStatus newStatus) {
        if (!isAdded() || mComment == null) {
            return;
        }
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        mPreviousStatus = mComment.getStatus();

        // Fire the appropriate listener if we have one
        if (mNote != null && mOnNoteCommentActionListener != null) {
            mOnNoteCommentActionListener.onModerateCommentForNote(mNote, newStatus);
            trackModerationFromNotification(newStatus);
            dispatchModerationAction(newStatus);
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener.onModerateComment(mSite, mComment, newStatus);
            // Sad, but onModerateComment does the moderation itself (due to the undo bar), this should be refactored,
            // That's why we don't call dispatchModerationAction() here.
        }

        updateStatusViews();
    }

    private void dispatchModerationAction(CommentStatus newStatus) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            mDispatcher
                    .dispatch(CommentActionBuilder.newDeleteCommentAction(new RemoteCommentPayload(mSite, mComment)));
        } else {
            // Actual moderation (push the modified comment).
            mComment.setStatus(newStatus.toString());
            mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(mSite, mComment)));
        }
    }

    /*
     * post comment box text as a reply to the current comment
     */
    private void submitReply() {
        if (mComment == null || !isAdded() || mIsSubmittingReply) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText)) {
            return;
        }

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mSubmitReplyBtn.setVisibility(View.GONE);
        final ProgressBar progress = getView().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);

        mIsSubmittingReply = true;

        AnalyticsUtils.trackCommentReplyWithDetails(false, mSite, mComment);

        // Pseudo comment reply
        CommentModel reply = new CommentModel();
        reply.setContent(replyText);

        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(new RemoteCreateCommentPayload(mSite,
                mComment,
                reply)));
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews() {
        if (!isAdded() || mComment == null) {
            return;
        }

        final int statusTextResId; // string resource id for status text
        final int statusColor; // color for status text

        CommentStatus commentStatus = CommentStatus.fromString(mComment.getStatus());
        switch (commentStatus) {
            case APPROVED:
                statusTextResId = R.string.comment_status_approved;
                statusColor = ContextExtensionsKt.getColorFromAttribute(getActivity(), R.attr.wpColorWarningDark);
                break;
            case UNAPPROVED:
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = ContextExtensionsKt.getColorFromAttribute(getActivity(), R.attr.wpColorWarningDark);
                break;
            case SPAM:
                statusTextResId = R.string.comment_status_spam;
                statusColor = ContextExtensionsKt.getColorFromAttribute(getActivity(), R.attr.colorError);
                break;
            case TRASH:
            default:
                statusTextResId = R.string.comment_status_trash;
                statusColor = ContextExtensionsKt.getColorFromAttribute(getActivity(), R.attr.colorError);
                break;
        }

        if (canLike()) {
            mBtnLikeComment.setVisibility(View.VISIBLE);
            if (mComment != null) {
                toggleLikeButton(mComment.getILike());
            } else if (mNote != null) {
                mNote.hasLikedComment();
            }
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been CommentStatus.APPROVED
        if (mIsUsersBlog && commentStatus != CommentStatus.APPROVED) {
            mTxtStatus.setText(getString(statusTextResId).toUpperCase(Locale.getDefault()));
            mTxtStatus.setTextColor(statusColor);
            if (mTxtStatus.getVisibility() != View.VISIBLE) {
                mTxtStatus.clearAnimation();
                AniUtils.fadeIn(mTxtStatus, AniUtils.Duration.LONG);
            }
        } else {
            mTxtStatus.setVisibility(View.GONE);
        }

        if (canModerate()) {
            setModerateButtonForStatus(commentStatus);
            mBtnModerateComment.setOnClickListener(v -> performModerateAction());
            mBtnModerateComment.setVisibility(View.VISIBLE);
        } else {
            mBtnModerateComment.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            mBtnSpamComment.setVisibility(View.VISIBLE);
            if (commentStatus == CommentStatus.SPAM) {
                mBtnSpamCommentText.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamCommentText.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        if (canTrash()) {
            if (commentStatus == CommentStatus.TRASH) {
                ColorUtils.INSTANCE.setImageResourceWithTint(mBtnModerateIcon, R.drawable.ic_undo_white_24dp,
                        ContextExtensionsKt
                                .getColorResIdFromAttribute(mBtnModerateTextView.getContext(), R.attr.colorOnSurface));
                mBtnModerateTextView.setText(R.string.mnu_comment_untrash);
            }
        }

        if (canShowMore()) {
            mBtnMoreComment.setVisibility(View.VISIBLE);
        } else {
            mBtnMoreComment.setVisibility(View.GONE);
        }

        mLayoutButtons.setVisibility(View.VISIBLE);
    }

    private void performModerateAction() {
        if (mComment == null || !isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        CommentStatus newStatus = CommentStatus.APPROVED;
        CommentStatus currentStatus = CommentStatus.fromString(mComment.getStatus());
        if (currentStatus == CommentStatus.APPROVED) {
            newStatus = CommentStatus.UNAPPROVED;
        }
        announceCommentStatusChangeForAccessibility(
                currentStatus == CommentStatus.TRASH ? CommentStatus.UNTRASH : newStatus);

        mComment.setStatus(newStatus.toString());
        setModerateButtonForStatus(newStatus);
        AniUtils.startAnimation(mBtnModerateIcon, R.anim.notifications_button_scale);
        moderateComment(newStatus);
    }

    private void setModerateButtonForStatus(CommentStatus status) {
        int color;

        if (status == CommentStatus.APPROVED) {
            color = ContextExtensionsKt
                    .getColorResIdFromAttribute(mBtnModerateTextView.getContext(), R.attr.colorSecondary);
            mBtnModerateTextView.setText(R.string.comment_status_approved);
            mBtnModerateTextView.setAlpha(mNormalOpacity);
            mBtnModerateIcon.setAlpha(mNormalOpacity);
        } else {
            color = ContextExtensionsKt
                    .getColorResIdFromAttribute(mBtnModerateTextView.getContext(), R.attr.colorOnSurface);
            mBtnModerateTextView.setText(R.string.mnu_comment_approve);
            mBtnModerateTextView.setAlpha(mMediumOpacity);
            mBtnModerateIcon.setAlpha(mMediumOpacity);
        }

        ColorUtils.INSTANCE.setImageResourceWithTint(mBtnModerateIcon, R.drawable.ic_checkmark_white_24dp, color);
        mBtnModerateTextView.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        return mEnabledActions != null && (mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
                                           || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE));
    }

    private boolean canMarkAsSpam() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_SPAM));
    }

    private boolean canReply() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_REPLY));
    }

    private boolean canTrash() {
        return canModerate();
    }

    private boolean canEdit() {
        return (mSite != null && canModerate());
    }

    private boolean canLike() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_LIKE)
                && mSite != null && SiteUtils.isAccessedViaWPComRest(mSite));
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
    private void showCommentAsNotification(Note note, @NonNull SiteModel site, @Nullable CommentModel comment) {
        if (getView() == null) {
            return;
        }
        View view = getView();

        // hide standard comment views, since we'll be adding note blocks instead
        View commentContent = view.findViewById(R.id.comment_content);
        if (commentContent != null) {
            commentContent.setVisibility(View.GONE);
        }

        View commentText = view.findViewById(R.id.text_content);
        if (commentText != null) {
            commentText.setVisibility(View.GONE);
        }

        /*
         * determine which actions to enable for this comment - if the comment is from this user's
         * blog then all actions will be enabled, but they won't be if it's a reply to a comment
         * this user made on someone else's blog
         */
        mEnabledActions = note.getEnabledActions();

        // Set 'Reply to (Name)' in comment reply EditText if it's a reasonable size
        if (!TextUtils.isEmpty(mNote.getCommentAuthorName()) && mNote.getCommentAuthorName().length() < 28) {
            mEditReply.setHint(String.format(getString(R.string.comment_reply_to_user), mNote.getCommentAuthorName()));
        }

        if (comment != null) {
            setComment(comment, site);
        } else {
            setComment(note.buildComment(), site);
        }

        addDetailFragment(note.getId());

        getActivity().invalidateOptionsMenu();
    }

    private void addDetailFragment(String noteId) {
        // Now we'll add a detail fragment list
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mNotificationsDetailListFragment = NotificationsDetailListFragment.newInstance(noteId);
        mNotificationsDetailListFragment.setFooterView(mLayoutButtons);
        fragmentTransaction.replace(mCommentContentLayout.getId(), mNotificationsDetailListFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    // Like or unlike a comment via the REST API
    private void likeComment(boolean forceLike) {
        if (!isAdded()) {
            return;
        }
        if (forceLike && mBtnLikeComment.isActivated()) {
            return;
        }

        toggleLikeButton(!mBtnLikeComment.isActivated());

        ReaderAnim.animateLikeButton(mBtnLikeIcon, mBtnLikeComment.isActivated());

        // Bump analytics
        AnalyticsTracker.track(mBtnLikeComment.isActivated() ? Stat.NOTIFICATION_LIKED : Stat.NOTIFICATION_UNLIKED);

        if (mNotificationsDetailListFragment != null && mComment != null) {
            // Optimistically set comment to approved when liking an unapproved comment
            // WP.com will set a comment to approved if it is liked while unapproved
            if (mBtnLikeComment.isActivated()
                && CommentStatus.fromString(mComment.getStatus()) == CommentStatus.UNAPPROVED) {
                mComment.setStatus(CommentStatus.APPROVED.toString());
                mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.APPROVED);
                setModerateButtonForStatus(CommentStatus.APPROVED);
            }
        }
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(
                new RemoteLikeCommentPayload(mSite, mComment, mBtnLikeComment.isActivated())));
        mBtnLikeComment.announceForAccessibility(getText(mBtnLikeComment.isActivated() ? R.string.comment_liked_talkback
                : R.string.comment_unliked_talkback));
    }

    private void toggleLikeButton(boolean isLiked) {
        int color;
        int drawable;

        if (isLiked) {
            color = ContextExtensionsKt.getColorResIdFromAttribute(mBtnLikeIcon.getContext(), R.attr.colorSecondary);
            drawable = R.drawable.ic_star_white_24dp;
            mBtnLikeTextView.setText(getResources().getString(R.string.mnu_comment_liked));
            mBtnLikeComment.setActivated(true);
            mBtnLikeTextView.setAlpha(mNormalOpacity);
            mBtnLikeIcon.setAlpha(mNormalOpacity);
        } else {
            color = ContextExtensionsKt.getColorResIdFromAttribute(mBtnLikeIcon.getContext(), R.attr.colorOnSurface);
            drawable = R.drawable.ic_star_outline_white_24dp;
            mBtnLikeTextView.setText(getResources().getString(R.string.reader_label_like));
            mBtnLikeComment.setActivated(false);
            mBtnLikeTextView.setAlpha(mMediumOpacity);
            mBtnLikeIcon.setAlpha(mMediumOpacity);
        }

        ColorUtils.INSTANCE.setImageResourceWithTint(mBtnLikeIcon, drawable, color);
        mBtnLikeTextView.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void setProgressVisible(boolean visible) {
        final ProgressBar progress = (isAdded() && getView() != null
                ? (ProgressBar) getView().findViewById(R.id.progress_loading) : null);
        if (progress != null) {
            progress.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void onCommentModerated(OnCommentChanged event) {
        // send signal for listeners to perform any needed updates
        if (mNote != null) {
            EventBus.getDefault().postSticky(new NotificationEvents.NoteLikeOrModerationStatusChanged(mNote.getId()));
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            mComment.setStatus(mPreviousStatus);
            updateStatusViews();
            ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
        } else {
            reloadComment();
        }
    }

    private void onCommentCreated(OnCommentChanged event) {
        mIsSubmittingReply = false;
        mEditReply.setEnabled(true);
        mSubmitReplyBtn.setVisibility(View.VISIBLE);
        getView().findViewById(R.id.progress_submit_comment).setVisibility(View.GONE);
        updateStatusViews();

        if (event.isError()) {
            if (isAdded()) {
                String strUnEscapeHTML = StringEscapeUtils.unescapeHtml4(event.error.message);
                ToastUtils.showToast(getActivity(), strUnEscapeHTML, ToastUtils.Duration.LONG);
                // refocus editor on failure and show soft keyboard
                EditTextUtils.showSoftInput(mEditReply);
            }
            return;
        }

        reloadComment();

        if (isAdded()) {
            ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
            mEditReply.setText(null);
            mEditReply.getAutoSaveTextHelper().clearSavedText(mEditReply);
        }

        // approve the comment
        if (mComment != null && !(CommentStatus.fromString(mComment.getStatus()) == CommentStatus.APPROVED)) {
            moderateComment(CommentStatus.APPROVED);
        }
    }

    private void onCommentLiked(OnCommentChanged event) {
        // send signal for listeners to perform any needed updates
        if (mNote != null) {
            EventBus.getDefault().postSticky(new NotificationEvents.NoteLikeOrModerationStatusChanged(mNote.getId()));
        }

        if (event.isError()) {
            // Revert button state in case of an error
            toggleLikeButton(!mBtnLikeComment.isActivated());
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        setProgressVisible(false);

        // Moderating comment
        if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
            onCommentModerated(event);
            mPreviousStatus = null;
            return;
        }

        // New comment (reply)
        if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
            onCommentCreated(event);
            return;
        }

        // Like/Unlike
        if (event.causeOfChange == CommentAction.LIKE_COMMENT) {
            onCommentLiked(event);
            return;
        }

        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            if (isAdded() && !TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
            return;
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
    private void showMoreMenu(View view) {
        androidx.appcompat.widget.PopupMenu morePopupMenu =
                new androidx.appcompat.widget.PopupMenu(requireContext(), view);
        morePopupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                editComment();
                return true;
            }
            if (item.getItemId() == R.id.action_trash) {
                trashComment();
                return true;
            }
            if (item.getItemId() == R.id.action_copy_link_address) {
                copyCommentLinkAddress();
                return true;
            }
            return false;
        });

        morePopupMenu.inflate(R.menu.menu_comment_more);

        MenuItem trashMenuItem = morePopupMenu.getMenu().findItem(R.id.action_trash);
        MenuItem copyLinkAddress = morePopupMenu.getMenu().findItem(R.id.action_copy_link_address);
        if (canTrash()) {
            CommentStatus commentStatus = CommentStatus.fromString(mComment.getStatus());
            if (commentStatus == CommentStatus.TRASH) {
                copyLinkAddress.setVisible(false);
                trashMenuItem.setTitle(R.string.mnu_comment_delete_permanently);
            } else {
                trashMenuItem.setTitle(R.string.mnu_comment_trash);
                if (commentStatus == CommentStatus.SPAM) {
                    copyLinkAddress.setVisible(false);
                } else {
                    copyLinkAddress.setVisible(true);
                }
            }
        } else {
            trashMenuItem.setVisible(false);
            copyLinkAddress.setVisible(false);
        }

        MenuItem editMenuItem = morePopupMenu.getMenu().findItem(R.id.action_edit);
        editMenuItem.setVisible(false);
        if (canEdit()) {
            editMenuItem.setVisible(true);
        }
        morePopupMenu.show();
    }

    private void trashComment() {
        if (!isAdded() || mComment == null) {
            return;
        }

        CommentStatus status = CommentStatus.fromString(mComment.getStatus());
        // If the comment status is trash or spam, next deletion is a permanent deletion.
        if (status == CommentStatus.TRASH || status == CommentStatus.SPAM) {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.delete));
            dialogBuilder.setMessage(getResources().getText(R.string.dlg_sure_to_delete_comment));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    (dialog, whichButton) -> {
                        moderateComment(CommentStatus.DELETED);
                        announceCommentStatusChangeForAccessibility(CommentStatus.DELETED);
                    });
            dialogBuilder.setNegativeButton(
                    getResources().getText(R.string.no),
                    null);
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            moderateComment(CommentStatus.TRASH);
            announceCommentStatusChangeForAccessibility(CommentStatus.TRASH);
        }
    }

    private void copyCommentLinkAddress() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("CommentLinkAddress", mComment.getUrl()));
            showSnackBar(getString(R.string.comment_q_action_copied_url));
        } catch (Exception e) {
            AppLog.e(T.UTILS, e);
            showSnackBar(getString(R.string.error_copy_to_clipboard));
        }
    }

    private void showSnackBar(String message) {
        WPSnackbar snackBar = WPSnackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                                        .setAction(getString(R.string.share_action),
                                                v -> {
                                                    try {
                                                        Intent intent = new Intent(Intent.ACTION_SEND);
                                                        intent.setType("text/plain");
                                                        intent.putExtra(Intent.EXTRA_TEXT, mComment.getUrl());
                                                        startActivity(Intent.createChooser(intent,
                                                                getString(R.string.comment_share_link_via)));
                                                    } catch (ActivityNotFoundException exception) {
                                                        ToastUtils.showToast(getContext(),
                                                                R.string.comment_toast_err_share_intent);
                                                    }
                                                })
                                        .setAnchorView(mSnackbarAnchor);
        snackBar.show();
    }
}
