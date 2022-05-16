package org.wordpress.android.modules;

import com.facebook.stetho.okhttp3.StethoInterceptor;

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
    public Interceptor provideStethoInterceptor() {
        return new StethoInterceptor();
    }
}
