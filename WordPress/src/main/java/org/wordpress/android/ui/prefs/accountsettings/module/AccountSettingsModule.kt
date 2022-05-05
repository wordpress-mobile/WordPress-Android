package org.wordpress.android.ui.prefs.accountsettings.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import org.wordpress.android.ui.utils.ContinuationWrapper
import org.wordpress.android.ui.utils.ContinuationWrapperWithConcurrency
import javax.inject.Named
import javax.inject.Singleton

@Module
class AccountSettingsModule {

    @Provides
    @Singleton
    fun provideContinuationWrapper(): ContinuationWrapper<OnAccountChanged> {
        return ContinuationWrapper<OnAccountChanged>()
    }

    @Provides
    @Singleton
    fun provideContinuationWrapperWithConcurrency(): ContinuationWrapperWithConcurrency<OnAccountChanged> {
        return ContinuationWrapperWithConcurrency<OnAccountChanged>()
    }

    @Provides
    @Singleton
    fun provideFetchAccountSettingsInteractor(
        dispatcher: Dispatcher,
        continuationWrapper: ContinuationWrapper<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): FetchAccountSettingsInteractor {
        return FetchAccountSettingsUseCase(
                dispatcher,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun providePushAccountSettingsInteractor(
        dispatcher: Dispatcher,
        continuationWrapper: ContinuationWrapperWithConcurrency<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): PushAccountSettingsInteractor {
        return PushAccountSettingsUseCase(
                dispatcher,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideGetSitesInteractor(
        siteStore: SiteStore,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): GetSitesInteractor {
        return GetSitesUseCase(
                ioDispatcher,
                siteStore
        )
    }

    @Provides
    @Singleton
    fun provideGetAccountInteractor(
        accountStore: AccountStore
    ): GetAccountInteractor {
        return GetAccountUseCase(
                accountStore
        )
    }
}
