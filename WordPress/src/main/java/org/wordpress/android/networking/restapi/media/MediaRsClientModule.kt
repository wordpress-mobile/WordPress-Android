package org.wordpress.android.networking.restapi.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpapi.media.MediaRsClient

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaRsClientModule {
    @Binds
    abstract fun bindMediaRsClient(impl: MediaRsClientImpl): MediaRsClient
}
