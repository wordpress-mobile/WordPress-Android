package org.wordpress.android.ui.comments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions.ChangeType;
import org.wordpress.android.ui.comments.CommentActions.ChangedFrom;
import org.wordpress.android.ui.comments.CommentActions.OnCommentChangeListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

public class CommentsListFragment extends Fragment {
    private boolean mIsUpdatingComments = false;
    private boolean mCanLoadMoreComments = true;
    private boolean mHasAutoRefreshedComments = false;

    private PullToRefreshHelper mPullToRefreshHelper;
    private ListView mListView;
    private View mEmptyView;
    private CommentAdapter mCommentAdapter;
    private ActionMode mActionMode;

    private OnCommentSelectedListener mOnCommentSelectedListener;
    private OnCommentChangeListener mOnCommentChangeListener;

    private static final int COMMENTS_PER_PAGE = 30;
    private static final String KEY_AUTO_REFRESHED = "has_auto_refreshed";

    private ListView getListView() {
        return mListView;
    }

    private CommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            /*
             * called after comments have been loaded
             */
            CommentAdapter.DataLoadedListener dataLoadedListener = new CommentAdapter.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!hasActivity())
                        return;
                    if (isEmpty) {
                        showEmptyView();
                    } else {
                        hideEmptyView();
                    }
                }
            };

            // adapter calls this to request more comments from server when it reaches the end
            CommentAdapter.OnLoadMoreListener loadMoreListener = new CommentAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMoreComments && !mIsUpdatingComments)
                        updateComments(true);
                }
            };

            // adapter calls this when selected comments have changed (CAB)
            CommentAdapter.OnSelectedItemsChangeListener changeListener = new CommentAdapter.OnSelectedItemsChangeListener() {
                @Override
                public void onSelectedItemsChanged() {
                    if (mActionMode != null) {
                        if (getSelectedCommentCount() == 0) {
                            mActionMode.finish();
                        } else {
                            updateActionModeTitle();
                            // must invalidate to ensure onPrepareActionMode is called
                            mActionMode.invalidate();
                        }
                    }
                }
            };

            mCommentAdapter = new CommentAdapter(getActivity(),
                                                 dataLoadedListener,
                                                 loadMoreListener,
                                                 changeListener);
        }
        return mCommentAdapter;
    }

    private boolean hasCommentAdapter() {
        return (mCommentAdapter != null);
    }

    private int getSelectedCommentCount() {
        return getCommentAdapter().getSelectedCommentCount();
    }

    void clear() {
        if (hasCommentAdapter()) {
            getCommentAdapter().clear();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mHasAutoRefreshedComments = savedInstanceState.getBoolean(KEY_AUTO_REFRESHED);
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setUpListView();
        getCommentAdapter().loadComments();
        if (!mHasAutoRefreshedComments) {
            updateComments(false);
            mHasAutoRefreshedComments = true;
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnCommentSelectedListener = (OnCommentSelectedListener) activity;
            mOnCommentChangeListener = (OnCommentChangeListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString() + " must implement Callback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.comment_list_fragment, container, false);

        mListView = (ListView) view.findViewById(android.R.id.list);
        mEmptyView = view.findViewById(android.R.id.empty);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) view.findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        updateComments(false);
                    }
                });
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

    private void moderateSelectedComments(final CommentStatus newStatus) {
        final CommentList selectedComments = getCommentAdapter().getSelectedComments();
        final CommentList updateComments = new CommentList();

        // build list of comments whose status is different than passed
        for (Comment comment: selectedComments) {
            if (comment.getStatusEnum() != newStatus)
                updateComments.add(comment);
        }
        if (updateComments.size() == 0)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final int dlgId;
        switch (newStatus) {
            case APPROVED:
                dlgId = CommentDialogs.ID_COMMENT_DLG_APPROVING;
                break;
            case UNAPPROVED:
                dlgId = CommentDialogs.ID_COMMENT_DLG_UNAPPROVING;
                break;
            case SPAM:
                dlgId = CommentDialogs.ID_COMMENT_DLG_SPAMMING;
                break;
            case TRASH:
                dlgId = CommentDialogs.ID_COMMENT_DLG_TRASHING;
                break;
            default :
                return;
        }
        getActivity().showDialog(dlgId);

        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList moderatedComments) {
                if (!hasActivity())
                    return;
                finishActionMode();
                dismissDialog(dlgId);
                if (moderatedComments.size() > 0) {
                    getCommentAdapter().clearSelectedComments();
                    getCommentAdapter().replaceComments(moderatedComments);
                    if (mOnCommentChangeListener != null) {
                        ChangeType changeType = (newStatus == CommentStatus.TRASH ? ChangeType.TRASHED : ChangeType.STATUS);
                        mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_LIST, changeType);
                    }
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        };

        CommentActions.moderateComments(WordPress.getCurrentLocalTableBlogId(), updateComments, newStatus, listener);
    }

    private void confirmDeleteComments() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dlg_confirm_trash_comments);
        builder.setTitle(R.string.trash);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.trash_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                deleteSelectedComments();
            }
        });
        builder.setNegativeButton(R.string.trash_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void deleteSelectedComments() {
        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final CommentList selectedComments = getCommentAdapter().getSelectedComments();
        getActivity().showDialog(CommentDialogs.ID_COMMENT_DLG_TRASHING);
        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList deletedComments) {
                if (!hasActivity())
                    return;
                finishActionMode();
                dismissDialog(CommentDialogs.ID_COMMENT_DLG_TRASHING);
                if (deletedComments.size() > 0) {
                    getCommentAdapter().clearSelectedComments();
                    getCommentAdapter().deleteComments(deletedComments);
                    if (mOnCommentChangeListener != null)
                        mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_LIST, ChangeType.TRASHED);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        };

        CommentActions.moderateComments(WordPress.getCurrentLocalTableBlogId(), selectedComments, CommentStatus.TRASH,
                listener);
    }

    long getHighlightedCommentId() {
        return (hasCommentAdapter() ? getCommentAdapter().getHighlightedCommentId() : 0);
    }
    void setHighlightedCommentId(long commentId) {
        getCommentAdapter().setHighlightedCommentId(commentId);
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
                    getCommentAdapter().toggleItemSelected(position, view);
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
                        getCommentAdapter().setItemSelected(position, true, view);
                    }
                } else {
                    getCommentAdapter().toggleItemSelected(position, view);
                }
                return true;
            }
        });
    }

    void loadComments() {
        // this is called from CommentsActivity when a comment was changed in the detail view,
        // and the change will already be in SQLite so simply reload the comment adapter
        // to show the change
        getCommentAdapter().loadComments();
    }

    /*
     * get latest comments from server, or pass loadMore=true to get comments beyond the
     * existing ones
     */
    @SuppressLint("NewApi")
    void updateComments(boolean loadMore) {
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        }
        if (SysUtils.canUseExecuteOnExecutor()) {
            new UpdateCommentsTask(loadMore).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new UpdateCommentsTask(loadMore).execute();
        }
    }

    /*
     * task to retrieve latest comments from server
     */
    private class UpdateCommentsTask extends AsyncTask<Void, Void, CommentList> {
        boolean isError;
        final boolean isLoadingMore;
        String xmlRpcErrorMessage;

        private UpdateCommentsTask(boolean loadMore) {
            isLoadingMore = loadMore;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsUpdatingComments = true;
            mPullToRefreshHelper.setRefreshing(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mPullToRefreshHelper.setRefreshing(false);
            mIsUpdatingComments = false;
        }

        @Override
        protected CommentList doInBackground(Void... args) {
            if (!hasActivity())
                return null;

            Blog blog = WordPress.getCurrentBlog();
            if (blog == null) {
                isError = true;
                return null;
            }

            Map<String, Object> hPost = new HashMap<String, Object>();
            if (isLoadingMore) {
                int numExisting = getCommentAdapter().getCount();
                hPost.put("offset", numExisting);
                hPost.put("number", COMMENTS_PER_PAGE);
            } else {
                hPost.put("number", COMMENTS_PER_PAGE);
            }

            Object[] params = { blog.getRemoteBlogId(),
                                blog.getUsername(),
                                blog.getPassword(),
                                hPost };
            try {
                return ApiHelper.refreshComments(getActivity(), params);
            } catch (XMLRPCException e) {
                xmlRpcErrorMessage = e.getMessage();
                isError = true;
                return null;
            }
        }

        protected void onPostExecute(CommentList comments) {
            mIsUpdatingComments = false;
            if (!hasActivity())
                return;
            mPullToRefreshHelper.setRefreshing(false);

            if (isCancelled())
                return;

            mCanLoadMoreComments = (comments != null && comments.size() > 0);

            // result will be null on error OR if no more comments exists
            if (comments == null) {
                if (isError && !getActivity().isFinishing()) {
                    if (!TextUtils.isEmpty(xmlRpcErrorMessage)) {
                        ToastUtils.showToastOrAuthAlert(getActivity(), xmlRpcErrorMessage, getString(R.string.error_refresh_comments));
                    } else {
                        ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_comments));
                    }
                }
                return;
            }

            if (comments.size() > 0)
                getCommentAdapter().loadComments();
        }
    }

    public interface OnCommentSelectedListener {
        public void onCommentSelected(Comment comment);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        outState.putBoolean(KEY_AUTO_REFRESHED, mHasAutoRefreshedComments);
        super.onSaveInstanceState(outState);
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    private void showEmptyView() {
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyView() {
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
    }

    /****
     * Contextual ActionBar (CAB) routines
     ***/
    private void updateActionModeTitle() {
        if (mActionMode == null)
            return;
        int numSelected = getSelectedCommentCount();
        if (numSelected > 0) {
            mActionMode.setTitle(Integer.toString(numSelected));
        } else {
            mActionMode.setTitle("");
        }
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

        private void setItemEnabled(com.actionbarsherlock.view.Menu menu, int menuId, boolean isEnabled) {
            final MenuItem item = menu.findItem(menuId);
            if (item == null || item.isEnabled() == isEnabled)
                return;
            item.setEnabled(isEnabled);
            if (item.getIcon() != null) {
                // must mutate the drawable to avoid affecting other instances of it
                Drawable icon = item.getIcon().mutate();
                icon.setAlpha(isEnabled ? 255 : 128);
                item.setIcon(icon);
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, com.actionbarsherlock.view.Menu menu) {
            final CommentList selectedComments = getCommentAdapter().getSelectedComments();

            boolean hasSelection = (selectedComments.size() > 0);
            boolean hasApproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.APPROVED);
            boolean hasUnapproved = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.UNAPPROVED);
            boolean hasSpam = hasSelection && selectedComments.hasAnyWithStatus(CommentStatus.SPAM);
            boolean hasAnyNonSpam = hasSelection && selectedComments.hasAnyWithoutStatus(CommentStatus.SPAM);

            setItemEnabled(menu, R.id.menu_approve,   hasUnapproved || hasSpam);
            setItemEnabled(menu, R.id.menu_unapprove, hasApproved);
            setItemEnabled(menu, R.id.menu_spam,      hasAnyNonSpam);
            setItemEnabled(menu, R.id.menu_trash,     hasSelection);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, com.actionbarsherlock.view.MenuItem menuItem) {
            int numSelected = getSelectedCommentCount();
            if (numSelected == 0)
                return false;

            switch (menuItem.getItemId()) {
                case R.id.menu_approve :
                    moderateSelectedComments(CommentStatus.APPROVED);
                    return true;
                case R.id.menu_unapprove :
                    moderateSelectedComments(CommentStatus.UNAPPROVED);
                    return true;
                case R.id.menu_spam :
                    moderateSelectedComments(CommentStatus.SPAM);
                    return true;
                case R.id.menu_trash :
                    // unlike the other status changes, we ask the user to confirm trashing
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