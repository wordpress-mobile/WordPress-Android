package org.wordpress.android.fluxc.module;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader.ImageCache;

import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseToolsModule {
    @Singleton
    @Provides
    public FluxCImageLoader provideFluxCImageLoader(@Named("custom-ssl") RequestQueue queue, ImageCache imageCache,
                                                    AccessToken accessToken, HTTPAuthManager httpAuthManager,
                                                    UserAgent userAgent) {
        return new FluxCImageLoader(queue, imageCache, accessToken, httpAuthManager, userAgent);
    }
}
