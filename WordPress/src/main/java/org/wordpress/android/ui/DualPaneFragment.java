package org.wordpress.android.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import org.wordpress.android.R;

/**
 * Superclass used for shared logic of fragments with dual pane layout support.
 */
public class DualPaneFragment extends Fragment {

    //Currently we only support dual pane functionality with nested fragments (aka if fragment is nested it's in dual pane)
    //depending on future requirements we could expand this method to add qualifying resource/view check, etc.
    protected boolean isInDualPaneMode() {
        return getParentFragment() != null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //To overlay main activity's tab bar, fragment elevation must be >= tab bar's elevation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(getResources().getDimensionPixelSize(R.dimen.tabs_elevation));
        }
    }
}
