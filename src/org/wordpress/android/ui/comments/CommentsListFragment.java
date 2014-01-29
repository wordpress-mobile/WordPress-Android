package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CommentsListFragment extends Fragment {
    private ArrayList<Comment> mComments = new ArrayList<Comment>();

    protected boolean shouldSelectAfterLoad = false;
    private boolean mCanLoadMoreComments = true;
    private boolean mIsRetrievingComments = false;
    private String mModerateErrorMsg = "";

    private GetRecentCommentsTask mGetCommentsTask;
    private HashSet<Integer> selectedCommentPositions = new HashSet<Integer>();
    private OnCommentSelectedListener mOnCommentSelectedListener;
    private OnAnimateRefreshButtonListener mOnAnimateRefreshButton;
    private CommentActions.OnCommentChangeListener mOnCommentChangeListener;
    private ProgressBar mProgressLoadMore;
    private ListView mListView;
    private CommentAdapter mCommentAdapter;

    private int mStatusColorSpam;
    private int mStatusColorUnapproved;

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
        mStatusColorSpam = Color.parseColor("#FF0000");
        mStatusColorUnapproved = Color.parseColor("#D54E21");
    }

    private ListView getListView() {
        return mListView;
    }

    private CommentAdapter getCommentAdapter() {
        if (mCommentAdapter == null) {
            mCommentAdapter = new CommentAdapter();
        }
        return mCommentAdapter;
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
        final int numChecked = getCheckedCommentCount();
        final List<Comment> updatedComments = new LinkedList<Comment>();

        mModerateErrorMsg = "";

        Iterator it = selectedCommentPositions.iterator();
        while (it.hasNext()) {
            int i = (Integer) it.next();
            XMLRPCClient client = new XMLRPCClient(
                    blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

            Comment curComment = (Comment) getListView().getItemAtPosition(i);

            if (newStatusStr.equals(curComment.getStatus())) {
                it.remove();
                continue;
            }

            Map<String, String> postHash = new HashMap<String, String>();
            postHash.put("status", newStatusStr);
            postHash.put("content", StringUtils.notNullStr(curComment.comment));
            postHash.put("author", StringUtils.notNullStr(curComment.name));
            postHash.put("author_url", StringUtils.notNullStr(curComment.authorURL));
            postHash.put("author_email", StringUtils.notNullStr(curComment.authorEmail));

            Object[] params = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    curComment.commentID,
                    postHash };

            Object result;
            try {
                result = client.call("wp.editComment", params);
                boolean bResult = Boolean.parseBoolean(result.toString());
                if (bResult) {
                    it.remove();
                    curComment.setStatus(newStatusStr);
                    mComments.set(i, curComment);
                    WordPress.wpDB.updateCommentStatus(WordPress.currentBlog.getLocalTableBlogId(), curComment.commentID, newStatusStr);
                    updatedComments.add(curComment);
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
                        if (numChecked > 1) {
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
        final int numChecked = getCheckedCommentCount();
        mModerateErrorMsg = "";

        for (int i : selectedCommentPositions) {
            XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Comment listRow = (Comment) getListView().getItemAtPosition(i);
            int curCommentID = listRow.commentID;

            Object[] params = {WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), curCommentID};

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
                        if (numChecked > 1) {
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        } else {
                            msg = getResources().getText(R.string.comment_moderated).toString();
                        }
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        hideModerationBar();
                        selectedCommentPositions.clear();
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

    /*
     * load comments from local db and add to listView adapter
     */
    protected boolean loadComments() {
        String author, postID, commentContent, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        int blogId = WordPress.currentBlog.getLocalTableBlogId();
        List<Map<String, Object>> loadedComments = WordPress.wpDB.loadComments(blogId);

        if (loadedComments == null) {
            return false;
        }

        for (int i = 0; i < loadedComments.size(); i++) {
            Map<String, Object> contentHash = loadedComments.get(i);
            author = StringUtils.unescapeHTML(contentHash.get("author").toString());
            commentID = (Integer) contentHash.get("commentID");
            postID = contentHash.get("postID").toString();
            commentContent = contentHash.get("comment").toString();
            dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
            status = contentHash.get("status").toString();
            authorEmail = StringUtils.unescapeHTML(contentHash.get("email").toString());
            authorURL = StringUtils.unescapeHTML(contentHash.get("url").toString());
            postTitle = StringUtils.unescapeHTML(contentHash.get("postTitle").toString());

            Comment comment = new Comment(postID,
                                          commentID,
                                          i,
                                          author,
                                          dateCreatedFormatted,
                                          commentContent,
                                          status,
                                          postTitle,
                                          authorURL,
                                          authorEmail,
                                          GravatarUtils.gravatarUrlFromEmail(authorEmail, 140));
            mComments.add(comment);
        }

        getCommentAdapter().notifyDataSetChanged();

        if (this.shouldSelectAfterLoad) {
            if (mComments != null && mComments.size() > 0) {
                Comment aComment = mComments.get(0);
                mOnCommentSelectedListener.onCommentSelected(aComment);
                getListView().setItemChecked(0, true);
            }
            shouldSelectAfterLoad = false;
        }

        return true;
    }

    private void setUpListView() {
        ListView listView = this.getListView();
        listView.setAdapter(getCommentAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // make sure position is in bounds (prevents ArrayIndexOutOfBoundsException that
                // would otherwise occur when footer is tapped)
                if (position < 0 || position >= mComments.size()) {
                    return;
                }
                Comment comment = mComments.get(position);
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
                WordPress.currentComment = mComments.get(info.position);
                menu.setHeaderTitle(getResources().getText(R.string.comment_actions));
                CommentStatus status = WordPress.currentComment.getStatusEnum();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class CommentAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        CommentAdapter() {
            mInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return (mComments != null ? mComments.size() : 0);
        }

        @Override
        public Object getItem(int position) {
            return mComments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private void clear() {
            if (mComments.size() > 0) {
                mComments.clear();
                notifyDataSetChanged();
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final Comment comment = mComments.get(position);
            final CommentEntryWrapper wrapper;

            if (convertView == null || convertView.getTag() == null) {
                convertView = mInflater.inflate(R.layout.comment_row, null);
                wrapper = new CommentEntryWrapper(convertView);
                convertView.setTag(wrapper);
            } else {
                wrapper = (CommentEntryWrapper) convertView.getTag();
            }

            wrapper.populateFrom(comment, position);

            // start task to load more comments when we near the end
            if (mCanLoadMoreComments && !mIsRetrievingComments && position >= getCount()-1) {
                AppLog.d(T.COMMENTS, "auto-loading more comments");
                refreshComments(true);
            }

            return convertView;
        }
    }

    class CommentEntryWrapper {
        private TextView txtName;
        private TextView txtEmailURL;
        private TextView txtComment;
        private TextView txtStatus;
        private TextView txtPostTitle;
        private NetworkImageView imgAvatar;
        private View row;
        private CheckBox bulkCheck;

        CommentEntryWrapper(View row) {
            this.row = row;

            txtName = (TextView) row.findViewById(R.id.name);
            txtEmailURL = (TextView) row.findViewById(R.id.email_url);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status);
            txtPostTitle = (TextView) row.findViewById(R.id.postTitle);
            bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);
            imgAvatar = (NetworkImageView) row.findViewById(R.id.avatar);
        }

        void populateFrom(Comment comment, final int position) {
            txtName.setText(!TextUtils.isEmpty(comment.name) ? comment.name : getString(R.string.anonymous));
            txtPostTitle.setText(getResources().getText(R.string.on) + " " + comment.postTitle);
            txtComment.setText(StringUtils.unescapeHTML(comment.comment));

            // use the email address if the commenter didn't add a url
            String fEmailURL = (TextUtils.isEmpty(comment.authorURL) ? comment.emailURL : comment.authorURL);
            txtEmailURL.setVisibility(TextUtils.isEmpty(fEmailURL) ? View.GONE : View.VISIBLE);
            txtEmailURL.setText(fEmailURL);

            row.setId(Integer.valueOf(comment.commentID));

            // status is only shown for comments that haven't been approved
            switch (comment.getStatusEnum()) {
                case SPAM :
                    txtStatus.setText(getResources().getText(R.string.spam).toString());
                    txtStatus.setTextColor(mStatusColorSpam);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                case UNAPPROVED:
                    txtStatus.setText(getResources().getText(R.string.unapproved).toString());
                    txtStatus.setTextColor(mStatusColorUnapproved);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                default :
                    txtStatus.setVisibility(View.GONE);
                    break;
            }

            bulkCheck.setChecked(selectedCommentPositions.contains(position));
            bulkCheck.setTag(position);
            bulkCheck.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    if (bulkCheck.isChecked()) {
                        selectedCommentPositions.add(position);
                    } else {
                        selectedCommentPositions.remove(position);
                    }
                    showOrHideModerationBar();
                }
            });

            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
            if (comment.hasProfileImageUrl()) {
                imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(comment.getProfileImageUrl()), WordPress.imageLoader);
            } else {
                imgAvatar.setImageResource(R.drawable.placeholder);
            }
        }
    }

    protected int getCheckedCommentCount() {
        return (selectedCommentPositions != null ? selectedCommentPositions.size() : 0);
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
        if (getCheckedCommentCount() > 0) {
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
                int numExisting = mComments.size();
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

            if (commentsResult == null) {
                WordPress.wpDB.clearComments(WordPress.currentBlog.getLocalTableBlogId());
                getCommentAdapter().clear();
                WordPress.currentComment = null;
                loadComments();

                if (isError && !getActivity().isFinishing())
                    ToastUtils.showToast(getActivity(), R.string.error_refresh_comments, ToastUtils.Duration.LONG);

                return;
            }

            if (commentsResult.size() > 0) {
                loadComments();
            }
        }
    }

    /*
     * replace existing comment with the passed one and refresh list to show changes
     */
    protected void replaceComment(Comment comment) {
        if (comment==null || mComments ==null)
            return;
        for (int i=0; i < mComments.size(); i++) {
            Comment thisComment = mComments.get(i);
            if (thisComment.commentID==comment.commentID && thisComment.postID==comment.postID) {
                mComments.set(i, comment);
                getListView().invalidateViews();
                return;
            }
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