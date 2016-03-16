package org.wordpress.android.modules;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.WordPress;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.module.ReleaseBaseModule;
import org.wordpress.android.stores.module.ReleaseNetworkModule;
import org.wordpress.android.stores.module.ReleaseStoreModule;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureActivity;
import org.wordpress.android.ui.stats.StatsWidgetProvider;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface AppComponent {
    void inject(WordPress application);
    void inject(SignInFragment object);
    void inject(WPMainActivity object);
    void inject(ShareIntentReceiverActivity object);
    void inject(BlogPreferencesActivity object);
    void inject(SiteSettingsFragment object);
    void inject(AccountSettingsFragment object);
    void inject(StatsWidgetConfigureActivity object);
    void inject(StatsWidgetProvider object);
    void inject(StatsActivity object);
    void inject(GCMMessageService object);
}
