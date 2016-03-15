package org.wordpress.android.stores.release;

import org.wordpress.android.stores.example.AppSecretsModule;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.module.ReleaseBaseModule;
import org.wordpress.android.stores.module.ReleaseNetworkModule;
import org.wordpress.android.stores.module.ReleaseStoreModule;

import javax.inject.Singleton;

import dagger.Component;

// Same module stack as the Release App component.
@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface ReleaseStack_AppComponent {
    void inject(ReleaseStack_AccountTest test);
    void inject(ReleaseStack_SiteTest test);
    void inject(ReleaseStack_SiteTestWPCOM test);
}
