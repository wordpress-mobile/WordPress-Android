package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.ToastUtils;

public class SiteCreationThemeFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_theme_fragment_tag";

    private static final String ARG_THEME_CATEGORY = "ARG_THEME_CATEGORY";

    public enum ThemeCategory {
        BLOG,
        WEBSITE,
        PORTFOLIO
    }

    private ThemeCategory mThemeCategory;

    public static SiteCreationThemeFragment newInstance(ThemeCategory themeCategory) {
        SiteCreationThemeFragment fragment = new SiteCreationThemeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_THEME_CATEGORY, themeCategory.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_theme_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        switch (mThemeCategory) {
            case BLOG:
                ToastUtils.showToast(getContext(), "Blog category selected");
                break;
            case WEBSITE:
                ToastUtils.showToast(getContext(), "Website category selected");
                break;
            case PORTFOLIO:
                ToastUtils.showToast(getContext(), "Portfolio category selected");
                break;
        }
    }

    @Override
    protected void onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpThemeScreen();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (getArguments() != null) {
            mThemeCategory = ThemeCategory.values()[getArguments().getInt(ARG_THEME_CATEGORY)];
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_THEME_VIEWED);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSiteCreationListener = null;
    }
}
