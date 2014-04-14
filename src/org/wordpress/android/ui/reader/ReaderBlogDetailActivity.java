package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.ToastUtils;

/*
 * shows reader posts in a specific blog
 */
public class ReaderBlogDetailActivity extends SherlockFragmentActivity implements ReaderPostListFragment.OnPostSelectedListener {
    private View mBlogHeaderView;
    private boolean mHasBlogInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_blog_detail);
        mBlogHeaderView = findViewById(R.id.layout_blog_header);

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

    private void showListFragment(long blogId) {
        Fragment fragment = ReaderPostListFragment.newInstance(blogId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    /*
     * request latest info for this blog
     */
    private void requestBlogInfo(final long blogId) {
        showLoadingProgress();

        ReaderActions.RequestBlogInfoListener listener = new ReaderActions.RequestBlogInfoListener() {
            @Override
            public void onResult(ReaderBlogInfo blogInfo) {
                if (isFinishing()) {
                    return;
                }
                hideLoadingProgress();
                if (blogInfo != null) {
                    showBlogInfo(blogInfo);
                } else if (!mHasBlogInfo) {
                    handleBlogNotFound();
                }
            }
        };

        ReaderBlogActions.updateBlogInfo(blogId, listener);
    }

    /*
     * alert user when blog couldn't be loaded, then dismiss activity after a brief delay
     */
    private void handleBlogNotFound() {
        ToastUtils.showToast(this, R.string.reader_toast_err_get_blog);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1000);
    }

    /*
     * show blog header with info from passed blog filled in
     */
    private void showBlogInfo(final ReaderBlogInfo blog) {
        if (mBlogHeaderView == null) {
            return;
        }

        final TextView txtBlogName = (TextView) mBlogHeaderView.findViewById(R.id.text_blog_name);
        final TextView txtDescription = (TextView) mBlogHeaderView.findViewById(R.id.text_blog_description);
        final TextView txtFollowCnt = (TextView) mBlogHeaderView.findViewById(R.id.text_follow_count);
        final TextView txtFollowBtn = (TextView) mBlogHeaderView.findViewById(R.id.text_follow_blog);
        final View divider = mBlogHeaderView.findViewById(R.id.divider_blog_header);

        mHasBlogInfo = (blog != null);

        if (mHasBlogInfo) {
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

            // show the list of posts in this blog if it isn't already showing
            if (!hasListFragment()) {
                showListFragment(blog.blogId);
            }
        } else {
            txtBlogName.setText(null);
            txtDescription.setText(null);
            txtFollowCnt.setText(null);
            txtFollowBtn.setVisibility(View.INVISIBLE);
            divider.setVisibility(View.INVISIBLE);
        }

        if (mBlogHeaderView.getVisibility() != View.VISIBLE) {
            mBlogHeaderView.setVisibility(View.VISIBLE);
        }
    }

    /*
     * used when viewing posts in a specific blog, toggles the follow status of the currently displayed blog
     */
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
        if (hasListFragment()) {
            getListFragment().updateFollowStatusOnPostsForBlog(blogInfo.blogId, isNowFollowing);
        }
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

    private void showLoadingProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingProgress() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading);
        if (progress != null) {
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPostSelected(long blogId, long postId) {
        showDetailFragment(blogId, postId);
    }

    /*
     * show fragment containing detail for passed post
     */
    private void showDetailFragment(long blogId, long postId) {
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        Fragment fragment = ReaderPostDetailFragment.newInstance(blogId, postId);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .replace(R.id.fragment_container, fragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .commit();
    }
}
