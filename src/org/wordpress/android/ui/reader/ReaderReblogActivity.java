package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.wordpress.android.widgets.WPNetworkImageView;

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
    private EditText mEditComment;
    private ProgressBar mProgress;
    private Spinner mSpinner;

    private long mDestinationBlogId;
    private boolean mIsSubmittingReblog = false;

    private static final int INTENT_SETTINGS = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_reblog);
        setupActionBar();

        mBlogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        mPostId = getIntent().getLongExtra(ReaderActivity.ARG_POST_ID, 0);

        mProgress = (ProgressBar) findViewById(R.id.progress);
        mEditComment = (EditText) findViewById(R.id.edit_comment);

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


        loadPost();
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBackPressed() {
        // don't allow backing out if we're still submitting the reblog
        if (!mIsSubmittingReblog)
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reader_reblog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_publish:
                submitReblog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            mSpinner.setEnabled(false);
            mProgress.setVisibility(View.VISIBLE);
            mEditComment.setEnabled(false);
        } else {
            mSpinner.setEnabled(true);
            mProgress.setVisibility(View.INVISIBLE);
            mEditComment.setEnabled(true);
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
     * AsyncTask to load and display post
     */
    private class LoadPostTask extends AsyncTask<Void, Void, Boolean> {
        ReaderPost tmpPost;

        ViewGroup layoutExcerpt;
        TextView txtBlogName;
        TextView txtTitle;
        TextView txtExcerpt;
        WPNetworkImageView imgAvatar;
        WPNetworkImageView imgFeatured;

        @Override
        protected Boolean doInBackground(Void... voids) {
            layoutExcerpt = (ViewGroup) findViewById(R.id.layout_post_excerpt);
            txtBlogName = (TextView) layoutExcerpt.findViewById(R.id.text_blog_name);
            txtTitle = (TextView) layoutExcerpt.findViewById(R.id.text_title);
            txtExcerpt = (TextView) layoutExcerpt.findViewById(R.id.text_excerpt);
            imgAvatar = (WPNetworkImageView) layoutExcerpt.findViewById(R.id.image_avatar);
            imgFeatured = (WPNetworkImageView) layoutExcerpt.findViewById(R.id.image_featured);

            tmpPost = ReaderPostTable.getPost(mBlogId, mPostId);
            return (tmpPost != null);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPost = tmpPost;

                txtTitle.setText(mPost.getTitle());

                if (mPost.hasBlogName()) {
                    txtBlogName.setText(mPost.getBlogName());
                } else if (mPost.hasAuthorName()) {
                    txtBlogName.setText(mPost.getAuthorName());
                }

                if (mPost.hasExcerpt()) {
                    txtExcerpt.setText(mPost.getExcerpt());
                } else {
                    txtExcerpt.setVisibility(View.GONE);
                }

                int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);

                if (mPost.hasFeaturedImage()) {
                    int displayWidth = DisplayUtils.getDisplayPixelWidth(ReaderReblogActivity.this);
                    int listMargin = getResources().getDimensionPixelSize(R.dimen.reader_list_margin);
                    int photonWidth = displayWidth - (listMargin * 2);
                    int photonHeight = getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);
                    final String imageUrl = mPost.getFeaturedImageForDisplay(photonWidth, photonHeight);
                    imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
                } else if (mPost.hasFeaturedVideo()) {
                    imgFeatured.setVideoUrl(mPost.postId, mPost.getFeaturedVideo());
                } else {
                    imgFeatured.setVisibility(View.GONE);
                }
            }
        }
    }
}
