package org.wordpress.android.ui.publicize;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;

class PublicizeBaseFragment extends Fragment {
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
            // important for accessibility - talkBack
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
