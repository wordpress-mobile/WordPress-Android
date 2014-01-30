package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
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

    // context menu IDs
    protected static final int MENU_ID_APPROVED = 100;
    protected static final int MENU_ID_UNAPPROVED = 101;
    protected static final int MENU_ID_SPAM = 102;
    protected static final int MENU_ID_DELETE = 103;
    protected static final int MENU_ID_EDIT = 105;

    // dialog IDs
    private static final int ID_DIALOG_MODERATING = 1;
    private static final int ID_DIALOG_DELETING = 2;

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

            // adapter calls this when checked comments have changed
            CommentAdapter.OnSelectionChangeListener changeListener = new CommentAdapter.OnSelectionChangeListener() {
                @Override
                public void onSelectionChanged() {
                    showOrHideModerationBar();
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
        TextView emptyView = (TextView) view.findViewById(android.R.id.empty);
        if (emptyView != null) {
            emptyView.setText(getText(R.string.comments_empty_list));
            mListView.setEmptyView(emptyView);
        }

        // progress bar that appears when loading more comments
        mProgressLoadMore = (ProgressBar) view.findViewById(R.id.progress_loading);
        mProgressLoadMore.setVisibility(View.GONE);

        final Button deleteComments = (Button) view.findViewById(R.id.bulkDeleteComment);
        final Button approveComments = (Button) view.findViewById(R.id.bulkApproveComment);
        final Button unapproveComments = (Button) view.findViewById(R.id.bulkUnapproveComment);
        final Button spamComments = (Button) view.findViewById(R.id.bulkMarkSpam);

        deleteComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                deleteSelectedComments();
            }
        });
        approveComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                moderateSelectedComments(CommentStatus.APPROVED);
            }
        });
        unapproveComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                moderateSelectedComments(CommentStatus.UNAPPROVED);
            }
        });
        spamComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                moderateSelectedComments(CommentStatus.SPAM);
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

    @SuppressWarnings("unchecked")
    private void moderateSelectedComments(CommentStatus newStatus) {
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

        getActivity().showDialog(ID_DIALOG_MODERATING);
        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList moderatedComments) {
                if (!hasActivity())
                    return;
                dismissDialog(ID_DIALOG_MODERATING);
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
    }

    private void deleteSelectedComments() {
        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final CommentList selectedComments = getCommentAdapter().getSelectedComments();
        getActivity().showDialog(ID_DIALOG_DELETING);
        CommentActions.OnCommentsModeratedListener listener = new CommentActions.OnCommentsModeratedListener() {
            @Override
            public void onCommentsModerated(final CommentList deletedComments) {
                if (!hasActivity())
                    return;
                dismissDialog(ID_DIALOG_DELETING);
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
    }

    private void setUpListView() {
        ListView listView = this.getListView();
        listView.setAdapter(getCommentAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Comment comment = (Comment)getCommentAdapter().getItem(position);
                mOnCommentSelectedListener.onCommentSelected(comment);
                getListView().invalidateViews();
            }
        });

        // configure the listView's context menu
        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info;
                try {
                    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                } catch (ClassCastException e) {
                    AppLog.e(T.COMMENTS, "bad menuInfo", e);
                    return;
                }

                WordPress.currentComment = (Comment)getCommentAdapter().getItem(info.position);
                CommentStatus status = WordPress.currentComment.getStatusEnum();

                menu.setHeaderTitle(getResources().getText(R.string.comment_actions));
                if (status != CommentStatus.APPROVED) {
                    menu.add(0, MENU_ID_APPROVED, 0, getResources().getText(R.string.mark_approved));
                } else {
                    menu.add(0, MENU_ID_UNAPPROVED, 0, getResources().getText(R.string.mark_unapproved));
                }
                if (status != CommentStatus.SPAM) {
                    menu.add(0, MENU_ID_SPAM, 0, getResources().getText(R.string.mark_spam));
                }
                menu.add(0, MENU_ID_DELETE, 0, getResources().getText(R.string.delete));
                menu.add(0, MENU_ID_EDIT, 0, getResources().getText(R.string.edit));
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

    protected void showOrHideModerationBar() {
        if (getCommentAdapter().getSelectedCommentCount() > 0) {
            showModerationBar();
        } else {
            hideModerationBar();
        }
    }

    protected void showModerationBar() {
        if (!hasActivity())
            return;
        ViewGroup moderationBar = (ViewGroup) getActivity().findViewById(R.id.moderationBar);
        if (moderationBar.getVisibility() != View.VISIBLE)
            AniUtils.flyIn(moderationBar);
    }

    protected void hideModerationBar() {
        if (!hasActivity())
            return;
        ViewGroup moderationBar = (ViewGroup) getActivity().findViewById(R.id.moderationBar);
        if (moderationBar.getVisibility() == View.VISIBLE)
            AniUtils.flyOut(moderationBar);
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
            showOrHideModerationBar();

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
}