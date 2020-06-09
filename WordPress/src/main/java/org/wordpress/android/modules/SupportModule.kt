package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.support.SupportHelper
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.support.ZendeskPlanFieldHelper
import javax.inject.Singleton

@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskHelper(
        accountStore: AccountStore,
        siteStore: SiteStore,
        supportHelper: SupportHelper,
        zendeskPlanFieldHelper: ZendeskPlanFieldHelper
    ): ZendeskHelper = ZendeskHelper(accountStore, siteStore, supportHelper, zendeskPlanFieldHelper)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()

    @Singleton
    @Provides
    fun provideZendeskPlanFieldHelper(): ZendeskPlanFieldHelper = ZendeskPlanFieldHelper()
}
