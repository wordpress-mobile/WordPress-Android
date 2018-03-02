package org.wordpress.android.modules;

import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.accounts.login.LoginAnalyticsTracker;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginAnalyticsModule {
    @Provides
    public LoginAnalyticsListener provideAnalyticsListener() {
        return new LoginAnalyticsTracker();
    }
}
