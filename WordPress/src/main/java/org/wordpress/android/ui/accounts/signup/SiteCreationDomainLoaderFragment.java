package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

public class SiteCreationDomainLoaderFragment extends Fragment {
    public static final String TAG = "site_creation_domain_loader_fragment_tag";

    private static final String ARG_USERNAME = "ARG_USERNAME";
    private static final String ARG_SITE_CATEGORY = "ARG_SITE_CATEGORY";

    public enum DomainUpdateStep {
        UPDATING,
        FINISHED,
        ERROR
    }

    public static class DomainSuggestionEvent {
        private final DomainUpdateStep mStep;
        private final String mQuery;
        private final OnSuggestedDomains mEvent;

        DomainSuggestionEvent(DomainUpdateStep step, String query, OnSuggestedDomains event) {
            mStep = step;
            mQuery = query;
            mEvent = event;
        }

        public OnSuggestedDomains getEvent() {
            return mEvent;
        }

        public DomainUpdateStep getStep() {
            return mStep;
        }

        public String getQuery() {
            return mQuery;
        }
    }

    @Inject Dispatcher mDispatcher;

    // need to inject it even though we're not using it directly, otherwise we can't listen for its event responses
    @Inject SiteStore mSiteStore;

    private void postUpdate(DomainSuggestionEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    public static SiteCreationDomainLoaderFragment newInstance(String username, String siteCategory) {
        SiteCreationDomainLoaderFragment fragment = new SiteCreationDomainLoaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_SITE_CATEGORY, siteCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        String siteCategory = "";
        if (getArguments() != null) {
            siteCategory = getArguments().getString(ARG_SITE_CATEGORY);
        }

        mDispatcher.register(this);

        load(getArguments().getString(ARG_USERNAME), siteCategory);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.unregister(this);
    }

    public void load(String keywords, String siteCategory) {
        // notify if no connectivity but continue anyway
        NetworkUtils.checkConnection(getActivity());

        postUpdate(new DomainSuggestionEvent(DomainUpdateStep.UPDATING, keywords, null));

        boolean isBlog = siteCategory.equals(ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG);

        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, isBlog, 20, isBlog);
        mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSuggestedDomains(OnSuggestedDomains event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "Error fetching domain suggestions: " + event.error.message);
            postUpdate(new DomainSuggestionEvent(DomainUpdateStep.ERROR, event.query, event));
        } else {
            AppLog.d(AppLog.T.API, "WordPress.com domain suggestions fetch successful!");
            postUpdate(new DomainSuggestionEvent(DomainUpdateStep.FINISHED, event.query, event));
        }
    }
}
