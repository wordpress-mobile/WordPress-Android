package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderBlogDetailActivity extends SherlockFragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_blog_detail);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        showBlogInfo(ReaderBlogTable.getBlogInfo(blogId));
        requestBlogInfo(blogId);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getMshotsUrl(String blogUrl, int width) {
        if (TextUtils.isEmpty(blogUrl)) {
            return "";
        }

        return "http://s.wordpress.com/mshots/v1/"
              + UrlUtils.urlEncode(blogUrl)
              + "?w=" + Integer.toString(width);
    }

    /*
     * request latest info for this blog
     */
    private void requestBlogInfo(final long blogId) {
        ReaderActions.RequestBlogInfoListener listener = new ReaderActions.RequestBlogInfoListener() {
            @Override
            public void onResult(ReaderBlogInfo blogInfo) {
                if (isFinishing()) {
                    return;
                }
                hideProgress();
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
                }
            }
        };
        showProgress();
        ReaderBlogActions.updateBlogInfo(blogId, listener);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlogInfo blog) {
        final TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) findViewById(R.id.text_blog_description);
        final TextView txtFollowCnt = (TextView) findViewById(R.id.text_follow_count);
        final TextView txtFollowBtn = (TextView) findViewById(R.id.text_follow_blog);
        final View divider = findViewById(R.id.divider);
        final WPNetworkImageView imgMshot = (WPNetworkImageView) findViewById(R.id.image_mshot);

        if (blog != null) {
            txtBlogName.setText(blog.getName());
            txtDescription.setText(blog.getDescription());
            txtDescription.setVisibility(blog.hasDescription() ? View.VISIBLE : View.GONE);
            String numFollowers = getResources().getString(R.string.reader_label_followers, FormatUtils.formatInt(blog.numSubscribers));
            txtFollowCnt.setText(numFollowers);

            boolean isFollowing = ReaderBlogTable.isFollowedBlogUrl(blog.getUrl());
            showBlogFollowStatus(txtFollowBtn, isFollowing);
            txtFollowBtn.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);

            txtFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleBlogFollowStatus(txtFollowBtn, blog);
                }
            });

            int width = DisplayUtils.getDisplayPixelWidth(this);
            imgMshot.setImageUrl(getMshotsUrl(blog.getUrl(), width), WPNetworkImageView.ImageType.PHOTO);
        } else {
            txtBlogName.setText(null);
            txtDescription.setText(null);
            txtFollowCnt.setText(null);
            txtFollowBtn.setVisibility(View.INVISIBLE);
            divider.setVisibility(View.INVISIBLE);
        }
    }

    private void toggleBlogFollowStatus(final TextView txtFollow, final ReaderBlogInfo blogInfo) {
        if (blogInfo == null || txtFollow == null) {
            return;
        }

        AniUtils.zoomAction(txtFollow);

        boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogInfo.getUrl());
        ReaderBlogActions.BlogAction blogAction = (isCurrentlyFollowing ? ReaderBlogActions.BlogAction.UNFOLLOW : ReaderBlogActions.BlogAction.FOLLOW);
        if (!ReaderBlogActions.performBlogAction(blogAction, blogInfo.blogId, blogInfo.getUrl()))
            return;

        boolean isNowFollowing = !isCurrentlyFollowing;
        showBlogFollowStatus(txtFollow, isNowFollowing);
    }

    /*
     * updates the follow button in the blog header to match whether the current
     * user is following this blog
     */
    private void showBlogFollowStatus(TextView txtFollow, boolean isFollowed) {
        String following = getString(R.string.reader_btn_unfollow).toUpperCase();
        String follow = getString(R.string.reader_btn_follow).toUpperCase();

        txtFollow.setSelected(isFollowed);
        txtFollow.setText(isFollowed ? following : follow);
        int drawableId = (isFollowed ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
    }

    private void showProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.GONE);
    }
}
