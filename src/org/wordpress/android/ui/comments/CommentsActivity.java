package org.wordpress.android.ui.comments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentListFragment.CommentAsyncModerationReturnListener;
import org.wordpress.android.ui.comments.CommentListFragment.CommentListFragmentListener;
import org.wordpress.android.ui.comments.CommentListFragment.OnAnimateRefreshButtonListener;

import java.util.ArrayList;

public class CommentsActivity extends WPActionBarActivity implements CommentAsyncModerationReturnListener,
        CommentListFragmentListener, OnAnimateRefreshButtonListener,
        CommentDetailFragment.OnCommentChangeListener, ActionMode.Callback {

    protected int id;

    private CommentListFragment commentList;
    private boolean fromNotification = false;
    private MenuItem refreshMenuItem;
    private ActionMode mActionMode;

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
        commentList = (CommentListFragment) fm.findFragmentById(R.id.commentList);

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

    /*
     * called from CommentDetailFragment when comment is moderated - replace the
     * existing comment in the list with the passed one
     */
    @Override
    public void onCommentModerated(Comment comment) {
        commentList.replaceComment(comment);
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
    public void onAsyncModerationReturnSuccess(CommentStatus commentModerationStatusType) {
        if (commentModerationStatusType == CommentStatus.APPROVED
                || commentModerationStatusType == CommentStatus.UNAPPROVED) {
            if (mActionMode != null) { mActionMode.invalidate(); }
        } else if (commentModerationStatusType == CommentStatus.SPAM
                || commentModerationStatusType == CommentStatus.TRASH) {
            if (mActionMode != null) { mActionMode.finish(); }
        }
    }

    @Override
    public void onAsyncModerationReturnFailure(CommentStatus commentModerationStatusType) { }

    @Override
    public void onCommentClicked(Comment comment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();
        CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);

        if (comment != null && fm.getBackStackEntryCount() == 0) {
            if (f == null || !f.isInLayout()) {
                WordPress.currentComment = comment;
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(commentList);
                f = CommentDetailFragment.newInstance(WordPress.getCurrentBlogId(), comment);
                ft.add(R.id.commentDetailFragmentContainer, f);
                //ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                f.setComment(WordPress.getCurrentBlogId(), comment);
            }
        }
    }

    /*
     * refresh the comment in the detail view if it's showing
     */
    private void refreshCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment fragment = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (fragment == null)
            return;

        fragment.refreshComment();
    }

    /*
     * clear the comment in the detail view if it's showing
     */
    private void clearCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentDetailFragment fragment = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (fragment == null)
            return;
        fragment.clearComment();
    }

    private void refreshCommentList() {
        if (commentList != null)
            commentList.refreshComments();
    }

    /**
     * these three methods implement OnCommentChangedListener and are triggered from this activity,
     * the list fragment, and the detail fragment whenever a comment is changed
     */
    @Override
    public void onCommentAdded() {
        refreshCommentList();
    }

    @Override
    public void onCommentDeleted() {
        refreshCommentList();
        clearCommentDetail();
    }
    @Override
    public void onCommentModerated() {
        refreshCommentList();
        refreshCommentDetail();
    }
    @Override
    public void onCommentSelected(int selectedCommentCount) {
        // Check the cases when we are entering/exiting into/out of multi-select mode
        if (selectedCommentCount > 0 && mActionMode == null) {
            mActionMode = getSherlock().startActionMode(this);
        } else if (selectedCommentCount == 0 && mActionMode != null) {
            mActionMode.finish();
        }

        // update contextual action bar title + action items
        if (mActionMode != null) {
            if (selectedCommentCount == 1) {
                mActionMode.setTitle(getString(R.string.reader_label_comment_count_singular));
            } else {
                mActionMode.setTitle(getString(R.string.reader_label_comment_count_plural,
                        selectedCommentCount));
            }
            mActionMode.invalidate();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.comments_multiselect, menu);
        mActionMode = mode;

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean retVal;
        ArrayList<Integer> commentListSelectedCommentIdArray = commentList.getSelectedCommentIdArray();
        int numCommentsSelected = commentListSelectedCommentIdArray.size();
        int selectedCommentStatusTypeBitMask = 0;

        if (mActionMode != null) {
            if (numCommentsSelected <= 0) { mode.finish(); }

            menu.findItem(R.id.comments_cab_approve).setVisible(true);
            menu.findItem(R.id.comments_cab_unapprove).setVisible(true);
            menu.findItem(R.id.comments_cab_spam).setVisible(true);
            menu.findItem(R.id.comments_cab_delete).setVisible(true);

            /* JCO - 12/10/2013 - If we start displaying a "SPAM" or "TRASH" comment list then the
             * following associated code should be uncommented.
             */
            if (numCommentsSelected >= 1) {
                CommentStatus.clearSelectedCommentStatusTypeCount();
                for(Comment comment : commentList.getSelectedCommentArray()) {
                    CommentStatus.incrementSelectedCommentStatusTypeCount(comment.getStatusEnum());
                }

                if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.APPROVED) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.APPROVED.getOffset();
                }
                if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.UNAPPROVED) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.UNAPPROVED.getOffset();
                }
                /*if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.SPAM) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.SPAM.getOffset();
                }*/
                /*if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.TRASH) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.TRASH.getOffset();
                }*/
                /*if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.UNKNOWN) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.UNKNOWN.getOffset();
                }*/

                if (selectedCommentStatusTypeBitMask == 1 << CommentStatus.APPROVED.getOffset()) {
                    menu.findItem(R.id.comments_cab_approve).setVisible(false);
                } else if (selectedCommentStatusTypeBitMask == 1 << CommentStatus.UNAPPROVED.getOffset()) {
                    menu.findItem(R.id.comments_cab_unapprove).setVisible(false);
                }
                /*else if (selectedCommentStatusTypeBitMask == CommentStatus.SPAM.getOffset()) {
                    menu.findItem(R.id.comments_cab_spam).setVisible(false);
                }*/
                /*else if (selectedCommentStatusTypeBitMask == CommentStatus.TRASH.getOffset()) {
                    menu.findItem(R.id.comments_cab_delete).setVisible(false);
                }*/
            }

            retVal = true;
        }
        else { retVal = false; }

        return retVal;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean retVal = true;
        int id = item.getItemId();

        switch (id) {
            // Both Single and Multi Item Select Actions
            case R.id.comments_cab_delete:
                commentList.deleteComments();
                break;
            case R.id.comments_cab_approve:
                commentList.moderateComments(CommentStatus.APPROVED);
                break;
            case R.id.comments_cab_unapprove:
                commentList.moderateComments(CommentStatus.UNAPPROVED);
                break;
            case R.id.comments_cab_spam:
                commentList.moderateComments(CommentStatus.SPAM);
                break;
            default:
                retVal = false;
        }
        return retVal;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        commentList.clearSelectedComments();
        mActionMode = null;
    }
}
