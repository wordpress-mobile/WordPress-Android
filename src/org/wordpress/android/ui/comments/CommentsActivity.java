package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;

public class CommentsActivity extends WPActionBarActivity
        implements OnCommentSelectedListener,
                   OnAnimateRefreshButtonListener,
                   CommentDetailFragment.OnPostClickListener,
                   CommentActions.OnCommentChangeListener {

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

        WordPress.currentComment = null;
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        if (hasListFragment()) {
            getListFragment().clear();
            updateCommentList();
        }
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
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                updateCommentList();
                return true;
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    // TODO: DOESN'T WORK - this is used to close the detail fragment when user trashes comment
    protected void popCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        if (hasDetailFragment() && fm.getBackStackEntryCount() > 0)
            fm.popBackStack();
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
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        // update the comment counter on the menu drawer
        updateMenuDrawer();

        switch (changedFrom) {
            case COMMENT_LIST:
                reloadCommentDetail();
                break;
            case COMMENT_DETAIL:
                switch (changeType) {
                    case TRASHED:
                        updateCommentList();
                        // remove the detail view since comment was deleted
                        FragmentManager fm = getSupportFragmentManager();
                        if (fm.getBackStackEntryCount() > 0) {
                            fm.popBackStack();
                        }
                        break;
                    case REPLIED:
                        updateCommentList();
                        break;
                    default:
                        reloadCommentList();
                        break;
                }
                break;
        }
    }

    private CommentDetailFragment getDetailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_comment_detail);
        if (fragment == null)
            return null;
        return (CommentDetailFragment)fragment;
    }

    private boolean hasDetailFragment() {
        return (getDetailFragment() != null);
    }

    private CommentsListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_comment_list);
        if (fragment == null)
            return null;
        return (CommentsListFragment)fragment;
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    /*
     * called from comment list when user taps a comment
     */
    @Override
    public void onCommentSelected(Comment comment) {
        if (comment == null)
            return;

        // if (fm.getBackStackEntryCount() > 0) return
        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();
        CommentDetailFragment detailFragment = getDetailFragment();
        CommentsListFragment listFragment = getListFragment();

        if (detailFragment == null) {
            WordPress.currentComment = comment;
            FragmentTransaction ft = fm.beginTransaction();
            if (listFragment != null)
                ft.hide(listFragment);
            detailFragment = CommentDetailFragment.newInstance(WordPress.getCurrentLocalTableBlogId(), comment.commentID);
            ft.add(R.id.layout_comment_fragment_container, detailFragment);
            ft.addToBackStack(null);
            ft.commitAllowingStateLoss();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            // tablet mode with list/detail side-by-side - show this comment in the detail view,
            // and highlight it in the list view
            detailFragment.setComment(WordPress.getCurrentLocalTableBlogId(), comment.commentID);
            if (listFragment != null)
                listFragment.setHighlightedCommentId(comment.commentID);
        }
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in the reader
     */
    @Override
    public void onPostClicked(long remoteBlogId, long postId) {
        // TODO: show as a fragment
        ReaderActivityLauncher.showReaderPostDetail(this, remoteBlogId, postId);
    }

    /*
     * reload the comment in the detail view if it's showing
     */
    private void reloadCommentDetail() {
        CommentDetailFragment detailFragment = getDetailFragment();
        if (detailFragment != null)
            detailFragment.reloadComment();
    }

    /*
     * reload the comment list from existing data
     */
    private void reloadCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null)
            listFragment.loadComments();
    }

    /*
     * tell the comment list to get recent comments from server
     */
    private void updateCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null)
            listFragment.updateComments(false);
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
