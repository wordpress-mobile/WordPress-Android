package org.wordpress.android.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseToolsModule

@InstallIn(SingletonComponent::class)
@Module(
        includes = [
            ReleaseNetworkModule::class,
            OkHttpClientModule::class,
            ReleaseToolsModule::class,
            DatabaseModule::class
        ]
)
interface FluxCModule
