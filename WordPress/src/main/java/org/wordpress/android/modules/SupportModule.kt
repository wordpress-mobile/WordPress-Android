package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.support.SupportHelper
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Singleton

@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskHelper(
        accountStore: AccountStore,
        siteStore: SiteStore,
        supportHelper: SupportHelper,
        buildConfigWrapper: BuildConfigWrapper
    ): ZendeskHelper = ZendeskHelper(accountStore, siteStore, supportHelper, buildConfigWrapper)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
