package org.wordpress.android.ui.comments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditCommentActivity extends Activity {
    static final String ARG_LOCAL_BLOG_ID = "blog_id";
    static final String ARG_COMMENT_ID = "comment_id";

    private static final int ID_DIALOG_SAVING = 0;

    private int mLocalBlogId;
    private long mCommentId;
    private Comment mComment;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.comment_edit_activity);
        setTitle(getString(R.string.edit_comment));

        ActionBar actionBar = getActionBar();
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
        mCommentId = intent.getLongExtra(ARG_COMMENT_ID, 0);
        mComment = CommentTable.getComment(mLocalBlogId, mCommentId);
        if (mComment == null)
            return false;

        final EditText editAuthorName = (EditText) this.findViewById(R.id.author_name);
        editAuthorName.setText(mComment.getAuthorName());

        final EditText editAuthorEmail = (EditText) this.findViewById(R.id.author_email);
        editAuthorEmail.setText(mComment.getAuthorEmail());

        final EditText editAuthorUrl = (EditText) this.findViewById(R.id.author_url);
        editAuthorUrl.setText(mComment.getAuthorUrl());

        final EditText editContent = (EditText) this.findViewById(R.id.comment_content);
        editContent.setText(mComment.getCommentText());

        // show error when comment content is empty
        editContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean hasError = (editContent.getError() != null);
                boolean hasText = (s != null && s.length() > 0);
                if (!hasText && !hasError) {
                    editContent.setError(getString(R.string.content_required));
                } else if (hasText && hasError) {
                    editContent.setError(null);
                }
            }
        });

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
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

        // return immediately if comment hasn't changed
        if (!isCommentEdited()) {
            ToastUtils.showToast(this, R.string.toast_comment_unedited);
            return;
        }

        // make sure we have an active connection
        if (!NetworkUtils.checkConnection(this))
            return;

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
        final String authorUrl = getEditTextStr(R.id.author_url);
        final String content = getEditTextStr(R.id.comment_content);

        return !(authorName.equals(mComment.getAuthorName())
                && authorEmail.equals(mComment.getAuthorEmail())
                && authorUrl.equals(mComment.getAuthorUrl())
                && content.equals(mComment.getCommentText()));
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
            final Blog blog;
            blog = WordPress.wpDB.instantiateBlogByLocalId(mLocalBlogId);
            if (blog == null) {
                AppLog.e(AppLog.T.COMMENTS, "Invalid local blog id:" + mLocalBlogId);
                return false;
            }
            final String authorName = getEditTextStr(R.id.author_name);
            final String authorEmail = getEditTextStr(R.id.author_email);
            final String authorUrl = getEditTextStr(R.id.author_url);
            final String content = getEditTextStr(R.id.comment_content);

            final Map<String, String> postHash = new HashMap<String, String>();

            // using CommentStatus.toString() rather than getStatus() ensures that the XML-RPC
            // status value is used - important since comment may have been loaded via the
            // REST API, which uses different status values
            postHash.put("status",       CommentStatus.toString(mComment.getStatusEnum()));
            postHash.put("content",      content);
            postHash.put("author",       authorName);
            postHash.put("author_url",   authorUrl);
            postHash.put("author_email", authorEmail);

            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            Object[] xmlParams = {blog.getRemoteBlogId(), blog.getUsername(), blog.getPassword(), Long.toString(
                    mCommentId), postHash};

            try {
                Object result = client.call("wp.editComment", xmlParams);
                boolean isSaved = (result != null && Boolean.parseBoolean(result.toString()));
                if (isSaved) {
                    mComment.setAuthorEmail(authorEmail);
                    mComment.setAuthorUrl(authorUrl);
                    mComment.setAuthorName(authorName);
                    mComment.setCommentText(content);
                    CommentTable.updateComment(mLocalBlogId, mComment);
                }
                return isSaved;
            } catch (XMLRPCException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            } catch (IOException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            } catch (XmlPullParserException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
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
                // alert user to error
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
