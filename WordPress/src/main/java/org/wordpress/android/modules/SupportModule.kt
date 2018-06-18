package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import org.wordpress.android.support.SupportHelper
import org.wordpress.android.support.ZendeskHelper
import javax.inject.Singleton

@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskHelper(supportHelper: SupportHelper): ZendeskHelper = ZendeskHelper(supportHelper)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
