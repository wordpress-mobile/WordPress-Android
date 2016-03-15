package org.wordpress.android.stores.module;

import org.wordpress.android.stores.Dispatcher;

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
