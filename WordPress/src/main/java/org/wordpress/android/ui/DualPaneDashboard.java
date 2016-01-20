package org.wordpress.android.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Exposes some of the DualPaneDashboardHostFragment functionality
 */

public interface DualPaneDashboard {

    void showContentInDashboard(Class contentFragmentClass, Intent activityIntent);

    void onContentActivityStarted();

    void removeContentFragment();

    Fragment getContentPaneFragment();

    boolean isInDualPaneMode();
}
