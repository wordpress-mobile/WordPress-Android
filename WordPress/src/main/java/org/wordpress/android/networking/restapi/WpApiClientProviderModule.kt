package org.wordpress.android.networking.restapi

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.networking.rs.WpApiClientProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class WpApiClientProviderModule {
    @Binds
    abstract fun bindWpApiClientProvider(impl: WpApiClientProviderImpl): WpApiClientProvider
}
