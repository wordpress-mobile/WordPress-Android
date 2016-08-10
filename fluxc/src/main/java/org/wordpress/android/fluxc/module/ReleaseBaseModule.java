package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.Dispatcher;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseBaseModule {
    @Singleton
    @Provides
    public Dispatcher provideDispatcher() {
        return new Dispatcher();
    }
}
