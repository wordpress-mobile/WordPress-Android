package org.wordpress.android.ui.comments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

public class EditCommentActivity extends SherlockActivity {
    protected static final String ARG_LOCAL_BLOG_ID = "blog_id";
    protected static final String ARG_COMMENT_ID = "comment_id";

    private static final int ID_DIALOG_SAVING = 0;

    private int mLocalBlogId;
    private int mCommentId;
    private Comment mComment;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.edit_comment);
        setTitle(getString(R.string.edit_comment));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (!loadComment(getIntent())) {
            ToastUtils.showToast(this, R.string.error_load_comment);
            finish();
        }
    }

    private boolean loadComment(Intent intent) {
        if (intent == null)
            return false;

        mLocalBlogId = intent.getIntExtra(ARG_LOCAL_BLOG_ID, 0);
        mCommentId = intent.getIntExtra(ARG_COMMENT_ID, 0);
        mComment = CommentTable.getComment(mLocalBlogId, mCommentId);
        if (mComment == null)
            return false;

        final EditText authorNameET = (EditText) this.findViewById(R.id.author_name);
        authorNameET.setText(mComment.getAuthorName());

        final EditText authorEmailET = (EditText) this.findViewById(R.id.author_email);
        authorEmailET.setText(mComment.getAuthorEmail());

        final EditText authorURLET = (EditText) this.findViewById(R.id.author_url);
        authorURLET.setText(mComment.getAuthorUrl());

        final EditText commentContentET = (EditText) this.findViewById(R.id.comment_content);
        commentContentET.setText(mComment.getCommentText());

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.edit_comment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_save_comment:
                saveComment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_SAVING) {
            ProgressDialog savingDialog = new ProgressDialog(this);
            savingDialog.setMessage(getResources().getText(R.string.saving_changes));
            savingDialog.setIndeterminate(true);
            savingDialog.setCancelable(true);
            return savingDialog;
        }
        return super.onCreateDialog(id);
    }

    private String getEditTextStr(int resId) {
        final EditText edit = (EditText) findViewById(resId);
        return EditTextUtils.getText(edit);
    }

    private void saveComment() {
        // make sure comment content was entered
        final EditText editContent = (EditText) findViewById(R.id.comment_content);
        if (EditTextUtils.isEmpty(editContent)) {
            editContent.setError(getString(R.string.content_required));
            return;
        }

        if (mIsUpdateTaskRunning)
            AppLog.w(AppLog.T.COMMENTS, "update task already running");
        new UpdateCommentTask().execute();
    }

    /*
     * returns true if user made any changes to the comment
     */
    private boolean isCommentEdited() {
        if (mComment == null)
            return false;

        final String authorName = getEditTextStr(R.id.author_name);
        final String authorEmail = getEditTextStr(R.id.author_email);
        final String authorURL = getEditTextStr(R.id.author_url);
        final String content = getEditTextStr(R.id.comment_content);

        return !(authorName.equals(mComment.getAuthorName())
                && authorEmail.equals(mComment.getAuthorEmail())
                && authorURL.equals(mComment.getAuthorUrl())
                && content.equals(mComment.getCommentText()));
    }

    private void showSaveDialog() {
        showDialog(ID_DIALOG_SAVING);
    }

    private void dismissSaveDialog() {
        try {
            dismissDialog(ID_DIALOG_SAVING);
        } catch (IllegalArgumentException e) {
            // dialog doesn't exist
        }
    }

    /*
     * AsyncTask to save comment to server
     */
    private boolean mIsUpdateTaskRunning = false;
    private class UpdateCommentTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            mIsUpdateTaskRunning = true;
            showSaveDialog();
        }
        @Override
        protected void onCancelled() {
            mIsUpdateTaskRunning = false;
            dismissSaveDialog();
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            // return true immediately if comment hasn't changed
            if (!isCommentEdited())
                return true;

            final Blog blog;
            try {
                blog = new Blog(mLocalBlogId);
            } catch (Exception e) {
                AppLog.e(AppLog.T.COMMENTS, "localTableBlogId: " + mLocalBlogId + " not found");
                return false;
            }

            final String authorName = getEditTextStr(R.id.author_name);
            final String authorEmail = getEditTextStr(R.id.author_email);
            final String authorURL = getEditTextStr(R.id.author_url);
            final String content = getEditTextStr(R.id.comment_content);

            final Map<String, String> postHash = new HashMap<String, String>();
            postHash.put("status",       mComment.getStatus());
            postHash.put("content",      content);
            postHash.put("author",       authorName);
            postHash.put("author_url",   authorURL);
            postHash.put("author_email", authorEmail);

            XMLRPCClient client = new XMLRPCClient(
                    blog.getUrl(),
                    blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] xmlParams = {
                    blog.getRemoteBlogId(),
                    blog.getUsername(),
                    blog.getPassword(),
                    mCommentId,
                    postHash};

            Object result;
            boolean isSaved;
            try {
                result = client.call("wp.editComment", xmlParams);
                isSaved = (result != null && Boolean.parseBoolean(result.toString()));
                if (isSaved) {
                    mComment.setAuthorEmail(authorEmail);
                    mComment.setAuthorUrl(authorURL);
                    mComment.setCommentText(content);
                    mComment.setAuthorName(authorName);
                    CommentTable.updateComment(mLocalBlogId, mComment);
                }
                return isSaved;
            } catch (XMLRPCException e) {
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mIsUpdateTaskRunning = false;
            dismissSaveDialog();

            if (result) {
                setResult(RESULT_OK);
                finish();
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditCommentActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.error));
                dialogBuilder.setMessage(R.string.error_edit_comment);
                dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // just close the dialog
                    }
                });
                dialogBuilder.setCancelable(true);
                dialogBuilder.create().show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isCommentEdited()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    EditCommentActivity.this);
            dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
            dialogBuilder.setMessage(getResources().getText(R.string.sure_to_cancel_edit_comment));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            dialogBuilder.setNegativeButton(
                    getResources().getText(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            super.onBackPressed();
        }
    }

}
