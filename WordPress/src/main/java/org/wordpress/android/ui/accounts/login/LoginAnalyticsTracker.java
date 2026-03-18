package org.wordpress.android.ui.accounts.login;

import androidx.annotation.NonNull;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class LoginAnalyticsTracker implements LoginAnalyticsListener {
    private AccountStore mAccountStore;
    private SiteStore mSiteStore;
    private UnifiedLoginTracker mUnifiedLoginTracker;

    public LoginAnalyticsTracker(AccountStore accountStore, SiteStore siteStore,
                                 UnifiedLoginTracker unifiedLoginTracker) {
        this.mAccountStore = accountStore;
        this.mSiteStore = siteStore;
        mUnifiedLoginTracker = unifiedLoginTracker;
    }

    @Override
    public void trackAnalyticsSignIn(boolean isWpcom) {
        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, isWpcom);
    }

    @Override
    public void trackLoginAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED);
    }

    @Override
    public void trackLoginAutofillCredentialsUpdated() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED);
    }

    @Override
    public void trackLoginMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);
    }

    @Override
    public void trackLoginMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED);
    }

    @Override
    public void trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.LOGIN_SITE_ADDRESS, Step.START);
    }

    @Override
    public void trackConnectedSiteInfoRequested(String url) {
        Map<String, String> properties = new HashMap<>();
        properties.put("url", url);
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_REQUESTED, properties);
    }

    @Override
    public void trackConnectedSiteInfoFailed(String url, String errorContext, String errorType,
                                             String errorDescription) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_FAILED, errorContext, errorType,
                errorDescription);
    }

    @Override
    public void trackConnectedSiteInfoSucceeded(@NonNull Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_SUCCEEDED, properties);
    }

    @Override
    public void trackFailure(String message) {
        mUnifiedLoginTracker.trackFailure(message);
    }

    @Override
    public void trackSubmitClicked() {
        mUnifiedLoginTracker.trackClick(Click.SUBMIT);
    }

    @Override
    public void trackShowHelpClick() {
        mUnifiedLoginTracker.trackClick(Click.SHOW_HELP);
        mUnifiedLoginTracker.track(Step.HELP);
    }

    @Override
    public void siteAddressFormScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.START);
    }
}
