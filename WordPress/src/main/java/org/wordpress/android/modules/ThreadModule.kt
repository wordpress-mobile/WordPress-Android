package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.android.Main
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

const val UI_SCOPE = "UI_SCOPE"
const val DEFAULT_SCOPE = "DEFAULT_SCOPE"
const val MAIN_DISPATCHER = "MAIN"
const val IO_DISPATCHER = "IO"

@Module
class ThreadModule {
    @Provides
    @Named(UI_SCOPE)
    fun provideUiScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main)
    }

    @Provides
    @Named(DEFAULT_SCOPE)
    fun provideBackgroundScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default)
    }

    @Provides
    @Named(MAIN_DISPATCHER)
    fun provideMainDispatcher(): CoroutineContext {
        return Dispatchers.Main
    }

    @Provides
    @Named(IO_DISPATCHER)
    fun provideIODispatcher(): CoroutineContext {
        return Dispatchers.IO
    }
}
