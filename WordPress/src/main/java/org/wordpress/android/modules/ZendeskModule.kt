package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import org.wordpress.android.support.ZendeskHelper
import javax.inject.Singleton

@Module
class ZendeskModule {
    @Singleton
    @Provides
    fun provideZendeskHelper(): ZendeskHelper = object : ZendeskHelper {}
}
