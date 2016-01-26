package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Exposes functionality of {@link DualPaneHostFragment}.
 */
public interface DualPaneHost {

    /**
     * Adds fragment to content pane of DualPaneHostFragment
     * Fragment would be instantiated using {@link Fragment#instantiate(Context, String, Bundle)}
     *
     * @param contentFragmentClass {@code Class} of a fragment you want to show
     * @param activityIntent       {@code Intent} that be used to start content activity in single pane.
     *                             Any parameters you would like to pass to fragment should be added to Intent.
     */
    void showContent(Class contentFragmentClass, String tag, Intent activityIntent);

    /**
     * Adds fragment to content pane of this {@code DualPaneHostFragment}
     * Fragment would be instantiated using {@link Fragment#instantiate(Context, String, Bundle)}
     *
     * @param contentFragmentClass {@code Class} of a fragment you want to show
     * @param fragmentArgs         Arguments that you would like to pass to fragment
     */
    void showContent(Class contentFragmentClass, String tag, Bundle fragmentArgs);

    /**
     * @return fragment attached to content pane of {@code DualPaneHost}
     */
    Fragment getContentPaneFragment();

    /**
     * @param tag of a {@code Fragment} you want to check
     * @return true if the {@code Fragment} exists and attached to content pane
     */
    boolean isFragmentAdded(String tag);

    /**
     * Used to notify {@code DualPaneHost} that content activity started.
     */
    void onContentActivityStarted();

    /**
     * Removes content fragment from the content pane of {@code DualPaneHost}
     */
    void resetContentPane();
}
