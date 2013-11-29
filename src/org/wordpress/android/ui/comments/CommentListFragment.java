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
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
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
    public getRecentCommentsTask getCommentsTask;

    private XMLRPCClient client;
    private String accountName = "", moderateErrorMsg = "";
    private ViewSwitcher switcher;
    private boolean loadMore = false, doInBackground = false, refreshOnly = false,
            mCommentsUpdating = false;
    private Object[] mCommentParams;
    private OnAnimateRefreshButtonListener onAnimateRefreshButton;
    private CommentListFragmentListener mOnCommentListFragmentListener;
    private CommentAsyncModerationReturnListener  mCommentAsyncModerationReturnListener;
    private View mFooterSpacer;
    private ListScrollPositionManager mListScrollPositionManager;
    private SparseBooleanArray mSavedSelectedCommentPositions = null;

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

        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switcher.showNext();
                refreshComments(true);
            }
        });

        mFooterSpacer = new View(getActivity());
        mFooterSpacer.setLayoutParams(new AbsListView.LayoutParams(10, 0));

        View progress = View.inflate(getActivity().getApplicationContext(),
                R.layout.list_footer_progress, null);

        switcher.addView(footer);
        switcher.addView(progress);

        getActivity().setTitle(accountName + " - Moderate Comments");

        return v;
    }

    //TODO: JCO - Remove if a common use between del/mod Comments()
    private void setCommentsUpdating(boolean updating) {
        mCommentsUpdating = updating;

        if (!updating) {
            //TODO: JCO. getSherlockActivity().invalidateOptionsMenu();
            //((CommentAdapter) getListAdapter()).notifyDataSetInvalidated();
        }
    }

    private boolean commentsUpdating() {
        return mCommentsUpdating;
    }

    /**
     * TODO: JCO - Add javadoc
     * @param commentStatus
     */
    public void moderateComments(final CommentStatus commentStatus) {
        final String newStatus =
                CommentStatus.toString(commentStatus, CommentStatus.ApiFormat.XMLRPC);
        final ArrayList<Integer> selectedCommentIds = getSelectedCommentIdArray();

        ApiHelper.ModerateCommentsTask task = new ApiHelper.ModerateCommentsTask(newStatus,
                allComments, selectedCommentIds,
                new ApiHelper.ModerateCommentsTask.Callback() {
                    @Override
                    public void onSuccess() {
                        setCommentsUpdating(false);
                        refreshComments(commentStatus);
                        //mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(commentStatus);
                    }
                    @Override
                    public void onFailure() {
                        getSherlockActivity().invalidateOptionsMenu();
                        ((CommentAdapter) getListAdapter()).notifyDataSetInvalidated();
                        setCommentsUpdating(false);
                        // TODO: JCO - Housekeeping + Inform user of failure (failure callback)
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());

        if(!commentsUpdating()) {
            setCommentsUpdating(true);
            task.execute(apiArgs);
        }
    }

    /**
     * TODO: JCO - We can prob. either call moderateComments() from this function or do away with deleteComments() (reduce code duplication)
     * TODO: JCO - Add javadoc
     *
     */
    public void deleteComments() {
        final ArrayList<Integer> selectedCommentIdArray = getSelectedCommentIdArray();

        ApiHelper.DeleteCommentsTask task = new ApiHelper.DeleteCommentsTask(allComments,
                selectedCommentIdArray,
                new ApiHelper.DeleteCommentsTask.Callback() {
                    @Override
                    public void onSuccess() {
                        setCommentsUpdating(false);
                        refreshComments(CommentStatus.TRASH);
                        //mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(CommentStatus.TRASH);
                    }
                    @Override
                    public void onFailure() {
                        setCommentsUpdating(false);
                        // TODO: JCO - Housekeeping + Inform user of failure (failure callback)
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());

        if(!commentsUpdating()) {
            // TODO: JCO - Need to animate?!
            setCommentsUpdating(true);
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
        listView.removeFooterView(mFooterSpacer);

        if (showSwitcher) {
            listView.addFooterView(switcher);
        }
        listView.addFooterView(mFooterSpacer);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                /* TODO: JCO - Part 1/2: There is a case here where we press "Load More Comments", let it load,
                 * leave/pause the app and then when we come back if we try and select the ViewSwitcher
                 * button (which is constantly animating) we will get back and INVALID_ITEM_ID from the framework
                 * of -1. Putting in a temp. check for right now. Will fix by 12/13/13.
                 */
                if (position != -1) {
                    onListItemCheck(position);
                }

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

    /**
     * TODO: JCO - Moved non-item specific methods out of the Adapter and into the fragment
     * @return
     */
    public ArrayList<Integer> getSelectedCommentIdArray() {
        SparseBooleanArray selectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;
        ArrayList<Integer> selectedCommentIdArray = new ArrayList<Integer>();

        for(int i=0; i<selectedCommentPositions.size(); i++) {
            if (selectedCommentPositions.valueAt(i))
                selectedCommentIdArray.add(selectedCommentPositions.keyAt(i));
        }

        return selectedCommentIdArray;
    }

    /**
     * TODO: JCO - Moved non-item specific methods out of the Adapter and into the fragment
     * @return
     */
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

    /**
     * TODO: JCO - Moved non-item specific methods out of the Adapter and into the fragment
     * @return
     */
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

    /**
     * TODO: JCO - Document
     * @param position
     */
    public void toggleCommentSelected(int position) {
        /* TODO: JCO - Part 2/2: There is a case here where we press "Load More Comments", let it load,
         * leave/pause the app and then when we come back if we try and long select the ViewSwitcher
         * button (which is constantly animating) we will get back and, sometimes, from the framework
         * an index of 30 when the model has only 30 elements. I think I know the issue here as opposed to 1/2.
         * Putting in a temp. check for right now. Will fix by 12/13/13.
         */
        if (position >= model.size()) { return; }

        SparseBooleanArray selectedCommentPositions = ((CommentAdapter) getListAdapter()).mSelectedCommentPositions;

        Comment comment = model.get(position);
        int currentId = comment.commentID;
        boolean isSelected = true;

        if (selectedCommentPositions.indexOfKey(currentId) == -1) {
            selectedCommentPositions.put(currentId, isSelected);
        } else {
            isSelected = selectedCommentPositions.get(currentId);
            selectedCommentPositions.put(currentId, !isSelected);
        }

        if (isSelected) {
            WordPress.currentComment = comment;
        } else {
            WordPress.currentComment = null;
        }
        ((CommentAdapter) getListAdapter()).notifyDataSetChanged(); //TODO: JCO - Do I need to redraw the entire visible row set or can I just draw the row/background in question?
    }

    /**
     * Clears the selected comment list as well as the saved set of selected comments.
     */
    public void clearSelectedComments() {
        CommentAdapter commentListAdapter = ((CommentAdapter) getListAdapter());
        commentListAdapter.mSelectedCommentPositions.clear();
        if (mSavedSelectedCommentPositions != null) { mSavedSelectedCommentPositions.clear(); }
        commentListAdapter.notifyDataSetChanged(); //TODO: JCO - Do I need to redraw the entire visible row set or can I just set the set of row background in question?
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

    // TODO: JCO - Need to find where I removed this call and check the functionality
    private void setFooterSpacerVisible(boolean visible) {
        LayoutParams params = (LayoutParams) mFooterSpacer.getLayoutParams();
        if (visible)
            params.height = getResources().getDimensionPixelSize(R.dimen.comments_moderation_bar_height);
        else
            params.height = 0;
        mFooterSpacer.setLayoutParams(params);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * TODO: JCO
     */
    public void refreshComments() {
        refreshComments(false, CommentStatus.UNKNOWN);
    }

    /**
     * TODO: JCO
     * @param loadMore
     */
    public void refreshComments(boolean loadMore) {
        refreshComments(loadMore, CommentStatus.UNKNOWN);
    }

    /**
     * TODO: JCO
     * @param newlyModeratedCommentStatus
     */
    public void refreshComments(CommentStatus newlyModeratedCommentStatus) {
        refreshComments(false, newlyModeratedCommentStatus);
    }

    /**
     * TODO: JCO
     * @param loadMore
     * @param newlyModeratedCommentStatus
     */
    public void refreshComments(boolean loadMore, CommentStatus newlyModeratedCommentStatus) {
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
        getCommentsTask = new getRecentCommentsTask(newlyModeratedCommentStatus);
        getCommentsTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class getRecentCommentsTask extends AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {
        private final CommentStatus mNewlyModeratedCommentStatus;

        getRecentCommentsTask(CommentStatus newlyModeratedCommentStatus) {
           mNewlyModeratedCommentStatus = newlyModeratedCommentStatus;
        }

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

                     if (commentsResult.size() > 0) {
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

            // TODO: JCO - FYI ... I do not like this. I need to break up refreshComments or spawn a listener to see when refreshRecentComments' onPostExecute completes ... so I can make the callback
            if (mNewlyModeratedCommentStatus != CommentStatus.UNKNOWN) {
                mCommentAsyncModerationReturnListener.onAsyncModerationReturnSuccess(mNewlyModeratedCommentStatus);
            }
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

    /*
     * replace existing comment with the passed one and refresh list to show changes
     */
    protected void replaceComment(Comment comment) {
        if (comment==null || model==null)
            return;
        for (int i=0; i < model.size(); i++) {
            Comment thisComment = model.get(i);
            if (thisComment.commentID==comment.commentID && thisComment.postID==comment.postID) {
                model.set(i, comment);
                getListView().invalidateViews();
                return;
            }
        }
    }
}