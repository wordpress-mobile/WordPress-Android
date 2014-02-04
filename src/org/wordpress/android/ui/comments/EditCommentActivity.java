package org.wordpress.android.ui.comments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

import org.wordpress.android.R;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

public class EditCommentActivity extends SherlockActivity {

    private static final int ID_DIALOG_SAVING = 0;

    protected static final String ARG_LOCAL_BLOG_ID = "blog_id";
    protected static final String ARG_COMMENT_ID = "comment_id";

    private int mLocalBlogId;
    private int mCommentId;
    private Comment mComment;

    // spinner indexes
    private static final int IDX_APPROVED = 0;
    private static final int IDX_UNAPPROVED = 1;
    private static final int IDX_SPAM = 2;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.edit_comment);
        setTitle(getString(R.string.edit_comment));
        
        // Capitalize headers
        ((TextView) findViewById(R.id.l_section1)).setText(getResources().getString(R.string.comment_content).toUpperCase());
        ((TextView) findViewById(R.id.l_status)).setText(getResources().getString(R.string.status).toUpperCase());

        // populate spinner
        String[] items = new String[] {
                getResources().getString(R.string.approved),
                getResources().getString(R.string.unapproved),
                getResources().getString(R.string.spam) };
        Spinner spinner = (Spinner) findViewById(R.id.status);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (!loadComment(getIntent())) {
            Toast.makeText(this, getString(R.string.error_load_comment), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final Button saveButton = (Button) findViewById(R.id.post);
        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                saveComment();
            }
        });

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

        final Spinner spinner = (Spinner) findViewById(R.id.status);
        switch (mComment.getStatusEnum()) {
            case APPROVED:
                spinner.setSelection(IDX_APPROVED, true);
                break;
            case UNAPPROVED:
                spinner.setSelection(IDX_UNAPPROVED, true);
                break;
            case SPAM:
                spinner.setSelection(IDX_SPAM, true);
                break;

        }

        return true;
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
        // make sure content was entered
        final EditText editContent = (EditText) findViewById(R.id.comment_content);
        if (EditTextUtils.isEmpty(editContent)) {
            editContent.setError(getString(R.string.content_required));
            return;
        }

        new UpdateCommentTask().execute();
    }

    private String getSelectedStatus() {
        Spinner spinner = (Spinner) findViewById(R.id.status);
        int selectedStatus = spinner.getSelectedItemPosition();
        final String status;
        switch (selectedStatus) {
            case IDX_APPROVED:
                return CommentStatus.toString(CommentStatus.APPROVED);
            case IDX_UNAPPROVED:
                return CommentStatus.toString(CommentStatus.UNAPPROVED);
            case IDX_SPAM:
                return CommentStatus.toString(CommentStatus.SPAM);
            default:
                return CommentStatus.toString(CommentStatus.UNKNOWN);
        }
    }

    private boolean isCommentEdited() {
        if (mComment == null)
            return false;

        final String authorName = getEditTextStr(R.id.author_name);
        final String authorEmail = getEditTextStr(R.id.author_email);
        final String authorURL = getEditTextStr(R.id.author_url);
        final String content = getEditTextStr(R.id.comment_content);
        final String status = getSelectedStatus();

        return (authorName.equals(mComment.getAuthorName())
             && authorEmail.equals(mComment.getAuthorEmail())
             && authorURL.equals(mComment.getAuthorUrl())
             && content.equals(mComment.getCommentText())
             && status.equals(mComment.getStatus()));
    }

    private void dismissDialogSafely(int dialogId) {
        try {
            dismissDialog(dialogId);
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
            dismissDialogSafely(ID_DIALOG_SAVING);
        }
        @Override
        protected void onCancelled() {
            mIsUpdateTaskRunning = false;
            dismissDialogSafely(ID_DIALOG_SAVING);
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
            final String status = getSelectedStatus();

            // TODO: update this to support new comment table schema (CommentTable.java)
            final Map<String, String> postHash = new HashMap<String, String>();
            postHash.put("status",       status);
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
                    postHash };

            Object result;
            boolean isSaved;
            try {
                result = client.call("wp.editComment", xmlParams);
                isSaved = (result != null && Boolean.parseBoolean(result.toString()));
                if (isSaved) {
                    mComment.setAuthorEmail(authorEmail);
                    mComment.setAuthorUrl(authorURL);
                    mComment.setCommentText(content);
                    mComment.setStatus(status);
                    mComment.setAuthorName(authorName);

                    postHash.put("url", postHash.get("author_url"));
                    postHash.put("email", postHash.get("author_email"));
                    postHash.put("comment", postHash.get("content"));

                    postHash.remove("author_url");
                    postHash.remove("author_email");
                    postHash.remove("content");

                    CommentTable.updateComment(
                            mLocalBlogId,
                            mCommentId,
                            postHash);

                }
                return isSaved;
            } catch (XMLRPCException e) {
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mIsUpdateTaskRunning = false;
            dismissDialogSafely(ID_DIALOG_SAVING);

            if (!result) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditCommentActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.error));
                dialogBuilder.setMessage(R.string.error_edit_comment);
                dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                dialogBuilder.create().show();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // TODO: ask to save changes rather than confirm cancel
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
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            // just close the dialog window
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            super.onBackPressed();
        }
    }

}
