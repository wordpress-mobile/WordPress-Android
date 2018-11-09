package org.wordpress.android.modules;

import android.app.Application;
import android.content.Context;

import org.wordpress.android.ui.news.LocalNewsService;
import org.wordpress.android.ui.news.NewsService;
import org.wordpress.android.ui.stats.refresh.StatsFragment;
import org.wordpress.android.ui.stats.refresh.StatsListFragment;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    abstract Context bindContext(Application application);

    @Provides
    public static NewsService provideLocalNewsService(Context context) {
        return new LocalNewsService(context);
    }

    @ContributesAndroidInjector
    abstract StatsListFragment contributeStatListFragment();

    @ContributesAndroidInjector
    abstract StatsFragment contributeStatsFragment();
}
