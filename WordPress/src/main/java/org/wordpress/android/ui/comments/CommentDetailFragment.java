package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.simperium.client.BucketObjectMissingException;
import com.wordpress.rest.RestRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.comments.CommentActions.ChangeType;
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener;
import org.wordpress.android.ui.comments.CommentActions.OnCommentChangeListener;
import org.wordpress.android.ui.comments.CommentActions.OnNoteCommentActionListener;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
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
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.wordpress.android.R.string.comments;
import static org.wordpress.android.fluxc.model.CommentStatus.APPROVED;
import static org.wordpress.android.fluxc.model.CommentStatus.SPAM;
import static org.wordpress.android.fluxc.model.CommentStatus.TRASH;
import static org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED;
import static org.wordpress.android.fluxc.persistence.CommentSqlUtils.getCommentBySiteAndRemoteId;

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private static final String KEY_MODE = "KEY_MODE";
    private static final String KEY_COMMENT = "KEY_COMMENT";
    private static final String KEY_NOTE_ID = "KEY_NOTE_ID";

    private static final int INTENT_COMMENT_EDITOR     = 1010;
    private static final int FROM_BLOG_COMMENT = 1;
    private static final int FROM_NOTE = 2;
    private static final int FROM_NOTE_REMOTE = 3;

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
    private View mBtnLikeComment;
    private ImageView mBtnLikeIcon;
    private TextView mBtnLikeTextView;
    private View mBtnModerateComment;
    private ImageView mBtnModerateIcon;
    private TextView mBtnModerateTextView;
    private TextView mBtnSpamComment;
    private TextView mBtnTrashComment;
    private String mRestoredReplyText;
    private String mRestoredNoteId;
    private boolean mIsUsersBlog = false;
    private boolean mShouldFocusReplyField;
    private boolean mShouldLikeInstantly;
    private boolean mShouldApproveInstantly;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject CommentStore mCommentStore;
    @Inject SiteStore mSiteStore;

    /*
     * Used to request a comment from a note using its site and comment ids, rather than build
     * the comment with the content in the note. See showComment()
     */
    private boolean mShouldRequestCommentFromNote = false;
    private boolean mIsSubmittingReply = false;
    private NotificationsDetailListFragment mNotificationsDetailListFragment;
    private OnCommentChangeListener mOnCommentChangeListener;
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
    static CommentDetailFragment newInstance(SiteModel site, CommentModel comment) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MODE, FROM_BLOG_COMMENT);
        args.putSerializable(WordPress.SITE, site);
        args.putSerializable(KEY_COMMENT, comment);
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
        fragment.setArguments(args);
        fragment.setNoteWithNoteId(noteId);
        fragment.setReplyText(replyText);
        return fragment;
    }

    /*
     * used when called from a comment notification 'like' action
     */
    public static CommentDetailFragment newInstanceForInstantLike(final String noteId) {
        CommentDetailFragment fragment = newInstance(noteId, null);
        //here tell the fragment to trigger the Like action when ready
        fragment.setLikeCommentWhenReady();
        return fragment;
    }

    /*
     * used when called from a comment notification 'approve' action
     */
    public static CommentDetailFragment newInstanceForInstantApprove(final String noteId) {
        CommentDetailFragment fragment = newInstance(noteId, null);
        //here tell the fragment to trigger the Like action when ready
        fragment.setApproveCommentWhenReady();
        return fragment;
    }

    /*
     * used when called from notifications to load a comment that doesn't already exist in the note
     */
    public static CommentDetailFragment newInstanceForRemoteNoteComment(final String noteId) {
        CommentDetailFragment fragment = newInstance(noteId, null);
        Bundle args = new Bundle();
        args.putInt(KEY_MODE, FROM_NOTE_REMOTE);
        args.putString(KEY_NOTE_ID, noteId);
        fragment.setArguments(args);
        fragment.enableShouldRequestCommentFromNote();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        switch (getArguments().getInt(KEY_MODE)) {
            case FROM_BLOG_COMMENT:
                setComment((CommentModel) getArguments().getSerializable(KEY_COMMENT),
                        (SiteModel) getArguments().getSerializable(WordPress.SITE));
                break;
            case FROM_NOTE:
                setNoteWithNoteId(getArguments().getString(KEY_NOTE_ID));
                break;
            default:
            case FROM_NOTE_REMOTE:
                setNoteWithNoteId(getArguments().getString(KEY_NOTE_ID));
                enableShouldRequestCommentFromNote();
                break;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getString(KEY_NOTE_ID) != null) {
                // The note will be set in onResume() because Simperium will be running there
                // See WordPress.deferredInit()
                mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID);
            } else {
                SiteModel site = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
                CommentModel comment = (CommentModel) savedInstanceState.getSerializable(KEY_COMMENT);
                setComment(comment, site);
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (hasComment()) {
            outState.putSerializable(WordPress.SITE, mSite);
            outState.putSerializable(KEY_COMMENT, mComment);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);

        mTxtStatus = (TextView) view.findViewById(R.id.text_status);
        mTxtContent = (TextView) view.findViewById(R.id.text_content);

        mLayoutButtons = (ViewGroup) inflater.inflate(R.layout.comment_action_footer, null, false);
        mBtnLikeComment = mLayoutButtons.findViewById(R.id.btn_like);
        mBtnLikeIcon = (ImageView) mLayoutButtons.findViewById(R.id.btn_like_icon);
        mBtnLikeTextView = (TextView) mLayoutButtons.findViewById(R.id.btn_like_text);
        mBtnModerateComment = mLayoutButtons.findViewById(R.id.btn_moderate);
        mBtnModerateIcon = (ImageView) mLayoutButtons.findViewById(R.id.btn_moderate_icon);
        mBtnModerateTextView = (TextView) mLayoutButtons.findViewById(R.id.btn_moderate_text);
        mBtnSpamComment = (TextView) mLayoutButtons.findViewById(R.id.text_btn_spam);
        mBtnTrashComment = (TextView) mLayoutButtons.findViewById(R.id.image_trash_comment);

        setTextDrawable(mBtnSpamComment, R.drawable.ic_action_spam);
        setTextDrawable(mBtnTrashComment, R.drawable.ic_action_trash);

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (SuggestionAutoCompleteText) mLayoutReply.findViewById(R.id.edit_comment);
        mEditReply.getAutoSaveTextHelper().setUniqueId(String.format(Locale.US, "%d%d", mSite.getSiteId(),
                mComment.getRemoteCommentId()));

        mSubmitReplyBtn = mLayoutReply.findViewById(R.id.btn_submit_reply);

        // hide comment like button until we know it can be enabled in showCommentForNote()
        mBtnLikeComment.setVisibility(View.GONE);

        // hide moderation buttons until updateModerationButtons() is called
        mLayoutButtons.setVisibility(View.GONE);

        // this is necessary in order for anchor tags in the comment text to be clickable
        mTxtContent.setLinksClickable(true);
        mTxtContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mEditReply.setHint(R.string.reader_hint_comment_on_comment);
        mEditReply.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)
                    submitReply();
                return false;
            }
        });

        if (!TextUtils.isEmpty(mRestoredReplyText)) {
            mEditReply.setText(mRestoredReplyText);
            mRestoredReplyText = null;
        }

        mSubmitReplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReply();
            }
        });

        mBtnSpamComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasComment()) return;

                if (CommentStatus.fromString(mComment.getStatus()) == SPAM) {
                    moderateComment(APPROVED);
                } else {
                    moderateComment(SPAM);
                }
            }
        });

        mBtnTrashComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasComment()) return;

                String status = mComment.getStatus();

                // If the comment status is trash or spam, next deletion is a permanent deletion.
                if (CommentStatus.TRASH.equals(status) || SPAM.equals(status)) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                    dialogBuilder.setTitle(getResources().getText(R.string.delete));
                    dialogBuilder.setMessage(getResources().getText(R.string.dlg_sure_to_delete_comment));
                    dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    moderateComment(CommentStatus.DELETED);
                                }
                            });
                    dialogBuilder.setNegativeButton(
                            getResources().getText(R.string.no),
                            null);
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();

                } else {
                    moderateComment(TRASH);
                }

            }
        });

        mBtnLikeComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeComment(false);
            }
        });

        setupSuggestionServiceAndAdapter();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.COMMENT_DETAIL);

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(mRestoredNoteId)) {
            setNoteWithNoteId(mRestoredNoteId);
            mRestoredNoteId = null;
        }

        if (mShouldLikeInstantly) {
            mShouldLikeInstantly = false;
            likeComment(true);
        } else if (mShouldApproveInstantly) {
            mShouldApproveInstantly = false;
            performModerateAction();
        }

    }

    private void setupSuggestionServiceAndAdapter() {
        if (!isAdded() || mSiteStore.hasWPComOrJetpackSiteWithSiteId(mSite.getSiteId())) {
            return;
        }
        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), mSite.getSiteId());
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mSiteStore.getSiteBySiteId(mSite.getSiteId()), getActivity(),
                mSuggestionServiceConnectionManager);
        if (mSuggestionAdapter != null) {
            mEditReply.setAdapter(mSuggestionAdapter);
        }
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
    }

    private void disableShouldFocusReplyField() {
        mShouldFocusReplyField = false;
    }

    public void enableShouldFocusReplyField() {
        mShouldFocusReplyField = true;
    }

    private void enableShouldRequestCommentFromNote() {
        mShouldRequestCommentFromNote = true;
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(Note note) {
        mNote = note;
        if (isAdded() && mNote != null) {
            showComment();
        }
    }

    private void setNoteWithNoteId(String noteId) {
        if (noteId == null) return;

        if (SimperiumUtils.getNotesBucket() != null) {
            try {
                Note note = SimperiumUtils.getNotesBucket().get(noteId);
                setNote(note);
                mSite = mSiteStore.getSiteBySiteId(note.getSiteId());
            } catch (BucketObjectMissingException e) {
                AppLog.e(T.NOTIFS, e.getMessage());
                SimperiumUtils.trackBucketObjectMissingWarning(e.getMessage(), noteId);
                showErrorToastAndFinish();
            }
        }
    }

    private void setReplyText(String replyText) {
        if (replyText == null) return;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnCommentChangeListener)
            mOnCommentChangeListener = (OnCommentChangeListener) activity;
        if (activity instanceof OnPostClickListener)
            mOnPostClickListener = (OnPostClickListener) activity;
        if (activity instanceof OnCommentActionListener)
            mOnCommentActionListener = (OnCommentActionListener) activity;
        if (activity instanceof OnNoteCommentActionListener)
            mOnNoteCommentActionListener = (OnNoteCommentActionListener) activity;
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
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mSite.getSiteId() && mSuggestionAdapter != null) {
            List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Reset comment if this is from a notification
        if (mNote != null) {
            mComment = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_COMMENT_EDITOR && resultCode == Activity.RESULT_OK) {
            if (mNote == null) {
                reloadComment();
            }
            // tell the host to reload the comment list
            if (mOnCommentChangeListener != null)
                mOnCommentChangeListener.onCommentChanged(ChangeType.EDITED);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.comment_detail, menu);
        if (!canEdit()) {
            menu.removeItem(R.id.menu_edit_comment);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_edit_comment) {
            editComment();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean hasComment() {
        return mComment == null;
    }

    /**
     * Reload the current comment from the local database
     */
    private void reloadComment() {
        // TODO: make sure this method is useful
        if (!hasComment()) {
            return;
        }
        CommentModel updatedComment = mCommentStore.getCommentByLocalId(mComment.getLocalSiteId());
        setComment(updatedComment, mSite);
    }

    /**
     * open the comment for editing
     */
    private void editComment() {
        if (!isAdded() || !hasComment())
            return;
        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        Intent intent = new Intent(getActivity(), EditCommentActivity.class);
        intent.putExtra(WordPress.SITE, mSite);
        intent.putExtra(KEY_COMMENT, mComment);
        if (mNote != null) {
            intent.putExtra(EditCommentActivity.ARG_NOTE_ID, mNote.getId());
        }
        startActivityForResult(intent, INTENT_COMMENT_EDITOR);
    }

    /*
     * display the current comment
     */
    private void showComment() {
        if (!isAdded() || getView() == null)
            return;

        // these two views contain all the other views except the progress bar
        final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll_view);
        final View layoutBottom = getView().findViewById(R.id.layout_bottom);

        // hide container views when comment is null (will happen when opened from a notification)
        if (!hasComment()) {
            scrollView.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.GONE);

            if (mNote != null && mShouldRequestCommentFromNote) {
                // If a remote comment was requested, check if we have the comment for display.
                // Otherwise request the comment via the REST API
                SiteModel site = mSiteStore.getSiteBySiteId(mNote.getSiteId());
                if (site != null) {
                    CommentModel comment = mCommentStore.getCommentBySiteAndRemoteId(site, mNote.getParentCommentId());
                    if (comment != null) {
                        setComment(comment, site);
                        return;
                    }
                    long commentId = mNote.getParentCommentId() > 0 ? mNote.getParentCommentId() : mNote.getCommentId();
                    // FIXME:
                    RemoteCommentPayload payload = new RemoteCommentPayload(site, commentId);
                    mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
                    setProgressVisible(true);
                    return;
                }
            }
            showCommentForNote(mNote);
            return;
        }

        scrollView.setVisibility(View.VISIBLE);
        layoutBottom.setVisibility(View.VISIBLE);

        // Add action buttons footer
        if ((mNote == null || mShouldRequestCommentFromNote) && mLayoutButtons.getParent() == null) {
            ViewGroup commentContentLayout = (ViewGroup) getView().findViewById(R.id.comment_content_container);
            commentContentLayout.addView(mLayoutButtons);
        }

        final WPNetworkImageView imgAvatar = (WPNetworkImageView) getView().findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) getView().findViewById(R.id.text_name);
        final TextView txtDate = (TextView) getView().findViewById(R.id.text_date);


        txtName.setText(mComment.getAuthorName() == null ? getString(R.string.anonymous) :
                HtmlUtils.fastUnescapeHtml(mComment.getAuthorName()));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(mComment.getDatePublished()),
                WordPress.getContext()));

        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(mTxtContent, mComment.getContent(), maxImageSz);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        if (mComment.getAuthorProfileImageUrl() != null) {
            imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(mComment.getAuthorProfileImageUrl(), avatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
        } else if (mComment.getAuthorEmail() != null) {
            String avatarUrl = GravatarUtils.gravatarFromEmail(mComment.getAuthorEmail(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            imgAvatar.setImageUrl(null, WPNetworkImageView.ImageType.AVATAR);
        }

        updateStatusViews();

        // navigate to author's blog when avatar or name clicked
        if (mComment.getAuthorUrl() != null) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getActivity(), mComment.getAuthorUrl(), mAccountStore.getAccount()
                            .getUserName());
                }
            };
            imgAvatar.setOnClickListener(authorListener);
            txtName.setOnClickListener(authorListener);
            txtName.setTextColor(ContextCompat.getColor(getActivity(), R.color.reader_hyperlink));
        } else {
            txtName.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey_darken_30));
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

        getFragmentManager().invalidateOptionsMenu();
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
        if (hasComment() && mComment.getPostTitle() == null) {
            mComment.setPostTitle(postTitle);
            mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(mComment));
        }

        // display "on [Post Title]..."
        if (isHyperlink) {
            String html = getString(R.string.on)
                    + " <font color=" + HtmlUtils.colorResToHtmlColor(getActivity(), R.color.reader_hyperlink) + ">"
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

        final TextView txtPostTitle = (TextView) getView().findViewById(R.id.text_post_title);
        boolean postExists = ReaderPostTable.postExists(site.getSiteId(), postId);

        // the post this comment is on can only be requested if this is a .com blog or a
        // jetpack-enabled self-hosted blog, and we have valid .com credentials
        boolean isDotComOrJetpack = site.isWPCom();
        boolean canRequestPost = isDotComOrJetpack && mAccountStore.hasAccessToken();

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
                        if (!isAdded()) return;

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

            txtPostTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnPostClickListener != null) {
                        mOnPostClickListener.onPostClicked(getNote(), site.getSiteId(),
                                (int) mComment.getRemotePostId());
                    } else {
                        // right now this will happen from notifications
                        AppLog.i(T.COMMENTS, "comment detail > no post click listener");
                        ReaderActivityLauncher.showReaderPostDetail(getActivity(), site.getSiteId(),
                                mComment.getRemotePostId());
                    }
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
    private void moderateComment(final CommentStatus newStatus) {
        if (!isAdded() || !hasComment())
            return;
        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        // Fire the appropriate listener if we have one
        if (mNote != null && mOnNoteCommentActionListener != null) {
            mOnNoteCommentActionListener.onModerateCommentForNote(mNote, newStatus);
            trackModerationFromNotification(newStatus);
            return;
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener.onModerateComment(mSite, mComment, newStatus);
            return;
        }

        if (mNote == null) return;

        // Basic moderation support, currently only used when this Fragment is in a CommentDetailActivity
        // Uses WP.com REST API and requires a note object
        final CommentStatus oldStatus = mComment.getStatusEnum();
        mComment.setStatus(CommentStatus.toString(newStatus));
        updateStatusViews();
        CommentActions.moderateCommentRestApi(mNote.getSiteId(), mComment.commentID, newStatus, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(CommentActionResult result) {
                if (!isAdded()) return;

                if (result.isSuccess()) {
                    if (newStatus.equals(APPROVED)) {
                        ToastUtils.showToast(getActivity(), R.string.comment_moderated_approved, ToastUtils.Duration.SHORT);
                    } else if (newStatus.equals(UNAPPROVED)) {
                        ToastUtils.showToast(getActivity(), R.string.comment_moderated_unapproved, ToastUtils.Duration.SHORT);
                    }
                } else {
                    mComment.setStatus(CommentStatus.toString(oldStatus));
                    updateStatusViews();
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        });
    }

    /*
     * post comment box text as a reply to the current comment
     */
    private void submitReply() {
        if (!hasComment() || !isAdded() || mIsSubmittingReply)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mSubmitReplyBtn.setVisibility(View.GONE);
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(CommentActionResult result) {
                mIsSubmittingReply = false;
                if (result.isSuccess() && mOnCommentChangeListener != null)
                    mOnCommentChangeListener.onCommentChanged(ChangeType.REPLIED);
                if (isAdded()) {
                    mEditReply.setEnabled(true);
                    mSubmitReplyBtn.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    updateStatusViews();
                    if (result.isSuccess()) {
                        ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
                        mEditReply.setText(null);
                        mEditReply.getAutoSaveTextHelper().clearSavedText(mEditReply);

                        // approve the comment
                        if (mComment != null && mComment.getStatusEnum() != APPROVED) {
                            moderateComment(APPROVED);
                        }
                    } else {
                        String errorMessage = TextUtils.isEmpty(result.getMessage()) ? getString(R.string.reply_failed) : result.getMessage();
                        String strUnEscapeHTML = StringEscapeUtils.unescapeHtml(errorMessage);
                        ToastUtils.showToast(getActivity(), strUnEscapeHTML, ToastUtils.Duration.LONG);
                        // refocus editor on failure and show soft keyboard
                        EditTextUtils.showSoftInput(mEditReply);
                    }
                }
            }
        };

        mIsSubmittingReply = true;

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO);
        if (mNote != null) {
            if (mShouldRequestCommentFromNote) {
                CommentActions.submitReplyToCommentRestApi(mNote.getSiteId(), mComment.commentID, replyText, actionListener);
            } else {
                CommentActions.submitReplyToCommentNote(mNote, replyText, actionListener);
            }
        } else {
            SiteModel site = mSiteStore.getSiteByLocalId(mLocalBlogId);
            CommentActions.submitReplyToComment(site, mComment, replyText, actionListener);
        }
    }

    /*
     * sets the drawable for moderation buttons
     */
    private void setTextDrawable(final TextView view, int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getActivity(), resId), null, null);
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews() {
        if (!isAdded() || !hasComment())
            return;

        final int statusTextResId;      // string resource id for status text
        final int statusColor;          // color for status text

        CommentStatus commentStatus = CommentStatus.valueOf(mComment.getStatus());
        switch (commentStatus) {
            case APPROVED:
                statusTextResId = R.string.comment_status_approved;
                statusColor = ContextCompat.getColor(getActivity(), R.color.notification_status_unapproved_dark);
                break;
            case UNAPPROVED:
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = ContextCompat.getColor(getActivity(), R.color.notification_status_unapproved_dark);
                break;
            case SPAM:
                statusTextResId = R.string.comment_status_spam;
                statusColor = ContextCompat.getColor(getActivity(), R.color.comment_status_spam);
                break;
            case TRASH:
            default:
                statusTextResId = R.string.comment_status_trash;
                statusColor = ContextCompat.getColor(getActivity(), R.color.comment_status_spam);
                break;
        }

        if (mNote != null && canLike()) {
            mBtnLikeComment.setVisibility(View.VISIBLE);

            toggleLikeButton(mNote.hasLikedComment());
        } else {
            mBtnLikeComment.setVisibility(View.GONE);
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been approved
        if (mIsUsersBlog && commentStatus != APPROVED) {
            mTxtStatus.setText(getString(statusTextResId).toUpperCase());
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
            mBtnModerateComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performModerateAction();
                }
            });
            mBtnModerateComment.setVisibility(View.VISIBLE);
        } else {
            mBtnModerateComment.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            mBtnSpamComment.setVisibility(View.VISIBLE);
            if (commentStatus == SPAM) {
                mBtnSpamComment.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamComment.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        if (canTrash()) {
            mBtnTrashComment.setVisibility(View.VISIBLE);
            if (commentStatus == TRASH) {
                mBtnModerateIcon.setImageResource(R.drawable.ic_action_restore);
                //mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.notification_status_unapproved_dark));
                mBtnModerateTextView.setText(R.string.mnu_comment_untrash);
                mBtnTrashComment.setText(R.string.mnu_comment_delete_permanently);
            } else {
                mBtnTrashComment.setText(R.string.mnu_comment_trash);
            }
        } else {
            mBtnTrashComment.setVisibility(View.GONE);
        }

        mLayoutButtons.setVisibility(View.VISIBLE);
    }

    private void performModerateAction(){
        if (!hasComment() || !isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        CommentStatus newStatus = APPROVED;
        if (mComment.getStatusEnum() == APPROVED) {
            newStatus = UNAPPROVED;
        }

        mComment.setStatus(newStatus.toString());
        setModerateButtonForStatus(newStatus);
        AniUtils.startAnimation(mBtnModerateIcon, R.anim.notifications_button_scale);
        moderateComment(newStatus);
    }

    private void setModerateButtonForStatus(CommentStatus status) {
        if (status == APPROVED) {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve_active);
            mBtnModerateTextView.setText(R.string.comment_status_approved);
            mBtnModerateTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.notification_status_unapproved_dark));
        } else {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve);
            mBtnModerateTextView.setText(R.string.mnu_comment_approve);
            mBtnModerateTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey));
        }
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        return mEnabledActions != null && (mEnabledActions.contains(EnabledActions.ACTION_APPROVE) || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE));
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
        return (mLocalBlogId > 0 && canModerate());
    }

    private boolean canLike() {
        return (!mShouldRequestCommentFromNote && mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_LIKE));
    }

    /*
     * display the comment associated with the passed notification
     */
    private void showCommentForNote(Note note) {
        if (getView() == null) return;
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

        // Now we'll add a detail fragment list
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mNotificationsDetailListFragment = NotificationsDetailListFragment.newInstance(note.getId());
        mNotificationsDetailListFragment.setFooterView(mLayoutButtons);
        // Listen for note changes from the detail list fragment, so we can update the status buttons
        mNotificationsDetailListFragment.setOnNoteChangeListener(new NotificationsDetailListFragment.OnNoteChangeListener() {
            @Override
            public void onNoteChanged(Note note) {
                mNote = note;
                mComment = mNote.buildComment();
                updateStatusViews();
            }
        });
        fragmentTransaction.replace(R.id.comment_content_container, mNotificationsDetailListFragment);
        fragmentTransaction.commitAllowingStateLoss();

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

        // note that the site won't be found if the comment is from someone else's blog
        SiteModel site = mSiteStore.getSiteBySiteId(mRemoteBlogId);

        // adjust enabledActions if this is a Jetpack site
        if (canLike() && site != null && site.isJetpack()) {
            // delete LIKE action from enabledActions for Jetpack sites
            mEnabledActions.remove(EnabledActions.ACTION_LIKE);
        }

        if (site != null) {
            setComment(site, note.buildComment());
        }
        getFragmentManager().invalidateOptionsMenu();
    }

    private void setLikeCommentWhenReady() {
        mShouldLikeInstantly = true;
    }

    private void setApproveCommentWhenReady() {
        mShouldApproveInstantly = true;
    }

    // Like or unlike a comment via the REST API
    private void likeComment(boolean forceLike) {
        if (mNote == null) return;
        if (!isAdded()) return;
        if (forceLike && mBtnLikeComment.isActivated()) return;

        toggleLikeButton(!mBtnLikeComment.isActivated());

        ReaderAnim.animateLikeButton(mBtnLikeIcon, mBtnLikeComment.isActivated());

        // Bump analytics
        AnalyticsTracker.track(mBtnLikeComment.isActivated() ? Stat.NOTIFICATION_LIKED : Stat.NOTIFICATION_UNLIKED);

        boolean commentWasUnapproved = false;
        if (mNotificationsDetailListFragment != null && mComment != null) {
            // Optimistically set comment to approved when liking an unapproved comment
            // WP.com will set a comment to approved if it is liked while unapproved
            if (mBtnLikeComment.isActivated() && mComment.getStatusEnum() == UNAPPROVED) {
                mComment.setStatus(CommentStatus.toString(APPROVED));
                mNotificationsDetailListFragment.refreshBlocksForCommentStatus(APPROVED);
                setModerateButtonForStatus(APPROVED);
                commentWasUnapproved = true;
            }
        }

        final boolean commentStatusShouldRevert = commentWasUnapproved;
        WordPress.getRestClientUtils().likeComment(mNote.getSiteId(),
                String.valueOf(mNote.getCommentId()),
                mBtnLikeComment.isActivated(),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null && !response.optBoolean("success")) {
                            if (!isAdded()) return;

                            // Failed, so switch the button state back
                            toggleLikeButton(!mBtnLikeComment.isActivated());

                            if (commentStatusShouldRevert) {
                                setCommentStatusUnapproved();
                            }
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (!isAdded()) return;

                        toggleLikeButton(!mBtnLikeComment.isActivated());

                        if (commentStatusShouldRevert) {
                            setCommentStatusUnapproved();
                        }
                    }
                });
    }

    private void setCommentStatusUnapproved() {
        mComment.setStatus(CommentStatus.toString(UNAPPROVED));
        mNotificationsDetailListFragment.refreshBlocksForCommentStatus(UNAPPROVED);
        setModerateButtonForStatus(UNAPPROVED);
    }

    private void toggleLikeButton(boolean isLiked) {
        if (isLiked) {
            mBtnLikeTextView.setText(getResources().getString(R.string.mnu_comment_liked));
            mBtnLikeTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.orange_jazzy));
            mBtnLikeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_like_active));
            mBtnLikeComment.setActivated(true);
        } else {
            mBtnLikeTextView.setText(getResources().getString(R.string.reader_label_like));
            mBtnLikeTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey));
            mBtnLikeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_like));
            mBtnLikeComment.setActivated(false);
        }
    }

    private void setProgressVisible(boolean visible) {
        final ProgressBar progress = (isAdded() && getView() != null ?
                (ProgressBar) getView().findViewById(R.id.progress_loading) : null);
        if (progress != null) {
            progress.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    // OnChanged events

    @Subscribe
    public void onCommentChanged(CommentStore.OnCommentChanged event) {
        setProgressVisible(false);
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            if (isAdded()) {
                ToastUtils.showToast(getActivity(), event.error.message);
            }
            return;
        }
        // FIXME:                         setComment(localBlogId, comment);
    }
}
