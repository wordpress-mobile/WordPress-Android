package org.wordpress.android.modules;

import android.app.Application;

import org.wordpress.android.fluxc.module.DatabaseModule;
import org.wordpress.android.fluxc.module.OkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseToolsModule;
import org.wordpress.android.login.di.LoginFragmentModule;
import org.wordpress.android.login.di.LoginServiceModule;
import org.wordpress.android.ui.stats.refresh.StatsModule;
import org.wordpress.android.ui.suggestion.SuggestionSourceSubcomponent.SuggestionSourceModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        AppConfigModule.class,
        OkHttpClientModule.class,
        InterceptorModule.class,
        ReleaseNetworkModule.class,
        DatabaseModule.class,
        LegacyModule.class,
        ReleaseToolsModule.class,
        AndroidSupportInjectionModule.class,
        ViewModelModule.class,
        StatsModule.class,
        TrackerModule.class,
        SuggestionSourceModule.class,
        ExperimentModule.class,
        // Login flow library
        LoginAnalyticsModule.class,
        LoginFragmentModule.class,
        LoginServiceModule.class,
        SupportModule.class,
        ThreadModule.class,
        CrashLoggingModule.class
})
public interface AppComponentDebug extends AppComponent {
    @Component.Builder
    interface Builder extends AppComponent.Builder {
        @Override
        @BindsInstance
        AppComponentDebug.Builder application(Application application);
    }
}
