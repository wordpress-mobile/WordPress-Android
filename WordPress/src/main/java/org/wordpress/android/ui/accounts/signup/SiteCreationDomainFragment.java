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
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainLoaderFragment.DomainSuggestionEvent;

import javax.inject.Inject;

public class SiteCreationDomainFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creation_domain_fragment_tag";

    private static final String ARG_USERNAME = "ARG_USERNAME";

    private static final String KEY_KEYWORDS = "KEY_KEYWORDS";
    private static final String KEY_SELECTED_DOMAIN_SUGGESTION_INDEX = "KEY_SELECTED_DOMAIN_SUGGESTION_INDEX";

    private String mUsername;
    private String mKeywords = "";
    private int mSelectedDomainSuggestionIndex = -1;

    private String mDomain;

    private Button mFinishButton;

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
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mSiteCreationDomainAdapter);

        View bottomShadow = rootView.findViewById(R.id.bottom_shadow);
        bottomShadow.setVisibility(View.VISIBLE);

        ViewGroup bottomButtons = rootView.findViewById(R.id.bottom_buttons);
        bottomButtons.setVisibility(View.VISIBLE);

        mFinishButton = rootView.findViewById(R.id.finish_button);
        mFinishButton.setVisibility(View.VISIBLE);
        mFinishButton.setEnabled(mSelectedDomainSuggestionIndex > -1);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSiteCreationListener.withDomain(mDomain);
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
            mUsername = getArguments().getString(ARG_USERNAME);
        }

        if (savedInstanceState != null) {
            mKeywords = savedInstanceState.getString(KEY_KEYWORDS);
            mSelectedDomainSuggestionIndex = savedInstanceState.getInt(KEY_SELECTED_DOMAIN_SUGGESTION_INDEX);
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

        outState.putString(KEY_KEYWORDS, mKeywords);
        outState.putInt(KEY_SELECTED_DOMAIN_SUGGESTION_INDEX, mSelectedDomainSuggestionIndex);
    }

    private SiteCreationDomainLoaderFragment getLoaderFragment() {
        return (SiteCreationDomainLoaderFragment) getChildFragmentManager()
                .findFragmentByTag(SiteCreationDomainLoaderFragment.TAG);
    }

    private void updateFinishButton() {
        // the UI will not be fully setup yet on the initial sticky event registration so, only update it if setup.
        if (mFinishButton != null) {
            mFinishButton.setEnabled(mSelectedDomainSuggestionIndex > -1);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDomainSuggestionEvent(DomainSuggestionEvent event) {
        if (mSiteCreationDomainAdapter == null) {
            // Fragment is initializing or rotating so, just instantiate a new adapter.
            mSiteCreationDomainAdapter = new SiteCreationDomainAdapter(getContext(), mKeywords,
                    new SiteCreationDomainAdapter.OnAdapterListener() {
                        @Override
                        public void onKeywordsChange(String keywords) {
                            mKeywords = keywords;

                            // fallback to using the provided username as query if text is empty
                            String queryString = TextUtils.isEmpty(keywords.trim()) ? mUsername : keywords;

                            getLoaderFragment().load(queryString);
                        }

                        @Override
                        public void onSelectionChange(int selectedDomainSuggestionIndex, String domain) {
                            mSelectedDomainSuggestionIndex = selectedDomainSuggestionIndex;
                            mDomain = domain;
                            updateFinishButton();
                        }
                    });
        }

        switch (event.phase) {
            case UPDATING:
                mSelectedDomainSuggestionIndex = -1;
                mSiteCreationDomainAdapter.setData(true, mSelectedDomainSuggestionIndex, null);
                break;
            case ERROR:
                mSiteCreationDomainAdapter.setData(false, mSelectedDomainSuggestionIndex, null);
                break;
            case FINISHED:
                if (mSelectedDomainSuggestionIndex == -1 ) {
                    // select the first result if none is selected
                    mSelectedDomainSuggestionIndex = 0;
                }

                mSiteCreationDomainAdapter.setData(false, mSelectedDomainSuggestionIndex,
                        event.event.suggestions);
                break;
        }

        updateFinishButton();
    }
}
