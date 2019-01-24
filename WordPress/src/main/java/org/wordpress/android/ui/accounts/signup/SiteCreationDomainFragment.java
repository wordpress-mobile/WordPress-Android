package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainLoaderFragment.DomainSuggestionEvent;
import org.wordpress.android.util.ActivityUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class SiteCreationDomainFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_domain_fragment_tag";

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_CATEGORY = "ARG_SITE_CATEGORY";

    private static final String KEY_QUERY_STRING = "KEY_QUERY_STRING";
    private static final String KEY_KEYWORDS = "KEY_KEYWORDS";
    private static final String KEY_CARRY_OVER_DOMAIN = "KEY_CARRY_OVER_DOMAIN";
    private static final String KEY_SELECTED_DOMAIN = "KEY_SELECTED_DOMAIN";

    private String mSiteTitle;
    private String mSiteCategory;
    private String mKeywords = "";

    private String mQueryString;

    private String mCarryOverDomain;
    private String mSelectedDomain;

    private Button mFinishButton;

    private SiteCreationDomainAdapter mSiteCreationDomainAdapter;

    @Inject SiteStore mSiteStore;

    public static SiteCreationDomainFragment newInstance(String siteTitle, String siteCategory) {
        SiteCreationDomainFragment fragment = new SiteCreationDomainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SITE_TITLE, siteTitle);
        args.putString(ARG_SITE_CATEGORY, siteCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_domain_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.site_creation_domain_selection_title);
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mSiteCreationDomainAdapter);

        View bottomShadow = rootView.findViewById(R.id.bottom_shadow);
        bottomShadow.setVisibility(View.VISIBLE);

        ViewGroup bottomButtons = rootView.findViewById(R.id.bottom_buttons);
        bottomButtons.setVisibility(View.VISIBLE);

        mFinishButton = rootView.findViewById(R.id.finish_button);
        mFinishButton.setVisibility(View.VISIBLE);
        mFinishButton.setEnabled(mSelectedDomain != null);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // hide keyboard before calling the site creation action
                if (getActivity() != null) {
                    ActivityUtils.hideKeyboardForced(getActivity().getCurrentFocus());
                }

                mSiteCreationListener.withDomain(mSelectedDomain);
            }
        });
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
            mSiteTitle = getArguments().getString(ARG_SITE_TITLE);
            mSiteCategory = getArguments().getString(ARG_SITE_CATEGORY);
        }

        if (savedInstanceState != null) {
            mQueryString = savedInstanceState.getString(KEY_QUERY_STRING);
            mKeywords = savedInstanceState.getString(KEY_KEYWORDS);
            mCarryOverDomain = savedInstanceState.getString(KEY_CARRY_OVER_DOMAIN);
            mSelectedDomain = savedInstanceState.getString(KEY_SELECTED_DOMAIN);
        } else {
            mQueryString = mSiteTitle;
        }

        // Need to do this early so the mSiteCreationDomainAdapter gets initialized before RecyclerView needs it. This
        // ensures that on rotation, the RecyclerView will have its data ready before layout and scroll position will
        // hold correctly automatically.
        EventBus.getDefault().register(this);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SiteCreationDomainLoaderFragment loaderFragment =
                    SiteCreationDomainLoaderFragment.newInstance(mSiteTitle, mSiteCategory);
            loaderFragment.setRetainInstance(true);
            fragmentTransaction.add(loaderFragment, SiteCreationDomainLoaderFragment.TAG);
            fragmentTransaction.commitNow();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_DOMAIN_VIEWED);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
        mSiteCreationListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_QUERY_STRING, mQueryString);
        outState.putString(KEY_KEYWORDS, mKeywords);
        outState.putString(KEY_CARRY_OVER_DOMAIN, mCarryOverDomain);
        outState.putString(KEY_SELECTED_DOMAIN, mSelectedDomain);
    }

    private SiteCreationDomainLoaderFragment getLoaderFragment() {
        return (SiteCreationDomainLoaderFragment) getChildFragmentManager()
                .findFragmentByTag(SiteCreationDomainLoaderFragment.TAG);
    }

    private void updateFinishButton() {
        // the UI will not be fully setup yet on the initial sticky event registration so, only update it if setup.
        if (mFinishButton != null) {
            mFinishButton.setEnabled(mSelectedDomain != null);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDomainSuggestionEvent(DomainSuggestionEvent event) {
        if (mSiteCreationDomainAdapter == null) {
            // Fragment is initializing or rotating so, just instantiate a new adapter.
            mSiteCreationDomainAdapter = new SiteCreationDomainAdapter(
                    getContext(), mKeywords,
                    new SiteCreationDomainAdapter.OnAdapterListener() {
                        @Override
                        public void onKeywordsChange(String keywords) {
                            mKeywords = keywords;
                            mCarryOverDomain = mSelectedDomain;

                            // fallback to using the provided site title as query if text is empty
                            mQueryString = TextUtils.isEmpty(keywords.trim()) ? mSiteTitle : keywords;

                            getLoaderFragment().load(mQueryString, mSiteCategory);
                        }

                        @Override
                        public void onSelectionChange(String domain) {
                            mSelectedDomain = domain;
                            updateFinishButton();
                        }
                    });
        }

        switch (event.getStep()) {
            case UPDATING:
                mSelectedDomain = mCarryOverDomain;
                mSiteCreationDomainAdapter.setData(true, mCarryOverDomain, mSelectedDomain, null);
                break;
            case ERROR:
                mSiteCreationDomainAdapter.setData(false, mCarryOverDomain, mSelectedDomain, null);
                break;
            case FINISHED:
                if (!event.getQuery().equals(mQueryString)) {
                    // this is not the result for the latest query the debouncer sent so, ignore it
                    break;
                }

                ArrayList<String> suggestions = new ArrayList<>();
                for (DomainSuggestionResponse suggestionResponse : event.getEvent().suggestions) {
                    suggestions.add(suggestionResponse.domain_name);
                }

                mSiteCreationDomainAdapter.setData(false, mCarryOverDomain, mSelectedDomain, suggestions);
                break;
        }

        updateFinishButton();
    }
}
