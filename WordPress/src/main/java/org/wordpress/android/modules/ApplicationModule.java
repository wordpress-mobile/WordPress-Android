package org.wordpress.android.modules;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;

import org.wordpress.android.ui.news.LocalNewsService;
import org.wordpress.android.ui.news.NewsService;
import org.wordpress.android.viewmodel.helpers.ConnectionStatus;
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData;

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

    @Provides
    public static LiveData<ConnectionStatus> provideConnectionStatusLiveData(Context context) {
        return new ConnectionStatusLiveData(context);
    }
}
