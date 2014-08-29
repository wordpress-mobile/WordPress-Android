package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderReblogAdapter;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.widgets.WPNetworkImageView;

/*
 * displayed when user taps to reblog a post in the Reader
 */
public class ReaderReblogActivity extends Activity {
    private long mBlogId;
    private long mPostId;
    private ReaderPost mPost;

    private ReaderReblogAdapter mAdapter;
    private EditText mEditComment;
    private ViewGroup mLayoutExcerpt;

    private long mDestinationBlogId;
    private boolean mIsSubmittingReblog = false;

    private static final int INTENT_SETTINGS = 200;
    private static final String KEY_DESTINATION_BLOG_ID = "destination_blog_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_reblog);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(getReblogAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    mDestinationBlogId = itemId;
                    return true;
                }
            });
        }

        mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
        mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);

        mEditComment = (EditText) findViewById(R.id.edit_comment);
        mLayoutExcerpt = (ViewGroup) findViewById(R.id.layout_post_excerpt);

        if (savedInstanceState == null) {
            mEditComment.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        animateCommentView();
                    }
                }
            }, 300);
        }

        loadPost();
    }

    void animateCommentView() {
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);

        ObjectAnimator commentAnim = ObjectAnimator.ofFloat(mEditComment, View.TRANSLATION_Y, displayHeight, 0f);
        commentAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mEditComment.setVisibility(View.VISIBLE);
            }
        });
        commentAnim.setInterpolator(new DecelerateInterpolator());
        commentAnim.setDuration(duration);
        commentAnim.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mDestinationBlogId != 0) {
            outState.putLong(KEY_DESTINATION_BLOG_ID, mDestinationBlogId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(KEY_DESTINATION_BLOG_ID)) {
            mDestinationBlogId = savedInstanceState.getLong(KEY_DESTINATION_BLOG_ID);
        }
    }

    @Override
    public void onBackPressed() {
        // don't allow backing out if we're still submitting the reblog
        if (!mIsSubmittingReblog) {
            super.onBackPressed();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // reload adapter if user returned from settings since blog visibility may have changed
        if (requestCode == INTENT_SETTINGS) {
            getReblogAdapter().reload();
        }
    }

    private boolean hasReblogAdapter() {
        return (mAdapter != null);
    }

    private ReaderReblogAdapter getReblogAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderReblogAdapter(this, mBlogId, new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    // show empty message and hide other views if there are no visible blogs to reblog to
                    final TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
                    final View scrollView = findViewById(R.id.scroll_view);

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
                    scrollView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    // restore the previously selected destination blog id
                    if (!isEmpty && mDestinationBlogId != 0) {
                        selectBlogInActionbar(mDestinationBlogId);
                    }
                }
            });
        }

        return mAdapter;
    }

    private void selectBlogInActionbar(long blogId) {
        ActionBar actionBar = getActionBar();
        if (!hasReblogAdapter() || actionBar == null) {
            return;
        }
        int index = getReblogAdapter().indexOfBlogId(blogId);
        if (index > -1
                && index < actionBar.getNavigationItemCount()
                && index != actionBar.getSelectedNavigationIndex()) {
            actionBar.setSelectedNavigationItem(index);
        }
    }

    private void showProgress() {
        final ViewGroup layoutProgress = (ViewGroup) findViewById(R.id.layout_progress);
        ObjectAnimator anim = ObjectAnimator.ofFloat(layoutProgress, View.ALPHA, 0f, 1f);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                layoutProgress.setVisibility(View.VISIBLE);
            }
        });
        anim.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        anim.start();
    }

    private void hideProgress() {
        final ViewGroup layoutProgress = (ViewGroup) findViewById(R.id.layout_progress);
        layoutProgress.clearAnimation();

        ObjectAnimator anim = ObjectAnimator.ofFloat(layoutProgress, View.ALPHA, 1f, 0f);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                layoutProgress.setVisibility(View.GONE);
            }
        });
        anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        anim.start();
    }

    private void submitReblog() {
        if (mDestinationBlogId == 0) {
            ToastUtils.showToast(this, R.string.reader_toast_err_reblog_requires_blog);
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        if (mIsSubmittingReblog) {
            return;
        }

        String commentText = EditTextUtils.getText(mEditComment);
        mIsSubmittingReblog = true;
        EditTextUtils.hideSoftInput(mEditComment);
        showProgress();

        final ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingReblog = false;
                if (!isFinishing()) {
                    hideProgress();
                    if (succeeded) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_REBLOGGED_ARTICLE);
                        reblogSucceeded();
                    } else {
                        ToastUtils.showToast(ReaderReblogActivity.this, R.string.reader_toast_err_reblog_failed);
                    }
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
                data.putExtra(ReaderConstants.ARG_BLOG_ID, mBlogId);
                data.putExtra(ReaderConstants.ARG_POST_ID, mPostId);
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

        TextView txtBlogName;
        TextView txtTitle;
        TextView txtExcerpt;

        WPNetworkImageView imgAvatar;
        WPNetworkImageView imgFeatured;

        @Override
        protected Boolean doInBackground(Void... voids) {
            txtBlogName = (TextView) mLayoutExcerpt.findViewById(R.id.text_blog_name);
            txtTitle = (TextView) mLayoutExcerpt.findViewById(R.id.text_title);
            txtExcerpt = (TextView) mLayoutExcerpt.findViewById(R.id.text_excerpt);
            imgAvatar = (WPNetworkImageView) mLayoutExcerpt.findViewById(R.id.image_avatar);
            imgFeatured = (WPNetworkImageView) mLayoutExcerpt.findViewById(R.id.image_featured);

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

                // actual avatar size is avatar_sz_small but use avatar_sz_medium since we know
                // that will be cached already
                int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
                imgAvatar.setImageUrl(mPost.getPostAvatarForDisplay(avatarSz), WPNetworkImageView.ImageType.AVATAR);

                // featured image is hidden in landscape so it doesn't obscure the comment text
                boolean isLandscape = DisplayUtils.isLandscape(ReaderReblogActivity.this);
                if (!isLandscape && mPost.hasFeaturedImage()) {
                    int displayWidth = DisplayUtils.getDisplayPixelWidth(ReaderReblogActivity.this);
                    int listMargin = getResources().getDimensionPixelSize(R.dimen.reader_list_margin);
                    int photonWidth = displayWidth - (listMargin * 2);
                    int photonHeight = getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);
                    final String imageUrl = mPost.getFeaturedImageForDisplay(photonWidth, photonHeight);
                    imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
                } else if (!isLandscape && mPost.hasFeaturedVideo()) {
                    imgFeatured.setVideoUrl(mPost.postId, mPost.getFeaturedVideo());
                } else {
                    imgFeatured.setVisibility(View.GONE);
                }
            }
        }
    }
}
