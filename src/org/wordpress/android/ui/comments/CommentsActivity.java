package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.OnAnimateRefreshButtonListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.util.AppLog;

public class CommentsActivity extends WPActionBarActivity
        implements OnCommentSelectedListener,
                   OnAnimateRefreshButtonListener,
                   CommentActions.OnCommentChangeListener {

    private CommentsListFragment mCommentListFragment;
    private MenuItem mRefreshMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.comments);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.tab_comments));

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mCommentListFragment = (CommentsListFragment) fm.findFragmentById(R.id.commentList);

        WordPress.currentComment = null;

        if (savedInstanceState != null)
            popCommentDetail();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        if (mCommentListFragment != null)
            mCommentListFragment.clear();
        updateCommentList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.basic_menu, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (mShouldAnimateRefreshButton) {
            mShouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(mRefreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            popCommentDetail();
            updateCommentList();
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

    protected void popCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.COMMENTS, "comment activity new intent");
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
                reloadCommentList();
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
                ft.hide(mCommentListFragment);
                f = CommentDetailFragment.newInstance(WordPress.getCurrentLocalTableBlogId(), comment.commentID);
                ft.add(R.id.commentDetailFragmentContainer, f);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                // tablet mode with list/detail side-by-side - show this comment in the detail view,
                // and highlight it in the list view
                f.setComment(WordPress.getCurrentLocalTableBlogId(), comment.commentID);
                if (mCommentListFragment != null)
                    mCommentListFragment.setHighlightedCommentId(comment.commentID);
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

    /*
     * reload the comment list from existing data
     */
    private void reloadCommentList() {
        if (mCommentListFragment != null)
            mCommentListFragment.loadComments();
    }

    /*
     * tell the comment list to get recent comments from server
     */
    private void updateCommentList() {
        if (mCommentListFragment != null)
            mCommentListFragment.updateComments(false);
    }

    @Override
    public void onAnimateRefreshButton(boolean start) {
        if (start) {
            mShouldAnimateRefreshButton = true;
            this.startAnimatingRefreshButton(mRefreshMenuItem);
        } else {
            mShouldAnimateRefreshButton = false;
            this.stopAnimatingRefreshButton(mRefreshMenuItem);
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
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }

}
