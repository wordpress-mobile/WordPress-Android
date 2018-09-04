package org.wordpress.android.ui.publicize;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import org.wordpress.android.R;

public class PublicizeBaseFragment extends Fragment {
    private Toolbar getToolbar() {
        if (getActivity() != null) {
            return (Toolbar) getActivity().findViewById(R.id.toolbar);
        } else {
            return null;
        }
    }

    void setTitle(@StringRes int resId) {
        setTitle(getString(resId));
    }

    void setTitle(String title) {
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
        if (getActivity() != null) {
            // important for accessibiility - talkBack
            getActivity().setTitle(title);
        }
    }

    void setNavigationIcon(@DrawableRes int resId) {
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setNavigationIcon(resId);
        }
    }
}
