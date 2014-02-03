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
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.OnAnimateRefreshButtonListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;

public class CommentsActivity extends WPActionBarActivity
        implements OnCommentSelectedListener,
                   OnAnimateRefreshButtonListener,
                   CommentActions.OnCommentChangeListener {

    private CommentsListFragment commentList;
    private boolean fromNotification = false;
    private MenuItem refreshMenuItem;

    protected static final int ID_DIALOG_MODERATING = 1;
    protected static final int ID_DIALOG_DELETING = 3;

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

        if (fromNotification)
            refreshCommentList();

        if (savedInstanceState != null)
            popCommentDetail();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        if (commentList != null)
            commentList.clear();
        refreshCommentList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.basic_menu, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (mShouldAnimateRefreshButton) {
            mShouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            popCommentDetail();
            refreshCommentList();
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

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    private void popCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        commentList.cancelCommentsTask();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean fromNotification = extras.getBoolean("fromNotification");
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

    /*
     * called from comment list & comment detail when comments are moderated, added, or deleted
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom) {
        // update the comment counter on the menu drawer
        updateMenuDrawer();

        switch (changedFrom) {
            case COMMENT_LIST:
                reloadCommentDetail();
                break;
            case COMMENT_DETAIL:
                refreshCommentList();
                break;
        }
    }

    /*
     * called from comment list when user taps a comment
     */
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
                f = CommentDetailFragment.newInstance(WordPress.getCurrentLocalTableBlogId(), comment);
                ft.add(R.id.commentDetailFragmentContainer, f);
                //ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                f.setComment(WordPress.getCurrentLocalTableBlogId(), comment);
            }
        }
    }

    /*
     * reload the comment in the detail view if it's showing
     */
    private void reloadCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment fragment = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (fragment != null)
            fragment.reloadComment();
    }

    private void refreshCommentList() {
        if (commentList != null)
            commentList.refreshComments();
    }

    @Override
    public void onAnimateRefreshButton(boolean start) {
        if (start) {
            mShouldAnimateRefreshButton = true;
            this.startAnimatingRefreshButton(refreshMenuItem);
        } else {
            mShouldAnimateRefreshButton = false;
            this.stopAnimatingRefreshButton(refreshMenuItem);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // https://code.google.com/p/android/issues/detail?id=19917
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_MODERATING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.getSelectedCommentCount() > 1) {
                loadingDialog.setMessage(getResources().getText(R.string.moderating_comments));
            } else {
                loadingDialog.setMessage(getResources().getText(R.string.moderating_comment));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_DELETING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.getSelectedCommentCount() > 1) {
                loadingDialog.setMessage(getResources().getText(R.string.deleting_comments));
            } else {
                loadingDialog.setMessage(getResources().getText(R.string.deleting_comment));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else {
            return super.onCreateDialog(id);
        }
    }

}
