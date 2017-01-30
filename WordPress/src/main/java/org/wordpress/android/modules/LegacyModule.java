package org.wordpress.android.modules;

import com.android.volley.toolbox.ImageLoader.ImageCache;

import org.wordpress.android.WordPress;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LegacyModule {
    @Singleton
    @Provides
    ImageCache getImageCache() {
        return WordPress.getBitmapCache();
    }
}
