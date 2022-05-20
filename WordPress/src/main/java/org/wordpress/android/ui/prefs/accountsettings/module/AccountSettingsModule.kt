package org.wordpress.android.ui.prefs.accountsettings.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.ui.utils.ConcurrentContinuationWrapper
import org.wordpress.android.ui.utils.ContinuationWrapper
import org.wordpress.android.ui.utils.DefaultContinuationWrapper
import javax.inject.Named
import javax.inject.Singleton

const val DEFAULT_CONTINUATION = "DEFAULT_CONTINUATION"
const val CONCURRENT_CONTINUATION = "CONCURRENT_CONTINUATION"

@Module
@InstallIn(SingletonComponent::class)
class AccountSettingsModule {
    @Provides
    @Singleton
    @Named(DEFAULT_CONTINUATION)
    fun provideDefaultContinuationWrapper(): ContinuationWrapper<OnAccountChanged> {
        return DefaultContinuationWrapper()
    }

    @Provides
    @Singleton
    @Named(CONCURRENT_CONTINUATION)
    fun provideConcurrentContinuationWrapper(): ContinuationWrapper<OnAccountChanged> {
        return ConcurrentContinuationWrapper()
    }
}
