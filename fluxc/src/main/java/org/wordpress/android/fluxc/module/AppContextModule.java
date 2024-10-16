package org.wordpress.android.fluxc.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppContextModule {
    private final Context mAppContext;

    public AppContextModule(Context appContext) {
        mAppContext = appContext;
    }

    @Singleton
    @Provides
    Context providesContext() {
        return mAppContext;
    }
}
