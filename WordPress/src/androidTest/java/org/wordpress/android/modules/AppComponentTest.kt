package org.wordpress.android.modules

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseToolsModule
import org.wordpress.android.login.di.LoginFragmentModule
import org.wordpress.android.login.di.LoginServiceModule
import org.wordpress.android.ui.stats.refresh.StatsModule
import org.wordpress.android.ui.suggestion.SuggestionSourceSubcomponent.SuggestionSourceModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            ApplicationModule::class,
            AppConfigModule::class,
            OkHttpClientModule::class,
            InterceptorModuleTest::class,
            ReleaseNetworkModule::class,
            LegacyModule::class,
            ReleaseToolsModule::class,
            DatabaseModule::class,
            AndroidSupportInjectionModule::class,
            ViewModelModule::class,
            StatsModule::class,
            TrackerTestModule::class,
            // Login flow library
            LoginAnalyticsModule::class,
            LoginFragmentModule::class,
            LoginServiceModule::class,
            SupportModule::class,
            ThreadModule::class,
            SuggestionSourceModule::class,
            ExperimentModule::class,
            CrashLoggingModule::class
        ]
)
interface AppComponentTest : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): AppComponentTest.Builder

        override fun build(): AppComponentTest
    }
}
