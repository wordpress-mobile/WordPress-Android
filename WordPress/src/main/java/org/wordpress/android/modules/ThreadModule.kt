package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.android.Main
import javax.inject.Named

const val UI_SCOPE = "UI_SCOPE"
const val DEFAULT_SCOPE = "DEFAULT_SCOPE"

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
}
