package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.DualPaneHostFragment;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.prefs.AppPrefs;

/**
 * Concrete implementation of MySite dual pane fragment.
 */

public class MySiteDualPaneFragment extends DualPaneHostFragment implements WPMainActivity.OnScrollToTopListener {

    private static final String PREVIOUS_BLOG_LOCAL_ID = "previous_blog_local_id";

    private int mPreviousBlogId = -1;
    private int mCurrentBlogId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPreviousBlogId = savedInstanceState.getInt(PREVIOUS_BLOG_LOCAL_ID, -1);
        }

        mCurrentBlogId = BlogUtils.getBlogLocalId(WordPress.getCurrentBlog());
    }

    public static MySiteDualPaneFragment newInstance() {
        return new MySiteDualPaneFragment();
    }

    @Override
    protected Fragment initializeSidebarFragment() {
        return MySiteFragment.newInstance();
    }

    @Override
    protected Fragment initializeDefaultFragment() {
        if (WordPress.getCurrentBlog() == null) {
            return null;
        }

        return Fragment.instantiate(getActivity(), PostsListFragment.class.getName(), null);
    }

    // We do not want to show content activity when switching to single pane mode in case we are not inside MySite tab or
    // when we changed blog (oppened site picker in dual pane, switched to single and picked a site)
    @Override
    protected boolean canOpenActivityAfterSwitchToSinglePaneMode() {
        boolean isSameBlog = isSameBlog();
        mPreviousBlogId = mCurrentBlogId;

        return AppPrefs.getMainTabIndex() == WPMainTabAdapter.TAB_MY_SITE && isSameBlog;
    }

    private boolean isSameBlog() {
        return mPreviousBlogId == -1 || mPreviousBlogId == mCurrentBlogId;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public MySiteFragment getMySiteFragment() {
        return (MySiteFragment) getSidebarFragment();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PREVIOUS_BLOG_LOCAL_ID, mCurrentBlogId);
    }

    @Override
    public void onScrollToTop() {
        passScrollToTopEvent(getSidebarFragment());
        passScrollToTopEvent(getContentPaneFragment());
    }

    private void passScrollToTopEvent(Fragment fragment) {
        if (fragment != null && fragment instanceof WPMainActivity.OnScrollToTopListener) {
            ((WPMainActivity.OnScrollToTopListener) fragment).onScrollToTop();
        }
    }
}
