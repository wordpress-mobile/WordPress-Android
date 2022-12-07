package org.wordpress.android.fluxc.module

import dagger.BindsOptionalOf
import dagger.Module
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@Module
interface ApplicationPasswordsModule {
    @BindsOptionalOf
    fun bindOptionalApplicationPasswordsListener(): ApplicationPasswordsListener
}
