package org.wordpress.android.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.DatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseToolsModule
import org.wordpress.android.login.di.LoginFragmentModule
import org.wordpress.android.login.di.LoginServiceModule

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        LoginFragmentModule::class,
        LoginServiceModule::class,
    ]
)
abstract class LoginModule
