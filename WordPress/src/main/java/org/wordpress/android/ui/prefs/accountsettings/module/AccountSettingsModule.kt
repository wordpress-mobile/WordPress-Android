package org.wordpress.android.ui.prefs.accountsettings.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCaseImpl
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCaseImpl
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCaseImpl
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCaseImpl
import org.wordpress.android.ui.utils.ConcurrentContinuationWrapper
import org.wordpress.android.ui.utils.ContinuationWrapper
import org.wordpress.android.ui.utils.DefaultContinuationWrapper
import javax.inject.Named
import javax.inject.Singleton

const val DEFAULT_CONTINUATION = "DEFAULT_CONTINUATION"
const val CONCURRENT_CONTINUATION = "CONCURRENT_CONTINUATION"

@Module
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

    @Provides
    @Singleton
    fun provideFetchAccountSettingsUseCase(
        dispatcher: Dispatcher,
        @Named(DEFAULT_CONTINUATION) continuationWrapper: ContinuationWrapper<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): FetchAccountSettingsUseCase {
        return FetchAccountSettingsUseCaseImpl(
                dispatcher,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun providePushAccountSettingsUseCase(
        dispatcher: Dispatcher,
        @Named(CONCURRENT_CONTINUATION) continuationWrapper: ContinuationWrapper<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): PushAccountSettingsUseCase {
        return PushAccountSettingsUseCaseImpl(
                dispatcher,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideGetSitesUseCase(
        siteStore: SiteStore,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): GetSitesUseCase {
        return GetSitesUseCaseImpl(
                ioDispatcher,
                siteStore
        )
    }

    @Provides
    @Singleton
    fun provideGetAccountUseCase(
        accountStore: AccountStore
    ): GetAccountUseCase {
        return GetAccountUseCaseImpl(
                accountStore
        )
    }
}
