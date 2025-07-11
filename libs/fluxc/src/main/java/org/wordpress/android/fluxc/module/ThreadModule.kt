package org.wordpress.android.fluxc.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

const val FLUXC_SCOPE = "FLUXC_SCOPE"
const val FLUXC_UI_THREAD = "FLUXC_UI_THREAD"

@Module
class ThreadModule {
    @Singleton
    @Provides
    @Named(FLUXC_SCOPE)
    fun provideFluxCScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }

    @Provides
    @Named(FLUXC_UI_THREAD)
    fun provideFluxCUiDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }
}
