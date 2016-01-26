package org.wordpress.android.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.DualPaneHost;

public class DualPaneHelper {

    public static boolean isInDualPaneConfiguration(Context context) {
        return context.getResources().getBoolean(R.bool.dual_pane);
    }

    @Nullable
    public static DualPaneHost getDualPaneHost(Fragment fragment) {
        if (!isPartOfDualPaneDashboard(fragment)) {
            return null;
        }
        return (DualPaneHost) fragment.getParentFragment();
    }

    public static boolean isPartOfDualPaneDashboard(Fragment fragment) {
        return fragment.getParentFragment() != null && fragment.getParentFragment() instanceof DualPaneHost;
    }

    public static boolean isInDualPaneMode(Fragment fragment) {
        return fragment.isAdded() && isInDualPaneConfiguration(fragment.getActivity()) && isPartOfDualPaneDashboard(fragment);
    }
}
