package org.wordpress.android.fluxc.module

import dagger.BindsOptionalOf
import dagger.Module
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsUnavailableListener

@Module
interface ApplicationPasswordsModule {
    @BindsOptionalOf
    fun bindOptionalUnavailableListener(): ApplicationPasswordsUnavailableListener
}
