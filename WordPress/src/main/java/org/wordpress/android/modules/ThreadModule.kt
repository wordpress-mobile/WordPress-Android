package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.util.helpers.Debouncer
import javax.inject.Named

const val UI_SCOPE = "UI_SCOPE"
const val DEFAULT_SCOPE = "DEFAULT_SCOPE"
const val UI_THREAD = "UI_THREAD"
const val BG_THREAD = "BG_THREAD"
const val IO_THREAD = "IO_THREAD"

@Module
class ThreadModule {
    @Provides
    @Named(UI_SCOPE)
    fun provideUiScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main)
    }

    @Provides
    @Named(UI_THREAD)
    fun provideUiDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    @Named(DEFAULT_SCOPE)
    fun provideBackgroundScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default)
    }

    @Provides
    @Named(BG_THREAD)
    fun provideBackgroundDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Named(IO_THREAD)
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    fun provideDebouncer(): Debouncer {
        return Debouncer()
    }
}
