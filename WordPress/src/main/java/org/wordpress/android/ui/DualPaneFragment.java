package org.wordpress.android.ui;

import android.support.v4.app.Fragment;

public class DualPaneFragment extends Fragment {

    protected boolean isNested() {
        return getParentFragment() != null;
    }
}
