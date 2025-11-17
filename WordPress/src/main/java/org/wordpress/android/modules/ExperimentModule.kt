package org.wordpress.android.modules

import android.content.Context
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ExperimentModule {

    @Provides
    @Singleton
    fun provideVariationsRepository(
        @ApplicationContext appContext: Context,
        appLogWrapper: AppLogWrapper,
        @Named(APPLICATION_SCOPE) appScope: CoroutineScope,
    ) = VariationsRepository.create(
        platform = "wpandroid",
        experiments = emptySet(),
        failFast = BuildConfig.DEBUG,
        logger = object : ExperimentLogger {
            override fun d(message: String) = appLogWrapper.d(AppLog.T.API, message)
            override fun e(message: String, throwable: Throwable?) = appLogWrapper.e(AppLog.T.API, message, throwable)
        },
        cacheDir = File(appContext.filesDir, "experiments"),
        coroutineScope = appScope
    )

}
