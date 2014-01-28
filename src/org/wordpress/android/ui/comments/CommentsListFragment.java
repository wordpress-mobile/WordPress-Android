package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

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
import org.wordpress.android.util.ListScrollPositionManager;
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

public class CommentsListFragment extends ListFragment {
    public ArrayList<Comment> model = null;
    public Map<Integer, Map<?, ?>> allComments = new HashMap<Integer, Map<?, ?>>();

    protected static final int ID_DIALOG_MODERATING = 1;
    protected static final int ID_DIALOG_DELETING = 3;
    private static final int COMMENTS_PER_PAGE = 30;

    protected boolean shouldSelectAfterLoad = false;
    private String moderateErrorMsg = "";
    private int scrollPosition = 0,
                scrollPositionTop = 0;
    protected int checkedCommentTotal = 0; // TODO: this is never set!
    private GetRecentCommentsTask mGetCommentsTask;

    private ViewSwitcher mSwitcher;
    private HashSet<Integer> selectedCommentPositions = new HashSet<Integer>();
    private OnCommentSelectedListener mOnCommentSelectedListener;
    private OnAnimateRefreshButtonListener mOnAnimateRefreshButton;
    private CommentActions.OnCommentChangeListener mOnCommentChangeListener;
    private ListScrollPositionManager mListScrollPositionManager;

