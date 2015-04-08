package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
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

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.comments.CommentActions.ChangeType;
import org.wordpress.android.ui.comments.CommentActions.ChangedFrom;
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
import org.wordpress.android.util.AccountHelper;
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

import de.greenrobot.event.EventBus;

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private int mLocalBlogId;
    private int mRemoteBlogId;

    private Comment mComment;
    private Note mNote;

    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private TextView mTxtStatus;
    private TextView mTxtContent;
    private ImageView mImgSubmitReply;
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

    private static final String KEY_LOCAL_BLOG_ID = "local_blog_id";
    private static final String KEY_COMMENT_ID = "comment_id";
    private static final String KEY_NOTE_ID = "note_id";

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
    public static CommentDetailFragment newInstance(final String noteId) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setNoteWithNoteId(noteId);
        return fragment;
    }

    /*
     * used when called from notifications to load a comment that doesn't already exist in the note
     */
    public static CommentDetailFragment newInstanceForRemoteNoteComment(final String noteId) {
        CommentDetailFragment fragment = newInstance(noteId);
        fragment.setShouldRequestCommentFromNote(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getString(KEY_NOTE_ID) != null) {
                setNoteWithNoteId(savedInstanceState.getString(KEY_NOTE_ID));
            } else {
                int localBlogId = savedInstanceState.getInt(KEY_LOCAL_BLOG_ID);
                long commentId = savedInstanceState.getLong(KEY_COMMENT_ID);
                setComment(localBlogId, commentId);
            }
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
        mBtnLikeIcon = (ImageView)mLayoutButtons.findViewById(R.id.btn_like_icon);
        mBtnLikeTextView = (TextView)mLayoutButtons.findViewById(R.id.btn_like_text);
        mBtnModerateComment = mLayoutButtons.findViewById(R.id.btn_moderate);
        mBtnModerateIcon = (ImageView)mLayoutButtons.findViewById(R.id.btn_moderate_icon);
        mBtnModerateTextView = (TextView)mLayoutButtons.findViewById(R.id.btn_moderate_text);
        mBtnSpamComment = (TextView) mLayoutButtons.findViewById(R.id.text_btn_spam);
        mBtnTrashComment = (TextView) mLayoutButtons.findViewById(R.id.image_trash_comment);

        setTextDrawable(mBtnSpamComment, R.drawable.ic_action_spam);
        setTextDrawable(mBtnTrashComment, R.drawable.ic_action_trash);

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (SuggestionAutoCompleteText) mLayoutReply.findViewById(R.id.edit_comment);
        mEditReply.getAutoSaveTextHelper().setUniqueId(String.format("%s%d%d",
                AccountHelper.getCurrentUsernameForBlog(WordPress.getCurrentBlog()),
                getRemoteBlogId(), getCommentId()));

        mImgSubmitReply = (ImageView) mLayoutReply.findViewById(R.id.image_post_comment);

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

        mImgSubmitReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReply();
            }
        });

        mBtnSpamComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(CommentStatus.SPAM);
            }
        });

        mBtnTrashComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(CommentStatus.TRASH);
            }
        });

        mBtnLikeComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeComment();
            }
        });

        setupSuggestionServiceAndAdapter();

        return view;
    }

    private void setupSuggestionServiceAndAdapter() {
        if (!isAdded()) return;

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), mRemoteBlogId);
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mRemoteBlogId, getActivity(), mSuggestionServiceConnectionManager);
        if (mSuggestionAdapter != null) {
            mEditReply.setAdapter(mSuggestionAdapter);
        }
    }

    void setComment(int localBlogId, long commentId) {
        setComment(localBlogId, CommentTable.getComment(localBlogId, commentId));
    }

    private void setComment(int localBlogId, final Comment comment) {
        mComment = comment;
        mLocalBlogId = localBlogId;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null && WordPress.wpDB.isLocalBlogIdInDatabase(mLocalBlogId));

        if (mIsUsersBlog)
            mRemoteBlogId = WordPress.wpDB.getRemoteBlogIdForLocalTableBlogId(mLocalBlogId);

        if (isAdded())
            showComment();
    }

    private void setShouldFocusReplyField(boolean shouldFocusReplyField) {
        mShouldFocusReplyField = shouldFocusReplyField;
    }

    void setShouldRequestCommentFromNote(boolean shouldRequestComment) {
        mShouldRequestCommentFromNote = shouldRequestComment;
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
                setRemoteBlogId(note.getSiteId());
            } catch (BucketObjectMissingException e) {
                e.printStackTrace();
            }
        }
    }

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
    public void onEventMainThread(SuggestionEvents.SuggestionListUpdated event) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == mRemoteBlogId) {
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
        EditTextUtils.hideSoftInput(mEditReply);
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
                mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_DETAIL, ChangeType.EDITED);
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
        return (mComment != null);
    }

    long getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }

    private int getLocalBlogId() {
        return mLocalBlogId;
    }

    private int getRemoteBlogId() {
        return mRemoteBlogId;
    }

    public void setRestoredReplyText(String restoredReplyText) {
        mRestoredReplyText = restoredReplyText;
    }

    /*
     * reload the current comment from the local database
     */
    void reloadComment() {
        if (!hasComment())
            return;
        Comment updatedComment = CommentTable.getComment(mLocalBlogId, getCommentId());
        setComment(mLocalBlogId, updatedComment);
    }

    /*
     * resets to no comment
     */
    void clear() {
        setNote(null);
        setComment(0, null);
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

        txtName.setText(mComment.hasAuthorName() ? mComment.getAuthorName() : getString(R.string.anonymous));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mComment.getDatePublished()));

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
            txtName.setTextColor(getResources().getColor(R.color.reader_hyperlink));
        } else {
            txtName.setTextColor(getResources().getColor(R.color.grey_darken_30));
        }

        showPostTitle(getRemoteBlogId(), mComment.postID);

        // make sure reply box is showing
        if (mLayoutReply.getVisibility() != View.VISIBLE && canReply()) {
            AniUtils.flyIn(mLayoutReply);
            if (mEditReply != null && mShouldFocusReplyField) {
                mEditReply.requestFocus();
                setShouldFocusReplyField(false);
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
            txtTitle.setText(getString(R.string.on) + " " + postTitle.trim());
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
        boolean canRequestPost = isDotComOrJetpack && AccountHelper.getDefaultAccount().hasAccessToken();

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
                ReaderPostActions.requestPost(blogId, postId, new ReaderActions.ActionListener() {
                    @Override
                    public void onActionResult(boolean succeeded) {
                        if (!isAdded())
                            return;
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
     * approve, unapprove, spam, or trash the current comment
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

        // Basic moderation support, currently only used when this Fragment is in a CommentDetailActivity
        // Uses WP.com REST API and requires a note object
        final CommentStatus oldStatus = mComment.getStatusEnum();
        mComment.setStatus(CommentStatus.toString(newStatus));
        updateStatusViews();
        CommentActions.moderateCommentRestApi(mNote.getSiteId(), mComment.commentID, newStatus, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!isAdded()) return;

                if (!succeeded) {
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
        if (!isAdded() || mIsSubmittingReply)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mImgSubmitReply.setVisibility(View.GONE);
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingReply = false;
                if (succeeded && mOnCommentChangeListener != null)
                    mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_DETAIL, ChangeType.REPLIED);
                if (isAdded()) {
                    mEditReply.setEnabled(true);
                    mImgSubmitReply.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    updateStatusViews();
                    if (succeeded) {
                        ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
                        mEditReply.setText(null);
                        mEditReply.getAutoSaveTextHelper().clearSavedText(mEditReply);
                    } else {
                        ToastUtils.showToast(getActivity(), R.string.reply_failed, ToastUtils.Duration.LONG);
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
     * sets the drawable for moderation buttons
     */
    private void setTextDrawable(final TextView view, int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(resId), null, null);
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
                statusColor = getActivity().getResources().getColor(R.color.notification_status_unapproved_dark);
                break;
            case UNAPPROVED:
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = getActivity().getResources().getColor(R.color.notification_status_unapproved_dark);
                break;
            case SPAM:
                statusTextResId = R.string.comment_status_spam;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
                break;
            case TRASH:
            default:
                statusTextResId = R.string.comment_status_trash;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
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
                AniUtils.fadeIn(mTxtStatus);
            }
        } else {
            mTxtStatus.setVisibility(View.GONE);
        }

        if (canModerate()) {
            setModerateButtonForStatus(mComment.getStatusEnum());
            mBtnModerateComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
            });
            mBtnModerateComment.setVisibility(View.VISIBLE);
        } else {
            mBtnModerateComment.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            mBtnSpamComment.setVisibility(View.VISIBLE);
            if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                mBtnSpamComment.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamComment.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        if (canTrash()) {
            mBtnTrashComment.setVisibility(View.VISIBLE);
        } else {
            mBtnTrashComment.setVisibility(View.GONE);
        }

        mLayoutButtons.setVisibility(View.VISIBLE);
    }

    private void setModerateButtonForStatus(CommentStatus status) {
        if (status == CommentStatus.APPROVED) {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve_active);
            mBtnModerateTextView.setText(R.string.comment_status_approved);
            mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.notification_status_unapproved_dark));
        } else {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve);
            mBtnModerateTextView.setText(R.string.mnu_comment_approve);
            mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.grey));
        }
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        if (mEnabledActions == null)
            return false;
        return (mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
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

        // note that the local blog id won't be found if the comment is from someone else's blog
        int localBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(mRemoteBlogId);

        setComment(localBlogId, note.buildComment());

        getFragmentManager().invalidateOptionsMenu();
    }

    // Like or unlike a comment via the REST API
    private void likeComment() {
        if (mNote == null) return;

        toggleLikeButton(!mBtnLikeComment.isActivated());

        ReaderAnim.animateLikeButton(mBtnLikeIcon, mBtnLikeComment.isActivated());

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
                        if (response != null && !response.optBoolean("success"))  {
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
        mComment.setStatus(CommentStatus.toString(CommentStatus.UNAPPROVED));
        mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.UNAPPROVED);
        setModerateButtonForStatus(CommentStatus.UNAPPROVED);
    }

    private void toggleLikeButton(boolean isLiked) {
        if (isLiked) {
            mBtnLikeTextView.setText(getResources().getString(R.string.mnu_comment_liked));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.orange_jazzy));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like_active));
            mBtnLikeComment.setActivated(true);
        } else {
            mBtnLikeTextView.setText(getResources().getString(R.string.reader_label_like));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.grey));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like));
            mBtnLikeComment.setActivated(false);
        }
    }

    public String getReplyText() {
        if (mEditReply == null) return null;

        return mEditReply.getText().toString();
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

        final String path = String.format("/sites/%s/comments/%s", remoteBlogId, commentId);
        WordPress.getRestClientUtils().get(path, restListener, restErrListener);
    }

    private void setRemoteBlogId(int remoteBlogId) {
        mRemoteBlogId = remoteBlogId;
    }
}
