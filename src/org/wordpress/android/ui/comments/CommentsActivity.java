package org.wordpress.android.ui.comments;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentFragment.OnCommentStatusChangeListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnAnimateRefreshButtonListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.ui.comments.CommentsListFragment.OnContextCommentStatusChangeListener;

public class CommentsActivity extends WPActionBarActivity implements
        OnCommentSelectedListener, OnCommentStatusChangeListener,
        OnAnimateRefreshButtonListener, OnContextCommentStatusChangeListener {

    protected int id;
    public int ID_DIALOG_MODERATING = 1;
    public int ID_DIALOG_REPLYING = 2;
    public int ID_DIALOG_DELETING = 3;
    private XMLRPCClient client;
    public ProgressDialog pd;
    private CommentsListFragment commentList;
    private boolean fromNotification = false;
    private MenuItem refreshMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.comments);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.tab_comments));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            fromNotification = extras.getBoolean("fromNotification");
            if (fromNotification) {
                try {
                    WordPress.currentBlog = new Blog(extras.getInt("id"));
                } catch (Exception e) {
                    Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        commentList = (CommentsListFragment) fm.findFragmentById(R.id.commentList);

        WordPress.currentComment = null;

        attemptToSelectComment();
        if (fromNotification)
            commentList.refreshComments(false, false, false);

        if (savedInstanceState != null)
            popCommentDetail();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        commentList.refreshComments(false, false, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.basic_menu, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            popCommentDetail();
            attemptToSelectComment();
            commentList.refreshComments(false, false, false);
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popCommentDetail();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    protected void popCommentDetail() {
        FragmentManager fm = getSupportFragmentManager();
        CommentFragment f = (CommentFragment) fm
                .findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (WordPress.currentBlog != null) {
            boolean commentsLoaded = commentList.loadComments(false, false);
            if (!commentsLoaded)
                commentList.refreshComments(false, false, false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentList.getCommentsTask != null)
            commentList.getCommentsTask.cancel(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean fromNotification = false;
            fromNotification = extras.getBoolean("fromNotification");
            if (fromNotification) {
                try {
                    WordPress.currentBlog = new Blog(extras.getInt("id"));
                } catch (Exception e) {
                    Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
                //titleBar.refreshBlog();
            }
        }

    }

    @Override
    public void onCommentSelected(Comment comment) {

        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();
        CommentFragment f = (CommentFragment) fm.findFragmentById(R.id.commentDetail);
        
        if (comment != null && fm.getBackStackEntryCount() == 0) {

            if (f == null || !f.isInLayout()) {
                WordPress.currentComment = comment;
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(commentList);
                f = new CommentFragment();
                ft.add(R.id.commentDetailFragmentContainer, f);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                f.loadComment(comment);
            }
        }
    }

    @Override
    public void onCommentStatusChanged(final String status) {

        if (WordPress.currentComment != null) {

            final int commentID = WordPress.currentComment.commentID;

            if (status.equals("approve") || status.equals("hold")
                    || status.equals("spam")) {
                showDialog(ID_DIALOG_MODERATING);
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        changeCommentStatus(status, commentID);
                    }
                }.start();
            } else if (status.equals("delete")) {
                Thread action3 = new Thread() {
                    public void run() {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                                CommentsActivity.this);
                        dialogBuilder.setTitle(getResources().getText(
                                R.string.confirm_delete));
                        dialogBuilder.setMessage(getResources().getText(R.string.confirm_delete_data));
                        dialogBuilder.setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        showDialog(ID_DIALOG_DELETING);
                                        // pop out of the detail view if on a smaller screen
                                        FragmentManager fm = getSupportFragmentManager();
                                        CommentFragment f = (CommentFragment) fm
                                                .findFragmentById(R.id.commentDetail);
                                        if (f == null) {
                                            fm.popBackStack();
                                        }
                                        new Thread() {
                                            public void run() {
                                                deleteComment(commentID);
                                            }
                                        }.start();


                                    }
                                });
                        dialogBuilder.setNegativeButton(getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        //Don't delete Comment
                                    }
                                });
                        dialogBuilder.setCancelable(true);
                        if (!isFinishing()) {
                            dialogBuilder.create().show();
                        }
                    }
                };
                runOnUiThread(action3);
            } else if (status.equals("reply")) {

                Intent i = new Intent(CommentsActivity.this, AddCommentActivity.class);
                i.putExtra("commentID", commentID);
                i.putExtra("postID", WordPress.currentComment.postID);
                startActivityForResult(i, 0);
            } else if (status.equals("clear")) {
                FragmentManager fm = getSupportFragmentManager();
                CommentFragment f = (CommentFragment) fm
                        .findFragmentById(R.id.commentDetail);
                if (f != null) {
                    f.clearContent();
                }
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void changeCommentStatus(final String newStatus,
            final int selCommentID) {
        // for individual comment moderation
        client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        Map<String, String> contentHash, postHash = new HashMap<String, String>();
        contentHash = (Map<String, String>) commentList.allComments.get(selCommentID);
        postHash.put("status", newStatus);
        postHash.put("content", contentHash.get("comment"));
        postHash.put("author", contentHash.get("author"));
        postHash.put("author_url", contentHash.get("url"));
        postHash.put("author_email", contentHash.get("email"));

        Object[] params = { WordPress.currentBlog.getBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), selCommentID, postHash };

        Object result = null;
        try {
            result = (Object) client.call("wp.editComment", params);
            boolean bResult = Boolean.parseBoolean(result.toString());
            if (bResult) {
                WordPress.currentComment.status = newStatus;
                commentList.model.set(WordPress.currentComment.position,
                        WordPress.currentComment);
                WordPress.wpDB.updateCommentStatus(id, WordPress.currentComment.commentID,
                        newStatus);
                contentHash.put("status", newStatus);
            }
            dismissDialog(ID_DIALOG_MODERATING);
            Thread action = new Thread() {
                public void run() {
                    Toast.makeText(CommentsActivity.this,
                            getResources().getText(R.string.comment_moderated),
                            Toast.LENGTH_SHORT).show();
                    
                    //Update the UI of the details view
                    FragmentManager fm = getSupportFragmentManager();
                    CommentFragment f = (CommentFragment) fm
                            .findFragmentById(R.id.commentDetail);

                    if (f != null) { //tablets
                        if(f.isInLayout())
                            f.processCommentStatus();
                    } else {//phone
                        f = (CommentFragment) fm.findFragmentById(R.id.commentDetailFragmentContainer);
                        if (f != null) 
                            f.processCommentStatus();
                    }
                }
            };
            runOnUiThread(action);
            Thread action2 = new Thread() {
                public void run() {
                    commentList.getListView().invalidateViews();
                }
            };
            runOnUiThread(action2);

        } catch (final XMLRPCException e) {
            dismissDialog(ID_DIALOG_MODERATING);
            Thread action3 = new Thread() {
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            CommentsActivity.this);
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.connection_error));
                    dialogBuilder.setMessage(getResources().getText(R.string.error_moderate_comment));
                    dialogBuilder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    // Just close the window.

                                }
                            });
                    dialogBuilder.setCancelable(true);
                    if (!isFinishing()) {
                        dialogBuilder.create().show();
                    }
                }
            };
            runOnUiThread(action3);
        }
    }

    private void deleteComment(int selCommentID) {
        // delete individual comment

        client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        Object[] params = { WordPress.currentBlog.getBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), selCommentID };

        try {
            client.call("wp.deleteComment", params);
            dismissDialog(ID_DIALOG_DELETING);
            attemptToSelectComment();
            Thread action = new Thread() {
                public void run() {
                    Toast.makeText(CommentsActivity.this,
                            getResources().getText(R.string.comment_moderated),
                            Toast.LENGTH_SHORT).show();
                }
            };
            runOnUiThread(action);
            Thread action2 = new Thread() {
                public void run() {
                    pd = new ProgressDialog(CommentsActivity.this);
                    commentList.refreshComments(false, true, false);
                }
            };
            runOnUiThread(action2);

        } catch (final XMLRPCException e) {
            dismissDialog(ID_DIALOG_DELETING);
            Thread action3 = new Thread() {
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            CommentsActivity.this);
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.connection_error));
                    dialogBuilder.setMessage(getResources().getText(R.string.error_moderate_comment));
                    dialogBuilder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    // Just close the window.

                                }
                            });
                    dialogBuilder.setCancelable(true);
                    if (!isFinishing()) {
                        dialogBuilder.create().show();
                    }
                }
            };
            runOnUiThread(action3);
        }
    }

    private void replyToComment(final String postID, final int commentID,
            final String comment) {
        // reply to individual comment
        client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        Map<String, Object> replyHash = new HashMap<String, Object>();
        replyHash.put("comment_parent", commentID);
        replyHash.put("content", comment);
        replyHash.put("author", "");
        replyHash.put("author_url", "");
        replyHash.put("author_email", "");

        Object[] params = { WordPress.currentBlog.getBlogId(),
                WordPress.currentBlog.getUsername(),
                WordPress.currentBlog.getPassword(), Integer.valueOf(postID),
                replyHash };

        try {
            int newCommentID = (Integer) client.call("wp.newComment", params);
            if (newCommentID >= 0)
            {
                WordPress.wpDB.updateLatestCommentID(WordPress.currentBlog.getId(), newCommentID);
            }
            dismissDialog(ID_DIALOG_REPLYING);
            Thread action = new Thread() {
                public void run() {
                    Toast.makeText(CommentsActivity.this,
                            getResources().getText(R.string.reply_added),
                            Toast.LENGTH_SHORT).show();
                }
            };
            runOnUiThread(action);
            Thread action2 = new Thread() {
                public void run() {
                    pd = new ProgressDialog(CommentsActivity.this); // to avoid
                    // crash
                    commentList.refreshComments(false, true, false);
                }
            };
            runOnUiThread(action2);

        } catch (final XMLRPCException e) {
            dismissDialog(ID_DIALOG_REPLYING);
            Thread action3 = new Thread() {
                public void run() {

                    Toast.makeText(CommentsActivity.this, getResources().getText(R.string.connection_error), Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(CommentsActivity.this, AddCommentActivity.class);
                    i.putExtra("commentID", commentID);
                    i.putExtra("postID", WordPress.currentComment.postID);
                    i.putExtra("comment", comment);
                    startActivityForResult(i, 0);
                }
            };
            runOnUiThread(action3);

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {

            Bundle extras = data.getExtras();

            switch (requestCode) {
            case 0:
                final String returnText = extras.getString("commentText");

                if (!returnText.equals("CANCEL")) {
                    final String postID = extras.getString("postID");
                    final int commentID = extras.getInt("commentID");
                    showDialog(ID_DIALOG_REPLYING);

                    new Thread(new Runnable() {
                        public void run() {
                            Looper.prepare();
                            pd = new ProgressDialog(CommentsActivity.this);
                            replyToComment(postID, commentID, returnText);
                        }
                    }).start();
                }

                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_MODERATING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.checkedCommentTotal <= 1) {
                loadingDialog.setMessage(getResources().getText(
                        R.string.moderating_comment));
            } else {
                loadingDialog.setMessage(getResources().getText(
                        R.string.moderating_comments));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_REPLYING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            loadingDialog.setMessage(getResources().getText(
                    R.string.replying_comment));
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_DELETING) {
            ProgressDialog loadingDialog = new ProgressDialog(CommentsActivity.this);
            if (commentList.checkedCommentTotal <= 1) {
                loadingDialog.setMessage(getResources().getText(
                        R.string.deleting_comment));
            } else {
                loadingDialog.setMessage(getResources().getText(
                        R.string.deleting_comments));
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }

        return super.onCreateDialog(id);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_BACK && titleBar.isShowingDashboard) {
            titleBar.hideDashboardOverlay();
            return false;
        }*/

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAnimateRefreshButton(boolean start) {

        if (start) {
            shouldAnimateRefreshButton = true;
            this.startAnimatingRefreshButton(refreshMenuItem);
        } else {
            this.stopAnimatingRefreshButton(refreshMenuItem);
        }

    }

    private void attemptToSelectComment() {

        FragmentManager fm = getSupportFragmentManager();
        CommentFragment f = (CommentFragment) fm
                .findFragmentById(R.id.commentDetail);

        if (f != null && f.isInLayout()) {
            commentList.shouldSelectAfterLoad = true;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
