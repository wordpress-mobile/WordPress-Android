package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.BlogPairId;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.util.AppLog;

public class CommentsActivity extends WPActionBarActivity
        implements OnCommentSelectedListener,
                   NotificationFragment.OnPostClickListener,
                   CommentActions.OnCommentChangeListener {
    private static final String KEY_HIGHLIGHTED_COMMENT_ID = "highlighted_comment_id";
    private static final String KEY_SELECTED_COMMENT_ID = "selected_comment_id";
    private static final String KEY_SELECTED_POST_ID = "selected_post_id";
    private boolean mDualPane;
    private long mSelectedCommentId;
    private boolean mCommentSelected;
    private BlogPairId mSelectedReaderPost;
    private BlogPairId mTmpSelectedReaderPost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        createMenuDrawer(R.layout.comment_activity);
        View detailView = findViewById(R.id.fragment_comment_detail);
        mDualPane = detailView != null && detailView.getVisibility() == View.VISIBLE;
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.tab_comments));
        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        restoreSavedInstance(savedInstanceState);
    }

    private void restoreSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // restore the highlighted comment
            long commentId = savedInstanceState.getLong(KEY_HIGHLIGHTED_COMMENT_ID);
            if (commentId != 0) {
                if (hasListFragment()) {
                    // on dual pane mode, the highlighted comment is also selected
                    getListFragment().setHighlightedCommentId(commentId);
                }
                if (mDualPane) {
                    onCommentSelected(commentId);
                }
            }
            // restore the selected comment
            if (!mDualPane) {
                commentId = savedInstanceState.getLong(KEY_SELECTED_COMMENT_ID);
                if (commentId != 0) {
                    onCommentSelected(commentId);
                }
            }
            // restore the post detail fragment if one was selected
            BlogPairId selectedPostId = (BlogPairId) savedInstanceState.get(KEY_SELECTED_POST_ID);
            if (selectedPostId != null) {
                showReaderFragment(selectedPostId.getRemoteBlogId(), selectedPostId.getId());
            }
        }
    }

    /**
     * Called by CommentAdapter after comment adapter first load
     */
    public void commentAdapterFirstLoad() {
        // if dual pane mode and no selected comments, select the first comment in the list
        if (mDualPane && !mCommentSelected && hasListFragment()) {
            long firstCommentId = getListFragment().getFirstCommentId();
            onCommentSelected(firstCommentId);
        }
        // used to scroll the list view after it has been created
        if (mSelectedCommentId != 0 && hasListFragment()) {
            getListFragment().setHighlightedCommentId(mSelectedCommentId);
        }
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();

        // clear the backstack
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // clear and update the comment list
        if (hasListFragment()) {
            getListFragment().onBlogChanged();
            getListFragment().clear();
            reloadCommentList();
            updateCommentList();
        }

        // clear comment detail
        if (hasDetailFragment()) {
            getDetailFragment().clear();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.comments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDualPane) {
                    // let WPActionBarActivity handle it (toggles menu drawer)
                    return super.onOptionsItemSelected(item);
                } else {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                        return true;
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
                    // This is ugly, but onBackStackChanged is not called just after a fragment commit.
                    // In a 2 commits in a row case, onBackStackChanged is called twice but after the
                    // 2 commits. That's why mSelectedReaderPost can't be affected correctly after the first commit.
                    switch (backStackEntryCount) {
                        case 2:
                            // 2 entries means we're showing the associated post in the reader detail fragment
                            // (can't happen in dual pane mode)
                            mSelectedReaderPost = mTmpSelectedReaderPost;
                            break;
                        case 1:
                            // In dual pane mode, 1 entry means:
                            // we're showing the associated post in the reader detail fragment
                            // In single pane mode, 1 entry means:
                            // we're showing the comment fragment on top of comment list
                            if (mDualPane) {
                                mSelectedReaderPost = mTmpSelectedReaderPost;
                            } else {
                                mSelectedReaderPost = null;
                            }
                            break;
                        case 0:
                            mMenuDrawer.setDrawerIndicatorEnabled(true);
                            mSelectedCommentId = 0;
                            mSelectedReaderPost = null;
                            break;
                    }
                }
            };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.COMMENTS, "comment activity new intent");
    }

    /*
     * called from comment list & comment detail when comments are moderated/replied/trashed
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
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_detail));
        if (fragment == null) {
            return null;
        }
        return (CommentDetailFragment) fragment;
    }

    private boolean hasDetailFragment() {
        return (getDetailFragment() != null);
    }

    private CommentsListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_list));
        if (fragment == null) {
            return null;
        }
        return (CommentsListFragment) fragment;
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    private ReaderPostDetailFragment getReaderFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_detail));
        if (fragment == null)
            return null;
        return (ReaderPostDetailFragment)fragment;
    }

    private boolean hasReaderFragment() {
        return (getReaderFragment() != null);
    }

    void showReaderFragment(long remoteBlogId, long postId) {
        mTmpSelectedReaderPost = new BlogPairId(remoteBlogId, postId);
        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();

        Fragment fragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        ft.add(R.id.layout_fragment_container, fragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (hasDetailFragment())
            ft.hide(getDetailFragment());
        ft.commit();
    }

    /*
     * called from comment list when user taps a comment
     */
    @Override
    public void onCommentSelected(long commentId) {
        mSelectedCommentId = commentId;
        FragmentManager fm = getSupportFragmentManager();
        if (fm == null) {
            return;
        }
        fm.executePendingTransactions();
        mCommentSelected = true;
        CommentDetailFragment detailFragment = getDetailFragment();
        CommentsListFragment listFragment = getListFragment();

        if (mDualPane) {
            // dual pane mode with list/detail side-by-side - remove the reader fragment if it exists,
            // then show this comment in the detail view and highlight it in the list view
            if (hasReaderFragment()) {
                fm.popBackStackImmediate();
            }
            detailFragment.setComment(WordPress.getCurrentLocalTableBlogId(), commentId);
            if (listFragment != null) {
                listFragment.setHighlightedCommentId(commentId);
            }
        } else {
            FragmentTransaction ft = fm.beginTransaction();
            String tagForFragment = getString(R.string.fragment_tag_comment_detail);
            detailFragment = CommentDetailFragment.newInstance(WordPress.getCurrentLocalTableBlogId(),
                    commentId);
            ft.add(R.id.layout_fragment_container, detailFragment, tagForFragment).addToBackStack(tagForFragment)
              .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if (listFragment != null) {
                listFragment.setHighlightedCommentId(commentId);
                ft.hide(listFragment);
            }
            ft.commitAllowingStateLoss();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        }
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        showReaderFragment(remoteBlogId, postId);
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
    void updateCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.updateComments(false);
            listFragment.setRefreshing(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // https://code.google.com/p/android/issues/detail?id=19917
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }

        // retain the id of the highlighted and selected comments
        if (mSelectedCommentId != 0) {
            outState.putLong(KEY_SELECTED_COMMENT_ID, mSelectedCommentId);
        }
        if (mSelectedReaderPost != null) {
            outState.putSerializable(KEY_SELECTED_POST_ID, mSelectedReaderPost);
        }
        if (hasListFragment()) {
            long commentId = getListFragment().getHighlightedCommentId();
            if (commentId != 0) {
                outState.putLong(KEY_HIGHLIGHTED_COMMENT_ID, commentId);
            }
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
