package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainLoaderFragment.DomainSuggestionEvent;

import javax.inject.Inject;

public class SiteCreationDomainFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_domain_fragment_tag";

    private static final String ARG_USERNAME = "ARG_USERNAME";

    private String mUsername;

    private SiteCreationDomainAdapter mSiteCreationDomainAdapter;

    @Inject SiteStore mSiteStore;

    public static SiteCreationDomainFragment newInstance(String username) {
        SiteCreationDomainFragment fragment = new SiteCreationDomainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_domain_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mSiteCreationDomainAdapter);
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
            mUsername = getArguments().getString(ARG_USERNAME);
        }

        // Need to do this early so the mSiteCreationDomainAdapter gets initialized before RecyclerView needs it. This
        //  ensures that on rotation, the RecyclerView will have its data ready before layout and scroll position will
        //  hold correctly automatically.
        EventBus.getDefault().register(this);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SiteCreationDomainLoaderFragment loaderFragment = SiteCreationDomainLoaderFragment.newInstance(mUsername);
            loaderFragment.setRetainInstance(true);
            fragmentTransaction.add(loaderFragment, SiteCreationDomainLoaderFragment.TAG);
            fragmentTransaction.commitNow();
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
        EventBus.getDefault().unregister(this);
        mSiteCreationListener = null;
    }

    private SiteCreationDomainLoaderFragment getLoaderFragment() {
        return (SiteCreationDomainLoaderFragment) getChildFragmentManager()
                .findFragmentByTag(SiteCreationDomainLoaderFragment.TAG);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDomainSuggestionEvent(DomainSuggestionEvent event) {
        if (mSiteCreationDomainAdapter == null) {
            // Fragment is initializing or rotating so, just instantiate a new adapter.
            mSiteCreationDomainAdapter = new SiteCreationDomainAdapter(getContext(), mSiteCreationListener,
                    new SiteCreationDomainAdapter.OnDomainKeywordsListener() {
                        @Override
                        public void onChange(String keywords) {
                            getLoaderFragment().load(keywords);
                        }
                    });
        }

        switch (event.phase) {
            case UPDATING:
                mSiteCreationDomainAdapter.setData(true, null);
                break;
            case ERROR:
                mSiteCreationDomainAdapter.setData(false, null);
                break;
            case FINISHED:
                mSiteCreationDomainAdapter.setData(false, event.event.suggestions);
                break;
        }
    }
}
