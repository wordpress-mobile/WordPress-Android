package org.wordpress.android.ui.prefs.accountsettings.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Named
import javax.inject.Singleton

@Module
class AccountSettingsModule {
    @Provides
    @Singleton
    fun provideFetchAccountSettingsUseCase(
        eventBusWrapper: EventBusWrapper,
        continuationWrapper: ContinuationWrapper<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): FetchAccountSettingsInteractor {
        return FetchAccountSettingsUseCase(
                eventBusWrapper,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun providePushAccountSettingsUseCase(
        eventBusWrapper: EventBusWrapper,
        continuationWrapper: ContinuationWrapperWithConcurrency<OnAccountChanged>,
        @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher
    ): PushAccountSettingsInteractor {
        return PushAccountSettingsUseCase(
                eventBusWrapper,
                continuationWrapper,
                ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideGetSitesUseCase(
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
    fun provideGetAccountsUseCase(
        accountStore: AccountStore
    ): GetAccountInteractor {
        return GetAccountUseCase(
                accountStore
        )
    }
}
