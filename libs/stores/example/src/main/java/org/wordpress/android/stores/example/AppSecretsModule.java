package org.wordpress.android.stores.example;

import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;

import dagger.Module;
import dagger.Provides;

@Module
public class AppSecretsModule {
    @Provides
    public AppSecrets provideAppSecrets() {
        return new AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET);
    }
}
