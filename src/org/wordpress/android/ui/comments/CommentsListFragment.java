package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CommentsListFragment extends Fragment {
    protected boolean shouldSelectAfterLoad = false;
    private boolean mIsRetrievingComments = false;
    private boolean mCanLoadMoreComments = true;
    private String mModerateErrorMsg = "";

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
                getActivity().showDialog(ID_DIALOG_DELETING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        deleteSelectedComments();
                    }
                }.start();

            }
        });

        approveComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                getActivity().showDialog(ID_DIALOG_MODERATING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        moderateSelectedComments(CommentStatus.APPROVED);
                    }
                }.start();
            }
        });

        unapproveComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                getActivity().showDialog(ID_DIALOG_MODERATING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        moderateSelectedComments(CommentStatus.UNAPPROVED);
                    }
                }.start();
            }
        });

        spamComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                getActivity().showDialog(ID_DIALOG_MODERATING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        moderateSelectedComments(CommentStatus.SPAM);
                    }
                }.start();
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
        final String newStatusStr = CommentStatus.toString(newStatus);
        final Blog blog = WordPress.currentBlog;
        final List<Comment> selectedComments = getCommentAdapter().getSelectedComments();
        final List<Comment> updatedComments = new LinkedList<Comment>();

        mModerateErrorMsg = "";

        for (Comment comment : selectedComments) {
            XMLRPCClient client = new XMLRPCClient(
                    blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

            // skip if comment status is already same as passed
            if (newStatusStr.equals(comment.getStatus())) {
                continue;
            }

            Map<String, String> postHash = new HashMap<String, String>();
            postHash.put("status", newStatusStr);
            postHash.put("content", StringUtils.notNullStr(comment.comment));
            postHash.put("author", StringUtils.notNullStr(comment.name));
            postHash.put("author_url", StringUtils.notNullStr(comment.authorURL));
            postHash.put("author_email", StringUtils.notNullStr(comment.authorEmail));

            Object[] params = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    comment.commentID,
                    postHash };

            Object result;
            try {
                result = client.call("wp.editComment", params);
                boolean bResult = Boolean.parseBoolean(result.toString());
                if (bResult) {
                    comment.setStatus(newStatusStr);
                    WordPress.wpDB.updateCommentStatus(WordPress.currentBlog.getLocalTableBlogId(), comment.commentID, newStatusStr);
                    updatedComments.add(comment);
                }
            } catch (XMLRPCException e) {
                mModerateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
            }
        }
        dismissDialog(ID_DIALOG_MODERATING);

        if (hasActivity()) {
            Thread action = new Thread() {
                public void run() {
                    hideModerationBar();
                    if (TextUtils.isEmpty(mModerateErrorMsg)) {
                        final String msg;
                        if (updatedComments.size() > 1) {
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        } else {
                            msg = getResources().getText(R.string.comment_moderated).toString();
                        }
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        mOnCommentChangeListener.onCommentsModerated(updatedComments);

                        // update the comment counter on the menu drawer
                        ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                    } else if (!getActivity().isFinishing()) {
                        // there was an xmlrpc error
                        getListView().invalidateViews();
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(mModerateErrorMsg);
                        ft.add(alert, "alert");
                        ft.commitAllowingStateLoss();
                    }
                }
            };
            getActivity().runOnUiThread(action);
        }
    }

    private void deleteSelectedComments() {
        final List<Comment> selectedComments = getCommentAdapter().getSelectedComments();
        mModerateErrorMsg = "";

        for (Comment comment : selectedComments) {
            XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Object[] params = {WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), comment.commentID};

            try {
                client.call("wp.deleteComment", params);
            } catch (final XMLRPCException e) {
                mModerateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
            }
        }
        dismissDialog(ID_DIALOG_DELETING);

        if (hasActivity()) {
            Thread action = new Thread() {
                public void run() {
                    if (TextUtils.isEmpty(mModerateErrorMsg)) {
                        final String msg;
                        if (selectedComments.size() > 1) {
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        } else {
                            msg = getResources().getText(R.string.comment_moderated).toString();
                        }
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        hideModerationBar();
                        getCommentAdapter().clearSelectedComments();
                        mOnCommentChangeListener.onCommentDeleted();
                    } else if (!getActivity().isFinishing()) {
                        // error occurred during delete request
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(mModerateErrorMsg);
                        ft.add(alert, "alert");
                        ft.commitAllowingStateLoss();
                    }
                }
            };
            getActivity().runOnUiThread(action);
        }
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

    protected void hideModerationBar() {
        if (!hasActivity())
            return;
        ViewGroup moderationBar = (ViewGroup) getActivity().findViewById(R.id.moderationBar);
        if( moderationBar.getVisibility() == View.INVISIBLE )
            return;
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(MODERATION_BAR_ANI_MS);
        set.addAnimation(animation);
        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(MODERATION_BAR_ANI_MS);
        set.addAnimation(animation);
        moderationBar.clearAnimation();
        moderationBar.startAnimation(set);
        moderationBar.setVisibility(View.INVISIBLE);
    }

    protected void showOrHideModerationBar() {
        if (getCommentAdapter().getSelectedCommentCount() > 0) {
            showModerationBar();
        } else {
            hideModerationBar();
        }
    }

    private static final long MODERATION_BAR_ANI_MS = 250;

    protected void showModerationBar() {
        if (!hasActivity())
            return;
        ViewGroup moderationBar = (ViewGroup) getActivity().findViewById(R.id.moderationBar);
        if( moderationBar.getVisibility() == View.VISIBLE )
            return;
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(MODERATION_BAR_ANI_MS);
        set.addAnimation(animation);
        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(MODERATION_BAR_ANI_MS);
        set.addAnimation(animation);
        moderationBar.setVisibility(View.VISIBLE);
        moderationBar.startAnimation(set);
    }

    protected void cancelCommentsTask() {
        if (mGetCommentsTask != null && !mGetCommentsTask.isCancelled())
            mGetCommentsTask.cancel(true);
    }

    /*
     * task to retrieve latest comments from server
     */
    private int mPrevLoadedBlogId;

    private class GetRecentCommentsTask extends AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {
        boolean isError;
        boolean isLoadingMore;

        private GetRecentCommentsTask(boolean loadMore) {
            isLoadingMore = loadMore;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mIsRetrievingComments = true;

            // clear adapter if blog has changed
            int blogId = WordPress.currentBlog.getRemoteBlogId();
            if (blogId != mPrevLoadedBlogId) {
                mPrevLoadedBlogId = blogId;
                getCommentAdapter().clear();
            }

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
        protected Map<Integer, Map<?, ?>> doInBackground(Void... args) {
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

        protected void onPostExecute(Map<Integer, Map<?, ?>> commentsResult) {
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

            mCanLoadMoreComments = (commentsResult != null && commentsResult.size() > 0);

            // result will be null on error OR if no more comments exists
            if (commentsResult == null) {
                if (isError && !getActivity().isFinishing())
                    ToastUtils.showToast(getActivity(), R.string.error_refresh_comments, ToastUtils.Duration.LONG);
                return;
            }

            if (commentsResult.size() > 0)
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