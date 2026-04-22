package org.wordpress.android.networking.restapi.taxonomy

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsClient

@Module
@InstallIn(SingletonComponent::class)
abstract class TaxonomyRsClientModule {
    @Binds
    abstract fun bindTaxonomyRsClient(impl: TaxonomyRsClientImpl): TaxonomyRsClient
}
