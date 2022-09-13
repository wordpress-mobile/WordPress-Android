package org.wordpress.android.modules

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(
            @ApplicationContext context: Context,
            crashLoggingDataProvider: CrashLoggingDataProvider
        ): CrashLogging {
            return CrashLoggingProvider.createInstance(context, crashLoggingDataProvider)
        }
    }

    @Binds fun bindCrashLoggingDataProvider(dataProvider: WPCrashLoggingDataProvider): CrashLoggingDataProvider
}
