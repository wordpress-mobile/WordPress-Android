package org.wordpress.android.ui.main;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.R;

/**
 * Superclass used for shared logic of fragments with dual pane layout support.
 */
public class DualPaneFragment extends Fragment {

    protected boolean isInDualPaneMode() {
        return getResources().getBoolean(R.bool.dual_pane);
    }

    @Nullable
    protected DualPaneDashboard getDashboard() {
        if (!isPartOfDualPaneDashboard()) {
            return null;
        }
        return (DualPaneDashboard) getParentFragment();
    }

    protected boolean isPartOfDualPaneDashboard() {
        return getParentFragment() != null && getParentFragment() instanceof DualPaneDashboard;
    }
}
