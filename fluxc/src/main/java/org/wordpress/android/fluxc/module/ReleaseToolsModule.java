package org.wordpress.android.fluxc.module;

import java.util.Locale;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseToolsModule {
    @Singleton
    @Provides
    public Locale provideLocale() {
        return Locale.getDefault();
    }
}
