package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.comments.CommentActions.ChangeType;
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener;
import org.wordpress.android.ui.comments.CommentActions.OnCommentChangeListener;
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

import de.greenrobot.event.EventBus;

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private static final String KEY_REMOTE_BLOG_ID = "remote_blog_id";
    private static final String KEY_LOCAL_BLOG_ID = "local_blog_id";
    private static final String KEY_COMMENT_ID = "comment_id";
    private static final String KEY_NOTE_ID = "note_id";
    private static final String KEY_FRAGMENT_CONTAINER_ID = "fragment_container_id";
    private int mIdForFragmentContainer;
    private int mLocalBlogId;
    private int mRemoteBlogId;
    private Comment mComment;
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
    private View mBtnEditComment;
    private ImageView mBtnModerateIcon;
    private TextView mBtnModerateTextView;
    private View mBtnSpamComment;
    private TextView mBtnSpamCommentText;
    private View mBtnTrashComment;
    private TextView mBtnTrashCommentText;
    private String mRestoredReplyText;
    private String mRestoredNoteId;
    private boolean mIsUsersBlog = false;
    private boolean mShouldFocusReplyField;

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
    static CommentDetailFragment newInstance(int localBlogId, long commentId) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(localBlogId, commentId);
        return fragment;
    }

    /*
     * used when called from notification list for a comment notification
     */
    public static CommentDetailFragment newInstance(final String noteId, final String replyText, final int idForFragmentContainer) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setNote(noteId);
        fragment.setReplyText(replyText);
        fragment.setIdForFragmentContainer(idForFragmentContainer + R.id.note_comment_fragment_container_base_id);
        return fragment;
    }

    /*
     * used when called from notifications to load a comment that doesn't already exist in the note
     */
    public static CommentDetailFragment newInstanceForRemoteNoteComment(final String noteId) {
        CommentDetailFragment fragment = newInstance(noteId, null, 0);
        fragment.enableShouldRequestCommentFromNote();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getString(KEY_NOTE_ID) != null) {
                // The note will be set in onResume()
                // See WordPress.deferredInit()
                mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID);
                mIdForFragmentContainer = savedInstanceState.getInt(KEY_FRAGMENT_CONTAINER_ID);
            } else {
                int localBlogId = savedInstanceState.getInt(KEY_LOCAL_BLOG_ID);
                long commentId = savedInstanceState.getLong(KEY_COMMENT_ID);
                setComment(localBlogId, commentId);
            }

            mRemoteBlogId = savedInstanceState.getInt(KEY_REMOTE_BLOG_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (hasComment()) {
            outState.putInt(KEY_LOCAL_BLOG_ID, getLocalBlogId());
            outState.putLong(KEY_COMMENT_ID, getCommentId());
        }

        if (mNote != null) {
            outState.putString(KEY_NOTE_ID, mNote.getId());
        }

        outState.putInt(KEY_REMOTE_BLOG_ID, mRemoteBlogId);
        outState.putInt(KEY_FRAGMENT_CONTAINER_ID, mIdForFragmentContainer);

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
        mBtnEditComment = mLayoutButtons.findViewById(R.id.btn_edit);
        mBtnSpamComment = mLayoutButtons.findViewById(R.id.btn_spam);
        mBtnSpamCommentText = (TextView) mLayoutButtons.findViewById(R.id.btn_spam_text);
        mBtnTrashComment = mLayoutButtons.findViewById(R.id.btn_trash);
        mBtnTrashCommentText = (TextView) mLayoutButtons.findViewById(R.id.btn_trash_text);

        //as we are using CommentDetailFragment in a ViewPager, and we also use nested fragments within
        //CommentDetailFragment itself:
        //it is important to have a live reference to the Comment Container layout at the moment this
        //layout is inflated (onCreateView), so we can make sure we set its ID correctly once we have an actual Comment
        //object to populate it with. Otherwise, we could be searching and finding the container for _another fragment/page
        //in the viewpager_, which would cause strange results (changing the views for a different fragment than we intended to).
        mCommentContentLayout = (ViewGroup) view.findViewById(R.id.comment_content_container);

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (SuggestionAutoCompleteText) mLayoutReply.findViewById(R.id.edit_comment);
        setReplyUniqueId();

        mSubmitReplyBtn = mLayoutReply.findViewById(R.id.btn_submit_reply);

        View replyBox = mLayoutReply.findViewById(R.id.reply_box);
        if (mComment != null &&
                (mComment.getStatusEnum() == CommentStatus.SPAM ||
                        mComment.getStatusEnum() == CommentStatus.TRASH ||
                        mComment.getStatusEnum() == CommentStatus.DELETE)) {
            replyBox.setVisibility(View.GONE);
        } else {
            replyBox.setVisibility(View.VISIBLE);
        }

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

                if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                    moderateComment(CommentStatus.APPROVED);
                } else {
                    moderateComment(CommentStatus.SPAM);
                }
            }
        });

        mBtnTrashComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasComment()) return;

                if (mComment.willTrashingPermanentlyDelete()) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            getActivity());
                    dialogBuilder.setTitle(getResources().getText(R.string.delete));
                    dialogBuilder.setMessage(getResources().getText(R.string.dlg_sure_to_delete_comment));
                    dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    moderateComment(CommentStatus.DELETE);
                                }
                            });
                    dialogBuilder.setNegativeButton(
                            getResources().getText(R.string.no),
                            null);
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();

                } else {
                    moderateComment(CommentStatus.TRASH);
                }

            }
        });

        mBtnLikeComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeComment(false);
            }
        });

        mBtnEditComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editComment();
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
            setNote(mRestoredNoteId);
            mRestoredNoteId = null;
        }
    }

    private void setupSuggestionServiceAndAdapter() {
        if (!isAdded()) return;

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), mRemoteBlogId);
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mRemoteBlogId, getActivity(), mSuggestionServiceConnectionManager);
        if (mSuggestionAdapter != null) {
            mEditReply.setAdapter(mSuggestionAdapter);
        }
    }

    private void setComment(int localBlogId, long commentId) {
        setComment(localBlogId, CommentTable.getComment(localBlogId, commentId));
    }

    private void setReplyUniqueId() {
        if (mEditReply != null && isAdded()) {
            mEditReply.getAutoSaveTextHelper().setUniqueId(String.format(Locale.US, "%s%d%d",
                            AccountHelper.getCurrentUsernameForBlog(WordPress.getCurrentBlog()),
                            getRemoteBlogId(), getCommentId()));
        }
    }

    private void setComment(int localBlogId, final Comment comment) {
        mComment = comment;
        mLocalBlogId = localBlogId;

        setIdForCommentContainer();

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null && WordPress.wpDB.isLocalBlogIdInDatabase(mLocalBlogId));

        if (mIsUsersBlog) {
            mRemoteBlogId = WordPress.wpDB.getRemoteBlogIdForLocalTableBlogId(mLocalBlogId);
        }

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

    private void enableShouldRequestCommentFromNote() {
        mShouldRequestCommentFromNote = true;
    }

    @Override
    public Note getNote() {
        return mNote;
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

        mNote = note;
        if (isAdded()) {
            setIdForCommentContainer();
            showComment();
        }

        mRemoteBlogId = note.getSiteId();
    }

    private void setIdForFragmentContainer(int id){
        if (id > 0) {
            mIdForFragmentContainer = id;
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
        showComment();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mRemoteBlogId && mSuggestionAdapter != null) {
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
        if (requestCode == Constants.INTENT_COMMENT_EDITOR && resultCode == Activity.RESULT_OK) {
            if (mNote == null) {
                reloadComment();
            }
            // tell the host to reload the comment list
            if (mOnCommentChangeListener != null)
                mOnCommentChangeListener.onCommentChanged(ChangeType.EDITED);
        }
    }

    private boolean hasComment() {
        return (mComment != null);
    }

    private long getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }

    private int getLocalBlogId() {
        return mLocalBlogId;
    }

    private int getRemoteBlogId() {
        return mRemoteBlogId;
    }

    /*
     * reload the current comment from the local database
     */
    private void reloadComment() {
        if (!hasComment())
            return;
        Comment updatedComment = CommentTable.getComment(mLocalBlogId, getCommentId());
        setComment(mLocalBlogId, updatedComment);
    }

    /*
     * open the comment for editing
     */
    private void editComment() {
        if (!isAdded() || !hasComment())
            return;
        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        Intent intent = new Intent(getActivity(), EditCommentActivity.class);
        intent.putExtra(EditCommentActivity.ARG_LOCAL_BLOG_ID, getLocalBlogId());
        intent.putExtra(EditCommentActivity.ARG_COMMENT_ID, getCommentId());
        if (mNote != null) {
            intent.putExtra(EditCommentActivity.ARG_NOTE_ID, mNote.getId());
        }
        startActivityForResult(intent, Constants.INTENT_COMMENT_EDITOR);
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
        if (mComment == null) {
            scrollView.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.GONE);

            if (mNote != null && mShouldRequestCommentFromNote) {
                // If a remote comment was requested, check if we have the comment for display.
                // Otherwise request the comment via the REST API
                int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(mNote.getSiteId());
                if (localTableBlogId > 0) {
                    Comment comment = CommentTable.getComment(localTableBlogId, mNote.getParentCommentId());
                    if (comment != null) {
                        setComment(localTableBlogId, comment);
                        return;
                    }
                }

                long commentId = mNote.getParentCommentId() > 0 ? mNote.getParentCommentId() : mNote.getCommentId();
                requestComment(localTableBlogId, mNote.getSiteId(), commentId);
            } else if (mNote != null) {
                showCommentForNote(mNote);
            }

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

        txtName.setText(mComment.hasAuthorName() ? HtmlUtils.fastUnescapeHtml(mComment.getAuthorName()) : getString(R.string.anonymous));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mComment.getDatePublished(), WordPress.getContext()));

        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(mTxtContent, mComment.getCommentText(), maxImageSz);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        if (mComment.hasProfileImageUrl()) {
            imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(mComment.getProfileImageUrl(), avatarSz), WPNetworkImageView.ImageType.AVATAR);
        } else if (mComment.hasAuthorEmail()) {
            String avatarUrl = GravatarUtils.gravatarFromEmail(mComment.getAuthorEmail(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            imgAvatar.setImageUrl(null, WPNetworkImageView.ImageType.AVATAR);
        }

        updateStatusViews();

        // navigate to author's blog when avatar or name clicked
        if (mComment.hasAuthorUrl()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getActivity(), mComment.getAuthorUrl());
                }
            };
            imgAvatar.setOnClickListener(authorListener);
            txtName.setOnClickListener(authorListener);
            txtName.setTextColor(ContextCompat.getColor(getActivity(), R.color.reader_hyperlink));
        } else {
            txtName.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey_darken_30));
        }

        showPostTitle(getRemoteBlogId(), mComment.postID);

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
        if (txtTitle == null || !isAdded())
            return;
        if (TextUtils.isEmpty(postTitle)) {
            txtTitle.setText(R.string.untitled);
            return;
        }

        // if comment doesn't have a post title, set it to the passed one and save to comment table
        if (hasComment() && !mComment.hasPostTitle()) {
            mComment.setPostTitle(postTitle);
            CommentTable.updateCommentPostTitle(getLocalBlogId(), getCommentId(), postTitle);
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
    private void showPostTitle(final int blogId, final long postId) {
        if (!isAdded())
            return;

        final TextView txtPostTitle = (TextView) getView().findViewById(R.id.text_post_title);
        boolean postExists = ReaderPostTable.postExists(blogId, postId);

        // the post this comment is on can only be requested if this is a .com blog or a
        // jetpack-enabled self-hosted blog, and we have valid .com credentials
        boolean isDotComOrJetpack = WordPress.wpDB.isRemoteBlogIdDotComOrJetpack(mRemoteBlogId);
        boolean canRequestPost = isDotComOrJetpack && AccountHelper.isSignedInWordPressDotCom();

        final String title;
        final boolean hasTitle;
        if (mComment.hasPostTitle()) {
            // use comment's stored post title if available
            title = mComment.getPostTitle();
            hasTitle = true;
        } else if (postExists) {
            // use title from post if available
            title = ReaderPostTable.getPostTitle(blogId, postId);
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
                ReaderPostActions.requestBlogPost(blogId, postId, new ReaderActions.OnRequestListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;

                        // update title if it wasn't set above
                        if (!hasTitle) {
                            String postTitle = ReaderPostTable.getPostTitle(blogId, postId);
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
                        mOnPostClickListener.onPostClicked(getNote(), mRemoteBlogId, (int) mComment.postID);
                    } else {
                        // right now this will happen from notifications
                        AppLog.i(T.COMMENTS, "comment detail > no post click listener");
                        ReaderActivityLauncher.showReaderPostDetail(getActivity(), mRemoteBlogId, mComment.postID);
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
            mOnCommentActionListener.onModerateComment(mLocalBlogId, mComment, newStatus);
            return;
        }

        if (mNote == null) return;

        // Basic moderation support
        // Uses WP.com REST API and requires a note object
        final CommentStatus oldStatus = mComment.getStatusEnum();
        mComment.setStatus(CommentStatus.toString(newStatus));
        updateStatusViews();
        CommentActions.moderateCommentRestApi(mNote.getSiteId(), mComment.commentID, newStatus, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(CommentActionResult result) {
                if (!isAdded()) return;

                if (result.isSuccess()) {
                    if (newStatus.equals(CommentStatus.APPROVED)) {
                        ToastUtils.showToast(getActivity(), R.string.comment_moderated_approved, ToastUtils.Duration.SHORT);
                    } else if (newStatus.equals(CommentStatus.UNAPPROVED)) {
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
                        if (mComment != null && mComment.getStatusEnum() != CommentStatus.APPROVED) {
                            moderateComment(CommentStatus.APPROVED);
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
            CommentActions.submitReplyToComment(mLocalBlogId, mComment, replyText, actionListener);
        }
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

        switch (mComment.getStatusEnum()) {
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
        if (mIsUsersBlog && mComment.getStatusEnum() != CommentStatus.APPROVED) {
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
            setModerateButtonForStatus(mComment.getStatusEnum());
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
            if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                mBtnSpamCommentText.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamCommentText.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        if (canTrash()) {
            mBtnTrashComment.setVisibility(View.VISIBLE);
            if (mComment.getStatusEnum() == CommentStatus.TRASH) {
                mBtnModerateIcon.setImageResource(R.drawable.ic_action_restore);
                //mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.notification_status_unapproved_dark));
                mBtnModerateTextView.setText(R.string.mnu_comment_untrash);
                mBtnTrashCommentText.setText(R.string.mnu_comment_delete_permanently);
            } else {
                mBtnTrashCommentText.setText(R.string.mnu_comment_trash);
            }
        } else {
            mBtnTrashComment.setVisibility(View.GONE);
        }

        if (canEdit()) {
            mBtnEditComment.setVisibility(View.VISIBLE);
        }

        mLayoutButtons.setVisibility(View.VISIBLE);
    }

    private void performModerateAction(){
        if (!hasComment() || !isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        CommentStatus newStatus = CommentStatus.APPROVED;
        if (mComment.getStatusEnum() == CommentStatus.APPROVED) {
            newStatus = CommentStatus.UNAPPROVED;
        }

        mComment.setStatus(newStatus.toString());
        setModerateButtonForStatus(newStatus);
        AniUtils.startAnimation(mBtnModerateIcon, R.anim.notifications_button_scale);
        moderateComment(newStatus);
    }

    private void setModerateButtonForStatus(CommentStatus status) {
        if (status == CommentStatus.APPROVED) {
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

        // note that the local blog id won't be found if the comment is from someone else's blog
        int localBlogId = WordPress.wpDB.getLocalTableBlogIdForJetpackOrWpComRemoteSiteId(mRemoteBlogId);

        /*
         * determine which actions to enable for this comment - if the comment is from this user's
         * blog then all actions will be enabled, but they won't be if it's a reply to a comment
         * this user made on someone else's blog
         */
        mEnabledActions = note.getEnabledActions();

        //adjust enabledActions if this is a Jetpack site
        if (canLike()) {
            Blog blog = WordPress.wpDB.instantiateBlogByLocalId(localBlogId);
            if (blog != null && blog.isJetpackPowered()) {
                //delete LIKE action from enabledActions for Jetpack sites
                mEnabledActions.remove(EnabledActions.ACTION_LIKE);
            }
        }

        // Set 'Reply to (Name)' in comment reply EditText if it's a reasonable size
        if (!TextUtils.isEmpty(mNote.getCommentAuthorName()) && mNote.getCommentAuthorName().length() < 28) {
            mEditReply.setHint(String.format(getString(R.string.comment_reply_to_user), mNote.getCommentAuthorName()));
        }

        setComment(localBlogId, note.buildComment());

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
                setIdForCommentContainer();
                updateStatusViews();
            }
        });
        fragmentTransaction.replace(mCommentContentLayout.getId(), mNotificationsDetailListFragment);
        fragmentTransaction.commitAllowingStateLoss();

        getFragmentManager().invalidateOptionsMenu();
    }

    private void setIdForCommentContainer(){
        if (mCommentContentLayout != null) {
            mCommentContentLayout.setId(mIdForFragmentContainer);
        }
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
            if (mBtnLikeComment.isActivated() && mComment.getStatusEnum() == CommentStatus.UNAPPROVED) {
                mComment.setStatus(CommentStatus.toString(CommentStatus.APPROVED));
                mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.APPROVED);
                setModerateButtonForStatus(CommentStatus.APPROVED);
                commentWasUnapproved = true;
            }
        }

        final boolean commentStatusShouldRevert = commentWasUnapproved;
        WordPress.getRestClientUtils().likeComment(String.valueOf(mNote.getSiteId()),
                String.valueOf(mNote.getCommentId()),
                mBtnLikeComment.isActivated(),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            if (response.optBoolean("success")) {
                                // send signal for listeners to perform any needed updates
                                EventBus.getDefault().postSticky(new NotificationEvents.NoteLikeStatusChanged(mNote.getId()));
                            } else {
                                //rollback op in UI
                                if (!isAdded()) return;

                                // Failed, so switch the button state back
                                toggleLikeButton(!mBtnLikeComment.isActivated());

                                if (commentStatusShouldRevert) {
                                    setCommentStatusUnapproved();
                                }
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
        mComment.setStatus(CommentStatus.toString(CommentStatus.UNAPPROVED));
        mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.UNAPPROVED);
        setModerateButtonForStatus(CommentStatus.UNAPPROVED);
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

    /*
     * request a comment - note that this uses the REST API rather than XMLRPC, which means the user must
     * either be wp.com or have Jetpack, but it's safe to do this since this method is only called when
     * displayed from a notification (and notifications require wp.com/Jetpack)
     */
    private void requestComment(final int localBlogId,
                                final int remoteBlogId,
                                final long commentId) {

        final ProgressBar progress = (isAdded() && getView() != null ?
                (ProgressBar) getView().findViewById(R.id.progress_loading) : null);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }

        RestRequest.Listener restListener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (isAdded()) {
                    if (progress != null) {
                        progress.setVisibility(View.GONE);
                    }
                    Comment comment = Comment.fromJSON(jsonObject);
                    if (comment != null) {
                        // save comment to local db if localBlogId is valid
                        if (localBlogId > 0) {
                            CommentTable.addComment(localBlogId, comment);
                        }
                        // now, at long last, show the comment
                        setComment(localBlogId, comment);
                    }
                }
            }
        };
        RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.COMMENTS, VolleyUtils.errStringFromVolleyError(volleyError), volleyError);
                if (isAdded()) {
                    if (progress != null) {
                        progress.setVisibility(View.GONE);
                    }
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_comment, ToastUtils.Duration.LONG);
                }
            }
        };

        final String path = String.format(Locale.US, "/sites/%s/comments/%s", remoteBlogId, commentId);
        WordPress.getRestClientUtils().get(path, restListener, restErrListener);
    }
}
