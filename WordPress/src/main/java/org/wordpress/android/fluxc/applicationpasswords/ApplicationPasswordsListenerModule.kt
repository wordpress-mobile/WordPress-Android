package org.wordpress.android.fluxc.applicationpasswords

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@InstallIn(SingletonComponent::class)
@Module
interface ApplicationPasswordsListenerModule {
    @Binds
    fun bindApplicationPasswordsListener(
        listener: WPApplicationPasswordsListener
    ): ApplicationPasswordsListener
}
