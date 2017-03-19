package org.wordpress.android.modules;

import com.android.volley.toolbox.ImageLoader.ImageCache;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.networking.OAuthAuthenticator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LegacyModule {
    @Singleton
    @Provides
    ImageCache provideImageCache() {
        return WordPress.getBitmapCache();
    }

    @Singleton
    @Provides
    OAuthAuthenticator provideOAuthAuthenicator(AccessToken accessToken) {
        return new OAuthAuthenticator(accessToken);
    }
}
