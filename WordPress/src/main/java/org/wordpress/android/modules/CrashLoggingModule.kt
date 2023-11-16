package org.wordpress.android.modules

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(
            application: Application,
            crashLoggingDataProvider: CrashLoggingDataProvider,
            @Named(APPLICATION_SCOPE) appScope: CoroutineScope
        ): CrashLogging {
            return CrashLoggingProvider.createInstance(application, crashLoggingDataProvider, appScope)
        }
    }

    @Binds
    fun bindCrashLoggingDataProvider(dataProvider: WPCrashLoggingDataProvider): CrashLoggingDataProvider
}
