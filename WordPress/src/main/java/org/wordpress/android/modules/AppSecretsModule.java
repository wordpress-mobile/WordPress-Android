package org.wordpress.android.modules;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;

import dagger.Module;
import dagger.Provides;

@Module
public class AppSecretsModule {
    @Provides
    public AppSecrets provideAppSecrets() {
        return new AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET);
    }
}
