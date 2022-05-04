package org.wordpress.android.ui.prefs.accountsettings.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsInteractor
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import org.wordpress.android.ui.stats.refresh.GRANULAR_USE_CASE_FACTORIES
import org.wordpress.android.ui.stats.refresh.YEAR_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.UiModelMapper
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
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
}
