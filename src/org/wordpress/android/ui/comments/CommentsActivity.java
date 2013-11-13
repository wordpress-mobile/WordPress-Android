package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Comment.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.OnAnimateRefreshButtonListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.XMLRPCClient;

public class CommentsActivity extends WPActionBarActivity
        implements OnCommentSelectedListener,
                   CommentDetailFragment.OnCommentChangeListener,
                   OnAnimateRefreshButtonListener {

    protected int id;

    private XMLRPCClient client;
    private CommentsListFragment commentList;
    private boolean fromNotification = false;
    private MenuItem refreshMenuItem;

    public static final int ID_DIALOG_MODERATING = 1;
    public static final int ID_DIALOG_DELETING = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.comments);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.tab_comments));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            fromNotification = extras.getBoolean("fromNotification");
            if (fromNotification) {
                try {
                    WordPress.currentBlog = new Blog(extras.getInt("id"));
                } catch (Exception e) {
                    Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        commentList = (CommentsListFragment) fm.findFragmentById(R.id.commentList);

        WordPress.currentComment = null;

        attemptToSelectComment();
        if (fromNotification)
            commentList.refreshComments();

        if (savedInstanceState != null)
            popCommentDetail();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        commentList.refreshComments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.basic_menu, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            popCommentDetail();
            attemptToSelectComment();
            commentList.refreshComments();
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popCommentDetail();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    protected void popCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (WordPress.currentBlog != null) {
            boolean commentsLoaded = commentList.loadComments(false, false);
            if (!commentsLoaded)
                commentList.refreshComments();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentList.getCommentsTask != null)
            commentList.getCommentsTask.cancel(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean fromNotification = false;
            fromNotification = extras.getBoolean("fromNotification");
            if (fromNotification) {
                try {
                    WordPress.currentBlog = new Blog(extras.getInt("id"));
                } catch (Exception e) {
                    Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onCommentSelected(Comment comment) {

        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);

        if (comment != null && fm.getBackStackEntryCount() == 0) {
            if (f == null || !f.isInLayout()) {
                WordPress.currentComment = comment;
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(commentList);
                f = CommentDetailFragment.newInstance(comment);
                ft.add(R.id.commentDetailFragmentContainer, f);
                //ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                f.setComment(comment);
            }
        }
    }


    /*
     * called from CommentDetailFragment when comment is changed (moderated) - replace the
     * existing comment in the list with the passed one
     */
    @Override
    public void onCommentModified(Comment comment) {
        commentList.replaceComment(comment);
    }

    /*
     * called from CommentListFragment after user selects from ListView's context menu
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getItemId()==CommentsListFragment.MENU_ID_EDIT) {
            /*
             * start activity to edit this comment
             */
            Intent i = new Intent(
                    getApplicationContext(),
                    EditCommentActivity.class);
            startActivityForResult(i, 0);
            return true;
        } else if (item.getItemId()==CommentsListFragment.MENU_ID_DELETE) {
            /*
             * fire background action to delete this comment
             */
            showDialog(ID_DIALOG_DELETING);
            CommentActions.deleteComment(WordPress.currentBlog,
                    WordPress.currentComment,
                    new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(boolean succeeded) {
                            dismissDialog(ID_DIALOG_DELETING);
                            if (succeeded) {
                                commentList.refreshComments();
                                ToastUtils.showToast(CommentsActivity.this, getString(R.string.comment_moderated));
                            } else {
                                ToastUtils.showToast(CommentsActivity.this, getString(R.string.error_moderate_comment));
                            }
                        }
                    });
            return true;
        } else {
            /*
             * remainder are all comment moderation actions
             */
            showDialog(ID_DIALOG_MODERATING);
            final CommentStatus status;
            switch (item.getItemId()) {
                case CommentsListFragment.MENU_ID_APPROVED:
                    status = CommentStatus.APPROVED;
                    break;
                case CommentsListFragment.MENU_ID_UNAPPROVED:
                    status = CommentStatus.PENDING;
                    break;
                case CommentsListFragment.MENU_ID_SPAM:
                    status = CommentStatus.SPAM;
                    break;
                default :
                    return true;
            }

            CommentActions.setCommentStatus(WordPress.currentBlog,
                                            WordPress.currentComment,
                                            status,
                    new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(boolean succeeded) {
                            dismissDialog(ID_DIALOG_MODERATING);
                            if (succeeded) {
                                commentList.refreshComments();
                                ToastUtils.showToast(CommentsActivity.this, getString(R.string.comment_moderated));
                            } else {
                                ToastUtils.showToast(CommentsActivity.this, getString(R.string.error_moderate_comment));
                            }
                        }
                    });

            return true;
        }
    }

    /*
     * called from CommentDetailFragment when comment is replied to (adding a new comment)
     */
    @Override
    public void onCommentAdded() {
        commentList.refreshComments();
    }


    @Override
    public void onAnimateRefreshButton(boolean start) {
        if (start) {
            shouldAnimateRefreshButton = true;
            this.startAnimatingRefreshButton(refreshMenuItem);
        } else {
            this.stopAnimatingRefreshButton(refreshMenuItem);
        }

    }

    private void attemptToSelectComment() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);

        if (f != null && f.isInLayout()) {
            commentList.shouldSelectAfterLoad = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_MODERATING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.checkedCommentTotal <= 1) {
                loadingDialog.setMessage(getResources().getText(
                        R.string.moderating_comment));
            } else {
                loadingDialog.setMessage(getResources().getText(
                        R.string.moderating_comments));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_DELETING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.checkedCommentTotal <= 1) {
                loadingDialog.setMessage(getResources().getText(
                        R.string.deleting_comment));
            } else {
                loadingDialog.setMessage(getResources().getText(
                        R.string.deleting_comments));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else {
            return super.onCreateDialog(id);
        }
    }
}
