package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
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
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
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
    public int ID_DIALOG_MODERATING = 1;
    public int ID_DIALOG_DELETING = 3;
    public static final int COMMENTS_PER_PAGE = 30;
    public boolean shouldSelectAfterLoad = false;
    public int numRecords = 0,
               checkedCommentTotal = 0, // TODO: this is never set!
               selectedPosition,
               scrollPosition = 0,
               scrollPositionTop = 0;
    public ProgressDialog progressDialog;
    public GetRecentCommentsTask getCommentsTask;

    private XMLRPCClient client;
    private String moderateErrorMsg = "";
    private ViewSwitcher switcher;
    private boolean loadMore = false, refreshOnly = false;
    private HashSet<Integer> selectedCommentPositions = new HashSet<Integer>();
    private Object[] commentParams;
    private OnCommentSelectedListener mOnCommentSelectedListener;
    private OnAnimateRefreshButtonListener mOnAnimateRefreshButton;
    private CommentActions.OnCommentChangeListener mOnCommentChangeListener;
    private View mFooterSpacer;
    private ListScrollPositionManager mListScrollPositionManager;

    private int mStatusColorSpam;
    private int mStatusColorUnapproved;

    // context menu IDs
    protected static final int MENU_ID_APPROVED = 100;
    protected static final int MENU_ID_UNAPPROVED = 101;
    protected static final int MENU_ID_SPAM = 102;
    protected static final int MENU_ID_DELETE = 103;
    protected static final int MENU_ID_EDIT = 105;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mStatusColorSpam = Color.parseColor("#FF0000");
        mStatusColorUnapproved = Color.parseColor("#D54E21");
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

        Button deleteComments = (Button) v.findViewById(R.id.bulkDeleteComment);

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

        Button approveComments = (Button) v
                .findViewById(R.id.bulkApproveComment);

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

        Button unapproveComments = (Button) v
                .findViewById(R.id.bulkUnapproveComment);

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

        Button spamComments = (Button) v.findViewById(R.id.bulkMarkSpam);

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
        Iterator it= selectedCommentPositions.iterator();
        final List<Comment> commentsUpdatedList = new LinkedList<Comment>();
        while (it.hasNext()) {
            int i = (Integer) it.next();
            client = new XMLRPCClient(
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
                    if (moderateErrorMsg == "") {
                        String msg = getResources().getText(R.string.comment_moderated).toString();
                        if (checkedCommentTotal > 1)
                            msg = getResources().getText(R.string.comments_moderated).toString();
                        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        checkedCommentTotal = 0;
                        hideModerationBar();
                        mOnCommentChangeListener.onCommentsModerated(commentsUpdatedList);

                        // update the comment counter on the menu drawer
                        ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                    } else {
                        // there was an xmlrpc error
                        if (!getActivity().isFinishing()) {
                            checkedCommentTotal = 0;
                            hideModerationBar();
                            getListView().invalidateViews();
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(moderateErrorMsg);
                            ft.add(alert, "alert");
                            ft.commitAllowingStateLoss();
                        }
                        moderateErrorMsg = "";
                    }
                }
            };
            getActivity().runOnUiThread(action);
            progressDialog = new ProgressDialog(getActivity().getApplicationContext());
        }
    }

    protected void deleteComments() {
        // bulk delete comments
        for (int i : selectedCommentPositions) {
            client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
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
                        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        checkedCommentTotal = 0;
                        hideModerationBar();
                        selectedCommentPositions.clear();
                        mOnCommentChangeListener.onCommentDeleted();
                    } else {
                        // error occurred during delete request
                        if (!getActivity().isFinishing()) {
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(moderateErrorMsg);
                            ft.add(alert, "alert");
                            ft.commitAllowingStateLoss();
                        }
                    }
                }
            };
            getActivity().runOnUiThread(action);
            progressDialog = new ProgressDialog(getActivity().getApplicationContext());
        }
    }

    public boolean loadComments(boolean refresh, boolean loadMore) {
        refreshOnly = refresh;
        String author, postID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        List<Map<String, Object>> loadedComments = WordPress.wpDB.loadComments(WordPress.currentBlog.getLocalTableBlogId());

        if (model != null) {
            model.clear();
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
                comment = contentHash.get("comment").toString();
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
                        Comment aComment = model.get((int) 0);
                        mOnCommentSelectedListener.onCommentSelected(aComment);
                        getListView().setItemChecked(0, true);
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
            return true;
        } else {
            setUpListView(false);
            return false;
        }
    }

    private void setUpListView(boolean showSwitcher) {
        ListView listView = this.getListView();
        listView.removeFooterView(switcher);
        listView.removeFooterView(mFooterSpacer);
        if (showSwitcher) {
            switcher.setDisplayedChild(0);
            listView.addFooterView(switcher);
        }
        listView.addFooterView(mFooterSpacer);
        setListAdapter(new CommentAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                // make sure position is in bounds (prevents ArrayIndexOutOfBoundsException that
                // would otherwise occur when footer is tapped)
                if (position < 0 || position >= model.size()) {
                    return;
                }
                selectedPosition = position;
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

                // make sure this is a valid position (long-tapping footer will pass invalid position)
                if (info.position < 0 || info.position >= model.size()) {
                    menu.clear();
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

    protected void clearComments() {
        if (model != null && model.size() > 0) {
            model.clear();
            allComments.clear();
            ((ArrayAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    public void refreshComments() {
        refreshComments(false);
    }
    public void refreshComments(boolean loadMore) {
        mListScrollPositionManager.saveScrollOffset();

        if (!loadMore) {
            mOnAnimateRefreshButton.onAnimateRefreshButton(true);
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

        Object[] params = { WordPress.currentBlog.getRemoteBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), hPost };

        commentParams = params;
        getCommentsTask = new GetRecentCommentsTask();
        getCommentsTask.execute();
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

            // status is only shown for comments that aren't approved
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
        setFooterSpacerVisible(false);
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
        setFooterSpacerVisible(true);
    }

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

    class GetRecentCommentsTask extends AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {
        protected void onPostExecute(Map<Integer, Map<?, ?>> commentsResult) {
            if (isCancelled() || !hasActivity())
                return;

            if (commentsResult == null) {
                if (model != null && model.size() == 1) {
                    WordPress.wpDB.clearComments(WordPress.currentBlog.getLocalTableBlogId());
                    model.clear();
                    allComments.clear();
                    getListView().invalidateViews();
                    WordPress.currentComment = null;
                    loadComments(false, false);
                }

                mOnAnimateRefreshButton.onAnimateRefreshButton(false);
                if (!moderateErrorMsg.equals("") && !getActivity().isFinishing()) {
                    ToastUtils.showToast(getActivity(), R.string.error_refresh_comments,
                            ToastUtils.Duration.LONG);
                    moderateErrorMsg = "";
                }
                return;
            }

            if (commentsResult.size() == 0) {
                // no comments found
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } else {
                allComments.putAll(commentsResult);
                loadComments(refreshOnly, loadMore);
            }

            mOnAnimateRefreshButton.onAnimateRefreshButton(false);

            if (loadMore) {
                switcher.showPrevious();
            }

            showOrHideModerationBar();
        }

        @Override
        protected Map<Integer, Map<?, ?>> doInBackground(Void... args) {
            if (!hasActivity())
                return null;
            try {
                Map<Integer, Map<?, ?>> commentsResult = ApiHelper.refreshComments(getActivity()
                        .getApplicationContext(), commentParams);
                return commentsResult;
            } catch (XMLRPCException e) {
                moderateErrorMsg = e.getLocalizedMessage();
                return null;
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