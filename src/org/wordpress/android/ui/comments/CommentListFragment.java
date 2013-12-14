package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
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
    private SparseBooleanArray mSavedSelectedCommentPositions = null;
    private OnAnimateRefreshButtonListener onAnimateRefreshButton;
    private CommentListFragmentListener mOnCommentListFragmentListener;
    private CommentAsyncModerationReturnListener  mCommentAsyncModerationReturnListener;
    private ListScrollPositionManager mListScrollPositionManager;

    public interface CommentListFragmentListener {
        public void onCommentClicked(Comment comment);
        public void onCommentSelected(int selectedCommentCount);
    }

    public interface OnAnimateRefreshButtonListener {
        public void onAnimateRefreshButton(boolean start);
    }

    public interface CommentAsyncModerationReturnListener {
        public void onAsyncModerationReturnSuccess(CommentStatus commentStatus);
        public void onAsyncModerationReturnFailure(CommentStatus commentStatus);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        TextView textview = (TextView) getListView().getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.comments_empty_list));
        }
        mListScrollPositionManager = new ListScrollPositionManager(getListView(), false);

        /* TODO: JCO - Need to weigh the pros and cons checking the view versus the model as it is
         * not always the case that we need to redraw a comment nor fetch it from the server as
         * even though the activity may have been destroyed, the fragment + data was added to the
         * BackStack and we should use said data as we can avoid needless redraw(s), if done correctly.
         * TODO: JCO - Note: In the case of the server data vs. the local database we still need to
         * account for changes from another client, on the server regardless of our local data. */
        /* TODO: JCO - Eventually replace with the work to compare the expected change set w/ the set
         * of comments actually changed (server + view). */
         refreshComments();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnCommentListFragmentListener = (CommentListFragmentListener) activity;
            mCommentAsyncModerationReturnListener = (CommentAsyncModerationReturnListener) activity;
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
        mSavedSelectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
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

    private static int getNumberOfToggledValues(SparseBooleanArray sparseBooleanArray) {
        int numberOfTrueValues = 0;
        for(int i=0; i< sparseBooleanArray.size(); i++) {
            if(sparseBooleanArray.valueAt(i)) {
                numberOfTrueValues++;
            }
        }
        return numberOfTrueValues;
    }

    /**
     * This function is used to update the local data models and views after completion of some RPC
     * server action.
     */
    private void updateChangedCommentSet(ArrayList<Comment> selectedCommentsSnapshot, SparseBooleanArray moderatedComments, String newStatusStr) {
        int currentCommentId;

        if (moderatedComments.indexOfValue(true) != -1) {
            for(Comment currentComment : selectedCommentsSnapshot) {
                currentCommentId = currentComment.commentID;
                if (moderatedComments.get(currentCommentId, false)) {
                    currentComment.setStatus(newStatusStr);
                    replaceComment(currentComment);
                    WordPress.wpDB.updateCommentStatus(WordPress.getCurrentBlogId(), currentCommentId, newStatusStr);
                }
            }
        }
    }

    /**
     * Start an AsyncTask to moderate the current comment selection set
     *
     * @param commentStatus The status to moderate the currently selected comment set type
     */
    public void moderateComments(final CommentStatus commentStatus) {
        final String newStatus = CommentStatus.toString(commentStatus);
        final ArrayList<Integer> selectedCommentIds = getSelectedCommentIdArray();
        final ArrayList<Comment> selectedCommentsSnapshot = getSelectedCommentArray();

        ApiHelper.ModerateCommentsTask task = new ApiHelper.ModerateCommentsTask(newStatus,
                allComments, selectedCommentIds,
                new ApiHelper.ModerateCommentsTask.Callback() {
                    String messageBarText;
                    int numCommentsModerated = 0;

                    @Override
                    public void onSuccess(SparseBooleanArray moderatedComments) {
                        mCommentsUpdating = false;
                        updateChangedCommentSet(selectedCommentsSnapshot, moderatedComments, newStatus);

                        if (getActivity() != null) {
                            numCommentsModerated = getNumberOfToggledValues(moderatedComments);
                            if (numCommentsModerated == 1) {
                                messageBarText = getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText = getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(commentStatus);
                    }
                    @Override
                    public void onCancelled(SparseBooleanArray moderatedComments) {
                        // For the time being we will update any comments that were changed on the server
                        mCommentsUpdating = false;
                        updateChangedCommentSet(selectedCommentsSnapshot, moderatedComments, newStatus);

                        if (getActivity() != null) {
                            numCommentsModerated = getNumberOfToggledValues(moderatedComments);
                            if (numCommentsModerated == 1) {
                                messageBarText = getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText = getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(commentStatus);
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
    public void deleteComments() {
        final ArrayList<Integer> selectedCommentIds = getSelectedCommentIdArray();

        ApiHelper.DeleteCommentsTask task = new ApiHelper.DeleteCommentsTask(allComments,
                selectedCommentIds,
                new ApiHelper.DeleteCommentsTask.Callback() {
                    String messageBarText;
                    int numCommentsDeleted = 0;

                    @Override
                    public void onSuccess(SparseBooleanArray deletedCommentIds) {
                        mCommentsUpdating = false;

                        if (getActivity() != null) {
                            numCommentsDeleted = getNumberOfToggledValues(deletedCommentIds);
                            if (numCommentsDeleted == 1) {
                                messageBarText =
                                        getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText =
                                        getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(CommentStatus.TRASH);
                    }
                    @Override
                    public void onCancelled(SparseBooleanArray deletedCommentIds) {
                        mCommentsUpdating = false;

                        if (getActivity() != null) {
                            numCommentsDeleted = getNumberOfToggledValues(deletedCommentIds);
                            if (numCommentsDeleted == 1) {
                                messageBarText =
                                        getActivity().getString(R.string.comment_moderated);
                            } else {
                                messageBarText =
                                        getActivity().getString(R.string.comments_moderated);
                            }
                            MessageBarUtils.showMessageBar(getActivity(), messageBarText);
                        }
                        mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(CommentStatus.TRASH);
                    }
                    @Override
                    public void onFailure() {
                        mCommentsUpdating = false;
                        if (getActivity() != null) {
                            MessageBarUtils.showMessageBar(getActivity(), getActivity().getString(R.string.error_moderate_comment));
                        }
                        mCommentAsyncModerationReturnListener.onAsyncModerationReturnFailure(CommentStatus.TRASH);
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
                        Comment aComment = model.get(0);
                        mOnCommentListFragmentListener.onCommentClicked(aComment);
                    }
                }
                shouldSelectAfterLoad = false;
            }

            if (loadMore && scrollPosition > 0) {
                ListView listView = this.getListView();
                try {
                    listView.setSelectionFromTop(scrollPosition, scrollPositionTop);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mListScrollPositionManager.restoreScrollOffset();

            if (mSavedSelectedCommentPositions != null) {
                ((CommentAdapter) getListAdapter()).mSelectedCommentPositions =
                        mSavedSelectedCommentPositions;
            }

            return true;
        } else {
            setUpListView(false);
            return false;
        }
    }

    private void setUpListView(boolean showSwitcher) {
        ListView listView = getListView();

        listView.removeFooterView(switcher);
        if (showSwitcher) { listView.addFooterView(switcher, null, false); }

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemCheck(position);
                return true;
            }
        });
        setListAdapter(new CommentAdapter(getSherlockActivity(), model));
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        int selectedCommentCount = getSelectedCommentCount();

        if(selectedCommentCount == 0) {
            Comment comment = model.get((int) id);
            mOnCommentListFragmentListener.onCommentClicked(comment);
            getListView().invalidateViews();
        } else {
            onListItemCheck((int) id);
        }
    }

    private void onListItemCheck(int id) {
        int selectedCommentCount;

        toggleCommentSelected(id);
        mSavedSelectedCommentPositions =
                ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
        selectedCommentCount = getSelectedCommentCount();
        mOnCommentListFragmentListener.onCommentSelected(selectedCommentCount);
    }

    public ArrayList<Integer> getSelectedCommentIdArray() {
        SparseBooleanArray selectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
        ArrayList<Integer> selectedCommentIdArray = new ArrayList<Integer>();

        for(int i=0; i<selectedCommentPositions.size(); i++) {
            if (selectedCommentPositions.valueAt(i))
                selectedCommentIdArray.add(selectedCommentPositions.keyAt(i));
        }

        return selectedCommentIdArray;
    }

    public ArrayList<Comment> getSelectedCommentArray() {
        ArrayList<Integer> selectedCommentIdArray = getSelectedCommentIdArray();
        ArrayList<Comment> selectedCommentArray = new ArrayList<Comment>();

        for(Comment comment : model) {
            if (selectedCommentIdArray.contains(comment.commentID)) {
                selectedCommentArray.add(comment);
            }
        }

        return selectedCommentArray;
    }

    public int getSelectedCommentCount() {
        SparseBooleanArray selectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
        int size = 0;
        for (int i=0; i<selectedCommentPositions.size(); i++) {
            if (selectedCommentPositions.valueAt(i)) {
                size++;
            }
        }
        return size;
    }

    public void toggleCommentSelected(int position) {
        SparseBooleanArray selectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
        boolean isSelected = true;
        Comment comment;
        int commentId;

        comment = model.get(position);
        commentId = comment.commentID;

        if (selectedCommentPositions.indexOfKey(commentId) == -1) {
            selectedCommentPositions.put(commentId, isSelected);
        } else {
            isSelected = !selectedCommentPositions.get(commentId);
            selectedCommentPositions.put(commentId, isSelected);
        }

        if (isSelected) {
            WordPress.currentComment = comment;
        } else { }

        ((CommentAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Clears the selected comment list as well as the saved set of selected comments.
     */
    public void clearSelectedComments() {
        CommentAdapter commentListAdapter = ((CommentAdapter) getListAdapter());
        commentListAdapter.mSelectedCommentPositions.clear();
        if (mSavedSelectedCommentPositions != null) { mSavedSelectedCommentPositions.clear(); }
        commentListAdapter.notifyDataSetChanged();
    }

    public class CommentAdapter extends ArrayAdapter<Comment> {
        final LayoutInflater mInflater;
        SparseBooleanArray mSelectedCommentPositions;
        boolean detailViewVisible = false;

        public CommentAdapter(Context context, ArrayList<Comment> objs) {
            super(context, R.layout.comment_row, objs);
            mSelectedCommentPositions = new SparseBooleanArray();
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
            row.setBackgroundColor(mSelectedCommentPositions.get(commentEntry.commentID, false)?
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

            row.setId(Integer.valueOf(comment.commentID));

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

    /**
     * Replace existing comment with the passed in value
     * @param comment The comment with the same postID and commentID that is to be replaced in the
     *                model
     */
    protected void replaceComment(Comment comment) {
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Calling this function will update data from the server, update the local database and update
     * the data model. Use sparingly as it is not cheap.
     */
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
            ListView listView = this.getListView();
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

                    // TODO: JCO - Review
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
}