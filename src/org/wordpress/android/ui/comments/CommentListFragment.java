package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.SherlockListFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.MessageBarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentListFragment extends SherlockListFragment {

    public ArrayList<Comment> model = null;
    public Map<Integer, Map<?, ?>> allComments = new HashMap<Integer, Map<?, ?>>();
    public static final int COMMENTS_PER_PAGE = 30;
    public boolean shouldSelectAfterLoad = false;
    public int numRecords = 0,
               selectedPosition,
               scrollPosition = 0,
               scrollPositionTop = 0;
    public ProgressDialog progressDialog;
    public getRecentCommentsTask getCommentsTask;

    private XMLRPCClient client;
    private String accountName = "", moderateErrorMsg = "";
    private ViewSwitcher switcher;
    private boolean loadMore = false, doInBackground = false, refreshOnly = false,
            mCommentsUpdating = false;
    private Object[] mCommentParams;
    private OnAnimateRefreshButtonListener onAnimateRefreshButton;
    private CommentListListener mOnCommentListListener;
    private ListScrollPositionManager mListScrollPositionManager;
    private CommentAdapter mAdapter;
    private ActionMode mActionMode;
    private Map<Integer, Boolean> mPriorCheckedCommentPositions;
    private Parcelable mSavedListViewState;

    public interface CommentListListener {
        public void onCommentClicked(Comment comment);
    }

    public interface OnAnimateRefreshButtonListener {
        public void onAnimateRefreshButton(boolean start);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        ListView listView = getListView();
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.comments_empty_list));
        }
        mListScrollPositionManager = new ListScrollPositionManager(listView, false);
        mPriorCheckedCommentPositions = new HashMap<Integer, Boolean>();

        /* TODO: JCO - We need to make sure we are only calling this once during the fragment's create life cycle */
        //refreshComments();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnCommentListListener = (CommentListListener) activity;
            onAnimateRefreshButton = (OnAnimateRefreshButtonListener) activity;

        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mListScrollPositionManager.saveScrollOffset();
        mSavedListViewState = getListView().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.view_comments_fragment, container, false);

        // create the ViewSwitcher in the current context
        switcher = new ViewSwitcher(getActivity().getApplicationContext());
        Button footer = (Button) View.inflate(getActivity()
                .getApplicationContext(), R.layout.list_footer_btn, null);
        footer.setText(getResources().getText(R.string.load_more) + " "
                + getResources().getText(R.string.tab_comments));
        footer.setFocusable(false);
        footer.setFocusableInTouchMode(false);
        footer.setLongClickable(false);
        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switcher.showNext();
                refreshComments(true);
            }
        });

        View progress = View.inflate(getActivity().getApplicationContext(),
                R.layout.list_footer_progress, null);

        switcher.addView(footer);
        switcher.addView(progress);

        getActivity().setTitle(accountName + " - Moderate Comments");

        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void onAsyncModerationReturnSuccess(CommentStatus commentModerationStatusType) {
        if (commentModerationStatusType == CommentStatus.APPROVED
                || commentModerationStatusType == CommentStatus.UNAPPROVED) {
            if (mActionMode != null) {
                mActionMode.invalidate();
            }
        } else if (commentModerationStatusType == CommentStatus.SPAM
                || commentModerationStatusType == CommentStatus.TRASH) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
    }

    private void onAsyncModerationReturnFailure(CommentStatus commentModerationStatusType) {
        if (commentModerationStatusType == CommentStatus.APPROVED
                || commentModerationStatusType == CommentStatus.UNAPPROVED
                || commentModerationStatusType == CommentStatus.SPAM) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        } else if (commentModerationStatusType == CommentStatus.TRASH) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
    }

    /**
     * Updates, all comments, model and the local database for a set of Comments changed server side.
     */
    private void updateChangedCommentSet(ArrayList<Comment> selectedCommentsSnapshot, ArrayList<Integer> moderatedComments, String newStatusStr) {
        if (moderatedComments.size() > 0) {
            for(Comment currentComment : selectedCommentsSnapshot) {
                Integer currentCommentId = currentComment.commentID;
                if (moderatedComments.contains(currentCommentId)) {
                    currentComment.setStatus(newStatusStr);
                    replaceCommentInModel(currentComment);
                    WordPress.wpDB.updateCommentStatus(WordPress.currentBlog.getId(), currentCommentId, newStatusStr);

                    if (mAdapter != null) { mAdapter.notifyDataSetChanged(); }

                    Map<String, String> contentHash;
                    contentHash = (Map<String, String>) allComments.get(currentCommentId);
                    contentHash.put("status", newStatusStr);
                    allComments.put(currentCommentId, contentHash);
                }
            }
        }
    }

    /**
     * Updates, all comments, model and the local database for a set of Comments changed server side.
     */
    private void deleteCommentSet(ArrayList<Comment> selectedCommentsSnapshot, ArrayList<Integer> moderatedComments) {
        if (moderatedComments.size() > 0) {
            for(Comment currentComment : selectedCommentsSnapshot) {
                Integer currentCommentId = currentComment.commentID;
                if (moderatedComments.contains(currentCommentId)) {
                    deleteCommentInModel(currentComment);
                    WordPress.wpDB.deleteComment(WordPress.currentBlog.getId(), currentCommentId);

                    if (mAdapter != null) { mAdapter.remove(currentComment); }

                    allComments.remove(currentCommentId);
                }
            }
        }
    }

    /**
     * Start an AsyncTask to moderate the current comment selection set
     *
     * @param commentStatus The status to moderate the currently selected comment set type
     */
    private void moderateComments(final CommentStatus commentStatus) {
        ListView listView = getListView();
        final String newStatus = CommentStatus.toString(commentStatus);
        final ArrayList<Integer> selectedCommentIds = getSelectedCommentIds(listView.getCheckedItemPositions());
        final ArrayList<Comment> selectedCommentsSnapshot = getSelectedCommentArray(listView.getCheckedItemPositions());

        ApiHelper.ModerateCommentsTask task = new ApiHelper.ModerateCommentsTask(newStatus,
                allComments, selectedCommentIds,
                new ApiHelper.ModerateCommentsTask.Callback() {
                    String messageBarText;
                    int numCommentsModerated = 0;

                    @Override
                    public void onSuccess(ArrayList<Integer> moderatedCommentIds) {
                        mCommentsUpdating = false;
                        updateChangedCommentSet(selectedCommentsSnapshot, moderatedCommentIds, newStatus);

                        if (getActivity() != null) {
                            numCommentsModerated = moderatedCommentIds.size();
                            if (numCommentsModerated == 1) {
                                messageBarText = getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText = getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        onAsyncModerationReturnSuccess(commentStatus);
                    }
                    @Override
                    public void onCancelled(ArrayList<Integer> moderatedCommentIds) {
                        // For the time being we will update any comments that were changed on the server
                        mCommentsUpdating = false;
                        updateChangedCommentSet(selectedCommentsSnapshot, moderatedCommentIds, newStatus);

                        if (getActivity() != null) {
                            numCommentsModerated = moderatedCommentIds.size();
                            if (numCommentsModerated == 1) {
                                messageBarText = getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText = getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        onAsyncModerationReturnSuccess(commentStatus);
                    }
                    @Override
                    public void onFailure() {
                        /* The server calls resulted in no changes but if two clients were modifying
                           the data and this local client had tried to mod the comment status to what
                           the other client had successfully completed the local client's model would
                           appear out of sync. Locally the view/model would show that the comments
                           status' did not change. For now we will refresh the data from the server. */
                        mCommentsUpdating = false;
                        if (getActivity() != null) {
                            MessageBarUtils.showMessageBar(getActivity(), getActivity().getString(R.string.error_moderate_comment));
                        }
                        refreshComments();
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());

        if(!mCommentsUpdating) {
            String messageBarText;

            if (selectedCommentIds.size() == 1) {
                messageBarText = getActivity().getString(R.string.moderating_comment);
            } else {
                messageBarText = getActivity().getString(R.string.moderating_comments);
            }
            MessageBarUtils.showMessageBar(getActivity(), messageBarText, MessageBarUtils.MessageBarType.INFO, null);
            mCommentsUpdating = true;
            task.execute(apiArgs);
        }
    }

    /**
     * Start an AsyncTask to delete the current comment selection set.
     */
    private void deleteComments() {
        ListView listView = getListView();
        final ArrayList<Integer> selectedCommentIds = getSelectedCommentIds(listView.getCheckedItemPositions());
        final ArrayList<Comment> selectedCommentsSnapshot = getSelectedCommentArray(listView.getCheckedItemPositions());

        ApiHelper.DeleteCommentsTask task = new ApiHelper.DeleteCommentsTask(selectedCommentIds,
                new ApiHelper.DeleteCommentsTask.Callback() {
                    String messageBarText;
                    int numCommentsDeleted = 0;

                    @Override
                    public void onSuccess(ArrayList<Integer> deletedCommentIds) {
                        mCommentsUpdating = false;
                        deleteCommentSet(selectedCommentsSnapshot, deletedCommentIds);

                        if (getActivity() != null) {
                            numCommentsDeleted = deletedCommentIds.size();
                            if (numCommentsDeleted == 1) {
                                messageBarText =
                                        getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText =
                                        getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        onAsyncModerationReturnSuccess(CommentStatus.TRASH);
                    }
                    @Override
                    public void onCancelled(ArrayList<Integer> deletedCommentIds) {
                        mCommentsUpdating = false;
                        deleteCommentSet(selectedCommentsSnapshot, deletedCommentIds);

                        if (getActivity() != null) {
                            numCommentsDeleted = deletedCommentIds.size();
                            if (numCommentsDeleted == 1) {
                                messageBarText =
                                        getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText =
                                        getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        onAsyncModerationReturnSuccess(CommentStatus.TRASH);
                    }
                    @Override
                    public void onFailure() {
                        mCommentsUpdating = false;
                        if (getActivity() != null) {
                            MessageBarUtils.showMessageBar(getActivity(), getActivity().getString(R.string.error_moderate_comment));
                        }
                        onAsyncModerationReturnFailure(CommentStatus.TRASH);
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());

        if(!mCommentsUpdating) {
            String messageBarText;
            if (selectedCommentIds.size() == 1) {
                messageBarText = getActivity().getString(R.string.deleting_comment);
            } else {
                messageBarText = getActivity().getString(R.string.deleting_comments);
            }
            MessageBarUtils.showMessageBar(getActivity(), messageBarText, MessageBarUtils.MessageBarType.INFO, null);

            mCommentsUpdating = true;
            task.execute(apiArgs);
        }
    }

    public boolean loadComments(boolean refresh, boolean loadMore) {
        refreshOnly = refresh;
        String author, postID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        List<Map<String, Object>> loadedComments = WordPress.wpDB.loadComments(WordPress.currentBlog.getId());

        if (refreshOnly) {
            if (model != null) {
                model.clear();
            }
        } else {
            model = new ArrayList<Comment>();
        }

        if (loadedComments != null) {
            numRecords = loadedComments.size();

            for (int i = 0; i < loadedComments.size(); i++) {
                Map<String, Object> contentHash = loadedComments.get(i);
                allComments.put((Integer) contentHash.get("commentID"), contentHash);
                author = StringUtils.unescapeHTML(contentHash.get("author").toString());
                commentID = (Integer) contentHash.get("commentID");
                postID = contentHash.get("postID").toString();
                comment = StringUtils.unescapeHTML(contentHash.get("comment").toString());
                dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
                status = contentHash.get("status").toString();
                authorEmail = StringUtils.unescapeHTML(contentHash.get("email").toString());
                authorURL = StringUtils.unescapeHTML(contentHash.get("url").toString());
                postTitle = StringUtils.unescapeHTML(contentHash.get("postTitle").toString());

                if (model == null) {
                    model = new ArrayList<Comment>();
                }

                // add to model
                model.add(new Comment(postID,
                                      commentID,
                                      i,
                                      author,
                                      dateCreatedFormatted,
                                      comment,
                                      status,
                                      postTitle,
                                      authorURL,
                                      authorEmail,
                                      GravatarUtils.gravatarUrlFromEmail(authorEmail, 140)));
            }

            if (!refreshOnly) {
                boolean showSwitcher = loadedComments.size() % COMMENTS_PER_PAGE == 0;
                setUpListView(showSwitcher);
            } else {
                getListView().invalidateViews();
            }

            if (this.shouldSelectAfterLoad) {
                if (model != null) {
                    if (model.size() > 0) {
                        selectedPosition = 0;
                        Comment firstComment = model.get(0);
                        mOnCommentListListener.onCommentClicked(firstComment);
                    }
                }
                shouldSelectAfterLoad = false;
            }

            if (loadMore && scrollPosition > 0) {
                try {
                    getListView().setSelectionFromTop(scrollPosition, scrollPositionTop);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mListScrollPositionManager.restoreScrollOffset();

            return true;
        } else {
            setUpListView(false);
            return false;
        }
    }

    /**
     * Replace existing comment with the passed in value
     * @param comment The comment with the same postID and commentID that is to be replaced in the
     *                model.
     */
    private void replaceCommentInModel(Comment comment) {
        if (comment==null || model==null)
            return;
        for (int i=0; i < model.size(); i++) {
            Comment thisComment = model.get(i);
            if (thisComment.commentID==comment.commentID && thisComment.postID==comment.postID) {
                model.set(i, comment);
                return;
            }
        }
    }

    /**
     * Delete the comment from the model
     * @param comment The comment with the same postID and commentID that is to be replaced in the
     *                model.
     */
    private void deleteCommentInModel(Comment comment) {
        if (comment==null || model==null)
            return;
        for (int i=0; i < model.size(); i++) {
            Comment thisComment = model.get(i);
            if (thisComment.commentID==comment.commentID) {
                model.remove(i);
                return;
            }
        }
    }

    public void refreshComments() {
        refreshComments(false);
    }
    public void refreshComments(boolean loadMore) {
        mListScrollPositionManager.saveScrollOffset();

        if (!loadMore) {
            onAnimateRefreshButton.onAnimateRefreshButton(true);
        }
        client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        Map<String, Object> hPost = new HashMap<String, Object>();
        if (loadMore) {
            ListView listView = getListView();
            scrollPosition = listView.getFirstVisiblePosition();
            View firstVisibleView = listView.getChildAt(0);
            scrollPositionTop = (firstVisibleView == null) ? 0
                    : firstVisibleView.getTop();
            hPost.put("number", numRecords + COMMENTS_PER_PAGE);
        } else {
            hPost.put("number", COMMENTS_PER_PAGE);
        }

        Object[] params = { WordPress.currentBlog.getBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), hPost };

        mCommentParams = params;
        getCommentsTask = new getRecentCommentsTask();
        getCommentsTask.execute();
    }

    class getRecentCommentsTask extends AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {
        protected void onPostExecute(Map<Integer, Map<?, ?>> commentsResult) {
            if (!isCancelled()) {
                if (commentsResult == null) {
                    if (model != null && model.size() == 1) {
                        WordPress.wpDB.clearComments(WordPress.currentBlog.getId());
                        model.clear();
                        allComments.clear();
                        getListView().invalidateViews();
                        WordPress.currentComment = null;
                        loadComments(false, false);
                    }

                    onAnimateRefreshButton.onAnimateRefreshButton(false);

                    if (!moderateErrorMsg.equals("") && !getActivity().isFinishing()) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(
                                String.format(getResources().getString(R.string.error_refresh),
                                        getResources().getText(R.string.tab_comments)), moderateErrorMsg);
                        alert.show(ft, "alert");
                        moderateErrorMsg = "";
                    }
                } else {
                    if (commentsResult.size() == 0) {
                        // no comments found
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    } else {
                        allComments.putAll(commentsResult);
                        if (!doInBackground) {
                            loadComments(refreshOnly, loadMore);
                        }
                    }
                    onAnimateRefreshButton.onAnimateRefreshButton(false);

                    if (loadMore) {
                        switcher.showPrevious();
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            onAnimateRefreshButton.onAnimateRefreshButton(false);
        }

        @Override
        protected Map<Integer, Map<?, ?>> doInBackground(Void... args) {

            Map<Integer, Map<?, ?>> commentsResult;
            try {
                commentsResult = ApiHelper.refreshComments(getActivity()
                        .getApplicationContext(), mCommentParams);
            } catch (XMLRPCException e) {
                if (!getActivity().isFinishing())
                    moderateErrorMsg = e.getLocalizedMessage();
                return null;
            }

            return commentsResult;
        }
    }

    private void setUpListView(boolean showSwitcher) {
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.removeFooterView(switcher);
        if (showSwitcher) { listView.addFooterView(switcher, null, false); }

        mAdapter = new CommentAdapter(getActivity(), model);
        setListAdapter(mAdapter);

        if (mSavedListViewState != null) {
            listView.onRestoreInstanceState(mSavedListViewState);
            mSavedListViewState = null;
        }

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    getListView().setItemChecked(position, true);
                    mPriorCheckedCommentPositions.put(position, true);
                    mActionMode = getSherlockActivity().startActionMode(new ActionModeCallback());
                    mActionMode.setTitle("1");
                    return true;
                }

                Boolean checkedPrevious = mPriorCheckedCommentPositions.get(position);
                if (checkedPrevious == null) { checkedPrevious = false; }

                if (!checkedPrevious) {
                    getListView().setItemChecked(position, true);
                } else {
                    getListView().setItemChecked(position, false);
                }

                int checkedItemCount = getSelectedPositionCount(getListView().getCheckedItemPositions());
                mPriorCheckedCommentPositions.put(position, !checkedPrevious);

                if (checkedItemCount == 0) {
                    mActionMode.finish();
                } else {
                    mActionMode.setTitle(Integer.toString(checkedItemCount));
                    mActionMode.invalidate();
                }
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Comment comment = mAdapter.getItem(position);
                if (mActionMode == null) {
                    getListView().clearChoices();
                    mOnCommentListListener.onCommentClicked(comment);
                } else {

                    Boolean checkedPrevious = mPriorCheckedCommentPositions.get(position);
                    if (checkedPrevious == null) { checkedPrevious = false; }

                    if (!checkedPrevious) {
                        getListView().setItemChecked(position, true);
                    } else {
                        getListView().setItemChecked(position, false);
                    }

                    int checkedItemCount = getSelectedPositionCount(getListView().getCheckedItemPositions());
                    mPriorCheckedCommentPositions.put(position, !checkedPrevious);

                    if (checkedItemCount == 0) {
                        mActionMode.finish();
                    } else {
                        mActionMode.setTitle(Integer.toString(checkedItemCount));
                        mActionMode.invalidate();
                    }
                }
            }
        });
    }

    /* Start CommentAdapter Helper Functions */
    private ArrayList<Integer> getSelectedCommentIds(SparseBooleanArray checkedPositionArray) {
        ArrayList<Integer> selectedCommentIdArray = new ArrayList<Integer>(checkedPositionArray.size());

        for(int i=0; i<checkedPositionArray.size(); i++) {
            if (checkedPositionArray.valueAt(i)) {
                int key = checkedPositionArray.keyAt(i);
                selectedCommentIdArray.add(mAdapter.getItem(key).commentID);
            }
        }
        return selectedCommentIdArray;
    }

    private ArrayList<Comment> getSelectedCommentArray(SparseBooleanArray checkedPositionArray) {
        ArrayList<Comment> selectedCommentArray = new ArrayList<Comment>();

        for(int i=0; i<checkedPositionArray.size(); i++) {
            if (checkedPositionArray.valueAt(i)) {
                selectedCommentArray.add(mAdapter.getItem(checkedPositionArray.keyAt(i)));
            }
        }
        return selectedCommentArray;
    }

    private int getSelectedPositionCount(SparseBooleanArray checkedPositionArray) {
        int size = 0;

        for (int i=0; i<checkedPositionArray.size(); i++) {
            if (checkedPositionArray.valueAt(i)) { size++; }
        }
        return size;
    }
    /* End CommentAdapter Helper Functions */

    private class CommentAdapter extends ArrayAdapter<Comment> {
        final LayoutInflater mInflater;
        boolean detailViewVisible = false;

        public CommentAdapter(Context context, ArrayList<Comment> objs) {
            super(context, R.layout.comment_row, objs);
            mInflater = getActivity().getLayoutInflater();

            FragmentManager fm = getActivity().getSupportFragmentManager();
            CommentDetailFragment f = (CommentDetailFragment) fm.findFragmentById(R.id.commentDetail);
            if (f != null && f.isInLayout())
                detailViewVisible = true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            CommentEntryWrapper wrapper;

            if (row == null) {
                row = mInflater.inflate(R.layout.comment_row, null);
                NetworkImageView avatar = (NetworkImageView)row.findViewById(R.id.avatar);
                avatar.setDefaultImageResId(R.drawable.placeholder);
                wrapper = new CommentEntryWrapper(row);
                row.setTag(wrapper);
            } else {
                wrapper = (CommentEntryWrapper) row.getTag();
            }

            Comment commentEntry = getItem(position);
            wrapper.populateFrom(commentEntry, position);

            row.setBackgroundColor(getListView().getCheckedItemPositions().get(position, false)?
                    getResources().getColor(R.color.blue_extra_light) : Color.TRANSPARENT);

            return row;
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

        CommentEntryWrapper(View row) {
            this.row = row;

            // locate views
            txtName = (TextView) row.findViewById(R.id.name);
            txtEmailURL = (TextView) row.findViewById(R.id.email_url);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status); // setTextSize(12)
            txtPostTitle = (TextView) row.findViewById(R.id.postTitle);
            imgAvatar = (NetworkImageView) row.findViewById(R.id.avatar);

        }

        void populateFrom(Comment comment, final int position) {
            txtName.setText(!TextUtils.isEmpty(comment.name) ? comment.name : getString(R.string.anonymous));
            txtComment.setText(comment.comment);
            txtPostTitle.setText(getResources().getText(R.string.on) + " " + comment.postTitle);

            // use the email address if the commenter didn't add a url
            String fEmailURL = (TextUtils.isEmpty(comment.authorURL) ? comment.emailURL : comment.authorURL);
            txtEmailURL.setVisibility(TextUtils.isEmpty(fEmailURL) ? View.GONE : View.VISIBLE);
            txtEmailURL.setText(fEmailURL);

            row.setId(position);

            final String status;
            final String textColor;

            switch (comment.getStatusEnum()) {
                case SPAM :
                    status = getResources().getText(R.string.spam).toString();
                    textColor = "#FF0000";
                    break;
                case UNAPPROVED:
                    status = getResources().getText(R.string.unapproved).toString();
                    textColor = "#D54E21";
                    break;
                default :
                    status = getResources().getText(R.string.approved).toString();
                    textColor = getResources().getString(R.color.grey_medium);
                    break;
            }

            txtStatus.setText(status);
            txtStatus.setTextColor(Color.parseColor(textColor));

            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
            if (comment.hasProfileImageUrl()) {
                imgAvatar.setImageUrl(comment.getProfileImageUrl(), WordPress.imageLoader);
            } else {
                imgAvatar.setImageResource(R.drawable.placeholder);
            }
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getSherlockActivity().getSupportMenuInflater();
            inflater.inflate(R.menu.comments_multiselect, menu);
            mActionMode = mode;

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean retVal = true;

            if (mActionMode != null) {
                menu.findItem(R.id.comments_cab_approve).setVisible(true);
                menu.findItem(R.id.comments_cab_unapprove).setVisible(true);
                menu.findItem(R.id.comments_cab_spam).setVisible(true);
                menu.findItem(R.id.comments_cab_delete).setVisible(true);

                CommentStatus.clearSelectedCommentStatusTypeCount();
                SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();
                for(int i=0; i<checkedItemPositions.size(); i++) {
                    if (checkedItemPositions.valueAt(i)) {
                        Comment comment = (Comment) getListAdapter().getItem(checkedItemPositions.keyAt(i));
                        CommentStatus.incrementSelectedCommentStatusTypeCount(comment.getStatusEnum());
                    }
                }

                /* Build a bit "set" representing which types of comments are selected.
                 * This was done so many different context permutations can be identified
                 * in the future. */
                int selectedCommentStatusTypeBitMask = 0;
                if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.APPROVED) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.APPROVED.getOffset();
                }
                if (CommentStatus.getSelectedCommentStatusTypeCount(CommentStatus.UNAPPROVED) > 0) {
                    selectedCommentStatusTypeBitMask |= 1 << CommentStatus.UNAPPROVED.getOffset();
                }

                /* Compare the bit set to the bit masks to see if they are equal. Currently we
                 * do not show an Action Icon if comments of the same status type are selected. */
                if (selectedCommentStatusTypeBitMask == 1 << CommentStatus.APPROVED.getOffset()) {
                    menu.findItem(R.id.comments_cab_approve).setVisible(false);
                } else if (selectedCommentStatusTypeBitMask == 1 << CommentStatus.UNAPPROVED.getOffset()) {
                    menu.findItem(R.id.comments_cab_unapprove).setVisible(false);
                }

            } else {
                retVal = false;
            }
            return retVal;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean retVal = true;
            int id = item.getItemId();

            switch (id) {
                case R.id.comments_cab_delete:
                    deleteComments();
                    break;
                case R.id.comments_cab_approve:
                    moderateComments(CommentStatus.APPROVED);
                    break;
                case R.id.comments_cab_unapprove:
                    moderateComments(CommentStatus.UNAPPROVED);
                    break;
                case R.id.comments_cab_spam:
                    moderateComments(CommentStatus.SPAM);
                    break;
                default:
                    retVal = false;
            }
            return retVal;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ListView listView = getListView();
            listView.clearChoices();
            listView.invalidateViews();
            mPriorCheckedCommentPositions.clear();
            mActionMode = null;
        }
    }
}