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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

public class SiteCreationDomainLoaderFragment extends Fragment {
    public static final String TAG = "site_creation_domain_loader_fragment_tag";

    private static final String ARG_USERNAME = "ARG_USERNAME";

    public enum DomainUpdateStep {
        UPDATING,
        FINISHED,
        ERROR
    }

    static class DomainSuggestionEvent {
        final DomainUpdateStep step;
        final String query;
        final OnSuggestedDomains event;

        DomainSuggestionEvent(DomainUpdateStep step, String query, OnSuggestedDomains event) {
            this.step = step;
            this.query = query;
            this.event = event;
        }
    }
    @Inject Dispatcher mDispatcher;

    // need to inject it even though we're not using it directly, otherwise we can't listen for its event responses
    @Inject SiteStore mSiteStore;

    private void postUpdate(DomainSuggestionEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    public static SiteCreationDomainLoaderFragment newInstance(String username) {
        SiteCreationDomainLoaderFragment fragment = new SiteCreationDomainLoaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mDispatcher.register(this);

        load(getArguments().getString(ARG_USERNAME));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.unregister(this);
    }

    public void load(String keywords) {
        // notify if no connectivity but continue anyway
        NetworkUtils.checkConnection(getActivity());

        postUpdate(new DomainSuggestionEvent(DomainUpdateStep.UPDATING, keywords, null));

        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, false, 20);
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
