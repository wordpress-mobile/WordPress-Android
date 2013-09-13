package org.wordpress.android.ui.comments;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAlertDialogFragment;

public class CommentsListFragment extends ListFragment {
    public ArrayList<Comment> model = null;
    private XMLRPCClient client;
    private String accountName = "", moderateErrorMsg = "";
    public int[] changedStatuses;
    public Map<Integer, Map<?, ?>> allComments = new HashMap<Integer, Map<?, ?>>();
    public int ID_DIALOG_MODERATING = 1;
    public int ID_DIALOG_REPLYING = 2;
    public int ID_DIALOG_DELETING = 3;
    public boolean initializing = true, shouldSelectAfterLoad = false;
    public int selectedID = 0, rowID = 0, numRecords = 0, totalComments = 0,
            commentsToLoad = 30, checkedCommentTotal = 0, selectedPosition,
            scrollPosition = 0, scrollPositionTop = 0;
    public ProgressDialog pd;
    private ViewSwitcher switcher;
    boolean loadMore = false, doInBackground = false, refreshOnly = false;
    private List<String> checkedComments;
    Object[] commentParams;
    boolean dualView;
    private OnCommentSelectedListener onCommentSelectedListener;
    private OnAnimateRefreshButtonListener onAnimateRefreshButton;
    private OnContextCommentStatusChangeListener onCommentStatusChangeListener;
    public getRecentCommentsTask getCommentsTask;
    private View mFooterSpacer;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onCommentSelectedListener = (OnCommentSelectedListener) activity;
            onAnimateRefreshButton = (OnAnimateRefreshButtonListener) activity;
            onCommentStatusChangeListener = (OnContextCommentStatusChangeListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideModerationBar(); //The app doesn't remember the selection state on resume, so don't show the bar.
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

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
                refreshComments(true, false, false);
            }
        });

        mFooterSpacer = new View(getActivity());
        mFooterSpacer.setLayoutParams(new AbsListView.LayoutParams(10, 0));
        
        View progress = View.inflate(getActivity().getApplicationContext(),
                R.layout.list_footer_progress, null);

        switcher.addView(footer);
        switcher.addView(progress);

        getActivity().setTitle(accountName + " - Moderate Comments");

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
                        moderateComments("approve");
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
                        moderateComments("hold");
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
                        moderateComments("spam");
                    }
                }.start();
            }
        });
        return v;
    }

    @SuppressWarnings("unchecked")
    protected void moderateComments(String newStatus) {
        // handles bulk moderation
        for (int i = 0; i < checkedComments.size(); i++) {
            if (checkedComments.get(i).toString().equals("true")) {

                client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                        WordPress.currentBlog.getHttpuser(),
                        WordPress.currentBlog.getHttppassword());

                Comment listRow = (Comment) getListView().getItemAtPosition(i);
                int curCommentID = listRow.commentID;

                Map<String, String> contentHash, postHash = new HashMap<String, String>();
                contentHash = (Map<String, String>) allComments.get(curCommentID);

                if(contentHash.get("status").equals(newStatus)){
                    checkedComments.set(i, "false");                    
                    continue;
                }
                
                postHash.put("status", newStatus);
                postHash.put("content", contentHash.get("comment"));
                postHash.put("author", contentHash.get("author"));
                postHash.put("author_url", contentHash.get("url"));
                postHash.put("author_email", contentHash.get("email"));

                Object[] params = { WordPress.currentBlog.getBlogId(),
                        WordPress.currentBlog.getUsername(),
                        WordPress.currentBlog.getPassword(), curCommentID,
                        postHash };

                Object result = null;
                try {
                    result = (Object) client.call("wp.editComment", params);
                    boolean bResult = Boolean.parseBoolean(result.toString());
                    if (bResult) {
                        checkedComments.set(i, "false");
                        listRow.status = newStatus;
                        contentHash.put("status", newStatus);
                        model.set(i, listRow);
                        WordPress.wpDB.updateCommentStatus(
                                WordPress.currentBlog.getId(),
                                listRow.commentID, newStatus);
                    }
                } catch (XMLRPCException e) {
                    moderateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
                }
            }
        }
        getActivity().dismissDialog(ID_DIALOG_MODERATING);
        Thread action = new Thread() {
            public void run() {
                if (moderateErrorMsg == "") {
                    String msg = getResources().getText(
                            R.string.comment_moderated).toString();
                    if (checkedCommentTotal > 1)
                        msg = getResources().getText(
                                R.string.comments_moderated).toString();
                    Toast.makeText(getActivity().getApplicationContext(), msg,
                            Toast.LENGTH_SHORT).show();
                    checkedCommentTotal = 0;
                    hideModerationBar();
                    getListView().invalidateViews();
                    
                    // update the comment counter on the menu drawer 
                    ((WPActionBarActivity) getActivity()).updateMenuDrawer();
                } else {
                    // there was an xmlrpc error
                    if (!getActivity().isFinishing()) {
                        checkedCommentTotal = 0;
                        hideModerationBar();
                        getListView().invalidateViews();
                        FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newInstance(moderateErrorMsg);
                        alert.show(ft, "alert");
                    }
                    moderateErrorMsg = "";

                }
            }
        };
        getActivity().runOnUiThread(action);
        pd = new ProgressDialog(getActivity().getApplicationContext());
    }

    protected void deleteComments() {
        // bulk delete comments

        for (int i = 0; i < checkedComments.size(); i++) {
            if (checkedComments.get(i).toString().equals("true")) {

                client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                        WordPress.currentBlog.getHttpuser(),
                        WordPress.currentBlog.getHttppassword());

                Comment listRow = (Comment) getListView().getItemAtPosition(i);
                int curCommentID = listRow.commentID;

                Object[] params = { WordPress.currentBlog.getBlogId(),
                        WordPress.currentBlog.getUsername(),
                        WordPress.currentBlog.getPassword(), curCommentID };

                try {
                    client.call("wp.deleteComment", params);
                } catch (final XMLRPCException e) {
                    moderateErrorMsg = getResources().getText(R.string.error_moderate_comment).toString();
                }
            }
        }
        getActivity().dismissDialog(ID_DIALOG_DELETING);
        Thread action = new Thread() {
            public void run() {
                if (moderateErrorMsg == "") {
                    String msg = getResources().getText(
                            R.string.comment_moderated).toString();
                    if (checkedCommentTotal > 1)
                        msg = getResources().getText(
                                R.string.comments_moderated).toString();
                    Toast.makeText(getActivity().getApplicationContext(), msg,
                            Toast.LENGTH_SHORT).show();
                    checkedCommentTotal = 0;
                    hideModerationBar();
                    refreshComments(false, false, false);
                } else {
                    // error occurred during delete request
                    if (!getActivity().isFinishing()) {
                        FragmentTransaction ft = getFragmentManager()
                                .beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment
                                .newInstance(moderateErrorMsg);
                        try {
                            alert.show(ft, "alert");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        getActivity().runOnUiThread(action);
        pd = new ProgressDialog(getActivity().getApplicationContext());

    }

    public boolean loadComments(boolean refresh, boolean loadMore) {
        refreshOnly = refresh;
        String author, postID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        List<Map<String, Object>> loadedComments = WordPress.wpDB
                .loadComments(WordPress.currentBlog.getId());
        if (loadedComments != null) {
            numRecords = loadedComments.size();
            if (refreshOnly) {
                if (model != null) {
                    model.clear();
                }
            } else {
                model = new ArrayList<Comment>();
            }

            checkedComments = new Vector<String>();
            for (int i = 0; i < loadedComments.size(); i++) {
                checkedComments.add(i, "false");
                Map<String, Object> contentHash = loadedComments.get(i);
                allComments.put((Integer)contentHash.get("commentID"),
                        contentHash);
                author = StringUtils.unescapeHTML(contentHash.get("author")
                        .toString());
                commentID = (Integer)contentHash.get("commentID");
                postID = contentHash.get("postID").toString();
                comment = StringUtils.unescapeHTML(contentHash.get("comment")
                        .toString());
                dateCreatedFormatted = contentHash.get("commentDateFormatted")
                        .toString();
                status = contentHash.get("status").toString();
                authorEmail = StringUtils.unescapeHTML(contentHash.get("email")
                        .toString());
                authorURL = StringUtils.unescapeHTML(contentHash.get("url")
                        .toString());
                postTitle = StringUtils.unescapeHTML(contentHash.get(
                        "postTitle").toString());

                if (model == null) {
                    model = new ArrayList<Comment>();
                }

                // add to model
                model.add(new Comment(postID, commentID, i, author,
                        dateCreatedFormatted, comment, status, postTitle,
                        authorURL, authorEmail, URI
                                .create("http://gravatar.com/avatar/"
                                        + StringUtils.getMd5Hash(authorEmail.trim())
                                        + "?s=140&d=404")));
            }

            if (!refreshOnly) {
                ListView listView = this.getListView();
                listView.removeFooterView(switcher);
                listView.removeFooterView(mFooterSpacer);
                if (loadedComments.size() % 30 == 0) {
                    listView.addFooterView(switcher);
                }
                listView.addFooterView(mFooterSpacer);
                setListAdapter(new CommentAdapter());
                

                listView.setOnItemClickListener(new OnItemClickListener() {

                    public void onItemClick(AdapterView<?> arg0, View view,
                            int position, long id) {
                        selectedPosition = position;
                        Comment comment = model.get((int) id);
                        onCommentSelectedListener.onCommentSelected(comment);
                        getListView().invalidateViews();
                    }
                });

//                listView.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//                    @Override
//                    public boolean onItemLongClick(AdapterView<?> arg0,
//                            View view, int position, long id) {
//
//                        selectedPosition = position;
//                        Comment comment = model.get((int) id);
//                        onCommentSelectedListener.onCommentSelected(comment);
//
//                        Intent i = new Intent(
//                                getActivity().getApplicationContext(),
//                                EditComment.class);
//                        startActivityForResult(i, 0);
//
//                        return false;
//                    }
//
//                });

                listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        AdapterView.AdapterContextMenuInfo info;
                        try {
                            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                        } catch (ClassCastException e) {
                            // Log.e(TAG, "bad menuInfo", e);
                            return;
                        }

                        WordPress.currentComment = model.get(info.position);

                        menu.setHeaderTitle(getResources().getText(
                                R.string.comment_actions));
                        menu.add(0, 0, 0,
                                getResources().getText(R.string.mark_approved));
                        menu.add(0, 1, 0,
                                getResources()
                                        .getText(R.string.mark_unapproved));
                        menu.add(0, 2, 0,
                                getResources().getText(R.string.mark_spam));
                        menu.add(0, 3, 0, getResources()
                                .getText(R.string.reply));
                        menu.add(0, 4, 0,
                                getResources().getText(R.string.delete));
                        menu.add(0, 5, 0,
                                getResources().getText(R.string.edit));
                    }
                });
            } else {
                getListView().invalidateViews();
            }

            if (this.shouldSelectAfterLoad) {
                if (model != null) {
                    if (model.size() > 0) {
                        selectedPosition = 0;
                        Comment aComment = model.get((int) 0);
                        onCommentSelectedListener.onCommentSelected(aComment);
                        getListView().setItemChecked(0, true);
                    }
                }
                shouldSelectAfterLoad = false;
            }

            if (loadMore && scrollPosition > 0) {
                ListView listView = this.getListView();
                try {
                    listView.setSelectionFromTop(scrollPosition,
                            scrollPositionTop);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void refreshComments(final boolean more, final boolean refresh,
            final boolean background) {
        loadMore = more;
        refreshOnly = refresh;
        doInBackground = background;

        if (!loadMore && !doInBackground) {
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
            hPost.put("number", numRecords + 30);
        } else {
            hPost.put("number", 30);
        }

        Object[] params = { WordPress.currentBlog.getBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), hPost };

        commentParams = params;
        getCommentsTask = new getRecentCommentsTask();
        getCommentsTask.execute();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    class CommentAdapter extends ArrayAdapter<Comment> {

        int sdk_version = 7;
        boolean detailViewVisible = false;

        CommentAdapter() {
            super(getActivity().getApplicationContext(), R.layout.comment_row, model);

            sdk_version = android.os.Build.VERSION.SDK_INT;
            FragmentManager fm = getActivity().getSupportFragmentManager();
            CommentFragment f = (CommentFragment) fm
                    .findFragmentById(R.id.commentDetail);
            if (f != null && f.isInLayout())
                detailViewVisible = true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            CommentEntryWrapper wrapper = null;

            if (row == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                row = inflater.inflate(R.layout.comment_row, null);
                NetworkImageView avatar = (NetworkImageView)row.findViewById(R.id.avatar);
                avatar.setDefaultImageResId(R.drawable.placeholder);
                wrapper = new CommentEntryWrapper(row);
                row.setTag(wrapper);
            } else {
                wrapper = (CommentEntryWrapper) row.getTag();
            }
            Comment commentEntry = getItem(position);
            wrapper.populateFrom(commentEntry, position);

            return (row);
        }
    }

    class CommentEntryWrapper {
        private TextView name = null;
        private TextView emailURL = null;
        private TextView comment = null;
        private TextView status = null;
        private TextView postTitle = null;
        private NetworkImageView avatar = null;
        private View row = null;
        private CheckBox bulkCheck = null;
        private RelativeLayout bulkEditGroup = null;

        CommentEntryWrapper(View row) {
            this.row = row;
        }

        void populateFrom(Comment s, final int position) {

            String authorName = s.name;

            if(authorName.length() > 0)
                getName().setText(authorName);
            else
                getName().setText(getResources().getText(R.string.anonymous));

            String fEmailURL = s.authorURL;
            // use the email address if the commenter didn't add a url
            if (fEmailURL == "")
                fEmailURL = s.emailURL;

            getEmailURL().setText(fEmailURL);
            getComment().setText(s.comment);
            getPostTitle().setText(
                    getResources().getText(R.string.on) + " " + s.postTitle);

            row.setId(Integer.valueOf(s.commentID));

            String prettyComment, textColor = "";

            if (s.status.equals("spam")) {
                prettyComment = getResources().getText(R.string.spam)
                        .toString();
                textColor = "#FF0000";
            } else if (s.status.equals("hold")) {
                prettyComment = getResources().getText(R.string.unapproved)
                        .toString();
                textColor = "#D54E21";
            } else {
                prettyComment = getResources().getText(R.string.approved)
                        .toString();
                textColor = "#006505";
            }

            getBulkEditGroup().setVisibility(View.VISIBLE);

            getStatus().setText(prettyComment);
            getStatus().setTextColor(Color.parseColor(textColor));

            getBulkCheck().setChecked(
                    Boolean.parseBoolean(checkedComments.get(position)
                            .toString()));
            getBulkCheck().setTag(position);
            getBulkCheck().setOnClickListener(new OnClickListener() {

                public void onClick(View arg0) {
                    checkedComments.set(position,
                            String.valueOf(getBulkCheck().isChecked()));
                    showOrHideModerationBar();
                }
            });

            getAvatar().setImageUrl(s.profileImageUrl.toString(), WordPress.imageLoader);
        }

        TextView getName() {
            if (name == null) {
                name = (TextView) row.findViewById(R.id.name);
            }

            return (name);
        }

        TextView getEmailURL() {
            if (emailURL == null) {
                emailURL = (TextView) row.findViewById(R.id.email_url);
            }

            return (emailURL);
        }

        TextView getComment() {
            if (comment == null) {
                comment = (TextView) row.findViewById(R.id.comment);
            }

            return (comment);
        }

        TextView getStatus() {
            if (status == null) {
                status = (TextView) row.findViewById(R.id.status);
            }

            status.setTextSize(12);

            return (status);
        }

        TextView getPostTitle() {
            if (postTitle == null) {
                postTitle = (TextView) row.findViewById(R.id.postTitle);
            }

            return (postTitle);
        }

        NetworkImageView getAvatar() {
            if (avatar == null) {
                avatar = (NetworkImageView) row.findViewById(R.id.avatar);
            }

            return (avatar);
        }

        CheckBox getBulkCheck() {
            if (bulkCheck == null) {
                bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);
            }

            return (bulkCheck);
        }

        RelativeLayout getBulkEditGroup() {
            if (bulkEditGroup == null) {
                bulkEditGroup = (RelativeLayout) row.findViewById(R.id.bulkEditGroup);
            }

            return (bulkEditGroup);
        }

    }

    protected void hideModerationBar() {
        RelativeLayout moderationBar = (RelativeLayout) getActivity().findViewById(R.id.moderationBar);
        if( moderationBar.getVisibility() == View.INVISIBLE )
            return;
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);
        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);
        moderationBar.clearAnimation();
        moderationBar.startAnimation(set);
        moderationBar.setVisibility(View.INVISIBLE);
        setFooterSpacerVisible(false);
    }

    protected void showOrHideModerationBar() {
        if( checkedComments == null )
            return;
        
        boolean shouldShowModerationBar = false;
        for (int i = 0; i < checkedComments.size(); i++) {
            if (checkedComments.get(i).equals("true")) {
                shouldShowModerationBar = true;
                break;
            }
        }

        if (shouldShowModerationBar)
            showModerationBar();
        else
            hideModerationBar();
    }
    
    protected void showModerationBar() {
        RelativeLayout moderationBar = (RelativeLayout) getActivity().findViewById(R.id.moderationBar);
        if( moderationBar.getVisibility() == View.VISIBLE )
            return;
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);
        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(500);
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

    interface XMLRPCMethodCallback {
        void callFinished(Object[] result);
    }

    class XMLRPCMethod extends Thread {
        private String method;
        private Object[] params;
        private Handler handler;
        private XMLRPCMethodCallback callBack;

        public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
            this.method = method;
            this.callBack = callBack;
            handler = new Handler();
        }

        public void call() {
            call(null);
        }

        public void call(Object[] params) {
            this.params = params;
            start();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                // get the total comments
                Map<Object, Object> countResult = new HashMap<Object, Object>();
                Object[] countParams = { WordPress.currentBlog.getBlogId(),
                        WordPress.currentBlog.getUsername(),
                        WordPress.currentBlog.getPassword(), 0 };
                try {
                    countResult = (Map<Object, Object>) client.call(
                            "wp.getCommentCount", countParams);
                    totalComments = Integer.valueOf(countResult.get(
                            "awaiting_moderation").toString())
                            + Integer.valueOf(countResult.get("approved")
                                    .toString());
                } catch (XMLRPCException e) {
                    e.printStackTrace();
                }
                final Object[] result = (Object[]) client.call(method, params);
                handler.post(new Runnable() {
                    public void run() {

                        callBack.callFinished(result);
                    }
                });
            } catch (final XMLRPCFault e) {
                handler.post(new Runnable() {
                    public void run() {
                        if (pd.isShowing()) {
                            pd.dismiss();
                        }
                        if (!getActivity().isFinishing()) {
                            onAnimateRefreshButton.onAnimateRefreshButton(false);
                            FragmentTransaction ft = getFragmentManager()
                                    .beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment
                                    .newInstance(e.getLocalizedMessage());
                            alert.show(ft, "alert");
                        }
                    }
                });
            } catch (final XMLRPCException e) {
                handler.post(new Runnable() {
                    public void run() {
                        if (pd.isShowing()) {
                            pd.dismiss();
                        }
                        if (!getActivity().isFinishing()) {
                            onAnimateRefreshButton.onAnimateRefreshButton(false);
                            FragmentTransaction ft = getFragmentManager()
                                    .beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment
                                    .newInstance(e.getLocalizedMessage());
                            alert.show(ft, "alert");
                        }
                    }
                });
            }
        }
    }

    interface XMLRPCMethodCallbackEditComment {
        void callFinished(Object result);
    }

    class XMLRPCMethodEditComment extends Thread {
        private String method;
        private Object[] params;
        private Handler handler;
        private XMLRPCMethodCallbackEditComment callBack;

        public XMLRPCMethodEditComment(String method,
                XMLRPCMethodCallbackEditComment callBack) {
            this.method = method;
            this.callBack = callBack;
            handler = new Handler();
        }

        public void call() {
            call(null);
        }

        public void call(Object[] params) {
            this.params = params;
            start();
        }

        @Override
        public void run() {
            try {
                final Object result = (Object) client.call(method, params);
                handler.post(new Runnable() {
                    public void run() {
                        callBack.callFinished(result);
                    }
                });
            } catch (final XMLRPCFault e) {
                handler.post(new Runnable() {
                    public void run() {
                        if (!getActivity().isFinishing()) {
                            getActivity().dismissDialog(ID_DIALOG_MODERATING);
                            FragmentTransaction ft = getFragmentManager()
                                    .beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment
                                    .newInstance(e.getFaultString());
                            alert.show(ft, "alert");
                        }
                    }
                });
            } catch (final XMLRPCException e) {
                handler.post(new Runnable() {
                    public void run() {
                        if (!getActivity().isFinishing()) {
                            getActivity().dismissDialog(ID_DIALOG_MODERATING);
                            FragmentTransaction ft = getFragmentManager()
                                    .beginTransaction();
                            WPAlertDialogFragment alert = WPAlertDialogFragment
                                    .newInstance(e.getLocalizedMessage());
                            alert.show(ft, "alert");
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        /* Switch on the ID of the item, to get what the user selected. */
        switch (item.getItemId()) {
        case 0:
            onCommentStatusChangeListener.onCommentStatusChanged("approve");
            return true;
        case 1:
            onCommentStatusChangeListener.onCommentStatusChanged("hold");
            return true;
        case 2:
            onCommentStatusChangeListener.onCommentStatusChanged("spam");
            return true;
        case 3:
            onCommentStatusChangeListener.onCommentStatusChanged("reply");
            return true;
        case 4:
            onCommentStatusChangeListener.onCommentStatusChanged("delete");
            return true;
        case 5:
//            selectedPosition = position;
//            Comment comment = model.get((int) id);
//            onCommentSelectedListener.onCommentSelected(comment);
            Intent i = new Intent(
                    getActivity().getApplicationContext(),
                    EditCommentActivity.class);
            startActivityForResult(i, 0);
            return true;

        }
        return false;
    }

    class getRecentCommentsTask extends
            AsyncTask<Void, Void, Map<Integer, Map<?, ?>>> {

        protected void onPostExecute(
                Map<Integer, Map<?, ?>> commentsResult) {

            if (isCancelled())
                return;

            if (commentsResult == null) {

                if (model != null && model.size() == 1) {
                    WordPress.wpDB.clearComments(WordPress.currentBlog
                            .getId());
                    model.clear();
                    allComments.clear();
                    getListView().invalidateViews();
                    onCommentStatusChangeListener
                            .onCommentStatusChanged("clear");
                    WordPress.currentComment = null;
                    loadComments(false, false);
                }

                onAnimateRefreshButton.onAnimateRefreshButton(false);
                if (!moderateErrorMsg.equals("") && !getActivity().isFinishing()) {
                    FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                    WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newInstance(String.format(getResources().getString(R.string.error_refresh), getResources().getText(R.string.tab_comments)), moderateErrorMsg);
                    alert.show(ft, "alert");
                    moderateErrorMsg = "";
                }
                return;
            }

            if (commentsResult.size() == 0) {
                // no comments found
                if (pd.isShowing()) {
                    pd.dismiss();
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
             
            if (!doInBackground) {
                showOrHideModerationBar();
            } 
            
        }

        @Override
        protected Map<Integer, Map<?, ?>> doInBackground(Void... args) {

            Map<Integer, Map<?, ?>> commentsResult;
            try {
                commentsResult = ApiHelper.refreshComments(getActivity()
                        .getApplicationContext(), commentParams);
            } catch (XMLRPCException e) {
                if (!getActivity().isFinishing())
                    moderateErrorMsg = e.getLocalizedMessage();
                return null;
            }

            return commentsResult;

        }

    }

    public interface OnCommentSelectedListener {
        public void onCommentSelected(Comment comment);
    }

    public interface OnAnimateRefreshButtonListener {
        public void onAnimateRefreshButton(boolean start);
    }

    public interface OnContextCommentStatusChangeListener {
        public void onCommentStatusChanged(String status);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