    // context menu IDs
    protected static final int MENU_ID_APPROVED = 100;
    protected static final int MENU_ID_UNAPPROVED = 101;
    protected static final int MENU_ID_SPAM = 102;
    protected static final int MENU_ID_DELETE = 103;
    protected static final int MENU_ID_EDIT = 105;

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
            mOnCommentSelectedListener = (OnCommentSelectedListener) activity;
            mOnAnimateRefreshButton = (OnAnimateRefreshButtonListener) activity;
            mOnCommentChangeListener = (CommentActions.OnCommentChangeListener) activity;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.view_comments_fragment, container, false);

        mSwitcher = new ViewSwitcher(getActivity());

        Button footer = (Button) View.inflate(getActivity(), R.layout.list_footer_btn, null);
        footer.setText(getResources().getText(R.string.load_more) + " " + getResources().getText(R.string.tab_comments));

        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mSwitcher.showNext();
                refreshComments(true);
            }
        });

        View progress = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        mSwitcher.addView(footer);
        mSwitcher.addView(progress);

        final Button deleteComments = (Button) v.findViewById(R.id.bulkDeleteComment);
        final Button approveComments = (Button) v.findViewById(R.id.bulkApproveComment);
        final Button unapproveComments = (Button) v.findViewById(R.id.bulkUnapproveComment);
        final Button spamComments = (Button) v.findViewById(R.id.bulkMarkSpam);

        deleteComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                getActivity().showDialog(ID_DIALOG_DELETING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        deleteComments();
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
                        moderateComments(CommentStatus.APPROVED);
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
                        moderateComments(CommentStatus.UNAPPROVED);
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
                        moderateComments(CommentStatus.SPAM);
                    }
                }.start();
            }
        });

        return v;
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
    protected void moderateComments(CommentStatus newStatus) {
        final String newStatusStr = CommentStatus.toString(newStatus);
        final Blog blog = WordPress.currentBlog;

        // handles bulk moderation
        Iterator it = selectedCommentPositions.iterator();
        final List<Comment> commentsUpdatedList = new LinkedList<Comment>();
        while (it.hasNext()) {
            int i = (Integer) it.next();
            XMLRPCClient client = new XMLRPCClient(
                    blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

            Comment listRow = (Comment) getListView().getItemAtPosition(i);
            int curCommentID = listRow.commentID;

            Map<String, String> contentHash, postHash = new HashMap<String, String>();
            contentHash = (Map<String, String>) allComments.get(curCommentID);

            if (contentHash.get("status").equals(newStatusStr)) {
                it.remove();
                continue;
            }

            postHash.put("status", newStatusStr);
            postHash.put("content", contentHash.get("comment"));
            postHash.put("author", contentHash.get("author"));
            postHash.put("author_url", contentHash.get("url"));
            postHash.put("author_email", contentHash.get("email"));

            Object[] params = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    curCommentID,
                    postHash };

            moderateErrorMsg = "";
            Object result;
            try {
                result = client.call("wp.editComment", params);
                boolean bResult = Boolean.parseBoolean(result.toString());
                if (bResult) {
                    it.remove();
                    listRow.setStatus(newStatusStr);
                    contentHash.put("status", newStatusStr);
                    model.set(i, listRow);
                    WordPress.wpDB.updateCommentStatus(WordPress.currentBlog.getLocalTableBlogId(), listRow.commentID, newStatusStr);
                    commentsUpdatedList.add(WordPress.wpDB.getComment(WordPress.currentBlog.getLocalTableBlogId(), listRow.commentID));
                }
            } catch (XMLRPCException e) {
                moderateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
            }
        }
        dismissDialog(ID_DIALOG_MODERATING);

        if (hasActivity()) {
            Thread action = new Thread() {
                public void run() {
                    hideModerationBar();
                    if (TextUtils.isEmpty(moderateErrorMsg)) {
                        String msg = getResources().getText(R.string.comment_moderated).toString();
                        if (checkedCommentTotal > 1)
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        checkedCommentTotal = 0;
                        mOnCommentChangeListener.onCommentsModerated(commentsUpdatedList);

                        // update the comment counter on the menu drawer
                        ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                    } else if (!getActivity().isFinishing()) {
                        // there was an xmlrpc error
                        checkedCommentTotal = 0;
                        getListView().invalidateViews();
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(moderateErrorMsg);
                        ft.add(alert, "alert");
                        ft.commitAllowingStateLoss();
                    }
                }
            };
            getActivity().runOnUiThread(action);
        }
    }

    protected void deleteComments() {
        // bulk delete comments
        for (int i : selectedCommentPositions) {
            XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Comment listRow = (Comment) getListView().getItemAtPosition(i);
            int curCommentID = listRow.commentID;

            Object[] params = {WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), curCommentID};

            moderateErrorMsg = "";
            try {
                client.call("wp.deleteComment", params);
            } catch (final XMLRPCException e) {
                moderateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
            }
        }
        dismissDialog(ID_DIALOG_DELETING);

        if (hasActivity()) {
            Thread action = new Thread() {
                public void run() {
                    if (TextUtils.isEmpty(moderateErrorMsg)) {
                        final String msg;
                        if (checkedCommentTotal > 1) {
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        } else {
                            msg = getResources().getText(R.string.comment_moderated).toString();
                        }
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        checkedCommentTotal = 0;
                        hideModerationBar();
                        selectedCommentPositions.clear();
                        mOnCommentChangeListener.onCommentDeleted();
                    } else if (!getActivity().isFinishing()) {
                        // error occurred during delete request
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(moderateErrorMsg);
                        ft.add(alert, "alert");
                        ft.commitAllowingStateLoss();
                    }
                }
            };
            getActivity().runOnUiThread(action);
        }
    }

    protected boolean loadComments() {
        return loadComments(false);
    }
    private boolean loadComments(boolean didLoadMore) {
        String author, postID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        List<Map<String, Object>> loadedComments = WordPress.wpDB.loadComments(WordPress.currentBlog.getLocalTableBlogId());

        if (model != null) {
            model.clear();
        } else {
            model = new ArrayList<Comment>();
        }

        if (loadedComments == null) {
            setUpListView(false);
            return false;
        }

        for (int i = 0; i < loadedComments.size(); i++) {
            Map<String, Object> contentHash = loadedComments.get(i);
            allComments.put((Integer) contentHash.get("commentID"), contentHash);
            author = StringUtils.unescapeHTML(contentHash.get("author").toString());
            commentID = (Integer) contentHash.get("commentID");
            postID = contentHash.get("postID").toString();
            comment = contentHash.get("comment").toString();
            dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
            status = contentHash.get("status").toString();
            authorEmail = StringUtils.unescapeHTML(contentHash.get("email").toString());
            authorURL = StringUtils.unescapeHTML(contentHash.get("url").toString());
            postTitle = StringUtils.unescapeHTML(contentHash.get("postTitle").toString());

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

        boolean showSwitcher = loadedComments.size() % COMMENTS_PER_PAGE == 0;
        setUpListView(showSwitcher);

        if (this.shouldSelectAfterLoad) {
            if (model != null) {
                if (model.size() > 0) {
                    Comment aComment = model.get(0);
                    mOnCommentSelectedListener.onCommentSelected(aComment);
                    getListView().setItemChecked(0, true);
                }
            }
            shouldSelectAfterLoad = false;
        }

        if (didLoadMore && scrollPosition > 0) {
            try {
                getListView().setSelectionFromTop(scrollPosition, scrollPositionTop);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mListScrollPositionManager.restoreScrollOffset();

        return true;
    }

    private void setUpListView(boolean showSwitcher) {
        ListView listView = this.getListView();
        listView.removeFooterView(mSwitcher);
        if (showSwitcher) {
            listView.addFooterView(mSwitcher);
            mSwitcher.setDisplayedChild(0);
        }
        setListAdapter(new CommentAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                // make sure position is in bounds (prevents ArrayIndexOutOfBoundsException that
                // would otherwise occur when footer is tapped)
                if (position < 0 || position >= model.size()) {
                    return;
                }
                Comment comment = model.get(position);
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
                WordPress.currentComment = model.get(info.position);
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
    private void refreshComments(boolean isLoadingMore) {
        mListScrollPositionManager.saveScrollOffset();

        if (!isLoadingMore) {
            mOnAnimateRefreshButton.onAnimateRefreshButton(true);
        }

        Map<String, Object> hPost = new HashMap<String, Object>();
        if (isLoadingMore) {
            ListView listView = this.getListView();
            scrollPosition = listView.getFirstVisiblePosition();
            View firstChild = listView.getChildAt(0);
            scrollPositionTop = (firstChild == null) ? 0 : firstChild.getTop();
            int numExisting = allComments.size();
            // TODO: use "offset" rather than "number" to limit result
            // http://codex.wordpress.org/XML-RPC/wp.getComments
            hPost.put("number", numExisting + COMMENTS_PER_PAGE);
        } else {
            hPost.put("number", COMMENTS_PER_PAGE);
        }

        Object[] params = { WordPress.currentBlog.getRemoteBlogId(),
                            WordPress.currentBlog.getUsername(),
                            WordPress.currentBlog.getPassword(),
                            hPost };

        mGetCommentsTask = new GetRecentCommentsTask(params);
        mGetCommentsTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class CommentAdapter extends ArrayAdapter<Comment> {
        private LayoutInflater mInflater;
        CommentAdapter() {
            super(getActivity(), R.layout.comment_row, model);
            mInflater = LayoutInflater.from(getActivity());
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final CommentEntryWrapper wrapper;

            if (convertView == null || convertView.getTag() == null) {
                convertView = mInflater.inflate(R.layout.comment_row, null);
                wrapper = new CommentEntryWrapper(convertView);
                convertView.setTag(wrapper);
            } else {
                wrapper = (CommentEntryWrapper) convertView.getTag();
            }
            Comment commentEntry = getItem(position);
            wrapper.populateFrom(commentEntry, position);

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

            // locate views
            txtName = (TextView) row.findViewById(R.id.name);
            txtEmailURL = (TextView) row.findViewById(R.id.email_url);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status); // setTextSize(12)
            txtPostTitle = (TextView) row.findViewById(R.id.postTitle);
            bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);

            imgAvatar = (NetworkImageView) row.findViewById(R.id.avatar);
            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
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
                    textColor = "#006505";
                    break;
            }

            txtStatus.setText(status);
            txtStatus.setTextColor(Color.parseColor(textColor));

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

            if (comment.hasProfileImageUrl()) {
                imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(comment.getProfileImageUrl()), WordPress.imageLoader);
            } else {
                imgAvatar.setImageResource(R.drawable.placeholder);
            }
        }
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
        if (selectedCommentPositions.size() > 0) {
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

    private class GetRecentCommentsTask extends AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {
        Object[] commentParams;
        boolean isError;

        private GetRecentCommentsTask(Object[] params) {
            commentParams = params;
        }

        @Override
        protected Map<Integer, Map<?, ?>> doInBackground(Void... args) {
            if (!hasActivity())
                return null;
            try {
                return ApiHelper.refreshComments(getActivity(), commentParams);
            } catch (XMLRPCException e) {
                isError = true;
                return null;
            }
        }

        protected void onPostExecute(Map<Integer, Map<?, ?>> commentsResult) {
            if (isCancelled() || !hasActivity())
                return;

            mOnAnimateRefreshButton.onAnimateRefreshButton(false);
            mSwitcher.setDisplayedChild(0);
            showOrHideModerationBar();

            if (commentsResult == null) {
                if (model != null && model.size() == 1) {
                    WordPress.wpDB.clearComments(WordPress.currentBlog.getLocalTableBlogId());
                    model.clear();
                    allComments.clear();
                    getListView().invalidateViews();
                    WordPress.currentComment = null;
                    loadComments(false);
                }

                if (isError && !getActivity().isFinishing()) {
                    ToastUtils.showToast(getActivity(), R.string.error_refresh_comments, ToastUtils.Duration.LONG);
                }

                return;
            }

            boolean didLoadMore = (commentsResult.size() > 0);
            if (commentsResult.size() > 0) {
                allComments.putAll(commentsResult);
                loadComments(didLoadMore);
            }
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
}