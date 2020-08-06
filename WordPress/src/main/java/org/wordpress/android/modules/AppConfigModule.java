package org.wordpress.android.modules;

import android.content.Context;
import android.util.Base64;

import com.goterl.lazycode.lazysodium.utils.Key;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLoggingKey;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;

import dagger.Module;
import dagger.Provides;

@Module
public class AppConfigModule {
    @Provides
    public AppSecrets provideAppSecrets() {
        return new AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET);
    }

    @Provides
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext, WordPress.USER_AGENT_APPNAME);
    }

    @Provides
    public EncryptedLoggingKey provideEncryptedLoggingKey() {
        return new EncryptedLoggingKey(Key.fromBytes(Base64.decode(BuildConfig.ENCRYPTED_LOGGING_KEY, Base64.DEFAULT)));
    }
}
