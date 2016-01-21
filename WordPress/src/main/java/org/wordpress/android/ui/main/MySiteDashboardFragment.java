package org.wordpress.android.ui.main;

import android.content.Intent;
import android.support.v4.app.Fragment;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.DualPaneDashboardHostFragment;
import org.wordpress.android.ui.prefs.AppPrefs;

/**
 * Dashboard fragment that shows dual pane layout for tablets and single pane for smartphones.
 */

public class MySiteDashboardFragment extends DualPaneDashboardHostFragment {

    public static MySiteDashboardFragment newInstance() {
        return new MySiteDashboardFragment();
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

        return MeFragment.newInstance();
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