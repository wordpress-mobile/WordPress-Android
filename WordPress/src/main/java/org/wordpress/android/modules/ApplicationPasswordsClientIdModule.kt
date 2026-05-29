package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.R
import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DeviceUtils
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApplicationPasswordsClientIdModule {
    @Provides
    @Singleton
    @ApplicationPasswordsClientId
    fun provideApplicationPasswordsClientId(
        @ApplicationContext context: Context,
        buildConfigWrapper: BuildConfigWrapper,
    ): String {
        val deviceName = DeviceUtils.getInstance().getDeviceName(context)
        val resId = if (buildConfigWrapper.isJetpackApp) {
            R.string.application_password_app_name_jetpack
        } else {
            R.string.application_password_app_name_wordpress
        }
        return context.getString(resId, deviceName)
    }
}
