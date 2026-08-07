package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.helpers.Debouncer
import javax.inject.Named

const val APPLICATION_SCOPE = "APPLICATION_SCOPE"

const val UI_THREAD = "UI_THREAD"
const val BG_THREAD = "BG_THREAD"
const val IO_THREAD = "IO_THREAD"

@InstallIn(SingletonComponent::class)
@Module
class ThreadModule {
    /* SCOPE */

    /**
     * Uses a [SupervisorJob] and a [CoroutineExceptionHandler] so a single uncaught exception can't
     * cancel the scope's job - without them, one failing child permanently kills the scope and every
     * later launch on it silently becomes a no-op.
     */
    @Provides
    @Named(APPLICATION_SCOPE)
    fun provideApplicationScope(): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            AppLog.e(AppLog.T.UTILS, "Uncaught exception in the application scope", throwable)
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    }

    /* DISPATCHER */

    @Provides
    @Named(UI_THREAD)
    fun provideUiDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    @Named(BG_THREAD)
    fun provideBgDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Named(IO_THREAD)
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    /* OTHER */

    @Provides
    fun provideDebouncer(): Debouncer {
        return Debouncer()
    }
}
