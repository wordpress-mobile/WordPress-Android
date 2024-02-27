package org.wordpress.android.modules;

import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor;

import org.wordpress.android.WordPressDebug;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;
import okhttp3.Interceptor;


@InstallIn(SingletonComponent.class)
@Module
public class InterceptorModule {
    @Provides @IntoSet @Named("network-interceptors")
    public Interceptor provideFlipperInterceptor() {
        return new FlipperOkhttpInterceptor(WordPressDebug.NETWORK_FLIPPER_PLUGIN);
    }
}
