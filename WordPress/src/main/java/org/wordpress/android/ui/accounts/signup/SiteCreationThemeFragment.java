package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeLoaderFragment.OnThemeLoadingUpdated;

import java.util.List;

import javax.inject.Inject;

public class SiteCreationThemeFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_theme_fragment_tag";

    private static final String ARG_THEME_CATEGORY = "ARG_THEME_CATEGORY";

    private String mThemeCategory;

    private SiteCreationThemeAdapter mSiteCreationThemeAdapter;

    @Inject ThemeStore mThemeStore;

    public static SiteCreationThemeFragment newInstance(String themeCategory) {
        SiteCreationThemeFragment fragment = new SiteCreationThemeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THEME_CATEGORY, themeCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_theme_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.site_creation_theme_selection_title);
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mSiteCreationThemeAdapter);
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
            mThemeCategory = getArguments().getString(ARG_THEME_CATEGORY);
        }

        // Will trigger the update method since the onThemeLoadingUpdated event is sticky.
        // Need to do this early so the mSiteCreationThemeAdapter gets initialized before RecyclerView needs it. This
        // ensures that on rotation, the RecyclerView will have its data ready before layout and scroll position will
        // hold correctly automatically.
        EventBus.getDefault().register(this);
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
        EventBus.getDefault().unregister(this);
        mSiteCreationListener = null;
    }

    private List<ThemeModel> getThemes() {
        return mThemeStore.getWpComMobileFriendlyThemes(mThemeCategory);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onThemeLoadingUpdated(OnThemeLoadingUpdated event) {
        if (mSiteCreationThemeAdapter == null) {
            // Fragment is initializing or rotating so, just instantiate a new adapter.
            mSiteCreationThemeAdapter = new SiteCreationThemeAdapter(getContext(), mSiteCreationListener);
        }

        switch (event.getPhase()) {
            case UPDATING:
                mSiteCreationThemeAdapter.setData(true, null, 0);
                break;
            case ERROR:
                mSiteCreationThemeAdapter.setData(false, null, R.string.error_generic);
                break;
            case ERROR_NO_CONNECTIVITY:
                mSiteCreationThemeAdapter.setData(false, null, R.string.error_generic_network);
                break;
            case FINISHED:
                mSiteCreationThemeAdapter.setData(false, getThemes(), 0);
                break;
        }
    }
}
