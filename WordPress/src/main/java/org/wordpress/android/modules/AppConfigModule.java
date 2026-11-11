package org.wordpress.android.modules;

import android.content.Context;

import androidx.annotation.NonNull;

import com.automattic.encryptedlogging.EncryptedLogging;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomiesRestApiMigrationConfig;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.util.config.TaxonomiesRestApiMigrationFeatureConfig;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;


@InstallIn(SingletonComponent.class)
@Module
public class AppConfigModule {
    @NonNull @Provides
    public AppSecrets provideAppSecrets() {
        return new AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET, BuildConfig.WPCOM_REDIRECT_URI);
    }

    @Singleton
    @Provides
    public UserAgent provideUserAgent(@ApplicationContext Context appContext) {
        return new UserAgent(appContext, WordPress.USER_AGENT_APPNAME);
    }

    @Provides
    public TaxonomiesRestApiMigrationConfig provideTaxonomiesRestApiMigrationConfig(
            TaxonomiesRestApiMigrationFeatureConfig featureConfig) {
        return featureConfig::isEnabled;
    }

    @Provides
    @Singleton
    public EncryptedLogging provideEncryptedLogging(@ApplicationContext Context appContext) {
        return EncryptedLogging.Companion.getInstance(
                appContext,
                BuildConfig.ENCRYPTED_LOGGING_KEY,
                BuildConfig.OAUTH_APP_SECRET
        );
    }
}
