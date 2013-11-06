package org.wordpress.android.ui.reader_native;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderLikeTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderThumbnailTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader_native.actions.ReaderPostActions;
import org.wordpress.android.ui.reader_native.adapters.ReaderCommentAdapter;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

/**
 * Created by nbradbury on 7/8/13.
 */
public class ReaderPostDetailActivity extends FragmentActivity {
    protected static final String ARG_BLOG_ID = "blog_id";
    protected static final String ARG_POST_ID = "post_id";

    private long mPostId;
    private long mBlogId;
    private ReaderPost mPost;

    private LayoutInflater mInflater;
    private ViewGroup mLayoutLikingAvatars;
    private ViewGroup mLayoutActions;
    private ListView mListView;
    private ViewGroup mCommentFooter;
    private ProgressBar mProgressFooter;

    private boolean mIsAddCommentBoxShowing = false;
    private long mReplyToCommentId = 0;
    private boolean mHasAlreadyUpdatedPost = false;
    private boolean mIsUpdatingComments = false;
    private boolean mIsPostChanged = false;

    private ReaderUrlList mVideoThumbnailUrls = new ReaderUrlList();

    private final Handler mHandler = new Handler();

    private ListView getListView() {
        if (mListView==null) {
            mListView = (ListView) findViewById(android.R.id.list);

            // enable replying to an individual comment when the user long clicks it - this is done
            // instead of setting an OnItemClickListener for two reasons:
            //   1. OnItemClickListener won't fire for comments that contain links (due
            //      to the comment text using autoLink="web")
            //   2. It's too easy to accidentally cause OnItemClickListener to fire while
            //      scrolling/flinging through the list, whereas OnItemLongClickListener
            //      is a more deliberate action
            mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    // id will be the id of the tapped comment - note that it will be -1 when the
                    // post detail header is long clicked, which we want to ignore
                    if (id > 0) {
                        showAddCommentBox(id);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
        return mListView;
    }

    /*
     * important to NOT call this until mPost has been loaded
     */
    private ReaderCommentAdapter mAdapter;
    private ReaderCommentAdapter getCommentAdapter() {
        if (mAdapter==null) {
            if (mPost==null)
                ReaderLog.w("comment adapter created before post loaded");

            ReaderActions.DataLoadedListener dataLoadedListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    // show divider between post detail and comments, and footer below comments, when
                    // comments exist
                    final View commentDivider = findViewById(R.id.divider_comments);
                    commentDivider.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    mCommentFooter.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                }
            };

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            ReaderActions.DataRequestedListener dataRequestedListener = new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData(ReaderActions.RequestDataAction action) {
                    if (mIsUpdatingComments)
                        return;
                    ReaderLog.i("requesting newer comments");
                    updateComments();
                }
            };
            mAdapter = new ReaderCommentAdapter(this, mPost, dataLoadedListener, dataRequestedListener);
        }
        return mAdapter;
    }

    private boolean hasCommentAdapter() {
        return mAdapter!=null;
    }

    private boolean isCommentAdapterEmpty() {
        return (mAdapter==null || mAdapter.isEmpty());
    }

    /*
     * triggered when user chooses to like or follow
     */
    private void doPostAction(View btnAction, ReaderPostActions.PostAction action, ReaderPost post) {
        boolean isSelected = btnAction.isSelected();
        btnAction.setSelected(!isSelected);
        ReaderAniUtils.zoomAction(btnAction);

        if (!ReaderPostActions.performPostAction(this, action, post, null)) {
            btnAction.setSelected(isSelected);
            return;
        }

        // get the post again, since it has changed
        mPost = ReaderPostTable.getPost(mBlogId, mPostId);
        mIsPostChanged = true;

        // call returns before api completes, but local version of post will have been changed
        // so refresh to show those changes
        switch (action) {
            case TOGGLE_LIKE:
                refreshLikes(true);
                break;
            case TOGGLE_FOLLOW:
                refreshFollowed();
                break;
        }
    }

    /*
     * triggered when user chooses to reblog the post
     */
    private void doPostReblog(View btnReblog, ReaderPost post) {
        if (post.isRebloggedByCurrentUser) {
            ToastUtils.showToast(this, R.string.reader_toast_err_already_reblogged);
            return;
        }
        btnReblog.setSelected(true);
        ReaderAniUtils.zoomAction(btnReblog);
        ReaderActivityLauncher.showReaderReblogForResult(this, post);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();
        setContentView(R.layout.reader_activity_post_detail);

        // remove window background since background color is set in layout (prevents overdraw)
        getWindow().setBackgroundDrawable(null);

        // set the "fake" ActionBar height to that of a real one
        final int actionbarHeight = DisplayUtils.getActionBarHeight(this);
        final ViewGroup layoutFakeActionBar = (ViewGroup) findViewById(R.id.layout_fake_actionbar);
        layoutFakeActionBar.setMinimumHeight(actionbarHeight);

        // add a header to the listView that's the same height as the "fake" ActionBar - this moves
        // the actual content of the listView below the ActionBar, but enables it to scroll under
        // the translucent ActionBar layout
        RelativeLayout headerFake = new RelativeLayout(this);
        headerFake.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, actionbarHeight));
        getListView().addHeaderView(headerFake, null, false);

        mBlogId = getIntent().getLongExtra(ARG_BLOG_ID, 0);
        mPostId = getIntent().getLongExtra(ARG_POST_ID, 0);

        // add post detail as header to listView - must be done before setting adapter
        ViewGroup headerDetail = (ViewGroup) mInflater.inflate(R.layout.reader_listitem_post_detail, getListView(), false);
        getListView().addHeaderView(headerDetail, null, false);

        // add listView footer containing progress bar - footer appears whenever there are comments,
        // progress bar appears when loading new comments
        mCommentFooter = (ViewGroup) mInflater.inflate(R.layout.reader_footer_progress, getListView(), false);
        mCommentFooter.setVisibility(View.GONE);
        mCommentFooter.setBackgroundColor(getResources().getColor(R.color.grey_extra_light));
        mProgressFooter = (ProgressBar) mCommentFooter.findViewById(R.id.progress_footer);
        mProgressFooter.setVisibility(View.INVISIBLE);
        getListView().addFooterView(mCommentFooter);

        mLayoutLikingAvatars = (ViewGroup) findViewById(R.id.layout_liking_avatars);
        mLayoutActions = (ViewGroup) findViewById(R.id.layout_actions);

        ImageView imgBack = (ImageView) findViewById(R.id.image_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageView imgShare = (ImageView) findViewById(R.id.image_share);
        imgShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePage();
            }
        });

        // hide listView until post is loaded
        getListView().setVisibility(View.INVISIBLE);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();

        if (mIsPostTaskRunning)
            ReaderLog.w("post task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new ShowPostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new ShowPostTask().execute();
        }
    }

    private static final String KEY_SHOW_COMMENT_BOX = "show_comment_box";
    private static final String KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id";
    private static final String KEY_ALREADY_UPDATED = "already_updated";
    private static final String KEY_IS_POST_CHANGED = "is_post_changed";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_ALREADY_UPDATED, mHasAlreadyUpdatedPost);
        outState.putBoolean(KEY_IS_POST_CHANGED, mIsPostChanged);
        outState.putBoolean(KEY_SHOW_COMMENT_BOX, mIsAddCommentBoxShowing);
        if (mIsAddCommentBoxShowing)
            outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState!=null) {
            mHasAlreadyUpdatedPost = savedInstanceState.getBoolean(KEY_ALREADY_UPDATED);
            mIsPostChanged = savedInstanceState.getBoolean(KEY_IS_POST_CHANGED);
            if (savedInstanceState.getBoolean(KEY_SHOW_COMMENT_BOX)) {
                long replyToCommentId = savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID);
                showAddCommentBox(replyToCommentId);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // if comment box is showing, cancel it rather than backing out of this activity
        if (mIsAddCommentBoxShowing) {
            hideAddCommentBox();
        } else {
            // return blogId/postId to caller, but only set to RESULT_OK if the post has changed
            // so the calling activity can refresh the displayed post
            Intent data = new Intent();
            data.putExtra(ARG_BLOG_ID, mBlogId);
            data.putExtra(ARG_POST_ID, mPostId);
            if (mIsPostChanged) {
                setResult(RESULT_OK, data);
            } else {
                setResult(RESULT_CANCELED, data);
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode== Activity.RESULT_OK);

        switch (requestCode) {
            case Constants.INTENT_READER_REBLOG :
                // user just returned from reblog activity - if post was successfully reblogged,
                // then update the local post and select the reblog button
                if (isResultOK) {
                    mPost.isRebloggedByCurrentUser = true;
                    TextView btnReblog = (TextView) findViewById(R.id.btn_reblog);
                    btnReblog.setSelected(true);
                }
        }
    }

    /*
     * pass current web page url to chosen sharing activity
     */
    private void sharePage() {
        String subject = getString(R.string.reader_share_subject, getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mPost.getUrl());
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }
    }
    /*
     * get the latest version of this post
     */
    private void updatePost() {
        if (mPost==null || !mPost.isWP())
            return;

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                switch (result) {
                    case CHANGED :
                        // post has changed, so get latest version
                        mPost = ReaderPostTable.getPost(mBlogId, mPostId);
                        mIsPostChanged = true;
                        break;
                    case FAILED:
                        // failed to get post, so do nothing here
                        return;
                    default :
                        // unchanged
                        break;
                }

                // determine whether we need to update likes/comments - done regardless of
                // whether the post has changed since local likes/comments could still be
                // different than what we already have for the post
                new Thread() {
                    @Override
                    public void run() {
                        final boolean isLikesChanged = mPost.numLikes!=ReaderLikeTable.getNumLikesForPost(mPost);
                        final boolean isCommentsChanged = mPost.numReplies!=ReaderCommentTable.getNumCommentsForPost(mPost);
                        if (isLikesChanged || isCommentsChanged) {
                            mHandler.post(new Runnable() {
                                public void run() {
                                    if (isLikesChanged)
                                        updateLikes();
                                    if (isCommentsChanged)
                                        updateComments();
                                }
                            });
                        }
                    }
                }.start();
            }
        };
        ReaderPostActions.updatePost(mPost, resultListener);
    }

    /*
     * request comments for this post
     */
    private void updateComments() {
        if (mPost==null || !mPost.isWP())
            return;

        if (mIsUpdatingComments)
            return;

        mIsUpdatingComments = true;

        if (!isCommentAdapterEmpty())
            showProgressFooter();

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                mIsUpdatingComments = false;
                hideProgressFooter();
                if (result== ReaderActions.UpdateResult.CHANGED)
                    refreshComments();
            }
        };
        ReaderCommentActions.updateCommentsForPost(mPost, resultListener);
    }

    /*
     * show progress bar at the bottom of the screen - used when getting newer comments
     */
    private void showProgressFooter() {
        if (mProgressFooter ==null || mProgressFooter.getVisibility()==View.VISIBLE )
            return;
        mProgressFooter.setVisibility(View.VISIBLE);
    }

    /*
     * hide the footer progress bar if it's showing
     */
    private void hideProgressFooter() {
        if (mProgressFooter ==null || mProgressFooter.getVisibility()!=View.VISIBLE )
            return;
        mProgressFooter.setVisibility(View.INVISIBLE);
    }

    /*
     * get the latest likes for this post
     */
    private void updateLikes() {
        if (mPost==null || !mPost.isWP())
            return;

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result== ReaderActions.UpdateResult.CHANGED) {
                    // get post again since likes have been updated
                    mPost = ReaderPostTable.getPost(mBlogId, mPostId);
                    refreshLikes(false);

                }
            }
        };
        ReaderPostActions.updateLikesForPost(mPost, resultListener);
    }

    /*
     * refresh adapter so latest comments appear
     */
    private void refreshComments() {
        getCommentAdapter().refreshComments();
    }

    /*
     * show latest likes for this post - pass true to force reloading avatars (used when user clicks
     * the like button, to ensure the current user's avatar appears)
     */
    private static final int NUM_ACTIONBAR_ICONS = 2;
    private void refreshLikes(final boolean forceReload) {
        if (mPost==null || !mPost.isWP())
            return;

        new Thread() {
            @Override
            public void run() {
                final TextView btnLike = (TextView) findViewById(R.id.btn_like);

                final int marginExtraSmall = getResources().getDimensionPixelSize(R.dimen.reader_margin_extra_small);
                final int marginLarge = getResources().getDimensionPixelSize(R.dimen.reader_margin_large);
                final int actionBarIconSz = DisplayUtils.getActionBarHeight(ReaderPostDetailActivity.this); // <-- fudging it here, but it works
                final int likeAvatarSize = getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_like);
                final int likeAvatarSizeWithMargin = likeAvatarSize + (marginExtraSmall * 2);

                // determine how many avatars will fit the space (takes the two ActionBar icons into account)
                final int displayWidth = DisplayUtils.getDisplayPixelWidth(ReaderPostDetailActivity.this);
                final int spaceForAvatars = displayWidth - (actionBarIconSz * NUM_ACTIONBAR_ICONS) - (marginLarge * NUM_ACTIONBAR_ICONS);
                final int maxAvatars = spaceForAvatars / likeAvatarSizeWithMargin;

                // get avatars of liking users up to the max
                final ArrayList<String> avatars = ReaderUserTable.getAvatarUrls(ReaderLikeTable.getLikesForPost(mPost), maxAvatars);

                mHandler.post(new Runnable() {
                    public void run() {
                        // set the like text
                        /*if (mPost.isLikedByCurrentUser) {
                            if (mPost.numLikes==1) {
                                txtLikes.setText(R.string.reader_likes_only_you);
                            } else {
                                txtLikes.setText(mPost.numLikes==2 ? getString(R.string.reader_likes_you_and_one_short) : getString(R.string.reader_likes_you_and_multi_short, mPost.numLikes-1));
                            }
                        } else {
                            txtLikes.setText(mPost.numLikes==1 ? getString(R.string.reader_likes_one) : getString(R.string.reader_likes_multi, mPost.numLikes));
                        }*/

                        btnLike.setText(mPost.isLikedByCurrentUser ? R.string.reader_btn_unlike : R.string.reader_btn_like);
                        btnLike.setSelected(mPost.isLikedByCurrentUser);
                        btnLike.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                doPostAction(btnLike, ReaderPostActions.PostAction.TOGGLE_LIKE, mPost);
                            }
                        });

                        // nothing more to do if no likes
                        if (avatars.size()==0 && mPost.numLikes==0) {
                            mLayoutLikingAvatars.setVisibility(View.GONE);
                            return;
                        }

                        // at this point it's possible that we know the post has likes but we haven't retrieved liking users yet, so
                        // make sure we have liking avatars before attempting to show them (if there are none, only like text appears)
                        if (avatars.size() > 0) {
                            // clicking likes view shows activity displaying all liking users - this is only set
                            // if we know there are liking avatars, otherwise tapping the likes view would show
                            // the liking users with "0 people like this"
                            mLayoutLikingAvatars.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ReaderActivityLauncher.showReaderLikingUsers(ReaderPostDetailActivity.this, mPost);
                                }
                            });

                            // skip adding liking avatars if the view's child count indicates that we've already
                            // added the max on a previous call to this routine
                            if (forceReload || mLayoutLikingAvatars.getChildCount() < maxAvatars) {
                                mLayoutLikingAvatars.removeAllViews();
                                for (String url: avatars) {
                                    WPNetworkImageView imgAvatar = (WPNetworkImageView) mInflater.inflate(R.layout.reader_like_avatar, mLayoutLikingAvatars, false);
                                    mLayoutLikingAvatars.addView(imgAvatar);
                                    imgAvatar.setImageUrl(PhotonUtils.fixAvatar(url, likeAvatarSize), WPNetworkImageView.ImageType.AVATAR);
                                }
                            }
                        }

                        // show the liking layout if it's not already showing
                        if (mLayoutLikingAvatars.getVisibility()!=View.VISIBLE) {
                            //ReaderAniUtils.startAnimation(mLayoutLikingAvatars, R.anim.reader_top_bar_in);
                            mLayoutLikingAvatars.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }.start();
    }


    /*
     * show the view enabling adding a comment - triggered when user hits comment icon/count in header
     * note that this view is hidden at design time, so it will be shown the first time user taps icon.
     * pass 0 for the replyToCommentId to add a parent-level comment to the post, or pass a real
     * comment id to reply to a specific comment
     */
    private void showAddCommentBox(final long replyToCommentId) {
        // skip if it's already showing
        if (mIsAddCommentBoxShowing)
            return;

        // don't show comment box if a comment is currently being submitted
        if (mIsSubmittingComment)
            return;

        final EditText editComment = (EditText) findViewById(R.id.edit_comment);
        final ViewGroup layoutCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        final TextView btnComment = (TextView) findViewById(R.id.btn_comment);

        // different hint depending on whether user is replying to a comment or commenting on the post
        editComment.setHint(replyToCommentId==0 ? R.string.reader_hint_comment_on_post : R.string.reader_hint_comment_on_comment);

        btnComment.setSelected(true);
        ReaderAniUtils.flyIn(layoutCommentBox);

        editComment.requestFocus();
        editComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_SEND)
                    submitComment(replyToCommentId);
                return false;
            }
        });

        // submit comment when image tapped
        final ImageView imgPostComment = (ImageView) findViewById(R.id.image_post_comment);
        imgPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment(replyToCommentId);
            }
        });

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editComment, InputMethodManager.SHOW_IMPLICIT);

        // if user is replying to another comment, highlight the comment being replied to and scroll
        // it to the top so the user can see which comment they're replying to - note that scrolling
        // is delayed to give time for listView to reposition due to soft keyboard appearing
        if (replyToCommentId!=0) {
            getCommentAdapter().setHighlightCommentId(replyToCommentId, false);
            getListView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollToCommentId(replyToCommentId);
                }
            }, 300);
        }

        // mReplyToCommentId must be saved here so it can be stored by onSaveInstanceState()
        mReplyToCommentId = replyToCommentId;
        mIsAddCommentBoxShowing = true;
    }

    private void hideAddCommentBox() {
        if (!mIsAddCommentBoxShowing)
            return;

        final EditText editComment = (EditText) findViewById(R.id.edit_comment);
        final ViewGroup layoutCommentBox = (ViewGroup) findViewById(R.id.layout_comment_box);
        final TextView btnComment = (TextView) findViewById(R.id.btn_comment);

        btnComment.setSelected(false);
        ReaderAniUtils.flyOut(layoutCommentBox);
        EditTextUtils.hideSoftInput(editComment);

        getCommentAdapter().setHighlightCommentId(0, false);

        mIsAddCommentBoxShowing = false;
        mReplyToCommentId = 0;
    }

    private void toggleShowAddCommentBox() {
        if (mIsAddCommentBoxShowing) {
            hideAddCommentBox();
        } else {
            showAddCommentBox(0);
        }
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private void scrollToCommentId(long commentId) {
        int position = getCommentAdapter().indexOfCommentId(commentId);
        if (position == -1)
            return;
        getListView().setSelectionFromTop(position + getListView().getHeaderViewsCount(), 0);
    }

    /*
     * post the text typed into the comment box as a comment on the current post
     */
    private boolean mIsSubmittingComment = false;
    private void submitComment(final long replyToCommentId) {
        final EditText editComment = (EditText) findViewById(R.id.edit_comment);
        final String commentText = EditTextUtils.getText(editComment);
        if (TextUtils.isEmpty(commentText))
            return;

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
                if (succeeded) {
                    // comment posted successfully so stop highlighting the fake one and replace
                    // it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false);
                    getCommentAdapter().replaceComment(fakeCommentId, newComment);
                    getListView().invalidateViews();
                } else {
                    // comment failed to post - show the comment box again with the comment text intact,
                    // and remove the "fake" comment from the adapter
                    editComment.setText(commentText);
                    showAddCommentBox(replyToCommentId);
                    getCommentAdapter().removeComment(fakeCommentId);
                    ToastUtils.showToast(ReaderPostDetailActivity.this, R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
            }
        };

        final ReaderComment newComment = ReaderCommentActions.submitPostComment(mPost,
                                                                                fakeCommentId,
                                                                                commentText,
                                                                                replyToCommentId,
                                                                                actionListener);
        if (newComment!=null) {
            mIsPostChanged = true;
            editComment.setText(null);
            // add the "fake" comment to the adapter, highlight it, and show a progress bar
            // next to it while it's submitted
            getCommentAdapter().setHighlightCommentId(newComment.commentId, true);
            getCommentAdapter().addComment(newComment);
            // make sure it's scrolled into view
            scrollToCommentId(fakeCommentId);
        }
    }

    /*
     * refresh the follow button based on whether this is a followed blog
     */
    private void refreshFollowed() {
        new Thread() {
            @Override
            public void run() {
                final TextView btnFollow = (TextView) findViewById(R.id.btn_follow);
                final boolean isFollowed = ReaderPostTable.isPostFollowed(mPost);
                mHandler.post(new Runnable() {
                    public void run() {
                        btnFollow.setText(isFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
                        btnFollow.setSelected(isFollowed);
                        btnFollow.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                doPostAction(btnFollow, ReaderPostActions.PostAction.TOGGLE_FOLLOW, mPost);
                            }
                        });
                    }
                });
            }
        }.start();
    }

    /*
     * extracts the src from iframes and inserts a video player image with a link to the actual video
     * this is necessary since embedded videos won't work (and the CSS we're using hides iframes)
     * ex: <iframe src='http://player.vimeo.com/video/36281008' width='500' height='281' frameborder='0'></iframe>
     * this isn't very performance-friendly due to all the string creation, but it does the job
     * and performance isn't critical since this is only called once (from inside the AsyncTask)
     */
    private static final String OVERLAY_IMG = "file:///android_res/drawable/ic_reader_video_overlay.png";
    private String processVideos(String text) {
        if (text==null)
            return "";

        int iFrameStart = text.indexOf("<iframe");
        if (iFrameStart == -1)
            return text;

        // hack to determine whether html uses single-quoted attributes
        boolean usesSingleQuotes = text.contains("src='");

        while (iFrameStart > -1) {
            int iFrameEnd = text.indexOf(">", iFrameStart);
            if (iFrameEnd == -1)
                return text;

            int srcStart = text.indexOf(usesSingleQuotes ? "src='" : "src=\"", iFrameStart);
            if (srcStart == -1 || srcStart > iFrameEnd)
                return text;

            int srcEnd = text.indexOf(usesSingleQuotes ? "'" : "\"", srcStart+5);
            if (srcEnd == -1 || srcEnd > iFrameEnd)
                return text;

            String src = text.substring(srcStart+5, srcEnd);

            boolean isVideo = (src.contains("youtube")
                            || src.contains("video")
                            || src.contains("vimeo"));

            final String videoDiv;
            if (isVideo) {
                // use generic video player overlay if we don't have the thumbnail for this video, otherwise
                // show the thumbnail with the player overlay on top of it - note there's a good chance we
                // have the thumbnail since it was likely already downloaded by the post list
                String thumbnailUrl = ReaderThumbnailTable.getThumbnailUrl(src);
                videoDiv = makeVideoDiv(src, thumbnailUrl);
                // keep track of thumbnail urls so we know when they're clicked
                if (!TextUtils.isEmpty(thumbnailUrl))
                    mVideoThumbnailUrls.add(thumbnailUrl);
                // insert the video div before the iframe - note that the iframe will be hidden by the CSS used
                // in the AsyncTask below
                text = text.substring(0, iFrameStart) + videoDiv + text.substring(iFrameStart);
            } else {
                // if we get here it means we're not sure the iframe is a video, in which case don't show anything
                videoDiv = "";
            }

            iFrameStart = text.indexOf("<iframe", iFrameEnd + videoDiv.length());
        }

        return text;
    }

    /*
     * creates formatted div for passed video with passed (optional) thumbnail
     */
    /*private String makeVideoDiv(String videoUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(videoUrl))
            return "";

        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        int marginLarge = getResources().getDimensionPixelSize(R.dimen.reader_margin_large);

        int thumbWidth = (int)(displayWidth * 0.5f) - marginLarge;
        int thumbHeight = (int)(thumbWidth * 0.65f);

        int overlaySz = getResources().getDimensionPixelSize(R.dimen.reader_video_overlay_size) / 2;
        int overlayLeft = (thumbWidth / 2) - (overlaySz / 2);
        int overlayTop = (thumbHeight / 2) - (overlaySz / 2);

        if (TextUtils.isEmpty(thumbnailUrl)) {
            return String.format("<div class='wpreader-video' align='center'><a href='%s'><img style='width:%dpx; height:%dpx; display:block;' src='%s' /></a></div>", videoUrl, overlaySz, overlaySz, OVERLAY_IMG);
        } else {
            return "<div style='position:relative'>"
                    + String.format("<a href='%s'><img src='%s' style='display:inline; width:%dpx; height:%dpx;' class='wpreader-video-thumb' /></a>", videoUrl, thumbnailUrl, thumbWidth, thumbHeight)
                    + String.format("<a href='%s'><img src='%s' style='display:inline; width:%dpx; height:%dpx; left:%dpx; top:%dpx; position:absolute;' /></a>", videoUrl, OVERLAY_IMG, overlaySz, overlaySz, overlayLeft, overlayTop)
                    + "</div>";
        }
    }*/

    private String makeVideoDiv(String videoUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(videoUrl))
            return "";

        int overlaySz = getResources().getDimensionPixelSize(R.dimen.reader_video_overlay_size) / 2;

        if (TextUtils.isEmpty(thumbnailUrl)) {
            return String.format("<div class='wpreader-video' align='center'><a href='%s'><img style='width:%dpx; height:%dpx; display:block;' src='%s' /></a></div>", videoUrl, overlaySz, overlaySz, OVERLAY_IMG);
        } else {
            return "<div style='position:relative'>"
                    + String.format("<a href='%s'><img src='%s' style='width:100%%; height:auto;' /></a>", videoUrl, thumbnailUrl)
                    + String.format("<a href='%s'><img src='%s' style='width:%dpx; height:%dpx; position:absolute; left:0px; right:0px; top:0px; bottom:0px; margin:auto;'' /></a>", videoUrl, OVERLAY_IMG, overlaySz, overlaySz)
                    + "</div>";
        }
    }

    /*
     * called when user taps an image in the webView - shows the image full-screen
     */
    private void showPhotoViewer(final String imageUrl) {
        if (TextUtils.isEmpty(imageUrl))
            return;
        // images in private posts must use https for auth token to be sent with request
        if (mPost.isPrivate) {
            ReaderActivityLauncher.showReaderPhotoViewer(this, UrlUtils.makeHttps(imageUrl));
        } else {
            ReaderActivityLauncher.showReaderPhotoViewer(this, imageUrl);
        }
    }
    /*
     * build html for post's content
     */
    private String getPostHtml(ReaderPost post) {
        if (post==null)
            return "";

        String content;
        if (post.hasText()) {
            content = post.getText();
            // insert video div before content if this is a VideoPress post (video otherwise won't appear)
            if (post.isVideoPress)
                content = makeVideoDiv(post.getFeaturedVideo(), post.getFeaturedImage()) + content;
        } else if (post.hasFeaturedImage()) {
            // some photo blogs have posts with empty content but still have a featured image, so
            // use the featured image as the content
            content = String.format("<p><img class='img.size-full' src='%s' /></p>", post.getFeaturedImage());
        } else {
            content = "";
        }

        int marginLarge = getResources().getDimensionPixelSize(R.dimen.reader_margin_large);
        int marginSmall = getResources().getDimensionPixelSize(R.dimen.reader_margin_small);

        final String linkColor = HtmlUtils.colorResToHtmlColor(this, R.color.reader_hyperlink);
        final String greyLight = HtmlUtils.colorResToHtmlColor(this, R.color.grey_light);

        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't strictly necessary, but source is invalid html5 without one
        sbHtml.append("<title>Reader Post</title>");

        // use "Open Sans" Google font
        sbHtml.append("<link rel='stylesheet' type='text/css' href='http://fonts.googleapis.com/css?family=Open+Sans' />");

        sbHtml.append("<style type='text/css'>")
                .append("  body { font-family: 'Open Sans', sans-serif; margin: 0px; padding: 0px; }")
                .append("  body, p, div { font-size: 1em; line-height: 1.5em; max-width: 100% !important;}");

        // use a consistent top/bottom margin for paragraphs
        sbHtml.append("  p { margin-top: 0px; margin-bottom: ").append(marginSmall).append("px; }");

        // css for video div when no video thumb available (see processVideos)
        sbHtml.append("  div.wpreader-video { background-color: ").append(greyLight).append(";")
              .append("     width: 100%; padding: ").append(marginLarge).append("px 0px }");

        // make sure links are shown in the same color they are elsewhere in the app
        sbHtml.append("  a { text-decoration: none; color: ").append(linkColor).append("; }");

        // hide iframes & embeds (they won't work since script is disabled)
        sbHtml.append("  iframe, embed { display: none; }");

        // show large wp images full-width (unnecessary in most cases since they'll already be at least
        // as wide as the display, except maybe when viewed on a large landscape tablet)
        sbHtml.append("  img.size-full, img.size-large { width: 100% !important; }");

        // center medium-sized wp image
        sbHtml.append("  img.size-medium { margin-left: auto !important; margin-right: auto !important; }");

        // hide VideoPress divs that don't make sense on mobile
        sbHtml.append("  div.video-player, div.videopress-title, div.play-button, div.videopress-watermark { display: none; }");

        // hide noscript
        //sbHtml.append("  noscript { display: none ;}");

        sbHtml.append("</style></head><body>")
                .append(processVideos(content))
                .append("</body></html>");

        return sbHtml.toString();
    }

    /*
     * AsyncTask to retrieve & display this post
     */
    private boolean mIsPostTaskRunning = false;
    private class ShowPostTask extends AsyncTask<Void, Void, Boolean> {
        TextView txtTitle;
        TextView txtSource;
        WebView webView;
        TextView btnReblog;
        TextView btnComment;
        ViewGroup layoutTitle;
        WPNetworkImageView imgAvatar;

        String postHtml;

        @Override
        protected void onPreExecute() {
            mIsPostTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsPostTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            // locate views
            txtTitle = (TextView) findViewById(R.id.text_title);
            txtSource = (TextView) findViewById(R.id.text_source);
            webView = (WebView) findViewById(R.id.webView);
            imgAvatar = (WPNetworkImageView) findViewById(R.id.image_avatar);
            btnReblog = (TextView) findViewById(R.id.btn_reblog);
            btnComment = (TextView) findViewById(R.id.btn_comment);
            layoutTitle = (ViewGroup) findViewById(R.id.layout_detail_title);

            // retrieve this post - return false if not found
            mPost = ReaderPostTable.getPost(mBlogId, mPostId);
            if (mPost==null)
                return false;

            postHtml = getPostHtml(mPost);

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mIsPostTaskRunning = false;

            if (!result) {
                /*
                 * TODO: post couldn't be loaded, which means it should be retrieved from server
                 */
                txtTitle.setText(R.string.reader_title_err_unable_to_load_post);
                txtSource.setVisibility(View.GONE);
                imgAvatar.setImageResource(R.drawable.ic_error);
                return;
            }

            if (mPost.hasTitle()) {
                txtTitle.setText(mPost.getTitle());
            } else {
                txtTitle.setText(R.string.reader_untitled_post);
            }

            // tapping title layout opens post in browser
            layoutTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(ReaderPostDetailActivity.this, mPost.getUrl());
                }
            });

            // blog name / author name / date
            txtSource.setText(mPost.getSource());

            if (mPost.hasPostAvatar()) {
                int avatarSz = getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_medium);
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);
                imgAvatar.setVisibility(View.VISIBLE);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }

            // enable reblogging wp posts
            btnReblog.setVisibility(mPost.isWP() ? View.VISIBLE : View.GONE);
            btnReblog.setSelected(mPost.isRebloggedByCurrentUser);
            if (mPost.isWP()) {
                btnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doPostReblog(btnReblog, mPost);
                    }
                });
            }

            // enable adding a comment if comments are open on this post
            if (mPost.isWP() && mPost.isCommentsOpen) {
                btnComment.setVisibility(View.VISIBLE);
                btnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleShowAddCommentBox();
                    }
                });
            } else {
                btnComment.setVisibility(View.GONE);
            }

            // webView settings must be configured on main thread - note that while JavaScript is
            // required for embedded videos, it's disabled since it's a security risk:
            //    http://developer.android.com/training/articles/security-tips.html#WebView
            // note: even with JavaScript enabled video embeds are unreliable (some work, some don't)
            webView.getSettings().setJavaScriptEnabled(false);
            webView.getSettings().setUserAgentString(Constants.USER_AGENT);

            // webView is hidden at design time, don't show it until the page finishes loading so it
            // has time to layout the post before it appears...
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (webView.getVisibility()!=View.VISIBLE)
                        webView.setVisibility(View.VISIBLE);
                }
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // open clicked urls in default browser or else urls will open in this webView,
                    // but only do this when webView has loaded (is visible) - have seen some posts
                    // containing iframes automatically try to open urls (without being clicked)
                    // before the page has loaded
                    if (view.getVisibility()==View.VISIBLE) {
                        ReaderActivityLauncher.openUrl(ReaderPostDetailActivity.this, url);
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            //...but force it to appear after a few seconds to ensure user never has to be faced
            // with a blank post for too long (very important on slow connections)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (webView.getVisibility()!=View.VISIBLE) {
                        webView.setVisibility(View.VISIBLE);
                        ReaderLog.w("forced webView to appear before page finished");
                    }
                }
            }, 2500);

            // detect image taps so we can open images in the photo viewer activity
            webView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction()==MotionEvent.ACTION_UP) {
                        HitTestResult hr = ((WebView)v).getHitTestResult();
                        if (hr!=null && (hr.getType()==HitTestResult.IMAGE_TYPE || hr.getType()==HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                            String imageUrl = hr.getExtra();
                            if (imageUrl==null)
                                return false;
                            // skip if image is a file: reference - this will be the video overlay, ie:
                            // file:///android_res/drawable/ic_reader_video_overlay.png
                            if (imageUrl.startsWith("file:"))
                                return false;
                            // skip if image is a video thumbnail (see processVideos)
                            if (mVideoThumbnailUrls.contains(imageUrl))
                                return false;
                            // skip if image is a VideoPress thumbnail (anchor around thumbnail will
                            // take user to actual video - see ReaderPost.cleanupVideoPress)
                            if (imageUrl.contains("videos.files."))
                                return false;
                            showPhotoViewer(imageUrl);
                            return true;
                        }
                    }
                    return false;
                }
            });

            // IMPORTANT: must use loadDataWithBaseURL() rather than loadData() since the latter often fails
            // https://code.google.com/p/android/issues/detail?id=4401
            webView.loadDataWithBaseURL(null, postHtml, "text/html", "UTF-8", null);

            // only show action buttons for WP posts
            mLayoutActions.setVisibility(mPost.isWP() ? View.VISIBLE : View.GONE);

            // make sure the adapter is assigned now that we've retrieved the post and updated views
            if (!hasCommentAdapter())
                getListView().setAdapter(getCommentAdapter());

            refreshFollowed();
            refreshLikes(false);
            refreshComments();

            // get the latest info for this post if we haven't updated it already
            if (!mHasAlreadyUpdatedPost) {
                updatePost();
                mHasAlreadyUpdatedPost = true;
            }

            // show the listView now that post has loaded and views have been updated
            getListView().setVisibility(View.VISIBLE);
        }
    }
}
