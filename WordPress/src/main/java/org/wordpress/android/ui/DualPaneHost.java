package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Exposes {@link DualPaneHostFragment} functionality
 */
public interface DualPaneHost {

    /**
     * Adds fragment to content pane of DualPaneHostFragment
     *
     * @param contentFragmentClass {@code Class} of a fragment you want to show
     * @param activityIntent       {@code Intent} that be used to start content activity in single pane.
     *                             Any parameters you would like to pass to fragment should be added to Intent.
     */
    void showContent(Class contentFragmentClass, Intent activityIntent);

    /**
     * Adds fragment to content pane of this {@code DualPaneHostFragment}
     *
     * @param contentFragmentClass {@code Class} of a fragment you want to show
     * @param fragmentArgs         Arguments that you would like to pass to fragment
     */
    void showContent(Class contentFragmentClass, Bundle fragmentArgs);

    /**
     * Used to notify {@code DualPaneHost} that content activity started.
     */
    void onContentActivityStarted();

    /**
     * Removes content fragment from the content pane of {@code DualPaneHost}
     */
    void removeContentPaneFragment();

    /**
     * @return fragment attached to content pane of {@code DualPaneHost}
     */
    Fragment getContentPaneFragment();

}
