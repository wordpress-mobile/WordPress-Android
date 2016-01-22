package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Exposes some of the DualPaneHostFragment functionality
 */

public interface DualPaneHost {

    void showContent(Class contentFragmentClass, Intent activityIntent);

    void showContent(Class contentFragmentClass, Bundle parameters);

    void showContentForResult(Class contentFragmentClass, Intent activityIntent, int requestCode);

    void onContentActivityStarted();

    void removeContentFragment();

    Fragment getContentPaneFragment();
}
