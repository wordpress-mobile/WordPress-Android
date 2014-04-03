package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.ui.reader.ReaderPostListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.FormatUtils;

/*
 * shows reader posts in a specific blog
 */
public class ReaderBlogDetailActivity extends SherlockFragmentActivity implements OnPostSelectedListener {

    private View mBlogHeaderView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_blog_detail);
        mBlogHeaderView = findViewById(R.id.layout_blog_header);

        long blogId = getIntent().getLongExtra(ReaderPostListFragment.ARG_BLOG_ID, 0);
        requestBlogInfo(blogId);
        showListFragment(blogId);
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
     * user tapped a post in the post list fragment
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        // TODO
    }


    private void requestBlogInfo(final long blogId) {
        // first get info from local db
        ReaderBlogInfo blogInfo = ReaderBlogTable.getBlogInfo(blogId);

        // show existing info for this blog (if any)
        if (blogInfo != null) {
            showBlogInfo(blogInfo);
        } else {
            showBlogInfo(null);
        }

        // then request latest info for this blog
        ReaderBlogActions.updateBlogInfo(blogId, new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (succeeded && !isFinishing()) {
                    showBlogInfo(ReaderBlogTable.getBlogInfo(blogId));
                }
            }
        });
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
        } else {
            txtBlogName.setText(null);
            txtDescription.setText(null);
            txtFollowCnt.setText(null);
            txtFollowBtn.setVisibility(View.INVISIBLE);
            divider.setVisibility(View.INVISIBLE);
        }

        if (mBlogHeaderView.getVisibility() != View.VISIBLE)
            mBlogHeaderView.setVisibility(View.VISIBLE);
    }

    /*
     * used when viewing posts in a specific blog, toggles the status of the currently displayed blog
     */
    private void toggleBlogFollowStatus(final TextView txtFollow, final ReaderBlogInfo blogInfo) {
        if (blogInfo == null || txtFollow == null) {
            return;
        }

        AniUtils.zoomAction(txtFollow);

        boolean isCurrentlyFollowing = ReaderBlogTable.isFollowedBlogUrl(blogInfo.getUrl());
        ReaderBlogActions.BlogAction blogAction = (isCurrentlyFollowing ? ReaderBlogActions.BlogAction.UNFOLLOW : ReaderBlogActions.BlogAction.FOLLOW);
        if (!ReaderBlogActions.performBlogAction(blogAction, blogInfo.getUrl()))
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
}
