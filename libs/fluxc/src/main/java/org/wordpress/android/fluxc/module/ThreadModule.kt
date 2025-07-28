package org.wordpress.android.fluxc.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

const val FLUXC_SCOPE = "FLUXC_SCOPE"

@Module
class ThreadModule {
    @Singleton
    @Provides
    @Named(FLUXC_SCOPE)
    fun provideFluxCScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }
}
