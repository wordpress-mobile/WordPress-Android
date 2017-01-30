package org.wordpress.android.modules;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.wordpress.rest.RestClient.REST_CLIENT_VERSIONS;

import org.wordpress.android.WordPress;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientUtils;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LegacyRestClientModule {
    @Singleton
    @Provides
    OAuthAuthenticator getOAuthAuthenticator() {
        return OAuthAuthenticatorFactory.instantiate();
    }

    @Singleton
    @Named("v1")
    @Provides
    RestClientUtils getRestClientUtils1(Context context, @Named("regular") RequestQueue queue,
                                        OAuthAuthenticator authenticator) {

        return new RestClientUtils(context, queue, authenticator, null, REST_CLIENT_VERSIONS.V1);
    }

    @Singleton
    @Named("v1.1")
    @Provides
    RestClientUtils getRestClientUtils1_1(Context context, @Named("regular") RequestQueue queue,
                                          OAuthAuthenticator authenticator) {
        return new RestClientUtils(context, queue, authenticator, null, REST_CLIENT_VERSIONS.V1_1);
    }

    @Singleton
    @Named("v1.2")
    @Provides
    RestClientUtils getRestClientUtils1_2(Context context, @Named("regular") RequestQueue queue,
                                          OAuthAuthenticator authenticator) {
        return new RestClientUtils(context, queue, authenticator, null, REST_CLIENT_VERSIONS.V1_2);
    }

    @Singleton
    @Named("v1.3")
    @Provides
    RestClientUtils getRestClientUtils1_3(Context context, @Named("regular") RequestQueue queue,
                                          OAuthAuthenticator authenticator) {
        return new RestClientUtils(context, queue, authenticator, null, REST_CLIENT_VERSIONS.V1_3);
    }

    @Singleton
    @Named("v0")
    @Provides
    RestClientUtils getRestClientUtils0(Context context, @Named("regular") RequestQueue queue,
                                        OAuthAuthenticator authenticator) {
        return new RestClientUtils(context, queue, authenticator, null, REST_CLIENT_VERSIONS.V0);
    }

    @Singleton
    @Provides
    ImageCache getImageCache() {
        return WordPress.getBitmapCache();
    }
}
