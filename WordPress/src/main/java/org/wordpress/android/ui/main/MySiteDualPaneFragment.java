package org.wordpress.android.ui.main;

import android.content.Intent;
import android.support.v4.app.Fragment;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.DualPaneHostFragment;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.prefs.AppPrefs;

/**
 * Concrete implementation of dual pane fragment.
 */

public class MySiteDualPaneFragment extends DualPaneHostFragment {

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

//        return MeFragment.newInstance();
        return Fragment.instantiate(getActivity(), PostsListFragment.class.getName(), null);
    }

    @Override
    protected boolean shouldOpenActivityAfterSwitchToSinglePane() {
        //show activity after switching to single pane mode only while we are inside MySite tab.
        return AppPrefs.getMainTabIndex() == WPMainTabAdapter.TAB_MY_SITE;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public MySiteFragment getMySiteFragment() {
        return (MySiteFragment) getSidebarFragment();
    }
}