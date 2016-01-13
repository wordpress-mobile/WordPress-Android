package org.wordpress.android.ui;

import android.support.v4.app.Fragment;

/**
 * Superclass used for fragments that require special logic while in dual pane layout.
 */
public class DualPaneFragment extends Fragment {

    //Currently we only support dual pane functionality with nested fragments (aka if fragment is nested it's in dual pane)
    //depending on future requirements we could expand this method to add qualifying resource/view check, etc.
    protected boolean isInDualPaneMode() {
        return getParentFragment() != null;
    }
}
