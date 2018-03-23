package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.ThemeStore;

public class SiteCreationCategoryFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_category_fragment_tag";

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_category_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.site_creation_category_title);
        rootView.findViewById(R.id.site_creation_card_blog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSiteCreationListener != null) {
                    mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG);
                }
            }
        });

        rootView.findViewById(R.id.site_creation_card_website).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSiteCreationListener != null) {
                    mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_WEBSITE);
                }
            }
        });

        rootView.findViewById(R.id.site_creation_card_portfolio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSiteCreationListener != null) {
                    mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_PORTFOLIO);
                }
            }
        });
    }

    @Override
    protected void onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CATEGORY_VIEWED);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSiteCreationListener = null;
    }
}
