package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

public class CommentsListFragment extends Fragment {
    protected boolean shouldSelectAfterLoad = false;
    private boolean mIsRetrievingComments = false;
    private boolean mCanLoadMoreComments = true;

    private GetRecentCommentsTask mGetCommentsTask;
    private OnCommentSelectedListener mOnCommentSelectedListener;
    private OnAnimateRefreshButtonListener mOnAnimateRefreshButton;
    private CommentActions.OnCommentChangeListener mOnCommentChangeListener;
    private ProgressBar mProgressLoadMore;
    private ListView mListView;
    private CommentAdapter mCommentAdapter;

    private ActionMode mActionMode;

    // context menu IDs
    protected static final int MENU_ID_APPROVED = 100;
    protected static final int MENU_ID_UNAPPROVED = 101;
    protected static final int MENU_ID_SPAM = 102;
    protected static final int MENU_ID_DELETE = 103;
    protected static final int MENU_ID_EDIT = 105;

    private static final int COMMENTS_PER_PAGE = 30;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    private ListView getListView() {
        return mListView;
    }

    private CommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            // adapter calls this to request more comments from server when it reaches the end
            CommentAdapter.OnLoadMoreListener loadMoreListener = new CommentAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMoreComments && !mIsRetrievingComments)
                        refreshComments(true);
                }
            };

            // adapter calls this when selected comments have changed (CAB)
            CommentAdapter.OnSelectedItemsChangeListener changeListener = new CommentAdapter.OnSelectedItemsChangeListener() {
                @Override
                public void onSelectedItemsChanged() {
                    if (mActionMode != null) {
                        updateActionModeTitle();
                        mActionMode.invalidate();
                    }
                }
            };

            mCommentAdapter = new CommentAdapter(getActivity(), loadMoreListener, changeListener);
        }
        return mCommentAdapter;
    }

    protected boolean loadComments() {
        return getCommentAdapter().loadComments();
    }

    protected int getSelectedCommentCount() {
        return getCommentAdapter().getSelectedCommentCount();
    }

    protected void clear() {
        getCommentAdapter().clear();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setUpListView();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnCommentSelectedListener = (OnCommentSelectedListener) activity;
            mOnAnimateRefreshButton = (OnAnimateRefreshButtonListener) activity;
            mOnCommentChangeListener = (CommentActions.OnCommentChangeListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString() + " must implement Callback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_comments_fragment, container, false);

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        // progress bar that appears when loading more comments
        mProgressLoadMore = (ProgressBar) view.findViewById(R.id.progress_loading);
        mProgressLoadMore.setVisibility(View.GONE);

        return view;
    }

    private void dismissDialog(int id) {
        if (!hasActivity())
            return;
        try {
            getActivity().dismissDialog(id);
        } catch (IllegalArgumentException e) {
            // raised when dialog wasn't created
        }
    }

    @SuppressWarnings("unchecked")
    private boolean moderateSelectedComments(CommentStatus newStatus) {
        final CommentList selectedComments = getCommentAdapter().getSelectedComments();
        final CommentList updateComments = new CommentList();

        // build list of comments whose status is different than passed
        for (Comment comment: selectedComments) {
            if (comment.getStatusEnum() != newStatus)
                updateComments.add(comment);
        }
        if (updateComments.size() == 0)
            return false;

        if (!NetworkUtils.checkConnection(getActivity()))
            return false;

        getActivity().showDialog(CommentsActivity.ID_DIALOG_MODERATING);
        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList moderatedComments) {
                if (!hasActivity())
                    return;
                finishActionMode();
                dismissDialog(CommentsActivity.ID_DIALOG_MODERATING);
                if (moderatedComments.size() > 0) {
                    getCommentAdapter().clearSelectedComments();
                    getCommentAdapter().replaceComments(moderatedComments);
                    // update the comment counter on the menu drawer
                    if (getActivity() instanceof  WPActionBarActivity)
                        ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        };

        CommentActions.moderateComments(WordPress.getCurrentLocalTableBlogId(), updateComments, newStatus, listener);
        return true;
    }

    private void confirmSpamComments() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dlg_confirm_spam_comments);
        builder.setTitle(R.string.spam);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.spam_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                moderateSelectedComments(CommentStatus.SPAM);
            }
        });
        builder.setNegativeButton(R.string.spam_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void confirmDeleteComments() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dlg_confirm_delete_comments);
        builder.setTitle(R.string.delete);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.delete_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                deleteSelectedComments();
            }
        });
        builder.setNegativeButton(R.string.delete_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean deleteSelectedComments() {
        if (!NetworkUtils.checkConnection(getActivity()))
            return false;

        final CommentList selectedComments = getCommentAdapter().getSelectedComments();
        getActivity().showDialog(CommentsActivity.ID_DIALOG_DELETING);
        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList deletedComments) {
                if (!hasActivity())
                    return;
                finishActionMode();
                dismissDialog(CommentsActivity.ID_DIALOG_DELETING);
                if (deletedComments.size() > 0) {
                    getCommentAdapter().clearSelectedComments();
                    getCommentAdapter().deleteComments(deletedComments);
                    // update the comment counter on the menu drawer
                    if (getActivity() instanceof  WPActionBarActivity)
                        ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        };

        CommentActions.deleteComments(WordPress.getCurrentLocalTableBlogId(), selectedComments, listener);
        return true;
    }

    private void setUpListView() {
        ListView listView = this.getListView();
        listView.setAdapter(getCommentAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    Comment comment = (Comment) getCommentAdapter().getItem(position);
                    mOnCommentSelectedListener.onCommentSelected(comment);
                    getListView().invalidateViews();
                } else {
                    getCommentAdapter().toggleItemSelected(position);
                    updateActionModeTitle();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // enable CAB if it's not already enabled
                if (mActionMode == null) {
                    if (getActivity() instanceof WPActionBarActivity) {
                        ((WPActionBarActivity) getActivity()).startActionMode(new ActionModeCallback());
                        getCommentAdapter().setEnableSelection(true);
                        getCommentAdapter().setItemSelected(position, true);
                        updateActionModeTitle();
                    }
                } else {
                    getCommentAdapter().toggleItemSelected(position);
                }
                return true;
            }
        });
    }

    protected void refreshComments() {
        refreshComments(false);
    }
    private void refreshComments(boolean loadMore) {
        mGetCommentsTask = new GetRecentCommentsTask(loadMore);
        mGetCommentsTask.execute();
    }

    protected void cancelCommentsTask() {
        if (mGetCommentsTask != null && !mGetCommentsTask.isCancelled())
            mGetCommentsTask.cancel(true);
    }

    /*
     * task to retrieve latest comments from server
     */
    private class GetRecentCommentsTask extends AsyncTask<Void, Void, CommentList> {
        boolean isError;
        boolean isLoadingMore;

        private GetRecentCommentsTask(boolean loadMore) {
            isLoadingMore = loadMore;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsRetrievingComments = true;
            if (isLoadingMore) {
                showLoadingProgress();
            } else {
                mOnAnimateRefreshButton.onAnimateRefreshButton(true);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsRetrievingComments = false;
        }

        @Override
        protected CommentList doInBackground(Void... args) {
            if (!hasActivity())
                return null;

            Map<String, Object> hPost = new HashMap<String, Object>();
            if (isLoadingMore) {
                int numExisting = getCommentAdapter().getCount();
                hPost.put("offset", numExisting);
                hPost.put("number", COMMENTS_PER_PAGE);
            } else {
                hPost.put("number", COMMENTS_PER_PAGE);
            }

            Object[] params = { WordPress.currentBlog.getRemoteBlogId(),
                                WordPress.currentBlog.getUsername(),
                                WordPress.currentBlog.getPassword(),
                                hPost };
            try {
                return ApiHelper.refreshComments(getActivity(), params);
            } catch (XMLRPCException e) {
                isError = true;
                return null;
            }
        }

        protected void onPostExecute(CommentList comments) {
            mIsRetrievingComments = false;
            if (!hasActivity())
                return;

            if (isLoadingMore) {
                hideLoadingProgress();
            } else {
                mOnAnimateRefreshButton.onAnimateRefreshButton(false);
            }

            if (isCancelled())
                return;

            mCanLoadMoreComments = (comments != null && comments.size() > 0);

            // result will be null on error OR if no more comments exists
            if (comments == null) {
                if (isError && !getActivity().isFinishing())
                    ToastUtils.showToast(getActivity(), R.string.error_refresh_comments, ToastUtils.Duration.LONG);
                return;
            }

            if (comments.size() > 0)
                getCommentAdapter().loadComments();
        }
    }

    public interface OnCommentSelectedListener {
        public void onCommentSelected(Comment comment);
    }

    public interface OnAnimateRefreshButtonListener {
        public void onAnimateRefreshButton(boolean start);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    /*
     * show/hide progress bar which appears at the bottom when loading more comments
     */
    private void showLoadingProgress() {
        if (hasActivity() && mProgressLoadMore != null)
            mProgressLoadMore.setVisibility(View.VISIBLE);
    }
    private void hideLoadingProgress() {
        if (hasActivity() && mProgressLoadMore != null)
            mProgressLoadMore.setVisibility(View.GONE);
    }

    /*
     * contextual ActionBar (CAB)
     */
    private void updateActionModeTitle() {
        if (mActionMode == null)
            return;
        int numSelected = getSelectedCommentCount();
        if (numSelected > 0) {
            mActionMode.setTitle(Integer.toString(numSelected));
        } else {
            mActionMode.setTitle("");
        }
        mActionMode.invalidate();
    }

    private void finishActionMode() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, com.actionbarsherlock.view.Menu menu) {
            mActionMode = actionMode;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_comments_cab, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, com.actionbarsherlock.view.Menu menu) {
            CommentList selectedComments = getCommentAdapter().getSelectedComments();
            boolean hasSelection = (selectedComments.size() > 0);
            boolean hasApproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.APPROVED);
            boolean hasUnapproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.UNAPPROVED);
            boolean hasSpam = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.SPAM);

            MenuItem mnuApprove = menu.findItem(R.id.menu_approve);
            MenuItem mnuUnapprove = menu.findItem(R.id.menu_unapprove);
            MenuItem mnuSpam = menu.findItem(R.id.menu_spam);
            MenuItem mnuTrash = menu.findItem(R.id.menu_trash);

            mnuApprove.setEnabled(hasUnapproved || hasSpam);
            mnuUnapprove.setEnabled(hasApproved);
            mnuSpam.setEnabled(hasSelection);
            mnuTrash.setEnabled(hasSelection);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, com.actionbarsherlock.view.MenuItem menuItem) {
            int numSelected = getSelectedCommentCount();
            if (numSelected == 0)
                return false;

            // note that approve/disapprove happen without confirmation, but confirmation is required
            // before comments are deleted/spammed
            switch (menuItem.getItemId()) {
                case R.id.menu_approve :
                    moderateSelectedComments(CommentStatus.APPROVED);
                    return true;
                case R.id.menu_unapprove :
                    moderateSelectedComments(CommentStatus.UNAPPROVED);
                    return true;
                case R.id.menu_spam :
                    confirmSpamComments();
                    return true;
                case R.id.menu_trash :
                    confirmDeleteComments();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getCommentAdapter().setEnableSelection(false);
            mActionMode = null;
        }
    }
}