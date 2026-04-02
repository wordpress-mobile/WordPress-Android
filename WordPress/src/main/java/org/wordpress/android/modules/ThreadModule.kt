package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.util.helpers.Debouncer
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class ThreadModule {
    /* SCOPE */

    @Provides
    @Named(APPLICATION_SCOPE)
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default)
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
