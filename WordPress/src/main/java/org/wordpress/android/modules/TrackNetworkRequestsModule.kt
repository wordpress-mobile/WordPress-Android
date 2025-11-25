package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.TrackNetworkRequestsPreference
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class TrackNetworkRequestsModule {
    @Singleton
    @Provides
    fun provideTrackNetworkRequestsPreference(appPrefsWrapper: AppPrefsWrapper): TrackNetworkRequestsPreference {
        return object : TrackNetworkRequestsPreference {
            override fun isEnabled(): Boolean = appPrefsWrapper.isTrackNetworkRequestsEnabled
            override fun getRetentionPeriod(): NetworkRequestsRetentionPeriod =
                NetworkRequestsRetentionPeriod.fromInt(appPrefsWrapper.trackNetworkRequestsRetentionPeriod)
        }
    }

    @Singleton
    @Provides
    @IntoSet
    @Named("interceptors")
    fun provideTrackNetworkRequestsInterceptor(
        @ApplicationContext context: Context,
        preference: TrackNetworkRequestsPreference
    ): Interceptor {
        return TrackNetworkRequestsInterceptor(context, preference)
    }
}
