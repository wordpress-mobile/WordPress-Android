package org.wordpress.android.stores.mocked;

import org.wordpress.android.stores.example.AppSecretsModule;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.module.ReleaseBaseModule;
import org.wordpress.android.stores.module.ReleaseStoreModule;
import org.wordpress.android.stores.module.MockedNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        MockedNetworkModule.class, // Mocked module
        ReleaseStoreModule.class
})
public interface MockedNetworkAppComponent {
    void inject(AccountStoreTest application);
}

