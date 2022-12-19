package org.wordpress.android;

import androidx.annotation.NonNull;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;

public class WordPressApp extends WordPress implements HasAndroidInjector {
    @Inject DispatchingAndroidInjector<Object> mDispatchingAndroidInjector;

    @Inject AppInitializer mAppInitializer;

    @Override
    public void onCreate() {
        super.onCreate();

        mAppInitializer.init();
    }

    @Override public AndroidInjector<Object> androidInjector() {
        return mDispatchingAndroidInjector;
    }

    @NonNull @Override public AppInitializer initializer() {
        return mAppInitializer;
    }
}
