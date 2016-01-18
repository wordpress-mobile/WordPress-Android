package org.wordpress.android.ui.main;

import android.content.Intent;

public interface DualPaneDashboard {

    void showContentInDashboard(Class contentFragmentClass, Intent activityIntent);

    void notifyContentActivityStarted();

    void removeContentFragment();

    boolean isContentFragmentAdded(Class contentFragmentClass);

    boolean isInDualPaneMode();

}
