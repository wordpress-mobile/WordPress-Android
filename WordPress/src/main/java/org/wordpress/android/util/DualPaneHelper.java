package org.wordpress.android.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.ui.DualPaneDashboard;

public class DualPaneHelper {

    public static boolean isInDualPaneMode(Context content) {
        return content.getResources().getBoolean(R.bool.dual_pane);
    }

    @Nullable
    public static DualPaneDashboard getDashboard(Fragment fragment) {
        if (!isPartOfDualPaneDashboard(fragment)) {
            return null;
        }
        return (DualPaneDashboard) fragment.getParentFragment();
    }

    public static boolean isPartOfDualPaneDashboard(Fragment fragment) {
        return fragment.getParentFragment() != null && fragment.getParentFragment() instanceof DualPaneDashboard;
    }
}
