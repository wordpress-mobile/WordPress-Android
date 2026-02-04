package org.wordpress.android.modules;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.AppLoginConfig;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.login.LoginAnalyticsTracker;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class LoginAnalyticsModule {
    @Provides
    public LoginAnalyticsListener provideAnalyticsListener(AccountStore accountStore, SiteStore siteStore,
                                                           UnifiedLoginTracker unifiedLoginTracker) {
        return new LoginAnalyticsTracker(accountStore, siteStore, unifiedLoginTracker);
    }

    @Provides
    public AppLoginConfig provideAppLoginConfig() {
        return () -> BuildConfig.IS_JETPACK_APP;
    }
}
