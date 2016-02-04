package org.wordpress.android.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.DualPaneHost;

/**
 * Collection of helper methods that helps dealing with with current configuration state
 */
public class DualPaneHelper {

    public static boolean isInDualPaneConfiguration(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.dual_pane);
    }

    @Nullable
    public static DualPaneHost getDualPaneHost(Fragment fragment) {
        if (!isPartOfDualPaneHost(fragment)) {
            return null;
        }
        return (DualPaneHost) fragment.getParentFragment();
    }

    public static boolean isPartOfDualPaneHost(Fragment fragment) {
        return fragment != null && fragment.getParentFragment() != null
                && fragment.getParentFragment() instanceof DualPaneHost;
    }

    //To be in dual pane mode, we need to be able to access dual pane resource qualifier AND be a part of dual pane host
    public static boolean isInDualPaneMode(Fragment fragment) {
        return fragment != null && fragment.isAdded() && isInDualPaneConfiguration(fragment.getActivity())
                && isPartOfDualPaneHost(fragment);
    }
}
