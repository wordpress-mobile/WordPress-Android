package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class EditCommentActivity extends LocaleAwareActivity {
    static final String KEY_COMMENT = "KEY_COMMENT";
    static final String KEY_NOTE_ID = "KEY_NOTE_ID";

    private static final int ID_DIALOG_SAVING = 0;
    private static final String ARG_CANCEL_EDITING_COMMENT_DIALOG_VISIBLE = "cancel_editing_comment_dialog_visible";

    private SiteModel mSite;
    private CommentModel mComment;
    private Note mNote;
    private boolean mFetchingComment;

    private AlertDialog mCancelEditCommentDialog;

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject CommentStore mCommentStore;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.comment_edit_activity);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        loadComment(getIntent());

        if (icicle != null) {
            if (icicle.getBoolean(ARG_CANCEL_EDITING_COMMENT_DIALOG_VISIBLE, false)) {
                cancelEditCommentConfirmation();
            }
        }

        ActivityId.trackLastActivity(ActivityId.COMMENT_EDITOR);
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCancelEditCommentDialog != null) {
            outState.putBoolean(ARG_CANCEL_EDITING_COMMENT_DIALOG_VISIBLE, mCancelEditCommentDialog.isShowing());
        }
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    private void loadComment(Intent intent) {
        if (intent == null) {
            showErrorAndFinish();
            return;
        }

        mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
        mComment = (CommentModel) intent.getSerializableExtra(KEY_COMMENT);
        final String noteId = intent.getStringExtra(KEY_NOTE_ID);

        // If the noteId is passed, load the comment from the note
        if (noteId != null) {
            loadCommentFromNote(noteId);
            return;
        }

        // Else make sure the comment has been passed
        if (mComment == null) {
            showErrorAndFinish();
            return;
        }
        configureViews();
    }

    private void loadCommentFromNote(String noteId) {
        mNote = NotificationsTable.getNoteById(noteId);
        if (mNote != null) {
            setFetchProgressVisible(true);
            mSite = mSiteStore.getSiteBySiteId(mNote.getSiteId());
            RemoteCommentPayload payload = new RemoteCommentPayload(mSite, mNote.getCommentId());
            mFetchingComment = true;
            mDispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload));
        } else {
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        ToastUtils.showToast(this, R.string.error_load_comment);
        finish();
    }

    private void configureViews() {
        final EditText editContent = this.findViewById(R.id.edit_comment_content);
        editContent.setText(mComment.getContent());

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
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == R.id.menu_save_comment) {
            saveComment();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private String getEditTextStr(int resId) {
        final EditText edit = findViewById(resId);
        return EditTextUtils.getText(edit);
    }

    private void saveComment() {
        // make sure comment content was entered
        final EditText editContent = findViewById(R.id.edit_comment_content);
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
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        showSaveDialog();
        mComment.setContent(getEditTextStr(R.id.edit_comment_content));
        mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(mSite, mComment)));
    }

    /*
     * returns true if user made any changes to the comment
     */
    private boolean isCommentEdited() {
        if (mComment == null) {
            return false;
        }
        final String content = getEditTextStr(R.id.edit_comment_content);
        return !content.equals(mComment.getContent());
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

    private void showEditErrorAlert() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(R.string.error_edit_comment);
        dialogBuilder.setPositiveButton(android.R.string.ok, (dialog1, whichButton) -> {
            // just close the dialog
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void setFetchProgressVisible(boolean progressVisible) {
        final ProgressBar progress = findViewById(R.id.edit_comment_progress);
        final View editContainer = findViewById(R.id.edit_comment_container);

        if (progress == null || editContainer == null) {
            return;
        }

        progress.setVisibility(progressVisible ? View.VISIBLE : View.GONE);
        editContainer.setVisibility(progressVisible ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (isCommentEdited()) {
            cancelEditCommentConfirmation();
        } else {
            super.onBackPressed();
        }
    }

    private void cancelEditCommentConfirmation() {
        if (mCancelEditCommentDialog != null) {
            mCancelEditCommentDialog.show();
            return;
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_cancel_edit_comment));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                (dialog, whichButton) -> finish());
        dialogBuilder.setNegativeButton(
                getResources().getText(R.string.no),
                (dialog, whichButton) -> {
                    // just close the dialog
                });
        dialogBuilder.setCancelable(true);

        mCancelEditCommentDialog = dialogBuilder.create();
        mCancelEditCommentDialog.show();
    }

    private void onCommentPushed(OnCommentChanged event) {
        if (isFinishing()) {
            return;
        }

        dismissSaveDialog();

        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            showEditErrorAlert();
            return;
        }

        setResult(RESULT_OK);
        finish();
    }

    private void onCommentFetched(OnCommentChanged event) {
        if (isFinishing() || !mFetchingComment) {
            return;
        }
        mFetchingComment = false;
        setFetchProgressVisible(false);

        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type + " - message: " + event.error.message);
            showErrorAndFinish();
            return;
        }

        if (mNote != null) {
            mComment = mCommentStore.getCommentBySiteAndRemoteId(mSite, mNote.getCommentId());
        } else if (mComment != null) {
            // Reload the comment
            mComment = mCommentStore.getCommentByLocalId(mComment.getId());
        }
        configureViews();
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        if (event.causeOfChange == CommentAction.FETCH_COMMENT) {
            onCommentFetched(event);
        }
        if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
            onCommentPushed(event);
        }
    }
}
