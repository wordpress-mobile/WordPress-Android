package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderReblogAdapter;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

/**
 * displayed when user taps to reblog a post in the Reader
 *
 * note that this activity uses android:configChanges="orientation|keyboardHidden|screenSize"
 * in the manifest to prevent re-creation when the device is rotated - important since we
 * don't want the activity re-created while the reblog is being submitted
 */
public class ReaderReblogActivity extends Activity {
    private long mBlogId;
    private long mPostId;
    private ReaderPost mPost;

    private ReaderReblogAdapter mAdapter;
    private Button mBtnReblog;
    private EditText mEditComment;
    private ProgressBar mProgress;
    private Spinner mSpinner;

    private long mDestinationBlogId;
    private boolean mIsSubmittingReblog = false;

    private static final int INTENT_SETTINGS = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reader_activity_reblog);

        mBlogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        mPostId = getIntent().getLongExtra(ReaderActivity.ARG_POST_ID, 0);

        mSpinner = (Spinner) findViewById(R.id.spinner_reblog);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mDestinationBlogId = id;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mDestinationBlogId = 0;
            }
        });
        mSpinner.setAdapter(getReblogAdapter());

        mProgress = (ProgressBar) findViewById(R.id.progress);
        mEditComment = (EditText) findViewById(R.id.edit_comment);
        mBtnReblog = (Button) findViewById(R.id.btn_reblog);
        mBtnReblog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReblog();
            }
        });

        loadPost();
    }

    @Override
    public void onBackPressed() {
        // don't allow backing out if we're still submitting the reblog
        if (!mIsSubmittingReblog)
            super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void loadPost() {
        new LoadPostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * called by adapter when data has been loaded
     */
    private final ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            // show empty message and hide other views if there are no visible blogs to reblog to
            final TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
            final View contentView = findViewById(R.id.layout_content);

            // empty message includes a link to settings so user can change blog visibility
            if (isEmpty) {
                String emptyMsg = getString(R.string.reader_label_reblog_empty);
                String emptyLink = "<a href='settings'>" + getString(R.string.reader_label_reblog_empty_link) + "</a>";
                txtEmpty.setText(Html.fromHtml(emptyMsg + "<br /><br />" + emptyLink));
                txtEmpty.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(ReaderReblogActivity.this, PreferencesActivity.class);
                        startActivityForResult(i, INTENT_SETTINGS);
                    }
                });
            }

            txtEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            contentView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // reload adapter if user returned from settings since blog visibility may have changed
        if (requestCode == INTENT_SETTINGS) {
            getReblogAdapter().reload();
        }
    }

    private ReaderReblogAdapter getReblogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderReblogAdapter(this, mBlogId, mDataLoadedListener);
        }
        return mAdapter;
    }

    private void setIsSubmittingReblog(boolean value) {
        mIsSubmittingReblog = value;
        if (mIsSubmittingReblog) {
            mProgress.setVisibility(View.VISIBLE);
            mBtnReblog.setVisibility(View.INVISIBLE);
            mEditComment.setEnabled(false);
            mSpinner.setEnabled(false);
        } else {
            mProgress.setVisibility(View.INVISIBLE);
            mBtnReblog.setVisibility(View.VISIBLE);
            mEditComment.setEnabled(true);
            mSpinner.setEnabled(true);
        }
    }

    private void submitReblog() {
        if (mDestinationBlogId == 0) {
            ToastUtils.showToast(this, R.string.reader_toast_err_reblog_requires_blog);
            return;
        }

        String commentText = EditTextUtils.getText(mEditComment);
        setIsSubmittingReblog(true);

        final ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                setIsSubmittingReblog(false);
                if (succeeded) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_REBLOGGED_ARTICLE);
                    reblogSucceeded();
                } else {
                    ToastUtils.showToast(ReaderReblogActivity.this, R.string.reader_toast_err_reblog_failed);
                }
            }
        };

        ReaderPostActions.reblogPost(mPost, mDestinationBlogId, commentText, actionListener);
    }

    private void reblogSucceeded() {
        ToastUtils.showToast(this, R.string.reader_toast_reblog_success);

        // wait a second before dismissing activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent data = new Intent();
                data.putExtra(ReaderActivity.ARG_BLOG_ID, mBlogId);
                data.putExtra(ReaderActivity.ARG_POST_ID, mPostId);
                setResult(RESULT_OK, data);
                finish();
            }
        }, 1000);
    }

    /*
     * AsyncTask to load post from db
     */
    private class LoadPostTask extends AsyncTask<Void, Void, Boolean> {
        ReaderPost tmpPost;
        @Override
        protected Boolean doInBackground(Void... voids) {
            tmpPost = ReaderPostTable.getPost(mBlogId, mPostId);
            return (tmpPost != null);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                mPost = tmpPost;
        }
    }
}
