package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.android.UI
import javax.inject.Named

const val UI_CONTEXT = "UI_CONTEXT"
const val COMMON_POOL_CONTEXT = "COMMON_POOL_CONTEXT"

@Module
class ThreadModule {
    @Provides
    @Named(UI_CONTEXT)
    fun provideUiContext(): CoroutineDispatcher {
        return UI
    }

    @Provides
    @Named(COMMON_POOL_CONTEXT)
    fun provideBackgroundContext(): CoroutineDispatcher {
        return CommonPool
    }
}
