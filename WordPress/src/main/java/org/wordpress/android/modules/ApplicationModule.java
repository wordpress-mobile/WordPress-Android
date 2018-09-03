package org.wordpress.android.modules;

import android.app.Application;
import android.content.Context;

import org.wordpress.android.ui.news.LocalNewsService;
import org.wordpress.android.ui.news.NewsService;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    abstract Context bindContext(Application application);

    @Provides
    public static NewsService provideLocalNewsService(Context context) {
        return new LocalNewsService(context);
    }
}
